---
title: Organizzazioni
description: Organizzazioni, rank, membri, conto in comune e comandi.
---

Un'organizzazione è un gruppo di personaggi con dei rank e un conto in comune. Nasce in due modi:

- **Da un lavoro.** Ogni [lavoro](/Quorum/docs/sistemi/lavori/) ha un'organizzazione con lo stesso `id`, che QRM crea all'avvio. Il suo nome segue quello del lavoro: se cambi `label` nel file, al riavvio cambia anche l'organizzazione.
- **Da un comando.** Un operatore crea un'organizzazione che non è legata a nessun lavoro, per esempio una banda o un'associazione.

L'`id` di un'organizzazione non si può cambiare.

## Chi ne fa parte

Un personaggio appartiene a un'organizzazione in due modi.

**Come membro**, quando un operatore lo aggiunge con un rank. Resta membro finché non viene rimosso.

**Per il lavoro**, mentre è in servizio. Un personaggio in servizio appartiene all'organizzazione del proprio lavoro senza bisogno di essere aggiunto, e smette di appartenervi quando esce dal servizio. Questi personaggi non compaiono nel conteggio `Membri` di `/org info`.

## Rank

Un rank ha un `id`, un livello e un elenco facoltativo di [nodi di permesso](/Quorum/docs/sistemi/permessi/).

```text
/org rank set <organizzazione> <rank> <livello> [nodi…]
```

- Due rank della stessa organizzazione non possono avere lo stesso livello.
- Un rank eredita i nodi di tutti i rank con livello inferiore.
- Ripetere il comando su un rank esistente lo sostituisce.
- Un rank assegnato a dei membri non si può rimuovere.

I nodi di un rank valgono solo dentro quell'organizzazione: non diventano permessi generali del personaggio. Li controllano i Modules che agiscono su un'organizzazione. QRM stesso ne controlla uno, `org.account.withdraw`, quando un Module chiede di prelevare dal conto.

Un personaggio che appartiene all'organizzazione per il lavoro non ha un rank: dentro l'organizzazione valgono i permessi del suo grado.

## Il conto

Ogni organizzazione ha un conto, creato insieme a lei. `/org info` ne mostra il saldo.

QRM non ha comandi per versare o prelevare dal conto di un'organizzazione: lo fanno i Modules.

## Archiviare ed eliminare

`/org archive <id>` archivia l'organizzazione e ne congela il conto. Da quel momento ogni modifica a rank e membri viene rifiutata con `Organizzazione archiviata`, e i personaggi non risultano più appartenervi. `/org unarchive <id>` la riattiva e scongela il conto.

`/org delete <id>` elimina un'organizzazione solo se è archiviata, non ha membri e ha il conto a zero. Il conto resta nel database, congelato, per conservare lo storico delle transazioni.

:::caution
Se archivi l'organizzazione di un lavoro, chi fa quel lavoro perde i permessi dentro l'organizzazione finché non la riattivi. QRM lo segnala nel log a ogni avvio.
:::

## Comandi

Ogni comando esiste anche sotto `/qrm org`. I comandi che indicano un giocatore agiscono sul suo personaggio attivo e richiedono che sia online.

| Comando | Chi | Effetto |
| --- | --- | --- |
| `/org` oppure `/org list` | Tutti | Elenca le organizzazioni del personaggio attivo. |
| `/org info <id>` | Tutti | Mostra nome, stato, rank, numero di membri e saldo. Chi non è operatore può vedere solo le proprie. |
| `/org create <id> <nome>` | Operatori | Crea un'organizzazione. |
| `/org archive <id>` | Operatori | Archivia e congela il conto. |
| `/org unarchive <id>` | Operatori | Riattiva. |
| `/org delete <id>` | Operatori | Elimina un'organizzazione archiviata, vuota e a saldo zero. |
| `/org rank set <org> <rank> <livello> [nodi…]` | Operatori | Crea o sostituisce un rank. |
| `/org rank remove <org> <rank>` | Operatori | Rimuove un rank non in uso. |
| `/org member add <org> <giocatore> <rank>` | Operatori | Aggiunge un membro. |
| `/org member setrank <org> <giocatore> <rank>` | Operatori | Cambia il rank di un membro. |
| `/org member remove <org> <giocatore>` | Operatori | Rimuove un membro. |

L'`id` di un'organizzazione segue le stesse regole di quello di un lavoro: inizia con una lettera minuscola, contiene solo minuscole, cifre e `_`, e ha al massimo 32 caratteri. Il nome ha da 1 a 64 caratteri.

Il completamento con Tab propone le organizzazioni e i rank esistenti. Ogni comando riservato agli operatori viene scritto nel registro delle azioni dello staff.
