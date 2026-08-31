import * as vscode from 'vscode';
import * as fs from 'fs';
import * as path from 'path';
import { IngeniousService } from '../client/service';
import { log } from '../util/logger';

/**
 * Renders an INGenious HTML report (summary-v2.html) inside a webview. The
 * report assets are already self-contained/inlined by the engine, so we can
 * host the HTML directly with scripts enabled and no network access.
 */
export class ReportPanel {
  private static current: ReportPanel | undefined;
  private readonly panel: vscode.WebviewPanel;

  private constructor(private readonly svc: IngeniousService) {
    this.panel = vscode.window.createWebviewPanel(
      'ingenious.report',
      'INGenious Report',
      vscode.ViewColumn.Active,
      { enableScripts: true, retainContextWhenHidden: true }
    );
    this.panel.onDidDispose(() => {
      if (ReportPanel.current === this) {
        ReportPanel.current = undefined;
      }
    });
    this.panel.webview.onDidReceiveMessage((m) => {
      if (m?.command === 'openExternal' && m.path) {
        vscode.env.openExternal(vscode.Uri.file(m.path));
      }
    });
  }

  static async show(svc: IngeniousService, target: string): Promise<void> {
    if (!ReportPanel.current) {
      ReportPanel.current = new ReportPanel(svc);
    }
    await ReportPanel.current.load(target);
    ReportPanel.current.panel.reveal();
  }

  private async load(target: string): Promise<void> {
    this.panel.title = `Report: ${target}`;
    const reportFile = await this.resolveReportFile(target);
    if (!reportFile) {
      this.panel.webview.html = this.notFoundHtml(target);
      return;
    }
    try {
      let html = fs.readFileSync(reportFile, 'utf8');
      html = this.injectBaseAndBar(html, reportFile);
      this.panel.webview.html = html;
    } catch (e) {
      log.error('Failed to load report', e);
      this.panel.webview.html = this.notFoundHtml(target);
    }
  }

  /** Ask the engine for the latest report path, then fall back to disk scan. */
  private async resolveReportFile(target: string): Promise<string | undefined> {
    const latest = await this.svc.reportLatest(target).catch(() => undefined);
    const fromTool = latest?.reportPath ?? latest?.path ?? latest?.summaryPath;
    if (fromTool && fs.existsSync(fromTool)) {
      return this.normalizeToHtml(fromTool);
    }
    // Disk fallback: Results/<Scenario>/<TestCase>/<timestamp>/summary-v2.html
    const proj = this.svc.activeProject;
    if (!proj) {
      return undefined;
    }
    const base = path.join(proj.location, 'Results', ...target.split('/'));
    if (!fs.existsSync(base)) {
      return undefined;
    }
    const latestDir = this.newestChild(base);
    if (!latestDir) {
      return undefined;
    }
    for (const name of ['summary-v2.html', 'summary.html', 'index.html']) {
      const candidate = path.join(latestDir, name);
      if (fs.existsSync(candidate)) {
        return candidate;
      }
    }
    return undefined;
  }

  private normalizeToHtml(p: string): string | undefined {
    if (p.toLowerCase().endsWith('.html')) {
      return p;
    }
    // A directory or json path was returned — look for the summary next to it.
    const dir = fs.statSync(p).isDirectory() ? p : path.dirname(p);
    for (const name of ['summary-v2.html', 'summary.html', 'index.html']) {
      const candidate = path.join(dir, name);
      if (fs.existsSync(candidate)) {
        return candidate;
      }
    }
    return undefined;
  }

  private newestChild(dir: string): string | undefined {
    let best: { p: string; t: number } | undefined;
    for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
      if (!e.isDirectory()) {
        continue;
      }
      const full = path.join(dir, e.name);
      const t = fs.statSync(full).mtimeMs;
      if (!best || t > best.t) {
        best = { p: full, t };
      }
    }
    return best?.p;
  }

  private injectBaseAndBar(html: string, reportFile: string): string {
    const baseUri = this.panel.webview.asWebviewUri(vscode.Uri.file(path.dirname(reportFile) + path.sep));
    const bar = `<div style="position:sticky;top:0;z-index:9999;background:var(--vscode-editor-background,#1e1e1e);color:var(--vscode-foreground,#ccc);padding:6px 10px;border-bottom:1px solid #444;font-family:sans-serif;font-size:12px">
      <a href="#" onclick="acquireVsCodeApi().postMessage({command:'openExternal',path:'${reportFile.replace(/\\/g, '\\\\')}'});return false;" style="color:var(--vscode-textLink-foreground,#3794ff)">Open in browser</a>
    </div>`;
    if (/<base\s/i.test(html)) {
      // already has a base; leave assets alone
    } else if (/<head[^>]*>/i.test(html)) {
      html = html.replace(/<head([^>]*)>/i, `<head$1><base href="${baseUri}/">`);
    }
    if (/<body[^>]*>/i.test(html)) {
      html = html.replace(/<body([^>]*)>/i, `<body$1>${bar}`);
    } else {
      html = bar + html;
    }
    return html;
  }

  private notFoundHtml(target: string): string {
    return `<!DOCTYPE html><html><body style="font-family:sans-serif;padding:20px;color:#ccc">
      <h3>No report found</h3>
      <p>There is no run report for <code>${escapeHtml(target)}</code> yet. Run the test case first.</p>
    </body></html>`;
  }
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string));
}
