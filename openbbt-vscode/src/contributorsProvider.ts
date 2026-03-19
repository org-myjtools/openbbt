import * as vscode from 'vscode';
import { ContributorInfo, OpenBBTClient } from './openbbtClient';

export class ContributorTypeItem extends vscode.TreeItem {
    constructor(
        public readonly type: string,
        public readonly implementations: string[]
    ) {
        super(type, implementations.length > 0
            ? vscode.TreeItemCollapsibleState.Expanded
            : vscode.TreeItemCollapsibleState.None);
        this.iconPath = new vscode.ThemeIcon('extensions');
        this.description = `${implementations.length}`;
        this.contextValue = 'contributorType';
    }
}

export class ContributorImplItem extends vscode.TreeItem {
    constructor(name: string) {
        super(name, vscode.TreeItemCollapsibleState.None);
        this.iconPath = new vscode.ThemeIcon('symbol-class');
        this.contextValue = 'contributorImpl';
    }
}

type TreeItem = ContributorTypeItem | ContributorImplItem;

export class ContributorsProvider implements vscode.TreeDataProvider<TreeItem> {

    private readonly _onDidChangeTreeData = new vscode.EventEmitter<TreeItem | undefined | void>();
    readonly onDidChangeTreeData = this._onDidChangeTreeData.event;

    private client: OpenBBTClient | undefined;
    private contributors: ContributorInfo[] = [];

    setClient(client: OpenBBTClient): void {
        this.client = client;
        client.onConnected = () => this.refresh();
    }

    async refresh(): Promise<void> {
        if (!this.client) {
            return;
        }
        try {
            this.contributors = await this.client.getContributors();
        } catch {
            this.contributors = [];
        }
        this._onDidChangeTreeData.fire();
    }

    getTreeItem(element: TreeItem): vscode.TreeItem {
        return element;
    }

    getChildren(element?: TreeItem): TreeItem[] {
        if (!element) {
            return this.contributors.map(c => new ContributorTypeItem(c.type, c.implementations));
        }
        if (element instanceof ContributorTypeItem) {
            return element.implementations.map(impl => new ContributorImplItem(impl));
        }
        return [];
    }
}