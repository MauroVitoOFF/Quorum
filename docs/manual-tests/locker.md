# Lista di controllo manuale — qrm-locker (v0.15.0)

Prerequisiti: server dev (`./gradlew prepareServerRun prepareClientRun`, poi `runServer`/`runClient` di `qrm-locker`; mai `clean`). Un job `police` con almeno due gradi, un job `medic`. Giocatore A con job `police` e due personaggi, giocatore B senza job.

1. [ ] `/qrm admin point create locker job police` inquadrando una faccia di una cassa: punto creato. `create locker org x` funziona (il tipo accetta job e organization).
2. [ ] A fa click destro: la cassa vanilla NON si apre, si apre un baule da 3 righe. Il titolo è il nome del punto, o "Armadietto"/"Locker" se non ha nome.
3. [ ] B (non autorizzato) fa click destro: non si apre nulla, messaggio "Non puoi usare questo punto.".
4. [ ] Deposita oggetti con nome, incantesimi e componenti (es. spada incantata rinominata, shulker box con contenuto). Chiudi e riapri: tutto identico.
5. [ ] A cambia personaggio (`/character`): l'armadietto è vuoto. Torna al primo: ritrova gli oggetti.
6. [ ] Un secondo punto `locker job police` in un altro posto apre lo STESSO armadietto. Un punto `locker job medic` (A con quel job) ne apre uno diverso.
7. [ ] `point move` e poi riapri: stesso contenuto. `point remove` e ricrea: stesso contenuto (non si cancella mai da solo).
8. [ ] Riavvia il server: contenuto conservato.
9. [ ] Con l'armadietto aperto: `point remove` dallo staff, oppure cambio grado/job di A, oppure cambio personaggio, oppure allontanarsi oltre 8 blocchi → il menu si chiude da solo e il contenuto resta salvato.
10. [ ] Un giocatore non apre due armadietti insieme (secondo click senza chiudere: nessun effetto).
11. [ ] Il giocatore si disconnette con l'armadietto aperto: al rientro il contenuto c'è.
12. [ ] Kill del processo del server mentre si sposta un oggetto (poi riavvio): nessun oggetto duplicato; si perde al più l'ultimo tick.
13. [ ] Client CON il mod: schermata a tema QRM (pannello, slot scuri), slot e shift-click come vanilla. Client SENZA il mod: entra nel server e vede un baule vanilla.
14. [ ] Un baule vanilla aperto normalmente, subito dopo, resta vanilla (il tema vale una sola apertura).
15. [ ] Documento corrotto (modifica a mano la riga in `qrm_mod_data`): l'armadietto non si apre, messaggio che invita a contattare lo staff, riga nel log, il documento non viene sovrascritto.
16. [ ] Rimuovi il mod di un oggetto presente nell'armadietto: l'armadietto si apre senza quell'oggetto; reinstallalo: l'oggetto riappare (se lo slot è libero).
