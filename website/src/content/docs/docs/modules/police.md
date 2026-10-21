---
title: Police
description: L'MDT della polizia, portatile e da scrivania, e la pistola d'ordinanza con caricatori e armeria. Anagrafe, BOLO, chiamate d'emergenza e codice penale.
---

Police dà alla polizia un terminale operativo, l'MDT. Ci sono l'anagrafe dei cittadini con i loro precedenti, gli avvisi di ricerca (BOLO), le chiamate d'emergenza da prendere in carico e un codice penale per calcolare multe e pene. Gli stessi dati si aprono da un tablet che si porta in tasca e da un terminale da mettere in centrale. Il modulo aggiunge anche una pistola d'ordinanza, con i caricatori e un punto di rifornimento: vedi [Pistola d'ordinanza](#pistola-dordinanza).

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

Tre riquadri riassumono la situazione: chiamate attive, ricercati e cittadini schedati. Sotto ci sono i BOLO attivi, con titolo, descrizione e priorità da 1 a 3: si aggiungono con `+ Nuovo BOLO` e si chiudono con `Risolvi`.

A destra ci sono quattro righe di note rapide. Restano solo sul computer di chi le scrive, nel file `qrm_police_notes.txt` della cartella del gioco, e non le vede nessun altro.

### Cittadini

L'anagrafe. Ogni personaggio entra in anagrafe la prima volta che viene selezionato, con nome, cognome e data di nascita. La ricerca accetta nome e cognome in qualsiasi ordine; `Solo Ricercati` filtra la lista.

Nella scheda di un cittadino si può:

- segnalarlo come ricercato, con un motivo, e revocare la segnalazione;
- aggiungere un reato al casellario, con importo della multa, mesi di carcere e note;
- concedere o revocare il porto d'armi, sospendere o ripristinare la patente;
- cambiare la pericolosità: bassa, media, alta, armato e pericoloso.

La fototessera è la faccia della skin per i giocatori online, le iniziali per gli altri. La schermata mostra gli ultimi 40 precedenti di ogni cittadino; quelli più vecchi restano nei dati.

### Dispatch

Le chiamate d'emergenza aperte, ognuna con tipo (medico, polizia, sparatoria), descrizione, chi ha chiamato, distanza e unità che l'hanno presa in carico.

- `Rispondi` prende in carico la chiamata e accende in alto sullo schermo una freccia con la distanza dal luogo. La freccia sparisce a 8 blocchi dall'arrivo.
- `Chiudi` toglie la chiamata dall'elenco, per tutti.
- `! Invia Posizione / Supporto` crea subito una chiamata di tipo polizia alla posizione dell'agente. Lo stesso agente può ripeterla dopo 10 secondi.

Una nuova chiamata arriva in chat, con un doppio segnale acustico, a tutti quelli che possono aprire l'MDT, anche con la schermata chiusa. Le chiamate arrivano anche da [Medical](/Quorum/docs/modules/medical/): quando un ferito a terra preme `C`, nasce una chiamata medica alla sua posizione.

### Codice penale

Un elenco di 18 reati divisi per categoria (traffico, ordine pubblico, armi, patrimonio, persona, droga), con la multa e i mesi di carcere consigliati. Si cerca per nome o per categoria e si spuntano più reati: la barra in basso somma multe e mesi. `Copia Riepilogo` copia il riepilogo negli appunti e, se è aperta la scheda di un cittadino, precompila il reato da aggiungere al casellario. `Azzera Selezione` svuota la scelta.

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

- **Cadenza.** Il server accetta al massimo un colpo ogni 5 tick, circa quattro al secondo, qualunque cosa mandi il client.
- **Danno.** 4 punti a colpo, circa 5 colpi al corpo per atterrare un giocatore senza armatura. Alla testa il danno è moltiplicato per 1,5. Gittata 64 blocchi. Il danno è di tipo proiettile: [Medical](/Quorum/docs/modules/medical/), se c'è, lo registra come ferita da arma da fuoco.
- **Precisione.** La dispersione è di 3° dal fianco e di 0,5° mirando, calcolata dal server.
- **Ricarica.** Il caricatore inserito torna nell'inventario a metà della ricarica, e il nuovo entra dopo; se l'inventario è pieno, il vecchio cade ai piedi del giocatore. Con la camera vuota, il colpo entra in canna solo a fine ricarica. Cambiare slot o perdere la pistola interrompe l'azione: nessun colpo viene creato o perso, e lo stato resta quello dell'ultimo passaggio concluso. Interrotta tra l'uscita e l'ingresso, la pistola resta senza caricatore; interrotta dopo l'ingresso a vuoto, resta con il carrello aperto e va chiuso con `V`.
- **Durante la ricarica non si spara.** Subire danni non la interrompe.
- **Chi è a terra** ([Medical](/Quorum/docs/modules/medical/)) non spara né ricarica; la pistola resta nell'inventario. Senza Medical questa regola non ha effetto.

Non c'è un contatore di colpi. Accanto alla pistola compare una breve scritta quando serve un'azione: ricaricare, sistemare il carrello, oppure l'avviso che non restano caricatori con colpi. Il controllo munizioni (`V`) dà una fascia approssimata per il caricatore inserito e dice con esattezza se c'è un colpo in canna.

Nell'inventario la fascia di un caricatore compare nel tooltip solo se il personaggio l'ha ispezionato a mano o con il controllo munizioni, e da quel momento segue il conteggio reale. Il ricordo è personale: chi riceve un caricatore da un altro non ne conosce il contenuto finché non lo ispeziona.

### Punto armeria

Il punto di tipo `armory` rifornisce i caricatori: con un clic destro, i caricatori nell'inventario principale si riempiono e ne vengono aggiunti fino ad averne 3 pieni. I caricatori già inseriti nella pistola non si toccano. Il rifornimento è illimitato e richiede un personaggio attivo. Si crea come gli altri punti, legato a un lavoro o a un'organizzazione:

```text
/qrm admin point create armory job police
```

Le regole di accesso sono quelle dei [punti di interazione](/Quorum/docs/sistemi/punti-di-interazione/); la guida [Aggiungere un punto di interazione](/Quorum/docs/guide/aggiungere-un-punto/) mostra i passaggi.

### Limiti

- I suoni sono ancora segnaposto di Minecraft.
- In prima persona si vede solo il braccio destro.
- Con la pistola in mano la mano secondaria e il clic destro sui blocchi non funzionano, perché il clic destro è la mira.
- L'ispezione del caricatore non ha animazione.

## Dati di prova

Per provare l'MDT senza giocatori:

```text
/qrm mdt seed
```

Aggiunge dieci cittadini inventati, alcuni ricercati e con precedenti, due BOLO e un registro di sei armi, oltre al personaggio di chi lo esegue. Rieseguirlo non crea doppioni. `/qrm mdt clear` cancella **tutti** i dati dell'MDT, compresi quelli veri.

```text
/qrm mdt simulatecall [tipo]
```

Crea una chiamata di prova tra 100 e 200 blocchi da chi scrive il comando. `tipo` è `medical`, `police` o `shooting`; senza tipo ne sceglie uno a caso.

## Comandi

Riservati agli operatori; in giocatore singolo li usa chiunque.

| Comando | Effetto |
| --- | --- |
| `/qrm mdt seed` | Riempie l'MDT con i dati di prova. |
| `/qrm mdt clear` | Svuota l'MDT: cittadini, armi, BOLO e chiamate. |
| `/qrm mdt simulatecall [tipo]` | Crea una chiamata di prova vicino a te. |

`seed` e `clear` vengono scritti nel [registro delle azioni dello staff](/Quorum/docs/sistemi/moderazione-e-registro/).

## Dove sono i dati

I dati dell'MDT stanno nel database di QRM, insieme al resto del mondo: si salvano da soli ogni cinque secondi e quando il server si ferma. Il [backup](/Quorum/docs/guide/backup-e-saldi/) del database li comprende.

## Limiti

- Il registro delle armi esiste nei dati, e `/qrm mdt seed` lo riempie, ma la schermata non lo mostra ancora.
- L'elenco tiene al massimo 50 chiamate aperte: chiudi quelle concluse.
- Le multe del codice penale e del casellario sono solo annotazioni: non tolgono denaro dal conto del cittadino.
- La pericolosità e lo stato di ricercato non hanno effetti in gioco al di fuori dell'MDT.
