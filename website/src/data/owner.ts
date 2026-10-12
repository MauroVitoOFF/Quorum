/**
 * Contenuti pensati per chi gestisce un server (nessuna programmazione).
 * Fatti verificati sulla wiki QRM-docs, versione 0.19.x.
 */

/* ---------- "Una serata sul server" ---------- */
export interface Beat {
  icon: string; // file in public/pixel/
  step: string;
  title: string;
  text: string;
  chat?: string;
  who: string;
  doc: string;
}

export const story: Beat[] = [
  {
    icon: 'gem_players.png', step: '1', title: 'Anna entra e crea il suo personaggio',
    text: 'Ogni Player ha più Characters (3 di default) e ne usa uno alla volta. Nome, cognome e data di nascita: la carta d’identità è del personaggio, non dell’account Minecraft.',
    chat: '/char create Anna Rossi 1998-03-14', who: 'Giocatore', doc: 'server-owner/personaggi',
  },
  {
    icon: 'gem_organizations.png', step: '2', title: 'Lo staff le dà un lavoro',
    text: 'I lavori li scrivi tu in un file di testo, con i gradi e i permessi di ciascun grado. Poi un operatore assegna il Job. Il giocatore entra ed esce di servizio quando vuole.',
    chat: '/job set Anna_RP police cadet', who: 'Staff', doc: 'server-owner/lavori',
  },
  {
    icon: 'duty.png', step: '3', title: 'Un blocco diventa il timbracartellino',
    text: 'Lo staff marca un blocco del mondo, anche di un altro mod: porta l’icona del servizio e solo chi ha quel lavoro lo usa. Un click per entrare in servizio, uno per uscire.',
    chat: '/qrm admin point create duty job police', who: 'Staff', doc: 'server-owner/punti-di-interazione',
  },
  {
    icon: 'cash.png', step: '4', title: 'Anna paga un altro personaggio',
    text: 'Il denaro appartiene al Character. Ogni movimento viene registrato, e lo staff può controllare in qualsiasi momento che i saldi tornino con lo storico.',
    chat: '/money pay Marco_RP 25.50', who: 'Giocatore', doc: 'server-owner/economia',
  },
  {
    icon: 'atm.png', step: '5', title: 'Passa all’ATM',
    text: 'Con il modulo Bank installato, Anna apre lo sportello con il click destro: vede il conto, ritira contante vero (oggetti nell’inventario) e fa bonifici. Il server controlla ogni richiesta.',
    who: 'Giocatore · modulo Bank', doc: 'modules/bank/',
  },
  {
    icon: 'gem_moderation.png', step: '6', title: 'Lo staff modera senza uscire dal gioco',
    text: 'Con il tasto F7 si apre il pannello staff: scheda del giocatore, mute, avvertimenti, note. Ogni azione che cambia qualcosa finisce nel registro, consultabile dopo.',
    chat: '/qrm admin log', who: 'Staff', doc: 'server-owner/pannello-staff',
  },
];

/* ---------- Installazione ---------- */
export const installSteps = [
  { tile: 'grass_top.png', title: 'Prepara il server', text: 'Minecraft {mc}, NeoForge {neo} o successiva e Java {java}. Nessun’altra versione è supportata.' },
  { tile: 'dirt.png', title: 'Copia un file', text: 'Metti `qrm-<versione>.jar` nella cartella `mods` del server. Sul computer dei giocatori è facoltativo: senza, giocano con i comandi; con, hanno anche HUD e schermate.' },
  { tile: 'stone.png', title: 'Aggiungi la banca (se vuoi)', text: 'Copia anche `qrm_bank-<versione>.jar` in `mods`, sul server **e** sul computer di ogni giocatore: aggiunge un blocco e degli oggetti.' },
  { tile: 'grass_top.png', title: 'Avvia il server', text: 'Al primo avvio QRM crea da solo configurazione e database. Da qui puoi cominciare a configurare.' },
];

export const installNotes = [
  'Chi ha QRM sul proprio computer deve avere la **stessa versione** del server.',
  'Prima di aggiornare, fai una copia della cartella `<world>/qrm/`.',
];

export const files = [
  { path: 'config/qrm-common.toml', what: 'Le impostazioni generali: slot dei personaggi, valuta, servizio, HUD.', edit: 'Sì, poi riavvia' },
  { path: '<world>/qrm/data/jobs/', what: 'Un file per ogni lavoro (grado, permessi, stipendio).', edit: 'Sì, li scrivi tu' },
  { path: '<world>/qrm/data/currencies/', what: 'Un file per ogni valuta.', edit: 'Sì, li scrivi tu' },
  { path: '<world>/qrm/qrm.db', what: 'Il database: personaggi, conti, movimenti.', edit: 'No, solo copie di sicurezza' },
  { path: 'config/qrm_bank-server.toml', what: 'Le impostazioni della banca, se l’hai installata.', edit: 'Sì, poi riavvia' },
];

/* ---------- Le impostazioni che userai di più ---------- */
export const settings = [
  { key: 'characters.slots', label: 'Quanti personaggi può avere un giocatore', value: '3', note: 'Da 1 a 50.' },
  { key: 'characters.uniqueNames', label: 'Nomi completi tutti diversi', value: 'sì', note: 'Rifiuta un nome già in uso.' },
  { key: 'economy.defaultCurrency', label: 'La valuta mostrata di default', value: 'QRM', note: 'Anche nell’HUD.' },
  { key: 'jobs.selfDuty', label: 'I giocatori entrano di servizio da soli', value: 'sì', note: 'Con /duty o dall’hub.' },
  { key: 'commands.shortAliases', label: 'Comandi brevi come /job e /money', value: 'sì', note: 'Mettilo a no se un altro mod usa gli stessi nomi.' },
  { key: 'gui.hud.elements', label: 'Cosa mostra l’HUD', value: 'nome, saldo, lavoro', note: 'Lista vuota = HUD nascosto.' },
];

export const jobExample = `{ "id": "police", "label": "Polizia", "grades": [
  { "id": "cadet", "label": "Recluta", "level": 1, "permissions": ["police.duty"],
    "salary": { "currency": "USD", "amount": "1200.00" } },
  { "id": "chief", "label": "Comandante", "level": 5, "permissions": ["police.*"] } ] }`;

/* ---------- Funzioni (hotbar) ---------- */
export interface Slot {
  id: string;
  icon: string;
  name: string; // nome dell'oggetto nella hotbar
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
    id: 'characters', icon: 'gem_players.png', name: 'Characters', kicker: 'Identità',
    title: 'Il giocatore non è il personaggio.',
    problem: 'In un server di roleplay chi gioca e chi viene interpretato sono due cose diverse. Se lavoro, soldi e permessi stanno sull’account, cambiare personaggio vuol dire cambiare tutto.',
    what: 'Ogni giocatore ha più personaggi e ne usa uno alla volta. Lavoro, conti, permessi e organizzazioni appartengono al personaggio.',
    owner: 'Decidi tu quanti slot, che nomi sono ammessi e se i nomi devono essere unici.',
    doc: 'server-owner/personaggi', link: 'Characters',
  },
  {
    id: 'economy', icon: 'gem_economy.png', name: 'Economy', kicker: 'Economia',
    title: 'Ogni movimento di denaro lascia una traccia.',
    problem: 'Un’economia di server si rompe quando due mod toccano lo stesso saldo o quando un pagamento viene eseguito due volte.',
    what: 'Valute, conti e movimenti registrati. Un pagamento ripetuto per errore non viene eseguito due volte. Il controllo `/qrm admin audit` confronta ogni saldo con la somma dei suoi movimenti.',
    owner: 'Crei le valute con un file di testo e scegli quale mostrare di default.',
    doc: 'server-owner/economia', link: 'Economy',
  },
  {
    id: 'jobs', icon: 'gem_organizations.png', name: 'Jobs e Organizations', kicker: 'Lavoro e gruppi',
    title: 'Lavori con gradi, gruppi con ranghi.',
    problem: 'Polizia, ospedale, negozio: ogni server li organizza a modo suo, e ogni mod finisce per reinventarli.',
    what: 'I lavori hanno gradi e permessi che valgono solo in servizio. Le organizzazioni hanno ranghi, membri e un conto in comune.',
    owner: 'Definisci un lavoro in un file di testo e lo assegni con `/job set`: la sua organizzazione nasce da sola, con lo stesso id. Gang e gruppi si gestiscono con `/org`.',
    doc: 'server-owner/organizzazioni', link: 'Organizations',
  },
  {
    id: 'points', icon: 'duty.png', name: 'Punti di interazione', kicker: 'Luoghi',
    title: 'Le funzioni stanno nei luoghi, non nei comandi.',
    problem: 'Entrare in servizio con un comando da qualsiasi punto della mappa toglie il senso del luogo di lavoro, e ogni mod si inventa il suo modo di marcare i blocchi.',
    what: 'Lo staff marca un blocco esistente e un lavoro o un’organizzazione vi offre una funzione. Oggi c’è il servizio (entra o esci di servizio con il click destro); armadietti e depositi arrivano come moduli. Chi non è autorizzato non vede l’icona e non può usare il punto.',
    owner: 'Crei e sposti i punti con `/qrm admin point`, scegli il grado minimo, il nome e la faccia su cui compare l’icona.',
    doc: 'server-owner/punti-di-interazione', link: 'Punti di interazione',
  },
  {
    id: 'staff', icon: 'gem_moderation.png', name: 'Pannello staff', kicker: 'Staff',
    title: 'Un pannello che non ferma il gioco.',
    problem: 'Moderare con comandi a memoria è lento, e un’azione senza traccia non si può rivedere.',
    what: 'Un overlay in alto a sinistra (tasto F7) con la scheda di ogni giocatore, mute, avvertimenti e note. Ogni azione finisce nel registro.',
    owner: 'Scegli per ogni membro dello staff cosa può vedere e fare, con `/qrm admin staff grant`.',
    doc: 'server-owner/pannello-staff', link: 'Pannello staff',
  },
  {
    id: 'bank', icon: 'atm.png', name: 'Bank (modulo)', kicker: 'Banca',
    title: 'Sportello ATM e contante vero.',
    problem: 'Un’economia senza luoghi in cui toccare i soldi resta astratta.',
    what: 'Un modulo facoltativo: conto per personaggio, sportello ATM, banconote come oggetti e bonifici con causale.',
    owner: 'Si installa con un file in più; le regole (limiti, distanza dallo sportello) sono in un file di impostazioni.',
    doc: 'modules/bank/', link: 'Bank',
  },
];
