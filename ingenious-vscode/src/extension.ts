import * as vscode from 'vscode';
import { IngeniousService } from './client/service';
import { RunTaskProvider } from './tasks/runTaskProvider';
import { StatusBar } from './status/statusBar';
import { StepDiagnostics } from './language/diagnostics';
import { registerCommands } from './commands';
import { TestCaseEditorProvider } from './editors/testCaseEditor';
import { ObjectRepoEditorProvider } from './editors/objectRepoEditor';
import { StepCompletionProvider, StepHoverProvider, StepCodeLensProvider } from './language/providers';
import {
  ApiCollectionsTree,
  DataTree,
  ObjectsTree,
  ProjectsTree,
  ReportsTree,
  TestPlanTree,
  TestSetsTree
} from './views/trees';
import { log } from './util/logger';

export async function activate(context: vscode.ExtensionContext): Promise<void> {
  log.info('Activating INGenious extension.');

  const svc = new IngeniousService();
  context.subscriptions.push(svc);
  await svc.init();

  // ---- trees -------------------------------------------------------------
  const projectsTree = new ProjectsTree(svc);
  const testPlanTree = new TestPlanTree(svc, false);
  const reusablesTree = new TestPlanTree(svc, true);
  const testSetsTree = new TestSetsTree(svc);
  const objectsTree = new ObjectsTree(svc);
  const dataTree = new DataTree(svc);
  const apiTree = new ApiCollectionsTree(svc);
  const reportsTree = new ReportsTree(svc);

  context.subscriptions.push(
    vscode.window.registerTreeDataProvider('ingenious.projects', projectsTree),
    vscode.window.registerTreeDataProvider('ingenious.testplan', testPlanTree),
    vscode.window.registerTreeDataProvider('ingenious.reusables', reusablesTree),
    vscode.window.registerTreeDataProvider('ingenious.testsets', testSetsTree),
    vscode.window.registerTreeDataProvider('ingenious.objects', objectsTree),
    vscode.window.registerTreeDataProvider('ingenious.data', dataTree),
    vscode.window.registerTreeDataProvider('ingenious.apicollections', apiTree),
    vscode.window.registerTreeDataProvider('ingenious.reports', reportsTree)
  );

  const refreshTrees = () => {
    projectsTree.refresh();
    testPlanTree.refresh();
    reusablesTree.refresh();
    testSetsTree.refresh();
    objectsTree.refresh();
    dataTree.refresh();
    apiTree.refresh();
    reportsTree.refresh();
  };

  // ---- task provider + status bar + diagnostics --------------------------
  const taskProvider = new RunTaskProvider(svc.cli);
  context.subscriptions.push(vscode.tasks.registerTaskProvider(RunTaskProvider.TYPE, taskProvider));

  const statusBar = new StatusBar(svc);
  context.subscriptions.push(statusBar);
  statusBar.show();

  const diagnostics = new StepDiagnostics(svc);
  diagnostics.register(context);
  context.subscriptions.push(diagnostics);

  // ---- commands ----------------------------------------------------------
  registerCommands({ context, svc, taskProvider, statusBar, diagnostics, refreshTrees });

  // ---- custom editors ----------------------------------------------------
  context.subscriptions.push(
    vscode.window.registerCustomEditorProvider(
      TestCaseEditorProvider.viewType,
      new TestCaseEditorProvider(context, svc),
      { webviewOptions: { retainContextWhenHidden: true }, supportsMultipleEditorsPerDocument: false }
    ),
    vscode.window.registerCustomEditorProvider(
      ObjectRepoEditorProvider.viewType,
      new ObjectRepoEditorProvider(context),
      { webviewOptions: { retainContextWhenHidden: true }, supportsMultipleEditorsPerDocument: false }
    )
  );

  // ---- language features -------------------------------------------------
  const csvSelector: vscode.DocumentSelector = [
    { language: 'csv', scheme: 'file' },
    { pattern: '**/TestPlan/**/*.csv' },
    { pattern: '**/ReusableComponents/**/*.csv' }
  ];
  context.subscriptions.push(
    vscode.languages.registerCompletionItemProvider(csvSelector, new StepCompletionProvider(svc), ',', '@', ':'),
    vscode.languages.registerHoverProvider(csvSelector, new StepHoverProvider(svc)),
    vscode.languages.registerCodeLensProvider(csvSelector, new StepCodeLensProvider())
  );

  // Refresh trees when the MCP server becomes available.
  context.subscriptions.push(svc.mcp.onStatus((s) => {
    if (s === 'connected') {
      refreshTrees();
    }
  }));

  log.info('INGenious extension activated.');
}

export function deactivate(): void {
  log.dispose();
}
