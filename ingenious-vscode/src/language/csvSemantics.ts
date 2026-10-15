import * as vscode from 'vscode';

/** True for CSV documents under TestPlan/ or ReusableComponents/. */
export function isStepDocument(doc: vscode.TextDocument): boolean {
  if (doc.languageId !== 'csv' && !doc.fileName.toLowerCase().endsWith('.csv')) {
    return false;
  }
  return /[\\/](TestPlan|ReusableComponents)[\\/]/.test(doc.fileName);
}

/**
 * Compute the 0-based CSV column index at a character offset within a line,
 * honouring double-quoted fields (so commas inside JSON payloads don't count).
 */
export function columnIndexAt(lineText: string, charIndex: number): number {
  let col = 0;
  let inQuotes = false;
  for (let i = 0; i < charIndex && i < lineText.length; i++) {
    const c = lineText[i];
    if (c === '"') {
      inQuotes = !inQuotes;
    } else if (c === ',' && !inQuotes) {
      col++;
    }
  }
  return col;
}

/** Split a single CSV line into fields (quote-aware). */
export function splitLine(lineText: string): string[] {
  const out: string[] = [];
  let field = '';
  let inQuotes = false;
  for (let i = 0; i < lineText.length; i++) {
    const c = lineText[i];
    if (inQuotes) {
      if (c === '"' && lineText[i + 1] === '"') {
        field += '"';
        i++;
      } else if (c === '"') {
        inQuotes = false;
      } else {
        field += c;
      }
    } else if (c === '"') {
      inQuotes = true;
    } else if (c === ',') {
      out.push(field);
      field = '';
    } else {
      field += c;
    }
  }
  out.push(field);
  return out;
}

export const COL = {
  STEP: 0,
  OBJECT: 1,
  DESCRIPTION: 2,
  ACTION: 3,
  INPUT: 4,
  CONDITION: 5,
  REFERENCE: 6
};

export function isHeaderLine(lineText: string): boolean {
  const first = splitLine(lineText)[0]?.trim().toLowerCase();
  return first === 'step';
}
