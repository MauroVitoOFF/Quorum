---
title: Interfaccia del client
description: I tasti, l'HUD, l'hub del personaggio e le chiavi che ne decidono il contenuto.
---

L'interfaccia è disponibile ai giocatori che hanno QRM sul client, quando il server ha QRM. Chi non ha il mod usa i comandi.

## Tasti

| Tasto | Apre |
| --- | --- |
| **O** | La schermata dei personaggi. |
| **K** | L'hub del personaggio. |
| **H** | Mostra o nasconde l'HUD. |
| **F7** | Il [pannello staff](/Quorum/docs/sistemi/pannello-staff/). |

Ogni giocatore può cambiarli dai controlli di Minecraft, nella categoria QRM.

## HUD

L'HUD mostra, in alto a sinistra, alcune righe sul personaggio attivo:

| Riga | Contenuto |
| --- | --- |
| `name` | Nome e cognome del personaggio. |
| `balance` | Il saldo nella [valuta predefinita](/Quorum/docs/sistemi/economia/). |
| `job` | Il lavoro e lo stato del servizio. |

L'HUD si nasconde quando è aperta una schermata. La scelta fatta con **H** viene salvata sul client, nella chiave `visible` della sezione `[hud]` di `config/qrm-client.toml`.

## Hub del personaggio

L'hub è una schermata a schede:

| Scheda | Contenuto |
| --- | --- |
| `accounts` | I conti del personaggio. |
| `job` | Il lavoro, con il pulsante per entrare e uscire dal servizio. |
| `orgs` | Le organizzazioni a cui il personaggio appartiene. |

Il pulsante del servizio segue la stessa regola di `/duty`: non funziona se `selfDuty` è `false`.

## Cosa decide il server

Righe dell'HUD e schede dell'hub le sceglie il server, e valgono per tutti i giocatori. Le chiavi stanno in `config/qrm-common.toml`.

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `gui.hud.elements` | `["name", "balance", "job"]` | Le righe dell'HUD, nell'ordine indicato. Un elenco vuoto nasconde l'HUD a tutti. |
| `gui.hub.tabs` | `["accounts", "job", "orgs"]` | Le schede dell'hub, nell'ordine indicato. Un elenco vuoto disattiva l'hub. |

In `gui.hud.elements` un Module può aggiungere righe proprie, indicate come `modulo:riga`. I nomi che QRM non riconosce vengono ignorati.

## Aspetto

Colori e immagini dell'interfaccia si cambiano con un resource pack: vedi [Cambiare l'aspetto dell'interfaccia](/Quorum/docs/guide/aspetto-interfaccia/).
