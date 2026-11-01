// Lettore/scrittore TOML "sul posto" per i file di configurazione di QRM.
// Il file viene tenuto riga per riga: si riscrive solo il testo del valore toccato, così commenti,
// rientri, ordine, fine riga e chiavi sconosciute restano identici. Non è un parser TOML completo:
// capisce ciò che NeoForge genera (sezioni, chiavi, stringhe, interi, decimali, booleani, elenchi di stringhe).

/** @typedef {string | boolean | bigint | number | string[]} TomlValue */

/**
 * @typedef {object} Line
 * @property {string} text  la riga senza fine riga
 * @property {string} eol   la fine riga originale ('' sull'ultima riga senza a capo)
 * @property {'blank'|'comment'|'section'|'key'|'other'} kind
 * @property {string} [section] sezione a cui appartiene (per section: il proprio nome)
 * @property {string} [key]
 * @property {string} [prefix]  per key: rientro, chiave e '='
 * @property {string} [raw]     per key: testo del valore
 * @property {string} [suffix]  per key: ciò che segue il valore (commento in coda)
 */

/** @typedef {{ lines: Line[], eol: string }} TomlDoc */

const SECTION = /^(\s*)\[([^\[\]]+)\]\s*(#.*)?$/;
const KEY = /^(\s*)([A-Za-z0-9_-]+)(\s*=\s*)(.*)$/;

/** Lunghezza del valore all'inizio di `s` (stringa, elenco su una riga o parola semplice). */
function scanValue(s) {
  const c = s[0];
  if (c === '"') {
    let i = 1;
    while (i < s.length) {
      if (s[i] === '\\') i += 2;
      else if (s[i++] === '"') return i;
    }
    return s.length;
  }
  if (c === "'") {
    const j = s.indexOf("'", 1);
    return j < 0 ? s.length : j + 1;
  }
  if (c === '[') {
    let i = 1;
    while (i < s.length) {
      if (s[i] === '"' || s[i] === "'") i += scanValue(s.slice(i));
      else if (s[i] === ']') return i + 1;
      else i++;
    }
    return s.length;
  }
  const m = /^[^\s#,\]]+/.exec(s);
  return m ? m[0].length : 0;
}

const ESCAPES = { b: '\b', t: '\t', n: '\n', f: '\f', r: '\r', '"': '"', '\\': '\\' };

function decodeString(raw) {
  if (raw.startsWith("'")) return raw.slice(1, raw.endsWith("'") && raw.length > 1 ? -1 : undefined);
  const body = raw.slice(1, raw.endsWith('"') && raw.length > 1 ? -1 : undefined);
  return body.replace(/\\(u[0-9a-fA-F]{4}|U[0-9a-fA-F]{8}|.)/g, (_, e) =>
    e.length > 1 ? String.fromCodePoint(parseInt(e.slice(1), 16)) : (ESCAPES[e] ?? e),
  );
}

/** @returns {TomlValue | undefined} undefined se il valore non è fra quelli supportati */
function decode(raw) {
  if (raw.startsWith('"') || raw.startsWith("'")) return decodeString(raw);
  if (raw === 'true') return true;
  if (raw === 'false') return false;
  if (/^[+-]?\d[\d_]*$/.test(raw)) return BigInt(raw.replace(/_/g, ''));
  if (/^[+-]?\d[\d_]*(\.\d[\d_]*)?([eE][+-]?\d+)?$/.test(raw)) return Number(raw.replace(/_/g, ''));
  if (raw.startsWith('[') && raw.endsWith(']')) {
    const items = [];
    const inner = raw.slice(1, -1);
    let i = 0;
    while (i < inner.length) {
      if (/[\s,]/.test(inner[i])) { i++; continue; }
      const len = scanValue(inner.slice(i)) || 1;
      const part = inner.slice(i, i + len);
      if (part.startsWith('"') || part.startsWith("'")) items.push(decodeString(part));
      i += len;
    }
    return items;
  }
  return undefined;
}

function encodeString(s) {
  let out = '"';
  for (const ch of s) {
    const code = ch.codePointAt(0);
    if (ch === '\\') out += '\\\\';
    else if (ch === '"') out += '\\"';
    else if (ch === '\n') out += '\\n';
    else if (ch === '\r') out += '\\r';
    else if (ch === '\t') out += '\\t';
    else if (code < 0x20 || code === 0x7f) out += `\\u${code.toString(16).padStart(4, '0')}`;
    else out += ch;
  }
  return out + '"';
}

/** @param {TomlValue} v */
function encode(v) {
  if (typeof v === 'string') return encodeString(v);
  if (typeof v === 'boolean') return String(v);
  if (typeof v === 'bigint') return v.toString();
  if (typeof v === 'number') return Number.isInteger(v) ? v.toFixed(1) : String(v);
  return `[${v.map(encodeString).join(', ')}]`;
}

const same = (a, b) =>
  Array.isArray(a) && Array.isArray(b) ? a.length === b.length && a.every((x, i) => x === b[i]) : a === b;

/** @param {string} text @returns {TomlDoc} */
export function parseToml(text) {
  const parts = text.split(/(\r\n|\n)/);
  const lines = [];
  let section = '';
  for (let i = 0; i < parts.length; i += 2) {
    const t = parts[i];
    const eol = parts[i + 1] ?? '';
    /** @type {Line} */
    const line = { text: t, eol, kind: 'other', section };
    let m;
    if (/^\s*$/.test(t)) line.kind = 'blank';
    else if (/^\s*#/.test(t)) line.kind = 'comment';
    else if ((m = SECTION.exec(t))) {
      section = m[2].trim();
      line.kind = 'section';
      line.section = section;
    } else if ((m = KEY.exec(t))) {
      const rest = m[4];
      const len = scanValue(rest);
      line.kind = 'key';
      line.key = m[2];
      line.prefix = m[1] + m[2] + m[3];
      line.raw = rest.slice(0, len);
      line.suffix = rest.slice(len);
    }
    lines.push(line);
  }
  const firstEol = /\r\n|\n/.exec(text);
  return { lines, eol: firstEol ? firstEol[0] : '\n' };
}

const keyPath = (l) => (l.section ? `${l.section}.${l.key}` : l.key);
const findKey = (doc, path) => doc.lines.findIndex((l) => l.kind === 'key' && keyPath(l) === path);

/** @param {TomlDoc} doc @returns {TomlValue | undefined} */
export function getValue(doc, path) {
  const i = findKey(doc, path);
  return i < 0 ? undefined : decode(doc.lines[i].raw);
}

export const hasKey = (doc, path) => findKey(doc, path) >= 0;

/** @returns {string[]} */
export function listKeys(doc) {
  return doc.lines.filter((l) => l.kind === 'key').map(keyPath);
}

/** Inserisce righe dopo l'indice `idx` (-1 = in cima), tenendo corretti i fine riga. */
function insertAfter(doc, idx, texts) {
  const lines = doc.lines;
  const prev = lines[idx];
  const noFinalEol = !!prev && idx === lines.length - 1 && prev.eol === '';
  if (noFinalEol) prev.eol = doc.eol;
  const fresh = texts.map((text) => ({ text, eol: doc.eol, kind: 'other' }));
  if (noFinalEol) fresh[fresh.length - 1].eol = '';
  lines.splice(idx + 1, 0, ...fresh);
  // le righe nuove vanno riclassificate (sezioni, chiavi) perché le inserzioni successive le trovino
  doc.lines = parseToml(serializeToml(doc)).lines;
}

/**
 * Imposta il valore di `path`. Se la chiave c'è, cambia solo il testo del valore (e solo se è diverso);
 * altrimenti la aggiunge in fondo alla sua sezione, con `comment` come riga di commento sopra.
 * @param {TomlDoc} doc @param {string} path @param {TomlValue} value @param {string} [comment]
 */
export function setValue(doc, path, value, comment) {
  const i = findKey(doc, path);
  if (i >= 0) {
    const l = doc.lines[i];
    if (same(decode(l.raw), value)) return;
    l.raw = encode(value);
    l.text = l.prefix + l.raw + l.suffix;
    return;
  }
  const dot = path.lastIndexOf('.');
  const section = dot < 0 ? '' : path.slice(0, dot);
  const key = path.slice(dot + 1);
  const lines = doc.lines;
  const header = lines.findIndex((l) => l.kind === 'section' && l.section === section);
  if (header >= 0 || section === '') {
    let last = header;
    let indent = header >= 0 ? `${/^\s*/.exec(lines[header].text)[0]}\t` : '';
    for (let j = header + 1; j < lines.length && lines[j].kind !== 'section'; j++) {
      if (lines[j].kind === 'key') {
        last = j;
        indent = /^\s*/.exec(lines[j].text)[0];
      }
    }
    const texts = [];
    if (comment) texts.push(`${indent}#${comment}`);
    texts.push(`${indent}${key} = ${encode(value)}`);
    insertAfter(doc, last, texts);
    return;
  }
  const trailingEmpty = lines.length > 0 && lines[lines.length - 1].text === '' && lines[lines.length - 1].eol === '';
  const end = trailingEmpty ? lines.length - 2 : lines.length - 1;
  const texts = [];
  if (end >= 0 && lines[end].kind !== 'blank') texts.push('');
  texts.push(`[${section}]`);
  if (comment) texts.push(`\t#${comment}`);
  texts.push(`\t${key} = ${encode(value)}`);
  insertAfter(doc, end, texts);
}

/** @param {TomlDoc} doc */
export function serializeToml(doc) {
  return doc.lines.map((l) => l.text + l.eol).join('');
}
