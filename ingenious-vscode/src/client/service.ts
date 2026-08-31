import * as vscode from 'vscode';
import { McpClient } from './mcp';
import { CliClient } from './cli';
import { discoverProjects, ProjectRef } from '../util/paths';
import { log } from '../util/logger';
import {
  ActionInfo,
  OrObject,
  ObjectPage,
  ReportEntry,
  ScenarioInfo,
  TestCaseDetail,
  ValidationResult
} from '../model/types';

/**
 * High-level facade the rest of the extension talks to. Prefers the MCP data
 * plane; the CLI is used for runs and bootstrap. Holds workspace-level state:
 * active project and environment.
 */
export class IngeniousService implements vscode.Disposable {
  readonly mcp = new McpClient();
  readonly cli = new CliClient();

  private _activeProject: ProjectRef | undefined;
  private _environment: string | undefined;

  private readonly _onDidChange = new vscode.EventEmitter<void>();
  /** Fires when trees should refresh (project/env change, data mutation). */
  readonly onDidChange = this._onDidChange.event;

  private actionCache: { value: ActionInfo[]; ts: number } | undefined;

  async init(): Promise<void> {
    const cfg = vscode.workspace.getConfiguration('ingenious');
    const projects = discoverProjects();
    this._activeProject = projects[0];
    if (cfg.get<boolean>('mcp.autoStart', true)) {
      this.mcp.start().catch((e) => log.error('MCP autostart failed', e));
    }
  }

  fireChange(): void {
    this._onDidChange.fire();
  }

  // ---- state -------------------------------------------------------------

  get projects(): ProjectRef[] {
    return discoverProjects();
  }

  get activeProject(): ProjectRef | undefined {
    if (this._activeProject && this.projects.some((p) => p.location === this._activeProject!.location)) {
      return this._activeProject;
    }
    this._activeProject = this.projects[0];
    return this._activeProject;
  }

  setActiveProject(p: ProjectRef): void {
    this._activeProject = p;
    this.fireChange();
  }

  get environment(): string | undefined {
    return this._environment;
  }

  setEnvironment(env: string | undefined): void {
    this._environment = env;
    this.fireChange();
  }

  private projectArg(project?: string): Record<string, string> {
    const loc = project ?? this.activeProject?.location;
    return loc ? { project: loc } : {};
  }

  // ---- data plane (MCP) --------------------------------------------------

  async listScenarios(project?: string): Promise<ScenarioInfo[]> {
    const res = await this.mcp.callTool<any>('ingenious_scenario_list', this.projectArg(project));
    return normArray(res, 'scenarios');
  }

  async listReusableScenarios(project?: string): Promise<ScenarioInfo[]> {
    // Reusables live under ReusableComponents/; scenario_list returns TestPlan.
    // The dedicated info tool distinguishes them, but for a tree we fall back to
    // scanning via scenario_info reusable flag is per-scenario, so we surface all
    // scenarios and let the tree read reusable test cases from disk.
    const res = await this.mcp
      .callTool<any>('ingenious_scenario_list', { ...this.projectArg(project), reusable: true })
      .catch(() => undefined);
    return res ? normArray(res, 'scenarios') : [];
  }

  async listTestCases(scenario: string, project?: string): Promise<string[]> {
    const res = await this.mcp.callTool<any>('ingenious_testcase_list', {
      ...this.projectArg(project),
      scenario
    });
    const arr = normArray(res, 'testCases', 'testcases');
    return arr.map((x: any) => (typeof x === 'string' ? x : x.name ?? x.testcase));
  }

  async showTestCase(scenario: string, testcase: string, project?: string): Promise<TestCaseDetail> {
    return this.mcp.callTool<TestCaseDetail>('ingenious_testcase_show', {
      ...this.projectArg(project),
      scenario,
      testcase
    });
  }

  async listObjectPages(project?: string): Promise<ObjectPage[]> {
    const res = await this.mcp.callTool<any>('ingenious_object_list', this.projectArg(project));
    return normArray(res, 'pages', 'objects');
  }

  async showObjectPage(page: string, project?: string): Promise<OrObject[]> {
    const res = await this.mcp.callTool<any>('ingenious_object_show', {
      ...this.projectArg(project),
      page
    });
    return normArray(res, 'objects');
  }

  async searchObjects(query: string, project?: string): Promise<OrObject[]> {
    const res = await this.mcp.callTool<any>('ingenious_object_search', {
      ...this.projectArg(project),
      query
    });
    return normArray(res, 'objects', 'results');
  }

  async listDataSheets(project?: string): Promise<string[]> {
    // No dedicated data_list tool; derive from env_list + data_show is heavy.
    // Fall back to the sheets reported by data via a lightweight probe.
    const res = await this.mcp
      .callTool<any>('ingenious_env_list', this.projectArg(project))
      .catch(() => undefined);
    return res ? normArray(res, 'sheets', 'environments').map((x: any) => (typeof x === 'string' ? x : x.name)) : [];
  }

  async showDataSheet(sheet: string, env?: string, project?: string): Promise<any> {
    return this.mcp.callTool<any>('ingenious_data_show', {
      ...this.projectArg(project),
      sheet,
      ...(env ? { env } : {})
    });
  }

  async listEnvironments(project?: string): Promise<string[]> {
    const res = await this.mcp.callTool<any>('ingenious_env_list', this.projectArg(project));
    return normArray(res, 'environments', 'envs').map((x: any) => (typeof x === 'string' ? x : x.name));
  }

  async listTestSets(project?: string): Promise<Array<{ release: string; testset: string }>> {
    const res = await this.mcp.callTool<any>('ingenious_testset_list', this.projectArg(project));
    const arr = normArray(res, 'testSets', 'testsets');
    return arr.map((x: any) => ({ release: x.release ?? '', testset: x.testset ?? x.name ?? '' }));
  }

  async listApiCollections(project?: string): Promise<string[]> {
    const res = await this.mcp
      .callTool<any>('ingenious_apicollection_list', this.projectArg(project))
      .catch(() => undefined);
    return res ? normArray(res, 'collections').map((x: any) => (typeof x === 'string' ? x : x.name)) : [];
  }

  async validateTestCase(scenario: string, testcase: string, reusable: boolean, project?: string): Promise<ValidationResult> {
    return this.mcp.callTool<ValidationResult>('ingenious_testcase_validate', {
      ...this.projectArg(project),
      scenario,
      testcase,
      reusable
    });
  }

  async listActions(force = false): Promise<ActionInfo[]> {
    if (!force && this.actionCache && Date.now() - this.actionCache.ts < 300000) {
      return this.actionCache.value;
    }
    const res = await this.mcp.callTool<any>('ingenious_action_list', {});
    const value = normArray(res, 'actions').map((a: any) => ({
      name: a.name,
      category: a.category,
      description: a.description,
      object: a.object,
      parameters: a.parameters,
      examples: a.examples,
      inputType: a.inputType,
      inputExample: a.inputExample
    })) as ActionInfo[];
    this.actionCache = { value, ts: Date.now() };
    return value;
  }

  async actionInfo(action: string): Promise<ActionInfo | undefined> {
    try {
      const res = await this.mcp.callTool<any>('ingenious_action_info', { action });
      return (res?.action ?? res) as ActionInfo;
    } catch {
      return undefined;
    }
  }

  async reportLatest(target: string, project?: string): Promise<ReportEntry | undefined> {
    try {
      return await this.mcp.callTool<ReportEntry>('ingenious_report_latest', {
        ...this.projectArg(project),
        target
      });
    } catch {
      return undefined;
    }
  }

  async reportHistory(target: string, limit = 10, project?: string): Promise<ReportEntry[]> {
    try {
      const res = await this.mcp.callTool<any>('ingenious_report_history', {
        ...this.projectArg(project),
        target,
        limit
      });
      return normArray(res, 'runs', 'history');
    } catch {
      return [];
    }
  }

  async doctor(): Promise<any> {
    return this.mcp.callTool<any>('ingenious_doctor', {});
  }

  async runSync(opts: {
    target: string;
    browser?: string;
    headless?: boolean;
    parallel?: number;
    tags?: string;
    rerun?: boolean;
  }): Promise<any> {
    return this.mcp.callTool<any>('ingenious_run', opts as any, 1800000);
  }

  dispose(): void {
    this.mcp.dispose();
    this._onDidChange.dispose();
  }
}

/** Normalise a tool result that may be an array, or an object with a named array. */
function normArray(res: any, ...keys: string[]): any[] {
  if (Array.isArray(res)) {
    return res;
  }
  if (res && typeof res === 'object') {
    for (const k of keys) {
      if (Array.isArray(res[k])) {
        return res[k];
      }
    }
  }
  return [];
}
