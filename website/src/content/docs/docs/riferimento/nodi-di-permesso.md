---
title: Nodi di permesso
description: I nodi che QRM riconosce, divisi fra accesso dello staff e organizzazioni.
---

QRM ha due insiemi di nodi, assegnati in modi diversi.

| Insieme | A chi si dà | Come |
| --- | --- | --- |
| Nodi dello staff | All'account del giocatore | `/qrm admin staff grant` |
| Nodi delle organizzazioni | A un rank | `/org rank set` |

I Modules aggiungono nodi propri, elencati in fondo. Gli altri nodi che un [lavoro](/Quorum/docs/sistemi/lavori/) dà ai suoi gradi hanno i nomi che scegli tu.

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

## Nodi di Bank

Spiegati in [Bank](/Quorum/docs/modules/bank/). Sono consentiti a tutti: servono solo per negarli a un personaggio con `/qrm perm deny`.

| Nodo | Cosa regola |
| --- | --- |
| `bank.atm.use` | Usare gli sportelli ATM. |
| `bank.transfer` | Fare bonifici. |

## Nodi di Medical

Spiegati in [Medical](/Quorum/docs/modules/medical/). Si danno ai gradi dei lavori di soccorso.

| Nodo | Cosa permette |
| --- | --- |
| `medical.alert.receive` | Ricevere l'allerta di chi è a terra, con le sue coordinate. |
| `medical.treat` | Vedere, nell'ispezione di un ferito, il tempo che gli resta. |

## Nodi di Police

Spiegato in [Police](/Quorum/docs/modules/police/). Gli operatori non ne hanno bisogno, e in giocatore singolo l'MDT è aperto a tutti.

| Nodo | Cosa permette |
| --- | --- |
| `police.mdt.use` | Aprire il tablet e il terminale, modificare i dati e ricevere le chiamate del dispatch. |

## Nodi di Boss

Spiegati in [Boss](/Quorum/docs/modules/boss/). Valgono dentro una singola azienda: si scrivono nei permessi di un grado, oppure si danno a un rank.

| Nodo | Cosa permette |
| --- | --- |
| `boss.menu` | Aprire il menu del boss. |
| `boss.members.manage` | Gestire i membri. |
| `boss.notice` | Scrivere l'avviso della bacheca. |
| `boss.title` | Assegnare il titolo interno ai membri. |
| `boss.bonus` | Dare un bonus a un dipendente. |
