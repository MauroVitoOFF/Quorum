---
title: Boss
description: Il menu con cui chi dirige un'azienda gestisce membri e conto senza comandi da operatore.
---

Boss aggiunge un menu con cui chi dirige un'[organizzazione](/Quorum/docs/sistemi/organizzazioni/) ne gestisce i membri e il conto. Sostituisce, per chi non è operatore, i comandi `/job set` e `/org member`.

**Disponibilità**: incluso nel progetto. Va installato sul server e sui client di chi deve usare il menu, che è una schermata.

## Installare

1. Copia `qrm-boss-<versione>.jar` in `mods/`, sul server e sui client.
2. Riavvia il server.
3. Dai i permessi a chi deve usare il menu: vedi sotto.

Boss non ha file di configurazione.

## Aprire il menu

- Con il comando `/boss`. Se il personaggio dirige più aziende, compare un elenco da cui scegliere; `/boss <id>` apre direttamente quella indicata.
- Da un [punto di interazione](/Quorum/docs/sistemi/punti-di-interazione/) di tipo `boss_desk`, che accetta come proprietario solo un'organizzazione:

  ```text
  /qrm admin point create boss_desk org <organizzazione>
  ```

  Sul punto valgono le regole delle organizzazioni: lo usa solo chi è stato aggiunto come membro con `/org member add`. Chi dirige un'azienda di un lavoro senza esserne anche membro apre il menu con `/boss`.

Serve un personaggio attivo. Chi non ha il permesso su nessuna azienda legge `Non puoi aprire un menu boss.`

## Permessi

Il menu ha tre sezioni, e ognuna richiede i suoi nodi.

| Nodo | Cosa permette |
| --- | --- |
| `boss.menu` | Aprire il menu e vedere la Panoramica. È necessario per tutto il resto. |
| `boss.members.manage` | La sezione Membri. |
| `boss.bank.view` | Vedere il saldo del conto aziendale. |
| `boss.bank.deposit` | Versare sul conto aziendale. |
| `org.account.withdraw` | Prelevare dal conto aziendale. |

I nodi valgono dentro una singola azienda, e si danno in modo diverso a seconda di come è nata.

**Azienda di un lavoro.** Scrivi i nodi nei `permissions` di un grado, nel [file del lavoro](/Quorum/docs/sistemi/lavori/). Valgono mentre il personaggio è in servizio.

```json sample="docs/samples/jobs/garage.json"
{
  "id": "garage",
  "label": "Autorimessa",
  "grades": [
    { "id": "employee", "label": "Dipendente", "level": 0 },
    {
      "id": "owner",
      "label": "Titolare",
      "level": 10,
      "permissions": [
        "boss.menu",
        "boss.members.manage",
        "boss.bank.view",
        "boss.bank.deposit",
        "org.account.withdraw"
      ]
    }
  ]
}
```

**Organizzazione creata con un comando.** Dai i nodi a un rank:

```text
/org rank set <organizzazione> <rank> <livello> boss.menu boss.members.manage boss.bank.view
```

## Le sezioni

### Panoramica

Mostra il nome dell'azienda, lo stato, il tuo rank o grado, il numero di membri e, se hai `boss.bank.view`, il saldo.

### Membri

Elenca i membri e permette di aggiungerne, promuoverli, declassarli e licenziarli. Il comportamento dipende dall'azienda.

**In un'azienda di un lavoro** i membri sono i personaggi che hanno quel lavoro, e i rank sono i gradi:

- Aggiungere un giocatore gli assegna il lavoro con il grado più basso. Si può aggiungere solo chi non ha già un lavoro: altrimenti il menu risponde `Questa persona ha già un lavoro.`
- Promuovere e declassare cambiano il grado.
- Licenziare toglie il lavoro.

**In un'organizzazione senza lavoro** si aggiungono e si tolgono membri, e si cambia il loro rank.

In entrambi i casi:

- Si possono aggiungere solo giocatori online.
- Non puoi agire su te stesso.
- Non puoi agire su chi ha un livello superiore al tuo, né dare un livello superiore al tuo.
- Chi ha il permesso ma non ha un rank o un grado proprio vede i membri in sola lettura.

### Conto

Versa dal conto personale a quello dell'azienda e preleva nel verso opposto. L'importo si sceglie come cifra libera, come percentuale o come totale.

:::caution[Una sola valuta]
Il menu lavora su una sola valuta, la prima fra quelle definite sul server, anche se la valuta predefinita è un'altra. Con una sola valuta definita non cambia nulla.
:::

## Problemi comuni

| Cosa vedi | Causa |
| --- | --- |
| `Non puoi aprire un menu boss.` | Manca `boss.menu`. In un'azienda di un lavoro, controlla di essere in servizio. |
| `Azienda sconosciuta.` | L'`id` è sbagliato, oppure non hai `boss.menu` su quell'azienda. Il messaggio è lo stesso nei due casi. |
| `Questa azienda non è più disponibile.` | L'organizzazione è stata archiviata. |
| `Il personaggio è cambiato. Riapri il menu.` | Hai cambiato personaggio con il menu aperto. |
| `Sola lettura.` | Hai il permesso ma non un rank o un grado nell'azienda. |
