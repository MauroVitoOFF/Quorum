---
title: Police
description: L'MDT della polizia, portatile e da scrivania. Anagrafe e casellario, BOLO, chiamate d'emergenza con dispatch e codice penale.
---

Police dà alla polizia un terminale operativo, l'MDT. Ci sono l'anagrafe dei cittadini con i loro precedenti, gli avvisi di ricerca (BOLO), le chiamate d'emergenza da prendere in carico e un codice penale per calcolare multe e pene. Gli stessi dati si aprono da un tablet che si porta in tasca e da un terminale da mettere in centrale.

**Disponibilità**: incluso nel progetto. Va installato sul server e sui client: aggiunge un oggetto e un blocco, quindi un client senza il mod non entra.

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
