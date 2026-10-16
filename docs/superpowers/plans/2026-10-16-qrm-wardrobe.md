# qrm-wardrobe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Un Module `qrm-wardrobe` in cui ogni lavoro ha divise che sono skin, cambiate da un armadietto (punto di interazione) e viste da tutti, con la testa originale del giocatore.

**Architecture:** Logica pura (catalogo, regole, stato, composizione dei pixel) testata con JUnit; sopra, il collegamento NeoForge: punto `wardrobe`, eventi di servizio/lavoro/Character, canale di rete opzionale, e sul client un aggancio a `RenderPlayerEvent.Pre` che sostituisce `AvatarRenderState.skin` con una `PlayerSkin` ricomposta. Segue il modello di `qrm-locker`.

**Tech Stack:** Java 25, NeoForge 26.2.0.88, Minecraft 26.2, JUnit 5.11, Gson, solo `qrm-api` e `qrm-client-api` a compile time.

**Spec:** `docs/superpowers/specs/2026-10-16-qrm-wardrobe-design.md`

## Global Constraints

- Versione di arrivo `0.24.0` in `gradle.properties` (`qrm_version`) e in `qrm-api/.../QrmVersion.java` (`API_VERSION`).
- Modulo `qrm-wardrobe`, modId `qrm_wardrobe`, pacchetto `dev.qrm.wardrobe` (`logic`, `mc`, `net`, `client`); compila solo contro `qrm-api` e `qrm-client-api`; il Core non cambia.
- Canale di rete `optional()`; il mod non registra blocchi, oggetti o tipi di menu.
- Tipo di punto `wardrobe`, proprietari solo `PointOwner.Kind.JOB`, `requiresDuty = true`.
- Cartella delle divise `config/qrm_wardrobe/outfits/`; ogni divisa è `<id>.json` + `<id>.png` con lo stesso nome; `<id>` valido secondo `JobDefinition.isValidId`.
- JSON: `label` (1–40 caratteri), `job` (lavoro esistente), `minGrade` (facoltativo, grado di quel lavoro), `model` (`classic`|`slim`, predefinito `classic`).
- PNG: skin 64x64, al massimo 32 KB (32768 byte); al massimo 128 divise.
- Rettangoli presi dalla skin originale: testa `x 0–31, y 0–15` e strato esterno `x 32–63, y 0–15`.
- Dati: `ModDataService`, namespace `qrm_wardrobe`, chiave per Character; divisa indossata ricontrollata a ogni evento elencato nella spec 5.2.
- Testi: concetti in italiano; due lingue `it_it` e `en_us` con le stesse chiavi (`LangCoverageTest`).
- Commit senza attribuzione diversa dal trailer già in uso; niente `clean` di Gradle.

## Review Focus

1. Un PNG a 64x32 (vecchio formato) o con trasparenza completa: la divisa viene saltata con il motivo nel log, non causa errori a runtime.
2. Un file `.json` senza `.png` (o il contrario) nella cartella: segnalato e ignorato, le altre divise si caricano.
3. Un lavoro o un grado che sparisce dopo un reload mentre qualcuno indossa la divisa: la perde, i client sono avvisati.
4. Skin originale in formato 64x32 o non ancora scaricata: nessuna eccezione, la testa è quella disponibile (Steve/Alex).
5. Un client malevolo che manda `ChoosePayload` con un id inesistente o di un altro lavoro: rifiutato dal server, nessun cambiamento.

---

### Task 1: Scheletro del Module

**Files:**
- Modify: `settings.gradle` (aggiungere `'qrm-wardrobe'` a `include`), `qrm-dev/build.gradle` (riga `runtimeOnly(project(':qrm-wardrobe')) { transitive = false }` accanto a quella di `qrm-locker`), `dev-start.bat` solo se elenca i moduli a mano (controllare con `grep locker`)
- Create: `qrm-wardrobe/build.gradle` (copia di `qrm-locker/build.gradle` con `archivesName = 'qrm-wardrobe'` e `mods { qrm_wardrobe ... }`), `qrm-wardrobe/src/main/resources/META-INF/neoforge.mods.toml` (come quello del Locker, modId `qrm_wardrobe`, displayName `QRM Wardrobe`), `qrm-wardrobe/src/main/resources/assets/qrm_wardrobe/lang/{it_it,en_us}.json`
- Create: `qrm-wardrobe/src/main/java/dev/qrm/wardrobe/mc/WardrobeMod.java`
- Test: `qrm-wardrobe/src/test/java/dev/qrm/wardrobe/logic/LangCoverageTest.java`

**Interfaces:**
- Produces: `WardrobeMod.MOD_ID = "qrm_wardrobe"`, `WardrobeMod.POINT_TYPE = "wardrobe"`; chiave lang `qrm_wardrobe.point.wardrobe`.

- [ ] **Step 1:** Scrivere `LangCoverageTest.bothLanguagesDefineTheSameKeys` (copia del test del Locker, percorso `src/main/resources/assets/qrm_wardrobe/lang/`, verifica che esista `qrm_wardrobe.point.wardrobe`).
- [ ] **Step 2:** Eseguire `./gradlew :qrm-wardrobe:test` — atteso: FAIL (modulo inesistente).
- [ ] **Step 3:** Creare il modulo come sopra. `WardrobeMod` è `@Mod(MOD_ID)` con costruttore `WardrobeMod(IEventBus modBus)` vuoto per ora; `it_it`/`en_us` contengono solo `qrm_wardrobe.point.wardrobe` ("Armadietto delle divise" / "Uniform locker").
- [ ] **Step 4:** `./gradlew :qrm-wardrobe:build` — atteso: BUILD SUCCESSFUL, test verde.
- [ ] **Step 5:** Commit `feat(wardrobe): scheletro del modulo`.

---

### Task 2: Catalogo delle divise (lettura e validazione)

**Files:**
- Create: `qrm-wardrobe/src/main/java/dev/qrm/wardrobe/logic/{OutfitModel,OutfitDefinition,JobLookup,OutfitCatalog}.java`
- Test: `qrm-wardrobe/src/test/java/dev/qrm/wardrobe/logic/OutfitCatalogTest.java`

**Interfaces:**
- Produces:
  - `enum OutfitModel { CLASSIC, SLIM }`
  - `record OutfitDefinition(String id, String label, String job, String minGrade, OutfitModel model, byte[] png)` (`minGrade` può essere `null`)
  - `interface JobLookup { boolean hasJob(String job); OptionalInt gradeLevel(String job, String grade); }`
  - `OutfitCatalog.load(Path dir, JobLookup jobs, Consumer<String> problems) -> OutfitCatalog`; `Optional<OutfitDefinition> find(String id)`; `List<OutfitDefinition> forJob(String job)`; `List<OutfitDefinition> all()` (ordinate per id); costanti `MAX_PNG_BYTES = 32768`, `MAX_OUTFITS = 128`, `MAX_LABEL = 40`.

- [ ] **Step 1:** Test (cartella temporanea `@TempDir`, PNG sintetici 64x64 generati con `BufferedImage`/`ImageIO`; `JobLookup` finto con lavoro `police` e gradi `officer`=1, `captain`=2):
  - `loadsAValidOutfit`: `agente.json` + `agente.png` → `find("agente")` presente, `model()` = `CLASSIC` quando il campo manca, `minGrade()` = `"officer"`.
  - `skipsMissingPng` e `skipsMissingJson`: la divisa non c'è, `problems` riceve una riga che cita il nome del file, le altre si caricano.
  - `skipsWrongSizePng` (64x32), `skipsOversizedPng` (> 32768 byte), `skipsNotAPng` (byte casuali).
  - `skipsUnknownJobOrGrade`, `skipsBadLabel` (vuoto e 41 caratteri), `skipsInvalidId` (nome file `Bad Name.json`), `skipsBadModel` (`"model":"wide"`).
  - `capsAt128Outfits`: 130 divise valide → `all().size() == 128` e un problema segnalato.
  - `emptyOrMissingDirectoryIsEmptyCatalog`.
- [ ] **Step 2:** `./gradlew :qrm-wardrobe:test --tests '*OutfitCatalogTest'` — atteso: FAIL (classi mancanti).
- [ ] **Step 3:** Implementare con Gson e `javax.imageio`: leggere ogni `*.json` della cartella, cercare il `*.png` omonimo, controllare nell'ordine id, file, JSON, label, job, grado, modello, PNG (dimensioni 64x64 dall'header, byte ≤ 32768). Un errore salta la sola divisa e chiama `problems.accept("<file>: <motivo>")`.
- [ ] **Step 4:** Rieseguire il test — atteso: PASS.
- [ ] **Step 5:** Commit `feat(wardrobe): catalogo delle divise`.

---

### Task 3: Regole e stato di chi indossa

**Files:**
- Create: `qrm-wardrobe/src/main/java/dev/qrm/wardrobe/logic/{Wearer,WardrobeRules,WardrobeStorage,ModDataWardrobeStorage,WardrobeService}.java`
- Test: `qrm-wardrobe/src/test/java/dev/qrm/wardrobe/logic/{WardrobeRulesTest,WardrobeServiceTest}.java`

**Interfaces:**
- Consumes: `OutfitCatalog`, `OutfitDefinition`, `JobLookup` (Task 2); `ModDataService`, `OwnerRef.character`, `CharacterId` da `qrm-api`.
- Produces:
  - `record Wearer(String job, String grade, boolean onDuty)`
  - `WardrobeRules.canWear(OutfitDefinition outfit, Wearer wearer, JobLookup jobs) -> boolean` (stesso lavoro, in servizio, livello del grado ≥ livello di `minGrade`, oppure `minGrade` assente); `WardrobeRules.available(OutfitCatalog catalog, String pointJob, Wearer wearer, JobLookup jobs) -> List<OutfitDefinition>`
  - `interface WardrobeStorage { Optional<String> read(CharacterId c); void write(CharacterId c, String outfitIdOrNull); }`; `ModDataWardrobeStorage(ModDataService)` (namespace `qrm_wardrobe`, chiave `worn`, JSON `{"outfit":"<id>"}`; `write(c, null)` cancella la chiave)
  - `enum ChooseResult { OK, UNKNOWN_OUTFIT, NOT_ALLOWED, WRONG_JOB }`
  - `WardrobeService(WardrobeStorage storage, JobLookup jobs)`: `void setCatalog(OutfitCatalog c)`; `ChooseResult choose(CharacterId c, String outfitId /* "" = civile */, String pointJob, Optional<Wearer> wearer)`; `boolean revalidate(CharacterId c, Optional<Wearer> wearer)` (true se la divisa è stata tolta); `Optional<String> worn(CharacterId c)` (legge dal disco la prima volta, poi dalla memoria); `Map<CharacterId,String> allWorn()`.

- [ ] **Step 1:** Test `WardrobeRulesTest`: lavoro diverso → false; fuori servizio → false; grado sotto/pari/sopra il minimo → false/true/true; `minGrade` nullo → true; `available` restituisce solo le divise del `pointJob` che passano.
- [ ] **Step 2:** Test `WardrobeServiceTest` con `WardrobeStorage` in memoria: `choose` valido salva e `worn` lo restituisce; `choose("")` rimuove; id inesistente → `UNKNOWN_OUTFIT`; divisa di un altro lavoro rispetto al punto → `WRONG_JOB`; grado insufficiente → `NOT_ALLOWED`; `revalidate` con `Optional.empty()` o fuori servizio toglie e ritorna true; `revalidate` ancora valida ritorna false; dopo `setCatalog` senza la divisa, `revalidate` la toglie; riavviare il servizio sullo stesso storage rilegge la divisa.
- [ ] **Step 3:** `./gradlew :qrm-wardrobe:test` — atteso: FAIL.
- [ ] **Step 4:** Implementare. Il JSON non valido o una chiave mancante in `ModDataWardrobeStorage.read` danno `Optional.empty()`, mai un'eccezione.
- [ ] **Step 5:** Rieseguire — atteso: PASS. Commit `feat(wardrobe): regole e stato delle divise indossate`.

---

### Task 4: Ricomposizione dei pixel

**Files:**
- Create: `qrm-wardrobe/src/main/java/dev/qrm/wardrobe/logic/SkinComposer.java`
- Test: `qrm-wardrobe/src/test/java/dev/qrm/wardrobe/logic/SkinComposerTest.java`

**Interfaces:**
- Produces: `SkinComposer.compose(int[] outfit, int[] original) -> int[]`. Entrambi sono pixel ARGB riga per riga, larghezza 64; `outfit.length == 64*64`, `original.length` è `64*64` oppure `64*32`. Restituisce una nuova matrice 64x64: copia di `outfit` con i rettangoli `x 0–31,y 0–15` e `x 32–63,y 0–15` presi da `original`.
- Constants: `SkinComposer.SIZE = 64`.

- [ ] **Step 1:** Test con immagini sintetiche (ogni pixel codifica `(x,y)`): i 2 rettangoli sono identici all'originale; ogni altro pixel è identico alla divisa; la divisa in ingresso non viene modificata; una skin originale 64x32 funziona; lunghezze errate lanciano `IllegalArgumentException`.
- [ ] **Step 2:** Eseguire `--tests '*SkinComposerTest'` — atteso: FAIL.
- [ ] **Step 3:** Implementare con `System.arraycopy` per riga.
- [ ] **Step 4:** PASS. Commit `feat(wardrobe): ricomposizione della skin con la testa originale`.

---

### Task 5: Collegamento al server

**Files:**
- Create: `qrm-wardrobe/src/main/java/dev/qrm/wardrobe/mc/{WardrobeServer,WardrobeFiles,WardrobeCommand}.java`
- Modify: `qrm-wardrobe/src/main/java/dev/qrm/wardrobe/mc/WardrobeMod.java`

**Interfaces:**
- Consumes: Task 2–3. Da `qrm-api`: `PointTypeRegistry`, `PointType`, `PointUse`, `JobRegistry`, `JobService.assignmentOf`, `CharacterService.activeOf`, `EventBus` con `DutyChangedEvent`, `JobChangedEvent`, `CharacterSelectedEvent`, `PlayerJoinEvent`.
- Produces:
  - `WardrobeFiles.outfitsDir() -> Path` (`FMLPaths.CONFIGDIR/qrm_wardrobe/outfits`, la crea se manca)
  - `WardrobeServer.service() -> WardrobeService`, `WardrobeServer.catalog() -> OutfitCatalog`, `WardrobeServer.reload() -> int` (numero di divise lette), `WardrobeServer.wearerOf(CharacterId) -> Optional<Wearer>`, `WardrobeServer.onUse(PointUse)`, `WardrobeServer.choose(ServerPlayer, String outfitId)` (usata dal pacchetto del Task 6).
  - Comando `/wardrobe reload` (livello operatore) che ricarica e stampa quante divise ha letto e quanti problemi.

- [ ] **Step 1:** Implementare `WardrobeMod`: nel costruttore registrare il `PointType("wardrobe", "qrm_wardrobe.point.wardrobe", Set.of(PointOwner.Kind.JOB), true, WardrobeServer::onUse)`; in `ServerStartedEvent` caricare il catalogo; registrare sull'`EventBus` di QRM i quattro eventi, che chiamano `revalidate` per la Character interessata e, se cambia qualcosa, notificano i client (hook che il Task 6 riempie: `WardrobeServer.setChangeListener(Runnable)`). Su `ServerStoppedEvent` azzerare lo stato statico.
- [ ] **Step 2:** `onUse` non fa nulla di visibile finché il Task 6 non c'è; per ora scrive nel log il numero di divise disponibili.
- [ ] **Step 3:** `./gradlew :qrm-wardrobe:build` — atteso: BUILD SUCCESSFUL.
- [ ] **Step 4:** Commit `feat(wardrobe): punto, eventi e comando di ricarica`.

---

### Task 6: Rete

**Files:**
- Create: `qrm-wardrobe/src/main/java/dev/qrm/wardrobe/net/{WardrobeNetwork,CatalogPayload,WornPayload,OpenPayload,ChoosePayload}.java`
- Modify: `qrm-wardrobe/src/main/java/dev/qrm/wardrobe/mc/{WardrobeServer,WardrobeMod}.java`
- Test: `qrm-wardrobe/src/test/java/dev/qrm/wardrobe/net/WardrobeWireTest.java`

**Interfaces:**
- Consumes: Task 3 e 5.
- Produces (tutti `CustomPacketPayload`, canale `optional()`, `PROTOCOL_VERSION = "1"`, con `TYPE` e `CODEC` come nel Locker):
  - `CatalogPayload(List<Entry> outfits)`, `record Entry(String id, String label, String job, String minGrade, boolean slim, byte[] png)` — S→C
  - `WornPayload(Map<UUID,String> worn)` — S→C, istantanea completa
  - `OpenPayload(List<String> outfitIds, String current)` — S→C
  - `ChoosePayload(String outfitId)` — C→S
  - `WardrobeNetwork.register(RegisterPayloadHandlersEvent)`; lato client i gestori delegano a `WardrobeClientBridge` (Task 7).
  - Server: invio del catalogo a chi entra e dopo `reload`; invio di `WornPayload` a tutti i giocatori con il canale ad ogni cambiamento e a chi entra; `onUse` manda `OpenPayload` con `WardrobeRules.available(...)`; il gestore di `ChoosePayload` chiama `WardrobeServer.choose` e poi rimanda `OpenPayload` aggiornato.

- [ ] **Step 1:** Test `WardrobeWireTest`: ogni payload si scrive e si rilegge con il suo `CODEC` su un `RegistryFriendlyByteBuf` (o buffer equivalente usato da `LockerSkinPayload`/`BossWireTest`); i limiti sono rispettati (`png` oltre 32768 byte → il codec rifiuta; `outfits` oltre 128 → rifiuta; `outfitId` oltre 64 caratteri → rifiuta).
- [ ] **Step 2:** Eseguire `--tests '*WardrobeWireTest'` — atteso: FAIL.
- [ ] **Step 3:** Implementare i payload e `WardrobeNetwork`; cablare il server. `WardrobeServer.choose` verifica sempre sul server: Character attiva, lavoro del punto aperto (ricordato in memoria per giocatore all'apertura, come la sessione dell'ATM), distanza dal punto, poi `WardrobeService.choose`; la risposta del server a un id inesistente o di un altro lavoro è solo il rifiuto, senza cambiare lo stato.
- [ ] **Step 4:** Test → PASS; `./gradlew :qrm-wardrobe:build` → BUILD SUCCESSFUL.
- [ ] **Step 5:** Commit `feat(wardrobe): pacchetti di rete e scelta della divisa`.

---

### Task 7: Rendering sul client

**Files:**
- Create: `qrm-wardrobe/src/main/java/dev/qrm/wardrobe/client/{WardrobeClient,WardrobeClientBridge,ClientOutfits,OutfitTextures,OriginalSkinReader}.java`
- Modify: `qrm-wardrobe/src/main/java/dev/qrm/wardrobe/net/WardrobeNetwork.java` (gestori S→C)

**Interfaces:**
- Consumes: `SkinComposer.compose` (Task 4); payload (Task 6).
- Produces:
  - `@Mod(value = "qrm_wardrobe", dist = Dist.CLIENT) WardrobeClient(IEventBus)`: registra il tema (`ThemeFragments.register("qrm_wardrobe", ...)` come `LockerClient`) e `NeoForge.EVENT_BUS.addListener` per `RenderPlayerEvent.Pre`.
  - `ClientOutfits`: `static void setCatalog(CatalogPayload)`, `static void setWorn(Map<UUID,String>)`, `static Optional<CatalogPayload.Entry> entry(String id)`, `static Optional<String> wornBy(UUID)`.
  - `OutfitTextures.skinFor(UUID player, String outfitId, PlayerSkin original) -> Optional<PlayerSkin>`: restituisce la `PlayerSkin` ricomposta (corpo = `ClientAsset.ResourceTexture` della `DynamicTexture` registrata nel `TextureManager`, modello classic/slim dalla divisa, mantello ed elitre dall'originale), con cache per coppia giocatore+divisa e rilascio (`TextureManager.release`) quando la coppia cambia o il catalogo si ricarica.
  - `OriginalSkinReader.read(PlayerSkin original) -> Optional<int[]>`: legge i pixel ARGB (64 di larghezza) della skin originale; per `ResourceTexture` dal `ResourceManager`, per `DownloadedTexture` dal file della cache su disco (`<gameDir>/assets/skins/<primi 2 caratteri dell'hash>/<hash>`, hash = ultimo segmento di `url`); `Optional.empty()` se non disponibile.
  - Aggancio: in `RenderPlayerEvent.Pre` se `renderState.id` corrisponde a un giocatore del livello client che indossa una divisa, si sostituisce `renderState.skin` con il risultato di `skinFor`; se `read` non riesce, resta la skin originale. Lo stesso per la mano in prima persona.

- [ ] **Step 1:** Implementare nell'ordine `ClientOutfits` → `OriginalSkinReader` → `OutfitTextures` → aggancio. Se `renderState.id` non basta a trovare il giocatore (verifica da spec 9.2), usare `RenderStateExtensions` per copiare l'UUID dall'entità allo stato; annotare la scelta nel codice.
- [ ] **Step 2:** `./gradlew :qrm-wardrobe:build` — atteso: BUILD SUCCESSFUL.
- [ ] **Step 3:** Prova in gioco con `dev-start.bat`: due client, un lavoro con una divisa di prova (PNG con corpo di colore pieno ben riconoscibile), cambiarla con `/wardrobe` non ancora disponibile → temporaneamente forzarla da un comando di sviluppo solo locale non committato; verificare testa originale, braccia classic/slim, mano in prima persona, nessun errore nel log. Annotare cosa si è visto.
- [ ] **Step 4:** Commit `feat(wardrobe): skin ricomposta sul client`.

---

### Task 8: Schermata dell'armadietto con manichino

**Files:**
- Create: `qrm-wardrobe/src/main/java/dev/qrm/wardrobe/client/{WardrobeScreen,MannequinSkin}.java`
- Create: `qrm-wardrobe/src/main/resources/assets/qrm_wardrobe/theme/qrm.json`
- Modify: `qrm-wardrobe/src/main/java/dev/qrm/wardrobe/client/WardrobeClientBridge.java`, lang `it_it`/`en_us`
- Test: `qrm-wardrobe/src/test/java/dev/qrm/wardrobe/logic/LangCoverageTest.java` (già presente: nuove chiavi nelle due lingue)

**Interfaces:**
- Consumes: `OpenPayload`, `ClientOutfits`, `ChoosePayload`.
- Produces: `WardrobeScreen(OpenPayload open)` estende `QrmScreen`: a sinistra la lista ("Abito civile" in testa, poi le divise di `open.outfitIds()`, la corrente evidenziata), a destra il manichino grigio con la divisa evidenziata, in basso il pulsante di conferma che manda `ChoosePayload` e il pulsante di chiusura; `MannequinSkin.forOutfit(String id) -> PlayerSkin` ricompone la divisa con una testa grigia neutra fissa (non legge la skin del giocatore).
- Chiavi lang: `qrm_wardrobe.screen.title`, `qrm_wardrobe.civil`, `qrm_wardrobe.confirm`, `qrm_wardrobe.close`, `qrm_wardrobe.need_mod` (messaggio al giocatore senza mod), `qrm_wardrobe.worn`.

- [ ] **Step 1:** Aggiungere le chiavi lang in entrambe le lingue; `LangCoverageTest` deve restare verde.
- [ ] **Step 2:** Implementare schermata, manichino e aggancio all'apertura nel `WardrobeClientBridge`. Il manichino è disegnato con lo stesso renderer dei giocatori (`InventoryScreen.extractEntityInInventory...` o equivalente di 26.2: verificare la firma in `AtmScreen`/`InventoryScreen`) usando `MannequinSkin`.
- [ ] **Step 3:** `./gradlew :qrm-wardrobe:build` — atteso: BUILD SUCCESSFUL.
- [ ] **Step 4:** Prova in gioco: scelta di tre divise, "Abito civile", chiusura con Esc, scala GUI 2, 3 e 4.
- [ ] **Step 5:** Commit `feat(wardrobe): schermata dell'armadietto e manichino`.

---

### Task 9: Documentazione, esempi, versione

**Files:**
- Create: `docs/samples/wardrobe/agente_pattuglia.json`, `docs/samples/wardrobe/agente_pattuglia.png` (skin 64x64 di prova generata da uno script usa-e-getta, non committato), `docs/manual-tests/wardrobe.md`, `website/src/content/docs/docs/modules/wardrobe.md`
- Modify: `website/src/content/docs/docs/riferimento/{configurazione,nodi-di-permesso}.md` solo se il Module aggiunge chiavi o nodi (non previsto: verificare con `npm run build`), `website/src/content/docs/docs/riferimento/versioni.md`, indice dei Module nella wiki (file che elenca `bank`/`boss`/`locker`), `gradle.properties` e `qrm-api/src/main/java/dev/qrm/api/QrmVersion.java` → `0.24.0`, `docs/superpowers/specs/2026-10-16-qrm-wardrobe-design.md` (stato: implementata)
- Test: `qrm-wardrobe/src/test/java/dev/qrm/wardrobe/logic/WikiSamplesTest.java` (carica `docs/samples/wardrobe/*.json` con `OutfitCatalog.load` e un `JobLookup` finto: la divisa di esempio deve risultare valida)

**Interfaces:**
- Consumes: `OutfitCatalog.load`.

- [ ] **Step 1:** Scrivere `WikiSamplesTest` (percorso relativo come i test di Bank/Locker; copiare la cartella di esempio in una `@TempDir`) — atteso: FAIL finché l'esempio non esiste.
- [ ] **Step 2:** Creare gli esempi, la pagina wiki (titoli italiani e descrittivi, niente schema fisso, esempio con ```` ```json sample="docs/samples/wardrobe/agente_pattuglia.json" ````) e il manuale di prova `docs/manual-tests/wardrobe.md` (le prove dell'8 della spec, con caselle `[ ]`).
- [ ] **Step 3:** Portare la versione a `0.24.0` nei due file.
- [ ] **Step 4:** `./gradlew build` e `cd website && npm run build` — atteso: entrambi BUILD SUCCESSFUL / Complete, `check-docs` senza errori.
- [ ] **Step 5:** Commit `docs(wardrobe): wiki, esempi, manuale e versione 0.24.0`.

---

## Self-review

- **Copertura della spec:** cartella e formato (Task 2), regole e persistenza (3), ricomposizione (4), punto/eventi/comando (5), rete (6), rendering (7), schermata e manichino (8), documentazione, esempi e versione (9); la verifica dei punti 9.1–9.5 della spec è nelle prove in gioco dei Task 7 e 8.
- **Coerenza dei tipi:** `OutfitDefinition`, `JobLookup`, `Wearer`, `ChooseResult` e `WardrobeService` sono definiti nei Task 2–3 e usati con gli stessi nomi nei Task 5–6; `CatalogPayload.Entry` (6) è quello letto da `ClientOutfits` (7).
- **Review Focus:** 1–2 sono coperti da `OutfitCatalogTest` (Task 2); 3 da `WardrobeServiceTest` (Task 3); 4 da `SkinComposerTest` (Task 4) e dal fallback del Task 7; 5 da `WardrobeWireTest` e dal controllo server del Task 6.
