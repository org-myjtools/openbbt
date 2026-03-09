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
exports.TestPlanProvider = exports.TestPlanItem = void 0;
const vscode = __importStar(require("vscode"));
class TestPlanItem extends vscode.TreeItem {
    label;
    kind;
    children;
    constructor(label, kind, collapsibleState, children = []) {
        super(label, collapsibleState);
        this.label = label;
        this.kind = kind;
        this.children = children;
        this.contextValue = kind;
        this.iconPath = this.resolveIcon();
        this.tooltip = label;
    }
    resolveIcon() {
        switch (this.kind) {
            case 'suite': return new vscode.ThemeIcon('folder-library');
            case 'feature': return new vscode.ThemeIcon('file-text');
            case 'scenario': return new vscode.ThemeIcon('circle-outline');
            case 'scenarioOutline': return new vscode.ThemeIcon('symbol-array');
        }
    }
}
exports.TestPlanItem = TestPlanItem;
const MOCK_PLAN = [
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
class TestPlanProvider {
    _onDidChangeTreeData = new vscode.EventEmitter();
    onDidChangeTreeData = this._onDidChangeTreeData.event;
    refresh() {
        this._onDidChangeTreeData.fire();
    }
    getTreeItem(element) {
        return element;
    }
    getChildren(element) {
        return element ? element.children : MOCK_PLAN;
    }
}
exports.TestPlanProvider = TestPlanProvider;
//# sourceMappingURL=testPlanProvider.js.map