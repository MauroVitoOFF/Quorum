---
title: Boss
description: Il menu con cui chi dirige un'azienda gestisce membri, bacheca, titoli e bonus senza comandi da operatore.
---

Boss aggiunge un menu con cui chi dirige un'[organizzazione](/Quorum/docs/sistemi/organizzazioni/) ne gestisce i membri e ne anima la vita interna, con una bacheca, i titoli di ruolo e i bonus ai dipendenti. Sostituisce, per chi non è operatore, i comandi `/job set` e `/org member`.

**Disponibilità**: incluso nel progetto. Va installato sul server e sui client di chi deve usare il menu, che è una schermata.

## Installare

1. Copia `qrm-boss-<versione>.jar` in `mods/`, sul server e sui client.
2. Riavvia il server.
3. Dai i permessi a chi deve usare il menu: vedi sotto.

Boss non ha file di configurazione.

## Aprire il menu

- Con il comando `/boss` (o `/qrm boss`). Se il personaggio dirige più aziende, compare un elenco da cui scegliere; `/boss <id>` apre direttamente quella indicata.
- Da un [punto di interazione](/Quorum/docs/sistemi/punti-di-interazione/) di tipo `boss_desk`, che accetta come proprietario solo un'organizzazione:

  ```text
  /qrm admin point create boss_desk org <organizzazione>
  ```

  Sul punto valgono le regole delle organizzazioni: lo usa solo chi è stato aggiunto come membro con `/org member set`. Chi dirige un'azienda di un lavoro senza esserne anche membro apre il menu con `/boss`.

Serve un personaggio attivo. Chi non ha il permesso su nessuna azienda legge `Non puoi aprire un menu boss.`

## Permessi

Il menu ha due sezioni. Ogni azione richiede il suo nodo.

| Nodo | Cosa permette |
| --- | --- |
| `boss.menu` | Aprire il menu e vedere la Panoramica. È necessario per tutto il resto. |
| `boss.members.manage` | Aggiungere membri, cambiare il loro rank o grado e licenziarli. |
| `boss.notice` | Scrivere e togliere l'avviso della bacheca. |
| `boss.title` | Assegnare il titolo interno ai membri. |
| `boss.bonus` | Dare un bonus a un dipendente. |

La sezione Membri compare con `boss.members.manage`, `boss.title` oppure `boss.bonus`.

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
        "boss.notice",
        "boss.title",
        "boss.bonus"
      ]
    }
  ]
}
```

**Organizzazione creata con un comando.** Dai i nodi a un rank:

```text
/org rank set <organizzazione> <rank> <livello> boss.menu boss.members.manage boss.notice
```

## Le sezioni

### Panoramica

In alto ci sono gli indicatori: quanti membri ha l'azienda, quanti sono online, quanti in servizio (solo per le aziende di un lavoro) e il tuo rank o grado. Sotto c'è la **bacheca**: un avviso fisso del boss, con il nome di chi l'ha scritto e la data. Chi ha `boss.notice` lo pubblica o lo toglie da qui, con un massimo di 160 caratteri. Quando lo pubblica, i membri online lo ricevono anche in chat. C'è un solo avviso alla volta: uno nuovo sostituisce il precedente.

### Membri

Elenca i membri, ognuno con la testa della sua skin. La testa è a colori se il giocatore è online e, nelle aziende di un lavoro, in servizio; è in grigio se è offline o fuori servizio. Sotto il nome compare il titolo interno, oppure lo stato di presenza. Un clic su un membro apre la sua scheda, con le azioni che puoi fare su di lui (Ruolo, Titolo e Bonus) e il suo fascicolo.

**Ruolo.** Promuovere, declassare e licenziare. Il comportamento dipende dall'azienda.

In un'azienda di un lavoro i membri sono i personaggi che hanno quel lavoro, e i rank sono i gradi:

- Aggiungere un giocatore gli assegna il lavoro con il grado più basso. Si può aggiungere solo chi non ha già un lavoro: altrimenti il menu risponde `Questa persona ha già un lavoro.`
- Promuovere e declassare cambiano il grado.
- Licenziare toglie il lavoro.

In un'organizzazione senza lavoro si aggiungono e si tolgono membri, e si cambia il loro rank.

In entrambi i casi:

- Si possono aggiungere solo giocatori online.
- Non puoi agire su te stesso.
- Non puoi agire su chi ha un livello superiore al tuo, né dare un livello superiore al tuo.
- Chi ha il permesso ma non ha un rank o un grado proprio vede i membri in sola lettura.

**Titolo.** Un nome di ruolo libero, di al massimo 24 caratteri ("Capocantiere", "Cassiere"), che compare accanto al nome nell'elenco dei membri. Vale per i membri di livello pari o inferiore al tuo, te compreso. Non cambia il rank né i permessi, e si vede solo nel menu.

**Bonus.** Una cifra pagata dal conto dell'azienda al conto personale del dipendente, con un motivo facoltativo di 40 caratteri. Si scrive la cifra oppure si sceglie uno degli importi rapidi. Il motivo finisce nello storico della banca; se il dipendente è online riceve anche un messaggio in chat. Ogni bonus finisce anche nel fascicolo del dipendente. Non si può dare un bonus a sé stessi, e il dipendente deve avere già un conto bancario personale. Un singolo bonus non supera 5000 unità della valuta, e si scrive come cifra: niente percentuali né "tutto". Il denaro deve essere sul conto aziendale.

**Fascicolo.** In fondo alla scheda, le ultime tre annotazioni su quel dipendente: i bonus ricevuti, con motivo e data, e i titoli assegnati o tolti. Lo vede chi apre la scheda; non cambia nulla per il dipendente.

:::caution[Una sola valuta]
Il menu lavora su una sola valuta, la prima fra quelle definite sul server, anche se la valuta predefinita è un'altra. Con una sola valuta definita non cambia nulla.
:::

Versare e prelevare dal conto aziendale non si fa più da qui: si usa la [banca](/Quorum/docs/modules/bank/).

## Problemi comuni

| Cosa vedi | Causa |
| --- | --- |
| `Non puoi aprire un menu boss.` | Manca `boss.menu`. In un'azienda di un lavoro, controlla di essere in servizio. |
| `Azienda sconosciuta.` | L'`id` è sbagliato, oppure non hai `boss.menu` su quell'azienda. Il messaggio è lo stesso nei due casi. |
| `Questa azienda non è più disponibile.` | L'organizzazione è stata archiviata. |
| `Il personaggio è cambiato. Riapri il menu.` | Hai cambiato personaggio con il menu aperto. |
| `Sola lettura.` | Hai il permesso ma non un rank o un grado nell'azienda. |
| `Non hai ancora un conto bancario personale.` | Il dipendente a cui dai il bonus non ha mai aperto un conto: gli serve almeno un uso dell'ATM. |
