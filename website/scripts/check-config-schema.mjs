// Confronta lo schema del configuratore (src/lib/config/schema.mjs) con le specifiche di configurazione Java
// del mod: chiavi e, dove nel codice sono numeri letterali, minimo e massimo. Una differenza fa fallire la build.
//
// Va lanciato da website/ (come la build): `node scripts/check-config-schema.mjs`.
import { readFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { SCHEMAS } from '../src/lib/config/schema.mjs';

const repo = resolve(process.cwd(), '..');
const SOURCES = {
  common: join(repo, 'qrm-neoforge/src/main/java/dev/qrm/neoforge/QrmConfig.java'),
  bank: join(repo, 'qrm-bank/src/main/java/dev/qrm/bank/mc/BankConfig.java'),
};

/** Argomenti (al livello più esterno) della chiamata che si apre in `from`, subito dopo la parentesi. */
function callArgs(src, from) {
  const args = [];
  let depth = 0;
  let cur = '';
  for (let i = from; i < src.length; i++) {
    const c = src[i];
    if (c === '"') {
      const start = i;
      for (i++; src[i] !== '"'; i++) if (src[i] === '\\') i++;
      cur += src.slice(start, i + 1);
      continue;
    }
    if (c === '(' || c === '[' || c === '{') depth++;
    if (c === ')' || c === ']' || c === '}') {
      if (depth === 0) break;
      depth--;
    }
    if (c === ',' && depth === 0) {
      args.push(cur.trim());
      cur = '';
    } else cur += c;
  }
  if (cur.trim()) args.push(cur.trim());
  return args;
}

/** Numero letterale Java → BigInt o number; null se non è un letterale. */
function literal(text) {
  if (text === undefined) return null;
  if (text === 'Long.MAX_VALUE') return 9223372036854775807n;
  const t = text.replace(/_/g, '').replace(/[lL]$/, '');
  if (/^-?\d+$/.test(t)) return BigInt(t);
  if (/^-?\d+\.\d+$/.test(t)) return Number(t);
  return null;
}

/** @returns {Map<string, { min: bigint|number|null, max: bigint|number|null }>} */
function javaKeys(src) {
  const out = new Map();
  const stack = [];
  const re = /\bb\.push\("([^"]+)"\)|\bb\.pop\(\)|\.(define\w*)\(/g;
  for (let m; (m = re.exec(src)); ) {
    if (m[1]) stack.push(m[1]);
    else if (!m[2]) stack.pop();
    else {
      const args = callArgs(src, re.lastIndex);
      const key = /^"(.*)"$/.exec(args[0])?.[1];
      if (key === undefined) continue;
      const ranged = m[2] === 'defineInRange';
      out.set([...stack, key].join('.'), {
        min: ranged ? literal(args[2]) : null,
        max: ranged ? literal(args[3]) : null,
      });
    }
  }
  return out;
}

const sameNum = (a, b) => (typeof a === 'bigint' || typeof b === 'bigint' ? BigInt(a) === BigInt(b) : a === b);

const errors = [];
for (const schema of SCHEMAS.filter((s) => s.kind === 'toml')) {
  const java = javaKeys(readFileSync(SOURCES[schema.id], 'utf8'));
  const fields = new Map(schema.sections.flatMap((s) => s.fields).map((f) => [f.path, f]));
  for (const path of java.keys()) {
    if (!fields.has(path)) errors.push(`${schema.fileName}: la chiave ${path} è nel codice Java ma non nello schema del configuratore`);
  }
  for (const [path, f] of fields) {
    const j = java.get(path);
    if (!j) {
      errors.push(`${schema.fileName}: la chiave ${path} è nello schema del configuratore ma non nel codice Java`);
      continue;
    }
    for (const bound of ['min', 'max']) {
      if (j[bound] !== null && (f[bound] === undefined || !sameNum(j[bound], f[bound]))) {
        errors.push(`${schema.fileName}: ${path} ${bound} è ${j[bound]} nel codice Java, ${f[bound]} nello schema`);
      }
    }
  }
}

if (errors.length) {
  console.error(`check-config-schema: ${errors.length} differenze\n` + errors.map((e) => `  - ${e}`).join('\n'));
  process.exit(1);
}
console.log('check-config-schema: lo schema del configuratore coincide con il codice Java.');
