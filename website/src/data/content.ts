/**
 * Contenuti del sito: il nucleo e i Modules. Ogni affermazione viene dal codice (versione 0.20.x)
 * ed è spiegata per esteso nella wiki. Il lessico è quello della wiki: personaggio, lavoro, organizzazione, permesso.
 */

/* ---------- Il nucleo ---------- */

export interface Requirement {
  label: string;
  value: string;
  /** obbligatorio: l'indicatore si evidenzia */
  required: boolean;
}

export const core = {
  badge: 'Il Nucleo · Obbligatorio',
  name: 'QRM',
  text: 'È il cuore dell’infrastruttura: gestisce identità, economia su database, lavori, organizzazioni, permessi, punti di interazione e moderazione. Tutta la logica gira in sicurezza sul server: i giocatori possono entrare anche senza avere il mod installato, mentre chi lo installa sul proprio client sblocca l’interfaccia grafica completa (HUD, schermata dei personaggi e pannello staff).',
  requirements: [
    { label: 'Server', value: 'Obbligatorio', required: true },
    { label: 'Client', value: 'Facoltativo (abilita le GUI)', required: false },
  ] as Requirement[],
  doc: 'per-iniziare/installazione/',
  link: 'Architettura e installazione',
};

/* ---------- I Modules ---------- */

export interface ModuleCard {
  id: string;
  name: string;
  /** illustrazione in public/art/ (senza estensione né larghezza) */
  art: string;
  badge: string;
  text: string;
  /** dove va installato */
  where: string;
  doc: string;
  link: string;
}

export const modules: ModuleCard[] = [
  {
    id: 'bank', name: 'Bank', art: 'module-bank', badge: 'Module · Economia fisica',
    text: 'Trasforma il denaro digitale in qualcosa di tangibile. Introduce gli sportelli ATM posizionabili nella mappa, i bonifici con causale e le banconote fisiche nell’inventario, per scambi a mano e dinamiche di gioco sul contante.',
    where: 'Server e client (aggiunge blocchi e oggetti).',
    doc: 'modules/bank/', link: 'Scopri Bank',
  },
  {
    id: 'locker', name: 'Locker', art: 'module-locker', badge: 'Module · Inventari isolati',
    text: 'Supera il limite dell’inventario unico di Minecraft, legato all’account. Permette di associare a un blocco del mondo un armadietto personale, separato per ogni personaggio, e tiene divisi gli oggetti tra una vita e l’altra.',
    where: 'Solo server (client facoltativo).',
    doc: 'modules/locker/', link: 'Scopri Locker',
  },
  {
    id: 'boss', name: 'Boss', art: 'module-boss', badge: 'Module · Autonomia delle fazioni',
    text: 'Delega la gestione delle attività direttamente ai giocatori. Offre a chi dirige un’azienda, un corpo pubblico o un’organizzazione un’interfaccia dedicata per assumere, promuovere, licenziare e amministrare il conto in comune senza chiedere aiuto allo staff.',
    where: 'Server e client (per chi usa il menu).',
    doc: 'modules/boss/', link: 'Scopri Boss',
  },
];
