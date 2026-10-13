---
title: Locker
description: Un armadietto personale per ogni personaggio, aperto da un punto di interazione.
---

Locker dà a ogni personaggio un armadietto personale da 27 slot. L'armadietto si apre da un [punto di interazione](/Quorum/docs/sistemi/punti-di-interazione/) di tipo `locker`: due personaggi che usano lo stesso punto vedono ciascuno il proprio contenuto.

**Disponibilità**: incluso nel progetto. Va installato sul server. Sul client è facoltativo: senza, l'armadietto si apre con l'aspetto di un baule normale.

## Installare

1. Copia `qrm-locker-<versione>.jar` in `mods/` del server.
2. Riavvia.

Locker non ha comandi né file di configurazione propri.

## Creare un armadietto

Guarda un blocco e crea un punto di tipo `locker`, di un lavoro oppure di un'organizzazione:

```text
/qrm admin point create locker job police
```

Valgono le regole di tutti i punti: chi può usarlo, grado minimo, icona. Per un lavoro non serve essere in servizio.

## Come funziona

- **Un armadietto per proprietario, non per blocco.** Tutti i punti `locker` dello stesso lavoro aprono lo stesso armadietto del personaggio. Una caserma con tre punti dà tre accessi allo stesso contenuto, non tre armadietti.
- **Un armadietto per personaggio.** Due personaggi dello stesso giocatore hanno armadietti separati.
- **Il contenuto sopravvive al punto.** Rimuovere il punto non cancella il contenuto: ricreandone uno per lo stesso proprietario, ognuno ritrova i propri oggetti.
- **Si chiude da solo** se il giocatore si allontana di più di 8 blocchi o cambia personaggio.

Il contenuto viene salvato quando cambia, alla chiusura e allo spegnimento del server. Sta nel database di QRM, quindi rientra nel normale [backup](/Quorum/docs/guide/backup-e-saldi/).

## Se un armadietto non si apre

Il messaggio `Il tuo armadietto non si può aprire in questo momento. Contatta lo staff.` significa che i dati salvati di quell'armadietto non sono leggibili. Locker non li sovrascrive, per non perdere ciò che contengono, e il log del server riporta il dettaglio.

Un oggetto di un mod che è stato disinstallato non blocca l'armadietto: non viene mostrato, ma resta salvato e ricompare quando il mod torna.
