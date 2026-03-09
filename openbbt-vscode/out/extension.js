"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.activate = activate;
exports.deactivate = deactivate;
const fs = __importStar(require("fs"));
const path = __importStar(require("path"));
const child_process_1 = require("child_process");
const vscode = __importStar(require("vscode"));
const featureFormatter_1 = require("./featureFormatter");
const testPlanProvider_1 = require("./testPlanProvider");
const node_1 = require("vscode-languageclient/node");
let client;
let extensionContext;
let errorNotificationShowing = false;
const OPENBBT_YAML_SKELETON = `project:
  organization: My Organization
  name: My Project
  test-suites:
    - name: default
      description: Default test suite
      tag-expression: ""

plugins:
  - gherkin

configuration:
  core:
    resourceFilter: '**/*.feature'

profiles: {}
`;
const EMPTY_YAML_DIAGNOSTIC = 'openbbt.emptyYaml';
const diagnosticCollection = vscode.languages.createDiagnosticCollection('openbbt');
function updateDiagnostics(document) {
    if (!document.fileName.endsWith('openbbt.yaml')) {
        return;
    }
    if (document.getText().trim() === '') {
        const diagnostic = new vscode.Diagnostic(new vscode.Range(0, 0, 0, 0), 'Empty openbbt.yaml. Generate a skeleton to get started.', vscode.DiagnosticSeverity.Hint);
        diagnostic.code = EMPTY_YAML_DIAGNOSTIC;
        diagnosticCollection.set(document.uri, [diagnostic]);
    }
    else {
        diagnosticCollection.delete(document.uri);
    }
}
class OpenbbtYamlCodeLensProvider {
    provideCodeLenses(document) {
        if (document.getText().trim() === '') {
            return [];
        }
        const text = document.getText();
        const lines = text.split('\n');
        const pluginsLineIndex = lines.findIndex(line => /^plugins\s*:/.test(line));
        if (pluginsLineIndex === -1) {
            return [];
        }
        const range = new vscode.Range(pluginsLineIndex, 0, pluginsLineIndex, 0);
        return [
            new vscode.CodeLens(range, {
                title: '$(cloud-download) Install plugins',
                command: 'openbbt.installPlugins',
                tooltip: 'Run openbbt install to download and install plugins',
            }),
        ];
    }
}
class OpenbbtYamlCodeActionProvider {
    static providedCodeActionKinds = [vscode.CodeActionKind.QuickFix];
    provideCodeActions(document, _range, context) {
        const hasDiagnostic = context.diagnostics.some(d => d.code === EMPTY_YAML_DIAGNOSTIC);
        if (!hasDiagnostic) {
            return [];
        }
        const action = new vscode.CodeAction('Generate OpenBBT skeleton', vscode.CodeActionKind.QuickFix);
        action.edit = new vscode.WorkspaceEdit();
        action.edit.insert(document.uri, new vscode.Position(0, 0), OPENBBT_YAML_SKELETON);
        action.isPreferred = true;
        return [action];
    }
}
async function showConnectionError(message) {
    if (errorNotificationShowing) {
        return;
    }
    errorNotificationShowing = true;
    try {
        const openSettings = 'Open Settings';
        const retry = 'Retry';
        const choice = await vscode.window.showErrorMessage(message, openSettings, retry);
        if (choice === openSettings) {
            await vscode.commands.executeCommand('workbench.action.openSettings', 'openbbt.executablePath');
        }
        else if (choice === retry) {
            await startClient();
        }
    }
    finally {
        errorNotificationShowing = false;
    }
}
function executableExists(command) {
    if (path.isAbsolute(command)) {
        return fs.existsSync(command);
    }
    const which = process.platform === 'win32' ? 'where' : 'which';
    return (0, child_process_1.spawnSync)(which, [command]).status === 0;
}
async function startClient() {
    if (!extensionContext) {
        return;
    }
    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    if (!workspaceFolder) {
        return;
    }
    if (client) {
        try {
            await client.stop();
        }
        catch {
            // client may already be in an error state
        }
        client = undefined;
    }
    const config = vscode.workspace.getConfiguration('openbbt');
    const executable = config.get('executablePath', 'openbbt');
    if (!executableExists(executable)) {
        showConnectionError(`OpenBBT LSP could not connect to '${executable}'. ` +
            `Make sure the CLI is installed or configure the correct path.`);
        return;
    }
    const serverOptions = {
        command: executable,
        args: ['lsp'],
        options: {
            cwd: workspaceFolder.uri.fsPath,
        },
    };
    const clientOptions = {
        documentSelector: [
            { scheme: 'file', language: 'feature' },
            { scheme: 'file', pattern: '**/openbbt.yaml' },
        ],
        workspaceFolder,
        outputChannelName: 'OpenBBT LSP',
        initializationFailedHandler: (_error) => {
            showConnectionError(`OpenBBT LSP could not connect to '${executable}'. ` +
                `Make sure the CLI is installed or configure the correct path.`);
            return false;
        },
        errorHandler: {
            error: () => ({ action: node_1.ErrorAction.Shutdown }),
            closed: () => ({ action: node_1.CloseAction.DoNotRestart }),
        },
    };
    client = new node_1.LanguageClient('openbbt-lsp', 'OpenBBT Language Server', serverOptions, clientOptions);
    await client.start();
    extensionContext.subscriptions.push(client);
}
function activate(context) {
    extensionContext = context;
    startClient();
    vscode.workspace.textDocuments.forEach(updateDiagnostics);
    const testPlanProvider = new testPlanProvider_1.TestPlanProvider();
    vscode.window.registerTreeDataProvider('openbbt.testPlan', testPlanProvider);
    context.subscriptions.push(vscode.commands.registerCommand('openbbt.testPlan.refresh', () => {
        testPlanProvider.refresh();
    }));
    context.subscriptions.push(vscode.commands.registerCommand('openbbt.installPlugins', () => {
        const config = vscode.workspace.getConfiguration('openbbt');
        const executable = config.get('executablePath', 'openbbt');
        const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
        const terminal = vscode.window.createTerminal({
            name: 'OpenBBT Install',
            cwd: workspaceFolder?.uri.fsPath,
        });
        terminal.show();
        terminal.sendText(`${executable} install`);
    }));
    context.subscriptions.push(vscode.workspace.onDidChangeConfiguration((event) => {
        if (event.affectsConfiguration('openbbt.executablePath')) {
            startClient();
        }
    }), vscode.languages.registerCodeLensProvider({ scheme: 'file', pattern: '**/openbbt.yaml' }, new OpenbbtYamlCodeLensProvider()), vscode.languages.registerCodeActionsProvider({ scheme: 'file', pattern: '**/openbbt.yaml' }, new OpenbbtYamlCodeActionProvider(), { providedCodeActionKinds: OpenbbtYamlCodeActionProvider.providedCodeActionKinds }), vscode.workspace.onDidOpenTextDocument(updateDiagnostics), vscode.workspace.onDidChangeTextDocument(e => updateDiagnostics(e.document)), vscode.workspace.onDidCloseTextDocument(doc => diagnosticCollection.delete(doc.uri)), vscode.workspace.onDidSaveTextDocument(doc => {
        if (doc.fileName.endsWith('openbbt.yaml')) {
            startClient();
        }
    }), diagnosticCollection, vscode.languages.registerDocumentFormattingEditProvider({ scheme: 'file', language: 'feature' }, {
        provideDocumentFormattingEdits(document) {
            const formatted = (0, featureFormatter_1.formatFeatureText)(document.getText());
            const full = new vscode.Range(document.positionAt(0), document.positionAt(document.getText().length));
            return [vscode.TextEdit.replace(full, formatted)];
        }
    }));
}
function deactivate() {
    return client?.stop();
}
//# sourceMappingURL=extension.js.map