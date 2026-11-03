---
title: Clock
description: Il giorno di gioco più lungo, con la data e la stagione che seguono il calendario reale.
---

Clock dà al server un tempo suo. Il sole e la luna girano più piano di quanto fanno in Minecraft, così una giornata ha il tempo di essere vissuta. La data, invece, è quella vera: il 25 dicembre di gioco è il Natale vero. La stagione segue la data.

**Disponibilità**: incluso nel progetto, **Sperimentale**. Va installato sul server. Sui client non serve: chi non ha il mod vede comunque il sole muoversi.

## Installare

Copia `qrm-clock-<versione>.jar` in `mods/` sul server. Non ha permessi: il comando è per tutti.

## Il giorno

Un giorno di gioco dura **60 minuti reali**, al posto dei 20 di Minecraft. Un'ora di gioco dura quindi due minuti e mezzo.

La mezzanotte di gioco cade sempre allo scoccare di un'ora reale: alle 21:00 reali è mezzanotte nel gioco, alle 21:15 è l'alba, alle 21:30 è mezzogiorno, alle 21:45 è il tramonto. Il tempo non dipende da quando il server si è acceso: dopo un riavvio il sole è già dov'è giusto che sia.

Perché a mezzanotte reale cominci sempre un ciclo, la durata del giorno deve dividere 1440 minuti: 10, 12, 15, 20, 24, 30, 36, 40, 45, 48, 60, 72, 90, 120 o 180.

## La data e la stagione

La data è quella reale del server, nel suo fuso orario. Cambia a mezzanotte reale, che è anche una mezzanotte di gioco: in un giorno reale si susseguono molti giorni di gioco, tutti con la stessa data.

Le stagioni cambiano come nella realtà, nell'emisfero nord:

| Stagione | Dal |
| --- | --- |
| Primavera | 20 marzo |
| Estate | 21 giugno |
| Autunno | 22 settembre |
| Inverno | 21 dicembre |

Con `southernHemisphere` le stagioni si invertono. Per ora la stagione non cambia nulla nel mondo: dice solo che stagione è. Gli effetti visibili verranno dopo.

## Comando

| Comando | Cosa fa |
| --- | --- |
| `/qrm clock` | Dice che ora è, che giorno è e che stagione è. |

## Per chi sviluppa

Gli altri Module possono chiedere il tempo con `QRM.get(ClockService.class).now()`, che restituisce una `GameTime` con data, ora, minuti e stagione. Senza Clock installato la chiamata lancia `IllegalStateException`, quindi un Module che ne dipende in modo facoltativo deve gestirla.

## Configurazione

Le chiavi sono in `qrm_clock-server.toml`, spiegate in [Configurazione e file](/Quorum/docs/riferimento/configurazione/). Una modifica vale subito, senza riavviare.

## Limiti

- Dormire per saltare la notte sposta il sole, che torna al suo posto entro un secondo.
- Un comando come `/time set` o una pausa del ciclo giorno e notte hanno effetto per un secondo al massimo: poi Clock rimette il sole dov'è. Per usarli, spegni `controlWorldTime`.
- Il tempo dell'Overworld è l'unico che Clock governa. Il Nether e l'End non hanno un ciclo del giorno.
- Il giorno di gioco non è ancora visibile in un orologio da polso o in un calendario: per ora c'è solo il comando.
