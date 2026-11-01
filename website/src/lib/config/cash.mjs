// Modello di config/qrm_bank/cash.json (tagli del contante per valuta), con le regole di CashDenominations.java.

/** @typedef {import('./validate.mjs').Issue} Issue */
/** @typedef {{ code: string, decimals: number, denominations: number[], extra: Record<string, unknown> }} CashCurrency */
/** @typedef {{ currencies: CashCurrency[] }} CashModel */

const CODE = /^[A-Z0-9_]{2,16}$/;
const MAX_DENOMINATION = 1_000_000_000_000;

const isObject = (v) => typeof v === 'object' && v !== null && !Array.isArray(v);
const asNumber = (v) => (typeof v === 'number' ? v : typeof v === 'string' && v.trim() !== '' ? Number(v) : Number.NaN);

/**
 * @param {string} text
 * @returns {CashModel}
 * @throws {Error} con un messaggio italiano se il testo non è un oggetto JSON di valute
 */
export function parseCash(text) {
  let root;
  try {
    root = JSON.parse(text);
  } catch (e) {
    throw new Error(`Il file non è un JSON valido: ${e.message}`);
  }
  if (!isObject(root)) throw new Error('Il file deve essere un oggetto JSON che elenca le valute.');
  const currencies = Object.entries(root).map(([code, v]) => {
    if (!isObject(v)) throw new Error(`${code}: deve essere un oggetto con decimals e denominations.`);
    if (!('decimals' in v) || !('denominations' in v)) throw new Error(`${code}: servono decimals e denominations.`);
    if (!Array.isArray(v.denominations)) throw new Error(`${code}: denominations deve essere un elenco di numeri.`);
    const { decimals, denominations, ...extra } = v;
    return { code, decimals: asNumber(decimals), denominations: denominations.map(asNumber), extra };
  });
  return { currencies };
}

/** Una valuta per riga, come nel file predefinito della mod. @param {CashModel} model */
export function serializeCash(model) {
  if (model.currencies.length === 0) return '{}\n';
  const rows = model.currencies.map((c) => {
    const body = [
      `"decimals": ${JSON.stringify(c.decimals)}`,
      `"denominations": [${c.denominations.map((d) => JSON.stringify(d)).join(', ')}]`,
      ...Object.entries(c.extra).map(([k, v]) => `${JSON.stringify(k)}: ${JSON.stringify(v)}`),
    ].join(', ');
    return `  ${JSON.stringify(c.code)}: { ${body} }`;
  });
  return `{\n${rows.join(',\n')}\n}\n`;
}

/** @param {CashModel} model @returns {Issue[]} */
export function validateCash(model) {
  /** @type {Issue[]} */
  const issues = [];
  const error = (path, message) => issues.push({ path, severity: 'error', message });
  const seen = new Set();
  for (const c of model.currencies) {
    const code = c.code;
    if (!CODE.test(code)) error(code, 'Il codice valuta deve avere da 2 a 16 caratteri fra A-Z, 0-9 e _.');
    if (seen.has(code)) error(code, 'Codice valuta ripetuto.');
    seen.add(code);
    if (!Number.isInteger(c.decimals) || c.decimals < 0 || c.decimals > 8) {
      error(`${code}.decimals`, 'I decimali devono essere un intero da 0 a 8.');
    }
    const path = `${code}.denominations`;
    if (c.denominations.length === 0) {
      error(path, 'Serve almeno un taglio.');
      continue;
    }
    const bad = c.denominations.find((d) => !Number.isInteger(d) || d <= 0 || d > MAX_DENOMINATION);
    if (bad !== undefined) {
      error(path, `Taglio non valido (${bad}): deve essere un intero da 1 a ${MAX_DENOMINATION}.`);
      continue;
    }
    const ascending = [...c.denominations].sort((a, b) => a - b);
    for (let i = 1; i < ascending.length; i++) {
      if (ascending[i] === ascending[i - 1]) {
        error(path, `Taglio ripetuto: ${ascending[i]}.`);
        break;
      }
      if (ascending[i] % ascending[i - 1] !== 0) {
        error(path, `${ascending[i]} non è multiplo di ${ascending[i - 1]}: ogni taglio deve essere multiplo del precedente.`);
        break;
      }
    }
  }
  return issues;
}
