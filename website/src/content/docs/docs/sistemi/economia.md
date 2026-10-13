---
title: Economia
description: Valute, conti, comandi per il denaro e controllo dei saldi.
---

Il denaro in QRM sta sui conti. Ogni personaggio riceve un conto alla creazione, e un conto può contenere importi in più valute. Un conto non può andare sotto zero.

## Valute

Le valute si definiscono con un file JSON ciascuna, nella cartella `<mondo>/qrm/data/currencies/`. QRM le legge all'avvio del server: dopo una modifica serve un riavvio.

```json sample="docs/samples/currencies/eur.json"
{ "code": "EUR", "symbol": "€", "decimals": 2 }
```

| Campo | Regola |
| --- | --- |
| `code` | Da 2 a 16 caratteri fra lettere maiuscole, cifre e `_`. È il nome usato nei comandi. |
| `symbol` | Il testo mostrato dopo l'importo, per esempio `12.50 €`. |
| `decimals` | Quante cifre decimali ha la valuta. Da 0 a 8. |

Un file non valido viene saltato e nel log compare `Invalid currency file`, con il motivo.

Se la cartella è vuota, QRM scrive `qrm.json` con una valuta di esempio: codice `QRM`, simbolo `Q`, due decimali.

### Valuta predefinita

La chiave `defaultCurrency` della sezione `[economy]` di `config/qrm-common.toml` indica la valuta usata dai comandi quando non ne specifichi una, e quella mostrata nell'HUD. Il valore predefinito è `QRM`.

:::caution
Se togli la valuta di esempio, cambia anche `defaultCurrency`. Altrimenti i comandi senza valuta rispondono `Valuta sconosciuta.`
:::

## Pagare un altro giocatore

```text
/pay <giocatore> <importo> [valuta]
```

Il denaro passa dal conto del tuo personaggio attivo a quello del personaggio attivo del destinatario. Entrambi i giocatori devono essere online e avere un personaggio attivo.

L'importo si scrive con il punto come separatore decimale, per esempio `12.50`, deve essere maggiore di zero e non può avere più decimali di quelli della valuta.

| Risposta | Causa |
| --- | --- |
| `Fondi insufficienti.` | Il saldo non copre l'importo. |
| `Il destinatario non ha un personaggio attivo.` | Il destinatario non ha scelto un personaggio. |
| `Conto congelato.` | Uno dei due conti è congelato. |
| `Valuta sconosciuta.` | Il codice della valuta non corrisponde a nessun file. |

## Dare e togliere denaro

Gli operatori creano e ritirano denaro con due comandi:

```text
/money give <giocatore> <importo> [valuta]
/money take <giocatore> <importo> [valuta]
```

`give` accredita il personaggio attivo del giocatore. `take` gli addebita l'importo, e fallisce con `Fondi insufficienti.` se il saldo non basta.

Il denaro non compare e non sparisce: `give` lo preleva da un conto di sistema, che è l'unico a poter andare in negativo, e `take` lo riporta lì. Entrambi i comandi vengono scritti nel registro delle azioni dello staff.

## Controllare i saldi

```text
/qrm admin audit
```

Il comando confronta il saldo di ogni conto con la somma delle sue transazioni. Se tutto torna risponde `Audit: tutti i saldi sono coerenti.`, altrimenti elenca i conti con il saldo atteso e quello trovato.

## Comandi

Ogni comando esiste anche sotto `/qrm`: `/qrm money`, `/qrm balance`, `/qrm pay`.

| Comando | Chi | Effetto |
| --- | --- | --- |
| `/money`, `/balance`, `/bal` | Tutti | Mostra i saldi del personaggio attivo, una riga per valuta. |
| `/pay <giocatore> <importo> [valuta]`, `/money pay …` | Tutti | Paga un altro giocatore. |
| `/money give <giocatore> <importo> [valuta]` | Operatori | Accredita denaro. |
| `/money take <giocatore> <importo> [valuta]` | Operatori | Addebita denaro. |
| `/qrm admin audit` | Operatori | Verifica la coerenza dei saldi. |

Il completamento con Tab propone le valute definite.
