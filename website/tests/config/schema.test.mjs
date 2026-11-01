import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { parseToml, listKeys } from '../../src/lib/config/toml.mjs';
import { SCHEMAS, schemaFor, findField } from '../../src/lib/config/schema.mjs';

const fx = (name) => readFileSync(new URL(`./fixtures/${name}`, import.meta.url), 'utf8');

test('ogni chiave dei file reali generati dalla mod esiste nello schema', () => {
  for (const [id, file] of [['common', 'qrm-common.toml'], ['bank', 'qrm_bank-server.toml']]) {
    for (const path of listKeys(parseToml(fx(file)))) {
      assert.ok(findField(id, path), `${id}: ${path} manca nello schema`);
    }
  }
});

test('i tre file sono descritti', () => {
  assert.deepEqual(SCHEMAS.map((s) => s.id), ['common', 'bank', 'cash']);
  assert.equal(schemaFor('cash').kind, 'json');
  assert.equal(schemaFor('common').fileName, 'qrm-common.toml');
  assert.equal(schemaFor('bank').fileName, 'qrm_bank-server.toml');
});

test('limiti e predefiniti del codice Java', () => {
  assert.equal(findField('bank', 'limits.maxPerDay').max, 9223372036854775807n);
  const slots = findField('common', 'characters.slots');
  assert.equal(slots.min, 1n);
  assert.equal(slots.max, 50n);
  assert.equal(slots.default, 3n);
  assert.equal(findField('bank', 'atm.maxDistance').min, 2);
  assert.equal(findField('bank', 'atm.maxDistance').max, 16);
  assert.equal(findField('bank', 'atm.vipBalance').default, 100000n);
});

test('le righe dell’HUD ammettono gli id dei moduli', () => {
  const hud = findField('common', 'gui.hud.elements');
  assert.equal(hud.itemPattern, '[a-z0-9_.-]+:[a-z0-9_./-]+');
  assert.deepEqual(hud.knownItems, ['name', 'balance', 'job']);
  assert.deepEqual(findField('common', 'gui.hub.tabs').knownItems, ['accounts', 'job', 'orgs']);
});

test('ogni campo ha etichetta, descrizione e commento', () => {
  for (const s of SCHEMAS) for (const sec of s.sections) for (const f of sec.fields) {
    assert.ok(f.label && f.description && f.comment, `${f.path} incompleto`);
  }
});
