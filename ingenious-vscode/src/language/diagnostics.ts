import * as vscode from 'vscode';
import { IngeniousService } from '../client/service';
import { testCaseCoords } from '../util/paths';
import { isStepDocument } from './csvSemantics';
import { ValidationFinding, ValidationResult } from '../model/types';
import { log } from '../util/logger';

/**
 * Runs `ingenious_testcase_validate` on save (debounced) and surfaces findings
 * as diagnostics on the step file.
 */
export class StepDiagnostics implements vscode.Disposable {
  private readonly collection: vscode.DiagnosticCollection;
  private timers = new Map<string, NodeJS.Timeout>();

  constructor(private readonly svc: IngeniousService) {
    this.collection = vscode.languages.createDiagnosticCollection('ingenious');
  }

  register(context: vscode.ExtensionContext): void {
    context.subscriptions.push(
      this.collection,
      vscode.workspace.onDidSaveTextDocument((doc) => this.schedule(doc)),
      vscode.workspace.onDidOpenTextDocument((doc) => this.schedule(doc)),
      vscode.workspace.onDidCloseTextDocument((doc) => this.collection.delete(doc.uri))
    );
    for (const doc of vscode.workspace.textDocuments) {
      this.schedule(doc);
    }
  }

  private schedule(doc: vscode.TextDocument): void {
    if (!isStepDocument(doc)) {
      return;
    }
    const key = doc.uri.toString();
    const existing = this.timers.get(key);
    if (existing) {
      clearTimeout(existing);
    }
    this.timers.set(
      key,
      setTimeout(() => {
        this.timers.delete(key);
        this.validate(doc).catch((e) => log.error('validate failed', e));
      }, 500)
    );
  }

  async validate(doc: vscode.TextDocument): Promise<void> {
    const coords = testCaseCoords(doc.fileName);
    if (!coords) {
      return;
    }
    let result: ValidationResult;
    try {
      result = await this.svc.validateTestCase(coords.scenario, coords.testcase, coords.reusable, coords.project);
    } catch (e) {
      // Validation unavailable (MCP down) — clear rather than spam.
      this.collection.delete(doc.uri);
      return;
    }

    const findings: ValidationFinding[] = [
      ...(result.errors ?? []).map((f) => ({ ...f, severity: f.severity ?? 'error' })),
      ...(result.warnings ?? []).map((f) => ({ ...f, severity: f.severity ?? 'warning' })),
      ...(result.info ?? []).map((f) => ({ ...f, severity: f.severity ?? 'info' })),
      ...(result.findings ?? [])
    ];

    const diagnostics: vscode.Diagnostic[] = findings.map((f) => {
      const line = Math.max(0, (f.line ?? f.step ?? 1) - 1);
      const range = doc.lineAt(Math.min(line, doc.lineCount - 1)).range;
      const d = new vscode.Diagnostic(range, f.message ?? 'Issue', severityOf(f.severity ?? f.level));
      d.source = 'ingenious';
      if (f.ruleId ?? f.rule) {
        d.code = f.ruleId ?? f.rule;
      }
      return d;
    });

    this.collection.set(doc.uri, diagnostics);
  }

  dispose(): void {
    for (const t of this.timers.values()) {
      clearTimeout(t);
    }
    this.timers.clear();
    this.collection.dispose();
  }
}

function severityOf(level?: string): vscode.DiagnosticSeverity {
  switch ((level ?? '').toLowerCase()) {
    case 'error':
      return vscode.DiagnosticSeverity.Error;
    case 'warn':
    case 'warning':
      return vscode.DiagnosticSeverity.Warning;
    case 'info':
    case 'information':
      return vscode.DiagnosticSeverity.Information;
    default:
      return vscode.DiagnosticSeverity.Warning;
  }
}
