import { test } from 'node:test';
import assert from 'node:assert/strict';
import { findField } from '../../src/lib/config/schema.mjs';
import { validateField, validateValues, parseOverride, formatOverride } from '../../src/lib/config/validate.mjs';

const f = (id, path) => findField(id, path);

test('interi: limiti inclusi, fuori limite è errore', () => {
  const slots = f('common', 'characters.slots');
  assert.equal(validateField(slots, 3n), null);
  assert.equal(validateField(slots, 0n)?.severity, 'error');
  assert.equal(validateField(slots, 51n)?.severity, 'error');
  assert.equal(validateField(f('bank', 'limits.maxPerDay'), 0n)?.severity, 'error');
  assert.equal(validateField(f('bank', 'limits.maxPerDay'), 9223372036854775807n), null);
  assert.equal(validateField(f('bank', 'limits.maxPerDay'), 9223372036854775808n)?.severity, 'error');
});

test('decimali: atm.maxDistance 2.0–16.0', () => {
  const d = f('bank', 'atm.maxDistance');
  assert.equal(validateField(d, 6), null);
  assert.equal(validateField(d, 1.5)?.severity, 'error');
  assert.equal(validateField(d, 16.5)?.severity, 'error');
  assert.equal(validateField(d, Number.NaN)?.severity, 'error');
});

test('righe dell’HUD: note, id di modulo, elenco vuoto', () => {
  const hud = f('common', 'gui.hud.elements');
  assert.equal(validateField(hud, ['name', 'bank:debt']), null);
  assert.equal(validateField(hud, []), null);
  assert.equal(validateField(hud, ['Bank:Debt'])?.severity, 'error');
  assert.equal(validateField(hud, ['x'])?.severity, 'error');
});

test('schede dell’hub: solo quelle note', () => {
  const tabs = f('common', 'gui.hub.tabs');
  assert.equal(validateField(tabs, ['orgs', 'job']), null);
  assert.equal(validateField(tabs, ['bank:debt'])?.severity, 'error');
});

test('limiti per valuta: formato CODICE:perOperazione:alGiorno', () => {
  const o = f('bank', 'limits.overrides');
  assert.equal(validateField(o, ['EUR:50000:200000']), null);
  for (const bad of ['EUR:5:', 'EUR:a:b', 'EUR:1:2:3', 'EUR:0:5', ':1:2', 'EUR:1:99999999999999999999']) {
    assert.equal(validateField(o, [bad])?.severity, 'error', bad);
  }
  assert.deepEqual(parseOverride('EUR:50000:200000'), { code: 'EUR', perOperation: 50000n, perDay: 200000n });
  assert.equal(parseOverride('EUR:5:'), null);
  assert.equal(formatOverride({ code: 'EUR', perOperation: 50000n, perDay: 200000n }), 'EUR:50000:200000');
});

test('namePattern: non compilabile nel browser è solo un avviso', () => {
  const p = f('common', 'characters.namePattern');
  assert.equal(validateField(p, '(')?.severity, 'warning');
  assert.equal(validateField(p, '^[\\p{L}]+$'), null);
});

test('valuta predefinita vuota è errore, i campi database vuoti no', () => {
  assert.equal(validateField(f('common', 'economy.defaultCurrency'), '')?.severity, 'error');
  assert.equal(validateField(f('common', 'economy.defaultCurrency'), 'EUR'), null);
  assert.equal(validateField(f('common', 'database.url'), ''), null);
});

test('tipo sbagliato è errore', () => {
  assert.equal(validateField(f('common', 'characters.uniqueNames'), 'sì')?.severity, 'error');
  assert.equal(validateField(f('common', 'characters.slots'), 3)?.severity, 'error');
});

test('nome minimo maggiore del massimo è errore sul minimo', () => {
  const issues = validateValues('common', new Map([
    ['characters.nameMinLength', 30n],
    ['characters.nameMaxLength', 24n],
  ]));
  const i = issues.find((x) => x.path === 'characters.nameMinLength');
  assert.equal(i?.severity, 'error');
  assert.deepEqual(validateValues('common', new Map([['characters.nameMinLength', 2n], ['characters.nameMaxLength', 24n]])), []);
});
