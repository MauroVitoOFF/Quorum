# Lista di controllo manuale — punti di interazione (v0.14.0)

Prerequisiti: server dev (`./gradlew prepareServerRun prepareClientRun`, poi `runServer`/`runClient`; mai `clean`), un job `police` con almeno due gradi (es. `cadet`, `chief`), due giocatori operatori/non operatori con personaggio attivo: A con job `police`, B senza job (o con un altro job).

1. [ ] Il server parte con la migrazione 7 applicata (tabella `qrm_interaction_point`); un mondo salvato con la 0.13.1 si apre senza perdere dati.
2. [ ] `/qrm admin point create duty job police` inquadrando una faccia laterale di un blocco (es. una cassa): "Punto #1 creato" e l'icona compare solo su quella faccia (guardando sopra o sotto, su tutte e quattro). Senza inquadrare un blocco: errore chiaro.
3. [ ] Stesso comando sullo stesso blocco: rifiutato ("ospita già il punto #1"). `create duty org x`: rifiutato (il tipo accetta solo job).
4. [ ] `/qrm admin point info` sul blocco: mostra proprietario, posizione, grado minimo, visibilità, stato "attivo".
5. [ ] A (police) vede sulla cassa una icona 32x32 del servizio (timbracartellino con orologio e frecce) incollata, in alto a sinistra, su ciascuna faccia laterale del blocco (fissa sul blocco, non segue la camera, illuminata come il blocco: di notte si scurisce), entro ~32 blocchi. B non vede né icona né suggerimento. Un resource pack che sostituisce `assets/qrm/textures/point/duty.png` cambia l'icona.
6. [ ] A fa click destro: la cassa NON si apre, A riceve "Sei in servizio." e l'HUD mostra il pallino di servizio acceso. Secondo click: "Sei fuori servizio.". Un click solo per volta (nessun doppio effetto dalla mano secondaria).
7. [ ] B fa click destro: la cassa NON si apre, messaggio "Non puoi usare questo punto.". Un giocatore senza personaggio attivo: "Nessun personaggio attivo.".
8. [ ] `set name Centrale di polizia`: il nome è visibile in `point info`/`list` (subito, senza riconnettersi). `set name none` lo toglie.
9. [ ] `set grade chief`: A (cadet) non può più usare il punto e l'icona scompare; promuovendo A a chief (`/qrm admin job set`) riappare senza riconnettersi. `set grade none` riapre a tutti i membri.
10. [ ] `set public true`: B vede l'icona ma il click resta negato. `set public false` la nasconde di nuovo.
11. [ ] Con `selfDuty = false` nella config: `/duty` risponde "Il servizio si gestisce in azienda." ma il click sul punto funziona.
12. [ ] Rompere il blocco marcato (anche in creativa, anche da un operatore): impedito, messaggio che invita a usare `point remove`.
13. [ ] Distruggere il blocco con una esplosione/pistone: `point list` lo segnala "inattivo (blocco mancante)", l'icona sparisce, un click sul posto non fa nulla. Rimettendo un blocco il punto torna attivo.
14. [ ] `/qrm admin point move 1` inquadrando un altro blocco: il punto si sposta, l'id resta, il vecchio blocco torna normale.
15. [ ] `/qrm admin point list` e `list job police`: id, tipo, proprietario, posizione, stato. `list org gang` per una organizzazione senza punti: "Nessun punto.".
16. [ ] Rimuovere il job dai file e riavviare: il punto resta salvato e compare come "inattivo (proprietario inesistente)"; ripristinando il job torna attivo.
17. [ ] `/qrm admin point remove` (inquadrato) e `remove 1` (per id): il punto sparisce dall'elenco e dai client.
18. [ ] I comandi non sono visibili/eseguibili da un giocatore non operatore; ogni comando riuscito compare nel registro staff (`cmd.point.*`).
19. [ ] Due giocatori in dimensioni diverse (Overworld/Nether) con punti nelle due: ognuno vede solo le etichette della propria dimensione.
20. [ ] Client senza il mod QRM su un server con punti: si connette e gioca normalmente (canale opzionale); il click sui punti funziona comunque lato server.
21. [ ] Un mod di terze parti che registra un proprio tipo nel costruttore (`QRM.get(PointTypeRegistry.class).register(...)`) compare tra i suggerimenti di `point create`.
22. [ ] `/qrm admin point set face here` guardando una faccia laterale del blocco: l'icona resta solo su quella faccia (anche per gli altri giocatori); `set face north|south|east|west` la fissa a una faccia precisa, `set face all` la rimette su tutte e quattro. Guardando sopra o sotto, `here` risponde con un errore. `info` mostra la faccia; dopo un riavvio del server la scelta è conservata e `point move` la mantiene.
