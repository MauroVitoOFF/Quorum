# qrm-vehicles — Sistema veicoli (design)

Data: 2026-11-04 · Stato: bozza in attesa di approvazione

## Intenzione

Portare in QRM veicoli guidabili che si sentano veri senza allontanarsi da Minecraft, e che il giocatore debba **prendersi cura** del proprio mezzo. Il veicolo non è un oggetto usa e getta: si consuma, va mantenuto, e quando viene trascurato lo si capisce dal comportamento (il motore fatica a partire, esce fumo), non da un messaggio a schermo. Il mestiere del meccanico diventa così necessario nel roleplay del server.

Primo modello: la Jeep Wrangler 2 porte già disegnata in `design/jeep-wrangler/` (vedi la memoria di progetto `jeep-wrangler-model` per rig, ossa e locator).

## Obiettivi

- Guida **morbida ma controllabile**: accelerazione graduale, inerzia in frenata e curva, leggera tendenza a scivolare, rollio e beccheggio del corpo. Sale i mezzi blocchi e le scale a bassa velocità, rallenta su sabbia/fango. Niente fisica da simulatore: velocità massima, grip e simili sono **parametri configurabili** da regolare provando in gioco.
- Un veicolo ha uno **stato che si consuma guidando**, non col tempo di calendario (un veicolo parcheggiato non si rovina).
- Due livelli di cura: il proprietario fa le piccole cose, il meccanico fa quelle serie e vede lo stato reale.
- Contachilometri per veicolo, non azzerabile.

## Non-obiettivi (per questa fase)

Danni da incidente e carrozzeria, scasso e furto, sirene e veicoli di servizio, personalizzazioni, altri modelli oltre alla Jeep, fisica realistica, cambio manuale.

## Anti-obiettivi (sarebbe un fallimento anche se funziona)

- Manutenzione invadente: l'utente ha chiesto un ritmo "non spesso". Un giocatore normale non deve sentirsi obbligato a occuparsene di continuo.
- Un'interfaccia che spiega i guasti al giocatore normale: il segnale è il suono/fumo, la causa la vede solo il meccanico.
- Blocchi meccanici basati sul ruolo: vedi sotto, l'accesso passa dagli oggetti.

## Cosa contiene la prima versione

Guida, carburante, chiavi e targa legate a un Character, più sedili e bagagliaio, fari e clacson, accensione/spegnimento, contachilometri, e il sistema di cura descritto sotto.

## La cura del veicolo (decisioni dell'utente)

**Componenti dello stato:** olio, motore, stato generale da revisionare, sporcizia, più i km totali.

**Ritmi iniziali** (proposta, configurabile; l'utente ha detto "va bene"):
- Olio: cambio dopo circa 10 ore di guida; ignorarlo accelera l'usura del motore.
- Revisione: circa ogni 2-3 settimane reali di uso normale; ignorarla alza la probabilità di guasti all'accensione, non ferma il veicolo subito.
- Pulizia: soprattutto estetica, con un piccolo effetto sul valore; si sporca di più su sterrato e fango.
- Le soglie si contano in **km (o ore di guida) dall'ultimo intervento**.

**Accensione di un veicolo trascurato:**
- Si sente l'avviamento che gira. A volte il motore prende.
- A volte non parte. Il giocatore può spegnere e chiamare un meccanico, oppure ritentare.
- Ritentare è una scommessa: a volte parte, a volte esce fumo e il danno aumenta. La probabilità dipende dalle condizioni del motore e dai tentativi consecutivi (scelta di design del builder: non è un tiro fisso).

**Livelli di gravità:** manutenzione ordinaria (poco costosa), guasto (serve il meccanico), guasto aggravato (serve il meccanico, ricambi e denaro).

**Proprietario:** lavaggio, controlli base e piccole azioni (es. rabbocco/cambio olio semplice; il builder decide quali esattamente, nel rispetto di "piccole cose").

**Meccanico:** appartiene a una fazione (organizzazione in `qrm-core/org`), che ha **oggetti esclusivi** per ispezionare i veicoli. L'ispezione mostra lo stato reale dei componenti e la **causa** di un mancato avvio. Gli interventi seri richiedono quegli strumenti.

**Accesso:** deciso dagli **oggetti posseduti**, non da un controllo di servizio/ruolo nel codice (coerente con la scelta già presa per le armi). La fazione resta il modo in cui quegli oggetti arrivano ai giocatori nel roleplay.

**Economia:** i guasti gravi costano ricambi e denaro. I ricambi sono oggetti, acquistabili da chiunque nei negozi e disponibili anche dai meccanici. Il denaro usa l'economia di `qrm-core` (`Money`, conto delle organizzazioni).

## Divisione in fasi (ordine concordato)

1. **Veicolo guidabile:** entità, modello Jeep, guida, passeggeri, carburante, contachilometri.
2. **Proprietà:** chiavi, targa, legame con il Character.
3. **Stato e manutenzione:** usura, azioni del proprietario, pulizia, comportamento all'accensione.
4. **Ispezione e riparazioni del meccanico:** oggetti esclusivi, ricambi, costi.

Ogni fase ha il suo piano di implementazione e può essere consegnata da sola.

## Vincoli tecnici

- NeoForge 26.2.0.88, Minecraft 26.2, Java 25.
- Nuovo modulo `qrm-vehicles`, con la stessa struttura di `qrm-police` e `qrm-locker`: `logic` (Java puro, testabile con JUnit), `mc`, `client`, `net`. Dipende solo da `qrm-api` e `qrm-client-api` a compile time.
- Nel progetto **non esiste ancora nessuna entità personalizzata**: il veicolo è la prima.
- Il modello è in formato GeckoLib/Bedrock geo, ma GeckoLib non è una dipendenza. **Scelta del design (non dell'utente):** riusare il sistema `BonePose` già in `qrm-police` invece di aggiungere GeckoLib. Va verificato che sia riusabile (oggi sta in `qrm-police/weapon/logic/mesh`); se serve, estrarlo in un posto condiviso.
- Il modello va letto nel suo stato attuale: l'utente lo ritocca a mano in Blockbench. Locator utili: `driver_eye`, `driver_hip`, `passenger_hip`, `exhaust_tip`, `engine_sound`. Il vetro è translucido. Fango e livree = texture sovrapposta, non un nuovo modello. Porte e pivot sinistri sono da ri-verificare in gioco.
- Convenzioni del repo: modulo nuovo nella wiki (`website`, la build ha `check-docs`), test manuali in `docs/manual-tests/`, aggiornare `qrm_version` e `API_VERSION`.

## Cosa lascia al builder

- Architettura dell'entità (movimento, sincronizzazione server/client, predizione sul client di chi guida).
- Valori di tuning della guida e delle soglie di usura.
- Formula esatta della probabilità di avvio/peggioramento.
- Quali azioni "piccole" spettano al proprietario.
- Suoni (avviamento, motore, fumo): se mancano gli asset, segnaposto da sostituire.
- Come persiste lo stato del veicolo (seguire il pattern di `qrm-locker`).

## Come si ottiene un veicolo (decisione dell'utente)

Un giocatore ottiene il suo veicolo tramite un **concessionario**. Il resto è una mia ipotesi da confermare: il concessionario è una fazione come i meccanici, con un oggetto o procedura esclusiva per consegnare un veicolo a un Character; il pagamento passa dall'economia di `qrm-core` verso il conto dell'organizzazione. Il builder può partire da qui, ma i dettagli (prezzi, catalogo, test drive) si decidono alla fase 2. I comandi di staff restano per test e correzioni.

## Scelte del redattore (non dell'utente, da confermare leggendo)

- **Carburante:** nella fase 1 si rifornisce con taniche (oggetti). I distributori sono rimandati.
- **Come nasce un veicolo nelle fasi 1-2:** con un comando di staff (`/qrm vehicle ...`), sul modello degli altri moduli, finché il concessionario non è pronto.
- **Un veicolo per volta alla guida, un proprietario per veicolo;** un Character può possederne più di uno.
- **Persistenza:** il veicolo non sparisce quando il chunk si scarica; stato e km sopravvivono ai riavvii.

## Domande aperte (da risolvere prima o durante la fase interessata)

- **Dettagli del concessionario** (fase 2): vedi la sezione seguente.
- **Negozi:** nel codice non risulta un sistema di negozi. Va verificato se esiste in un altro modulo o se serve crearlo; riguarda la fase 4.
- **Chi incassa** il pagamento di una riparazione: la fazione (conto organizzazione) o il singolo meccanico. Da decidere in base a `DefaultOrganizationBankService`; fase 4.
