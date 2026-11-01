---
title: Police
description: L'MDT della polizia, portatile e da scrivania, e la pistola d'ordinanza con caricatori e armeria. Anagrafe, chiamate d'emergenza e glossario dei reati.
---

Police dà alla polizia un terminale operativo, l'MDT. Ci sono l'anagrafe dei cittadini con i loro precedenti, le chiamate d'emergenza da prendere in carico e un glossario dei reati con multe e pene. Gli stessi dati si aprono da un tablet che si porta in tasca e da un terminale da mettere in centrale. Il modulo aggiunge anche una pistola d'ordinanza, con i caricatori e un punto di rifornimento: vedi [Pistola d'ordinanza](#pistola-dordinanza).

**Disponibilità**: incluso nel progetto. Va installato sul server e sui client: aggiunge oggetti e un blocco, quindi un client senza il mod non entra.

## Installare

1. Copia `qrm-police-<versione>.jar` in `mods/`, sul server e sui client.
2. Dai il nodo `police.mdt.use` ai gradi del lavoro di polizia, oppure ai singoli personaggi con `/qrm perm grant`. Vedi [Permessi](/Quorum/docs/sistemi/permessi/).
3. Consegna i tablet agli agenti e piazza i terminali.

Il **Tablet MDT di Polizia** è nella scheda degli strumenti dell'inventario creativo, il **Terminale di Polizia** fra i blocchi funzionali. Clic destro sul tablet in mano o sul terminale apre la stessa schermata.

## Chi lo usa

Apre l'MDT chi ha `police.mdt.use`, e ogni operatore. In giocatore singolo è aperto a tutti, per provarlo senza configurare niente.

Il nodo arriva dal grado del lavoro, quindi vale solo [in servizio](/Quorum/docs/sistemi/lavori/): un agente fuori servizio non apre il tablet e non riceve le chiamate. Chi non ha accesso vede il messaggio `Non hai accesso all'MDT di polizia.`

Le azioni sono firmate con il nome del personaggio attivo dell'agente.

## Le schede

### Dashboard

In alto ci sono lo stato dell'agente e il pulsante di panico.

- **Stato:** `Disponibile` (verde), `Occupato` (giallo) e `Fuori servizio` (grigio). Chi non ha scelto niente risulta fuori servizio. È una dichiarazione dell'agente e non cambia i permessi.
- **Panico:** `! PANICO` crea una chiamata di tipo panico alla posizione dell'agente, con nome e numero ID, e la manda in chat con un triplo segnale acustico a tutti quelli che possono aprire l'MDT. Si può ripetere dopo 10 secondi.

A sinistra c'è la centrale operativa: le chiamate aperte, quelle senza incaricato per prime, con orario, tipo, descrizione e coordinate. `Prendi in carico` assegna la chiamata a chi preme e al suo posto compare il nome dell'incaricato: una chiamata ha una sola persona incaricata, quindi nessuno può sovrapporsi. Chi la prende vede anche la freccia verso il luogo, come nel Dispatch.

A destra ci sono i ricercati attivi (un clic apre l'elenco dei cittadini già filtrato sui ricercati) e il totale degli schedati, poi le unità in servizio, cioè i colleghi collegati con il loro stato, e la bacheca del dipartimento. La bacheca è un testo breve, uguale per tutti, che cambiano gli operatori e chi ha il nodo `police.mdt.board`: per loro compare il pulsante `Modifica`.

In fondo c'è un'area di testo per le note rapide, su più righe, fino a 600 caratteri. Restano solo sul computer di chi le scrive, nel file `qrm_police_notes.txt` della cartella del gioco, e non le vede nessun altro.

### Cittadini

L'anagrafe contiene tutti i personaggi del server, anche quelli dei giocatori che non sono collegati: l'elenco si allinea a ogni apertura dell'MDT. La ricerca accetta nome e cognome in qualsiasi ordine; `Solo Ricercati` filtra la lista. Un clic su una riga apre la scheda a pagina intera, con la barra laterale sempre visibile.

In cima alla scheda ci sono le cose da sapere subito: se è ricercato (con il motivo), le multe da pagare, la patente, il porto d'armi e le armi registrate a suo nome, comprese quelle segnalate come rubate o sequestrate. Ogni cittadino ha un numero identificativo, come `QRM-000123`.

Dalla scheda si può:

- segnalarlo come ricercato, con un motivo, e revocare la segnalazione;
- emettere una multa: si sceglie l'infrazione fra cinque categorie (infrazioni stradali, violazioni amministrative, disturbo della quiete pubblica, aggressioni, possesso di oggetti illegali) e l'importo è quello del catalogo. La multa nasce non pagata; un agente la segna pagata o la annulla dalla voce nello storico;
- registrare un arresto scegliendo uno o più reati: multa e mesi di detenzione sono la somma del glossario;
- scrivere un rapporto o una nota;
- concedere o revocare il porto d'armi, sospendere o ripristinare la patente;
- cambiare la pericolosità: bassa, media, alta, armato e pericoloso.

Lo storico elenca multe, arresti, rapporti, note e mandati, con agente e data; un clic apre la voce per intero. Una voce si può archiviare: resta consultabile ma non conta più nella fedina, e si può ripristinare. Una multa non pagata non si archivia. Il filtro dello storico passa fra voci attive, un tipo alla volta, archiviate e multe da pagare.

La fototessera è la faccia della skin attuale del giocatore proprietario del personaggio, se è collegato; per gli altri sono le iniziali. La schermata mostra gli ultimi 40 precedenti di ogni cittadino; quelli più vecchi restano nei dati.

### Dispatch

La centrale operativa, divisa in tre viste: `Chiamate`, `Unità` e `Storico`. Sotto le schede ci sono lo stato della centrale (`ATTIVO` se c'è almeno un agente in servizio) e tre contatori: chiamate attive, unità disponibili, interventi in corso. Nella vista Chiamate ci sono anche `Richiedi supporto` e, per gli operatori di centrale, `+ Chiamata`.

**Unità.** Gli agenti collegati con matricola, grado (quello del lavoro del personaggio, se ne ha uno), stato, coordinate, distanza in linea retta e intervento in carico. Le posizioni si aggiornano da sole ogni due secondi finché la vista è aperta. Gli stati `In viaggio`, `Sul posto` e `In intervento` non si scelgono: li ricava il server dalla chiamata che l'agente ha in carico, e `Occupato` compare appena gli viene assegnata. `Fuori servizio` resta tale. `Posizione` imposta il waypoint sulla posizione dell'agente al momento del clic (non lo segue); `Intervento` apre la chiamata.

**Storico.** Le chiamate concluse e annullate, dalla più recente, con ricerca per ID, agente, matricola, segnalante o testo, e filtri per categoria, priorità, esito e data. Un clic apre la scheda in sola lettura, con tutta la cronologia. Gli operatori di centrale vedono tutto; un agente semplice solo gli interventi a cui ha partecipato o che ha segnalato. Il filtro lo applica il server.

Le chiamate aperte sono in ordine di priorità, poi di orario. Ogni riga mostra ID (`#D-001`), priorità, tipo, orario, descrizione, coordinate, dimensione, distanza, stato e incaricato con la matricola. Le P1 senza incaricato hanno il bordo rosso che pulsa.

**Priorità.** P1 critica, P2 urgente, P3 ordinaria, P4 informativa. Ogni tipo ha la sua predefinita: P1 per panico, sparatoria, rapina e sequestro, P2 per soccorso medico, aggressione, effrazione e richieste di rinforzi o supporto, P3 per furto, persona sospetta, disturbo, scorta e chiamate di polizia generiche, P4 per segnalazioni, controlli di routine e altro. Gli operatori di centrale la possono cambiare.

**Stati.** In attesa, Assegnata, In viaggio, Sul posto, In intervento, Rinforzi richiesti, Conclusa, Annullata. Lo stato della chiamata non è lo stato dell'agente della Dashboard.

Un clic su una riga apre la pagina della chiamata, con segnalante, posizione, distanza in linea retta e direzione, assegnatario con matricola e ora, descrizione, le azioni possibili e la cronologia. La cronologia si scrive da sola a ogni passaggio e nessuno la può modificare.

Le azioni compaiono solo se hanno senso per lo stato della chiamata e per chi guarda:

- `Prendi in carico`: tutti gli agenti, solo con stato diverso da Fuori servizio. Una chiamata ha un solo incaricato e il controllo lo fa il server: se due agenti premono insieme, il secondo riceve un avviso e vede subito il nome del primo. Chi la prende vede anche la freccia verso il luogo; la freccia sparisce a 8 blocchi dall'arrivo.
- `Rilascia`, `Segna in viaggio`, `Segna sul posto`, `Segna in intervento`, `Richiedi rinforzi`, `Chiudi intervento` (con l'esito): l'incaricato o un operatore di centrale. Chiedere rinforzi avvisa tutti gli agenti.
- `Assegna unità`, `Priorità`, `Annulla chiamata` e la creazione di chiamate: solo operatori di centrale. Si assegna soltanto a un agente collegato e non fuori servizio.
- `Imposta waypoint`: tutti.

Chi esce dal gioco, perde la connessione o si mette `Fuori servizio` mentre ha una chiamata in carico la lascia: torna in attesa, con una riga nella cronologia.

`Richiedi supporto` crea una chiamata di tipo richiesta rinforzi alla posizione dell'agente, collegata all'intervento in cui è impegnato, se ne ha uno. Non ne crea una seconda finché la prima è aperta. Anche il panico si crea una sola volta.

Una nuova chiamata arriva in chat, con un doppio segnale acustico, a tutti quelli che possono aprire l'MDT, anche con la schermata chiusa. Le chiamate arrivano anche da [Medical](/Quorum/docs/modules/medical/): quando un ferito a terra preme `C`, nasce una chiamata di soccorso medico alla sua posizione.

### Glossario

Un elenco di 18 reati divisi per categoria (traffico, ordine pubblico, armi, patrimonio, persona, droga), con la multa e i mesi di carcere previsti. Si cerca per nome o per categoria. È solo da consultare: gli arresti prendono multa e detenzione da qui quando si scelgono i reati.

L'elenco dei reati è scritto nel mod e non si cambia da configurazione.

## Pistola d'ordinanza

**Sperimentale.** La pistola `qrm_police:police_pistol` e il caricatore `qrm_police:police_magazine` sono nella scheda «Combattimento» dell'inventario creativo; si possono dare anche con `/give`. Non c'è nessun controllo di servizio, lavoro o permesso: chiunque abbia la pistola può impugnarla, sparare e ricaricare.

### Comandi del giocatore

| Tasto | Azione |
| --- | --- |
| Clic sinistro | Spara. Un clic, un colpo. |
| Clic destro tenuto | Mira: la pistola passa alla posa di mira in circa 0,2 secondi, il campo visivo si stringe e i colpi sono più precisi. |
| `R` | Ricarica. |
| `V` | Interazione: pressione breve per sistemare il carrello quando serve, altrimenti controllo delle munizioni; tenuto premuto oltre 0,3 secondi, ispezione completa. Con un caricatore in mano ispeziona il caricatore. |

I tasti si cambiano dalle opzioni di controllo del gioco.

### Come si comporta

Il server decide tutto: munizioni, ricariche e colpi a segno. Ogni caricatore è un oggetto a sé, con il proprio numero di colpi; la capienza è 17.

- **Cadenza.** Il client normale si limita a un colpo ogni 5 tick, circa quattro al secondo; il server ne accetta uno ogni 4 tick, per assorbire la latenza, e rifiuta i colpi più ravvicinati.
- **Danno.** 4 punti a colpo, circa 5 colpi al corpo per atterrare un giocatore senza armatura. Alla testa il danno è moltiplicato per 1,5. Gittata 64 blocchi. Il danno è di tipo proiettile: [Medical](/Quorum/docs/modules/medical/), se c'è, lo registra come ferita da arma da fuoco.
- **Precisione.** La dispersione è di 0,3° dal fianco e di 0,1° mirando (la mira non è ancora attiva), dimezzata da accovacciati, calcolata dal server.
- **Ricarica.** Il caricatore inserito torna nell'inventario a metà della ricarica, e il nuovo entra dopo; se l'inventario è pieno, il vecchio cade ai piedi del giocatore. Con la camera vuota, il colpo entra in canna solo a fine ricarica. Cambiare slot o perdere la pistola interrompe l'azione: nessun colpo viene creato o perso, e lo stato resta quello dell'ultimo passaggio concluso. Interrotta tra l'uscita e l'ingresso, o se al momento dell'ingresso non resta un caricatore con colpi, la pistola resta senza caricatore; interrotta dopo l'ingresso a vuoto, resta con il carrello aperto e va chiuso con `V`.
- **Durante la ricarica non si spara.** Subire danni non la interrompe.
- **Chi è a terra** ([Medical](/Quorum/docs/modules/medical/)) non spara, non mira e non ricarica; premendo i tasti non si vede né si sente nessuno sparo. La pistola resta nell'inventario. Senza Medical questa regola non ha effetto.

Non c'è un contatore di colpi. Accanto alla pistola compare una breve scritta quando serve un'azione: ricaricare, sistemare il carrello, oppure l'avviso che non restano caricatori con colpi. Il controllo munizioni (`V`) dà una fascia approssimata per il caricatore inserito e dice con esattezza se c'è un colpo in canna.

Nell'inventario la fascia di un caricatore compare nel tooltip solo se il personaggio l'ha ispezionato a mano o con il controllo munizioni, e da quel momento segue il conteggio reale. Il ricordo è personale: chi riceve un caricatore da un altro non ne conosce il contenuto finché non lo ispeziona.

### Punto armeria

Il punto di tipo `armory` rifornisce i caricatori: con un clic destro, i caricatori nell'inventario principale si riempiono e ne vengono aggiunti fino ad averne 3 pieni. I caricatori già inseriti nella pistola non si toccano. Il rifornimento è illimitato e richiede un personaggio attivo. Si usa con la pistola non in mano: con la pistola in mano il clic destro è la mira e non arriva al punto. Si crea come gli altri punti, legato a un lavoro o a un'organizzazione:

```text
/qrm admin point create armory job police
```

Le regole di accesso sono quelle dei [punti di interazione](/Quorum/docs/sistemi/punti-di-interazione/); la guida [Aggiungere un punto di interazione](/Quorum/docs/guide/aggiungere-un-punto/) mostra i passaggi.

### Limiti

- I suoni sono ancora segnaposto di Minecraft.
- In prima persona le braccia sono rigide, senza gomito, come il braccio vanilla.
- Con la pistola in mano la mano secondaria e il clic destro sui blocchi non funzionano, perché il clic destro è la mira.
- Il punto armeria non si può usare con la pistola in mano: va riposta prima.
- L'ispezione del caricatore non ha animazione: si sente un suono e, alla fine, la fascia compare sopra la barra degli oggetti.

## Dati di prova

Per provare l'MDT senza giocatori:

```text
/qrm mdt seed
```

Aggiunge dieci cittadini inventati, alcuni ricercati e con precedenti, e un registro di sei armi, oltre al personaggio di chi lo esegue. Rieseguirlo non crea doppioni. `/qrm mdt clear` cancella **tutti** i dati dell'MDT, compresi quelli veri.

```text
/qrm mdt simulatecall [tipo]
```

Crea una chiamata di prova tra 100 e 200 blocchi da chi scrive il comando. `tipo` è `medical`, `police` o `shooting`; senza tipo ne sceglie uno a caso.

## Comandi

Riservati agli operatori; in giocatore singolo li usa chiunque.

| Comando | Effetto |
| --- | --- |
| `/qrm mdt seed` | Riempie l'MDT con i dati di prova. |
| `/qrm mdt clear` | Svuota l'MDT: cittadini, armi e chiamate. |
| `/qrm mdt simulatecall [tipo]` | Crea una chiamata di prova vicino a te. |

`seed` e `clear` vengono scritti nel [registro delle azioni dello staff](/Quorum/docs/sistemi/moderazione-e-registro/).

## Dove sono i dati

I dati dell'MDT stanno nel database di QRM, insieme al resto del mondo: si salvano da soli ogni cinque secondi e quando il server si ferma. Il [backup](/Quorum/docs/guide/backup-e-saldi/) del database li comprende.

## Limiti

- Il registro delle armi esiste nei dati, e `/qrm mdt seed` lo riempie, ma la schermata non lo mostra ancora.
- La centrale tiene al massimo 50 chiamate aperte: oltre, le più vecchie vengono annullate. Le chiamate concluse e annullate si salvano (le ultime 300), ma la schermata non le mostra ancora.
- Le multe del codice penale e del casellario sono solo annotazioni: non tolgono denaro dal conto del cittadino.
- La pericolosità e lo stato di ricercato non hanno effetti in gioco al di fuori dell'MDT.
