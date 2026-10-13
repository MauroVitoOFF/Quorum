/**
 * Contenuti della pagina di presentazione, per chi gestisce un server. Fatti verificati sul codice (versione 0.20.x);
 * i dettagli stanno nella wiki, a cui ogni voce rimanda. Il lessico è quello della wiki.
 */

/* ---------- "Una serata sul tuo server": la sera di Anna ---------- */
export interface Beat {
  icon: string; // file in public/pixel/
  /** orario del momento, nel racconto */
  time: string;
  title: string;
  /** cosa succede in gioco: una frase, dal punto di vista di Anna */
  game: string;
  /** cosa serve a te, che gestisci il server */
  you: string;
  chat?: string;
  /** pagina della wiki (percorso in src/content/docs/docs) */
  doc: string;
}

export const story: Beat[] = [
  {
    icon: 'gem_players.png', time: '21:00', title: 'Anna crea il suo personaggio',
    game: 'Sceglie nome, cognome e data di nascita. Da ora è Anna Rossi: nome e documenti appartengono al personaggio, non al suo account.',
    you: 'Niente da configurare. Ogni giocatore può avere più personaggi e usarne uno alla volta.',
    chat: '/char create Anna Rossi 1998-03-14', doc: 'sistemi/personaggi/',
  },
  {
    icon: 'gem_organizations.png', time: '21:05', title: 'Un operatore le dà un lavoro',
    game: 'La assume come agente di polizia. Anna ha ora un lavoro, un grado e i permessi di quel grado.',
    you: 'Il lavoro è un file di testo che scrivi tu, con i suoi gradi e i permessi di ciascuno. Un comando lo assegna.',
    chat: '/job set Anna_RP police officer', doc: 'sistemi/lavori/',
  },
  {
    icon: 'duty.png', time: '21:10', title: 'Anna timbra il cartellino',
    game: 'Fa clic destro su un blocco della centrale e risulta in servizio. Un secondo clic la fa uscire. I permessi del suo grado valgono solo finché è in servizio.',
    you: 'Il blocco lo scegli tu, anche di un altro mod, con un comando. Lo può usare solo chi ha quel lavoro.',
    chat: '/qrm admin point create duty job police', doc: 'sistemi/punti-di-interazione/',
  },
  {
    icon: 'cash.png', time: '21:30', title: 'Anna paga Marco',
    game: 'Gli manda 25,50 con un comando. Il denaro è del personaggio: passa dal suo conto a quello di Marco.',
    you: 'Ogni movimento viene registrato. Puoi controllare in qualsiasi momento che i saldi corrispondano allo storico.',
    chat: '/pay Marco_RP 25.50', doc: 'sistemi/economia/',
  },
  {
    icon: 'atm.png', time: '21:45', title: 'Anna passa allo sportello',
    game: 'Fa clic destro su uno sportello ATM: vede il conto, ritira banconote che finiscono nell’inventario e fa un bonifico.',
    you: 'È il Module Bank: lo installi a parte, sul server e su ogni client.',
    doc: 'modules/bank/',
  },
  {
    icon: 'gem_moderation.png', time: '22:30', title: 'Lo staff interviene',
    game: 'Un giocatore esagera in chat. Lo staff apre il pannello con F7 e, dalla sua scheda, lo silenzia e gli lascia un avvertimento, senza uscire dal gioco.',
    you: 'Ogni azione che cambia qualcosa finisce in un registro che puoi rileggere.',
    chat: '/qrm admin log', doc: 'sistemi/pannello-staff/',
  },
];

/* ---------- Cosa aggiunge al tuo server (barra rapida) ---------- */
export interface Slot {
  id: string;
  icon: string;
  name: string; // nome dell'oggetto nella barra rapida
  kicker: string;
  title: string;
  problem: string;
  what: string;
  owner: string;
  doc: string;
  link: string;
}

export const slots: Slot[] = [
  {
    id: 'characters', icon: 'gem_players.png', name: 'Personaggi', kicker: 'Identità',
    title: 'Il giocatore non è il personaggio.',
    problem: 'In un server roleplay chi gioca e chi viene interpretato sono due cose diverse. Se lavoro, soldi e permessi stanno sull’account, cambiare personaggio vuol dire perdere tutto.',
    what: 'Ogni giocatore ha più personaggi e ne usa uno alla volta. Lavoro, conti, permessi e organizzazioni appartengono al personaggio.',
    owner: 'Decidi tu quanti personaggi può avere un giocatore, quali nomi sono ammessi e se devono essere tutti diversi.',
    doc: 'sistemi/personaggi/', link: 'Personaggi',
  },
  {
    id: 'economy', icon: 'gem_economy.png', name: 'Economia', kicker: 'Economia',
    title: 'Il denaro non compare e non sparisce.',
    problem: 'Un’economia di server si rompe quando i soldi nascono dal nulla e nessuno sa più da dove vengono.',
    what: 'Ogni movimento è una transazione registrata. Anche il denaro che lo staff crea esce da un conto di sistema, e `/qrm admin audit` confronta ogni saldo con la somma dei suoi movimenti.',
    owner: 'Crei le valute con un file di testo e scegli quale usare quando non è indicata.',
    doc: 'sistemi/economia/', link: 'Economia',
  },
  {
    id: 'jobs', icon: 'gem_organizations.png', name: 'Lavori e organizzazioni', kicker: 'Lavoro e gruppi',
    title: 'Lavori con gradi, organizzazioni con rank.',
    problem: 'Polizia, ospedale, officina: ogni server li organizza a modo suo, e ogni mod finisce per reinventarli.',
    what: 'Un lavoro ha gradi, e i permessi di un grado valgono solo in servizio. Un’organizzazione ha rank, membri e un conto in comune.',
    owner: 'Definisci un lavoro in un file di testo e lo assegni con `/job set`: la sua organizzazione nasce da sola. Gang e associazioni si creano con `/org create`.',
    doc: 'sistemi/lavori/', link: 'Lavori',
  },
  {
    id: 'points', icon: 'duty.png', name: 'Punti di interazione', kicker: 'Luoghi',
    title: 'Le funzioni stanno nei luoghi, non nei comandi.',
    problem: 'Entrare in servizio con un comando, da qualsiasi punto della mappa, toglie senso al luogo di lavoro.',
    what: 'Lo staff sceglie un blocco e lo riserva a un lavoro o a un’organizzazione. Il clic destro fa entrare e uscire dal servizio; con i Modules lo stesso meccanismo apre un armadietto o il menu di un’azienda.',
    owner: 'Crei e sposti i punti con `/qrm admin point`, e per ognuno scegli il grado minimo, il nome e chi vede l’icona.',
    doc: 'sistemi/punti-di-interazione/', link: 'Punti di interazione',
  },
  {
    id: 'staff', icon: 'gem_moderation.png', name: 'Pannello staff', kicker: 'Staff',
    title: 'Un pannello che non ferma il gioco.',
    problem: 'Moderare con comandi a memoria è lento, e un’azione senza traccia non si può rivedere.',
    what: 'Con F7 il pannello compare sopra il gioco: scheda di ogni giocatore, mute, avvertimenti e note. Ogni azione che cambia qualcosa finisce nel registro.',
    owner: 'Decidi per ogni membro dello staff cosa può vedere e fare, senza renderlo operatore.',
    doc: 'sistemi/pannello-staff/', link: 'Pannello staff',
  },
];

/* ---------- Come si inizia ---------- */
export const startSteps = [
  {
    tile: 'grass_top.png', title: 'Installa',
    text: 'Copia QRM nella cartella `mods` del server, e i Modules che vuoi usare.',
    doc: 'per-iniziare/installazione/', link: 'Requisiti e installazione',
  },
  {
    tile: 'dirt.png', title: 'Avvia',
    text: 'Al primo avvio QRM crea configurazione e database. Entra, crea un personaggio e dagli del denaro.',
    doc: 'per-iniziare/primo-avvio/', link: 'Primo avvio',
  },
  {
    tile: 'stone.png', title: 'Crea il primo lavoro',
    text: 'Scrivi un file, riavvia e assegna il lavoro a un giocatore.',
    doc: 'guide/creare-un-lavoro/', link: 'Creare un lavoro',
  },
];
