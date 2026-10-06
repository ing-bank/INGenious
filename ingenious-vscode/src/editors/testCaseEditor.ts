import * as vscode from 'vscode';
import { IngeniousService } from '../client/service';
import { parseStepCsv, stepsToCsv, StepRow } from '../util/csvCodec';

/**
 * Visual grid editor for INGenious step CSV files. Two-way bound to the
 * underlying TextDocument: grid edits produce a full-document WorkspaceEdit,
 * and external text edits refresh the grid.
 */
export class TestCaseEditorProvider implements vscode.CustomTextEditorProvider {
  static readonly viewType = 'ingenious.testCase';

  constructor(private readonly context: vscode.ExtensionContext, private readonly svc: IngeniousService) {}

  resolveCustomTextEditor(
    document: vscode.TextDocument,
    panel: vscode.WebviewPanel
  ): void {
    panel.webview.options = { enableScripts: true };
    panel.webview.html = this.html(panel.webview);

    let updatingFromWebview = false;

    const postDocument = () => {
      const parsed = parseStepCsv(document.getText());
      panel.webview.postMessage({ type: 'load', steps: parsed.steps, hasHeader: parsed.hasHeader });
    };

    const changeSub = vscode.workspace.onDidChangeTextDocument((e) => {
      if (e.document.uri.toString() === document.uri.toString() && !updatingFromWebview) {
        postDocument();
      }
    });
    panel.onDidDispose(() => changeSub.dispose());

    panel.webview.onDidReceiveMessage(async (msg) => {
      switch (msg.type) {
        case 'ready':
          postDocument();
          this.sendCatalogs(panel);
          break;
        case 'save': {
          updatingFromWebview = true;
          await this.writeSteps(document, msg.steps as StepRow[], msg.hasHeader as boolean);
          updatingFromWebview = false;
          break;
        }
        case 'run':
          vscode.commands.executeCommand('ingenious.run.testcase', document.uri);
          break;
        case 'validate':
          vscode.commands.executeCommand('ingenious.validate.testcase', document.uri);
          break;
      }
    });
  }

  private async sendCatalogs(panel: vscode.WebviewPanel): Promise<void> {
    const [actions, pages] = await Promise.all([
      this.svc.listActions().catch(() => []),
      this.svc.listObjectPages().catch(() => [])
    ]);
    const objects: string[] = ['@Browser', 'Webservice', 'Execute'];
    for (const p of pages) {
      const page = p.page ?? p.name;
      if (!page) {
        continue;
      }
      const objs = await this.svc.showObjectPage(page).catch(() => []);
      for (const o of objs) {
        if (o.name) {
          objects.push(`${page}.${o.name}`);
        }
      }
    }
    panel.webview.postMessage({
      type: 'catalogs',
      actions: actions.map((a) => a.name),
      objects
    });
  }

  private async writeSteps(document: vscode.TextDocument, steps: StepRow[], hasHeader: boolean): Promise<void> {
    const eol = document.eol === vscode.EndOfLine.CRLF ? '\r\n' : '\n';
    const text = stepsToCsv(steps, hasHeader, eol) + eol;
    const edit = new vscode.WorkspaceEdit();
    const full = new vscode.Range(0, 0, document.lineCount, 0);
    edit.replace(document.uri, full, text);
    await vscode.workspace.applyEdit(edit);
  }

  private html(webview: vscode.Webview): string {
    const nonce = String(Math.random()).slice(2);
    const csp = `default-src 'none'; style-src 'unsafe-inline'; script-src 'nonce-${nonce}';`;
    return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta http-equiv="Content-Security-Policy" content="${csp}">
<style>
  body { font-family: var(--vscode-font-family); color: var(--vscode-foreground); padding: 6px; }
  .toolbar { margin-bottom: 8px; display:flex; gap:6px; flex-wrap:wrap; }
  button { background: var(--vscode-button-background); color: var(--vscode-button-foreground); border:none; padding:4px 8px; cursor:pointer; border-radius:3px; }
  button.secondary { background: var(--vscode-button-secondaryBackground); color: var(--vscode-button-secondaryForeground); }
  table { border-collapse: collapse; width:100%; }
  th, td { border:1px solid var(--vscode-panel-border,#444); padding:2px 4px; text-align:left; font-size:12px; vertical-align:top; }
  th { background: var(--vscode-editorWidget-background); position:sticky; top:0; }
  input { width:100%; box-sizing:border-box; background:transparent; color:var(--vscode-foreground); border:none; font-size:12px; }
  input:focus { outline:1px solid var(--vscode-focusBorder); }
  td.num { width:36px; text-align:center; color:var(--vscode-descriptionForeground); }
  td.rowops { width:64px; white-space:nowrap; }
  td.rowops button { padding:0 5px; }
</style>
</head>
<body>
  <div class="toolbar">
    <button id="add">+ Step</button>
    <button class="secondary" id="run">▶ Run</button>
    <button class="secondary" id="validate">✔ Validate</button>
    <span id="status" style="align-self:center;color:var(--vscode-descriptionForeground)"></span>
  </div>
  <table>
    <thead><tr><th class="num">#</th><th>Object</th><th>Description</th><th>Action</th><th>Input</th><th>Condition</th><th>Reference</th><th class="rowops"></th></tr></thead>
    <tbody id="rows"></tbody>
  </table>
  <datalist id="actions"></datalist>
  <datalist id="objects"></datalist>
<script nonce="${nonce}">
  const vscode = acquireVsCodeApi();
  let steps = [];
  let hasHeader = true;

  function opt(list, values){ list.innerHTML = values.map(v => '<option value="'+escapeAttr(v)+'">').join(''); }
  function escapeAttr(s){ return String(s).replace(/"/g,'&quot;'); }
  function cell(field, i, listId){
    const v = escapeAttr(steps[i][field] ?? '');
    const list = listId ? ' list="'+listId+'"' : '';
    return '<input data-i="'+i+'" data-f="'+field+'"'+list+' value="'+v+'">';
  }
  function render(){
    const tb = document.getElementById('rows');
    tb.innerHTML = steps.map((s,i) =>
      '<tr>'
      + '<td class="num">'+(i+1)+'</td>'
      + '<td>'+cell('object',i,'objects')+'</td>'
      + '<td>'+cell('description',i)+'</td>'
      + '<td>'+cell('action',i,'actions')+'</td>'
      + '<td>'+cell('input',i)+'</td>'
      + '<td>'+cell('condition',i)+'</td>'
      + '<td>'+cell('reference',i)+'</td>'
      + '<td class="rowops"><button data-op="up" data-i="'+i+'">↑</button><button data-op="del" data-i="'+i+'">✕</button></td>'
      + '</tr>'
    ).join('');
  }
  function renumber(){ steps.forEach((s,i)=> s.step = String(i+1)); }
  function save(){ renumber(); vscode.postMessage({type:'save', steps, hasHeader}); }

  document.addEventListener('input', (e) => {
    const t = e.target;
    if (t.dataset && t.dataset.i !== undefined){
      steps[+t.dataset.i][t.dataset.f] = t.value;
      save();
    }
  });
  document.addEventListener('click', (e) => {
    const t = e.target;
    if (t.dataset && t.dataset.op === 'del'){ steps.splice(+t.dataset.i,1); render(); save(); }
    else if (t.dataset && t.dataset.op === 'up'){ const i=+t.dataset.i; if(i>0){ const tmp=steps[i-1]; steps[i-1]=steps[i]; steps[i]=tmp; render(); save(); } }
  });
  document.getElementById('add').addEventListener('click', () => { steps.push({step:'',object:'',description:'',action:'',input:'',condition:'',reference:''}); render(); save(); });
  document.getElementById('run').addEventListener('click', () => vscode.postMessage({type:'run'}));
  document.getElementById('validate').addEventListener('click', () => vscode.postMessage({type:'validate'}));

  window.addEventListener('message', (ev) => {
    const m = ev.data;
    if (m.type === 'load'){ steps = m.steps || []; hasHeader = m.hasHeader; render(); }
    else if (m.type === 'catalogs'){ opt(document.getElementById('actions'), m.actions||[]); opt(document.getElementById('objects'), m.objects||[]); }
  });
  vscode.postMessage({type:'ready'});
</script>
</body>
</html>`;
  }
}
