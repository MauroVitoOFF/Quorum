---
title: Risolvere i problemi comuni
description: I messaggi e i comportamenti che si incontrano più spesso, con la causa e il rimedio.
---

## All'avvio del server

**Il server si ferma con `QRM could not start`.** Il motivo è scritto subito dopo, nella stessa riga del log. QRM non parte con dati incompleti: finché l'errore resta, il server non si avvia.

**Un lavoro o una valuta non compare.** Cerca nel log `Invalid job file` o `Invalid currency file`: la riga indica il file e l'errore. Gli altri file vengono caricati comunque. La riga `QRM started (… currencies, … jobs)` dice quanti ne sono stati letti.

**Nel log compare `is no longer defined`.** Dei personaggi hanno ancora un lavoro il cui file non c'è più. Vengono trattati come disoccupati finché il file non torna: vedi [Lavori](/Quorum/docs/sistemi/lavori/).

## Comandi

**`Nessun personaggio attivo.`** Il giocatore non ha scelto un personaggio. Deve crearne uno o selezionarlo: vedi [Personaggi](/Quorum/docs/sistemi/personaggi/).

**`Il giocatore non ha un personaggio attivo.`** Vale lo stesso per il giocatore indicato nel comando. I comandi degli operatori agiscono sul personaggio attivo di un giocatore online.

**`Valuta sconosciuta.`** Il codice non corrisponde a nessun file di valuta. Se succede senza indicare una valuta, la chiave `defaultCurrency` punta a una valuta che non esiste: vedi [Economia](/Quorum/docs/sistemi/economia/).

**`/job`, `/org` o `/money` fanno altro.** Un altro mod usa lo stesso nome. Imposta `shortAliases = false` nella sezione `[commands]` di `config/qrm-common.toml` e usa le forme lunghe, `/qrm job`, `/qrm org`, `/qrm money`.

**Un giocatore non vede `/job set` o `/org create`.** I sottocomandi riservati agli operatori sono nascosti agli altri.

## Lavori e permessi

**`/job` mostra `[definizione mancante]`.** Il file del lavoro, o il grado, non esiste più.

**`Permessi: nessuno`.** Il personaggio è fuori servizio: i permessi del grado valgono solo in servizio.

**`Il servizio si gestisce in azienda.`** `selfDuty` è `false`: il servizio si cambia da un [punto di interazione](/Quorum/docs/sistemi/punti-di-interazione/) di tipo `duty`, non con `/duty`.

## Interfaccia

**I tasti di QRM non fanno nulla.** Il giocatore non ha QRM sul client, oppure il server non lo ha.

**F7 non apre il pannello staff.** Al giocatore manca il permesso `staff.panel`: vedi [Dare accesso allo staff](/Quorum/docs/guide/accesso-staff/).

**L'HUD non si vede.** Il giocatore lo ha nascosto con **H**, oppure `gui.hud.elements` è vuoto: vedi [Interfaccia del client](/Quorum/docs/sistemi/interfaccia/).

**L'hub non si apre.** `gui.hub.tabs` è vuoto.

## Punti di interazione

**Un blocco non si rompe.** Ospita un punto. Il messaggio ne riporta il numero: rimuovilo con `/qrm admin point remove <numero>`.

**Un punto non reagisce.** È inattivo. Guardalo e scrivi `/qrm admin point info`: la riga `Stato` indica la causa.

## Saldi

**Un saldo non torna.** Esegui `/qrm admin audit`: vedi [Fare un backup e controllare i saldi](/Quorum/docs/guide/backup-e-saldi/).
