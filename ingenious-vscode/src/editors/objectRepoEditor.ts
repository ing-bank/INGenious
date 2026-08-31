import * as vscode from 'vscode';
import * as YAML from 'yaml';
import { log } from '../util/logger';

const LOCATOR_KEYS = [
  'role',
  'text',
  'label',
  'placeholder',
  'css',
  'xpath',
  'altText',
  'title',
  'testId',
  'chainedLocator',
  'jsPath'
];

interface OrRow {
  name: string;
  props: Record<string, string>;
}

/**
 * Editor for YAML Object Repository pages (ObjectRepository/Web/<Page>.yaml).
 * Renders one row per object with its locator properties. Edits are written
 * back to the YAML document text; the Datalib model remains authoritative when
 * the engine re-reads it.
 */
export class ObjectRepoEditorProvider implements vscode.CustomTextEditorProvider {
  static readonly viewType = 'ingenious.objectRepo';

  constructor(private readonly context: vscode.ExtensionContext) {}

  resolveCustomTextEditor(document: vscode.TextDocument, panel: vscode.WebviewPanel): void {
    panel.webview.options = { enableScripts: true };
    panel.webview.html = this.html(panel.webview);

    let updatingFromWebview = false;

    const post = () => {
      const parsed = this.parse(document.getText());
      panel.webview.postMessage({ type: 'load', page: parsed.page, rows: parsed.rows, keys: LOCATOR_KEYS });
    };

    const sub = vscode.workspace.onDidChangeTextDocument((e) => {
      if (e.document.uri.toString() === document.uri.toString() && !updatingFromWebview) {
        post();
      }
    });
    panel.onDidDispose(() => sub.dispose());

    panel.webview.onDidReceiveMessage(async (msg) => {
      if (msg.type === 'ready') {
        post();
      } else if (msg.type === 'save') {
        updatingFromWebview = true;
        await this.write(document, msg.page, msg.rows as OrRow[]);
        updatingFromWebview = false;
      }
    });
  }

  private parse(text: string): { page: string; rows: OrRow[] } {
    try {
      const doc = YAML.parse(text) ?? {};
      const page = doc.page ?? doc.name ?? '';
      const elements = doc.elements ?? doc.objects ?? {};
      const rows: OrRow[] = [];
      for (const [name, val] of Object.entries(elements)) {
        const props: Record<string, string> = {};
        if (val && typeof val === 'object') {
          for (const [k, v] of Object.entries(val as Record<string, unknown>)) {
            props[k] = v == null ? '' : String(v);
          }
        }
        rows.push({ name, props });
      }
      return { page, rows };
    } catch (e) {
      log.error('Failed to parse OR YAML', e);
      return { page: '', rows: [] };
    }
  }

  private async write(document: vscode.TextDocument, page: string, rows: OrRow[]): Promise<void> {
    let root: any;
    try {
      root = YAML.parse(document.getText()) ?? {};
    } catch {
      root = {};
    }
    if (page) {
      root.page = page;
    }
    const elements: Record<string, Record<string, string>> = {};
    for (const r of rows) {
      if (!r.name) {
        continue;
      }
      const props: Record<string, string> = {};
      for (const [k, v] of Object.entries(r.props)) {
        if (v !== undefined && v !== '') {
          props[k] = v;
        }
      }
      elements[r.name] = props;
    }
    root.elements = elements;
    const text = YAML.stringify(root);
    const edit = new vscode.WorkspaceEdit();
    const full = new vscode.Range(0, 0, document.lineCount, 0);
    edit.replace(document.uri, full, text);
    await vscode.workspace.applyEdit(edit);
  }

  private html(webview: vscode.Webview): string {
    const nonce = String(Math.random()).slice(2);
    const csp = `default-src 'none'; style-src 'unsafe-inline'; script-src 'nonce-${nonce}';`;
    return `<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8">
<meta http-equiv="Content-Security-Policy" content="${csp}">
<style>
  body { font-family: var(--vscode-font-family); color: var(--vscode-foreground); padding:6px; }
  table { border-collapse: collapse; width:100%; }
  th, td { border:1px solid var(--vscode-panel-border,#444); padding:2px 4px; font-size:12px; }
  th { background: var(--vscode-editorWidget-background); position:sticky; top:0; }
  input { width:100%; box-sizing:border-box; background:transparent; color:var(--vscode-foreground); border:none; font-size:12px; }
  input:focus { outline:1px solid var(--vscode-focusBorder); }
  .toolbar { margin-bottom:8px; display:flex; gap:6px; align-items:center; }
  button { background: var(--vscode-button-background); color: var(--vscode-button-foreground); border:none; padding:4px 8px; cursor:pointer; border-radius:3px; }
</style></head>
<body>
  <div class="toolbar">
    <strong>Page:</strong> <input id="page" style="max-width:200px;border:1px solid var(--vscode-panel-border)">
    <button id="add">+ Object</button>
  </div>
  <table><thead id="head"></thead><tbody id="rows"></tbody></table>
<script nonce="${nonce}">
  const vscode = acquireVsCodeApi();
  let rows = []; let keys = []; let page = '';
  function esc(s){ return String(s??'').replace(/"/g,'&quot;'); }
  function head(){ document.getElementById('head').innerHTML = '<tr><th>Name</th>' + keys.map(k=>'<th>'+k+'</th>').join('') + '<th></th></tr>'; }
  function render(){
    document.getElementById('page').value = page;
    document.getElementById('rows').innerHTML = rows.map((r,i)=>
      '<tr><td><input data-i="'+i+'" data-k="__name" value="'+esc(r.name)+'"></td>'
      + keys.map(k=>'<td><input data-i="'+i+'" data-k="'+k+'" value="'+esc(r.props[k]||'')+'"></td>').join('')
      + '<td><button data-op="del" data-i="'+i+'">✕</button></td></tr>'
    ).join('');
  }
  function save(){ vscode.postMessage({type:'save', page, rows}); }
  document.addEventListener('input',(e)=>{
    const t=e.target;
    if(t.id==='page'){ page=t.value; save(); return; }
    if(t.dataset && t.dataset.i!==undefined){
      const i=+t.dataset.i, k=t.dataset.k;
      if(k==='__name') rows[i].name=t.value; else rows[i].props[k]=t.value;
      save();
    }
  });
  document.addEventListener('click',(e)=>{
    const t=e.target;
    if(t.dataset && t.dataset.op==='del'){ rows.splice(+t.dataset.i,1); render(); save(); }
  });
  document.getElementById('add').addEventListener('click',()=>{ rows.push({name:'newObject',props:{}}); render(); save(); });
  window.addEventListener('message',(ev)=>{
    const m=ev.data;
    if(m.type==='load'){ rows=m.rows||[]; keys=m.keys||[]; page=m.page||''; head(); render(); }
  });
  vscode.postMessage({type:'ready'});
</script>
</body></html>`;
  }
}
