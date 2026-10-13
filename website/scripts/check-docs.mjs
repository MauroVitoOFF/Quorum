// Confronta le tabelle di riferimento della wiki con il codice del mod, nei due sensi, e controlla lo stile.
// Un comando, una chiave o un nodo presente da una parte sola fa fallire la build (exit 1).
// Le violazioni di stile sono avvisi: vengono stampate ma non fanno fallire.
//
// Va lanciato da website/ (come la build): `node scripts/check-docs.mjs`.
import { readFileSync, readdirSync } from 'node:fs';
import { join, relative, resolve } from 'node:path';

const repo = resolve(process.cwd(), '..');
const docs = resolve(process.cwd(), 'src/content/docs/docs');
const neoforge = join(repo, 'qrm-neoforge/src/main/java');
const api = join(repo, 'qrm-api/src/main/java');

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
const codeCommands = new Set(javaIn(neoforge).flatMap((f) => [...read(f).matchAll(LITERAL)].map((m) => m[1])));
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
const codeKeys = new Set([...configKeys(find(neoforge, 'QrmConfig')), ...configKeys(find(neoforge, 'ClientConfig'))]);
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
const codeNodes = new Set([
  ...staffNodes.filter((n) => !n.endsWith('.')),
  ...actions.map((a) => `staff.player.${a}`),
  withdraw,
]);
const docNodes = new Set(
  page('riferimento/nodi-di-permesso.md').spans.filter(
    (s) => /^(staff|org)\.[a-z_.]+$/.test(s) && !prefixes.includes(s),
  ),
);
const nNodes = compare('Nodi di permesso', codeNodes, docNodes, 'riferimento/nodi-di-permesso.md');

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
  `Riferimento allineato al codice: ${nCommands} parole di comando, ${nKeys} chiavi, ${nNodes} nodi.` +
    (warnings.length ? ` ${warnings.length} avvisi di stile.` : ' Nessun avviso di stile.'),
);
