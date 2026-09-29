import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import { IngeniousService } from '../client/service';
import { RunTaskProvider, IngeniousTaskDefinition } from '../tasks/runTaskProvider';
import { StatusBar } from '../status/statusBar';
import { ReportPanel } from '../webview/reportPanel';
import { CliNotInstalledError } from '../client/cli';
import { testCaseCoords, discoverProjects, ProjectRef } from '../util/paths';
import { testCaseFileCandidates } from '../views/trees';
import { StepDiagnostics } from '../language/diagnostics';
import { log } from '../util/logger';

interface Ctx {
  context: vscode.ExtensionContext;
  svc: IngeniousService;
  taskProvider: RunTaskProvider;
  statusBar: StatusBar;
  diagnostics: StepDiagnostics;
  refreshTrees: () => void;
}

/** Resolve the test case coordinates from an explicit uri or the active editor. */
function resolveCoords(uri?: vscode.Uri): ReturnType<typeof testCaseCoords> {
  const target = uri ?? vscode.window.activeTextEditor?.document.uri;
  if (!target) {
    return undefined;
  }
  return testCaseCoords(target.fsPath);
}

async function runTestCase(
  ctx: Ctx,
  uri: vscode.Uri | undefined,
  opts: { headless?: boolean; rerun?: boolean }
): Promise<void> {
  const coords = resolveCoords(uri);
  if (!coords) {
    vscode.window.showWarningMessage('Open an INGenious test case (under TestPlan/) to run it.');
    return;
  }
  const cfg = vscode.workspace.getConfiguration('ingenious');
  const def: IngeniousTaskDefinition = {
    type: RunTaskProvider.TYPE,
    mode: opts.rerun ? 'rerun' : 'testcase',
    project: coords.project,
    target: `${coords.scenario}/${coords.testcase}`,
    browser: cfg.get<string>('defaultBrowser', 'Chromium'),
    headless: opts.headless ?? cfg.get<boolean>('headless', false)
  };

  const reportTarget = `${coords.scenario}/${coords.testcase}`;
  try {
    const task = ctx.taskProvider.buildTask(def);
    const endSub = vscode.tasks.onDidEndTaskProcess((e) => {
      if (e.execution.task.definition === def || (e.execution.task.name === task.name)) {
        endSub.dispose();
        const ok = e.exitCode === 0;
        ctx.statusBar.setLastRun(`${ok ? '✓' : '✗'} ${coords.testcase}`, ok);
        ctx.refreshTrees();
        if (cfg.get<boolean>('report.openOnRunEnd', true)) {
          ReportPanel.show(ctx.svc, reportTarget).catch((err) => log.error('open report', err));
        }
      }
    });
    await vscode.tasks.executeTask(task);
  } catch (e) {
    handleCliError(e);
  }
}

async function dryRunTestCase(ctx: Ctx, uri?: vscode.Uri): Promise<void> {
  const coords = resolveCoords(uri);
  if (!coords) {
    vscode.window.showWarningMessage('Open an INGenious test case to dry-run it.');
    return;
  }
  // Resolve + count steps via testcase_show (accepts an absolute project arg,
  // so it works regardless of project layout) and report validation findings.
  await vscode.window.withProgress(
    { location: vscode.ProgressLocation.Notification, title: `Dry-run ${coords.testcase}…` },
    async () => {
      try {
        const detail = await ctx.svc.showTestCase(coords.scenario, coords.testcase, coords.project);
        const validation: any = await ctx.svc
          .validateTestCase(coords.scenario, coords.testcase, coords.reusable, coords.project)
          .catch(() => ({}));
        log.main.show(true);
        log.main.appendLine(`=== Dry-run ${coords.scenario}/${coords.testcase} ===`);
        log.main.appendLine(`Format: ${detail.format ?? 'unknown'}`);
        log.main.appendLine(`Steps: ${detail.steps?.length ?? 0}`);
        log.main.appendLine(
          `Validation: ${(validation.errors ?? []).length} error(s), ${(validation.warnings ?? []).length} warning(s)`
        );
      } catch (e) {
        vscode.window.showErrorMessage(`Dry-run failed: ${String(e)}`);
      }
    }
  );
}

async function validateTestCase(ctx: Ctx, uri?: vscode.Uri): Promise<void> {
  const target = uri ?? vscode.window.activeTextEditor?.document.uri;
  if (!target) {
    return;
  }
  const doc = await vscode.workspace.openTextDocument(target);
  await ctx.diagnostics.validate(doc);
  const coords = testCaseCoords(target.fsPath);
  if (coords) {
    try {
      const res: any = await ctx.svc.validateTestCase(coords.scenario, coords.testcase, coords.reusable, coords.project);
      const errs = (res.errors ?? []).length;
      const warns = (res.warnings ?? []).length;
      vscode.window.showInformationMessage(`Validation: ${errs} error(s), ${warns} warning(s).`);
    } catch (e) {
      vscode.window.showErrorMessage(`Validation unavailable: ${String(e)}`);
    }
  }
}

async function openTestCaseFile(projectDir: string, top: string, scenario: string, testcase: string): Promise<void> {
  for (const candidate of testCaseFileCandidates(projectDir, top, scenario, testcase)) {
    if (fs.existsSync(candidate)) {
      const uri = vscode.Uri.file(candidate);
      if (candidate.endsWith('.csv')) {
        await vscode.commands.executeCommand('vscode.openWith', uri, 'ingenious.testCase');
      } else {
        await vscode.window.showTextDocument(uri);
      }
      return;
    }
  }
  vscode.window.showWarningMessage(`Could not find file for ${scenario}/${testcase}.`);
}

function handleCliError(e: unknown): void {
  if (e instanceof CliNotInstalledError) {
    vscode.window
      .showErrorMessage('INGenious CLI not found. Install or configure it.', 'Install / Update CLI', 'Open Settings')
      .then((choice) => {
        if (choice === 'Install / Update CLI') {
          vscode.commands.executeCommand('ingenious.cli.install');
        } else if (choice === 'Open Settings') {
          vscode.commands.executeCommand('workbench.action.openSettings', 'ingenious.cliPath');
        }
      });
  } else {
    vscode.window.showErrorMessage(`INGenious: ${String(e)}`);
  }
}

async function pickProject(svc: IngeniousService): Promise<ProjectRef | undefined> {
  const projects = discoverProjects();
  if (projects.length === 0) {
    vscode.window.showWarningMessage('No INGenious projects found in this workspace.');
    return undefined;
  }
  if (projects.length === 1) {
    return projects[0];
  }
  const picked = await vscode.window.showQuickPick(
    projects.map((p) => ({ label: p.name, description: p.location, p })),
    { placeHolder: 'Select active INGenious project' }
  );
  return picked?.p;
}

export function registerCommands(ctx: Ctx): void {
  const { context, svc } = ctx;
  const reg = (id: string, fn: (...args: any[]) => any) =>
    context.subscriptions.push(vscode.commands.registerCommand(id, fn));

  reg('ingenious.refresh', () => ctx.refreshTrees());

  reg('ingenious.run.testcase', (uri?: vscode.Uri) => runTestCase(ctx, uri, {}));
  reg('ingenious.debug.testcase', (uri?: vscode.Uri) => runTestCase(ctx, uri, { headless: false }));
  reg('ingenious.dryrun.testcase', (uri?: vscode.Uri) => dryRunTestCase(ctx, uri));
  reg('ingenious.run.rerun', (uri?: vscode.Uri) => runTestCase(ctx, uri, { rerun: true }));
  reg('ingenious.validate.testcase', (uri?: vscode.Uri) => validateTestCase(ctx, uri));

  reg('ingenious.run.testset', async (node?: any) => {
    const proj = svc.activeProject;
    if (!proj) {
      return;
    }
    let release = node?.data?.release ?? node?.release;
    let testset = node?.data?.testset ?? node?.testset;
    if (!release || !testset) {
      const value = await vscode.window.showInputBox({ prompt: 'Enter <Release>/<TestSet>' });
      if (!value || !value.includes('/')) {
        return;
      }
      [release, testset] = value.split('/');
    }
    const cfg = vscode.workspace.getConfiguration('ingenious');
    const def: IngeniousTaskDefinition = {
      type: RunTaskProvider.TYPE,
      mode: 'testset',
      project: proj.location,
      target: `${release}/${testset}`,
      browser: cfg.get<string>('defaultBrowser', 'Chromium'),
      headless: cfg.get<boolean>('headless', false),
      parallel: 1
    };
    try {
      await vscode.tasks.executeTask(ctx.taskProvider.buildTask(def));
    } catch (e) {
      handleCliError(e);
    }
  });

  reg('ingenious.run.tags', async () => {
    const tags = await vscode.window.showInputBox({ prompt: 'Tag filter (e.g. @smoke,@api)' });
    if (!tags) {
      return;
    }
    const proj = svc.activeProject;
    const value = await vscode.window.showInputBox({ prompt: 'Test set as <Release>/<TestSet>' });
    if (!proj || !value || !value.includes('/')) {
      return;
    }
    const def: IngeniousTaskDefinition = {
      type: RunTaskProvider.TYPE,
      mode: 'tags',
      project: proj.location,
      target: value,
      tags
    };
    try {
      await vscode.tasks.executeTask(ctx.taskProvider.buildTask(def));
    } catch (e) {
      handleCliError(e);
    }
  });

  reg('ingenious.report.open', (arg?: { target: string }) => {
    const target = arg?.target;
    if (target) {
      ReportPanel.show(svc, target);
    }
  });

  reg('ingenious.report.openLatest', async () => {
    const coords = resolveCoords();
    if (coords) {
      ReportPanel.show(svc, `${coords.scenario}/${coords.testcase}`);
    } else {
      const value = await vscode.window.showInputBox({ prompt: 'Report target <Scenario>/<TestCase>' });
      if (value) {
        ReportPanel.show(svc, value);
      }
    }
  });

  reg('ingenious.project.select', async (p?: ProjectRef) => {
    const chosen = p ?? (await pickProject(svc));
    if (chosen) {
      svc.setActiveProject(chosen);
      // restart MCP in the project's context if the project is not the workspace root
      ctx.refreshTrees();
    }
  });

  reg('ingenious.env.select', async (env?: string) => {
    if (env) {
      svc.setEnvironment(env);
      return;
    }
    const envs = await svc.listEnvironments().catch(() => []);
    const picked = await vscode.window.showQuickPick(envs, { placeHolder: 'Select environment' });
    if (picked) {
      svc.setEnvironment(picked);
    }
  });

  reg('ingenious.mcp.restart', async () => {
    await svc.mcp.restart().catch((e) => vscode.window.showErrorMessage(`MCP restart failed: ${String(e)}`));
    ctx.refreshTrees();
  });

  reg('ingenious.ai.terminal', () => {
    const term = vscode.window.createTerminal({ name: 'INGenious AI', cwd: svc.activeProject?.location });
    const cli = require('../util/paths').resolveCliPath();
    term.sendText(`${cli} ai`);
    term.show();
  });

  reg('ingenious.object.discover', async () => {
    const url = await vscode.window.showInputBox({ prompt: 'URL to discover objects from' });
    if (!url) {
      return;
    }
    const prompt = await vscode.window.showInputBox({ prompt: 'Describe the flow to discover' });
    try {
      const res = await svc.mcp.callTool('ingenious_browser_discover', {
        project: svc.activeProject?.location,
        url,
        prompt: prompt ?? ''
      });
      log.main.show(true);
      log.main.appendLine('Browser discovery session started:');
      log.main.appendLine(JSON.stringify(res, null, 2));
    } catch (e) {
      vscode.window.showErrorMessage(`Discovery failed: ${String(e)}`);
    }
  });

  reg('ingenious.internal.openTestCase', (projectDir: string, top: string, scenario: string, testcase: string) =>
    openTestCaseFile(projectDir, top, scenario, testcase)
  );

  reg('ingenious.cli.doctor', async () => {
    await vscode.window.withProgress(
      { location: vscode.ProgressLocation.Notification, title: 'INGenious Doctor…' },
      async () => {
        try {
          const res = await svc.doctor();
          log.main.show(true);
          log.main.appendLine('=== INGenious Doctor ===');
          log.main.appendLine(JSON.stringify(res, null, 2));
        } catch (e) {
          const ver = await svc.cli.version();
          log.main.show(true);
          log.main.appendLine('=== INGenious Doctor (CLI fallback) ===');
          log.main.appendLine(ver ? `CLI: ${ver}` : `CLI not available: ${String(e)}`);
        }
      }
    );
  });

  reg('ingenious.cli.install', async () => {
    const choice = await vscode.window.showInformationMessage(
      'The INGenious CLI must be installed and on PATH (or set ingenious.cliPath). Build it with `mvn -pl Engine -am package` or download a CLI release.',
      'Open Settings',
      'Open Docs'
    );
    if (choice === 'Open Settings') {
      vscode.commands.executeCommand('workbench.action.openSettings', 'ingenious.cliPath');
    } else if (choice === 'Open Docs') {
      vscode.env.openExternal(vscode.Uri.parse('https://ing-bank.github.io/ingenious-doc/'));
    }
  });

  reg('ingenious.project.open', async () => {
    const uris = await vscode.window.showOpenDialog({ canSelectFolders: true, canSelectFiles: false, openLabel: 'Open Project' });
    if (uris && uris[0]) {
      vscode.commands.executeCommand('vscode.openFolder', uris[0], { forceNewWindow: false });
    }
  });

  reg('ingenious.project.create', async () => {
    const name = await vscode.window.showInputBox({ prompt: 'New project name' });
    if (!name) {
      return;
    }
    const parent =
      vscode.workspace.workspaceFolders?.[0]?.uri.fsPath ??
      (await vscode.window.showOpenDialog({ canSelectFolders: true }))?.[0]?.fsPath;
    if (!parent) {
      return;
    }
    try {
      const res = await svc.mcp.callTool('ingenious_project_create', {
        name,
        parentDir: path.join(parent, 'Projects')
      });
      log.main.appendLine(`Created project: ${JSON.stringify(res)}`);
      ctx.refreshTrees();
      vscode.window.showInformationMessage(`Project '${name}' created.`);
    } catch (e) {
      vscode.window.showErrorMessage(`Create project failed: ${String(e)}`);
    }
  });
}
