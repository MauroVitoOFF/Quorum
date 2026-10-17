---
title: Medical
description: Atterramento, coma e ricovero al posto della morte. RCP, defibrillatore, kit medico, trasporto e comandi.
---

Medical toglie la morte di Minecraft dalle scene di gioco di ruolo. Quando un giocatore arriva a zero vita non muore, non perde l'inventario e non torna al letto: cade a terra, resiste qualche minuto, e se nessuno lo aiuta viene ricoverato in ospedale. Nel frattempo cittadini, polizia e medici hanno cose da fare.

**Disponibilità**: incluso nel progetto. Va installato sul server e sui client: aggiunge degli oggetti, quindi un client senza il mod non entra.

## Installare

1. Copia `qrm-medical-<versione>.jar` in `mods/`, sul server e sui client.
2. Entra nel gioco, mettiti nel punto in cui vuoi l'ospedale e scrivi `/qrm medical sethospital`.

Senza un ospedale impostato, la morte clinica cura il giocatore ma non lo sposta.

## Gli stati

| Stato | Quando | Cosa può fare |
| --- | --- | --- |
| A terra | Riceve un colpo che lo ucciderebbe. La vita va a mezzo cuore. | Striscia molto lentamente. Non salta, non attacca, non rompe né piazza blocchi, non usa oggetti, non apre l'inventario. |
| Incosciente | Gli restano meno di 60 secondi, oppure lo colpisce direttamente un'altra creatura mentre è a terra. | Niente: è immobile e vede quasi tutto nero. |
| Morte clinica | Il tempo finisce, oppure si arrende al coma dopo almeno 120 secondi a terra. | Viene ricoverato: vita al 50%, effetti tolti, spostato in ospedale con debolezza e lentezza per 60 secondi. L'inventario non cambia. |

Il tempo di partenza è di 3 minuti. Danni ambientali e colpi di chi non è una creatura non lo toccano mentre è a terra; `/kill` e il vuoto sì. Lo stato è legato alil **personaggio**, non al giocatore: cambiare personaggio non porta con sé le ferite.

## Cosa può fare chi è a terra

- **`C`** manda a tutti i soccorritori in linea un messaggio con le sue coordinate. Si può ripetere dopo 30 secondi.
- **`G`**, tenuto premuto per 5 secondi, lo fa arrendere al coma e lo manda in ospedale. Funziona solo dopo 120 secondi a terra.

Lo schermo si copre di rosso ai bordi, sempre più forte mentre il tempo scende, e si sente il battito, sempre più veloce. Da incosciente lo schermo è nero al 95%.

## Soccorrere

| Come | Cosa fa |
| --- | --- |
| Clic destro a mani vuote | Ispezione: coscienza, polso e ferite visibili con tipo, gravità e quanto tempo fa. |
| Maiusc + clic destro a mani vuote, tenuto premuto | RCP: il tempo si ferma all'80%, escono cuori e si sente un battito. Basta restare accovacciati vicino al ferito. |
| `/carry` | Prende il ferito più vicino e lo trascina dietro di sé, andando più piano. Di nuovo `/carry` per lasciarlo. |
| Defibrillatore (clic destro sul ferito) | Dopo 4 secondi lo rialza con 3 cuori. Bisogna restare vicini e tenere l'oggetto in mano. |
| Kit medico | Cura un giocatore in piedi al massimo e toglie debolezza e lentezza. Su un ferito a terra azzera il timer ma non lo rialza. Anche su se stessi. |
| Benda | Cura 4 cuori e toglie debolezza e lentezza. Non ferma un ferito a terra. |

Gli oggetti sono nella scheda degli strumenti dell'inventario creativo. Defibrillatore e kit non sono limitati a un lavoro: li decide il server dando o non dando gli oggetti.

## Permessi

Medical usa due nodi di permesso, da dare ai lavori che servono (nel file del lavoro o con i comandi dei permessi):

| Nodo | Cosa fa |
| --- | --- |
| `medical.alert.receive` | Riceve l'allerta di chi preme `C`, con le sue coordinate. |
| `medical.treat` | Nell'ispezione vede anche il tempo che resta al ferito. |

L'ispezione, la RCP e le cure non richiedono altro: possono farle tutti.

## Registro delle ferite

Ogni colpo importante e quello che fa cadere a terra restano registrati sul personaggio, fino a otto, con tipo (caduta, corpo contundente, lama, proiettile, ustione, esplosione, fame, annegamento, altro), gravità e momento. Servono ai medici per la diagnosi e alla scientifica per capire cosa è successo. Vengono azzerate quando il giocatore si riprende.

## Comandi

Per gli operatori:

```text
/qrm medical down <giocatore>
/qrm medical revive <giocatore>
/qrm medical inspect <giocatore>
/qrm medical sethospital
```

Per tutti: `/carry`.

## Limiti

- Una morte che non passa dal danno normale, per esempio causata da un altro mod, fa cadere a terra lo stesso.
- Gli oggetti illegali non vengono tolti al ricovero: l'inventario resta intatto.
- Un ferito che esce dal gioco ritrova il suo stato, con il tempo rimasto, quando rientra con lo stesso personaggio.
