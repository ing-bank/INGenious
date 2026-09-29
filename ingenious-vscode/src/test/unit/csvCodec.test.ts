import * as assert from 'assert';
import { parseCsv, stringifyCsv, parseStepCsv, stepsToCsv, encodeField } from '../../util/csvCodec';

describe('csvCodec', () => {
  it('round-trips a simple row', () => {
    const rows = parseCsv('a,b,c');
    assert.deepStrictEqual(rows, [['a', 'b', 'c']]);
  });

  it('keeps commas inside quoted fields', () => {
    const rows = parseCsv('1,Webservice,Send,postRestRequest,"{""a"":1,""b"":2}",,');
    assert.strictEqual(rows[0][4], '{"a":1,"b":2}');
    assert.strictEqual(rows[0].length, 7);
  });

  it('quotes only when required', () => {
    assert.strictEqual(encodeField('plain'), 'plain');
    assert.strictEqual(encodeField('has,comma'), '"has,comma"');
    assert.strictEqual(encodeField('has"quote'), '"has""quote"');
  });

  it('parses a step csv with header', () => {
    const csv = 'Step,ObjectName,Description,Action,Input,Condition,Reference\n1,Login.user,Enter user,Fill,@bob,,';
    const { hasHeader, steps } = parseStepCsv(csv);
    assert.strictEqual(hasHeader, true);
    assert.strictEqual(steps.length, 1);
    assert.strictEqual(steps[0].action, 'Fill');
    assert.strictEqual(steps[0].input, '@bob');
  });

  it('serialises steps back to csv preserving payload commas', () => {
    const csv =
      'Step,ObjectName,Description,Action,Input,Condition,Reference\n' +
      '1,Webservice,Post,postRestRequest,"{""x"":1,""y"":2}",,';
    const { hasHeader, steps } = parseStepCsv(csv);
    const out = stepsToCsv(steps, hasHeader);
    const reparsed = parseStepCsv(out);
    assert.strictEqual(reparsed.steps[0].input, '{"x":1,"y":2}');
  });

  it('stringify uses the given eol', () => {
    const text = stringifyCsv([['a', 'b'], ['c', 'd']], '\r\n');
    assert.strictEqual(text, 'a,b\r\nc,d');
  });
});
