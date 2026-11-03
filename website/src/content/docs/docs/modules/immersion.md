---
title: Immersion
description: Nomi nascosti finché non ci si presenta, mani alzate e braccio che indica, bussare alle porte.
---

Immersion toglie le comodità di Minecraft che rompono la scena e aggiunge qualche gesto. Sopra la testa delle persone non c'è più nessun nome: il nome di chi si conosce affiora come un pensiero, piccolo, sotto il mirino. Finché non ci si presenta si è sconosciuti, e di uno sconosciuto non compare niente. Si possono alzare le mani, indicare, bussare a una porta.

**Disponibilità**: incluso nel progetto, **Sperimentale**. Va installato sul server e sui client: aggiunge due tasti, quindi un client senza il mod non entra. [Medical](/Quorum/docs/modules/medical/) non è richiesto, ma se c'è le due cose si parlano.

## Installare

Copia `qrm-immersion-<versione>.jar` in `mods/`, sul server e sui client. Non ha permessi: vale per tutti i giocatori. Le distanze e i tempi si regolano in `qrm_immersion-server.toml`, vedi [Configurazione](#configurazione).

## Nomi e presentazioni

Il nome del giocatore non si vede mai e sopra la testa non c'è niente. Sotto il mirino compare il nome solo se sono vere tutte e tre le condizioni:

- è a 5 blocchi o meno;
- la si guarda, con il mirino sul corpo o appena accanto (se ce ne sono più d'una, vale la più vicina);
- niente si mette in mezzo fra gli occhi e la persona.

Il nome non appare subito: serve uno sguardo che si ferma per mezzo secondo circa, poi sale piano. È scritto a metà della dimensione normale, in corsivo e in un grigio chiaro e trasparente, e svanisce piano quando si guarda altrove. Di chi non si è presentato non compare nulla, nemmeno la scritta «Sconosciuto». Dopo la presentazione si vede il nome e cognome del suo [personaggio](/Quorum/docs/sistemi/personaggi/). Se il giocatore passa a un altro personaggio, chi lo conosceva non lo riconosce più.

Chi porta qualsiasi oggetto sulla testa torna sconosciuto per tutti, anche per chi lo conosce: non solo caschi e maschere, ma anche una zucca o un blocco.

### Presentarsi

1. Avvicinati a una persona, a 3 blocchi al massimo, con la mano principale vuota.
2. Fai Maiusc + clic destro su di lei. L'altra persona riceve un messaggio.
3. Entro 30 secondi lei fa lo stesso gesto su di te. Da quel momento vi conoscete tutti e due.

Con Medical installato, non ci si presenta a chi è a terra o incosciente, né mentre uno dei due ha le mani alzate.

Le conoscenze appartengono al personaggio di chi guarda: cambiando personaggio si ricomincia da zero, e tornando al primo le si ritrova. Sono salvate nel database di QRM.

## Chat

La chat normale è **locale**: chi scrive viene sentito solo da chi sta nella stessa dimensione a 15 blocchi o meno. Ognuno legge il messaggio con il nome che conosce di chi parla, oppure «Sconosciuto». Chi scrive vede il proprio nome da personaggio. Un giocatore silenziato non scrive nemmeno qui.

Per parlare fuori dal ruolo c'è `/ooc <messaggio>`: lo leggono tutti, con il nome utente, e un silenziato non può usarlo. Nel registro del server ogni messaggio locale resta con il nome utente e il nome del personaggio, per chi modera.

La lista dei giocatori che si apre con il tasto Tab non si vede più, perché elencherebbe i nomi utente di tutti.

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

## Configurazione

Le distanze e i tempi di questa pagina (5 blocchi per i nomi, 15 per la chat, 3 per la mano, 30 secondi per una mano tesa, 10 e 20 blocchi per le bussate) si cambiano in `qrm_immersion-server.toml`. L'elenco delle chiavi è in [Configurazione e file](/Quorum/docs/riferimento/configurazione/). Con `chat.localEnabled = false` la chat torna quella normale di Minecraft.

## Limiti

- Restano visibili i nomi utente nei messaggi di ingresso e uscita dal server, in quelli di morte e nei comandi di Minecraft come `/me` e `/msg`. I messaggi di morte si spengono con la regola di gioco che li governa.
- Se un personaggio cambia nome, chi lo conosce vede il nuovo nome solo quando è online. Le conoscenze salvate prima della versione 0.34 sono state azzerate, perché erano legate al giocatore.
- Il manichino perde la posa quando il server si riavvia.
- Le pose delle braccia sono ancora in prova e possono cambiare.
