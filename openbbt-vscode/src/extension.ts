import * as fs from 'fs';
import * as path from 'path';
import { spawnSync } from 'child_process';
import * as vscode from 'vscode';
import { formatFeatureText } from './featureFormatter';
import {
    CloseAction,
    ErrorAction,
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
} from 'vscode-languageclient/node';

let client: LanguageClient | undefined;
let extensionContext: vscode.ExtensionContext | undefined;
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

function updateDiagnostics(document: vscode.TextDocument): void {
    if (!document.fileName.endsWith('openbbt.yaml')) {
        return;
    }
    if (document.getText().trim() === '') {
        const diagnostic = new vscode.Diagnostic(
            new vscode.Range(0, 0, 0, 0),
            'Empty openbbt.yaml. Generate a skeleton to get started.',
            vscode.DiagnosticSeverity.Hint
        );
        diagnostic.code = EMPTY_YAML_DIAGNOSTIC;
        diagnosticCollection.set(document.uri, [diagnostic]);
    } else {
        diagnosticCollection.delete(document.uri);
    }
}

class OpenbbtYamlCodeLensProvider implements vscode.CodeLensProvider {
    provideCodeLenses(document: vscode.TextDocument): vscode.CodeLens[] {
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

class OpenbbtYamlCodeActionProvider implements vscode.CodeActionProvider {
    static readonly providedCodeActionKinds = [vscode.CodeActionKind.QuickFix];

    provideCodeActions(
        document: vscode.TextDocument,
        _range: vscode.Range,
        context: vscode.CodeActionContext
    ): vscode.CodeAction[] {
        const hasDiagnostic = context.diagnostics.some(d => d.code === EMPTY_YAML_DIAGNOSTIC);
        if (!hasDiagnostic) {
            return [];
        }
        const action = new vscode.CodeAction(
            'Generate OpenBBT skeleton',
            vscode.CodeActionKind.QuickFix
        );
        action.edit = new vscode.WorkspaceEdit();
        action.edit.insert(document.uri, new vscode.Position(0, 0), OPENBBT_YAML_SKELETON);
        action.isPreferred = true;
        return [action];
    }
}

async function showConnectionError(message: string): Promise<void> {
    if (errorNotificationShowing) {
        return;
    }
    errorNotificationShowing = true;
    try {
        const openSettings = 'Open Settings';
        const retry = 'Retry';
        const choice = await vscode.window.showErrorMessage(message, openSettings, retry);
        if (choice === openSettings) {
            await vscode.commands.executeCommand(
                'workbench.action.openSettings',
                'openbbt.executablePath'
            );
        } else if (choice === retry) {
            await startClient();
        }
    } finally {
        errorNotificationShowing = false;
    }
}

function executableExists(command: string): boolean {
    if (path.isAbsolute(command)) {
        return fs.existsSync(command);
    }
    const which = process.platform === 'win32' ? 'where' : 'which';
    return spawnSync(which, [command]).status === 0;
}

async function startClient(): Promise<void> {
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
        } catch {
            // client may already be in an error state
        }
        client = undefined;
    }

    const config = vscode.workspace.getConfiguration('openbbt');
    const executable = config.get<string>('executablePath', 'openbbt');

    if (!executableExists(executable)) {
        showConnectionError(
            `OpenBBT LSP could not connect to '${executable}'. ` +
            `Make sure the CLI is installed or configure the correct path.`
        );
        return;
    }

    const serverOptions: ServerOptions = {
        command: executable,
        args: ['lsp'],
        options: {
            cwd: workspaceFolder.uri.fsPath,
        },
    };

    const clientOptions: LanguageClientOptions = {
        documentSelector: [
            { scheme: 'file', language: 'feature' },
            { scheme: 'file', pattern: '**/openbbt.yaml' },
        ],
        workspaceFolder,
        outputChannelName: 'OpenBBT LSP',
        initializationFailedHandler: (_error) => {
            showConnectionError(
                `OpenBBT LSP could not connect to '${executable}'. ` +
                `Make sure the CLI is installed or configure the correct path.`
            );
            return false;
        },
        errorHandler: {
            error: () => ({ action: ErrorAction.Shutdown }),
            closed: () => ({ action: CloseAction.DoNotRestart }),
        },
    };

    client = new LanguageClient(
        'openbbt-lsp',
        'OpenBBT Language Server',
        serverOptions,
        clientOptions
    );

    await client.start();
    extensionContext.subscriptions.push(client);
}

export function activate(context: vscode.ExtensionContext): void {
    extensionContext = context;
    startClient();
    vscode.workspace.textDocuments.forEach(updateDiagnostics);

    context.subscriptions.push(
        vscode.commands.registerCommand('openbbt.installPlugins', () => {
            const config = vscode.workspace.getConfiguration('openbbt');
            const executable = config.get<string>('executablePath', 'openbbt');
            const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
            const terminal = vscode.window.createTerminal({
                name: 'OpenBBT Install',
                cwd: workspaceFolder?.uri.fsPath,
            });
            terminal.show();
            terminal.sendText(`${executable} install`);
        })
    );

    context.subscriptions.push(
        vscode.workspace.onDidChangeConfiguration((event) => {
            if (event.affectsConfiguration('openbbt.executablePath')) {
                startClient();
            }
        }),
        vscode.languages.registerCodeLensProvider(
            { scheme: 'file', pattern: '**/openbbt.yaml' },
            new OpenbbtYamlCodeLensProvider()
        ),
        vscode.languages.registerCodeActionsProvider(
            { scheme: 'file', pattern: '**/openbbt.yaml' },
            new OpenbbtYamlCodeActionProvider(),
            { providedCodeActionKinds: OpenbbtYamlCodeActionProvider.providedCodeActionKinds }
        ),
        vscode.workspace.onDidOpenTextDocument(updateDiagnostics),
        vscode.workspace.onDidChangeTextDocument(e => updateDiagnostics(e.document)),
        vscode.workspace.onDidCloseTextDocument(doc => diagnosticCollection.delete(doc.uri)),
        vscode.workspace.onDidSaveTextDocument(doc => {
            if (doc.fileName.endsWith('openbbt.yaml')) {
                startClient();
            }
        }),
        diagnosticCollection,
        vscode.languages.registerDocumentFormattingEditProvider(
            { scheme: 'file', language: 'feature' },
            {
                provideDocumentFormattingEdits(document): vscode.TextEdit[] {
                    const formatted = formatFeatureText(document.getText());
                    const full = new vscode.Range(
                        document.positionAt(0),
                        document.positionAt(document.getText().length)
                    );
                    return [vscode.TextEdit.replace(full, formatted)];
                }
            }
        )
    );
}

export function deactivate(): Thenable<void> | undefined {
    return client?.stop();
}
