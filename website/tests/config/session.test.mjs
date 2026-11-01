import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import {
  ConfigFileError, detectFile, openFile, newSession, getField, setField, resetField, setCash,
  issues, render, changedLines, downloadName,
} from '../../src/lib/config/session.mjs';
import { parseCash } from '../../src/lib/config/cash.mjs';

const fx = (name) => readFileSync(new URL(`./fixtures/${name}`, import.meta.url), 'utf8');
const COMMON = fx('qrm-common.toml');
const BANK = fx('qrm_bank-server.toml');
const CASH = '{\n  "QRM": { "decimals": 2, "denominations": [100, 500, 1000, 5000, 10000] }\n}\n';
const lineOf = (text, content) => text.split(/\r?\n/).indexOf(content) + 1;

test('file non modificato: render identico all’originale', () => {
  assert.equal(render(openFile('qrm-common.toml', COMMON)), COMMON);
  assert.equal(render(openFile('qrm_bank-server.toml', BANK)), BANK);
  assert.equal(render(openFile('cash.json', CASH)), CASH);
  assert.deepEqual(changedLines(openFile('qrm-common.toml', COMMON)), []);
});

test('una modifica cambia una sola riga e la segnala', () => {
  const s = openFile('qrm-common.toml', COMMON);
  setField(s, 'characters.slots', 5n);
  assert.deepEqual(changedLines(s), [lineOf(COMMON, '\tslots = 3')]);
  assert.equal(getField(s, 'characters.slots').modified, true);
  assert.equal(getField(s, 'characters.slots').value, 5n);
  const out = render(s);
  assert.equal(out.split(/\r?\n/).length, COMMON.split(/\r?\n/).length);
  assert.equal(out.split('\n').length, out.split('\r\n').length, 'CRLF omogeneo');
});

test('chiave assente: compare col predefinito, entra nel file solo se modificata', () => {
  const s = openFile('qrm_bank-server.toml', BANK);
  assert.deepEqual(getField(s, 'atm.vipBalance'), { value: 100000n, present: false, modified: false });
  setField(s, 'atm.vipBalance', 500n);
  assert.equal(getField(s, 'atm.vipBalance').modified, true);
  const out = render(s);
  assert.ok(out.includes('\tvipBalance = 500'));
  assert.equal(changedLines(s).length, 2, 'commento e chiave');
});

test('resetField torna all’originale', () => {
  const s = openFile('qrm_bank-server.toml', BANK);
  setField(s, 'atm.vipBalance', 500n);
  setField(s, 'atm.historyRows', 30n);
  resetField(s, 'atm.vipBalance');
  resetField(s, 'atm.historyRows');
  assert.equal(render(s), BANK);
  assert.equal(getField(s, 'atm.historyRows').value, 20n);
});

test('chiavi sconosciute: elencate e conservate', () => {
  const text = `${COMMON}\r\n[foo]\r\n\tbar = 1\r\n`;
  const s = openFile('qrm-common.toml', text);
  assert.deepEqual(s.unknownKeys, ['foo.bar']);
  assert.equal(render(s), text);
});

test('riconoscimento del file: per nome, poi per contenuto', () => {
  assert.equal(detectFile('qrm-common.toml', ''), 'common');
  assert.equal(detectFile('C:\\x\\qrm_bank-server.toml', ''), 'bank');
  assert.equal(detectFile('qrm-common (1).toml', COMMON), 'common');
  assert.equal(detectFile('copia.toml', BANK), 'bank');
  assert.equal(detectFile('copia.json', CASH), 'cash');
});

test('file non riconosciuti: errore chiaro', () => {
  assert.throws(() => detectFile('x.txt', '{"a":1}'), ConfigFileError);
  assert.throws(() => openFile('a.toml', ''), ConfigFileError);
  assert.throws(() => openFile('a.bin', '\u0000\u0001'), ConfigFileError);
  assert.throws(() => openFile('cash.json', '[]'), ConfigFileError);
  assert.throws(() => openFile('cash.json', 'non json'), ConfigFileError);
});

test('issues: valore fuori limite e tipo sbagliato', () => {
  const s = openFile('qrm-common.toml', COMMON);
  assert.deepEqual(issues(s), []);
  setField(s, 'characters.slots', 80n);
  assert.equal(issues(s).find((i) => i.path === 'characters.slots')?.severity, 'error');
  const bad = openFile('qrm-common.toml', COMMON.replace('slots = 3', 'slots = "tre"'));
  assert.equal(issues(bad).find((i) => i.path === 'characters.slots')?.severity, 'error');
});

test('sessione nuova: file completo, riletto senza problemi', () => {
  for (const id of ['common', 'bank']) {
    const s = newSession(id);
    const out = render(s);
    assert.ok(out.length > 0);
    const headers = out.split(/\r?\n/).filter((l) => /^\s*\[.+\]\s*$/.test(l)).map((l) => l.trim());
    assert.equal(new Set(headers).size, headers.length, `sezioni duplicate: ${headers}`);
    const back = openFile(s.fileName, out);
    assert.deepEqual(issues(back), []);
    assert.deepEqual(back.unknownKeys, []);
    for (const path of s.values.keys()) assert.equal(getField(back, path).present, true, path);
  }
  assert.equal(render(newSession('cash')), CASH);
  assert.equal(downloadName(newSession('bank')), 'qrm_bank-server.toml');
});

test('cash.json: modifica, righe cambiate e problemi', () => {
  const s = openFile('cash.json', CASH);
  const model = parseCash(CASH);
  model.currencies.push({ code: 'EUR', decimals: 2, denominations: [1, 5, 10, 50], extra: {} });
  setCash(s, model);
  assert.ok(render(s).includes('"EUR": { "decimals": 2, "denominations": [1, 5, 10, 50] }'));
  assert.ok(changedLines(s).length >= 1);
  assert.deepEqual(issues(s), []);
  model.currencies[1].denominations = [2, 7];
  setCash(s, model);
  assert.equal(issues(s)[0].path, 'EUR.denominations');
});

test('downloadName è il nome del file caricato', () => {
  assert.equal(downloadName(openFile('qrm-common (1).toml', COMMON)), 'qrm-common (1).toml');
});
