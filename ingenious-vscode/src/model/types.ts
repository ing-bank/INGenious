/** Shared data shapes returned by the INGenious MCP tools (loosely typed). */

export interface ScenarioInfo {
  name: string;
  testCaseCount?: number;
  testCases?: string[];
}

export interface TestCaseStep {
  step?: string | number;
  object?: string;
  description?: string;
  action?: string;
  input?: string;
  condition?: string;
  reference?: string;
}

export interface TestCaseDetail {
  scenario?: string;
  testcase?: string;
  format?: string;
  steps?: TestCaseStep[];
}

export interface ObjectPage {
  page?: string;
  name?: string;
  objectCount?: number;
  count?: number;
}

export interface OrObject {
  name?: string;
  type?: string;
  locator?: string;
  value?: string;
  description?: string;
  locators?: Record<string, string>;
}

export interface DataSheetRef {
  name?: string;
  sheet?: string;
}

export interface TestSetRef {
  release?: string;
  testset?: string;
  name?: string;
}

export interface ValidationFinding {
  ruleId?: string;
  rule?: string;
  severity?: string;
  level?: string;
  message?: string;
  scenario?: string;
  testcase?: string;
  step?: number;
  line?: number;
}

export interface ValidationResult {
  valid?: boolean;
  errors?: ValidationFinding[];
  warnings?: ValidationFinding[];
  info?: ValidationFinding[];
  findings?: ValidationFinding[];
}

export interface ActionInfo {
  name: string;
  category?: string;
  description?: string;
  object?: string;
  parameters?: Array<{ name: string; type?: string; description?: string }>;
  examples?: string[];
  inputType?: string;
  inputExample?: string;
}

export interface ReportEntry {
  timestamp?: string;
  status?: string;
  passed?: number;
  failed?: number;
  reportPath?: string;
  path?: string;
  summaryPath?: string;
}
