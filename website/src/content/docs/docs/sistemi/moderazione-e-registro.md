---
title: Moderazione e registro delle azioni
description: Mute, avvertimenti, note e il registro di ciò che fa lo staff.
---

Mute, avvertimenti e note sono legati all'account del giocatore, non al personaggio. Si applicano dal [pannello staff](/Quorum/docs/sistemi/pannello-staff/), dalla scheda del giocatore.

## Mute

Un giocatore silenziato non può scrivere in chat né usare i comandi di messaggistica di Minecraft: `/msg`, `/tell`, `/w`, `/me`, `/teammsg`, `/tm` e `/say`. Ogni tentativo viene scartato e il giocatore riceve un avviso con la scadenza del mute.

Gli altri comandi restano utilizzabili, compresi quelli di QRM.

Un mute ha una durata oppure è permanente, e ha un motivo facoltativo. Il giocatore viene avvisato quando il mute inizia e quando viene tolto. Un mute a tempo scade da solo.

## Avvertimenti

Un avvertimento ha un motivo, obbligatorio. Il giocatore lo riceve subito in chat, insieme al numero di avvertimenti che ha accumulato. Gli avvertimenti non hanno effetti automatici: non silenziano e non espellono nessuno.

## Note

Una nota è un testo fino a 200 caratteri che vede solo lo staff. Il giocatore non ne viene informato.

## Pagine di moderazione

La sezione Moderazione del pannello ha due pagine, visibili con il permesso `staff.moderation`:

- **Mute attivi**: chi è silenziato in questo momento.
- **Ultimi avvertimenti**: gli avvertimenti più recenti, di tutti i giocatori.

## Il registro delle azioni

QRM registra ciò che fa lo staff: chi ha agito, su chi, quale azione, con quale dettaglio e con quale esito. Finiscono nel registro:

- le azioni del pannello che cambiano qualcosa, anche quando vengono rifiutate per mancanza di permesso;
- i comandi riservati agli operatori che vanno a buon fine: `/money give` e `take`, `/job set` e `remove`, `/perm`, i comandi di `/org` e quelli di `/qrm admin point` e `/qrm admin staff`.

Le azioni che mostrano soltanto, come vedere un personaggio o leggere le note, non vengono registrate.

### Consultarlo

Dal pannello, con il permesso `staff.logs`:

- **Ultime azioni**, nella sezione Log e transazioni, mostra le più recenti.
- **history**, nella scheda di un giocatore, mostra quelle che lo riguardano.

Da comando, per gli operatori:

| Comando | Effetto |
| --- | --- |
| `/qrm admin log` | Mostra le ultime 20 azioni. |
| `/qrm admin log <giocatore> [n]` | Mostra le ultime azioni che riguardano un giocatore online. `n` va da 1 a 200; se manca vale 20. |
| `/qrm admin log prune <giorni>` | Cancella le voci più vecchie del numero di giorni indicato. |

### Conservazione

Le voci del registro non scadono. Restano nel database finché un operatore non le cancella con `prune`, e anche la cancellazione viene registrata.
