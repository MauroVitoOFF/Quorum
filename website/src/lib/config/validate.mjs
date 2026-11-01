// Validazione dei valori del configuratore, con le stesse regole del codice Java della mod.

/** @typedef {import('./toml.mjs').TomlValue} TomlValue */
/** @typedef {import('./schema.mjs').Field} Field */
/** @typedef {{ path: string, severity: 'error' | 'warning', message: string }} Issue */

import { findField } from './schema.mjs';

const LONG_MAX = 9223372036854775807n;

/** @returns {Issue} */
const error = (path, message) => ({ path, severity: 'error', message });

/**
 * Voce di `limits.overrides`: "CODICE:perOperazione:alGiorno", due interi positivi (Long) come in LimitsProvider.
 * @param {string} entry
 * @returns {{ code: string, perOperation: bigint, perDay: bigint } | null}
 */
export function parseOverride(entry) {
  const p = entry.split(':');
  if (p.length !== 3 || p[0] === '') return null;
  const num = (s) => {
    if (!/^\+?\d+$/.test(s)) return null;
    const n = BigInt(s);
    return n > 0n && n <= LONG_MAX ? n : null;
  };
  const perOperation = num(p[1]);
  const perDay = num(p[2]);
  return perOperation === null || perDay === null ? null : { code: p[0], perOperation, perDay };
}

/** @param {{ code: string, perOperation: bigint, perDay: bigint }} o */
export const formatOverride = (o) => `${o.code}:${o.perOperation}:${o.perDay}`;

const fmt = (n) => String(n);

/**
 * @param {Field} field @param {TomlValue} value
 * @returns {Issue | null}
 */
export function validateField(field, value) {
  const path = field.path;
  switch (field.type) {
    case 'bool':
      return typeof value === 'boolean' ? null : error(path, 'Deve essere sì o no.');
    case 'int': {
      if (typeof value !== 'bigint') return error(path, 'Deve essere un numero intero.');
      if (field.min !== undefined && value < BigInt(field.min)) return error(path, `Il minimo è ${fmt(field.min)}.`);
      if (field.max !== undefined && value > BigInt(field.max)) return error(path, `Il massimo è ${fmt(field.max)}.`);
      return null;
    }
    case 'float': {
      if (typeof value !== 'number' || !Number.isFinite(value)) return error(path, 'Deve essere un numero.');
      if (field.min !== undefined && value < Number(field.min)) return error(path, `Il minimo è ${fmt(field.min)}.`);
      if (field.max !== undefined && value > Number(field.max)) return error(path, `Il massimo è ${fmt(field.max)}.`);
      return null;
    }
    case 'string':
      if (typeof value !== 'string') return error(path, 'Deve essere un testo.');
      return value === '' && field.default !== '' ? error(path, 'Non può essere vuoto.') : null;
    case 'regex': {
      if (typeof value !== 'string') return error(path, 'Deve essere un testo.');
      try {
        new RegExp(value, 'u');
        return null;
      } catch {
        return {
          path,
          severity: 'warning',
          message: 'Il browser non riconosce questa espressione: potrebbe comunque essere valida per Java. Controllala.',
        };
      }
    }
    case 'list': {
      if (!Array.isArray(value)) return error(path, 'Deve essere un elenco.');
      const pattern = field.itemPattern ? new RegExp(`^(?:${field.itemPattern})$`) : null;
      for (const item of value) {
        if (field.knownItems?.includes(item)) continue;
        if (field.onlyKnown) return error(path, `«${item}» non è una voce valida. Valide: ${field.knownItems.join(', ')}.`);
        if (!pattern || !pattern.test(item)) {
          return error(path, `«${item}» non è valida: usa ${field.knownItems?.join(', ')} oppure una riga di modulo nella forma modulo:riga.`);
        }
      }
      return null;
    }
    case 'limitOverrides': {
      if (!Array.isArray(value)) return error(path, 'Deve essere un elenco.');
      for (const item of value) {
        if (!parseOverride(item)) {
          return error(path, `«${item}» non è valida: usa CODICE:perOperazione:alGiorno, con due numeri maggiori di zero.`);
        }
      }
      return null;
    }
    default:
      return null;
  }
}

/**
 * Valida i valori presenti (campo per campo) e le regole fra campi.
 * @param {'common' | 'bank'} fileId @param {Map<string, TomlValue>} values
 * @returns {Issue[]}
 */
export function validateValues(fileId, values) {
  /** @type {Issue[]} */
  const issues = [];
  for (const [path, value] of values) {
    const field = findField(fileId, path);
    const issue = field && validateField(field, value);
    if (issue) issues.push(issue);
  }
  if (fileId === 'common') {
    const min = values.get('characters.nameMinLength');
    const max = values.get('characters.nameMaxLength');
    if (typeof min === 'bigint' && typeof max === 'bigint' && min > max) {
      issues.push(error('characters.nameMinLength', 'La lunghezza minima non può superare quella massima: nessun nome sarebbe valido.'));
    }
  }
  return issues;
}
