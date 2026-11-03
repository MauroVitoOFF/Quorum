# Lista di controllo manuale — Arrival (primo arrivo)

Prerequisiti: `dev-start.bat` (mai `clean`), due giocatori A e B (due client), un operatore. Il modulo è già incluso in
`qrm-dev`. L'arrivo parte solo per i Character creati dopo la prima installazione del modulo: creare un Character nuovo
per ogni prova.

## Impostare il punto

1. [ ] Senza punto impostato, A crea un Character e lo seleziona: entra come sempre (nessun arrivo) e il log del server dice che manca il punto (`/qrm arrival setspawn`).
2. [ ] Un operatore sta in piedi in un vicolo e lancia `/qrm arrival setspawn`: risponde "Punto di arrivo impostato." e compare `config/qrm_arrival/spawn.json`.
3. [ ] Un non operatore non può lanciare `/qrm arrival setspawn`.

## Il risveglio

4. [ ] A crea un Character nuovo e lo seleziona: compare nel vicolo, **disteso a terra**, con lo schermo buio e le palpebre chiuse.
5. [ ] Le palpebre si aprono e il buio si schiarisce; il personaggio passa da disteso ad accovacciato e poi in piedi. La camera non si muove da sola.
6. [ ] B, vicino, vede A distesa a terra, poi accovacciata, poi in piedi (la posa è visibile agli altri).
7. [ ] Durante il risveglio A non riesce a camminare, attaccare, rompere o piazzare blocchi, né usare oggetti.
8. [ ] A fine animazione A è libera, senza nessuna indicazione su cosa fare, e ha in inventario 100,00 di contanti (una banconota da 10000 unità minime della valuta `QRM`).
9. [ ] A seleziona di nuovo lo stesso Character (anche da `/character`): non succede nulla, nessun secondo risveglio, nessun secondo kit.
10. [ ] Un Character già esistente prima dell'installazione non vive l'arrivo e non riceve il kit.

## Interruzioni

11. [ ] A esce dal gioco a metà del risveglio e rientra: nessun kit ricevuto; selezionando di nuovo il Character l'arrivo riparte da capo.
12. [ ] A cambia Character a metà del risveglio: il blocco finisce e il primo Character, se riselezionato, rivive l'arrivo.
13. [ ] Il server si ferma a metà del risveglio: alla ripartenza A non è bloccata e, selezionando il Character, l'arrivo riparte.
14. [ ] Un client senza il modulo entra e gioca: il server lo sposta nel vicolo ma non vede buio né palpebre; gli altri lo vedono comunque disteso.

## Kit e file

15. [ ] Con l'inventario pieno, i contanti cadono a terra ai piedi di A e non spariscono.
16. [ ] In `config/qrm_arrival/kit.json` con un oggetto inesistente: l'oggetto è saltato e il log lo dice; l'arrivo non si blocca.
17. [ ] Con `kit.json` rotto: si usa il kit predefinito e il log lo dice.
18. [ ] Con una dimensione inesistente in `spawn.json`: A entra come sempre e il log lo dice.
19. [ ] `config/qrm_arrival/timing.json` con `{"lyingTicks":20,"risingTicks":20}` e `/qrm arrival reload`: il risveglio dura circa due secondi.
