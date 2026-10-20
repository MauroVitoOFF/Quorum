---
title: Bank
description: Sportelli ATM, contante fisico e bonifici. Installazione, configurazione e comandi.
---

Bank aggiunge lo sportello ATM e il contante. Da uno sportello un personaggio ritira banconote dal proprio [conto](/Quorum/docs/sistemi/economia/), le deposita e fa bonifici. Le banconote sono oggetti: si tengono nell'inventario, si passano di mano e si possono perdere.

**Disponibilità**: incluso nel progetto. Va installato sul server e su ogni client, perché aggiunge un blocco e degli oggetti.

## Installare

1. Copia `qrm-bank-<versione>.jar` in `mods/`, sul server e sui client.
2. Avvia il server. Bank crea `config/qrm_bank/cash.json`, con i tagli della valuta di esempio, e il file `qrm_bank-server.toml` fra le configurazioni del server.
3. Procurati uno sportello con `/qrm bank atm give` e piazzalo. Lo sportello si trova anche fra i blocchi funzionali della modalità creativa.

Bank non crea un conto a parte: usa il conto che ogni personaggio ha già.

## Lo sportello

Il clic destro su uno sportello apre una finestra sola, divisa in tre colonne.

**A sinistra** c'è il titolare: la testa della sua skin, il nome, il codice del conto, il saldo e i contanti che ha con sé. Chi supera la soglia `atm.vipBalance` vede anche il badge Cliente VIP.

**Al centro** ci sono le operazioni. Il prelievo e il deposito rapidi hanno importi già pronti, oppure si scrive una cifra: il prelievo consegna banconote per quell'importo, il deposito versa quel contante dall'inventario. Sotto c'è il bonifico, con destinatario, importo e una causale facoltativa fino a 64 caratteri. Il destinatario si scrive come `Nome Cognome` per un personaggio, oppure `@id` per un'organizzazione. Chi riceve viene avvisato in chat, se è online.

**A destra** c'è lo storico del conto, con un'icona per ogni tipo di movimento. Un clic su una riga ne apre il dettaglio: controparte, data e causale.

L'esito di ogni operazione compare in basso, con un timbro se è riuscita. Per usare lo sportello serve un personaggio attivo, e bisogna restare entro la distanza configurata.

### Conti delle organizzazioni

Se il personaggio può prelevare dal conto di una o più [organizzazioni](/Quorum/docs/sistemi/organizzazioni/), cioè ha il nodo `org.account.withdraw`, compare il pulsante Conto. Serve a scegliere su quale conto operare: il personale, oppure uno aziendale. Con un conto aziendale scelto, saldo, storico, prelievi, depositi e bonifici riguardano quel conto. Se il permesso viene tolto mentre la finestra è aperta, lo sportello torna al conto personale.

| Risposta | Causa |
| --- | --- |
| `Limite di prelievo superato.` | L'importo supera il massimo per operazione, o il totale ritirato nelle ultime 24 ore supera il massimo giornaliero. |
| `Questo importo non si può pagare con le banconote disponibili.` | L'importo non è componibile con i tagli definiti. |
| `Non hai abbastanza spazio nell'inventario.` | Le banconote non ci stanno. |
| `Più personaggi hanno questo nome.` | Il nome del destinatario non è unico. |
| `Questo contante non è stato emesso dalla banca.` | Stai depositando più contante di quanto la banca ne abbia emesso. |

## Il contante

I tagli si definiscono in `config/qrm_bank/cash.json`, una voce per valuta:

```json sample="docs/samples/bank/cash.json"
{
  "QRM": { "decimals": 2, "denominations": [100, 500, 1000, 5000, 10000] }
}
```

| Campo | Regola |
| --- | --- |
| Nome della voce | Il codice di una valuta di QRM. |
| `decimals` | Lo stesso numero di decimali della valuta in QRM. |
| `denominations` | I tagli, in unità minime: con due decimali, `100` è una banconota da 1.00. Interi, maggiori di zero, tutti diversi, e ognuno multiplo del taglio più piccolo che lo precede. |

Ogni taglio diventa un oggetto del gioco. Per questo il file viene letto una sola volta, al caricamento del mod:

:::caution
Dopo aver cambiato `cash.json` vanno riavviati il server e i client, e il file deve essere identico su entrambi. Un file non valido impedisce l'avvio, con un errore che indica il file e il motivo.
:::

Una valuta senza voce in `cash.json` esiste solo sui conti: non si può ritirare. Se una voce cita una valuta che QRM non conosce, o con un numero diverso di decimali, il log del server lo segnala.

### La cassa

Il contante in circolazione è sempre coperto. Ogni ritiro sposta l'importo dal conto del personaggio a un conto interno, la cassa; ogni deposito lo riporta indietro. La somma delle banconote in gioco non può quindi superare il saldo della cassa, e `/qrm admin audit bank` controlla che sia così.

## Configurazione

Le chiavi stanno in `qrm_bank-server.toml`. Una modifica al file vale subito, senza riavviare.

| Chiave | Predefinito | Significato |
| --- | --- | --- |
| `atm.maxDistance` | `6.0` | Distanza massima dallo sportello, in blocchi. Da 2 a 16. |
| `atm.minIntervalMillis` | `250` | Tempo minimo fra due operazioni dello stesso giocatore, in millisecondi. |
| `atm.historyRows` | `20` | Quanti movimenti mostra lo storico. Da 1 a 50. |
| `atm.transfersEnabled` | `true` | Se `false`, i bonifici non funzionano. |
| `atm.vipBalance` | `100000` | Saldo, in unità intere della valuta, da cui compare il badge Cliente VIP. `0` lo disattiva. |
| `limits.maxPerOperation` | `100000` | Ritiro massimo per operazione, in unità minime. |
| `limits.maxPerDay` | `500000` | Ritiro massimo nelle ultime 24 ore per personaggio, in unità minime. |
| `limits.overrides` | vuoto | Limiti diversi per singole valute. |

Con due decimali i limiti predefiniti valgono 1000.00 per operazione e 5000.00 al giorno.

Una voce di `limits.overrides` ha la forma `CODICE:perOperazione:alGiorno`, per esempio `"EUR:50000:200000"`.

## Permessi

Due nodi regolano lo sportello. A differenza degli altri [permessi](/Quorum/docs/sistemi/permessi/), sono consentiti a tutti: servono solo a togliere l'accesso a un personaggio.

| Nodo | Cosa regola |
| --- | --- |
| `bank.atm.use` | Usare gli sportelli. |
| `bank.transfer` | Fare bonifici. |

```text
/qrm perm deny <giocatore> bank.transfer
```

Conta solo un `deny` scritto esattamente su quel nodo: un `deny` su `bank.*` non ha effetto.

## Comandi

Tutti riservati agli operatori, sotto `/qrm`. `<nome>` è il nome e cognome di un personaggio, e funziona anche se il giocatore è offline.

| Comando | Effetto |
| --- | --- |
| `/qrm bank atm give [giocatore]` | Dà uno sportello, a te o al giocatore indicato. |
| `/qrm bank balance <nome>` | Mostra i saldi di un personaggio. |
| `/qrm bank freeze <nome>` | Congela il conto: ogni movimento in entrata e in uscita viene rifiutato. |
| `/qrm bank unfreeze <nome>` | Scongela il conto. |
| `/qrm admin audit bank` | Controlla il registro delle transazioni e confronta il contante dei giocatori online con la cassa. `/qrm admin audit` senza argomento esegue anche il controllo dei saldi. |

## Nel pannello staff

Con il permesso `staff.bank`, il [pannello staff](/Quorum/docs/sistemi/pannello-staff/) ha una voce Banca che agisce su un giocatore online: mostra saldi e movimenti, dà e toglie denaro, congela e scongela il conto, consegna uno sportello ed esegue l'audit. Le azioni vengono scritte nel registro dello staff.
