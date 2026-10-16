# Lista di controllo manuale — banca (v0.7.3)

Prerequisiti: server dev (`./gradlew :qrm-bank:prepareServerRun :qrm-bank:prepareClientRun`, poi `runServer`/`runClient`; mai `clean`), una valuta `QRM` con 2 decimali registrata in QRM, un giocatore con personaggio attivo e fondi (`/qrm admin ...`).

1. [ ] Il server parte con `config/qrm_bank/cash.json` creato; con un file invalido si ferma con un messaggio che nomina il file.
2. [ ] `/bank atm give` consegna l'ATM; il blocco si piazza e si rompe (con un piccone); in creativa compare in "Blocchi funzionali".
3. [ ] Click destro senza personaggio attivo: messaggio, nessuna schermata.
4. [ ] Click destro con personaggio: schermata con saldo, storico, contanti con te.
5. [ ] Ritiro di 17.00 QRM: arrivano 1×10, 1×5, 2×1 (nomi "Banconota 10.00 QRM", ...), saldo scalato, storico aggiornato.
6. [ ] Ritiro di 1.50 (non multiplo del taglio minimo): rifiutato, nessun addebito.
7. [ ] Ritiro con inventario pieno: rifiutato PRIMA dell'addebito.
8. [ ] Ritiro oltre il limite per operazione e oltre il limite giornaliero: rifiutati; dopo 24 ore il limite si libera.
9. [ ] Deposito: tutto il contante della valuta scompare e il saldo risale.
10. [ ] Deposito di banconote date da un admin (non emesse dalla banca): rifiutato ("non emesso dalla banca"), item intatti.
11. [ ] Bonifico a "Nome Cognome" (maiuscole diverse): ok per entrambi; a un nome inesistente / ambiguo / a se stessi: errore chiaro.
12. [ ] Bonifico a `@organizzazione`: il saldo dell'organizzazione aumenta.
13. [ ] Allontanarsi oltre `maxDistance` con la schermata aperta e premere un'azione: schermata chiusa, operazione non eseguita.
14. [ ] Doppio click veloce su "Ritira": una sola operazione (rate limit).
15. [ ] Chiudere il gioco con forza durante un ritiro, poi riaprire: nessuna banconota duplicata (`/bank audit` coerente).
16. [ ] `/perm deny <pg> bank.atm.use`: l'ATM non si apre per quel personaggio; `bank.transfer` negato: il bonifico è rifiutato.
17. [ ] `/bank freeze <pg>`: ritiro e bonifico rifiutati ("conto congelato"); `/bank unfreeze` li ripristina.
18. [ ] Un altro mod annulla `TransactionPreEvent` per `bank.withdraw`: esito "operazione annullata", nessun contante.
19. [ ] `/bank audit` senza anomalie; dopo aver dato banconote con `/give` oltre la cassa segnala l'eccesso.
20. [ ] Server dedicato: parte senza caricare classi client (`dev.qrm.bank.client`).
21. [ ] Un resource pack che ridefinisce `qrm_bank.positive`/`qrm_bank.negative` cambia i colori della schermata.
22. [ ] Client con `cash.json` diverso dal server: connessione respinta con un errore di registro (comportamento documentato).
23. [ ] Le banconote e l'ATM hanno texture e nome corretti (nessuna texture "mancante" viola/nera).
24. [ ] Bonifico con causale: compare nello storico di chi invia ("Bonifico a Nome Cognome" + causale) e di chi riceve ("Bonifico da ..."); senza causale non c'è la riga extra.
25. [ ] Chi riceve un bonifico (online, personaggio attivo) vede un avviso in chat con importo, mittente e causale; con un altro personaggio attivo o per un'organizzazione non arriva nulla.
26. [ ] ATM aperto e blocco rotto da un altro giocatore (o allontanandosi): la schermata si chiude da sola entro mezzo secondo.
27. [ ] Un importo scritto nella scheda Contanti non compare nella scheda Bonifico e viceversa.
28. [ ] `/money take <giocatore> 10`: il saldo scende, l'audit resta coerente; con fondi insufficienti fallisce senza modificare nulla.
