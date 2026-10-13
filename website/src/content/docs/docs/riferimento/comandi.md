---
title: Comandi
description: Tutti i comandi di QRM, con chi può usarli e la pagina che li spiega.
---

Ogni comando breve esiste anche sotto `/qrm`, con gli stessi argomenti: `/job` e `/qrm job`, `/org` e `/qrm org`, e così via. Le forme brevi si disattivano con `commands.shortAliases`: vedi [Configurazione e file](/Quorum/docs/riferimento/configurazione/).

"Operatori" indica chi ha il livello di permesso di Minecraft che consente di cambiare modalità di gioco. I sottocomandi riservati agli operatori sono nascosti agli altri giocatori.

## Personaggi

Spiegati in [Personaggi](/Quorum/docs/sistemi/personaggi/).

| Comando | Forma lunga | Chi |
| --- | --- | --- |
| `/char` | `/qrm character` | Tutti |
| `/char list` | `/qrm character list` | Tutti |
| `/char create <nome> <cognome> <nascita>` | `/qrm character create …` | Tutti |
| `/char select <n>` | `/qrm character select <n>` | Tutti |
| `/char archive <n>` | `/qrm character archive <n>` | Tutti |

## Economia

Spiegati in [Economia](/Quorum/docs/sistemi/economia/).

| Comando | Forma lunga | Chi |
| --- | --- | --- |
| `/money` | `/qrm money` | Tutti |
| `/balance`, `/bal` | `/qrm balance` | Tutti |
| `/pay <giocatore> <importo> [valuta]` | `/qrm pay …` | Tutti |
| `/money pay <giocatore> <importo> [valuta]` | `/qrm money pay …` | Tutti |
| `/money give <giocatore> <importo> [valuta]` | `/qrm money give …` | Operatori |
| `/money take <giocatore> <importo> [valuta]` | `/qrm money take …` | Operatori |
| | `/qrm admin audit` | Operatori |

## Lavori

Spiegati in [Lavori](/Quorum/docs/sistemi/lavori/).

| Comando | Forma lunga | Chi |
| --- | --- | --- |
| `/job` | `/qrm job` | Tutti |
| `/job info` | `/qrm job info` | Tutti |
| `/duty` | `/qrm duty` | Tutti |
| `/duty on`, `/duty off` | `/qrm duty on`, `/qrm duty off` | Tutti |
| `/job set <giocatore> <lavoro> <grado>` | `/qrm job set …` | Operatori |
| `/job remove <giocatore>` | `/qrm job remove …` | Operatori |
| `/job list` | `/qrm job list` | Operatori |

## Permessi

Spiegati in [Permessi](/Quorum/docs/sistemi/permessi/).

| Comando | Forma lunga | Chi |
| --- | --- | --- |
| `/perm grant <giocatore> <nodo>` | `/qrm perm grant …` | Operatori |
| `/perm deny <giocatore> <nodo>` | `/qrm perm deny …` | Operatori |
| `/perm clear <giocatore> <nodo>` | `/qrm perm clear …` | Operatori |

## Organizzazioni

Spiegati in [Organizzazioni](/Quorum/docs/sistemi/organizzazioni/).

| Comando | Forma lunga | Chi |
| --- | --- | --- |
| `/org` | `/qrm org` | Tutti |
| `/org list` | `/qrm org list` | Tutti |
| `/org info <id>` | `/qrm org info <id>` | Tutti |
| `/org create <id> <nome>` | `/qrm org create …` | Operatori |
| `/org archive <id>` | `/qrm org archive <id>` | Operatori |
| `/org unarchive <id>` | `/qrm org unarchive <id>` | Operatori |
| `/org delete <id>` | `/qrm org delete <id>` | Operatori |
| `/org rank set <org> <rank> <livello> [nodi…]` | `/qrm org rank set …` | Operatori |
| `/org rank remove <org> <rank>` | `/qrm org rank remove …` | Operatori |
| `/org member add <org> <giocatore> <rank>` | `/qrm org member add …` | Operatori |
| `/org member setrank <org> <giocatore> <rank>` | `/qrm org member setrank …` | Operatori |
| `/org member remove <org> <giocatore>` | `/qrm org member remove …` | Operatori |

## Punti di interazione

Spiegati in [Punti di interazione](/Quorum/docs/sistemi/punti-di-interazione/). Non hanno una forma breve e sono tutti riservati agli operatori.

| Comando |
| --- |
| `/qrm admin point create <tipo> job <lavoro> [grado]` |
| `/qrm admin point create <tipo> org <organizzazione> [rank]` |
| `/qrm admin point set grade <grado>` |
| `/qrm admin point set face <faccia>` |
| `/qrm admin point set public <valore>` |
| `/qrm admin point set name <nome>` |
| `/qrm admin point move <numero>` |
| `/qrm admin point remove [numero]` |
| `/qrm admin point info` |
| `/qrm admin point list` |
| `/qrm admin point list job <lavoro>` |
| `/qrm admin point list org <organizzazione>` |

## Staff

Spiegati in [Dare accesso allo staff](/Quorum/docs/guide/accesso-staff/) e in [Moderazione e registro delle azioni](/Quorum/docs/sistemi/moderazione-e-registro/). Non hanno una forma breve e sono tutti riservati agli operatori.

| Comando |
| --- |
| `/qrm admin staff grant <giocatore> <nodo>` |
| `/qrm admin staff revoke <giocatore> <nodo>` |
| `/qrm admin staff list <giocatore>` |
| `/qrm admin log` |
| `/qrm admin log <giocatore> [n]` |
| `/qrm admin log prune <giorni>` |

## Modules

I comandi di un Module esistono solo se il Module è installato.

| Comando | Chi | Spiegato in |
| --- | --- | --- |
| `/bank atm give [giocatore]` | Operatori | [Bank](/Quorum/docs/modules/bank/) |
| `/bank balance <nome>` | Operatori | [Bank](/Quorum/docs/modules/bank/) |
| `/bank freeze <nome>` | Operatori | [Bank](/Quorum/docs/modules/bank/) |
| `/bank unfreeze <nome>` | Operatori | [Bank](/Quorum/docs/modules/bank/) |
| `/bank audit` | Operatori | [Bank](/Quorum/docs/modules/bank/) |
| `/boss [organizzazione]` | Chi ha `boss.menu` | [Boss](/Quorum/docs/modules/boss/) |
