---
title: Primo avvio
description: Cosa crea QRM al primo avvio e come arrivare al primo personaggio con un saldo.
---

Questa procedura porta da un server appena installato a un personaggio attivo con del denaro sul conto. Servono i permessi di operatore.

## 1. Avvia il server

Al primo avvio QRM crea questi file:

| File | Contenuto |
| --- | --- |
| `config/qrm-common.toml` | La configurazione di QRM |
| `<mondo>/qrm/qrm.db` | Il database SQLite con tutti i dati |
| `<mondo>/qrm/data/currencies/qrm.json` | Una valuta di esempio, `QRM` |
| `<mondo>/qrm/data/jobs/` | La cartella dei lavori, vuota |

Nel log del server compare la riga:

```text
QRM started (1 currencies, 0 jobs)
```

Se QRM non riesce ad avviarsi, per esempio perché non può aprire il database, il server si ferma con l'errore `QRM could not start` seguito dal motivo.

## 2. Crea un personaggio

Entra nel server.

- **Con QRM sul client**: premi **O** per aprire la schermata dei personaggi e creane uno. Finché non hai un personaggio attivo, ogni 30 secondi un promemoria sopra la barra rapida ti ricorda il tasto.
- **Senza QRM sul client**: usa il comando.

  ```text
  /char create Mario Rossi 1990-05-17
  ```

Il primo personaggio che crei diventa subito quello attivo.

## 3. Dai del denaro al personaggio

Un personaggio nasce con un conto vuoto. Da operatore:

```text
/money give <giocatore> 500
```

L'importo è nella valuta predefinita, `QRM`. Il giocatore deve essere online e avere un personaggio attivo.

## 4. Controlla

- `/char` elenca i tuoi personaggi e segna quello attivo.
- `/money` mostra il saldo: `500.00 Q`.
- `/qrm admin audit` verifica che i saldi corrispondano alle transazioni registrate e risponde `Audit: tutti i saldi sono coerenti.`

## Passi successivi

- Sostituisci la valuta di esempio con la tua: vedi [Economia](/Quorum/docs/sistemi/economia/).
- [Crea il primo lavoro](/Quorum/docs/guide/creare-un-lavoro/).
- [Dai accesso allo staff](/Quorum/docs/guide/accesso-staff/) senza renderlo operatore.
