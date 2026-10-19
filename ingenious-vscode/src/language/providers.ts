import * as vscode from 'vscode';
import { IngeniousService } from '../client/service';
import { testCaseCoords } from '../util/paths';
import { COL, columnIndexAt, isHeaderLine, isStepDocument } from './csvSemantics';

const CONDITIONS = ['if', 'else if', 'else', 'loop', 'end loop', 'end if', 'while', 'break', 'continue'];

/** Completion for Action, Object and Input columns of a step CSV. */
export class StepCompletionProvider implements vscode.CompletionItemProvider {
  constructor(private readonly svc: IngeniousService) {}

  async provideCompletionItems(
    doc: vscode.TextDocument,
    pos: vscode.Position
  ): Promise<vscode.CompletionItem[]> {
    if (!isStepDocument(doc)) {
      return [];
    }
    const lineText = doc.lineAt(pos.line).text;
    if (isHeaderLine(lineText)) {
      return [];
    }
    const col = columnIndexAt(lineText, pos.character);

    if (col === COL.ACTION) {
      return this.actionItems();
    }
    if (col === COL.OBJECT) {
      return this.objectItems();
    }
    if (col === COL.CONDITION) {
      return CONDITIONS.map((c) => new vscode.CompletionItem(c, vscode.CompletionItemKind.Keyword));
    }
    if (col === COL.INPUT) {
      return this.inputItems();
    }
    return [];
  }

  private async actionItems(): Promise<vscode.CompletionItem[]> {
    const actions = await this.svc.listActions().catch(() => []);
    return actions.map((a) => {
      const item = new vscode.CompletionItem(a.name, vscode.CompletionItemKind.Method);
      item.detail = a.category ? `${a.category}${a.object ? ' · ' + a.object : ''}` : a.object;
      item.documentation = new vscode.MarkdownString(a.description ?? '');
      return item;
    });
  }

  private async objectItems(): Promise<vscode.CompletionItem[]> {
    const items: vscode.CompletionItem[] = [];
    const specials = ['@Browser', 'Webservice', 'Execute'];
    for (const s of specials) {
      const it = new vscode.CompletionItem(s, vscode.CompletionItemKind.Constant);
      it.detail = 'engine object';
      items.push(it);
    }
    try {
      const pages = await this.svc.listObjectPages();
      for (const p of pages) {
        const page = p.page ?? p.name;
        if (!page) {
          continue;
        }
        const objects = await this.svc.showObjectPage(page).catch(() => []);
        for (const o of objects) {
          if (!o.name) {
            continue;
          }
          const it = new vscode.CompletionItem(`${page}.${o.name}`, vscode.CompletionItemKind.Field);
          it.detail = o.type ?? 'object';
          items.push(it);
        }
      }
    } catch {
      /* object repo may be empty */
    }
    return items;
  }

  private inputItems(): vscode.CompletionItem[] {
    const literal = new vscode.CompletionItem('@', vscode.CompletionItemKind.Snippet);
    literal.insertText = new vscode.SnippetString('@${1:value}');
    literal.detail = 'hard-coded literal';

    const dataRef = new vscode.CompletionItem('Sheet:Column', vscode.CompletionItemKind.Snippet);
    dataRef.insertText = new vscode.SnippetString('${1:Sheet}:${2:Column}');
    dataRef.detail = 'data-sheet reference';

    const runtimeVar = new vscode.CompletionItem('%var%', vscode.CompletionItemKind.Snippet);
    runtimeVar.insertText = new vscode.SnippetString('%${1:variable}%');
    runtimeVar.detail = 'runtime variable';

    return [literal, dataRef, runtimeVar];
  }
}

/** Hover for the Action column: description, parameters, example. */
export class StepHoverProvider implements vscode.HoverProvider {
  constructor(private readonly svc: IngeniousService) {}

  async provideHover(doc: vscode.TextDocument, pos: vscode.Position): Promise<vscode.Hover | undefined> {
    if (!isStepDocument(doc)) {
      return undefined;
    }
    const lineText = doc.lineAt(pos.line).text;
    const col = columnIndexAt(lineText, pos.character);
    if (col !== COL.ACTION) {
      return undefined;
    }
    const range = doc.getWordRangeAtPosition(pos, /[A-Za-z0-9_]+/);
    if (!range) {
      return undefined;
    }
    const word = doc.getText(range);
    const info = await this.svc.actionInfo(word);
    if (!info || !info.name) {
      return undefined;
    }
    const md = new vscode.MarkdownString();
    md.appendMarkdown(`**${info.name}**${info.category ? ` — _${info.category}_` : ''}\n\n`);
    if (info.description) {
      md.appendMarkdown(`${info.description}\n\n`);
    }
    if (info.inputType || info.inputExample) {
      md.appendMarkdown(`Input: \`${info.inputType ?? ''}\`${info.inputExample ? ` e.g. \`${info.inputExample}\`` : ''}\n\n`);
    }
    if (info.parameters?.length) {
      md.appendMarkdown('Parameters:\n');
      for (const p of info.parameters) {
        md.appendMarkdown(`- \`${p.name}\`${p.type ? ` (${p.type})` : ''}${p.description ? ` — ${p.description}` : ''}\n`);
      }
    }
    return new vscode.Hover(md, range);
  }
}

/** CodeLens: file-level run/debug/dry-run/report + per-row run-from-here. */
export class StepCodeLensProvider implements vscode.CodeLensProvider {
  provideCodeLenses(doc: vscode.TextDocument): vscode.CodeLens[] {
    if (!isStepDocument(doc)) {
      return [];
    }
    const coords = testCaseCoords(doc.fileName);
    const top = new vscode.Range(0, 0, 0, 0);
    const lenses: vscode.CodeLens[] = [
      new vscode.CodeLens(top, { title: '$(play) Run', command: 'ingenious.run.testcase', arguments: [doc.uri] }),
      new vscode.CodeLens(top, { title: '$(debug-alt) Debug', command: 'ingenious.debug.testcase', arguments: [doc.uri] }),
      new vscode.CodeLens(top, { title: '$(beaker) Dry-run', command: 'ingenious.dryrun.testcase', arguments: [doc.uri] }),
      new vscode.CodeLens(top, { title: '$(check) Validate', command: 'ingenious.validate.testcase', arguments: [doc.uri] })
    ];
    if (coords) {
      lenses.push(
        new vscode.CodeLens(top, {
          title: '$(graph) Last report',
          command: 'ingenious.report.open',
          arguments: [{ target: `${coords.scenario}/${coords.testcase}` }]
        })
      );
    }
    return lenses;
  }
}
