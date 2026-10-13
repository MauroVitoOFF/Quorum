---
title: Pannello staff
description: Come si apre e si usa il pannello staff e cosa fa ogni azione sui giocatori.
---

Il pannello staff si apre con **F7**. Lo vede chi ha il permesso `staff.panel`; gli operatori lo hanno sempre. Richiede QRM sul client.

Per concedere l'accesso a chi non è operatore vedi [Dare accesso allo staff](/Quorum/docs/guide/accesso-staff/).

## Usare il pannello

Il pannello compare sopra il gioco senza fermarlo: puoi continuare a muoverti.

| Tasto | Effetto |
| --- | --- |
| **F7** | Apre e chiude il pannello. |
| **↑** **↓** | Scorre le voci. |
| **→** oppure **Invio** | Apre la voce selezionata. |
| **←** oppure **Backspace** | Torna indietro. |
| **Alt sinistro**, tenuto premuto | Libera il cursore: puoi fare clic sulle voci e scorrere con la rotella. |

F7 e Alt sinistro si cambiano dai controlli di Minecraft, nella categoria QRM.

Il pannello è diviso in sezioni e ognuno vede solo le voci per cui ha il permesso. Una sezione senza voci mostra `Niente di disponibile`.

## La scheda di un giocatore

La sezione Giocatori elenca chi è online. Per ognuno la scheda mostra il personaggio attivo, il lavoro, le organizzazioni e lo stato di moderazione: se è silenziato e fino a quando, quanti avvertimenti e quante note ha.

Dalla scheda partono le azioni. Ogni azione ha un proprio nodo, `staff.player.<azione>`, e chi non lo ha non la vede.

| Azione | Cosa fa |
| --- | --- |
| `teleport_to` | Ti porta dal giocatore, anche in un'altra dimensione. |
| `bring` | Porta il giocatore da te. |
| `freeze` | Blocca il giocatore dove si trova, o lo sblocca se era già bloccato. Il blocco finisce quando si disconnette. |
| `heal` | Riporta al massimo salute e fame e spegne il fuoco. |
| `give_item` | Gli dà un oggetto. |
| `set_gamemode` | Gli cambia modalità di gioco. |
| `kick` | Lo espelle, con un motivo facoltativo. |
| `ban` | Lo banna per una durata, o per sempre. Usa la lista dei ban di Minecraft. |
| `mute`, `unmute` | Lo silenzia, o lo riattiva. |
| `warn` | Gli dà un avvertimento. |
| `note` | Scrive una nota che vede solo lo staff. |
| `view_character` | Mostra nome, data di nascita, lavoro e organizzazioni del personaggio attivo. |
| `view_permissions` | Mostra i permessi del personaggio attivo e le assegnazioni individuali. |
| `warnings`, `notes` | Mostra gli avvertimenti e le note del giocatore. |
| `history` | Mostra le azioni dello staff che lo riguardano. Richiede `staff.logs`. |

Mute, avvertimenti, note e registro sono descritti in [Moderazione e registro delle azioni](/Quorum/docs/sistemi/moderazione-e-registro/).

### Come si scrivono i campi

**Oggetto**, per `give_item`: l'identificatore dell'oggetto, seguito se vuoi dalla quantità. `diamond 16` dà sedici diamanti. Senza quantità ne dà uno. Per gli oggetti di un mod si scrive anche lo spazio dei nomi, per esempio `qrm_bank:atm`. La quantità massima è 2304.

**Durata**, per `ban` e `mute`: un numero seguito da `m`, `h`, `d` o `w`, cioè minuti, ore, giorni o settimane. `7d` vale sette giorni. Lasciando il campo vuoto la durata è permanente. Il massimo è dieci anni.

## Regole

- **Il giocatore deve essere online.** Se si è disconnesso l'azione risponde che non è più online.
- **Chi non è operatore non può agire su un operatore.** Fanno eccezione le azioni che si limitano a mostrare qualcosa.
- **Tra un'azione e la successiva deve passare un quarto di secondo.** Altrimenti il pannello risponde `Troppo veloce: riprova tra un attimo.`
- **Il server ricontrolla il permesso a ogni azione.** Nascondere una voce sul client non è l'unica protezione.
- **Ogni azione che cambia qualcosa finisce nel registro**, compresi i tentativi rifiutati. Le azioni che mostrano soltanto non vengono registrate.
