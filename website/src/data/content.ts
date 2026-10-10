/**
 * Contenuti del sito. Ogni affermazione è tratta dalla wiki QRM-docs o dal codice
 * (versione 0.14.x). Per cambiare un testo si modifica solo questo file.
 */
import { docs } from '../config/links';

/* ---------- Scena hero: i blocchi del framework ---------- */

export interface Block {
  id: string;
  label: string;
  glyph: 'person' | 'coin' | 'tool' | 'flag' | 'key' | 'core' | 'atm';
  kind: 'core' | 'framework' | 'module';
  /** posizione sulla griglia isometrica */
  x: number;
  y: number;
  z: number;
  title: string;
  text: string;
  doc: string;
}

export const blocks: Block[] = [
  {
    id: 'core', label: 'Core', glyph: 'core', kind: 'core', x: 2, y: 2, z: 1,
    title: 'Core',
    text: 'Il nucleo del framework: i servizi dell’API, la persistenza in SQLite e il bus di eventi. Un solo jar, `qrm`, da copiare in `mods/`.',
    doc: 'intro/moduli',
  },
  {
    id: 'characters', label: 'Characters', glyph: 'person', kind: 'framework', x: 0, y: 2, z: 0,
    title: 'Characters',
    text: 'Un Player ha più Characters e ne usa uno alla volta. Job, Accounts, Permissions e Organizations appartengono al Character, non al Player.',
    doc: 'server-owner/personaggi',
  },
  {
    id: 'economy', label: 'Economy', glyph: 'coin', kind: 'framework', x: 2, y: 0, z: 0,
    title: 'Economy',
    text: 'Currencies, Accounts e Transactions. Ogni Transaction è registrata e ha una chiave di idempotenza: ripeterla non duplica il pagamento.',
    doc: 'server-owner/economia',
  },
  {
    id: 'jobs', label: 'Jobs', glyph: 'tool', kind: 'framework', x: 4, y: 2, z: 0,
    title: 'Jobs e Grades',
    text: 'Jobs definiti in JSON, con Grades. Il Character entra e esce di servizio con `/duty`.',
    doc: 'server-owner/lavori',
  },
  {
    id: 'orgs', label: 'Organizations', glyph: 'flag', kind: 'framework', x: 2, y: 4, z: 0,
    title: 'Organizations',
    text: 'Gruppi con Ranks, membri e un Account in comune, gestiti con `/org`.',
    doc: 'server-owner/organizzazioni',
  },
  {
    id: 'perms', label: 'Permissions', glyph: 'key', kind: 'framework', x: 0, y: 0, z: 0,
    title: 'Permissions',
    text: 'Nodi di Permission per Character (`/perm grant`, `deny`, `clear`) e Permissions dello staff per account.',
    doc: 'server-owner/permessi',
  },
  {
    id: 'bank', label: 'Bank', glyph: 'atm', kind: 'module', x: 4, y: 0, z: 1,
    title: 'Bank, un Module',
    text: '`qrm_bank` è un mod separato che usa solo l’API pubblica: Account bancario per Character, ATM, contante fisico e bonifici. Facoltativo.',
    doc: 'modules/bank/',
  },
];

/* ---------- Architettura: le parti del jar `qrm` ---------- */

export interface Part {
  id: string;
  name: string;
  where: 'jar' | 'module';
  role: string;
  detail: string;
  install: string;
}

export const parts: Part[] = [
  {
    id: 'api', name: 'qrm-api', where: 'jar', role: 'L’API pubblica',
    detail: 'Interfacce, eventi e tipi (`dev.qrm.api`), senza dipendenze da Minecraft. È l’unica dipendenza di compilazione per chi scrive un Module. Segue il semantic versioning.',
    install: 'Dentro il jar `qrm`, come jar-in-jar.',
  },
  {
    id: 'core', name: 'qrm-core', where: 'jar', role: 'Il Core',
    detail: 'L’implementazione dei servizi dell’API e il database SQLite. È un dettaglio interno: nessun Module deve dipenderne.',
    install: 'Dentro il jar `qrm`, come jar-in-jar.',
  },
  {
    id: 'client-api', name: 'qrm-client-api', where: 'jar', role: 'L’API client',
    detail: 'Righe HUD, schede dell’hub, frammenti di tema, sezioni e azioni del pannello staff, per i Modules con interfaccia.',
    install: 'Le classi sono nel jar `qrm`.',
  },
  {
    id: 'neoforge', name: 'qrm-neoforge', where: 'jar', role: 'L’integrazione con NeoForge',
    detail: 'Comandi, configurazione, rete, GUI, HUD e pannello staff. Mette insieme le altre tre parti nel jar `qrm`.',
    install: 'Il mod `qrm`.',
  },
  {
    id: 'bank', name: 'qrm-bank', where: 'module', role: 'Module ufficiale: Bank',
    detail: 'Mod separato (`qrm_bank`) costruito con gli stessi strumenti che QRM offre a tutti. Il framework non dipende da nessun Module.',
    install: 'Jar separato. Installalo su server e client, alla stessa versione di `qrm`.',
  },
  {
    id: 'example', name: 'qrm-example', where: 'module', role: 'Module di esempio',
    detail: 'Registra una Currency, un Job, handler di eventi e un frammento di tema. È per chi sviluppa: non va installato su un server di gioco.',
    install: 'Solo per lo sviluppo.',
  },
];

/* ---------- Comandi ---------- */

export interface Cmd {
  cmd: string;
  who: 'Players' | 'operatori' | 'membri e operatori';
  what: string;
  area: string;
}

export const commands: Cmd[] = [
  { cmd: '/char create <first> <last> <birth>', who: 'Players', what: 'Crea un Character. `<birth>` è AAAA-MM-GG.', area: 'Characters' },
  { cmd: '/char list', who: 'Players', what: 'Elenca i propri Characters.', area: 'Characters' },
  { cmd: '/char select <n>', who: 'Players', what: 'Rende attivo il Character numero n.', area: 'Characters' },
  { cmd: '/char archive <n>', who: 'Players', what: 'Archivia il Character numero n.', area: 'Characters' },
  { cmd: '/money', who: 'Players', what: 'Mostra i saldi del Character attivo.', area: 'Economy' },
  { cmd: '/money pay <player> <amount> [currency]', who: 'Players', what: 'Paga il Character attivo di un altro Player.', area: 'Economy' },
  { cmd: '/money give <player> <amount> [currency]', who: 'operatori', what: 'Crea denaro dall’Account di sistema verso il Character attivo.', area: 'Economy' },
  { cmd: '/money take <player> <amount> [currency]', who: 'operatori', what: 'Sposta denaro dal Character attivo all’Account di sistema.', area: 'Economy' },
  { cmd: '/job', who: 'Players', what: 'Mostra Job e Grade del Character attivo.', area: 'Jobs' },
  { cmd: '/duty', who: 'Players', what: 'Entra o esce di servizio (se `jobs.selfDuty` è true).', area: 'Jobs' },
  { cmd: '/job set <player> <job> <grade>', who: 'operatori', what: 'Assegna Job e Grade.', area: 'Jobs' },
  { cmd: '/qrm admin point create <type> job|org <owner> [grade]', who: 'operatori', what: 'Marca il blocco inquadrato come punto di interazione (0.14). L’icona parte dalla faccia che guardi.', area: 'Punti' },
  { cmd: '/qrm admin point set face|grade|public|name <value>', who: 'operatori', what: 'Cambia faccia dell’icona, grado minimo, visibilità o nome del punto inquadrato.', area: 'Punti' },
  { cmd: '/qrm admin point list|info|move|remove', who: 'operatori', what: 'Elenca, ispeziona, sposta o rimuove i punti.', area: 'Punti' },
  { cmd: '/org', who: 'Players', what: 'Elenca le Organizations del Character attivo.', area: 'Organizations' },
  { cmd: '/org create <id> <label>', who: 'operatori', what: 'Crea un’Organization.', area: 'Organizations' },
  { cmd: '/perm grant <player> <node>', who: 'operatori', what: 'Concede il nodo al Character attivo.', area: 'Permissions' },
  { cmd: '/qrm admin audit', who: 'operatori', what: 'Verifica che i saldi coincidano con le Transactions.', area: 'Staff' },
  { cmd: '/qrm admin log [n]', who: 'operatori', what: 'Le ultime azioni dello staff (predefinito 20).', area: 'Staff' },
  { cmd: '/qrm admin staff grant <player> <node>', who: 'operatori', what: 'Concede un nodo del pannello staff all’account.', area: 'Staff' },
  { cmd: '/bank atm give [player]', who: 'operatori', what: 'Con Bank: dà un ATM.', area: 'Bank' },
  { cmd: '/bank audit', who: 'operatori', what: 'Con Bank: controlla il registro e il contante in circolazione.', area: 'Bank' },
];

/* ---------- Quick start ---------- */

export const quickstart = {
  dev: {
    build: `./gradlew build`,
    buildNote: 'Il jar del mod si trova in `qrm-neoforge/build/libs/`. Serve un JDK 25.',
    publish: `./gradlew publishToMavenLocal`,
    publishNote: 'QRM non pubblica ancora gli artefatti su un repository Maven: `qrm-api` si ottiene in locale.',
    gradle: `dependencies {
    compileOnly 'dev.qrm:qrm-api:{version}'
}`,
    toml: `[[dependencies.mymod]]
modId = "qrm"
type = "required"
versionRange = "[{version},)"
ordering = "AFTER"
side = "BOTH"`,
    java: `@Mod("mymod")
public final class MyMod {
    public MyMod(IEventBus modBus) {
        QRM.get(CurrencyRegistry.class).register(new Currency("EXC", "E", 2));

        QRM.events().register(TransactionPostEvent.class, "mymod", e ->
                LOGGER.info("Transaction {}", e.transaction().id()));
    }
}`,
  },
};

/* ---------- Compatibilità ---------- */

export const limits = [
  'QRM è alla versione 0.x: nessuna parte è ancora Stable. Comandi, configurazione e API possono cambiare tra una minor e l’altra.',
  'Funziona solo con Minecraft 26.2 e NeoForge 26.2.0.88 o successiva.',
  'Un client che ha QRM deve avere la stessa versione del server. Un client senza QRM entra comunque.',
  'Oggi è incluso solo il driver SQLite: un database esterno non è disponibile.',
  'Nessun repository Maven pubblico per l’API.',
];

export const docLinks = {
  intro: docs('/'),
  owner: docs('server-owner/'),
  dev: docs('sviluppatori/'),
  modules: docs('modules/'),
  reference: docs('riferimento/'),
  stability: docs('intro/stabilita'),
};
