import * as vscode from 'vscode';
import * as path from 'path';
import { IngeniousService } from '../client/service';
import { ProjectRef } from '../util/paths';

/** Generic tree node used across all INGenious views. */
export class IngNode extends vscode.TreeItem {
  constructor(
    label: string,
    collapsible: vscode.TreeItemCollapsibleState,
    public readonly kind: string,
    public readonly data?: any
  ) {
    super(label, collapsible);
    this.contextValue = kind;
  }
}

abstract class BaseTree implements vscode.TreeDataProvider<IngNode> {
  protected readonly _onDidChange = new vscode.EventEmitter<IngNode | undefined | void>();
  readonly onDidChangeTreeData = this._onDidChange.event;

  constructor(protected readonly svc: IngeniousService) {
    svc.onDidChange(() => this.refresh());
  }

  refresh(): void {
    this._onDidChange.fire();
  }

  getTreeItem(el: IngNode): vscode.TreeItem {
    return el;
  }

  abstract getChildren(el?: IngNode): Promise<IngNode[]>;

  protected errorNode(e: unknown): IngNode[] {
    const n = new IngNode(`⚠ ${String(e)}`, vscode.TreeItemCollapsibleState.None, 'error');
    return [n];
  }
}

// ---------------------------------------------------------------------------

export class ProjectsTree extends BaseTree {
  async getChildren(): Promise<IngNode[]> {
    const active = this.svc.activeProject;
    return this.svc.projects.map((p: ProjectRef) => {
      const n = new IngNode(p.name, vscode.TreeItemCollapsibleState.None, 'ingenious.project', p);
      n.resourceUri = vscode.Uri.file(p.location);
      n.iconPath = new vscode.ThemeIcon(active?.location === p.location ? 'folder-active' : 'folder');
      n.description = active?.location === p.location ? 'active' : undefined;
      n.command = {
        command: 'ingenious.project.select',
        title: 'Select',
        arguments: [p]
      };
      n.tooltip = p.location;
      return n;
    });
  }
}

// ---------------------------------------------------------------------------

export class TestPlanTree extends BaseTree {
  constructor(svc: IngeniousService, private readonly reusable: boolean) {
    super(svc);
  }

  async getChildren(el?: IngNode): Promise<IngNode[]> {
    try {
      if (!el) {
        const scenarios = this.reusable
          ? await this.svc.listReusableScenarios()
          : await this.svc.listScenarios();
        if (scenarios.length === 0) {
          return [new IngNode(this.reusable ? 'No reusables' : 'No scenarios', vscode.TreeItemCollapsibleState.None, 'empty')];
        }
        return scenarios.map((s) => {
          const n = new IngNode(
            s.name,
            vscode.TreeItemCollapsibleState.Collapsed,
            'ingenious.scenario',
            { scenario: s.name, reusable: this.reusable }
          );
          n.iconPath = new vscode.ThemeIcon('folder');
          if (s.testCaseCount !== undefined) {
            n.description = `${s.testCaseCount}`;
          }
          return n;
        });
      }
      if (el.kind === 'ingenious.scenario') {
        const cases = await this.svc.listTestCases(el.data.scenario);
        return cases.map((tc) => this.testCaseNode(el.data.scenario, tc));
      }
      return [];
    } catch (e) {
      return this.errorNode(e);
    }
  }

  private testCaseNode(scenario: string, testcase: string): IngNode {
    const n = new IngNode(testcase, vscode.TreeItemCollapsibleState.None, 'ingenious.testcase', {
      scenario,
      testcase,
      reusable: this.reusable
    });
    n.iconPath = new vscode.ThemeIcon('file-code');
    const proj = this.svc.activeProject;
    if (proj) {
      const top = this.reusable ? 'ReusableComponents' : 'TestPlan';
      // Prefer the YAML file, fall back to CSV; the open command tries both.
      n.command = {
        command: 'ingenious.internal.openTestCase',
        title: 'Open',
        arguments: [proj.location, top, scenario, testcase]
      };
      n.data.projectDir = proj.location;
      n.data.top = top;
    }
    return n;
  }
}

// ---------------------------------------------------------------------------

export class TestSetsTree extends BaseTree {
  async getChildren(el?: IngNode): Promise<IngNode[]> {
    try {
      if (!el) {
        const sets = await this.svc.listTestSets();
        if (sets.length === 0) {
          return [new IngNode('No test sets', vscode.TreeItemCollapsibleState.None, 'empty')];
        }
        const byRelease = new Map<string, Array<{ release: string; testset: string }>>();
        for (const s of sets) {
          const list = byRelease.get(s.release) ?? [];
          list.push(s);
          byRelease.set(s.release, list);
        }
        return Array.from(byRelease.keys()).map((rel) => {
          const n = new IngNode(rel || '(release)', vscode.TreeItemCollapsibleState.Collapsed, 'ingenious.release', {
            release: rel,
            sets: byRelease.get(rel)
          });
          n.iconPath = new vscode.ThemeIcon('milestone');
          return n;
        });
      }
      if (el.kind === 'ingenious.release') {
        return (el.data.sets as Array<{ release: string; testset: string }>).map((s) => {
          const n = new IngNode(s.testset, vscode.TreeItemCollapsibleState.None, 'ingenious.testset', s);
          n.iconPath = new vscode.ThemeIcon('checklist');
          return n;
        });
      }
      return [];
    } catch (e) {
      return this.errorNode(e);
    }
  }
}

// ---------------------------------------------------------------------------

export class ObjectsTree extends BaseTree {
  async getChildren(el?: IngNode): Promise<IngNode[]> {
    try {
      if (!el) {
        const pages = await this.svc.listObjectPages();
        if (pages.length === 0) {
          return [new IngNode('No pages', vscode.TreeItemCollapsibleState.None, 'empty')];
        }
        return pages.map((p) => {
          const name = p.page ?? p.name ?? '(page)';
          const n = new IngNode(name, vscode.TreeItemCollapsibleState.Collapsed, 'ingenious.orpage', { page: name });
          n.iconPath = new vscode.ThemeIcon('browser');
          const count = p.objectCount ?? p.count;
          if (count !== undefined) {
            n.description = `${count}`;
          }
          return n;
        });
      }
      if (el.kind === 'ingenious.orpage') {
        const objects = await this.svc.showObjectPage(el.data.page);
        return objects.map((o) => {
          const n = new IngNode(o.name ?? '(object)', vscode.TreeItemCollapsibleState.None, 'ingenious.orobject', o);
          n.iconPath = new vscode.ThemeIcon('symbol-field');
          const loc = o.locator ?? (o.locators ? Object.values(o.locators)[0] : undefined);
          n.description = o.type ?? '';
          n.tooltip = loc ? `${o.type ?? ''} ${loc}` : o.type;
          return n;
        });
      }
      return [];
    } catch (e) {
      return this.errorNode(e);
    }
  }
}

// ---------------------------------------------------------------------------

export class DataTree extends BaseTree {
  async getChildren(el?: IngNode): Promise<IngNode[]> {
    try {
      if (!el) {
        const envs = await this.svc.listEnvironments().catch(() => []);
        const nodes: IngNode[] = [];
        if (envs.length > 0) {
          const envNode = new IngNode('Environments', vscode.TreeItemCollapsibleState.Collapsed, 'ingenious.envgroup', { envs });
          envNode.iconPath = new vscode.ThemeIcon('globe');
          nodes.push(envNode);
        }
        return nodes.length > 0
          ? nodes
          : [new IngNode('No test data', vscode.TreeItemCollapsibleState.None, 'empty')];
      }
      if (el.kind === 'ingenious.envgroup') {
        return (el.data.envs as string[]).map((env) => {
          const n = new IngNode(env, vscode.TreeItemCollapsibleState.None, 'ingenious.env', { env });
          n.iconPath = new vscode.ThemeIcon('symbol-enum');
          n.command = {
            command: 'ingenious.env.select',
            title: 'Select environment',
            arguments: [env]
          };
          return n;
        });
      }
      return [];
    } catch (e) {
      return this.errorNode(e);
    }
  }
}

// ---------------------------------------------------------------------------

export class ApiCollectionsTree extends BaseTree {
  async getChildren(): Promise<IngNode[]> {
    try {
      const cols = await this.svc.listApiCollections();
      if (cols.length === 0) {
        return [new IngNode('No API collections', vscode.TreeItemCollapsibleState.None, 'empty')];
      }
      return cols.map((c) => {
        const n = new IngNode(c, vscode.TreeItemCollapsibleState.None, 'ingenious.apicollection', { name: c });
        n.iconPath = new vscode.ThemeIcon('symbol-interface');
        return n;
      });
    } catch (e) {
      return this.errorNode(e);
    }
  }
}

// ---------------------------------------------------------------------------

export class ReportsTree extends BaseTree {
  async getChildren(el?: IngNode): Promise<IngNode[]> {
    try {
      if (!el) {
        const scenarios = await this.svc.listScenarios();
        if (scenarios.length === 0) {
          return [new IngNode('No runs yet', vscode.TreeItemCollapsibleState.None, 'empty')];
        }
        return scenarios.map((s) => {
          const n = new IngNode(s.name, vscode.TreeItemCollapsibleState.Collapsed, 'ingenious.reportscenario', {
            scenario: s.name
          });
          n.iconPath = new vscode.ThemeIcon('folder');
          return n;
        });
      }
      if (el.kind === 'ingenious.reportscenario') {
        const cases = await this.svc.listTestCases(el.data.scenario);
        return cases.map((tc) => {
          const n = new IngNode(tc, vscode.TreeItemCollapsibleState.None, 'ingenious.report', {
            target: `${el.data.scenario}/${tc}`
          });
          n.iconPath = new vscode.ThemeIcon('graph');
          n.command = {
            command: 'ingenious.report.open',
            title: 'Open report',
            arguments: [{ target: `${el.data.scenario}/${tc}` }]
          };
          return n;
        });
      }
      return [];
    } catch (e) {
      return this.errorNode(e);
    }
  }
}

/** Locate the on-disk file for a test case, preferring YAML then CSV. */
export function testCaseFileCandidates(projectDir: string, top: string, scenario: string, testcase: string): string[] {
  return [
    path.join(projectDir, top, scenario, `${testcase}.yaml`),
    path.join(projectDir, top, scenario, `${testcase}.yml`),
    path.join(projectDir, top, scenario, `${testcase}.csv`)
  ];
}
