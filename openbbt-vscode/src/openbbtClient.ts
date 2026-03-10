import { ChildProcessWithoutNullStreams, spawn } from 'child_process';

export interface PlanInfo {
    planId: string;
    projectId: string;
    createdAt: string;
    planNodeRoot: string;
}

export interface NodeInfo {
    nodeId: string;
    nodeType: string | null;
    name: string | null;
    identifier: string | null;
    source: string | null;
    keyword: string | null;
    language: string | null;
    validationStatus: string | null;
    validationMessage: string | null;
    hasIssues: boolean;
    tags: string[];
    properties: Record<string, string>;
    childCount: number;
}

type PendingRequest = {
    resolve: (result: unknown) => void;
    reject: (err: Error) => void;
};

/**
 * Manages an `openbbt serve` subprocess and communicates with it
 * via JSON-RPC 2.0 over stdio with Content-Length framing (same as LSP).
 */
export class OpenBBTClient {

    private process: ChildProcessWithoutNullStreams | undefined;
    private buffer = Buffer.alloc(0);
    private nextId = 1;
    private pending = new Map<number, PendingRequest>();
    private readonly cwd: string;
    private readonly executable: string;
    private readonly log: (msg: string) => void;

    constructor(executable: string, cwd: string, log: (msg: string) => void = () => {}) {
        this.executable = executable;
        this.cwd = cwd;
        this.log = log;
    }

    get connected(): boolean {
        return this.process !== undefined && !this.process.killed;
    }

    connect(): void {
        if (this.connected) {
            return;
        }
        this.log(`[serve] spawning: ${this.executable} serve (cwd=${this.cwd})`);
        const proc = spawn(this.executable, ['serve'], {
            cwd: this.cwd,
            stdio: ['pipe', 'pipe', 'pipe'],
        });
        proc.stdout.on('data', (chunk: Buffer) => this.onData(chunk));
        proc.stderr.on('data', (chunk: Buffer) => {
            this.log(`[serve stderr] ${chunk.toString('utf8').trimEnd()}`);
        });
        proc.on('error', (err) => {
            this.log(`[serve error] ${err.message}`);
            this.process = undefined;
            this.rejectAll(err);
        });
        proc.on('close', (code) => {
            this.log(`[serve] process closed (exit code ${code})`);
            this.process = undefined;
            this.rejectAll(new Error(`openbbt serve process exited (code ${code})`));
        });
        this.process = proc;
    }

    async refresh(): Promise<void> {
        await this.call('refresh', {});
    }

    async listPlans(): Promise<PlanInfo[]> {
        return this.call('browse/plans', {}) as Promise<PlanInfo[]>;
    }

    async getNode(nodeId: string): Promise<NodeInfo> {
        return this.call('browse/node', { nodeId }) as Promise<NodeInfo>;
    }

    async getChildren(nodeId: string): Promise<NodeInfo[]> {
        return this.call('browse/children', { nodeId }) as Promise<NodeInfo[]>;
    }

    async shutdown(): Promise<void> {
        if (!this.connected) {
            return;
        }
        try {
            await this.call('shutdown', {});
        } catch {
            // ignore — process may have already exited
        }
        this.process?.kill();
        this.process = undefined;
    }

    // --- Internal ---

    private call(method: string, params: object): Promise<unknown> {
        if (!this.connected) {
            this.connect();
        }
        return new Promise((resolve, reject) => {
            const id = this.nextId++;
            this.pending.set(id, { resolve, reject });
            const body = JSON.stringify({ jsonrpc: '2.0', id, method, params });
            const header = `Content-Length: ${Buffer.byteLength(body, 'utf8')}\r\n\r\n`;
            this.log(`[serve →] ${method} (id=${id})`);
            this.process!.stdin.write(header + body, 'utf8');
        });
    }

    private onData(chunk: Buffer): void {
        this.buffer = Buffer.concat([this.buffer, chunk]);
        while (true) {
            const msg = this.tryReadMessage();
            if (msg === null) {
                break;
            }
            this.handleMessage(msg);
        }
    }

    private tryReadMessage(): string | null {
        const headerEnd = this.buffer.indexOf('\r\n\r\n');
        if (headerEnd === -1) {
            return null;
        }
        const headerStr = this.buffer.subarray(0, headerEnd).toString('utf8');
        let contentLength = -1;
        for (const line of headerStr.split('\r\n')) {
            if (line.toLowerCase().startsWith('content-length:')) {
                contentLength = parseInt(line.substring(15).trim(), 10);
            }
        }
        if (contentLength < 0) {
            return null;
        }
        const bodyStart = headerEnd + 4;
        if (this.buffer.length < bodyStart + contentLength) {
            return null;
        }
        const body = this.buffer.subarray(bodyStart, bodyStart + contentLength).toString('utf8');
        this.buffer = this.buffer.subarray(bodyStart + contentLength);
        return body;
    }

    private handleMessage(body: string): void {
        let msg: { id?: number; result?: unknown; error?: { code: number; message: string } };
        try {
            msg = JSON.parse(body);
        } catch {
            this.log(`[serve ←] unparseable response: ${body}`);
            return;
        }
        if (msg.id === undefined) {
            return; // notification — ignore
        }
        this.log(`[serve ←] response id=${msg.id} ${msg.error ? 'ERROR: ' + msg.error.message : 'OK'}`);
        const pending = this.pending.get(msg.id);
        if (!pending) {
            return;
        }
        this.pending.delete(msg.id);
        if (msg.error) {
            pending.reject(new Error(`JSON-RPC error ${msg.error.code}: ${msg.error.message}`));
        } else {
            pending.resolve(msg.result);
        }
    }

    private rejectAll(err: Error): void {
        for (const p of this.pending.values()) {
            p.reject(err);
        }
        this.pending.clear();
    }
}
