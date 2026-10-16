// Confronta le tabelle di riferimento della wiki con il codice del mod, nei due sensi, e controlla lo stile.
// Un comando, una chiave o un nodo presente da una parte sola fa fallire la build (exit 1).
// Le violazioni di stile sono avvisi: vengono stampate ma non fanno fallire.
//
// Va lanciato da website/ (come la build): `node scripts/check-docs.mjs`.
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { join, relative, resolve, sep } from 'node:path';

const repo = resolve(process.cwd(), '..');
const docs = resolve(process.cwd(), 'src/content/docs/docs');
const neoforge = join(repo, 'qrm-neoforge/src/main/java');
const api = join(repo, 'qrm-api/src/main/java');
const bank = join(repo, 'qrm-bank/src/main/java');
const boss = join(repo, 'qrm-boss/src/main/java');
const wardrobe = join(repo, 'qrm-wardrobe/src/main/java');
// Locker non ha comandi, chiavi né nodi propri.

const walk = (dir) =>
  readdirSync(dir, { withFileTypes: true }).flatMap((e) =>
    e.isDirectory() ? walk(join(dir, e.name)) : [join(dir, e.name)],
  );
const read = (file) => readFileSync(file, 'utf8');
const javaIn = (dir) => walk(dir).filter((f) => f.endsWith('.java'));
const find = (dir, name) => {
  const hit = javaIn(dir).find((f) => f.endsWith(`${name}.java`));
  if (!hit) throw new Error(`${name}.java non trovato in ${dir}: aggiorna scripts/check-docs.mjs`);
  return read(hit);
};

/** Il testo di una pagina senza frontmatter; `spans` sono i frammenti fra backtick, blocchi di codice esclusi. */
function page(path) {
  const body = read(join(docs, path)).replace(/^---\n[\s\S]*?\n---\n/, '');
  const prose = body.replace(/```[\s\S]*?```/g, '');
  return { body, prose, spans: [...prose.matchAll(/`([^`\n]+)`/g)].map((m) => m[1].replace(/\\\|/g, '|')) };
}

const errors = [];
function compare(what, inCode, inDocs, docPath) {
  const missing = [...inCode].filter((x) => !inDocs.has(x)).sort();
  const extra = [...inDocs].filter((x) => !inCode.has(x)).sort();
  if (missing.length) errors.push(`${what}: nel codice ma non in ${docPath}: ${missing.join(', ')}`);
  if (extra.length) errors.push(`${what}: in ${docPath} ma non nel codice: ${extra.join(', ')}`);
  return inCode.size;
}

// ---- comandi ----------------------------------------------------------------------------------------------
// Le parole fisse dei comandi sono i literal Brigadier. Alcune passano da un metodo di supporto che riceve il
// nome come argomento: se ne aggiungi uno, aggiungilo anche qui.
const LITERAL = /(?:Commands\.literal|permLiteral|removeNode|characterNode)\(\s*"([a-z_]+)"/g;
const codeCommands = new Set([neoforge, bank, boss, wardrobe].flatMap(javaIn).flatMap((f) => [...read(f).matchAll(LITERAL)].map((m) => m[1])));
const docCommands = new Set(
  page('riferimento/comandi.md')
    .spans.filter((s) => s.startsWith('/'))
    .flatMap((s) => s.slice(1).split(/\s+/))
    .filter((w) => /^[a-z_]+$/.test(w)),
);
const nCommands = compare('Comandi', codeCommands, docCommands, 'riferimento/comandi.md');

// ---- chiavi di configurazione -----------------------------------------------------------------------------
/** Ricostruisce le chiavi puntate seguendo push/pop del ModConfigSpec.Builder nell'ordine in cui compaiono. */
function configKeys(source) {
  const keys = [];
  const stack = [];
  for (const m of source.matchAll(/\.push\("([^"]+)"\)|\.pop\(\)|\.define\w*\(\s*"([^"]+)"/g)) {
    if (m[1]) stack.push(m[1]);
    else if (m[2]) keys.push([...stack, m[2]].join('.'));
    else stack.pop();
  }
  return keys;
}
const codeKeys = new Set([
  ...configKeys(find(neoforge, 'QrmConfig')),
  ...configKeys(find(neoforge, 'ClientConfig')),
  ...configKeys(find(bank, 'BankConfig')),
]);
const docKeys = new Set(page('riferimento/configurazione.md').spans.filter((s) => /^[a-z]+(\.[A-Za-z]+)+$/.test(s)));
const nKeys = compare('Chiavi di configurazione', codeKeys, docKeys, 'riferimento/configurazione.md');

// ---- nodi di permesso -------------------------------------------------------------------------------------
// I nodi delle azioni sui giocatori non sono costanti: nascono dai nomi dell'enum PlayerAction (tranne HISTORY,
// che usa staff.logs).
const staffNodes = [...find(api, 'StaffNodes').matchAll(/"(staff\.[a-z_.]+)"/g)].map((m) => m[1]);
const prefixes = staffNodes.filter((n) => n.endsWith('.'));
const enumBody = find(neoforge, 'PlayerAction').match(/enum PlayerAction \{([^;]+);/)[1];
const actions = [...enumBody.matchAll(/[A-Z_]+/g)].map((m) => m[0].toLowerCase()).filter((a) => a !== 'history');
const withdraw = find(api, 'OrganizationBankService').match(/WITHDRAW_NODE = "([^"]+)"/)[1];
const moduleNodes = [find(bank, 'BankPermissions'), find(boss, 'BossPermissions')].flatMap((source) =>
  [...source.matchAll(/"((?:bank|boss)\.[a-z_.]+)"/g)].map((m) => m[1]),
);
const codeNodes = new Set([
  ...staffNodes.filter((n) => !n.endsWith('.')),
  ...actions.map((a) => `staff.player.${a}`),
  withdraw,
  ...moduleNodes,
]);
const docNodes = new Set(
  page('riferimento/nodi-di-permesso.md').spans.filter(
    (s) => /^(staff|org|bank|boss)\.[a-z_.]+$/.test(s) && !prefixes.includes(s),
  ),
);
const nNodes = compare('Nodi di permesso', codeNodes, docNodes, 'riferimento/nodi-di-permesso.md');

// ---- esempi ----------------------------------------------------------------------------------------------
// Un blocco ```json sample="docs/samples/..." mostra un file vero, che i test del mod caricano con i lettori reali
// (WikiSamplesTest). Qui si controlla che la pagina mostri proprio quel file.
let nSamples = 0;
for (const file of walk(docs).filter((f) => /\.mdx?$/.test(f))) {
  const path = relative(docs, file).replace(/\\/g, '/');
  for (const m of read(file).matchAll(/```json sample="(docs\/samples\/[^"]+)"\r?\n([\s\S]*?)\r?\n```/g)) {
    nSamples++;
    let shown;
    let real;
    try {
      shown = JSON.stringify(JSON.parse(m[2]));
      real = JSON.stringify(JSON.parse(read(join(repo, m[1]))));
    } catch (e) {
      errors.push(`Esempi: ${path}: ${m[1]} non si legge (${e.message})`);
      continue;
    }
    if (shown !== real) errors.push(`Esempi: ${path}: il blocco non coincide con ${m[1]}`);
  }
}

// ---- link della vetrina -------------------------------------------------------------------------------------
// La vetrina (src/, fuori da content/) linka la wiki con docs('percorso') e con i campi `doc: 'percorso'` dei dati.
// Il validatore dei link di Starlight guarda solo le pagine della wiki: questi li controlliamo qui.
const pageExists = (slug) => {
  const base = join(docs, slug.replace(/\/$/, ''));
  return ['.md', '.mdx', '/index.md', '/index.mdx'].some((ext) => existsSync(base + ext)) ||
    (slug === '' && (existsSync(join(docs, 'index.md')) || existsSync(join(docs, 'index.mdx'))));
};
let nSiteLinks = 0;
const siteSrc = resolve(process.cwd(), 'src');
for (const file of walk(siteSrc).filter((f) => /\.(astro|ts)$/.test(f) && !f.includes(`${sep}content${sep}`))) {
  for (const m of read(file).matchAll(/\bdocs\('([^']*)'\)|\bdoc: '([^']*)'/g)) {
    const slug = m[1] ?? m[2];
    nSiteLinks++;
    if (!pageExists(slug)) {
      errors.push(`Link della vetrina: ${relative(siteSrc, file).replace(/\\/g, '/')} punta a una pagina che non esiste: '${slug}'`);
    }
  }
}

// ---- stile ------------------------------------------------------------------------------------------------
// Regole della guida editoriale (docs/superpowers/specs/2026-10-13-wiki-starlight-design.md).
const STYLE = [
  [/Questa (pagina|guida|sezione) (descrive|elenca|spiega|documenta)/, 'apertura che descrive la pagina invece del sistema'],
  [/^#{2,4} (Overview|How it works|Usage|Examples?|Limitations|Related|API)\s*$/m, 'titolo fisso in inglese'],
  [
    /\b(Characters?|Players?|Jobs?|Grades?|Organizations?|Permissions?|Accounts?|Currenc(?:y|ies)|Transactions?)\b/,
    'concetto di QRM in inglese: usa il termine italiano',
  ],
  [/\bÈ rivolta (a|ai|agli|allo)\b/, 'indicazione del destinatario'],
  [/\b(D|d)isponibile dalla \d/, 'storia interna delle versioni'],
];
const warnings = [];
for (const file of walk(docs).filter((f) => /\.mdx?$/.test(f))) {
  const path = relative(docs, file).replace(/\\/g, '/');
  const text = page(path).prose.replace(/`[^`\n]+`/g, '').replace(/^import .*$/gm, '');
  for (const [pattern, rule] of STYLE) {
    const hit = text.match(pattern);
    if (hit) warnings.push(`${path}: ${rule} («${hit[0].trim()}»)`);
  }
}

// ---- esito ------------------------------------------------------------------------------------------------
for (const w of warnings) console.warn(`avviso  ${w}`);
if (errors.length) {
  for (const e of errors) console.error(`errore  ${e}`);
  console.error('\nLa wiki e il codice non coincidono: aggiorna la pagina di riferimento oppure il codice.');
  process.exit(1);
}
console.log(
  `Riferimento allineato al codice: ${nCommands} parole di comando, ${nKeys} chiavi, ${nNodes} nodi, ${nSamples} esempi, ${nSiteLinks} link della vetrina.` +
    (warnings.length ? ` ${warnings.length} avvisi di stile.` : ' Nessun avviso di stile.'),
);
