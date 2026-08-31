/**
 * Minimal RFC 4180 CSV codec. INGenious step files can contain commas inside
 * the Input column (e.g. JSON payloads), so a naive split is not safe. This
 * codec quotes only when needed and round-trips faithfully.
 */

export function parseCsv(text: string): string[][] {
  const rows: string[][] = [];
  let field = '';
  let row: string[] = [];
  let inQuotes = false;
  let i = 0;

  const pushField = () => {
    row.push(field);
    field = '';
  };
  const pushRow = () => {
    pushField();
    rows.push(row);
    row = [];
  };

  while (i < text.length) {
    const c = text[i];
    if (inQuotes) {
      if (c === '"') {
        if (text[i + 1] === '"') {
          field += '"';
          i += 2;
          continue;
        }
        inQuotes = false;
        i++;
        continue;
      }
      field += c;
      i++;
      continue;
    }

    if (c === '"') {
      inQuotes = true;
      i++;
      continue;
    }
    if (c === ',') {
      pushField();
      i++;
      continue;
    }
    if (c === '\r') {
      // handled by the \n branch; skip a lone CR
      if (text[i + 1] === '\n') {
        pushRow();
        i += 2;
        continue;
      }
      pushRow();
      i++;
      continue;
    }
    if (c === '\n') {
      pushRow();
      i++;
      continue;
    }
    field += c;
    i++;
  }

  // flush trailing field/row unless the file ended on a newline
  if (field.length > 0 || row.length > 0) {
    pushRow();
  }
  return rows;
}

function needsQuoting(value: string): boolean {
  return /[",\r\n]/.test(value);
}

export function encodeField(value: string): string {
  if (needsQuoting(value)) {
    return '"' + value.replace(/"/g, '""') + '"';
  }
  return value;
}

export function stringifyCsv(rows: string[][], eol = '\n'): string {
  return rows.map((r) => r.map(encodeField).join(',')).join(eol);
}

export interface StepRow {
  step: string;
  object: string;
  description: string;
  action: string;
  input: string;
  condition: string;
  reference: string;
}

export const STEP_HEADER = [
  'Step',
  'ObjectName',
  'Description',
  'Action',
  'Input',
  'Condition',
  'Reference'
];

function isHeaderRow(cells: string[]): boolean {
  const first = (cells[0] ?? '').trim().toLowerCase();
  const fourth = (cells[3] ?? '').trim().toLowerCase();
  return first === 'step' && (fourth === 'action' || fourth === '');
}

/** Parse a step CSV into a header flag + typed rows. */
export function parseStepCsv(text: string): { hasHeader: boolean; steps: StepRow[] } {
  const rows = parseCsv(text).filter((r) => r.some((c) => c.trim() !== ''));
  if (rows.length === 0) {
    return { hasHeader: false, steps: [] };
  }
  const hasHeader = isHeaderRow(rows[0]);
  const body = hasHeader ? rows.slice(1) : rows;
  const steps = body.map((r) => ({
    step: r[0] ?? '',
    object: r[1] ?? '',
    description: r[2] ?? '',
    action: r[3] ?? '',
    input: r[4] ?? '',
    condition: r[5] ?? '',
    reference: r[6] ?? ''
  }));
  return { hasHeader, steps };
}

export function stepsToCsv(steps: StepRow[], includeHeader: boolean, eol = '\n'): string {
  const rows: string[][] = [];
  if (includeHeader) {
    rows.push(STEP_HEADER);
  }
  for (const s of steps) {
    rows.push([
      s.step,
      s.object,
      s.description,
      s.action,
      s.input,
      s.condition,
      s.reference
    ]);
  }
  return stringifyCsv(rows, eol);
}
