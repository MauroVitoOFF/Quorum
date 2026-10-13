---
title: Nodi di permesso
description: I nodi che QRM riconosce, divisi fra accesso dello staff e organizzazioni.
---

QRM ha due insiemi di nodi, assegnati in modi diversi.

| Insieme | A chi si dà | Come |
| --- | --- | --- |
| Nodi dello staff | All'account del giocatore | `/qrm admin staff grant` |
| Nodi delle organizzazioni | A un rank | `/org rank set` |

I nodi che un [lavoro](/Quorum/docs/sistemi/lavori/) dà ai suoi gradi non sono in questa pagina: hanno i nomi che scegli tu, e li controllano i Modules.

## Nodi dello staff

Spiegati in [Dare accesso allo staff](/Quorum/docs/guide/accesso-staff/). Gli operatori li hanno tutti.

| Nodo | Cosa sblocca |
| --- | --- |
| `staff.panel` | Aprire il pannello staff. |
| `staff.logs` | Il registro delle azioni e lo storico di un giocatore. |
| `staff.moderation` | Le pagine dei mute attivi e degli ultimi avvertimenti. |
| `staff.bank` | La schermata di Bank nel pannello. |
| `staff.jobs` | Nulla, nella versione corrente. |
| `staff.orgs` | Nulla, nella versione corrente. |
| `staff.perms` | Nulla, nella versione corrente. |

### Azioni sulla scheda di un giocatore

Descritte in [Pannello staff](/Quorum/docs/sistemi/pannello-staff/).

| Nodo | Azione |
| --- | --- |
| `staff.player.teleport_to` | Teletrasportarsi dal giocatore. |
| `staff.player.bring` | Portare il giocatore da sé. |
| `staff.player.freeze` | Bloccarlo e sbloccarlo. |
| `staff.player.heal` | Curarlo. |
| `staff.player.give_item` | Dargli un oggetto. |
| `staff.player.set_gamemode` | Cambiargli modalità di gioco. |
| `staff.player.kick` | Espellerlo. |
| `staff.player.ban` | Bannarlo. |
| `staff.player.mute` | Silenziarlo. |
| `staff.player.unmute` | Togliere il mute. |
| `staff.player.warn` | Dargli un avvertimento. |
| `staff.player.note` | Scrivere una nota. |
| `staff.player.warnings` | Leggere i suoi avvertimenti. |
| `staff.player.notes` | Leggere le sue note. |
| `staff.player.view_character` | Vedere il suo personaggio. |
| `staff.player.view_permissions` | Vedere i suoi permessi. |

### Voci dei Modules

Un Module che aggiunge una voce al pannello la protegge con un nodo che inizia per `staff.module.`, seguito dal nome scelto dal Module.

### Jolly

`staff.player.*` copre tutte le azioni sui giocatori. `staff.*` copre tutto il pannello. Il comando accetta anche `*`.

## Nodi delle organizzazioni

Spiegati in [Organizzazioni](/Quorum/docs/sistemi/organizzazioni/). Valgono solo dentro l'organizzazione del rank che li contiene.

| Nodo | Cosa permette |
| --- | --- |
| `org.account.withdraw` | Prelevare dal conto dell'organizzazione, attraverso un Module che lo consente. |
