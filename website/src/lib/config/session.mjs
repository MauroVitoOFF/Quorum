// Sessione di modifica del configuratore: apre un file, tiene i valori correnti, produce il file risultante
// e le righe cambiate. Non tocca il DOM: la pagina usa solo queste funzioni.

import { parseToml, getValue, listKeys, setValue, serializeToml } from './toml.mjs';
import { parseCash, serializeCash, validateCash } from './cash.mjs';
import { validateValues } from './validate.mjs';
import { schemaFor, findField } from './schema.mjs';

/** @typedef {import('./toml.mjs').TomlValue} TomlValue */
/** @typedef {import('./validate.mjs').Issue} Issue */
/** @typedef {import('./cash.mjs').CashModel} CashModel */
/**
 * @typedef {object} Session
 * @property {'common'|'bank'|'cash'} id
 * @property {string} fileName
 * @property {string} original
 * @property {boolean} isNew          partita dai valori predefiniti, senza file
 * @property {boolean} bom            il file aperto iniziava con il BOM UTF-8, da riscrivere
 * @property {Map<string, TomlValue>} values     valori correnti dei campi noti (letti dal file o impostati)
 * @property {Map<string, TomlValue>} originals  valori letti dal file all'apertura
 * @property {string[]} unknownKeys   chiavi del file che lo schema non conosce
 * @property {string[]} unreadable    chiavi note con un valore che il portale non sa leggere
 * @property {CashModel} [cash]
 * @property {CashModel | null} [cashOriginal]
 */

export class ConfigFileError extends Error {}

const DEFAULT_CASH = '{\n  "QRM": { "decimals": 2, "denominations": [100, 500, 1000, 5000, 10000] }\n}\n';
const NAMES = { 'qrm-common.toml': 'common', 'qrm_bank-server.toml': 'bank', 'cash.json': 'cash' };

const same = (a, b) =>
  Array.isArray(a) && Array.isArray(b) ? a.length === b.length && a.every((x, i) => x === b[i]) : a === b;

/** @returns {'common'|'bank'|'cash'} @throws {ConfigFileError} */
export function detectFile(fileName, text) {
  if (text.includes('\u0000')) throw new ConfigFileError('Il file non sembra un file di testo.');
  const base = fileName.split(/[\\/]/).pop().toLowerCase();
  if (NAMES[base]) return NAMES[base];
  const body = text.replace(/^﻿/, '');
  const t = body.trim();
  if (t.startsWith('{')) {
    let model;
    try {
      model = parseCash(t);
    } catch (e) {
      throw new ConfigFileError(`Il file sembra un JSON di tagli ma non è valido: ${e.message}`);
    }
    if (model.currencies.length) return 'cash';
  } else {
    // per contenuto serve almeno una chiave QRM nota: il TOML di un altro mod non va scambiato per il nostro
    const keys = listKeys(parseToml(body));
    if (keys.some((k) => findField('common', k))) return 'common';
    if (keys.some((k) => findField('bank', k))) return 'bank';
  }
  throw new ConfigFileError(
    'Non riconosco questo file. Il configuratore apre qrm-common.toml, qrm_bank-server.toml e cash.json.',
  );
}

/**
 * Byte del file → testo, solo se è UTF-8 valido: con un'altra codifica (cp1252...) i caratteri accentati dei
 * commenti verrebbero sostituiti senza avviso. Il BOM resta nel testo (lo gestisce openFile).
 * @param {Uint8Array} bytes @returns {string} @throws {ConfigFileError}
 */
export function decodeBytes(bytes) {
  try {
    return new TextDecoder('utf-8', { fatal: true, ignoreBOM: true }).decode(bytes);
  } catch {
    throw new ConfigFileError(
      'Il file non è in UTF-8 (forse è salvato con un’altra codifica): non lo modifico per non alterare le lettere accentate. Salvalo come UTF-8 e riprova.',
    );
  }
}

/** @returns {Session} */
function blank(id, fileName, original, isNew) {
  return { id, fileName, original, isNew, bom: false, values: new Map(), originals: new Map(), unknownKeys: [], unreadable: [] };
}

/** @param {string} fileName @param {string} fileText @returns {Session} @throws {ConfigFileError} */
export function openFile(fileName, fileText) {
  const bom = fileText.startsWith('\uFEFF');
  const text = bom ? fileText.slice(1) : fileText;
  const id = detectFile(fileName, text);
  const s = blank(id, fileName, text, false);
  s.bom = bom;
  if (id === 'cash') {
    try {
      s.cash = parseCash(text);
      s.cashOriginal = parseCash(text);
    } catch (e) {
      throw new ConfigFileError(e.message);
    }
    return s;
  }
  const doc = parseToml(text);
  for (const path of listKeys(doc)) {
    if (!findField(id, path)) {
      s.unknownKeys.push(path);
      continue;
    }
    const v = getValue(doc, path);
    if (v === undefined) s.unreadable.push(path);
    else {
      s.values.set(path, v);
      s.originals.set(path, v);
    }
  }
  return s;
}

/** Sessione da zero, con tutti i valori predefiniti. @param {'common'|'bank'|'cash'} id @returns {Session} */
export function newSession(id) {
  const schema = schemaFor(id);
  const s = blank(id, schema.fileName, '', true);
  if (id === 'cash') {
    s.cash = parseCash(DEFAULT_CASH);
    s.cashOriginal = null;
    return s;
  }
  for (const sec of schema.sections) for (const f of sec.fields) s.values.set(f.path, f.default);
  return s;
}

function isModified(s, path) {
  if (s.originals.has(path)) return !same(s.values.get(path), s.originals.get(path));
  return s.values.has(path);
}

/** @returns {{ value: TomlValue, present: boolean, modified: boolean, unreadable: boolean }} */
export function getField(s, path) {
  const field = findField(s.id, path);
  const value = s.values.has(path) ? s.values.get(path) : field.default;
  const unreadable = s.unreadable.includes(path);
  return { value, present: s.originals.has(path) || unreadable, modified: isModified(s, path), unreadable };
}

/** Un valore che il portale non sa leggere (es. elenco su più righe) non si modifica: resta com'è nel file. */
export function setField(s, path, value) {
  if (s.unreadable.includes(path)) return;
  s.values.set(path, value);
}

/** Torna al valore del file; per una chiave assente, al predefinito e fuori dal file. */
export function resetField(s, path) {
  if (s.originals.has(path)) s.values.set(path, s.originals.get(path));
  else if (s.isNew) s.values.set(path, findField(s.id, path).default);
  else s.values.delete(path);
}

/** @param {CashModel} model */
export function setCash(s, model) {
  s.cash = model;
}

const cashModified = (s) => s.isNew || JSON.stringify(s.cash) !== JSON.stringify(s.cashOriginal);

/** @returns {Issue[]} */
export function issues(s) {
  if (s.id === 'cash') return validateCash(s.cash);
  const out = validateValues(s.id, s.values);
  for (const path of s.unreadable) {
    out.push({ path, severity: 'warning', message: 'Valore in una forma che il configuratore non legge: resta com’è nel file.' });
  }
  return out;
}

/** Il file risultante. @returns {string} */
export function render(s) {
  return (s.bom ? '\uFEFF' : '') + renderBody(s);
}

function renderBody(s) {
  if (s.id === 'cash') return cashModified(s) ? serializeCash(s.cash) : s.original;
  const doc = parseToml(s.original);
  for (const sec of schemaFor(s.id).sections) {
    for (const f of sec.fields) {
      if (isModified(s, f.path)) setValue(doc, f.path, s.values.get(f.path), f.comment);
    }
  }
  return serializeToml(doc);
}

/** Numeri di riga (da 1) del file risultante che sono nuovi o diversi dall'originale. @returns {number[]} */
export function changedLines(s) {
  const a = s.original.split(/\r?\n/);
  const b = renderBody(s).split(/\r?\n/);
  const lcs = Array.from({ length: a.length + 1 }, () => new Array(b.length + 1).fill(0));
  for (let i = a.length - 1; i >= 0; i--) {
    for (let j = b.length - 1; j >= 0; j--) {
      lcs[i][j] = a[i] === b[j] ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
    }
  }
  const kept = new Set();
  for (let i = 0, j = 0; i < a.length && j < b.length; ) {
    if (a[i] === b[j]) {
      kept.add(j);
      i++;
      j++;
    } else if (lcs[i + 1][j] >= lcs[i][j + 1]) i++;
    else j++;
  }
  return b.map((_, j) => j).filter((j) => !kept.has(j)).map((j) => j + 1);
}

export const downloadName = (s) => s.fileName;
