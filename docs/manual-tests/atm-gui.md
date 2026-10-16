# Lista di controllo manuale — interfaccia dell'ATM (sportello d'epoca)

Prerequisiti: `dev-start.bat` (mai `clean`), un giocatore con Character e un conto con del denaro, un ATM piazzato
(`/qrm admin atm give`), almeno una valuta con contanti. Le regole non sono cambiate: qui si controlla solo la resa.

1. [x] Click destro sull'ATM: si apre la cornice di marmo smeraldo con filetto d'ottone, targa "ATM" in alto al centro.
2. [x] Il cassiere (bombetta smeraldo, panciotto, papillon) sta sul bordo superiore a destra, con i piedi sulla cornice.
   Ciclo di circa 1,1 s: alza il braccio, lancia una moneta che gira e la riprende, poi pollice in su sorridendo.
3. [x] Tre tasti d'ottone a sinistra (libro, banconote, frecce). Il selezionato è verde acceso; hover più chiaro; il
   nome compare come suggerimento.
4. [x] Scheda Conto: targa nera con il saldo in oro (fino a 3 valute). "Attività recente" su carta da registro;
   importi positivi verdi, negativi rossi; la causale in inchiostro più chiaro; nessun testo esce dal riquadro.
5. [x] Scheda Conto senza movimenti: "Nessuna attività" nel registro; senza saldo: testo attenuato nella targa.
6. [x] Scheda Contanti: pulsante valuta (cicla), campo importo, Preleva (verde smalto) e Versa tutto (verde
   banconota). I contanti in mano compaiono nel riquadro di carta. Senza contanti: messaggio attenuato.
7. [x] Scheda Bonifico: destinatario, valuta, importo, causale, Invia. Il campo con il focus ha il bordo d'ottone;
   il testo digitato è in inchiostro e il suggerimento è visibile.
8. [x] Dopo ogni operazione l'esito compare in basso a sinistra (verde se riuscita, rosso altrimenti).
9. [x] Pulsante Chiudi (rosso) in basso a destra; Esc chiude. Hover e premuto cambiano texture.
10. [x] Resa a scala GUI 2, 3 e 4 e con finestra piccola: la cornice resta intera, la mascotte non esce dallo schermo,
    i tasti laterali non vengono tagliati.
11. [x] Cambio scheda con testo nei campi: i valori digitati restano per scheda come prima.
12. [x] Client senza il mod della banca: nessun errore (la schermata è solo del client del modulo).

## Seconda passata (leggibilità e funzioni)

13. [x] Il titolo "ATM" sulla targa è nitido (senza ombra). I suggerimenti dei campi sono più chiari del testo digitato.
14. [x] I pulsanti normali (Valuta, 25%…) sono d'ottone con testo scuro e si staccano dal panno verde.
15. [x] Contanti e Bonifico: sotto l'importo ci sono 25%, 50%, 75% e Tutto (Max). Calcolano sul saldo del conto nella
    valuta scelta (arrotondato per difetto) e riempiono il campo; disabilitati con saldo zero.
16. [x] Preleva/Invia sono attivi solo con un importo valido e non oltre il saldo (Invia anche con un destinatario);
    un importo non valido o troppo alto diventa rosso. "Versa tutto" è attivo solo se hai contanti in quella valuta.
17. [x] Su Contanti e Bonifico, in basso a sinistra compare "Saldo: …" nella valuta scelta; dopo un'operazione
    compare l'esito (verde/rosso) al suo posto.
18. [x] Conto: il registro è rigato come un libro mastro. Con più movimenti di quanti ne entrano, la rotellina scorre e
    compare una barra d'ottone a destra.
19. [x] Dopo un'operazione riuscita il cassiere fa il pollice in su e sorride per circa 2 secondi, poi riprende il ciclo.

## Terza passata (più grafica, meno testo)

20. [x] Conto: nessuna etichetta. Targa del saldo con moneta; registro con un'icona per riga (banconota che esce/entra,
    busta, freccia verde/rossa, sigillo, moneta). Riga a due linee: nome della controparte e data, oppure causale.
    Passando sopra una riga compare il suggerimento con motivo, data e causale.
21. [x] Conto senza movimenti: nel registro compare solo il libro (nessun testo). Nessun contante: la banconota.
22. [x] Bonifico: i campi hanno un'icona (persona, moneta, foglio) e nessun testo di suggerimento; a campo vuoto
    il mouse sopra mostra il nome del campo.
23. [x] Il pulsante valuta mostra moneta, codice e le frecce; con una sola valuta è disattivato.
24. [x] In basso a sinistra una targa a vetro scuro: l'esito con spunta verde o croce rossa, altrimenti la moneta e il
    saldo in oro. A destra una X rossa chiude (suggerimento "Chiudi").
25. [x] Contanti: nel riquadro di carta ogni valuta ha la sua banconota e l'importo; senza contanti la targa ha la croce.
26. [x] Testi più leggibili: chiaro su fondo scuro, scuro senza ombra su carta e ottone.

## Quarta passata (icone con aria, tasti disegnati)

27. [x] Le icone hanno più margine: nei campi, nelle righe del registro, nella targa del saldo e in basso.
28. [x] Chiudi è il simbolo power (cerchio con la barra) su un tasto rosso; il nome compare al passaggio. Esc chiude.
29. [x] 25%, 50%, 75% e MAX sono tasti d'ottone con il numero disegnato nella texture (non testo del gioco), in rilievo;
    normale, hover, premuto e disattivato (senza saldo) hanno texture diverse.
30. [x] Bonifico: il destinatario ha una testa chibi (capelli arancioni, codino, sorriso); importo e causale non hanno
    icone ma un breve suggerimento in marrone. Il pulsante valuta ha solo il codice e le frecce.
31. [x] SEND è un tasto verde disegnato a parte (busta, borchie, scintille e tre chevron) con la scritta nella texture;
    disattivato è grigio. Si attiva solo con destinatario e importo validi.

32. [x] Il cassiere ha lo stile a blocchi con contorno scuro e luci calde; la testa del destinatario nel Bonifico è la sua.

## Quinta passata (contrasto)

33. [x] Nessun testo ha l'ombra, né quello del gioco né quello disegnato nelle texture (numeri e SEND).
34. [x] 25%, 50%, 75%, MAX e SEND hanno testo crema su smalto verde scuro; disattivati: testo chiaro su grigio scuro.
35. [x] Campi di testo, registro e riquadro dei contanti sono scuri con testo crema; i suggerimenti sono verde chiaro.
    Importi positivi verde chiaro, negativi rosso chiaro; un importo non valido nel campo diventa rosso chiaro.
36. [x] Non resta testo scuro su fondo chiaro, tranne "ATM" sulla targa d'ottone.
