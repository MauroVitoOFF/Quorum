import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parseCash, serializeCash, validateCash } from '../../src/lib/config/cash.mjs';

const DEFAULT_JSON = `{
  "QRM": { "decimals": 2, "denominations": [100, 500, 1000, 5000, 10000] }
}
`;

const cur = (code, decimals, denominations, extra = {}) => ({ code, decimals, denominations, extra });
const issuesFor = (...currencies) => validateCash({ currencies });

test('il file predefinito si riscrive identico', () => {
  assert.equal(serializeCash(parseCash(DEFAULT_JSON)), DEFAULT_JSON);
});

test('ordine delle valute e chiavi sconosciute di una valuta sono conservati', () => {
  const text = '{"ZZZ":{"decimals":0,"denominations":[1,5],"note":"x"},"AAA":{"decimals":2,"denominations":[100]}}';
  const model = parseCash(text);
  assert.deepEqual(model.currencies.map((c) => c.code), ['ZZZ', 'AAA']);
  assert.deepEqual(model.currencies[0].extra, { note: 'x' });
  const out = serializeCash(model);
  assert.ok(out.indexOf('"ZZZ"') < out.indexOf('"AAA"'));
  assert.ok(out.includes('"note": "x"'));
  assert.deepEqual(parseCash(out), model);
});

test('nessuna valuta: oggetto vuoto', () => {
  assert.equal(serializeCash({ currencies: [] }), '{}\n');
});

test('tagli validi non danno problemi', () => {
  assert.deepEqual(issuesFor(cur('QRM', 2, [100, 500, 1000, 5000, 10000])), []);
});

test('regole di CashDenominations', () => {
  const bad = {
    'taglio non multiplo': cur('EUR', 2, [100, 250]),
    'taglio duplicato': cur('EUR', 2, [100, 100]),
    'nessun taglio': cur('EUR', 2, []),
    'decimali oltre 8': cur('EUR', 9, [1]),
    'decimali non interi': cur('EUR', 1.5, [1]),
    'codice minuscolo': cur('eur', 2, [1]),
    'codice troppo corto': cur('E', 2, [1]),
    'taglio zero': cur('EUR', 2, [0]),
    'taglio oltre il massimo': cur('EUR', 2, [1_000_000_000_001]),
    'taglio non intero': cur('EUR', 2, [1.5]),
  };
  for (const [name, c] of Object.entries(bad)) {
    const issues = issuesFor(c);
    assert.ok(issues.length > 0 && issues.every((i) => i.severity === 'error'), name);
  }
  assert.ok(issuesFor(cur('EUR', 2, [1]), cur('EUR', 2, [5])).length > 0, 'codice duplicato');
  assert.deepEqual(issuesFor(cur('EUR', 2, [1_000_000_000_000])), []);
});

test('i problemi indicano la valuta', () => {
  const [i] = issuesFor(cur('EUR', 2, [100, 250]));
  assert.equal(i.path, 'EUR.denominations');
});

test('file che non è un oggetto di valute lancia un errore leggibile', () => {
  assert.throws(() => parseCash('[]'), /valute/);
  assert.throws(() => parseCash('{"A": 5}'), /A/);
  assert.throws(() => parseCash('non json'), /JSON/);
  assert.throws(() => parseCash('{"A": {"decimals": 2}}'), /denominations/);
});
