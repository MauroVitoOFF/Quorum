---
title: Personaggi
description: Come funzionano i personaggi, i comandi per gestirli e le chiavi di configurazione.
---

Ogni giocatore ha uno o più personaggi e ne usa uno alla volta, il personaggio attivo. Lavoro, conti, permessi e organizzazioni appartengono al personaggio, non al giocatore.

Un personaggio ha nome, cognome e data di nascita.

## Creare un personaggio

Con QRM sul client si usa la schermata dei personaggi, che si apre con **O**. Senza, o in alternativa, il comando:

```text
/char create <nome> <cognome> <nascita>
```

`<nascita>` è nel formato `AAAA-MM-GG`. Un nome o un cognome che contiene spazi, apostrofi o lettere accentate va scritto fra virgolette.

La creazione viene rifiutata in questi casi:

- Il giocatore ha già tutti gli slot occupati. I personaggi archiviati non contano.
- Il nome o il cognome è più corto o più lungo dei limiti configurati, oppure contiene caratteri non ammessi. Con la configurazione predefinita sono ammesse lettere, spazi, apostrofi e trattini, e il primo carattere deve essere una lettera.
- Esiste già un personaggio non archiviato con lo stesso nome e cognome. Il confronto non distingue maiuscole e minuscole.

Dalla schermata la data di nascita deve essere compresa fra il 1° gennaio 1900 e oggi. Il comando non applica questo limite.

Se il giocatore non ha ancora un personaggio attivo, quello appena creato lo diventa. Alla creazione il personaggio riceve un [conto](/Quorum/docs/sistemi/economia/).

## Scegliere il personaggio attivo

```text
/char select <n>
```

`<n>` è il numero mostrato da `/char list`. Quando un giocatore cambia personaggio, gli altri suoi personaggi escono dal [servizio](/Quorum/docs/sistemi/lavori/).

Senza un personaggio attivo i comandi di QRM rispondono `Nessun personaggio attivo.`

## Archiviare un personaggio

```text
/char archive <n>
```

L'archiviazione non si annulla: non c'è un comando per riattivare un personaggio archiviato. Lo slot e il nome tornano liberi. Se il personaggio archiviato era quello attivo, il giocatore resta senza personaggio attivo finché non ne sceglie un altro.

## Comandi

`/char` e `/qrm character` sono lo stesso comando. Ogni giocatore agisce solo sui propri personaggi.

| Comando | Effetto |
| --- | --- |
| `/char` oppure `/char list` | Elenca i personaggi non archiviati e segna quello attivo. |
| `/char create <nome> <cognome> <nascita>` | Crea un personaggio. |
| `/char select <n>` | Rende attivo il personaggio numero `n`. |
| `/char archive <n>` | Archivia il personaggio numero `n`. |

## Configurazione

Le chiavi stanno nella sezione `[characters]` di `config/qrm-common.toml`.

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `slots` | `3` | Quanti personaggi può avere un giocatore. Da 1 a 50. |
| `nameMinLength` | `2` | Lunghezza minima di nome e cognome, ciascuno. Da 1 a 64. |
| `nameMaxLength` | `24` | Lunghezza massima di nome e cognome, ciascuno. Da 1 a 64. |
| `namePattern` | `^[\p{L}][\p{L}' -]*$` | Espressione regolare che nome e cognome devono rispettare. |
| `uniqueNames` | `true` | Se `true`, due personaggi non archiviati non possono avere lo stesso nome e cognome. |

:::caution[`slots` vale solo per i nuovi giocatori]
Il numero di slot viene assegnato a un giocatore la prima volta che entra nel server e poi resta quello. Cambiare `slots` non modifica i giocatori già registrati, e non c'è un comando per farlo.
:::
