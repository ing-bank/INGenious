import * as vscode from 'vscode';
import * as fs from 'fs';
import * as path from 'path';

/** A discovered INGenious project on disk. */
export interface ProjectRef {
  /** Display name (folder name). */
  name: string;
  /** Absolute path to the project root. */
  location: string;
}

/**
 * Markers that identify a folder as an INGenious project root. A project must
 * contain at least one of these.
 */
const PROJECT_MARKERS = ['.project', 'TestPlan', 'ObjectRepository', 'Settings'];

function looksLikeProject(dir: string): boolean {
  try {
    return PROJECT_MARKERS.some((m) => fs.existsSync(path.join(dir, m)));
  } catch {
    return false;
  }
}

/**
 * Discover INGenious projects visible from the current workspace:
 *  - each workspace folder that is itself a project;
 *  - immediate children of each workspace folder;
 *  - immediate children of a top-level `Projects/` folder.
 */
export function discoverProjects(): ProjectRef[] {
  const found = new Map<string, ProjectRef>();
  const roots = vscode.workspace.workspaceFolders ?? [];

  const consider = (dir: string) => {
    if (looksLikeProject(dir)) {
      const loc = path.normalize(dir);
      found.set(loc, { name: path.basename(loc), location: loc });
    }
  };

  const scanChildren = (dir: string) => {
    let entries: fs.Dirent[] = [];
    try {
      entries = fs.readdirSync(dir, { withFileTypes: true });
    } catch {
      return;
    }
    for (const e of entries) {
      if (e.isDirectory() && !e.name.startsWith('.')) {
        consider(path.join(dir, e.name));
      }
    }
  };

  for (const root of roots) {
    const rootPath = root.uri.fsPath;
    consider(rootPath);
    scanChildren(rootPath);
    const projectsDir = path.join(rootPath, 'Projects');
    if (fs.existsSync(projectsDir)) {
      scanChildren(projectsDir);
    }
  }

  return Array.from(found.values()).sort((a, b) => a.name.localeCompare(b.name));
}

/**
 * Given a file inside a project, walk up to find the project root (a folder
 * containing one of the project markers).
 */
export function projectRootFor(fileOrDir: string): string | undefined {
  let dir = fs.existsSync(fileOrDir) && fs.statSync(fileOrDir).isDirectory()
    ? fileOrDir
    : path.dirname(fileOrDir);
  // Never walk above the filesystem root.
  for (let i = 0; i < 40; i++) {
    if (looksLikeProject(dir)) {
      return dir;
    }
    const parent = path.dirname(dir);
    if (parent === dir) {
      break;
    }
    dir = parent;
  }
  return undefined;
}

/**
 * From an absolute path to a TestPlan/ReusableComponents CSV or YAML test case,
 * derive { project, scenario, testcase, reusable }.
 */
export interface TestCaseCoords {
  project: string;
  scenario: string;
  testcase: string;
  reusable: boolean;
}

export function testCaseCoords(file: string): TestCaseCoords | undefined {
  const root = projectRootFor(file);
  if (!root) {
    return undefined;
  }
  const rel = path.relative(root, file);
  const parts = rel.split(path.sep);
  const topIdx = parts.findIndex(
    (p) => p === 'TestPlan' || p === 'ReusableComponents'
  );
  if (topIdx < 0 || parts.length < topIdx + 3) {
    return undefined;
  }
  const reusable = parts[topIdx] === 'ReusableComponents';
  const scenario = parts[topIdx + 1];
  const file0 = parts[parts.length - 1];
  const testcase = file0.replace(/\.(csv|yaml|yml)$/i, '');
  return { project: root, scenario, testcase, reusable };
}

/**
 * Resolve the ingenious launcher path. Order: explicit setting, then the
 * bundled launcher next to the workspace `Resources/` folder, then `PATH`.
 */
export function resolveCliPath(): string {
  const cfg = vscode.workspace.getConfiguration('ingenious');
  const configured = (cfg.get<string>('cliPath') ?? '').trim();
  if (configured && configured !== 'ingenious') {
    return configured;
  }

  const launcher = process.platform === 'win32' ? 'ingenious.bat' : 'ingenious.command';
  for (const root of vscode.workspace.workspaceFolders ?? []) {
    const candidate = path.join(root.uri.fsPath, 'Resources', launcher);
    if (fs.existsSync(candidate)) {
      return candidate;
    }
    const bare = path.join(root.uri.fsPath, 'Resources', 'ingenious');
    if (fs.existsSync(bare)) {
      return bare;
    }
  }
  return configured || 'ingenious';
}

/** Environment for spawning the CLI, honouring the optional JAVA_HOME override. */
export function cliEnv(): NodeJS.ProcessEnv {
  const cfg = vscode.workspace.getConfiguration('ingenious');
  const javaHome = (cfg.get<string>('javaHome') ?? '').trim();
  const env = { ...process.env };
  if (javaHome) {
    env.JAVA_HOME = javaHome;
  }
  return env;
}
