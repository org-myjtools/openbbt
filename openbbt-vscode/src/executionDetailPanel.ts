import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';
import * as vscode from 'vscode';
import { AttachmentMeta, ExecNodeInfo, ExecutionListItem, NodeInfo, OpenBBTClient } from './openbbtClient';

// ---------------------------------------------------------------------------
// Messages between extension host and webview
// ---------------------------------------------------------------------------

interface NodeWithResult extends NodeInfo {
    status: 'PENDING' | 'RUNNING' | 'FINISHED';
    result: string | null;
    message: string | null;
    executionNodeId: string | null;
    attachmentCount: number;
}

interface NodeUpdate {
    nodeId: string;
    status: 'PENDING' | 'RUNNING' | 'FINISHED';
    result: string | null;
    message: string | null;
    executionNodeId: string | null;
    attachmentCount: number;
}

interface ExecutionHeader {
    organization: string;
    project: string;
    description: string;
    planId: string;
    planCreatedAt: string;
    executedAt: string;
    executionId: string;
}

type ToWebview =
    | { type: 'init'; node: NodeWithResult; header: ExecutionHeader }
    | { type: 'children'; msgId: number; nodes: NodeWithResult[]; attachments: AttachmentMeta[] }
    | { type: 'update'; nodes: NodeUpdate[] };

type FromWebview =
    | { type: 'ready' }
    | { type: 'expand'; nodeId: string; msgId: number }
    | { type: 'poll'; nodeIds: string[] }
    | { type: 'openAttachment'; executionId: string; executionNodeId: string; attachmentId: string; contentType: string };

// ---------------------------------------------------------------------------
// Panel management — one panel per execution
// ---------------------------------------------------------------------------

const openPanels = new Map<string, vscode.WebviewPanel>();

function formatDate(iso: string): string {
    const d = new Date(iso);
    const p = (n: number) => String(n).padStart(2, '0');
    return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

export async function openExecutionDetail(
    context: vscode.ExtensionContext,
    client: OpenBBTClient,
    execution: ExecutionListItem,
    label: string
): Promise<void> {
    const existing = openPanels.get(execution.executionId);
    if (existing) {
        existing.reveal(vscode.ViewColumn.One);
        return;
    }

    const panel = vscode.window.createWebviewPanel(
        'openbbt.executionDetail',
        `Execution: ${label}`,
        vscode.ViewColumn.One,
        { enableScripts: true, retainContextWhenHidden: true }
    );

    openPanels.set(execution.executionId, panel);
    panel.onDidDispose(() => openPanels.delete(execution.executionId));

    panel.webview.html = buildHtml(panel.webview, context.extensionUri);

    panel.webview.onDidReceiveMessage(async (msg: FromWebview) => {
        if (msg.type === 'ready') {
            try {
                const planNodeRoot = await resolvePlanNodeRoot(client, execution);
                const rootNode = await client.getNode(planNodeRoot);
                const execInfo = await client.getExecutionNode(execution.executionId, rootNode.nodeId);
                const node = mergeResult(rootNode, execInfo);
                const planInfo = await client.getPlan(execution.planId).catch(() => null);
                const header: ExecutionHeader = {
                    organization:  planInfo?.organization          ?? '',
                    project:       planInfo?.project               ?? '',
                    description:   planInfo?.description           ?? '',
                    planId:        execution.planId,
                    planCreatedAt: planInfo?.createdAt ? formatDate(planInfo.createdAt) : '',
                    executedAt:    formatDate(execution.executedAt),
                    executionId:   execution.executionId,
                };
                const payload: ToWebview = { type: 'init', node, header };
                panel.webview.postMessage(payload);
            } catch (err) {
                vscode.window.showErrorMessage(`OpenBBT: failed to load execution detail — ${err}`);
            }
        } else if (msg.type === 'expand') {
            try {
                const [children, attachments] = await Promise.all([
                    client.getChildren(msg.nodeId).then(ch =>
                        Promise.all(ch.map(async n => {
                            const execInfo = await client.getExecutionNode(execution.executionId, n.nodeId);
                            return mergeResult(n, execInfo);
                        }))
                    ),
                    client.listAttachments(execution.executionId, msg.nodeId).catch(() => [] as AttachmentMeta[]),
                ]);
                const payload: ToWebview = { type: 'children', msgId: msg.msgId, nodes: children, attachments };
                panel.webview.postMessage(payload);
            } catch (err) {
                vscode.window.showErrorMessage(`OpenBBT: failed to load children — ${err}`);
            }
        } else if (msg.type === 'poll') {
            try {
                const updates: NodeUpdate[] = await Promise.all(
                    (msg.nodeIds as string[]).map(async (nodeId: string) => {
                        const execInfo = await client.getExecutionNode(execution.executionId, nodeId);
                        return {
                            nodeId,
                            status: deriveStatus(execInfo),
                            result: execInfo?.result ?? null,
                            message: execInfo?.message ?? null,
                            executionNodeId: execInfo?.executionNodeId ?? null,
                            attachmentCount: execInfo?.attachmentCount ?? 0,
                        };
                    })
                );
                panel.webview.postMessage({ type: 'update', nodes: updates } as ToWebview);
            } catch {
                // ignore transient errors
            }
        } else if (msg.type === 'openAttachment') {
            try {
                const data = await client.getAttachment(msg.executionId, msg.executionNodeId, msg.attachmentId);
                const ext = contentTypeToExtension(data.contentType);
                const tmpFile = path.join(os.tmpdir(), `openbbt-attachment-${msg.attachmentId}${ext}`);
                fs.writeFileSync(tmpFile, Buffer.from(data.data, 'base64'));
                await vscode.commands.executeCommand('vscode.open', vscode.Uri.file(tmpFile));
            } catch (err) {
                vscode.window.showErrorMessage(`OpenBBT: failed to open attachment — ${err}`);
            }
        }
    });
}


async function resolvePlanNodeRoot(client: OpenBBTClient, execution: ExecutionListItem): Promise<string> {
    if (execution.planNodeRoot) {
        return execution.planNodeRoot;
    }
    // Fallback for older server versions that don't include planNodeRoot in executions/list
    const plans = await client.listPlans();
    const plan = plans.find(p => p.planId === execution.planId);
    if (!plan) {
        throw new Error(`Plan not found: ${execution.planId}`);
    }
    return plan.planNodeRoot;
}

function deriveStatus(execInfo: ExecNodeInfo | null): 'PENDING' | 'RUNNING' | 'FINISHED' {
    if (!execInfo || !execInfo.startedAt) { return 'PENDING'; }
    if (!execInfo.finishedAt) { return 'RUNNING'; }
    return 'FINISHED';
}

function mergeResult(node: NodeInfo, execInfo: ExecNodeInfo | null): NodeWithResult {
    return {
        ...node,
        status: deriveStatus(execInfo),
        result: execInfo?.result ?? null,
        message: execInfo?.message ?? null,
        executionNodeId: execInfo?.executionNodeId ?? null,
        attachmentCount: execInfo?.attachmentCount ?? 0,
    };
}

function contentTypeToExtension(contentType: string): string {
    const base = contentType.split(';')[0].trim().toLowerCase();
    const map: Record<string, string> = {
        'text/plain': '.txt',
        'text/html': '.html',
        'text/xml': '.xml',
        'application/xml': '.xml',
        'application/json': '.json',
        'image/png': '.png',
        'image/jpeg': '.jpg',
        'image/gif': '.gif',
        'image/svg+xml': '.svg',
        'application/pdf': '.pdf',
    };
    return map[base] ?? '.bin';
}

// ---------------------------------------------------------------------------
// HTML
// ---------------------------------------------------------------------------

function buildHtml(webview: vscode.Webview, _extensionUri: vscode.Uri): string {
    return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline';">
<title>Execution Detail</title>
<style>
  body {
    font-family: var(--vscode-font-family, sans-serif);
    font-size: calc(var(--vscode-font-size, 13px) * 1.3);
    color: var(--vscode-foreground);
    background: var(--vscode-editor-background);
    margin: 0;
    padding: 8px;
  }
  .tree { list-style: none; padding: 0; margin: 0; }
  .tree li { padding: 0; margin: 0; }
  .node-row {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 2px 4px;
    border-radius: 3px;
    cursor: default;
    user-select: none;
    white-space: nowrap;
  }
  .node-row:hover { background: var(--vscode-list-hoverBackground); }
  .node-row.test-case {
    background: var(--vscode-editor-inactiveSelectionBackground);
    border-radius: 3px;
    margin: 1px 0;
  }
  .node-row.test-case:hover { background: var(--vscode-list-hoverBackground); }
  .expander {
    display: inline-block;
    width: 16px;
    text-align: center;
    cursor: pointer;
    flex-shrink: 0;
    color: var(--vscode-foreground);
    opacity: 0.7;
  }
  .expander.leaf { opacity: 0; cursor: default; }
  .node-icon { flex-shrink: 0; }
  .status-icon {
    flex-shrink: 0;
    width: 18px;
    text-align: center;
    line-height: 1;
  }
  .node-name { flex: 1; overflow: hidden; text-overflow: ellipsis; }
  .node-type {
    font-size: 0.8em;
    opacity: 0.6;
    margin-left: 4px;
    flex-shrink: 0;
  }
  .result-message {
    font-size: 0.8em;
    opacity: 0.7;
    margin-left: 6px;
    flex-shrink: 0;
    max-width: 300px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .header {
    border: 1px solid var(--vscode-editorWidget-border, #555);
    border-radius: 6px;
    padding: 12px 16px;
    margin-bottom: 12px;
    background: var(--vscode-editor-inactiveSelectionBackground);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }
  .header-left { flex: 1; min-width: 0; }
  .header-title {
    font-size: 1.4em;
    font-weight: bold;
    margin-bottom: 6px;
  }
  .header-meta {
    font-size: 0.9em;
    color: var(--vscode-descriptionForeground);
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .header-result {
    flex-shrink: 0;
    width: 64px;
    height: 64px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 2em;
    font-weight: bold;
    color: #fff;
  }
  @keyframes spin { to { transform: rotate(360deg); } }
  .spin { display: inline-block; animation: spin 1s linear infinite; }
  .arg-item { padding: 4px 4px 4px 56px; }
  .arg-item pre {
    margin: 0;
    padding: 6px 10px;
    background: var(--vscode-textCodeBlock-background);
    border: 1px solid var(--vscode-editorWidget-border, #444);
    border-radius: 4px;
    font-family: var(--vscode-editor-font-family, monospace);
    font-size: 0.85em;
    white-space: pre-wrap;
    word-break: break-word;
    color: var(--vscode-editor-foreground);
  }
  .arg-item table {
    border-collapse: collapse;
    font-size: 0.85em;
    width: auto;
  }
  .arg-item td, .arg-item th {
    border: 1px solid var(--vscode-editorWidget-border, #444);
    padding: 3px 10px;
    text-align: left;
    white-space: nowrap;
  }
  .arg-item th {
    background: var(--vscode-editor-inactiveSelectionBackground);
    font-weight: bold;
  }
  .tags { display: flex; gap: 4px; flex-shrink: 0; flex-wrap: nowrap; overflow: hidden; }
  .tag {
    font-size: 0.75em;
    padding: 1px 7px;
    border-radius: 999px;
    color: #fff;
    white-space: nowrap;
    opacity: 0.85;
  }
  .children { list-style: none; padding: 0 0 0 20px; margin: 0; }
  .loading { opacity: 0.5; font-style: italic; padding-left: 20px; }
  .project-description {
    font-size: 0.9em;
    color: var(--vscode-descriptionForeground);
    border-left: 3px solid var(--vscode-editorWidget-border, #555);
    padding: 6px 12px;
    margin-bottom: 12px;
    white-space: pre-wrap;
  }
</style>
</head>
<body>
<div id="header"></div>
<div id="description"></div>
<ul class="tree" id="root"></ul>
<script>
  const vscode = acquireVsCodeApi();
  let msgId = 0;
  const pendingExpand = new Map();   // msgId -> resolve
  const nodeStatusMap = new Map();   // nodeId -> { status, result }
  const statusIconEls = new Map();   // nodeId -> span element
  let rootNodeId = null;
  let headerBadgeEl = null;
  let pollTimer = null;
  let pollAttempts = 0;
  const POLL_INTERVAL = 1000;
  const POLL_MAX = 300;

  function startPolling() {
    if (pollTimer !== null) return;
    pollTimer = setInterval(doPoll, POLL_INTERVAL);
  }

  function stopPolling() {
    if (pollTimer !== null) { clearInterval(pollTimer); pollTimer = null; }
  }

  function doPoll() {
    const pending = [];
    for (const [nodeId, info] of nodeStatusMap) {
      if (info.status === 'PENDING' || info.status === 'RUNNING') pending.push(nodeId);
    }
    if (pending.length === 0 || pollAttempts >= POLL_MAX) { stopPolling(); return; }
    pollAttempts++;
    vscode.postMessage({ type: 'poll', nodeIds: pending });
  }

  function trackNode(nodeId, status, result) {
    nodeStatusMap.set(nodeId, { status, result });
  }

  function registerStatusIconEl(nodeId, el) {
    statusIconEls.set(nodeId, el);
  }

  function tagColor(tag) {
    let hash = 0;
    for (let i = 0; i < tag.length; i++) {
      hash = (hash * 31 + tag.charCodeAt(i)) >>> 0;
    }
    const hue = hash % 360;
    return 'hsl(' + hue + ', 55%, 38%)';
  }

  function createTagsEl(tags) {
    if (!tags || tags.length === 0) { return null; }
    const container = document.createElement('div');
    container.className = 'tags';
    for (const tag of tags) {
      const span = document.createElement('span');
      span.className = 'tag';
      span.textContent = tag;
      span.style.background = tagColor(tag);
      container.appendChild(span);
    }
    return container;
  }

  // Node type icon
  function nodeIcon(nodeType) {
    switch (nodeType) {
      case 'TEST_PLAN':    return '📋';
      case 'TEST_SUITE':   return '📁';
      case 'TEST_FEATURE': return '📄';
      case 'TEST_CASE':    return '🔵';
      default:             return null;
    }
  }

  // Apply status/result styling to an existing status-icon element
  function applyStatusIcon(el, status, result) {
    el.removeAttribute('style');
    el.innerHTML = '';
    el.className = 'status-icon';
    if (status === 'PENDING') {
      el.textContent = '○';
      el.style.color = 'var(--vscode-disabledForeground, #999)';
      el.title = 'Pending';
    } else if (status === 'RUNNING') {
      el.innerHTML = '<span class="spin">↻</span>';
      el.style.color = 'var(--vscode-progressBar-background, #0078d4)';
      el.title = 'Running';
    } else {
      el.style.display = 'inline-flex';
      el.style.alignItems = 'center';
      el.style.justifyContent = 'center';
      el.style.width = '18px';
      el.style.height = '18px';
      el.style.borderRadius = '3px';
      el.style.color = '#fff';
      el.style.fontWeight = 'bold';
      el.style.fontSize = '0.85em';
      el.style.flexShrink = '0';
      switch (result) {
        case 'PASSED':  el.textContent = '✓'; el.style.background = 'var(--vscode-testing-iconPassed, #388a34)';  el.title = 'Passed';  break;
        case 'FAILED':  el.textContent = '✗'; el.style.background = 'var(--vscode-testing-iconFailed, #c72e0f)';  el.title = 'Failed';  break;
        case 'ERROR':   el.textContent = '⚠'; el.style.background = '#c72e0f';                                    el.title = 'Error';   break;
        case 'SKIPPED': el.textContent = '⊘'; el.style.background = 'var(--vscode-disabledForeground, #999)';    el.title = 'Skipped'; break;
        default:        el.textContent = '✓'; el.style.background = 'var(--vscode-disabledForeground, #999)';    el.title = 'Finished';
      }
    }
  }

  // Status icon element: placed just before the node label
  function statusIconEl(status, result) {
    const el = document.createElement('span');
    applyStatusIcon(el, status, result);
    return el;
  }

  function applyHeaderBadge(el, status, result) {
    el.removeAttribute('style');
    el.innerHTML = '';
    const resultColors = { PASSED: 'var(--vscode-testing-iconPassed, #388a34)', FAILED: 'var(--vscode-testing-iconFailed, #c72e0f)', ERROR: '#c72e0f', SKIPPED: 'var(--vscode-disabledForeground, #999)' };
    const resultSymbols = { PASSED: '✓', FAILED: '✗', ERROR: '⚠', SKIPPED: '⊘' };
    if (status === 'PENDING') {
      el.style.background = 'var(--vscode-disabledForeground, #555)';
      el.textContent = '○';
      el.title = 'Pending';
    } else if (status === 'RUNNING') {
      el.style.background = 'var(--vscode-progressBar-background, #0078d4)';
      el.innerHTML = '<span class="spin">↻</span>';
      el.title = 'Running';
    } else {
      el.style.background = resultColors[result] || 'var(--vscode-disabledForeground, #999)';
      el.textContent = resultSymbols[result] || '✓';
      el.title = result || 'Finished';
    }
  }

  function createNodeEl(node, indentLevel) {
    const li = document.createElement('li');

    const row = document.createElement('div');
    const highlighted = ['TEST_PLAN','TEST_SUITE','TEST_FEATURE','TEST_CASE'].includes(node.nodeType);
    row.className = 'node-row' + (highlighted ? ' test-case' : '');
    row.style.paddingLeft = (indentLevel * 20) + 'px';

    const hasChildren = node.childCount > 0 || node.attachmentCount > 0 || !!node.document || !!node.dataTable || !!node.message;
    const expander = document.createElement('span');
    expander.className = 'expander' + (hasChildren ? '' : ' leaf');
    expander.textContent = hasChildren ? '▶' : ' ';

    const name = document.createElement('span');
    name.className = 'node-name';
    const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
    const identifier = node.identifier && !uuidPattern.test(node.identifier) ? node.identifier : null;
    name.textContent = node.display || node.name || identifier
      || (node.nodeType ? node.nodeType.replace('TEST_', '') : null)
      || node.nodeId;

    // Track node for polling
    trackNode(node.nodeId, node.status, node.result);
    const sIcon = statusIconEl(node.status, node.result);
    registerStatusIconEl(node.nodeId, sIcon);

    row.appendChild(expander);
    row.appendChild(sIcon);
    row.appendChild(name);
    const tagsEl = createTagsEl(node.tags);
    if (tagsEl) { row.appendChild(tagsEl); }


    li.appendChild(row);

    if (hasChildren) {
      let expanded = false;
      let childrenLoaded = false;

      const childrenContainer = document.createElement('ul');
      childrenContainer.className = 'children';
      childrenContainer.style.display = 'none';
      li.appendChild(childrenContainer);

      expander.addEventListener('click', async () => {
        expanded = !expanded;
        expander.textContent = expanded ? '▼' : '▶';
        childrenContainer.style.display = expanded ? '' : 'none';

        if (expanded && !childrenLoaded) {
          childrenLoaded = true;
          const loading = document.createElement('li');
          loading.className = 'loading';
          loading.textContent = 'Loading…';
          childrenContainer.appendChild(loading);

          const id = msgId++;
          const { nodes, attachments } = await requestChildren(node.nodeId, id);
          childrenContainer.removeChild(loading);

          // docstring / datatable first
          if (node.document) {
            childrenContainer.appendChild(createDocumentEl(node.document));
          }
          if (node.dataTable) {
            childrenContainer.appendChild(createDataTableEl(node.dataTable));
          }
          for (const child of nodes) {
            childrenContainer.appendChild(createNodeEl(child, 0));
          }
          for (const att of attachments) {
            childrenContainer.appendChild(createAttachmentEl(att, node));
          }
          if (node.message) {
            childrenContainer.appendChild(createMessageEl(node.message));
          }
        }
      });
    } else {
      expander.className = 'expander leaf';
    }

    return li;
  }

  function createDocumentEl(doc) {
    const li = document.createElement('li');
    li.className = 'arg-item';
    const pre = document.createElement('pre');
    pre.textContent = doc.content;
    li.appendChild(pre);
    return li;
  }

  function createDataTableEl(rows) {
    const li = document.createElement('li');
    li.className = 'arg-item';
    const table = document.createElement('table');
    rows.forEach((row, i) => {
      const tr = document.createElement('tr');
      row.forEach(cell => {
        const el = document.createElement(i === 0 ? 'th' : 'td');
        el.textContent = cell;
        tr.appendChild(el);
      });
      table.appendChild(tr);
    });
    li.appendChild(table);
    return li;
  }

  function attachmentIcon(contentType) {
    if (!contentType) return '📎';
    const t = contentType.split(';')[0].trim().toLowerCase();
    if (t.startsWith('image/'))      return '🖼';
    if (t === 'application/json')    return '{}';
    if (t === 'text/html')           return '🌐';
    if (t === 'application/xml' || t === 'text/xml') return '📄';
    if (t.startsWith('text/'))       return '📝';
    if (t === 'application/pdf')     return '📋';
    return '📎';
  }

  function createMessageEl(message) {
    const li = document.createElement('li');
    li.className = 'arg-item';
    const pre = document.createElement('pre');
    pre.style.color = 'var(--vscode-testing-message-error-decorationForeground, #e51400)';
    pre.style.borderColor = 'var(--vscode-testing-message-error-decorationForeground, #e51400)';
    pre.textContent = message;
    li.appendChild(pre);
    return li;
  }

  function createAttachmentEl(att, parentNode) {
    const li = document.createElement('li');
    const row = document.createElement('div');
    row.className = 'node-row';
    row.style.cursor = 'pointer';
    row.style.fontSize = '0.85em';
    row.style.color = 'var(--vscode-descriptionForeground)';

    const expander = document.createElement('span');
    expander.className = 'expander leaf';
    expander.textContent = ' ';

    const label = document.createElement('span');
    label.className = 'node-name';
    label.textContent = 'Attachment: ' + (att.contentType || att.attachmentId);

    row.appendChild(expander);
    row.appendChild(label);
    li.appendChild(row);

    row.addEventListener('click', () => {
      vscode.postMessage({
        type: 'openAttachment',
        executionId: att.executionId,
        executionNodeId: att.executionNodeId,
        attachmentId: att.attachmentId,
        contentType: att.contentType,
      });
    });

    return li;
  }

  function requestChildren(nodeId, id) {
    return new Promise((resolve) => {
      pendingExpand.set(id, resolve);
      vscode.postMessage({ type: 'expand', nodeId, msgId: id });
    });
  }

  window.addEventListener('message', (event) => {
    const msg = event.data;
    if (msg.type === 'init') {
      rootNodeId = msg.node.nodeId;
      const h = msg.header;
      const headerEl = document.getElementById('header');
      headerEl.innerHTML = '';
      const box = document.createElement('div');
      box.className = 'header';

      const left = document.createElement('div');
      left.className = 'header-left';
      const title = document.createElement('div');
      title.className = 'header-title';
      title.textContent = (h.organization && h.project) ? h.organization + ' / ' + h.project : (h.organization || h.project || 'Execution');
      const meta = document.createElement('div');
      meta.className = 'header-meta';
      meta.innerHTML =
        '<span>Plan: ' + h.planId + (h.planCreatedAt ? '  ·  ' + h.planCreatedAt : '') + '</span>' +
        '<span>Execution: ' + h.executionId + '  ·  ' + h.executedAt + '</span>';
      left.appendChild(title);
      left.appendChild(meta);
      box.appendChild(left);

      const result = msg.node.result;
      const badge = document.createElement('div');
      badge.className = 'header-result';
      if (result) {
        const resultColors = {
          PASSED:  'var(--vscode-testing-iconPassed, #388a34)',
          FAILED:  'var(--vscode-testing-iconFailed, #c72e0f)',
          ERROR:   '#c72e0f',
          SKIPPED: 'var(--vscode-disabledForeground, #999)',
        };
        const resultSymbols = { PASSED: '✓', FAILED: '✗', ERROR: '⚠', SKIPPED: '⊘' };
        badge.style.background = resultColors[result] || 'var(--vscode-disabledForeground, #999)';
        badge.textContent = resultSymbols[result] || '✓';
        badge.title = result;
      } else {
        badge.style.background = 'var(--vscode-disabledForeground, #555)';
        badge.innerHTML = '<span class="spin" style="font-size:1.2em">↻</span>';
        badge.id = 'header-badge';
        badge.title = msg.node.status;
      }
      box.appendChild(badge);

      headerEl.appendChild(box);

      const descEl = document.getElementById('description');
      descEl.innerHTML = '';
      if (h.description) {
        const desc = document.createElement('div');
        desc.className = 'project-description';
        desc.textContent = h.description;
        descEl.appendChild(desc);
      }

      const root = document.getElementById('root');
      root.innerHTML = '';
      root.appendChild(createNodeEl(msg.node, 0));
    } else if (msg.type === 'update') {
      const resultColors = {
        PASSED:  'var(--vscode-testing-iconPassed, #388a34)',
        FAILED:  'var(--vscode-testing-iconFailed, #c72e0f)',
        ERROR:   '#c72e0f',
        SKIPPED: 'var(--vscode-disabledForeground, #999)',
      };
      const resultSymbols = { PASSED: '✓', FAILED: '✗', ERROR: '⚠', SKIPPED: '⊘' };
      for (const upd of msg.nodes) {
        nodeStatusMap.set(upd.nodeId, { status: upd.status, result: upd.result });
        const iconEl = statusIconEls.get(upd.nodeId);
        if (iconEl) { applyStatusIcon(iconEl, upd.status, upd.result); }
        if (upd.nodeId === rootNodeId && upd.status === 'FINISHED' && upd.result) {
          const badgeEl = document.getElementById('header-badge');
          if (badgeEl) {
            badgeEl.removeAttribute('id');
            badgeEl.style.background = resultColors[upd.result] || 'var(--vscode-disabledForeground, #999)';
            badgeEl.innerHTML = '';
            badgeEl.textContent = resultSymbols[upd.result] || '✓';
            badgeEl.title = upd.result;
          }
        }
      }
    } else if (msg.type === 'children') {
      const resolve = pendingExpand.get(msg.msgId);
      if (resolve) {
        pendingExpand.delete(msg.msgId);
        resolve({ nodes: msg.nodes, attachments: msg.attachments || [] });
      }
    }
  });

  vscode.postMessage({ type: 'ready' });
</script>
</body>
</html>`;
}
