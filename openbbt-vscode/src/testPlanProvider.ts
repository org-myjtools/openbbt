import * as vscode from 'vscode';

export type TestPlanItemKind = 'suite' | 'feature' | 'scenario' | 'scenarioOutline';

export class TestPlanItem extends vscode.TreeItem {
    constructor(
        public readonly label: string,
        public readonly kind: TestPlanItemKind,
        collapsibleState: vscode.TreeItemCollapsibleState,
        public readonly children: TestPlanItem[] = []
    ) {
        super(label, collapsibleState);
        this.contextValue = kind;
        this.iconPath = this.resolveIcon();
        this.tooltip = label;
    }

    private resolveIcon(): vscode.ThemeIcon {
        switch (this.kind) {
            case 'suite':           return new vscode.ThemeIcon('folder-library');
            case 'feature':         return new vscode.ThemeIcon('file-text');
            case 'scenario':        return new vscode.ThemeIcon('circle-outline');
            case 'scenarioOutline': return new vscode.ThemeIcon('symbol-array');
        }
    }
}

const MOCK_PLAN: TestPlanItem[] = [
    new TestPlanItem('Regression Suite', 'suite', vscode.TreeItemCollapsibleState.Expanded, [
        new TestPlanItem('Login', 'feature', vscode.TreeItemCollapsibleState.Expanded, [
            new TestPlanItem('Successful login with valid credentials', 'scenario', vscode.TreeItemCollapsibleState.None),
            new TestPlanItem('Failed login with wrong password', 'scenario', vscode.TreeItemCollapsibleState.None),
            new TestPlanItem('Login with multiple user types', 'scenarioOutline', vscode.TreeItemCollapsibleState.None),
        ]),
        new TestPlanItem('Checkout', 'feature', vscode.TreeItemCollapsibleState.Collapsed, [
            new TestPlanItem('Add item to cart', 'scenario', vscode.TreeItemCollapsibleState.None),
            new TestPlanItem('Remove item from cart', 'scenario', vscode.TreeItemCollapsibleState.None),
            new TestPlanItem('Pay with different payment methods', 'scenarioOutline', vscode.TreeItemCollapsibleState.None),
            new TestPlanItem('Apply discount coupon', 'scenario', vscode.TreeItemCollapsibleState.None),
        ]),
        new TestPlanItem('User Profile', 'feature', vscode.TreeItemCollapsibleState.Collapsed, [
            new TestPlanItem('Update profile information', 'scenario', vscode.TreeItemCollapsibleState.None),
            new TestPlanItem('Change password', 'scenario', vscode.TreeItemCollapsibleState.None),
            new TestPlanItem('Upload avatar with different formats', 'scenarioOutline', vscode.TreeItemCollapsibleState.None),
        ]),
    ]),
    new TestPlanItem('Smoke Suite', 'suite', vscode.TreeItemCollapsibleState.Expanded, [
        new TestPlanItem('Landing Page', 'feature', vscode.TreeItemCollapsibleState.Expanded, [
            new TestPlanItem('Page loads correctly', 'scenario', vscode.TreeItemCollapsibleState.None),
            new TestPlanItem('Navigation links are accessible', 'scenario', vscode.TreeItemCollapsibleState.None),
        ]),
        new TestPlanItem('API Health', 'feature', vscode.TreeItemCollapsibleState.Expanded, [
            new TestPlanItem('Health check endpoint responds 200', 'scenario', vscode.TreeItemCollapsibleState.None),
        ]),
    ]),
];

export class TestPlanProvider implements vscode.TreeDataProvider<TestPlanItem> {
    private readonly _onDidChangeTreeData =
        new vscode.EventEmitter<TestPlanItem | undefined | void>();
    readonly onDidChangeTreeData = this._onDidChangeTreeData.event;

    refresh(): void {
        this._onDidChangeTreeData.fire();
    }

    getTreeItem(element: TestPlanItem): vscode.TreeItem {
        return element;
    }

    getChildren(element?: TestPlanItem): TestPlanItem[] {
        return element ? element.children : MOCK_PLAN;
    }
}
