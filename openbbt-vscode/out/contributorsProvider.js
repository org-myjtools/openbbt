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
exports.ContributorsProvider = exports.ContributorImplItem = exports.ContributorTypeItem = void 0;
const vscode = __importStar(require("vscode"));
class ContributorTypeItem extends vscode.TreeItem {
    type;
    implementations;
    constructor(type, implementations) {
        super(type, implementations.length > 0
            ? vscode.TreeItemCollapsibleState.Expanded
            : vscode.TreeItemCollapsibleState.None);
        this.type = type;
        this.implementations = implementations;
        this.iconPath = new vscode.ThemeIcon('extensions');
        this.description = `${implementations.length}`;
        this.contextValue = 'contributorType';
    }
}
exports.ContributorTypeItem = ContributorTypeItem;
class ContributorImplItem extends vscode.TreeItem {
    constructor(name) {
        super(name, vscode.TreeItemCollapsibleState.None);
        this.iconPath = new vscode.ThemeIcon('symbol-class');
        this.contextValue = 'contributorImpl';
    }
}
exports.ContributorImplItem = ContributorImplItem;
class ContributorsProvider {
    _onDidChangeTreeData = new vscode.EventEmitter();
    onDidChangeTreeData = this._onDidChangeTreeData.event;
    client;
    contributors = [];
    setClient(client) {
        this.client = client;
        client.onConnected = () => this.refresh();
    }
    async refresh() {
        if (!this.client) {
            return;
        }
        try {
            this.contributors = await this.client.getContributors();
        }
        catch {
            this.contributors = [];
        }
        this._onDidChangeTreeData.fire();
    }
    getTreeItem(element) {
        return element;
    }
    getChildren(element) {
        if (!element) {
            return this.contributors.map(c => new ContributorTypeItem(c.type, c.implementations));
        }
        if (element instanceof ContributorTypeItem) {
            return element.implementations.map(impl => new ContributorImplItem(impl));
        }
        return [];
    }
}
exports.ContributorsProvider = ContributorsProvider;
//# sourceMappingURL=contributorsProvider.js.map