---
title: Fare un backup e controllare i saldi
description: Quali file copiare per salvare i dati di QRM, come ripristinarli e come verificare i saldi.
---

Tutti i dati di QRM stanno in una cartella del mondo e in un file di configurazione. Servono l'accesso ai file del server e, per il controllo dei saldi, i permessi di operatore.

## Cosa copiare

| Percorso | Contenuto |
| --- | --- |
| `<mondo>/qrm/qrm.db` | Il database: personaggi, conti, transazioni, lavori assegnati, organizzazioni, permessi, punti di interazione, moderazione e registro dello staff. |
| `<mondo>/qrm/data/jobs/` | I file dei lavori. |
| `<mondo>/qrm/data/currencies/` | I file delle valute. |
| `config/qrm-common.toml` | La configurazione. |

Un backup dell'intera cartella del mondo comprende già i primi tre.

## Fare il backup

1. Ferma il server. Il database è un file SQLite: copiarlo mentre il server lo sta scrivendo può dare una copia incoerente.
2. Copia la cartella `<mondo>/qrm/` e il file `config/qrm-common.toml`.
3. Riavvia il server.

## Ripristinare

1. Ferma il server.
2. Sostituisci `<mondo>/qrm/` con la copia.
3. Riavvia.

Ripristina il database insieme ai file dei lavori e delle valute presi nello stesso momento. Un database che cita un lavoro senza il suo file lascia quei personaggi disoccupati finché il file non torna; un saldo in una valuta senza file viene mostrato in unità minime, seguito dal codice della valuta.

## Prima di un aggiornamento

All'avvio una versione nuova di QRM può modificare la struttura del database. Non c'è un comando per tornare indietro: fai sempre un backup prima di cambiare i jar.

## Controllare i saldi

```text
/qrm admin audit
```

Il comando ricalcola il saldo di ogni conto dalla somma delle transazioni e lo confronta con quello memorizzato.

- `Audit: tutti i saldi sono coerenti.` significa che i conti tornano.
- Altrimenti compare una riga per ogni conto che non torna, con la valuta, il saldo atteso e quello trovato.

Eseguilo dopo un ripristino e dopo un arresto anomalo del server. Se segnala un'incoerenza, non c'è un comando che la corregga: conserva il database così com'è e torna all'ultimo backup coerente.
