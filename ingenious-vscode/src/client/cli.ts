import * as cp from 'child_process';
import * as vscode from 'vscode';
import { resolveCliPath, cliEnv } from '../util/paths';
import { log } from '../util/logger';

export interface CliResult {
  stdout: string;
  stderr: string;
  code: number;
}

export class CliNotInstalledError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'CliNotInstalledError';
  }
}

/**
 * Thin wrapper around the ingenious launcher. Used for the control plane
 * (running tests, version/doctor checks) — the data plane goes through MCP.
 */
export class CliClient {
  private versionCache: { value: string; ts: number } | undefined;

  /** Spawn the CLI and collect its output. Never uses a shell. */
  run(args: string[], opts: { cwd?: string } = {}): Promise<CliResult> {
    const cli = resolveCliPath();
    const cwd = opts.cwd ?? vscode.workspace.workspaceFolders?.[0]?.uri.fsPath;
    return new Promise((resolve, reject) => {
      let proc: cp.ChildProcess;
      try {
        proc = cp.spawn(cli, args, { cwd, env: cliEnv(), shell: false });
      } catch (e: any) {
        if (e?.code === 'ENOENT') {
          reject(new CliNotInstalledError(`ingenious CLI not found at '${cli}'`));
        } else {
          reject(e);
        }
        return;
      }
      let stdout = '';
      let stderr = '';
      proc.stdout?.on('data', (d) => (stdout += d.toString()));
      proc.stderr?.on('data', (d) => (stderr += d.toString()));
      proc.on('error', (e: any) => {
        if (e?.code === 'ENOENT') {
          reject(new CliNotInstalledError(`ingenious CLI not found at '${cli}'`));
        } else {
          reject(e);
        }
      });
      proc.on('close', (code) => resolve({ stdout, stderr, code: code ?? 0 }));
    });
  }

  /** Build the argv for a `run` invocation. */
  buildRunArgs(opts: {
    target: string;
    browser?: string;
    headless?: boolean;
    parallel?: number;
    tags?: string;
    rerun?: boolean;
  }): string[] {
    const args = ['run', opts.target];
    if (opts.browser) {
      args.push('-b', opts.browser);
    }
    if (opts.headless) {
      args.push('--headless');
    }
    if (opts.parallel && opts.parallel > 1) {
      args.push('--parallel', String(opts.parallel));
    }
    if (opts.tags) {
      args.push('-t', opts.tags);
    }
    if (opts.rerun) {
      args.push('--rerun');
    }
    return args;
  }

  async version(): Promise<string | undefined> {
    if (this.versionCache && Date.now() - this.versionCache.ts < 30000) {
      return this.versionCache.value;
    }
    try {
      const res = await this.run(['--version']);
      const value = (res.stdout + res.stderr).trim();
      this.versionCache = { value, ts: Date.now() };
      return value;
    } catch (e) {
      log.error('CLI version check failed', e);
      return undefined;
    }
  }

  async isInstalled(): Promise<boolean> {
    return (await this.version()) !== undefined;
  }
}
