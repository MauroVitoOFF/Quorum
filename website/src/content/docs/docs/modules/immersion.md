---
title: Immersion
description: Nomi nascosti finché non ci si presenta, mani alzate e braccio che indica, bussare alle porte e sedersi su scale e lastre.
---

Immersion toglie le comodità di Minecraft che rompono la scena e aggiunge qualche gesto. Sopra la testa delle persone non c'è più il nome del giocatore: finché non ci si presenta si è sconosciuti. Si possono alzare le mani, indicare, bussare a una porta e sedersi.

**Disponibilità**: incluso nel progetto, **Sperimentale**. Va installato sul server e sui client: aggiunge un'entità e due tasti, quindi un client senza il mod non entra. [Medical](/Quorum/docs/modules/medical/) non è richiesto, ma se c'è le due cose si parlano.

## Installare

Copia `qrm-immersion-<versione>.jar` in `mods/`, sul server e sui client. Non ha configurazione né permessi: vale per tutti i giocatori.

## Nomi e presentazioni

Il nome del giocatore non si vede mai. Al suo posto, sopra una persona compare un'etichetta solo se sono vere tutte e tre le condizioni:

- è a 5 blocchi o meno;
- la si guarda quasi dritto in faccia;
- niente si mette in mezzo fra gli occhi e la persona.

L'etichetta dice **Sconosciuto #1234**, in grigio, finché non ci si è presentati. Le quattro cifre derivano dall'identità del giocatore e non cambiano: servono a riconoscere la stessa persona prima di saperne il nome. Dopo la presentazione compare, in bianco, il nome e cognome del suo [personaggio](/Quorum/docs/sistemi/personaggi/).

Chi porta qualsiasi oggetto sulla testa torna sconosciuto per tutti, anche per chi lo conosce: non solo caschi e maschere, ma anche una zucca o un blocco.

### Presentarsi

1. Avvicinati a una persona, a 3 blocchi al massimo, con la mano principale vuota.
2. Fai Maiusc + clic destro su di lei. L'altra persona riceve un messaggio.
3. Entro 30 secondi lei fa lo stesso gesto su di te. Da quel momento vi conoscete tutti e due.

Con Medical installato, non ci si presenta a chi è a terra o incosciente, né mentre uno dei due ha le mani alzate.

Le conoscenze appartengono al personaggio di chi guarda: cambiando personaggio si ricomincia da zero, e tornando al primo le si ritrova. Sono salvate nel database di QRM.

## Linguaggio del corpo

| Tasto | Cosa fa |
| --- | --- |
| `X` | Alza o abbassa le mani. Con le mani alzate non si attacca, non si usano oggetti, non si rompono né si piazzano blocchi. |
| `B`, tenuto premuto | Indica con il braccio destro nella direzione in cui si guarda. Non funziona con le mani alzate. |

Le pose le vedono tutti. Si perdono quando il giocatore va a terra, muore o passa in modalità spettatore. In prima persona le mani alzate non si vedono: le braccia escono dall'inquadratura.

I tasti si cambiano dalle opzioni dei comandi, nella categoria **QRM Immersion**.

## Bussare

Clic sinistro a mano vuota su una porta: chi è vicino sente bussare, fino a 10 blocchi. Con Maiusc la bussata è forte e si sente fino a 20 blocchi. Fra due bussate passa almeno mezzo secondo.

In modalità creativa il clic sinistro rompe la porta: lì si bussa solo con Maiusc.

## Sedersi

Clic destro a mano vuota, senza Maiusc, sulla faccia superiore di una scala o di una lastra posata in basso, a 2,5 blocchi al massimo. Sopra deve esserci spazio libero, e la seduta non deve essere già occupata. Per alzarsi si preme Maiusc.

## Il manichino di prova

Per provare nomi e pose da soli, gli operatori possono creare un manichino con l'aspetto di un giocatore. Si presenta subito, senza aspettare la conferma.

| Comando | Effetto |
| --- | --- |
| `/qrm dummy spawn [nome]` | Crea un manichino davanti a te. Senza nome si chiama "Mario Rossi". |
| `/qrm dummy pose hands_up` | Il manichino più vicino, entro 16 blocchi, alza le mani. |
| `/qrm dummy pose point` | Il manichino più vicino indica. |
| `/qrm dummy pose none` | Il manichino più vicino torna in posizione normale. |
| `/qrm dummy remove` | Toglie il manichino più vicino, entro 16 blocchi. |
| `/qrm dummy clear [raggio]` | Toglie tutti i manichini entro il raggio: 16 blocchi se non lo indichi, 64 al massimo. |

I comandi agiscono solo sui manichini creati con `spawn`, riconoscibili dall'etichetta `qrm_dummy`. Quelli creati prima della versione 0.28 non ce l'hanno: si tolgono a mano, per esempio con `/kill`.

## Limiti

- Il nome di una persona conosciuta si aggiorna solo quando è online. Se nel frattempo ha cambiato personaggio e non la si incontra, resta l'ultimo nome visto.
- Il manichino perde la posa quando il server si riavvia.
- La seduta è un'entità invisibile. Se il server si ferma mentre qualcuno è seduto, la seduta sparisce al caricamento successivo.
- Altezza della seduta e pose delle braccia sono ancora in prova e possono cambiare.
