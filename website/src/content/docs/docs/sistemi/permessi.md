---
title: Permessi
description: Come un personaggio ottiene un permesso, come si risolvono i conflitti e i comandi per assegnarli.
---

Un permesso è un nodo, cioè un nome come `police.arrest`, che un personaggio ha oppure non ha. I Modules chiedono a QRM se un personaggio ha un certo nodo prima di lasciargli fare qualcosa. I nomi dei nodi li scegli tu nei file dei lavori, oppure li stabilisce il Module che li controlla.

I permessi di questa pagina appartengono al personaggio. L'accesso al pannello staff è un sistema separato, legato all'account del giocatore: vedi [Dare accesso allo staff](/Quorum/docs/guide/accesso-staff/).

## Come si scrive un nodo

Un nodo è fatto di parti separate da punti. Ogni parte contiene solo lettere minuscole, cifre e `_`. La lunghezza massima è 128 caratteri.

Dove si concede un permesso si può usare un jolly:

| Scrittura | Cosa copre |
| --- | --- |
| `police.arrest` | Solo quel nodo. |
| `police.*` | Ogni nodo che inizia con `police.` |
| `*` | Ogni nodo. |

## Da dove arriva un permesso

Un personaggio ottiene permessi da due fonti.

**Il grado del lavoro.** Valgono solo mentre il personaggio è [in servizio](/Quorum/docs/sistemi/lavori/), e comprendono i permessi dei gradi inferiori.

**Un'assegnazione individuale.** Un operatore concede o nega un nodo a un singolo personaggio. Vale sempre, in servizio e fuori.

```text
/perm grant <giocatore> <nodo>
/perm deny <giocatore> <nodo>
/perm clear <giocatore> <nodo>
```

`grant` concede, `deny` nega, `clear` toglie l'assegnazione individuale e lascia decidere al lavoro. I comandi agiscono sul personaggio attivo del giocatore, che deve essere online.

## Come si decide

Quando un Module chiede se un personaggio ha un nodo, QRM procede così:

1. Cerca fra le assegnazioni individuali quelle che coprono il nodo. Se ce ne sono, vince la più specifica: un nodo esatto batte `police.*`, che batte `*`. A parità di specificità vince `deny`.
2. Se nessuna assegnazione individuale copre il nodo, guarda il grado: il personaggio ha il nodo se è in servizio e il suo grado lo copre.
3. Altrimenti non lo ha.

Un esempio. Un agente ha dal grado `police.*`, e un operatore gli ha negato `police.arrest`:

| Nodo richiesto | In servizio | Fuori servizio |
| --- | --- | --- |
| `police.search` | Sì, dal grado | No |
| `police.arrest` | No, è negato | No |

Un personaggio archiviato non ha nessun permesso.

## Vedere i permessi di un personaggio

`/job` mostra al giocatore, nella riga `Permessi`, i nodi che il suo personaggio attivo ha in quel momento.

## Comandi

`/perm` e `/qrm perm` sono lo stesso comando, riservato agli operatori. Ogni uso viene scritto nel registro delle azioni dello staff.

| Comando | Effetto |
| --- | --- |
| `/perm grant <giocatore> <nodo>` | Concede il nodo al personaggio attivo del giocatore. |
| `/perm deny <giocatore> <nodo>` | Nega il nodo. |
| `/perm clear <giocatore> <nodo>` | Toglie l'assegnazione individuale di quel nodo. |

`clear` va usato con la stessa scrittura dell'assegnazione: per togliere un `grant` su `police.*` serve `/perm clear <giocatore> police.*`.
