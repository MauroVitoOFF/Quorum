---
title: Configurazione e file
description: Tutte le chiavi di configurazione di QRM e i file che crea.
---

## Dove stanno i file

| Percorso | Contenuto |
| --- | --- |
| `config/qrm-common.toml` | La configurazione di QRM, sul server. |
| `config/qrm-client.toml` | Le preferenze di ogni giocatore, sul suo client. |
| `<mondo>/qrm/qrm.db` | Il database SQLite. |
| `<mondo>/qrm/data/jobs/` | Un file JSON per ogni [lavoro](/Quorum/docs/sistemi/lavori/). |
| `<mondo>/qrm/data/currencies/` | Un file JSON per ogni [valuta](/Quorum/docs/sistemi/economia/). |
| `config/qrm_bank/cash.json` | I tagli del contante. Solo con il Module Bank. |
| `qrm_bank-server.toml` | La configurazione di Bank, fra le configurazioni del server. |

`<mondo>` è la cartella del mondo del server.

## Configurazione del server

Dopo aver modificato `config/qrm-common.toml` riavvia il server.

### Personaggi

Spiegate in [Personaggi](/Quorum/docs/sistemi/personaggi/).

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `characters.slots` | `3` | Personaggi per giocatore, da 1 a 50. Vale solo per i giocatori che entrano per la prima volta. |
| `characters.nameMinLength` | `2` | Lunghezza minima di nome e cognome. |
| `characters.nameMaxLength` | `24` | Lunghezza massima di nome e cognome. |
| `characters.namePattern` | `^[\p{L}][\p{L}' -]*$` | Espressione regolare per nome e cognome. |
| `characters.uniqueNames` | `true` | Vieta due personaggi con lo stesso nome e cognome. |

### Economia

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `economy.defaultCurrency` | `QRM` | La valuta usata dai comandi quando non è indicata, e mostrata nell'HUD. |

### Lavori

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `jobs.selfDuty` | `true` | Se `false`, i giocatori non cambiano il servizio con `/duty` né dall'hub. |

### Comandi

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `commands.shortAliases` | `true` | Registra le forme brevi dei comandi per i giocatori: `/char`, `/money`, `/bal`, `/pay`, `/job`, `/duty`, `/org`. Con `false` restano solo quelle sotto `/qrm`; `/boss` e `/carry` restano comunque. Da usare se un altro mod ha comandi con lo stesso nome. |

### Interfaccia

Spiegate in [Interfaccia del client](/Quorum/docs/sistemi/interfaccia/).

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `gui.hud.elements` | `["name", "balance", "job"]` | Le righe dell'HUD, in ordine. Vuoto: HUD nascosto. |
| `gui.hub.tabs` | `["accounts", "job", "orgs"]` | Le schede dell'hub, in ordine. Vuoto: hub disattivato. |

### Database

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `database.url` | vuoto | Lascia vuoto: QRM usa il file `<mondo>/qrm/qrm.db`. |
| `database.user` | vuoto | Non usata con SQLite. |
| `database.password` | vuoto | Non usata con SQLite. |

:::caution
Nella versione corrente QRM include solo il driver SQLite. Un database esterno non è disponibile.
:::

## Configurazione del client

Il file `config/qrm-client.toml` sta sul computer di ogni giocatore.

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `hud.visible` | `true` | Mostra l'HUD. Cambia anche con il tasto **H**. |

## Configurazione di Bank

Le chiavi di `qrm_bank-server.toml`, spiegate in [Bank](/Quorum/docs/modules/bank/). Una modifica vale subito, senza riavviare.

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `atm.maxDistance` | `6.0` | Distanza massima dallo sportello, in blocchi. |
| `atm.minIntervalMillis` | `250` | Tempo minimo fra due operazioni, in millisecondi. |
| `atm.historyRows` | `20` | Movimenti mostrati nello storico. |
| `atm.transfersEnabled` | `true` | Abilita i bonifici dallo sportello. |
| `atm.vipBalance` | `100000` | Saldo da cui compare il badge Cliente VIP; `0` lo disattiva. |
| `limits.maxPerOperation` | `100000` | Ritiro massimo per operazione, in unità minime. |
| `limits.maxPerDay` | `500000` | Ritiro massimo in 24 ore, in unità minime. |
| `limits.overrides` | vuoto | Limiti diversi per singole valute. |

Locker e Boss non hanno file di configurazione. Wardrobe legge le divise da `config/qrm_wardrobe/outfits/`, spiegata nella sua [pagina](/Quorum/docs/modules/wardrobe/).
