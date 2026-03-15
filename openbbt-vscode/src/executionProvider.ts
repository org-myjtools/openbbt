import * as fs from 'fs';
import * as path from 'path';
import * as vscode from 'vscode';
import { ExecutionListItem, OpenBBTClient, PlanListItem } from './openbbtClient';
import { ISSUE_URI_SCHEME } from './testPlanProvider';

// ---------------------------------------------------------------------------
// openbbt.yaml reader
// ---------------------------------------------------------------------------

function readProjectInfo(workspacePath: string): { organization: string; projectName: string } {
    const defaults = { organization: 'Unknown Organization', projectName: 'Unknown Project' };
    try {
        const yamlPath = path.join(workspacePath, 'openbbt.yaml');
        const content = fs.readFileSync(yamlPath, 'utf8');
        let inProjectSection = false;
        let organization = defaults.organization;
        let projectName = defaults.projectName;
        for (const raw of content.split('\n')) {
            const line = raw.trimEnd();
            if (/^project\s*:/.test(line)) {
                inProjectSection = true;
                continue;
            }
            if (inProjectSection && /^\S/.test(line) && line.trim() !== '') {
                inProjectSection = false;
            }
            if (inProjectSection) {
                const orgMatch = line.match(/^\s+organization\s*:\s*(.+)/);
                if (orgMatch) { organization = orgMatch[1].trim().replace(/^['"]|['"]$/g, ''); }
                const nameMatch = line.match(/^\s+name\s*:\s*(.+)/);
                if (nameMatch) { projectName = nameMatch[1].trim().replace(/^['"]|['"]$/g, ''); }
            }
        }
        return { organization, projectName };
    } catch {
        return defaults;
    }
}

// ---------------------------------------------------------------------------
// Tree item
// ---------------------------------------------------------------------------

type ExecutionItemKind = 'project' | 'plan' | 'execution';

export class ExecutionItem extends vscode.TreeItem {
    constructor(
        public readonly kind: ExecutionItemKind,
        label: string,
        collapsibleState: vscode.TreeItemCollapsibleState,
        public readonly planId?: string,
        public readonly execution?: ExecutionListItem,
        description?: string,
    ) {
        super(label, collapsibleState);
        this.description = description;
        this.iconPath = resolveIcon(kind, execution?.result);
        this.tooltip = label;
        this.contextValue = kind;
        if (kind === 'plan' && description !== undefined) {
            this.resourceUri = vscode.Uri.parse(`${ISSUE_URI_SCHEME}://${planId}`);
        }
        if (kind === 'execution' && execution) {
            this.command = {
                command: 'openbbt.executions.openDetail',
                title: 'Open Execution Detail',
                arguments: [execution],
            };
        }
    }
}

function resolveIcon(kind: ExecutionItemKind, result?: string): vscode.ThemeIcon {
    switch (kind) {
        case 'project':   return new vscode.ThemeIcon('organization');
        case 'plan':      return new vscode.ThemeIcon('list-tree');
        case 'execution': return executionIcon(result);
    }
}

function executionIcon(result?: string): vscode.ThemeIcon {
    switch (result) {
        case 'PASSED':  return new vscode.ThemeIcon('pass',           new vscode.ThemeColor('testing.iconPassed'));
        case 'FAILED':  return new vscode.ThemeIcon('error',          new vscode.ThemeColor('testing.iconFailed'));
        case 'ERROR':   return new vscode.ThemeIcon('warning',        new vscode.ThemeColor('testing.iconErrored'));
        case 'SKIPPED': return new vscode.ThemeIcon('debug-step-over',new vscode.ThemeColor('testing.iconSkipped'));
        default:        return new vscode.ThemeIcon('run-all');
    }
}

function formatDate(iso: string): string {
    const d = new Date(iso);
    const p = (n: number) => String(n).padStart(2, '0');
    return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

// ---------------------------------------------------------------------------
// Provider
// ---------------------------------------------------------------------------

export class ExecutionProvider implements vscode.TreeDataProvider<ExecutionItem> {

    private readonly _onDidChangeTreeData = new vscode.EventEmitter<ExecutionItem | undefined | void>();
    readonly onDidChangeTreeData = this._onDidChangeTreeData.event;

    private client: OpenBBTClient | undefined;

    constructor(private readonly workspacePath: string | undefined) {}

    setClient(client: OpenBBTClient): void {
        this.client = client;
    }

    refresh(): void {
        this._onDidChangeTreeData.fire();
    }

    getTreeItem(element: ExecutionItem): vscode.TreeItem {
        return element;
    }

    async getChildren(element?: ExecutionItem): Promise<ExecutionItem[]> {
        if (!element) {
            return [this.projectItem()];
        }
        switch (element.kind) {
            case 'project':   return this.loadPlans();
            case 'plan':      return this.loadExecutions(element.planId!);
            case 'execution': return [];
        }
    }

    // --- Private ---

    private projectItem(): ExecutionItem {
        const { organization, projectName } = this.workspacePath
            ? readProjectInfo(this.workspacePath)
            : { organization: 'Unknown Organization', projectName: 'Unknown Project' };
        return new ExecutionItem(
            'project',
            `${organization} / ${projectName}`,
            vscode.TreeItemCollapsibleState.Expanded,
        );
    }

    private async loadPlans(): Promise<ExecutionItem[]> {
        if (!this.client) {
            return [];
        }
        const { organization, projectName } = this.workspacePath
            ? readProjectInfo(this.workspacePath)
            : { organization: '', projectName: '' };
        try {
            const plans: PlanListItem[] = await this.client.listPlansByProject(organization, projectName);
            return plans.map(plan => new ExecutionItem(
                'plan',
                formatDate(plan.createdAt),
                vscode.TreeItemCollapsibleState.Collapsed,
                plan.planId,
                undefined,
                plan.hasIssues ? '⚠ issues' : undefined,
            ));
        } catch {
            return [];
        }
    }

    private async loadExecutions(planId: string): Promise<ExecutionItem[]> {
        if (!this.client) {
            return [];
        }
        try {
            const executions: ExecutionListItem[] = await this.client.listExecutionsByPlan(planId);
            return executions.map(ex => new ExecutionItem(
                'execution',
                formatDate(ex.executedAt),
                vscode.TreeItemCollapsibleState.None,
                undefined,
                ex,
            ));
        } catch {
            return [];
        }
    }
}
