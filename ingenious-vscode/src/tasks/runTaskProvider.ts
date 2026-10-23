import * as vscode from 'vscode';
import { CliClient } from '../client/cli';
import { resolveCliPath } from '../util/paths';

export interface IngeniousTaskDefinition extends vscode.TaskDefinition {
  mode: 'testcase' | 'testset' | 'tags' | 'rerun';
  target?: string;
  project?: string;
  browser?: string;
  headless?: boolean;
  parallel?: number;
  env?: string;
  tags?: string;
}

/**
 * Contributes `ingenious` tasks that shell out to `ingenious run …`. Output is
 * shown in the terminal; structured results come from the run commands (which
 * use the MCP run tools).
 */
export class RunTaskProvider implements vscode.TaskProvider {
  static readonly TYPE = 'ingenious';

  constructor(private readonly cli: CliClient) {}

  provideTasks(): vscode.Task[] {
    return [];
  }

  resolveTask(task: vscode.Task): vscode.Task | undefined {
    const def = task.definition as IngeniousTaskDefinition;
    if (def.type !== RunTaskProvider.TYPE || !def.target) {
      return undefined;
    }
    return this.buildTask(def);
  }

  buildTask(def: IngeniousTaskDefinition): vscode.Task {
    const args = this.cli.buildRunArgs({
      target: def.target ?? '',
      browser: def.browser,
      headless: def.headless,
      parallel: def.parallel,
      tags: def.tags,
      rerun: def.mode === 'rerun'
    });
    const exec = new vscode.ProcessExecution(resolveCliPath(), args, {
      cwd: vscode.workspace.workspaceFolders?.[0]?.uri.fsPath
    });
    const task = new vscode.Task(
      def,
      vscode.TaskScope.Workspace,
      `run ${def.target}`,
      RunTaskProvider.TYPE,
      exec,
      []
    );
    task.presentationOptions = {
      reveal: vscode.TaskRevealKind.Always,
      panel: vscode.TaskPanelKind.Dedicated,
      clear: true
    };
    return task;
  }
}
