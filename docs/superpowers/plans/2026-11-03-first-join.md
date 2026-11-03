# Primo arrivo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Un Module `qrm-arrival` che, alla prima selezione di un Character nuovo, lo fa risvegliare a terra in un vicolo, lo rialza con un'animazione e gli dà il kit (100 € in contanti), una sola volta.

**Architecture:** Un Module esterno sul modello di `qrm-wardrobe`: compila contro `qrm-api` e `qrm-client-api`, usa Minecraft/NeoForge direttamente, ascolta `CharacterSelectedEvent`. La logica (chi deve vivere l'arrivo, fasi dell'animazione, kit) sta in `logic/` senza Minecraft e si prova nei test; `mc/` e `client/` la collegano al gioco. Gli scenari sono un'interfaccia con una sola implementazione (il vicolo), estratti a sorte.

**Tech Stack:** Java 25, NeoForge 26.2.0.88, Gson, JUnit 5. Dati per Character in `ModDataService`.

**Spec:** `docs/superpowers/specs/2026-11-03-first-join-design.md`

## Global Constraints

- Il Module compila solo contro `compileOnly project(':qrm-api')` e `compileOnly project(':qrm-client-api')`; il Core non cambia (spec §4). Se serve una funzione nuova in `qrm-api`, estensione compatibile e aumento di `QrmVersion.API_VERSION`.
- Nessuna telecamera o cinematica: solo animazione del personaggio ed effetto a schermo (spec §2).
- Nessun tutorial, obiettivo o indicazione dopo il risveglio (spec §2).
- Un solo scenario (il vicolo); estrazione casuale tra quelli disponibili (spec §3).
- Una sola volta per Character; i Character creati prima dell'installazione risultano già arrivati (spec §4).
- Kit della prima versione: abiti civili e **100 € in contanti**, nient'altro; la carta d'identità è un oggetto futuro e non è nel kit. Il kit è un elenco configurabile (spec §6).
- "Abiti civili" = la skin normale del giocatore: il Module non toglie né dà nulla (wardrobe offre già "Abito civile"). Se in futuro dovessero essere un oggetto, si aggiunge al kit.
- Il personaggio sa chi è: nessun testo parla di amnesia o perdita di memoria (spec §6).
- Kit e contrassegno "arrivo avvenuto" vengono scritti insieme, a risveglio finito; morte o uscita prima = si riparte da capo e non è stato dato nulla (spec §4).
- Punto non impostato, bloccato o in un chunk non caricabile: il giocatore entra come oggi e il log lo segnala (spec §4).
- Testi in italiano e inglese con le stesse chiavi (`LangCoverageTest`).
- Ambiente: non usare `gradlew clean` (cancella gli argomenti di avvio: rigenerarli con `prepareClientRun`/`prepareServerRun`); Gradle va lanciato da `ctx_execute`; se mancano gli agenti, procedere inline. I commit non hanno il trailer di coautore Claude. Versione del mod: portare `qrm_version` in `gradle.properties` da 0.34.0 a 0.35.0 (task 9).

## Review Focus

- Un Character selezionato due volte di fila (il primo selezionato in automatico alla creazione, poi di nuovo dalla GUI): l'arrivo parte una volta sola.
- Il giocatore cambia Character mentre l'animazione è in corso: il blocco si toglie e il Character precedente non risulta arrivato.
- Il server si ferma o il giocatore esce durante l'animazione: nessun contrassegno, nessun kit; alla riconnessione riparte.
- Inventario pieno al momento di dare i 100 €: i contanti non spariscono (cadono a terra) e il contrassegno viene scritto comunque.
- File del kit con un oggetto inesistente o formato rotto: si usa il kit predefinito e il log lo segnala; non blocca l'arrivo.

---

## File Structure

`qrm-arrival/` (nuovo, copia la forma di `qrm-wardrobe`):

- `build.gradle`, `src/main/resources/META-INF/neoforge.mods.toml`, `assets/qrm_arrival/lang/{en_us,it_it}.json`
- `logic/ArrivalStorage.java` e `logic/ModDataArrivalStorage.java`: contrassegno per Character e data di installazione.
- `logic/ArrivalPolicy.java`: decide se un Character deve vivere l'arrivo.
- `logic/Kit.java`, `logic/KitEntry.java`: contenuto del kit e sua lettura da JSON.
- `logic/Scenario.java`, `logic/AlleyScenario.java`, `logic/ScenarioPicker.java`: scenari e sorteggio.
- `logic/ArrivalSession.java`: le fasi dell'animazione, tick per tick.
- `mc/ArrivalMod.java`, `mc/ArrivalServer.java`, `mc/ArrivalFiles.java`, `mc/KitGiver.java`, `mc/ArrivalCommand.java`
- `net/ArrivalNetwork.java`, `net/ArrivalPayloads.java`
- `client/ArrivalClient.java`, `client/ArrivalClientBridge.java`: posa a terra, effetto occhi.

Modificati: `settings.gradle`, `qrm-dev/build.gradle`, `gradle.properties`, `docs/moduli.md`.

---

### Task 1: Scaffold del Module

**Files:**
- Create: `qrm-arrival/build.gradle`, `qrm-arrival/src/main/resources/META-INF/neoforge.mods.toml`, `qrm-arrival/src/main/resources/assets/qrm_arrival/lang/en_us.json`, `.../it_it.json`, `qrm-arrival/src/main/java/dev/qrm/arrival/mc/ArrivalMod.java`
- Modify: `settings.gradle`, `qrm-dev/build.gradle`
- Test: `qrm-arrival/src/test/java/dev/qrm/arrival/logic/LangCoverageTest.java`

**Interfaces:**
- Produces: `ArrivalMod.MOD_ID = "qrm_arrival"`, classe `@Mod(ArrivalMod.MOD_ID)` con costruttore `ArrivalMod(IEventBus modBus)`; `static final Logger LOG`.

- [ ] **Step 1: Committare la specifica** (resta fuori dal commit di codice): `git add docs/superpowers/specs/2026-11-03-first-join-design.md docs/superpowers/plans/2026-11-03-first-join.md` e `git commit -m "docs: specifica e piano del primo arrivo"`.
- [ ] **Step 2: Test che fallisce.** `LangCoverageTest.bothLanguagesDefineTheSameKeys` sul modello di `qrm-wardrobe/.../LangCoverageTest.java` (percorso `src/main/resources/assets/qrm_arrival/lang/`), con `assertTrue(lang("en_us.json").has("qrm_arrival.command.spawn_set"))`.
- [ ] **Step 3: Lanciarlo.** `gradlew :qrm-arrival:test` (da `ctx_execute`). Atteso: errore, il progetto non esiste.
- [ ] **Step 4: Creare il Module.** `build.gradle` e `neoforge.mods.toml` copiati da `qrm-wardrobe` con `qrm_wardrobe`→`qrm_arrival`, `archivesName = 'qrm-arrival'` e descrizione "Modulo primo arrivo di QRM: il risveglio di un Character nuovo". Aggiungere `'qrm-arrival'` a `include` in `settings.gradle` e `runtimeOnly(project(':qrm-arrival')) { transitive = false }` in `qrm-dev/build.gradle`. `ArrivalMod` con il solo costruttore vuoto. Lang con la chiave `qrm_arrival.command.spawn_set` ("Punto di arrivo impostato." / "Arrival point set.").
- [ ] **Step 5: Lanciare il test.** Atteso: PASS. Poi `gradlew :qrm-arrival:compileJava` senza errori.
- [ ] **Step 6: Commit.** `git add qrm-arrival settings.gradle qrm-dev/build.gradle` e `git commit -m "feat(arrival): scheletro del Module"`.

---

### Task 2: Contrassegno per Character e regola dell'arrivo

**Files:**
- Create: `logic/ArrivalStorage.java`, `logic/ModDataArrivalStorage.java`, `logic/ArrivalPolicy.java`
- Test: `logic/ModDataArrivalStorageTest.java`, `logic/ArrivalPolicyTest.java`

**Interfaces:**
- Produces:
  - `interface ArrivalStorage { boolean hasArrived(CharacterId c); void markArrived(CharacterId c); Instant installedAt(); }`
  - `final class ModDataArrivalStorage implements ArrivalStorage` — `ModDataArrivalStorage(ModDataService data, Clock clock)`; namespace `"qrm_arrival"`, chiave `"arrived"` per Character (`OwnerRef.character`); la data di installazione sotto `OwnerRef` di un titolare globale già usato dal progetto, chiave `"installed"`, scritta la prima volta che manca (vedi nota).
  - `final class ArrivalPolicy { ArrivalPolicy(ArrivalStorage s); boolean shouldArrive(PlayerCharacter c); }`
- Nota: se `OwnerRef` non ha un titolare globale adatto, salvare la data in `config/qrm_arrival/installed.txt` (ISO-8601) con `ArrivalFiles`; scelta di chi costruisce.

- [ ] **Step 1: Test.** In `ModDataArrivalStorageTest` (con la finta `MemoryData` di `ModDataWardrobeStorageTest`): `markArrived_then_hasArrived_true`; `unknownCharacter_hasArrived_false`; `installedAt_isWrittenOnceAndStable` (due istanze sullo stesso `MemoryData`, `Clock` che avanza, stessa data). In `ArrivalPolicyTest`: `newCharacterCreatedAfterInstall_shouldArrive`; `characterCreatedBeforeInstall_shouldNot` (`createdAt` < `installedAt`); `alreadyArrived_shouldNot`; `archivedOrDeceasedCharacter_shouldNot` (`CharacterStatus != ACTIVE`).
- [ ] **Step 2: Lanciarli.** `gradlew :qrm-arrival:test`. Atteso: errori di compilazione (classi mancanti).
- [ ] **Step 3: Implementare** `ArrivalStorage`, `ModDataArrivalStorage` (JSON `{"at":"<ISO>"}` per il contrassegno) e `ArrivalPolicy.shouldArrive`: vero solo se `status == ACTIVE`, `!hasArrived` e `createdAt >= installedAt`.
- [ ] **Step 4: Lanciare i test.** Atteso: PASS.
- [ ] **Step 5: Commit.** `feat(arrival): contrassegno per Character e regola dell'arrivo`.

---

### Task 3: Kit configurabile

**Files:**
- Create: `logic/KitEntry.java`, `logic/Kit.java`
- Test: `logic/KitTest.java`

**Interfaces:**
- Produces:
  - `record KitEntry(String itemId, int count)` — `itemId` è un id di oggetto Minecraft (es. `qrm_bank:cash_qrm_10000`), `count` 1..64.
  - `final class Kit { static final String DEFAULT_JSON; static Kit parse(String json) throws IllegalArgumentException; static Kit defaults(); List<KitEntry> entries(); }`
  - `DEFAULT_JSON` = `{"items":[{"item":"qrm_bank:cash_qrm_10000","count":1}]}`: 10000 unità minime = 100,00 nella valuta di esempio `QRM` (2 decimali). Un server con un'altra valuta cambia l'id nel file.

- [ ] **Step 1: Test.** `defaults_hasOneCashEntryOf10000`; `parse_readsItemsAndCounts`; `parse_rejectsCountZeroOrOver64`; `parse_rejectsBlankItem`; `parse_rejectsBrokenJson`; `parse_emptyItemsIsAnEmptyKit`.
- [ ] **Step 2: Lanciarli.** Atteso: FAIL (non compila).
- [ ] **Step 3: Implementare** `Kit.parse` con Gson, lanciando `IllegalArgumentException` con un messaggio leggibile.
- [ ] **Step 4: Lanciare i test.** Atteso: PASS.
- [ ] **Step 5: Commit.** `feat(arrival): kit configurabile`.

---

### Task 4: Scenari e punto di arrivo

**Files:**
- Create: `logic/Scenario.java`, `logic/AlleyScenario.java`, `logic/ScenarioPicker.java`, `logic/ArrivalPoint.java`
- Test: `logic/ScenarioPickerTest.java`, `logic/ArrivalPointTest.java`

**Interfaces:**
- Produces:
  - `record ArrivalPoint(String dimension, double x, double y, double z, float yaw)` con `static ArrivalPoint parse(String json)` e `String toJson()`.
  - `interface Scenario { String id(); Optional<ArrivalPoint> point(); }`
  - `final class AlleyScenario implements Scenario` — `AlleyScenario(Supplier<Optional<ArrivalPoint>> source)`, `id()` = `"alley"`.
  - `final class ScenarioPicker { ScenarioPicker(List<Scenario> all, RandomGenerator random); Optional<Scenario> pick(); }` — sceglie a caso tra gli scenari con `point().isPresent()`; vuoto se nessuno.

- [ ] **Step 1: Test.** `pick_withOneUsableScenario_returnsIt`; `pick_skipsScenarioWithoutPoint`; `pick_noUsable_returnsEmpty`; `pick_withTwoUsable_returnsBothOverManyDraws` (random con seme fisso, 100 estrazioni); `arrivalPoint_roundTripsThroughJson`; `arrivalPoint_parseRejectsMissingFields`.
- [ ] **Step 2: Lanciarli.** Atteso: FAIL.
- [ ] **Step 3: Implementare.** `ArrivalPoint` con Gson; il sorteggio filtra e poi usa `random.nextInt(n)`.
- [ ] **Step 4: Lanciare i test.** Atteso: PASS.
- [ ] **Step 5: Commit.** `feat(arrival): scenari, sorteggio e punto di arrivo`.

---

### Task 5: Fasi dell'animazione

**Files:**
- Create: `logic/ArrivalSession.java`
- Test: `logic/ArrivalSessionTest.java`

**Interfaces:**
- Produces: `final class ArrivalSession` — `ArrivalSession(int lyingTicks, int risingTicks, int maxLockTicks)`; `enum Phase { LYING, RISING, DONE, ABORTED }`; `Phase tick()` (avanza di un tick e torna la fase corrente); `Phase phase()`; `int elapsed()`; `boolean locksPlayer()` (vero in LYING e RISING); `void abort()`; `boolean completed()` (vero solo in DONE).
- Valori di partenza consigliati (configurabili in `ArrivalFiles`, task 7): 60 tick a terra, 40 tick per rialzarsi, 400 tick di blocco massimo.

- [ ] **Step 1: Test.** `startsInLying_locked`; `afterLyingTicks_becomesRising`; `afterLyingPlusRising_becomesDone_unlocked_completed`; `abort_neverCompletes_andUnlocks`; `exceedingMaxLock_forcesDoneWithoutCompleted` (con `maxLockTicks` minore della durata: sblocca ma `completed()` è falso); `tickAfterDone_staysDone`.
- [ ] **Step 2: Lanciarli.** Atteso: FAIL.
- [ ] **Step 3: Implementare** come macchina a stati sul contatore dei tick.
- [ ] **Step 4: Lanciare i test.** Atteso: PASS.
- [ ] **Step 5: Commit.** `feat(arrival): fasi dell'animazione`.

---

### Task 6: Prova di fattibilità della posa a terra (spike, scartabile)

Il progetto non ha una posa "a terra / che si alza" (spec §5) e la spec ipotizza di riusare una posa di gioco esistente. Va verificato prima di costruirci sopra.

**Files:**
- Create (temporaneo, non committato): una classe client in `qrm-arrival/src/main/java/dev/qrm/arrival/client/` e un comando di prova.

- [ ] **Step 1: Provare, in questo ordine, finché una funziona:** (a) posa di gioco `Pose.SLEEPING` impostata in modo che il server non la riporti subito in piedi e i client la vedano; (b) `Pose.SWIMMING`; (c) rotazione del modello del giocatore in un evento di rendering client (come fa `qrm-immersion` per le braccia, ma sul corpo) guidata da un valore sincronizzato.
- [ ] **Step 2: Criteri.** Il giocatore deve sembrare disteso, restare tale per l'intera fase, tornare in piedi senza scatti ed essere visto così anche da un secondo client; la visuale in prima persona del giocatore stesso deve restare bassa, vicina al suolo.
- [ ] **Step 3: Scrivere l'esito** in `docs/superpowers/specs/2026-11-03-first-join-design.md` (sezione 4, voce "Effetto degli occhi" accanto), con il metodo scelto e quello scartato. Se nessuno dei tre regge, fermarsi e tornare dal committente: cambia la spec.
- [ ] **Step 4: Eliminare il codice di prova** e committare solo la nota nella spec: `docs: esito della prova sulla posa a terra`.

---

### Task 7: Logica sul server

**Files:**
- Create: `mc/ArrivalFiles.java`, `mc/KitGiver.java`, `mc/ArrivalServer.java`, `mc/ArrivalCommand.java`
- Modify: `mc/ArrivalMod.java`, lang `en_us.json`/`it_it.json`
- Test: `logic/ArrivalFilesTest.java` (la parte di sola lettura di file)

**Interfaces:**
- Consumes: `ArrivalPolicy.shouldArrive(PlayerCharacter)`, `ArrivalStorage.markArrived(CharacterId)`, `Kit`, `ScenarioPicker.pick()`, `ArrivalSession`, `ArrivalPoint`.
- Produces:
  - `ArrivalFiles`: cartella `config/qrm_arrival/` con `kit.json` (creato con `Kit.DEFAULT_JSON` se manca; se illeggibile usa `Kit.defaults()` e scrive il motivo nel log), `spawn.json` (un `ArrivalPoint`) e `timing.json` (`lyingTicks`, `risingTicks`, `maxLockTicks`). `static ArrivalFiles forServer(Path serverDir)`; `Kit kit()`; `Optional<ArrivalPoint> spawn()`; `void saveSpawn(ArrivalPoint p)`.
  - `KitGiver`: `static void give(ServerPlayer player, Kit kit)` risolve ogni `itemId` con `BuiltInRegistries.ITEM`; un id sconosciuto viene saltato con un avviso nel log; ciò che non entra nell'inventario cade a terra (`player.drop(stack, false)`).
  - `ArrivalServer`: `onSelected(PlayerCharacter c)`, `onTick(ServerPlayer p)` (avanza la `ArrivalSession` del giocatore e invia la fase ai client), `forget(UUID player)` (uscita o cambio Character: `abort()`), `reset()`.
  - `/qrm arrival setspawn` (salva la posizione e la direzione di chi lo lancia, permesso `Permissions.COMMANDS_GAMEMASTER` come in `WardrobeCommand`) e `/qrm arrival reload`.

- [ ] **Step 1: Test** per `ArrivalFiles`: `missingKitFile_isCreatedWithDefault`; `brokenKitFile_fallsBackToDefaultKit`; `spawn_roundTripsThroughFile`; `timingFile_missingKeysUseDefaults`.
- [ ] **Step 2: Lanciarli.** Atteso: FAIL.
- [ ] **Step 3: Implementare `ArrivalFiles`.** Atteso dopo: i test passano.
- [ ] **Step 4: Implementare `ArrivalServer.onSelected`:** se `shouldArrive` è falso, non fare nulla; altrimenti `ScenarioPicker.pick()`; se vuoto, scrivere nel log "nessun punto di arrivo impostato" e uscire senza errori; teletrasportare il giocatore al punto (se la dimensione non esiste o il chunk non si carica, log e uscita senza errori) e creare una `ArrivalSession`. Due selezioni di fila dello stesso Character non devono creare due sessioni (la seconda è ignorata se la prima è attiva).
- [ ] **Step 5: Implementare il blocco** durante `locksPlayer()`: annullare movimento, attacco e uso di oggetti con gli stessi eventi usati da `ImmersionEvents` per `HANDS_UP` (`PlayerInteractEvent.*`, `LivingIncomingDamageEvent` solo se serve); a `DONE` con `completed()`: `KitGiver.give`, `ArrivalStorage.markArrived`, togliere il blocco. A `ABORTED` o oltre il blocco massimo: nessun kit, nessun contrassegno.
- [ ] **Step 6: Agganciare in `ArrivalMod`:** `CharacterSelectedEvent` → `onSelected`; `NeoForge.EVENT_BUS` per `PlayerTickEvent.Post`, `PlayerLoggedOutEvent`, `LivingDeathEvent` (→ `forget`), `ServerStoppedEvent` (→ `reset`), `RegisterCommandsEvent`. Aggiungere le chiavi lang del comando e dei log leggibili dallo staff, identiche nelle due lingue.
- [ ] **Step 7: Lanciare** `gradlew :qrm-arrival:test` e `gradlew :qrm-arrival:compileJava`. Atteso: PASS.
- [ ] **Step 8: Commit.** `feat(arrival): arrivo sul server, kit e comandi`.

---

### Task 8: Client e rete

**Files:**
- Create: `net/ArrivalNetwork.java`, `net/ArrivalPayloads.java`, `client/ArrivalClient.java`, `client/ArrivalClientBridge.java`
- Modify: `mc/ArrivalMod.java`, `mc/ArrivalServer.java`

**Interfaces:**
- Consumes: il metodo scelto dalla prova del task 6; `ArrivalSession.Phase`.
- Produces: `ArrivalPayloads.PhaseUpdate(int entityId, int phase, int elapsed)` (server → client, canale opzionale come `ImmersionNetwork`: `registrar(PROTOCOL_VERSION).optional()`, lambda che caricano il ponte client solo all'arrivo del pacchetto).

- [ ] **Step 1: Pacchetto e registrazione** sul modello di `ImmersionPayloads`/`ImmersionNetwork`, `PROTOCOL_VERSION = "1"`.
- [ ] **Step 2: Posa a terra e rialzo** sul client con il metodo deciso al task 6, per tutti i giocatori visti dal client; il progresso del rialzo segue `elapsed`.
- [ ] **Step 3: Effetto occhi** solo per il giocatore stesso: velo scuro con sfocatura che si schiarisce da inizio LYING a fine RISING (usare il livello di overlay di NeoForge, senza muovere la camera). Un client senza il Module vede il giocatore normale e non riceve il velo (canale opzionale).
- [ ] **Step 4: Verifica in gioco**, a mano con due client: avviare `runServer` e `runClient` (rigenerare con `prepareServerRun`/`prepareClientRun` se mancano gli argomenti), creare un Character nuovo, impostare il punto con `/qrm arrival setspawn`, selezionare. Atteso: risveglio nel vicolo, secondo client lo vede disteso e poi in piedi, 100 € nell'inventario a fine animazione, nessun secondo arrivo alla riselezione. Scrivere i passi in `docs/manual-tests/arrival.md` sul modello di `docs/manual-tests/wardrobe.md`.
- [ ] **Step 5: Commit.** `feat(arrival): posa a terra, rialzo ed effetto occhi`.

---

### Task 9: Versione e documentazione

**Files:**
- Modify: `gradle.properties` (`qrm_version=0.35.0`), `docs/moduli.md` (conteggio moduli 12 → 13 e riga di `qrm-arrival`), `docs/comandi.md` (i due comandi)
- Create: pagina wiki in `website/` sul modello della pagina di wardrobe (italiano, concetti senza schema fisso di titoli, solo "Sperimentale" come etichetta)

- [ ] **Step 1: Portare** `qrm_version` a `0.35.0`; lasciare `API_VERSION` invariata se `qrm-api` non è stata toccata, altrimenti aumentarla.
- [ ] **Step 2: Aggiornare** `docs/moduli.md` e `docs/comandi.md`; scrivere la pagina wiki: cos'è l'arrivo, come impostare il punto, il file del kit, i personaggi già esistenti che non lo vivono.
- [ ] **Step 3: Eseguire** la build del sito che contiene `check-docs` (come descritto in `website/`) e la suite `gradlew test`. Atteso: PASS.
- [ ] **Step 4: Commit.** `docs(arrival): versione 0.35.0, moduli, comandi e wiki`.
