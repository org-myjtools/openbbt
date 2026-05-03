import * as vscode from 'vscode';
import { OpenBBTClient } from './openbbtClient';

// Step-line prefixes across supported locales (lowercase for matching)
const STEP_PREFIXES = [
    'given ', 'when ', 'then ', 'and ', 'but ', '* ',
    'dado que ', 'dado ', 'cuando ', 'entonces ', 'y ', 'pero ',
];

function localeFromDocument(doc: vscode.TextDocument): string {
    for (let i = 0; i < Math.min(5, doc.lineCount); i++) {
        const m = doc.lineAt(i).text.trim().match(/^#\s*language\s*:\s*(\w+)/i);
        if (m) { return m[1]; }
    }
    return 'en';
}

function isOnStepLine(lineText: string): boolean {
    const lower = lineText.trimStart().toLowerCase();
    return STEP_PREFIXES.some(p => lower.startsWith(p));
}

export class AiCompletionProvider implements vscode.InlineCompletionItemProvider {

    constructor(
        private readonly getClient: () => OpenBBTClient | undefined,
        private readonly statusBar: vscode.StatusBarItem
    ) {}

    async provideInlineCompletionItems(
        document: vscode.TextDocument,
        position: vscode.Position,
        _context: vscode.InlineCompletionContext,
        token: vscode.CancellationToken
    ): Promise<vscode.InlineCompletionList | undefined> {
        const config = vscode.workspace.getConfiguration('openbbt.ai');
        if (!config.get<boolean>('enabled', false)) { return; }
        const apiKey = config.get<string>('apiKey', '');
        if (!apiKey) { return; }

        if (!isOnStepLine(document.lineAt(position.line).text)) { return; }

        const locale = localeFromDocument(document);
        const prefix = document.getText(new vscode.Range(new vscode.Position(0, 0), position));

        let stepsIndex = '[]';
        const client = this.getClient();
        if (client) {
            try {
                stepsIndex = await client.getStepsIndex();
            } catch {
                // serve not running or steps/index not available — proceed without index
            }
        }

        if (token.isCancellationRequested) { return; }

        const model = config.get<string>('model', 'claude-haiku-4-5-20251001');
        this.statusBar.text = '$(loading~spin) OpenBBT AI';
        this.statusBar.show();
        try {
            const completion = await this.callClaude(apiKey, model, locale, stepsIndex, prefix, token);
            if (!completion || token.isCancellationRequested) { return; }

            const endOfLine = document.lineAt(position.line).range.end;
            return new vscode.InlineCompletionList([
                new vscode.InlineCompletionItem(
                    completion,
                    new vscode.Range(position, endOfLine)
                )
            ]);
        } finally {
            this.statusBar.hide();
        }
    }

    private async callClaude(
        apiKey: string,
        model: string,
        locale: string,
        stepsIndex: string,
        prefix: string,
        token: vscode.CancellationToken
    ): Promise<string | undefined> {
        const systemText =
            `You are an assistant for writing BDD tests in Gherkin.\n` +
            `Active language: ${locale}\n` +
            `Available steps in this project:\n<steps>\n${stepsIndex}\n</steps>\n` +
            `Suggest the text that completes the current step from the cursor position onward.\n` +
            `Return ONLY the completion text. No explanations, no quotes.\n` +
            `If no step fits, return an empty string.`;

        const body = JSON.stringify({
            model,
            max_tokens: 120,
            system: [{ type: 'text', text: systemText, cache_control: { type: 'ephemeral' } }],
            messages: [{ role: 'user', content: prefix }],
        });

        const controller = new AbortController();
        const cancel = token.onCancellationRequested(() => controller.abort());
        try {
            const response = await fetch('https://api.anthropic.com/v1/messages', {
                method: 'POST',
                headers: {
                    'x-api-key': apiKey,
                    'anthropic-version': '2023-06-01',
                    'anthropic-beta': 'prompt-caching-2024-07-31',
                    'content-type': 'application/json',
                },
                body,
                signal: controller.signal,
            });
            if (!response.ok) { return undefined; }
            const data = await response.json() as { content?: { type: string; text: string }[] };
            const text = data.content?.[0]?.text?.trim();
            return text || undefined;
        } catch {
            return undefined;
        } finally {
            cancel.dispose();
        }
    }
}