import * as vscode from 'vscode';

/**
 * Centralised output channels. Keeps the three logical streams the plan calls
 * for (general, MCP, run) separate so users can triage quickly.
 */
class Loggers {
  private channels = new Map<string, vscode.OutputChannel>();

  private get(name: string): vscode.OutputChannel {
    let ch = this.channels.get(name);
    if (!ch) {
      ch = vscode.window.createOutputChannel(name);
      this.channels.set(name, ch);
    }
    return ch;
  }

  get main(): vscode.OutputChannel {
    return this.get('INGenious');
  }

  get mcp(): vscode.OutputChannel {
    return this.get('INGenious – MCP');
  }

  get run(): vscode.OutputChannel {
    return this.get('INGenious – Run');
  }

  info(msg: string): void {
    this.main.appendLine(`[info] ${msg}`);
  }

  warn(msg: string): void {
    this.main.appendLine(`[warn] ${msg}`);
  }

  error(msg: string, err?: unknown): void {
    this.main.appendLine(`[error] ${msg}${err ? ' — ' + String(err) : ''}`);
  }

  dispose(): void {
    for (const ch of this.channels.values()) {
      ch.dispose();
    }
    this.channels.clear();
  }
}

export const log = new Loggers();
