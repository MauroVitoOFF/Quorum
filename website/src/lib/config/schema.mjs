// Schema dei file di configurazione di QRM per il configuratore.
// Chiavi, tipi, limiti e predefiniti vengono dal codice Java (QrmConfig, BankConfig): `npm run check:config`
// li confronta a ogni build. I testi vengono da website/src/content/docs/docs/riferimento/configurazione.md.

/** @typedef {import('./toml.mjs').TomlValue} TomlValue */
/**
 * @typedef {object} Field
 * @property {string} path
 * @property {string} label
 * @property {string} description  testo mostrato nel portale
 * @property {string} comment      commento scritto nel file quando la chiave viene aggiunta
 * @property {'bool'|'int'|'float'|'string'|'regex'|'list'|'limitOverrides'} type
 * @property {TomlValue} default
 * @property {bigint|number} [min]
 * @property {bigint|number} [max]
 * @property {string[]} [knownItems]
 * @property {boolean} [onlyKnown]   l'elenco ammette solo `knownItems`
 * @property {string} [itemPattern]  forma delle voci libere di un elenco
 * @property {string} [emptyMeaning] cosa significa l'elenco vuoto
 * @property {boolean} [unused]      chiave non usata nella versione corrente
 */
/** @typedef {{ id: string, label: string, fields: Field[] }} Section */
/** @typedef {{ id: 'common'|'bank'|'cash', fileName: string, kind: 'toml'|'json', label: string, sections: Section[] }} FileSchema */

const LONG_MAX = 9223372036854775807n;

/** @type {FileSchema[]} */
export const SCHEMAS = [
  {
    id: 'common',
    fileName: 'qrm-common.toml',
    kind: 'toml',
    label: 'Configurazione del server',
    sections: [
      {
        id: 'characters',
        label: 'Personaggi',
        fields: [
          { path: 'characters.slots', label: 'Slot per giocatore', type: 'int', default: 3n, min: 1n, max: 50n,
            description: 'Personaggi per giocatore, da 1 a 50. Vale solo per i giocatori che entrano per la prima volta.',
            comment: 'Slot personaggio per giocatore' },
          { path: 'characters.nameMinLength', label: 'Lunghezza minima del nome', type: 'int', default: 2n, min: 1n, max: 64n,
            description: 'Lunghezza minima di nome e cognome.',
            comment: 'Lunghezza minima di nome e cognome' },
          { path: 'characters.nameMaxLength', label: 'Lunghezza massima del nome', type: 'int', default: 24n, min: 1n, max: 64n,
            description: 'Lunghezza massima di nome e cognome.',
            comment: 'Lunghezza massima di nome e cognome' },
          { path: 'characters.namePattern', label: 'Formato di nome e cognome', type: 'regex', default: "^[\\p{L}][\\p{L}' -]*$",
            description: 'Espressione regolare per nome e cognome.',
            comment: 'Espressione regolare valida per nome e cognome' },
          { path: 'characters.uniqueNames', label: 'Nomi unici', type: 'bool', default: true,
            description: 'Vieta due personaggi con lo stesso nome e cognome.',
            comment: 'Vieta due personaggi con lo stesso nome e cognome' },
        ],
      },
      {
        id: 'economy',
        label: 'Economia',
        fields: [
          { path: 'economy.defaultCurrency', label: 'Valuta predefinita', type: 'string', default: 'QRM',
            description: "La valuta usata dai comandi quando non è indicata, e mostrata nell'HUD.",
            comment: "Valuta principale: usata dai comandi quando non specificata e mostrata nell'HUD" },
        ],
      },
      {
        id: 'jobs',
        label: 'Lavori',
        fields: [
          { path: 'jobs.selfDuty', label: 'Servizio autonomo', type: 'bool', default: true,
            description: 'Se disattivato, i giocatori non cambiano il servizio con /duty né dall’hub.',
            comment: 'I giocatori possono entrare/uscire di servizio da soli' },
        ],
      },
      {
        id: 'commands',
        label: 'Comandi',
        fields: [
          { path: 'commands.shortAliases', label: 'Forme brevi dei comandi', type: 'bool', default: true,
            description: 'Registra /char, /money, /bal, /pay, /job, /duty, /org. Disattivalo se un altro mod ha comandi con lo stesso nome: restano solo quelli sotto /qrm.',
            comment: 'Registra le scorciatoie /job, /duty, /money, /char, /org e /perm, oltre ai comandi /qrm' },
        ],
      },
      {
        id: 'gui',
        label: 'Interfaccia',
        fields: [
          { path: 'gui.hud.elements', label: 'Righe dell’HUD', type: 'list', default: ['name', 'balance', 'job'],
            knownItems: ['name', 'balance', 'job'], itemPattern: '[a-z0-9_.-]+:[a-z0-9_./-]+', emptyMeaning: 'HUD nascosto',
            description: 'Le righe dell’HUD, in ordine. Oltre a quelle note puoi usare le righe dei moduli, nella forma modulo:riga (es. bank:debt).',
            comment: 'Righe dell\'HUD nell\'ordine indicato: name, balance, job e id di righe dei moduli (es. bank:debt). Elenco vuoto = HUD nascosto' },
          { path: 'gui.hub.tabs', label: 'Schede dell’hub', type: 'list', default: ['accounts', 'job', 'orgs'],
            knownItems: ['accounts', 'job', 'orgs'], onlyKnown: true, emptyMeaning: 'hub disattivato',
            description: 'Le schede dell’hub, in ordine.',
            comment: 'Schede dell\'hub, nell\'ordine indicato: accounts, job, orgs. Elenco vuoto = hub non disponibile' },
        ],
      },
      {
        id: 'database',
        label: 'Database',
        fields: [
          { path: 'database.url', label: 'URL del database', type: 'string', default: '', unused: true,
            description: 'Lascia vuoto: QRM usa il file <mondo>/qrm/qrm.db. Un database esterno non è ancora disponibile.',
            comment: 'JDBC URL. Vuoto = SQLite in <world>/qrm/qrm.db' },
          { path: 'database.user', label: 'Utente del database', type: 'string', default: '', unused: true,
            description: 'Non usata con SQLite.', comment: 'Non usata con SQLite' },
          { path: 'database.password', label: 'Password del database', type: 'string', default: '', unused: true,
            description: 'Non usata con SQLite.', comment: 'Non usata con SQLite' },
        ],
      },
    ],
  },
  {
    id: 'bank',
    fileName: 'qrm_bank-server.toml',
    kind: 'toml',
    label: 'Configurazione di Bank',
    sections: [
      {
        id: 'atm',
        label: 'Sportello (ATM)',
        fields: [
          { path: 'atm.maxDistance', label: 'Distanza massima', type: 'float', default: 6, min: 2, max: 16,
            description: 'Distanza massima dallo sportello, in blocchi.',
            comment: "Distanza massima (in blocchi) dall'ATM aperto entro cui le operazioni sono accettate" },
          { path: 'atm.minIntervalMillis', label: 'Intervallo fra operazioni', type: 'int', default: 250n, min: 0n, max: 5000n,
            description: 'Tempo minimo fra due operazioni, in millisecondi.',
            comment: 'Intervallo minimo fra due richieste dello stesso giocatore (millisecondi)' },
          { path: 'atm.historyRows', label: 'Righe dello storico', type: 'int', default: 20n, min: 1n, max: 50n,
            description: 'Movimenti mostrati nello storico.',
            comment: 'Movimenti mostrati nello storico' },
          { path: 'atm.transfersEnabled', label: 'Bonifici dallo sportello', type: 'bool', default: true,
            description: 'Abilita i bonifici dallo sportello.',
            comment: "Abilita i bonifici dall'ATM" },
          { path: 'atm.vipBalance', label: 'Saldo Cliente VIP', type: 'int', default: 100000n, min: 0n, max: LONG_MAX,
            description: 'Saldo da cui compare il badge Cliente VIP, in unità intere della valuta; 0 lo disattiva.',
            comment: "Saldo (in unita' intere della valuta) da cui il titolare vede il badge VIP; 0 = disattivato" },
        ],
      },
      {
        id: 'limits',
        label: 'Limiti di prelievo',
        fields: [
          { path: 'limits.maxPerOperation', label: 'Massimo per operazione', type: 'int', default: 100000n, min: 1n, max: LONG_MAX,
            description: 'Ritiro massimo per operazione, in unità minime.',
            comment: "Ritiro massimo per singola operazione, in unita' minime (valore predefinito)" },
          { path: 'limits.maxPerDay', label: 'Massimo in 24 ore', type: 'int', default: 500000n, min: 1n, max: LONG_MAX,
            description: 'Ritiro massimo in 24 ore, in unità minime.',
            comment: "Ritiro massimo nelle ultime 24 ore per personaggio, in unita' minime (predefinito)" },
          { path: 'limits.overrides', label: 'Limiti per valuta', type: 'limitOverrides', default: [],
            description: 'Limiti diversi per singole valute.',
            comment: 'Limiti per valuta, voci "CODICE:perOperazione:alGiorno" (es. "EUR:50000:200000")' },
        ],
      },
    ],
  },
  { id: 'cash', fileName: 'cash.json', kind: 'json', label: 'Tagli del contante (Bank)', sections: [] },
];

/** @param {'common'|'bank'|'cash'} id @returns {FileSchema} */
export function schemaFor(id) {
  const s = SCHEMAS.find((x) => x.id === id);
  if (!s) throw new Error(`schema sconosciuto: ${id}`);
  return s;
}

/** @param {'common'|'bank'|'cash'} id @param {string} path @returns {Field | undefined} */
export function findField(id, path) {
  for (const sec of schemaFor(id).sections) {
    const f = sec.fields.find((x) => x.path === path);
    if (f) return f;
  }
  return undefined;
}
