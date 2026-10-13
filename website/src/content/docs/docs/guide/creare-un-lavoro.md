---
title: Creare un lavoro
description: Dal file vuoto a un lavoro assegnato, con un titolare e un dipendente in servizio.
---

Questa guida crea un'officina meccanica con due gradi, la assegna a due giocatori e verifica che funzioni. Servono i permessi di operatore e l'accesso ai file del server.

Le regole complete sono in [Lavori](/Quorum/docs/sistemi/lavori/).

## 1. Scrivi il file

Crea `<mondo>/qrm/data/jobs/mechanic.json`:

```json
{
  "id": "mechanic",
  "label": "Officina",
  "grades": [
    { "id": "apprentice", "label": "Apprendista", "level": 0, "permissions": ["mechanic.repair"] },
    { "id": "owner", "label": "Titolare", "level": 10, "permissions": ["mechanic.*"] }
  ]
}
```

Il nome del file è libero, purché finisca in `.json`. Conta l'`id` scritto dentro.

## 2. Riavvia il server

QRM legge i lavori solo all'avvio. Nel log, il numero dei lavori deve essere salito di uno:

```text
QRM started (1 currencies, 1 jobs)
```

Se il numero non è cambiato, cerca nel log la riga `Invalid job file`: indica il file e il motivo per cui è stato saltato.

Da operatore, `/job list` conferma che il lavoro è stato caricato:

```text
mechanic (Officina): apprentice[0], owner[10]
```

## 3. Assegna il titolare

Il giocatore deve essere online e avere un personaggio attivo.

```text
/job set <giocatore> mechanic owner
```

## 4. Assegna un dipendente

```text
/job set <giocatore> mechanic apprentice
```

## 5. Fai entrare in servizio

I permessi del grado valgono solo in servizio. Ogni giocatore entra con:

```text
/duty
```

## Verifica

Il dipendente, in servizio, scrive `/job` e legge:

```text
Lavoro: Officina - Apprendista  (in servizio)
Permessi: mechanic.repair
```

Il titolare legge `Permessi: mechanic.*, mechanic.repair`: i suoi e quelli ereditati dal grado inferiore.

Da operatore, `/org info mechanic` mostra l'organizzazione che QRM ha creato per il lavoro.

## Problemi comuni

| Cosa vedi | Causa |
| --- | --- |
| `Lavoro sconosciuto: mechanic` | Il file non è stato caricato. Controlla il log e riavvia. |
| `Grado sconosciuto: …` | L'`id` del grado non corrisponde a quello nel file. |
| `Il giocatore non ha un personaggio attivo.` | Il giocatore deve prima creare o scegliere un personaggio. |
| `Permessi: nessuno` | Il personaggio è fuori servizio. |
| `Il servizio si gestisce in azienda.` | `selfDuty` è `false` nella configurazione: i giocatori non possono cambiare il servizio con `/duty`. |
