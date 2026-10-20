---
title: Dare accesso allo staff
description: Come concedere a un giocatore parti del pannello staff senza renderlo operatore.
---

Un operatore ha già tutto. Questa guida serve a dare a un moderatore solo alcune funzioni del pannello staff, senza i poteri di un operatore.

I permessi dello staff sono legati all'account del giocatore, non al personaggio: valgono qualunque personaggio stia usando. Sono un sistema separato dai [permessi dei personaggi](/Quorum/docs/sistemi/permessi/).

## Cosa serve

- I permessi di operatore, per eseguire i comandi.
- Il giocatore online.
- QRM installato sul client del giocatore: il pannello staff è una schermata.

## 1. Concedi l'accesso al pannello

```text
/qrm admin staff grant <giocatore> staff.panel
```

Senza `staff.panel` il pannello non si apre, qualunque altro nodo abbia il giocatore. Il pannello si apre con **F7**.

## 2. Concedi le funzioni

Ripeti il comando per ogni nodo che vuoi dare. La modifica vale subito, senza che il giocatore debba rientrare.

| Nodo | Cosa sblocca |
| --- | --- |
| `staff.logs` | Il registro delle azioni dello staff e lo storico di un giocatore. |
| `staff.moderation` | Le pagine con i mute attivi e gli ultimi avvertimenti. |
| `staff.bank` | La schermata di Bank nel pannello. Richiede il Module Bank. |
| `staff.player.<azione>` | Una singola azione della scheda di un giocatore. |

Le azioni sulla scheda di un giocatore sono:

| Azione | Nodo |
| --- | --- |
| Teletrasportarsi dal giocatore | `staff.player.teleport_to` |
| Portare il giocatore da sé | `staff.player.bring` |
| Bloccarlo sul posto | `staff.player.freeze` |
| Curarlo | `staff.player.heal` |
| Dargli un oggetto | `staff.player.give_item` |
| Cambiargli modalità di gioco | `staff.player.set_gamemode` |
| Espellerlo | `staff.player.kick` |
| Bannarlo | `staff.player.ban` |
| Vedere il suo personaggio | `staff.player.view_character` |
| Vedere i suoi permessi | `staff.player.view_permissions` |
| Silenziarlo e riattivarlo | `staff.player.mute`, `staff.player.unmute` |
| Dargli un avvertimento | `staff.player.warn` |
| Scrivere una nota | `staff.player.note` |
| Leggere avvertimenti e note | `staff.player.warnings`, `staff.player.notes` |

Puoi usare un jolly:

```text
/qrm admin staff grant <giocatore> staff.player.*
/qrm admin staff grant <giocatore> staff.*
```

`staff.player.*` dà tutte le azioni sui giocatori. `staff.*` dà tutto il pannello.

## Verifica

```text
/qrm admin staff list <giocatore>
```

Il comando elenca i nodi concessi. Il giocatore preme **F7**: il pannello si apre e mostra solo le funzioni che gli hai dato.

## Togliere un accesso

```text
/qrm admin staff clear <giocatore> <nodo>
```

Il nodo va scritto come è stato concesso. Se hai concesso `staff.player.*`, non puoi togliere la sola `staff.player.ban`: togli il jolly e concedi le singole azioni.

## Limiti

- Chi non è operatore non può agire su un operatore, nemmeno con il nodo giusto.
- I comandi di amministrazione, come `/job set`, `/org create` e `/money give`, restano riservati agli operatori. I nodi dello staff non li sbloccano.
- I nodi `staff.jobs`, `staff.orgs` e `staff.perms` vengono accettati dal comando ma nella versione corrente non sbloccano nulla.
