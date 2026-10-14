/**
 * Contenuti della pagina di presentazione, per chi gestisce un server. Fatti verificati sul codice (versione 0.20.x);
 * i dettagli stanno nella wiki, a cui ogni voce rimanda. Il lessico è quello della wiki.
 */

/* ---------- I quattro pilastri ---------- */
export interface Pillar {
  /** icona in public/pixel/ */
  icon: string;
  /** indice e nome, nel kicker */
  index: string;
  title: string;
  /** cosa si vede in gioco */
  game: string;
  /** cosa serve a chi gestisce il server */
  arch: string;
  /** pagina della wiki (percorso in src/content/docs/docs) */
  doc: string;
  link: string;
}

export const pillars: Pillar[] = [
  {
    icon: 'gem_players.png', index: '01 · Identità',
    title: 'Il giocatore interpreta. Il personaggio possiede.',
    game: 'Un singolo utente può vivere più vite sullo stesso server, passando da un’identità all’altra. Ogni personaggio ha il proprio nome, cognome e data di nascita, indipendenti dall’account Minecraft.',
    arch: 'Conti, lavori, gradi e permessi appartengono sempre al personaggio attivo; con il Module Locker anche l’armadietto personale. Cambiare personaggio significa cambiare contesto in modo netto.',
    doc: 'sistemi/personaggi/', link: 'I personaggi nella wiki',
  },
  {
    icon: 'gem_organizations.png', index: '02 · Società e ruoli',
    title: 'Lavori, gerarchie e luoghi che funzionano da soli.',
    game: 'I personaggi entrano in corpi pubblici, aziende o fazioni. I permessi si attivano solo quando si è in servizio, le funzioni sono legate a blocchi fisici nella mappa e i gruppi condividono gradi e un conto in comune.',
    arch: 'Definisci la struttura dei lavori in semplici file di testo e leghi le interazioni agli edifici del tuo mondo. Con il Module Boss, chi dirige un’azienda assume, promuove e licenzia senza l’intervento dello staff.',
    doc: 'sistemi/lavori/', link: 'Lavori e organizzazioni nella wiki',
  },
  {
    icon: 'gem_economy.png', index: '03 · Economia verificabile',
    title: 'Il denaro non nasce dal nulla e non sparisce nel vuoto.',
    game: 'Dai pagamenti diretti ai conti delle organizzazioni, il denaro circola tra cittadini e aziende in modo credibile. Con il Module Bank si aggiungono sportelli ATM e banconote fisiche nell’inventario.',
    arch: 'Ogni movimento passa da controlli rigorosi e viene scritto in una transazione su database SQLite. In qualsiasi momento puoi verificare che ogni saldo del server corrisponda allo storico delle transazioni.',
    doc: 'sistemi/economia/', link: 'L’economia nella wiki',
  },
  {
    icon: 'gem_moderation.png', index: '04 · Controllo e moderazione',
    title: 'Invisibile per chi gioca, trasparente per chi amministra.',
    game: 'Separare i personaggi dà libertà al roleplay, ma lo staff mantiene il quadro completo. Con un tasto si apre in sovrimpressione il pannello: la scheda di ogni giocatore con personaggio attivo, lavoro e organizzazioni, e le note e le sanzioni.',
    arch: 'Assegni permessi granulari ai moderatori senza dover concedere l’accesso da operatore, e ogni azione che cambia qualcosa resta scritta in un registro che puoi rileggere.',
    doc: 'sistemi/pannello-staff/', link: 'Il pannello staff nella wiki',
  },
];

export const startSteps = [
  {
    tile: 'grass_top.png', title: 'Ottieni i file e installa',
    text: 'Compila il progetto dal codice sorgente e inserisci QRM, insieme ai Modules che hai scelto, nella cartella `mods`.',
    doc: 'per-iniziare/installazione/', link: 'Requisiti e installazione',
  },
  {
    tile: 'dirt.png', title: 'Primo avvio',
    text: 'Avvia il server per generare configurazioni e database, entra in gioco e crea la prima identità.',
    doc: 'per-iniziare/primo-avvio/', link: 'Guida al primo avvio',
  },
  {
    tile: 'stone.png', title: 'Configura la società',
    text: 'Definisci valute, lavori e gradi nei file di configurazione e assegna i primi ruoli.',
    doc: 'guide/creare-un-lavoro/', link: 'Creare un lavoro',
  },
];
