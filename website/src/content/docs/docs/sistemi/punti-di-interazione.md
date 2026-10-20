---
title: Punti di interazione
description: Come un blocco del mondo diventa un punto d'uso riservato a un lavoro o a un'organizzazione.
---

Un punto di interazione è un blocco già presente nel mondo a cui lo staff assegna una funzione. Chi ha diritto di usarlo fa clic destro sul blocco e la funzione parte; per tutti gli altri il clic viene annullato.

Per la procedura passo per passo vedi [Aggiungere un punto di interazione](/Quorum/docs/guide/aggiungere-un-punto/).

## Com'è fatto un punto

Ogni punto ha un numero, che QRM assegna alla creazione, e queste proprietà:

| Proprietà | Significato |
| --- | --- |
| Tipo | La funzione del punto. |
| Proprietario | Un lavoro oppure un'organizzazione. |
| Grado minimo | Facoltativo. Il grado, o il rank, da cui in su il punto è utilizzabile. |
| Nome | Facoltativo. Fino a 64 caratteri. |
| Faccia dell'icona | Su quale lato del blocco compare l'icona. |
| Visibile a tutti | Se l'icona si vede anche da chi non può usare il punto. |

Un blocco ospita un solo punto.

## Tipi

QRM include un tipo, `duty`: il clic fa entrare in servizio il personaggio, o lo fa uscire. Accetta come proprietario solo un lavoro, e funziona anche quando `selfDuty` è `false`, cioè quando i giocatori non possono usare `/duty`.

Gli altri tipi li aggiungono i Modules. Il completamento con Tab di `/qrm admin point create` elenca quelli disponibili sul server.

## Chi può usare un punto

Dipende dal proprietario.

**Se il proprietario è un lavoro**, il personaggio deve avere quel lavoro e, se è impostato un grado minimo, un grado di livello pari o superiore. Alcuni tipi richiedono anche di essere in servizio; `duty` no, altrimenti non si potrebbe entrare.

**Se il proprietario è un'organizzazione**, il personaggio deve esserne membro con un rank di livello pari o superiore al minimo. Contano solo i membri aggiunti con `/org member set`: chi appartiene all'organizzazione per il lavoro non basta.

Chi fa clic senza averne diritto legge `Non puoi usare questo punto.` Chi non ha un personaggio attivo legge `Nessun personaggio attivo.`

## L'icona

I giocatori con QRM sul client vedono un'icona sul blocco, fino a 32 blocchi di distanza. Senza QRM sul client il punto funziona lo stesso, ma non si vede.

- L'icona compare sulla faccia che lo staff stava guardando quando ha creato il punto. Se guardava il blocco da sopra o da sotto, compare su tutte e quattro le facce laterali.
- Di norma la vede solo chi può usare il punto. Con `set public true` la vedono tutti.

## Protezione del blocco

Un blocco che ospita un punto non si può rompere. Chi ci prova legge il numero del punto e l'indicazione di rimuoverlo prima. Mentre il punto è attivo, il clic destro non raggiunge il blocco: una cassa marcata come punto non si apre.

## Punti inattivi

Un punto diventa inattivo quando viene a mancare qualcosa da cui dipende. Un punto inattivo non reagisce ai clic e non mostra l'icona: il clic destro torna a raggiungere il blocco. Il blocco resta però protetto dalla rottura finché il punto non viene rimosso. Il punto torna attivo da solo quando la causa sparisce.

`/qrm admin point list` e `info` mostrano lo stato e la causa:

| Stato | Causa |
| --- | --- |
| `tipo non registrato` | Il Module che forniva il tipo non è più installato. |
| `proprietario inesistente` | Il lavoro non è più definito, oppure l'organizzazione è archiviata o eliminata. |
| `grado inesistente` | Il grado minimo non esiste più nel proprietario. |
| `dimensione inesistente` | La dimensione in cui si trova il punto non esiste sul server. |
| `blocco mancante` | Al posto del blocco c'è aria. |

## Comandi

Tutti i comandi sono riservati agli operatori e vengono scritti nel registro delle azioni dello staff. Quelli senza un numero agiscono sul blocco che stai guardando, entro 6 blocchi.

| Comando | Effetto |
| --- | --- |
| `/qrm admin point create <tipo> job <lavoro> [grado]` | Crea un punto di un lavoro sul blocco guardato. |
| `/qrm admin point create <tipo> org <organizzazione> [rank]` | Crea un punto di un'organizzazione. |
| `/qrm admin point set grade <grado>` | Imposta il grado minimo. `none` lo toglie. |
| `/qrm admin point set face <faccia>` | Sposta l'icona: `all`, `here`, `north`, `south`, `east`, `west`. |
| `/qrm admin point set public <true\|false>` | Rende l'icona visibile a tutti, o solo a chi può usare il punto. |
| `/qrm admin point set name <nome>` | Dà un nome al punto. `none` lo toglie. |
| `/qrm admin point move <numero>` | Sposta il punto sul blocco guardato. |
| `/qrm admin point remove [numero]` | Rimuove il punto guardato, o quello con il numero indicato. |
| `/qrm admin point info` | Mostra tutte le proprietà del punto guardato. |
| `/qrm admin point list` | Elenca tutti i punti con posizione e stato. |
| `/qrm admin point list job <lavoro>` | Elenca i punti di un lavoro. |
| `/qrm admin point list org <organizzazione>` | Elenca i punti di un'organizzazione. |

`here` indica la faccia che stai guardando, e richiede di guardare un lato del blocco, non sopra né sotto.
