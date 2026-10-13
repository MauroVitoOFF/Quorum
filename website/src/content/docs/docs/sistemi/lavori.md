---
title: Lavori
description: Il file che definisce un lavoro, i gradi, il servizio e i comandi.
---

Un personaggio ha al massimo un lavoro, con un grado, e può essere in servizio o fuori servizio. I permessi del grado valgono solo mentre il personaggio è in servizio.

Per la procedura completa vedi [Creare un lavoro](/Quorum/docs/guide/creare-un-lavoro/).

## Il file di un lavoro

Ogni lavoro è un file JSON nella cartella `<mondo>/qrm/data/jobs/`. QRM legge i file all'avvio del server: dopo una modifica serve un riavvio.

```json
{
  "id": "police",
  "label": "Polizia",
  "grades": [
    { "id": "officer", "label": "Agente", "level": 0, "permissions": ["police.arrest"] },
    {
      "id": "chief",
      "label": "Comandante",
      "level": 10,
      "permissions": ["police.*"],
      "salary": { "currency": "QRM", "amount": "250.00" }
    }
  ]
}
```

| Campo | Regola |
| --- | --- |
| `id` | Inizia con una lettera minuscola e contiene solo minuscole, cifre e `_`. Al massimo 32 caratteri. |
| `label` | Il nome mostrato ai giocatori. Da 1 a 64 caratteri. |
| `grades` | Almeno un grado. |
| `grades[].id` | Stesse regole dell'`id` del lavoro. Unico dentro il lavoro. |
| `grades[].label` | Il nome del grado. Da 1 a 64 caratteri. |
| `grades[].level` | Un numero da 0 in su. Unico dentro il lavoro. |
| `grades[].permissions` | Facoltativo. I [nodi di permesso](/Quorum/docs/sistemi/permessi/) del grado. |
| `grades[].salary` | Facoltativo. Una valuta esistente e un importo. |

Un file che non rispetta queste regole viene saltato, e nel log compare `Invalid job file` con il motivo.

Tre cose da sapere:

- **I gradi ereditano verso l'alto.** Un grado ha i propri permessi più quelli di tutti i gradi con `level` inferiore. Nell'esempio il Comandante ha anche `police.arrest`.
- **Lo stipendio è solo un dato.** QRM lo legge e lo rende disponibile ai Modules, ma non paga nessuno.
- **Ogni lavoro ha un'organizzazione con lo stesso `id`.** QRM la crea all'avvio se manca: vedi [Organizzazioni](/Quorum/docs/sistemi/organizzazioni/).

## Assegnare un lavoro

```text
/job set <giocatore> <lavoro> <grado>
```

Il comando agisce sul personaggio attivo del giocatore, che deve essere online. Lo stesso comando serve a cambiare grado. Passare a un lavoro diverso mette il personaggio fuori servizio; cambiare solo il grado no.

`/job remove <giocatore>` toglie il lavoro.

## Il servizio

Il giocatore entra ed esce dal servizio con `/duty`, oppure dal pulsante dell'hub del personaggio. `/duty on` e `/duty off` impostano uno stato preciso.

Il personaggio esce dal servizio da solo quando:

- il giocatore si disconnette;
- il giocatore passa a un altro personaggio;
- il personaggio cambia lavoro o lo perde;
- il server si riavvia.

Se imposti `selfDuty = false` nella sezione `[jobs]` di `config/qrm-common.toml`, i giocatori non possono più cambiare il proprio servizio: `/duty` risponde `Il servizio si gestisce in azienda.` e il servizio si cambia solo dai punti di interazione di tipo `duty` o dai Modules. Il valore predefinito è `true`.

## Se un file sparisce

Se togli il file di un lavoro, o un grado, mentre dei personaggi lo hanno ancora, QRM non cancella l'assegnazione. Quei personaggi vengono trattati come disoccupati: non possono entrare in servizio e `/job` mostra `[definizione mancante]`. All'avvio il log segnala il lavoro rimasto senza definizione. Rimettendo il file tutto torna com'era.

## Comandi

Ogni comando esiste anche sotto `/qrm`: `/qrm job`, `/qrm duty`.

| Comando | Chi | Effetto |
| --- | --- | --- |
| `/job` oppure `/job info` | Tutti | Mostra lavoro, grado, stato del servizio e permessi del personaggio attivo. |
| `/duty` | Tutti | Entra in servizio, o ne esce. |
| `/duty on`, `/duty off` | Tutti | Imposta lo stato del servizio. |
| `/job set <giocatore> <lavoro> <grado>` | Operatori | Assegna un lavoro o cambia il grado. |
| `/job remove <giocatore>` | Operatori | Toglie il lavoro. |
| `/job list` | Operatori | Elenca i lavori definiti, con i gradi e i loro livelli. |

Il completamento con Tab propone i lavori definiti e i gradi del lavoro scelto. Gli operatori vedono in più i sottocomandi riservati a loro.
