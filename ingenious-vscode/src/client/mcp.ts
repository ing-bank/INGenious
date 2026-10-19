import * as cp from 'child_process';
import * as vscode from 'vscode';
import { log } from '../util/logger';
import { resolveCliPath, cliEnv } from '../util/paths';

type Pending = {
  resolve: (v: any) => void;
  reject: (e: Error) => void;
  timer: NodeJS.Timeout;
};

export type McpStatus = 'stopped' | 'starting' | 'connected' | 'error';

/**
 * Newline-delimited JSON-RPC 2.0 client for the INGenious MCP server
 * (`ingenious server mcp`). Used as the primary machine-readable data channel
 * for tree views and language features.
 */
export class McpClient {
  private proc: cp.ChildProcessWithoutNullStreams | undefined;
  private buffer = '';
  private nextId = 1;
  private pending = new Map<number, Pending>();
  private _status: McpStatus = 'stopped';
  private restarts = 0;
  private disposed = false;
  private startPromise: Promise<void> | undefined;

  private readonly _onStatus = new vscode.EventEmitter<McpStatus>();
  readonly onStatus = this._onStatus.event;

  get status(): McpStatus {
    return this._status;
  }

  private setStatus(s: McpStatus): void {
    this._status = s;
    this._onStatus.fire(s);
  }

  /** Start the server (idempotent). */
  async start(cwd?: string): Promise<void> {
    if (this._status === 'connected' || this._status === 'starting') {
      return this.startPromise ?? Promise.resolve();
    }
    this.startPromise = this.doStart(cwd);
    return this.startPromise;
  }

  private async doStart(cwd?: string): Promise<void> {
    this.setStatus('starting');
    const cli = resolveCliPath();
    const workDir =
      cwd ?? vscode.workspace.workspaceFolders?.[0]?.uri.fsPath ?? process.cwd();
    log.mcp.appendLine(`Starting MCP server: ${cli} server mcp (cwd=${workDir})`);

    try {
      this.proc = cp.spawn(cli, ['server', 'mcp'], {
        cwd: workDir,
        env: cliEnv(),
        shell: false
      }) as cp.ChildProcessWithoutNullStreams;
    } catch (e) {
      this.setStatus('error');
      throw new Error(`Failed to spawn MCP server: ${String(e)}`);
    }

    this.proc.stdout.setEncoding('utf8');
    this.proc.stdout.on('data', (d: string) => this.onData(d));
    this.proc.stderr.setEncoding('utf8');
    this.proc.stderr.on('data', (d: string) => log.mcp.append(d));
    this.proc.on('error', (e) => {
      log.mcp.appendLine(`MCP process error: ${String(e)}`);
      this.setStatus('error');
    });
    this.proc.on('exit', (code) => {
      log.mcp.appendLine(`MCP server exited (code ${code})`);
      this.failAllPending(new Error('MCP server exited'));
      this.proc = undefined;
      if (!this.disposed) {
        this.setStatus('stopped');
        this.scheduleRestart(workDir);
      }
    });

    // MCP initialize handshake.
    await this.request('initialize', {
      protocolVersion: '2024-11-05',
      capabilities: {},
      clientInfo: { name: 'ingenious-vscode', version: '0.1.0' }
    });
    this.notify('notifications/initialized', {});
    this.restarts = 0;
    this.setStatus('connected');
    log.mcp.appendLine('MCP server connected.');
  }

  private scheduleRestart(cwd: string): void {
    const cfg = vscode.workspace.getConfiguration('ingenious');
    if (!cfg.get<boolean>('mcp.autoStart', true)) {
      return;
    }
    if (this.restarts >= 5) {
      log.mcp.appendLine('MCP server gave up after 5 restart attempts.');
      this.setStatus('error');
      return;
    }
    const delay = Math.min(1000 * 2 ** this.restarts, 15000);
    this.restarts++;
    this.startPromise = undefined;
    setTimeout(() => {
      if (!this.disposed) {
        this.start(cwd).catch((e) => log.mcp.appendLine(String(e)));
      }
    }, delay);
  }

  private onData(chunk: string): void {
    this.buffer += chunk;
    let idx: number;
    while ((idx = this.buffer.indexOf('\n')) >= 0) {
      const line = this.buffer.slice(0, idx).trim();
      this.buffer = this.buffer.slice(idx + 1);
      if (!line) {
        continue;
      }
      this.handleLine(line);
    }
  }

  private handleLine(line: string): void {
    let msg: any;
    try {
      msg = JSON.parse(line);
    } catch {
      // Not JSON — server diagnostic that leaked to stdout; ignore.
      return;
    }
    if (typeof msg.id === 'number' && this.pending.has(msg.id)) {
      const p = this.pending.get(msg.id)!;
      this.pending.delete(msg.id);
      clearTimeout(p.timer);
      if (msg.error) {
        p.reject(new Error(msg.error.message || 'MCP error'));
      } else {
        p.resolve(msg.result);
      }
    }
  }

  private failAllPending(err: Error): void {
    for (const p of this.pending.values()) {
      clearTimeout(p.timer);
      p.reject(err);
    }
    this.pending.clear();
  }

  private send(obj: any): void {
    if (!this.proc) {
      throw new Error('MCP server not running');
    }
    this.proc.stdin.write(JSON.stringify(obj) + '\n');
  }

  private notify(method: string, params: any): void {
    try {
      this.send({ jsonrpc: '2.0', method, params });
    } catch (e) {
      log.mcp.appendLine(`notify failed: ${String(e)}`);
    }
  }

  private request(method: string, params: any, timeoutMs = 60000): Promise<any> {
    const id = this.nextId++;
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.pending.delete(id);
        reject(new Error(`MCP request '${method}' timed out`));
      }, timeoutMs);
      this.pending.set(id, { resolve, reject, timer });
      try {
        this.send({ jsonrpc: '2.0', id, method, params });
      } catch (e) {
        clearTimeout(timer);
        this.pending.delete(id);
        reject(e as Error);
      }
    });
  }

  /**
   * Call an ingenious_* tool and return its structured payload
   * (result.structuredContent, falling back to parsing the text content block).
   */
  async callTool<T = any>(name: string, args: Record<string, unknown> = {}, timeoutMs = 120000): Promise<T> {
    if (this._status !== 'connected') {
      await this.start();
    }
    const result = await this.request('tools/call', { name, arguments: args }, timeoutMs);
    if (result && result.structuredContent !== undefined) {
      return result.structuredContent as T;
    }
    const text: string | undefined = result?.content?.[0]?.text;
    if (text) {
      try {
        return JSON.parse(text) as T;
      } catch {
        return text as unknown as T;
      }
    }
    return result as T;
  }

  async listTools(): Promise<string[]> {
    if (this._status !== 'connected') {
      await this.start();
    }
    const res = await this.request('tools/list', {});
    return (res?.tools ?? []).map((t: any) => t.name);
  }

  async restart(): Promise<void> {
    this.stop();
    this.restarts = 0;
    this.startPromise = undefined;
    await this.start();
  }

  stop(): void {
    if (this.proc) {
      try {
        this.notify('shutdown', {});
      } catch {
        /* ignore */
      }
      this.proc.kill();
      this.proc = undefined;
    }
    this.failAllPending(new Error('MCP server stopped'));
    this.setStatus('stopped');
  }

  dispose(): void {
    this.disposed = true;
    this.stop();
    this._onStatus.dispose();
  }
}
