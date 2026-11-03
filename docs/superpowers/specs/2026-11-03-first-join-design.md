# Primo arrivo — specifica di progetto

- Data: 2026-11-03
- Stato: bozza, da approvare. Nulla è implementato.
- Versione di partenza: QRM 0.29.0 (Minecraft 26.2, NeoForge 26.2.0.88, Java 25)
- Si appoggia a: [Interaction Points](2026-10-10-interaction-points-design.md) (punti configurati dallo staff),
  [qrm-wardrobe](2026-10-16-qrm-wardrobe-design.md) (abiti), `qrm-bank` (contanti) come esempi di Module esterno.

## 1. Obiettivo

Quando un giocatore inizia la vita di un Character nuovo, non compare semplicemente nel mondo: **si risveglia**. Si
trova a terra in un vicolo della città, apre gli occhi, si rialza e da quel momento è libero. Nessun obiettivo, nessuna
guida, nessun tutorial.

Il senso che deve restare al giocatore è quello di essersi perso in un mondo nuovo, di cominciare una nuova vita da zero.
Per questo l'arrivo è il primo momento di gioco e non un'introduzione da guardare.

Il personaggio ha con sé il minimo per ripartire: abiti civili, la carta d'identità, un po' di contanti e qualche altro
oggetto (da definire, vedi sezione 6).

Criteri di riuscita:

1. Un giocatore crea un Character, lo seleziona per la prima volta e si ritrova a terra nel vicolo, con l'animazione
   del risveglio.
2. Finita l'animazione è libero di muoversi e non riceve nessuna indicazione su cosa fare.
3. L'arrivo accade **una sola volta per Character**: selezionarlo di nuovo, riconnettersi o cambiare Character e
   tornare non lo ripetono.
4. Lo staff imposta il punto del vicolo con un comando, senza toccare file o codice.
5. Aggiungere in futuro un secondo scenario d'arrivo non richiede di riscrivere il sistema.

## 2. Fuori dalla prima versione (anti-obiettivi)

- **Nessuna telecamera o cinematica.** Niente camera bloccata o in movimento: è solo l'animazione del personaggio.
- **Nessun tutorial, obiettivo o indicazione** dopo il risveglio. È voluto che il giocatore sia lasciato libero.
- **Un solo scenario** (il vicolo). Autobus, treno, ospedale, centrale e gli altri arrivi sono previsti dopo.
- **Nessuna scelta del giocatore** sullo scenario: quando ce ne saranno più di uno, l'estrazione sarà totalmente casuale.
- Nessun legame con la storia scritta dal giocatore per il Character.
- Non è un successo se funziona ma il risveglio sembra un teletrasporto con un effetto sopra: l'animazione del corpo
  che si alza è la parte che conta.

## 3. Decisioni prese

| Tema | Decisione |
|---|---|
| Quando scatta | Alla prima volta che un Character nuovo entra in gioco, una sola volta per Character |
| Dove | Un vicolo della città. Il primo e per ora unico punto di arrivo |
| Chi sceglie lo scenario | Casuale, ma con un solo scenario per ora |
| Animazione | Il giocatore giace a terra, apre gli occhi, si alza. Niente camera |
| Dopo il risveglio | Libero subito. Abiti civili, carta d'identità, contanti e altro addosso |
| Struttura | Scenari intercambiabili: aggiungerne uno non cambia gli altri |

## 4. Scelte mie (da confermare)

Queste non sono state dette dal committente: sono le mie ipotesi, segnate come tali.

- **Dove vive il codice.** Un Module esterno nuovo (nome di lavoro `qrm-arrival`), compilato solo contro `qrm-api` e
  `qrm-client-api`, come `qrm-bank` e `qrm-locker`. Il Core non cambia. L'alternativa è metterlo in `qrm-immersion`,
  che ha già pose del corpo e sincronizzazione al client.
- **Aggancio.** Il Module ascolta `CharacterSelectedEvent` e controlla un contrassegno "arrivo già avvenuto" salvato
  per Character. La prima volta che manca, avvia lo scenario. Si usa la selezione e non `CharacterCreatedEvent`,
  perché alla creazione il Character potrebbe non essere ancora quello attivo.
- **Primo Character.** Il Core rende attivo il primo Character di un giocatore alla creazione senza annunciare una
  selezione: il Module ascolta anche `CharacterCreatedEvent` e avvia l'arrivo se il Character creato è quello attivo
  (emerso dalla revisione finale).
- **Il punto del vicolo.** Un punto di arrivo impostato dallo staff con un comando. Se nessun punto è impostato, il
  giocatore entra come oggi (nessun arrivo, nessun errore), e il log lo segnala allo staff.
- **Effetto degli occhi.** Effetto a schermo: buio e sfocatura che si schiariscono mentre il personaggio si
  rialza. Non muove la camera.
- **Esito della prova sulla posa a terra (piano, task 6).** Si usano le pose di gioco, come già fa `qrm-medical` per il
  ferito a terra: `Pose.SWIMMING` (disteso) tenuta ogni tick dal server e dal client del giocatore, poi
  `Pose.CROUCHING` nella prima metà del rialzo e `Pose.STANDING`. Gli altri giocatori la vedono perché la posa è
  replicata dal server. Scartate: `Pose.SLEEPING` (richiede un letto) e la rotazione del modello nel rendering
  (inutilmente fragile). **Non ancora verificata in gioco**: va controllata con due client (`docs/manual-tests/arrival.md`).
- **Durante l'animazione** il giocatore non può muoversi, attaccare o usare oggetti (come la posa `HANDS_UP` di
  `qrm-immersion`). Il blocco ha una durata massima, così un'interruzione non lo lascia bloccato.
- **Disconnessione a metà.** Il contrassegno "già avvenuto" si scrive solo a risveglio finito. Se il giocatore esce
  prima, alla riconnessione l'animazione riparte da capo.
- **Il kit** viene dato una volta, insieme al contrassegno, e non si ridà se il giocatore riesce a ripetere l'arrivo.
- **Scenari intercambiabili.** Uno scenario descrive posizione, animazione e kit. Il sistema ne estrae uno a sorte tra
  quelli disponibili; oggi ce n'è uno.
- **Modulo separato, con la sua posa.** `qrm-arrival` è un Module a sé. Come `qrm-wardrobe` e `qrm-immersion`, può usare
  direttamente le API di Minecraft e NeoForge (si appoggia a `qrm-neoforge` in esecuzione): il vincolo "solo `qrm-api` e
  `qrm-client-api`" riguarda la compilazione contro i moduli QRM, non l'accesso a Minecraft. Non riusa `BodyPose`, che
  descrive solo posizioni delle braccia: la posa a terra si ottiene con la posa di gioco già esistente per chi dorme/giace.
  Se mancasse una funzione in `qrm-api` (spostare il giocatore, salvare dati per Character), si estende l'API in modo
  compatibile, con un aumento di `API_VERSION`.
- **Character già esistenti.** Quelli creati prima dell'installazione del Module risultano già arrivati e non vivono
  l'arrivo né ricevono il kit. Il Module registra la data della sua prima installazione e confronta con la data di
  creazione del Character.
- **Character creati da staff o importati.** Seguono la stessa regola: contano solo la selezione e il contrassegno,
  non il modo in cui il Character è stato creato.
- **Kit e contrassegno insieme.** Il kit viene dato e il contrassegno scritto nello stesso passo, a risveglio finito.
  Se il giocatore muore o esce prima, l'animazione riparte da capo e non è stato dato niente.
- **Durate.** Animazione e durata massima del blocco sono valori configurabili, con valori di partenza scelti da chi
  costruisce.
- **Punto non utilizzabile.** Se il punto è bloccato o in un chunk non caricabile, il giocatore entra come oggi e il
  log lo segnala. Per ora un solo punto globale; con più scenari ne avrà uno ciascuno.

## 5. Cosa esiste già nel progetto

- Selezione e creazione del Character: `CharacterScreen`, `CharacterActions`, `CharacterCreatedEvent`,
  `CharacterSelectedEvent`. Gli altri moduli (wardrobe, medical, police, immersion) si agganciano già a questi eventi.
- Contanti: `qrm-bank` ha tagli e inventario di contanti (`CashDenominations`, `CashInventory`).
- Abiti: `qrm-wardrobe` gestisce gli outfit come skin.
- Punti configurati dallo staff: `PointService` e i punti di interazione.
- Pose del corpo sincronizzate al client: `BodyPose` in `qrm-immersion` (oggi solo mani alzate e indicare).
- **Non esistono**: la carta d'identità come oggetto (oggi è solo una grafica nella scheda del Character, con un
  numero documento derivato dall'id), un sistema di spawn dedicato, una posa "a terra / che si alza" e qualunque
  telecamera scriptata (esiste solo quella della pistola).

## 6. Decisioni sul kit e sul personaggio

- **Il kit della prima versione:** abiti civili e **100 € in contanti**. Nient'altro.
- **La carta d'identità** diventerà un oggetto reale, ma in un secondo momento. Nella prima versione non è nel kit;
  il kit è pensato come elenco configurabile, così l'oggetto si aggiunge senza cambiare il sistema.
- **Il personaggio sa chi è.** Non c'è amnesia: il senso di smarrimento viene dal luogo e dalla ripartenza da zero.
  Testi e wiki non devono parlare di perdita di memoria.

## 7. Lasciato a chi costruisce

- Nome definitivo e collocazione del Module.
- Come si fa la posa a terra e l'animazione del rialzarsi, e come arriva ai client che guardano.
- Il formato in cui si descrive uno scenario e dove si salva il contrassegno per Character.
- I testi della wiki e del comando.
