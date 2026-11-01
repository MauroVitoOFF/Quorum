# Promemoria per la wiki

Funzioni già implementate nel codice che **non hanno ancora una pagina** nella wiki (`website/src/content/docs/docs/`).
Regole da ricordare: fatti ricavati dal codice, concetti in italiano, nessuno schema fisso di titoli, link con base `/Quorum/docs/`,
esempi JSON in `docs/samples/`, e `npm run build` in `website/` per far girare `check-docs`.

## 1. Modulo `qrm-police` (0.26.0, nessuna pagina)

Pagina da creare: `modules/police.md`. Fonte: `qrm-police/`, test manuali in `docs/manual-tests/police.md`.

- MDT portatile (tablet) e terminale di centrale (blocco computer) con anagrafe dei cittadini, registro armi e glossario dei reati.
- Dispatch delle chiamate d'emergenza (`CallType`, `DispatchCall`), con grado di pericolo.
- Comandi `/qrm mdt seed`, `/qrm mdt clear`, `/qrm mdt simulatecall` (requisito: `PoliceCommand.allowed`).
  Aggiungerli alla tabella di `riferimento/comandi.md`.
- Nodi di permesso del modulo da aggiungere a `riferimento/nodi-di-permesso.md`.
- Dati dimostrativi: `PoliceSeed`.
- Collegamenti: lavori (agenti in servizio), modulo medico (chiamate di soccorso), divise del modulo wardrobe.

## 2. Modulo `qrm-immersion` (0.27.0, nessuna pagina)

Pagina da creare: `modules/immersion.md`. Fonte: `qrm-immersion/`. Modulo opzionale; il modulo medico è una dipendenza morbida.
Stato da indicare: **Sperimentale** (provato in gioco solo in parte).

### Nomi nascosti e presentazioni
- Il nome vanilla non si vede mai. L'etichetta compare solo entro 5 blocchi, guardando la persona quasi dritto in faccia e con linea di vista libera.
- "Sconosciuto #XXXX" (grigio) finché non ci si è presentati, o se si ha qualsiasi oggetto sulla testa; poi "Nome Cognome" del personaggio (bianco).
  Le 4 cifre derivano dall'UUID, sono stabili.
- Le conoscenze sono per **personaggio**: cambiando personaggio si ricomincia da zero. Sono salvate nei dati dei mod, namespace `qrm_immersion`, chiave `known`.
- Presentarsi: Shift + Click Destro a mani vuote su una persona (a 3 blocchi al massimo). L'altra persona conferma con lo stesso gesto entro 30 secondi.
  Non vale se uno dei due è a terra (c'è la rianimazione del modulo medico) o ha le mani alzate.
- Il manichino di prova accetta subito.

### Linguaggio del corpo
- Tasto `X`: alza/abbassa le mani. Con le mani alzate non si attacca, non si usano oggetti, non si rompe né si piazza. Premessa per perquisizione e ammanettamento.
- Tasto `B` (tenuto premuto): indica col braccio destro, seguendo la testa. Non con le mani alzate.
- Le pose cadono se il giocatore va a terra, muore o diventa spettatore. I tasti sono cambiabili dalle opzioni (categoria "QRM Immersion").
- In prima persona le mani alzate escono dall'inquadratura (le braccia non si disegnano).

### Ambiente
- Bussare: Click Sinistro a mani vuote su una porta. Con Shift è la bussata forte (si sente a 20 blocchi, normale a 10). In creativa solo con Shift. Pausa di mezzo secondo tra due bussate.
- Sedersi: Click Destro a mano vuota, senza Shift, sul lato alto di una scala o lastra bassa, a 2,5 blocchi al massimo, con spazio libero sopra. Si scende con Shift.

### Comandi (operatori)
- `/qrm dummy spawn [nome]`: crea un manichino di prova (default "Mario Rossi"), marcato con l'etichetta `qrm_dummy`.
- `/qrm dummy pose <hands_up|point|none>`: cambia la posa del manichino più vicino entro 16 blocchi.
- `/qrm dummy remove`: toglie il manichino più vicino (entro 16 blocchi). `/qrm dummy clear [raggio]`: li toglie tutti entro il raggio (default 16, massimo 64). Tutti e tre agiscono solo sui manichini creati da `spawn`.
  Aggiungerli a `riferimento/comandi.md` (e verificare che `check-docs` li riconosca).

### Limiti da dichiarare
- Qualsiasi oggetto sulla testa fa tornare "Sconosciuto", non solo elmi e maschere.
- Il nome di chi è conosciuto si aggiorna solo se quella persona è online; altrimenti resta l'ultimo nome visto.
- Il manichino non conserva la posa dopo il riavvio del server.
- Il sedile è un'entità invisibile; se il server si ferma mentre qualcuno è seduto, sparisce al caricamento successivo.

## 3. Da verificare prima di scrivere

- Provare in gioco altezza e orientamento da seduti (`SEAT_LIFT` in `ImmersionRules`), pose delle braccia e il flusso di presentazione tra due giocatori.
- Radice dei comandi: decisa, ora tutto sta sotto `/qrm` (`/quorum`, `/bank` e `/wardrobe` non esistono più). Vedi `docs/comandi.md`.
- Valutare se la versione della pagina `riferimento/versioni.mdx` va aggiornata a 0.28.0.
