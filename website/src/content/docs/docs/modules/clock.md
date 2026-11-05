---
title: Clock
description: Ora, data e stagione reali, a scopo informativo.
---

Clock dice che ora è, che giorno è e che stagione è, secondo il calendario reale. Non cambia il mondo: il giorno e la notte di Minecraft girano come sempre, e comandi come `/time set` funzionano normalmente.

**Disponibilità**: incluso nel progetto, **Sperimentale**. Va installato sul server. Sui client non serve.

## Installare

Copia `qrm-clock-<versione>.jar` in `mods/` sul server. Non ha permessi: il comando è per tutti.

Se hai usato una versione 0.37 di Clock che allungava il giorno, all'avvio il ritmo del sole viene riportato a quello normale di Minecraft.

## Ora, data e stagione

L'ora e la data sono quelle reali del server, nel suo fuso orario. Le stagioni seguono la data, nell'emisfero nord:

| Stagione | Dal |
| --- | --- |
| Primavera | 20 marzo |
| Estate | 21 giugno |
| Autunno | 22 settembre |
| Inverno | 21 dicembre |

Con `southernHemisphere` le stagioni si invertono. Per ora la stagione non cambia nulla nel mondo: dice solo che stagione è.

## Comando

| Comando | Cosa fa |
| --- | --- |
| `/qrm clock` | Dice che ora è, che giorno è e che stagione è. |

## Per chi sviluppa

Gli altri Module possono chiedere il tempo con `QRM.get(ClockService.class).now()`, che restituisce una `GameTime` con data, ora, minuti e stagione. Senza Clock installato la chiamata lancia `IllegalStateException`, quindi un Module che ne dipende in modo facoltativo deve gestirla.

## Configurazione

Le chiavi sono in `qrm_clock-server.toml`, spiegate in [Configurazione e file](/Quorum/docs/riferimento/configurazione/). Una modifica vale subito, senza riavviare.

## Limiti

- L'ora che Clock dice non coincide con il sole che si vede nel gioco.
- L'ora e la data non sono ancora visibili in un orologio da polso o in un calendario: per ora c'è solo il comando.
