/**
 * Contenuti del sito: la scena dell'intestazione e i Modules. Ogni affermazione viene dal codice (versione 0.20.x)
 * ed è spiegata per esteso nella wiki. Il lessico è quello della wiki: personaggio, lavoro, organizzazione, permesso.
 */

/* ---------- Scena dell'intestazione: QRM, i suoi sistemi e i Modules ---------- */

export interface Block {
  id: string;
  label: string;
  glyph: 'person' | 'coin' | 'tool' | 'flag' | 'key' | 'core' | 'atm' | 'locker' | 'crown';
  kind: 'core' | 'framework' | 'module';
  /** posizione sulla griglia isometrica */
  x: number;
  y: number;
  z: number;
  title: string;
  text: string;
  /** pagina della wiki (percorso in src/content/docs/docs) */
  doc: string;
}

export const blocks: Block[] = [
  {
    id: 'core', label: 'QRM', glyph: 'core', kind: 'core', x: 2, y: 2, z: 1,
    title: 'QRM',
    text: 'Un solo file da copiare in `mods/` sul server. Tiene i dati in un database dentro la cartella del mondo e si gestisce con comandi in chat.',
    doc: '',
  },
  {
    id: 'characters', label: 'Personaggi', glyph: 'person', kind: 'framework', x: 0, y: 2, z: 0,
    title: 'Personaggi',
    text: 'Ogni giocatore ha più personaggi e ne usa uno alla volta. Lavoro, conti, permessi e organizzazioni appartengono al personaggio.',
    doc: 'sistemi/personaggi/',
  },
  {
    id: 'economy', label: 'Economia', glyph: 'coin', kind: 'framework', x: 2, y: 0, z: 0,
    title: 'Economia',
    text: 'Valute che definisci tu, un conto per personaggio e ogni movimento registrato. Si paga con `/pay`.',
    doc: 'sistemi/economia/',
  },
  {
    id: 'jobs', label: 'Lavori', glyph: 'tool', kind: 'framework', x: 4, y: 2, z: 0,
    title: 'Lavori',
    text: 'Un lavoro è un file di testo con i suoi gradi. Il personaggio entra ed esce dal servizio con `/duty`.',
    doc: 'sistemi/lavori/',
  },
  {
    id: 'orgs', label: 'Organizzazioni', glyph: 'flag', kind: 'framework', x: 2, y: 4, z: 0,
    title: 'Organizzazioni',
    text: 'Gruppi con rank, membri e un conto in comune. Ogni lavoro ha già la sua; le altre si creano con `/org`.',
    doc: 'sistemi/organizzazioni/',
  },
  {
    id: 'perms', label: 'Permessi', glyph: 'key', kind: 'framework', x: 0, y: 0, z: 0,
    title: 'Permessi',
    text: 'Il grado del lavoro dà i permessi, che valgono solo in servizio. Un operatore può concederne o negarne a un singolo personaggio.',
    doc: 'sistemi/permessi/',
  },
  {
    id: 'bank', label: 'Bank', glyph: 'atm', kind: 'module', x: 4, y: 0, z: 1,
    title: 'Bank',
    text: 'Sportelli ATM, contante fatto di oggetti e bonifici. Si installa a parte, sul server e sui client.',
    doc: 'modules/bank/',
  },
  {
    id: 'locker', label: 'Locker', glyph: 'locker', kind: 'module', x: 0, y: 4, z: 1,
    title: 'Locker',
    text: 'Un armadietto personale per ogni personaggio, aperto da un blocco del mondo. Si installa a parte, sul server.',
    doc: 'modules/locker/',
  },
  {
    id: 'boss', label: 'Boss', glyph: 'crown', kind: 'module', x: 4, y: 4, z: 1,
    title: 'Boss',
    text: 'Un menu con cui chi dirige un’azienda gestisce membri e conto, senza comandi da operatore. Si installa a parte.',
    doc: 'modules/boss/',
  },
];

/* ---------- QRM e i suoi Modules ---------- */

export interface ModuleCard {
  id: string;
  name: string;
  /** illustrazione in public/art/ (senza estensione né larghezza), oppure null per il logo di QRM */
  art: string | null;
  need: 'Necessario' | 'Facoltativo';
  text: string;
  /** dove va installato */
  where: string;
  doc: string;
  link: string;
}

export const modules: ModuleCard[] = [
  {
    id: 'qrm', name: 'QRM', art: null, need: 'Necessario',
    text: 'Personaggi, economia, lavori, organizzazioni, permessi, punti di interazione e pannello staff.',
    where: 'Sul server. Sui client è facoltativo: chi lo ha vede anche l’interfaccia.',
    doc: 'per-iniziare/installazione/', link: 'Installazione',
  },
  {
    id: 'bank', name: 'Bank', art: 'module-bank', need: 'Facoltativo',
    text: 'Sportelli ATM da piazzare nel mondo, banconote come oggetti nell’inventario e bonifici con causale.',
    where: 'Sul server e su ogni client.',
    doc: 'modules/bank/', link: 'Bank',
  },
  {
    id: 'locker', name: 'Locker', art: 'module-locker', need: 'Facoltativo',
    text: 'Un armadietto da 27 slot per ogni personaggio. Si apre da un blocco che scegli tu.',
    where: 'Sul server. Sui client è facoltativo.',
    doc: 'modules/locker/', link: 'Locker',
  },
  {
    id: 'boss', name: 'Boss', art: 'module-boss', need: 'Facoltativo',
    text: 'Chi dirige un’azienda assume, promuove, licenzia e muove il denaro del conto aziendale da un menu.',
    where: 'Sul server e sui client di chi usa il menu.',
    doc: 'modules/boss/', link: 'Boss',
  },
];
