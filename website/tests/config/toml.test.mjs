import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { parseToml, getValue, hasKey, listKeys, setValue, serializeToml } from '../../src/lib/config/toml.mjs';

const fx = (name) => readFileSync(new URL(`./fixtures/${name}`, import.meta.url), 'utf8');
const COMMON = fx('qrm-common.toml');
const BANK = fx('qrm_bank-server.toml');

test('il file riletto e riscritto senza modifiche è identico byte per byte', () => {
  assert.equal(serializeToml(parseToml(COMMON)), COMMON);
  assert.equal(serializeToml(parseToml(BANK)), BANK);
});

test('legge interi, decimali, booleani, stringhe ed elenchi', () => {
  const c = parseToml(COMMON);
  assert.equal(getValue(c, 'characters.slots'), 3n);
  assert.equal(getValue(c, 'characters.uniqueNames'), true);
  assert.deepEqual(getValue(c, 'gui.hub.tabs'), ['accounts', 'job', 'orgs']);
  assert.equal(getValue(c, 'characters.namePattern'), "^[\\p{L}][\\p{L}' -]*$");
  assert.equal(getValue(parseToml(BANK), 'atm.maxDistance'), 6);
  assert.deepEqual(getValue(parseToml(BANK), 'limits.overrides'), []);
  assert.equal(getValue(c, 'nope.nope'), undefined);
  assert.equal(hasKey(c, 'gui.hud.elements'), true);
  assert.ok(listKeys(c).includes('commands.shortAliases'));
});

test('setValue cambia solo la riga toccata e conserva CRLF', () => {
  const doc = parseToml(COMMON);
  setValue(doc, 'characters.slots', 5n);
  const out = serializeToml(doc);
  const a = COMMON.split('\r\n');
  const b = out.split('\r\n');
  assert.equal(b.length, a.length);
  const diff = b.map((l, i) => (l === a[i] ? -1 : i)).filter((i) => i >= 0);
  assert.equal(diff.length, 1);
  assert.equal(b[diff[0]], '\tslots = 5');
  assert.equal(out.replace(/\r\n/g, '').includes('\n'), false);
});

test('setValue con lo stesso valore non cambia nulla', () => {
  const doc = parseToml(BANK);
  setValue(doc, 'atm.maxDistance', 6);
  assert.equal(serializeToml(doc), BANK);
});

test('setValue aggiunge una chiave assente dopo l’ultima della sezione, col rientro e il commento', () => {
  const doc = parseToml(BANK);
  setValue(doc, 'atm.vipBalance', 500n, 'Saldo da cui compare il badge VIP');
  const out = serializeToml(doc);
  const lines = out.split(/\r?\n/);
  const i = lines.indexOf('\ttransfersEnabled = true');
  assert.ok(i >= 0);
  assert.equal(lines[i + 1], '\t#Saldo da cui compare il badge VIP');
  assert.equal(lines[i + 2], '\tvipBalance = 500');
  assert.equal(out.split('\n').length, out.split('\r\n').length, 'fine riga omogenea come nell’originale');
  assert.equal(getValue(parseToml(out), 'atm.vipBalance'), 500n);
});

test('setValue crea la sezione mancante in fondo al file', () => {
  const doc = parseToml('[a]\n\tx = 1\n');
  setValue(doc, 'b.y', true);
  assert.equal(serializeToml(doc), '[a]\n\tx = 1\n\n[b]\n\ty = true\n');
});

test('i valori Long oltre 2^53 non perdono cifre', () => {
  const doc = parseToml(BANK);
  setValue(doc, 'limits.maxPerDay', 9223372036854775807n);
  assert.equal(getValue(parseToml(serializeToml(doc)), 'limits.maxPerDay'), 9223372036854775807n);
});

test('namePattern: i backslash sono raddoppiati nel file e tornano singoli alla lettura', () => {
  const doc = parseToml(COMMON);
  setValue(doc, 'characters.namePattern', '^\\d+$');
  const out = serializeToml(doc);
  assert.ok(out.includes('namePattern = "^\\\\d+$"'));
  assert.equal(getValue(parseToml(out), 'characters.namePattern'), '^\\d+$');
});

test('elenchi: scrittura con virgolette e elenco vuoto', () => {
  const doc = parseToml(COMMON);
  setValue(doc, 'gui.hud.elements', ['name', 'bank:debt']);
  assert.ok(serializeToml(doc).includes('elements = ["name", "bank:debt"]'));
  setValue(doc, 'gui.hub.tabs', []);
  assert.ok(serializeToml(doc).includes('tabs = []'));
});

test('un file senza chiavi o vuoto non fa crashare il lettore', () => {
  assert.equal(serializeToml(parseToml('')), '');
  assert.deepEqual(listKeys(parseToml('# solo commenti\n')), []);
});
