# Lista di controllo manuale — Wardrobe (divise di lavoro)

Prerequisiti: `dev-start.bat` (mai `clean`), due giocatori A e B con un Character ciascuno, il lavoro di esempio
`police` (`docs/samples/jobs/police.json`) con i gradi `officer` e `chief`. Copiare `docs/samples/wardrobe/` in
`config/qrm_wardrobe/outfits/` del server, aggiungere una seconda divisa `capo.json` + `capo.png` con
`"minGrade": "chief"` e `"model": "slim"`, riavviare. Creare un armadietto: `/qrm admin point create wardrobe job police`.

## Armadietto e scelta

1. [ ] A è `officer` in servizio: il clic destro sul punto apre la schermata con "Abito civile" e "Agente di pattuglia"; "Capo" non c'è.
2. [ ] A seleziona "Agente di pattuglia": il manichino grigio la indossa. "Indossa" diventa attivo; premuto, il punto elenco mostra il segno sulla divisa corrente e "Indossa" si disattiva.
3. [ ] B, vicino, vede A con il corpo della divisa e la testa di A (viso e capelli, strato esterno compreso), non con la testa del manichino.
4. [ ] A, in prima persona, ha ancora il braccio con la skin originale (limite noto).
5. [ ] A apre di nuovo l'armadietto e sceglie "Abito civile": B rivede A con la skin normale.
6. [ ] A promosso a `chief`: nell'armadietto compare anche "Capo"; indossata, A ha le braccia sottili.
7. [ ] A declassato a `officer` mentre indossa "Capo": la divisa torna alla skin normale senza riaprire nulla.

## Servizio, Character, riavvio

8. [ ] A indossa la divisa ed esce di servizio (`/duty off`): torna alla skin normale. Rientrando in servizio la divisa non si rimette da sola.
9. [ ] Fuori servizio, il clic sul punto non apre nulla (regola del Core).
10. [ ] A indossa la divisa e cambia Character (`/character`): B vede A con la skin normale; tornando al primo Character in servizio, la divisa non c'è finché non la sceglie di nuovo.
11. [ ] A indossa la divisa e il server si riavvia: dopo il rientro in servizio la divisa risulta ancora indossata (e viene tolta se A non è in servizio o non ha più il grado).

## Cartella e comando

12. [ ] Un PNG 64x32, un PNG oltre 32 KB, un JSON senza PNG e un lavoro inesistente: il server li salta e il log dice file e motivo; le altre divise si caricano.
13. [ ] `/wardrobe reload` da operatore: stampa quante divise ha caricato. Rimossa una divisa indossata, chi la aveva torna alla skin normale.
14. [ ] `/wardrobe reload` da non operatore: non è accettato.

## Client e resa

15. [ ] Un client senza il mod entra e gioca: vede tutti con la skin normale. Il clic sul punto gli mostra il messaggio che serve il mod.
16. [ ] Un giocatore con una skin 64x32 (vecchio formato) o appena entrato: nessun errore nel log; la divisa compare appena la sua skin è disponibile.
17. [ ] Schermata a scala GUI 2, 3 e 4: lista, manichino e pulsanti dentro il pannello; la rotellina scorre una lista con più di 6 voci.
18. [ ] Con 20 divise caricate e due giocatori in vista, nessun calo evidente di prestazioni.
