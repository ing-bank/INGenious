import * as vscode from 'vscode';
import { IngeniousService } from '../client/service';

/** The five status-bar items described in the plan. */
export class StatusBar implements vscode.Disposable {
  private readonly brand: vscode.StatusBarItem;
  private readonly project: vscode.StatusBarItem;
  private readonly env: vscode.StatusBarItem;
  private readonly last: vscode.StatusBarItem;
  private readonly mcp: vscode.StatusBarItem;

  constructor(private readonly svc: IngeniousService) {
    const A = vscode.StatusBarAlignment.Left;
    this.brand = vscode.window.createStatusBarItem(A, 100);
    this.project = vscode.window.createStatusBarItem(A, 99);
    this.env = vscode.window.createStatusBarItem(A, 98);
    this.last = vscode.window.createStatusBarItem(A, 97);
    this.mcp = vscode.window.createStatusBarItem(A, 96);

    this.brand.text = '$(circle-large-outline) INGenious';
    this.brand.command = 'ingenious.cli.doctor';
    this.brand.tooltip = 'INGenious — click for Doctor';

    this.project.command = 'ingenious.project.select';
    this.env.command = 'ingenious.env.select';
    this.last.command = 'ingenious.report.openLatest';
    this.mcp.command = 'ingenious.mcp.restart';

    svc.onDidChange(() => this.update());
    svc.mcp.onStatus(() => this.update());
  }

  show(): void {
    this.brand.show();
    this.project.show();
    this.env.show();
    this.mcp.show();
    this.update();
  }

  setLastRun(text: string, ok: boolean): void {
    this.last.text = `$(beaker) ${text}`;
    this.last.color = ok ? undefined : new vscode.ThemeColor('errorForeground');
    this.last.tooltip = 'Open latest report';
    this.last.show();
  }

  private update(): void {
    const proj = this.svc.activeProject;
    this.project.text = `$(folder) ${proj ? proj.name : 'No project'}`;
    this.project.tooltip = proj?.location ?? 'Select an INGenious project';

    this.env.text = `$(globe) ${this.svc.environment ?? 'env: default'}`;
    this.env.tooltip = 'Select test data environment';

    const s = this.svc.mcp.status;
    const icon = s === 'connected' ? '$(plug)' : s === 'starting' ? '$(sync~spin)' : s === 'error' ? '$(error)' : '$(debug-disconnect)';
    this.mcp.text = `${icon} MCP: ${s}`;
    this.mcp.tooltip = 'Restart INGenious MCP server';
  }

  dispose(): void {
    this.brand.dispose();
    this.project.dispose();
    this.env.dispose();
    this.last.dispose();
    this.mcp.dispose();
  }
}
