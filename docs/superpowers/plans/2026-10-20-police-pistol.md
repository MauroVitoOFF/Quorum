# Pistola d'ordinanza di QRM Police — piano di implementazione

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Una pistola d'ordinanza giocabile in `qrm-police`: caricatori fisici, ricarica a istanti decisi dal server, mira, suggerimenti vicino all'arma, animazioni in prima persona esportate dal `.bbmodel`.

**Architecture:** Regole pure (stato dell'arma, caricatori, sessioni d'azione, fasce, balistica, valutatore delle animazioni) testate con JUnit senza Minecraft. Sopra, il collegamento NeoForge in `qrm-police`: oggetti con data component, rete, sessioni per giocatore nel tick del server, punto armeria. Sul client: riproduttore delle animazioni, rendering per parte in `RenderHandEvent`, input, suggerimenti, effetti. Gli asset nascono dal `.bbmodel` tramite un esportatore Python.

**Tech Stack:** Java 25, NeoForge 26.2.0.88, Minecraft 26.2, JUnit 5.11, Python 3 + Pillow per gli strumenti, Blockbench 5.2.2.

**Spec:** `docs/superpowers/specs/2026-10-20-police-pistol-design.md`

## Global Constraints

- Versione di arrivo `0.29.0` in `gradle.properties` (`qrm_version`) e `qrm-api/.../QrmVersion.java` (`API_VERSION`): cambia `qrm-api` (Task C1).
- Codice in `qrm-police`, modId `qrm_police`, pacchetti `dev.qrm.police.weapon.{logic,mc,net,client}`; `qrm-police` resta `compileOnly` su `qrm-api` e `qrm-client-api`, nessuna dipendenza da `qrm-medical`.
- Asset nel namespace `qrm_police` (`qrm-police/src/main/resources/assets/qrm_police/`); gli asset della pistola in `qrm-neoforge/.../assets/qrm/` vengono rimossi (proposta per Q6a, vedi Handoff).
- Oggetti: `qrm_police:police_pistol` (stacksTo 1), `qrm_police:police_magazine` (stacksTo 1, perché ogni caricatore ha un id; vedi Handoff per Q4e).
- Nessun controllo di servizio, ruolo o permesso su impugnare, sparare, ricaricare (D0).
- Valori della specifica: capienza 17; danno 4.0 a colpo; intervallo minimo tra i colpi 5 tick (0,25 s); soglia pressione breve/tenuta 6 tick (0,3 s); transizione di mira 4 tick (0,2 s); FOV in mira ×0,85.
- Valori proposti qui (Q5a, da confermare in Handoff): testa ×1,5; gittata 64 blocchi; stesso danno alle creature; dispersione dal fianco 3,0°, in mira 0,5°.
- Tasti predefiniti (Q2d): ricarica `R`, interazione `V` (liberi tra quelli già usati da QRM: B, C, G, H, K, O, X, F7, Alt sinistro); mira = tasto "usa" (destro), sparo = tasto "attacca" (sinistro).
- Testi: concetti in italiano; `it_it` e `en_us` con le stesse chiavi.
- Server autoritativo: il client non decide mai munizioni, colpi a segno o danno.
- Gradle: mai `clean`; dopo modifiche ai run usare `prepareClientRun`/`prepareServerRun`.
- Commit in italiano (`feat(police): …`), senza trailer di co-autore.

## Review Focus

1. Una ricarica che inizia e il caricatore di scorta scelto viene buttato a terra o spostato prima di `mag_in`: la pistola resta senza caricatore, nessun colpo creato o perso (test in Task B4).
2. Il giocatore cambia slot e torna subito sulla pistola durante la ricarica: la sessione è già annullata, nessun istante successivo viene applicato a un oggetto diverso (test in Task B4 con `sameWeapon=false` per un tick).
3. Il caricatore appena tolto è il più pieno dell'inventario: non viene reinserito nella stessa ricarica (test in Task B4).
4. Un client modificato che manda `FIRE` a raffica o durante una ricarica: il server rispetta i 5 tick e rifiuta lo sparo durante la ricarica (test in Task B3/B4 su `PistolRules`/`ActionSession`; verifica manuale in Task G1).
5. Inventario pieno al momento di `mag_out`: il caricatore tolto cade a terra vicino al giocatore, non sparisce (test in Task B4 con inventario finto pieno).

---

## Parte A — Pipeline degli asset (dal `.bbmodel`)

### Task A1: Esportatore di geometria e rig

**Files:**
- Create: `tools/pistol_export.py`
- Create: `tools/test_pistol_export.py`
- Delete: `tools/police_pistol_model.py`, `tools/police_pistol_texture.py`, `tools/police_pistol_bbmodel.py`
- Modify: `tools/police_pistol_check.py` (`ASSETS = Path("qrm-police/src/main/resources/assets/qrm_police")`)

**Interfaces:**
- Produces: `python tools/pistol_export.py [design/police-pistol/police_pistol.bbmodel]` scrive in `assets/qrm_police/`: `models/item/police_pistol.json`, `models/item/police_pistol_parts/<bone>.json`, `rigs/police_pistol.json` (stesso schema dell'attuale rig), `textures/item/police_pistol.png` (dalla texture incorporata).

- [ ] **Step 1:** Scrivere `test_pistol_export.py::test_head_bbmodel_matches_head_assets`: esporta il `.bbmodel` di `HEAD` (via `git show HEAD:design/police-pistol/police_pistol.bbmodel`) in una cartella temporanea e confronta con gli asset di `HEAD` in `qrm-neoforge/.../assets/qrm/`: stessi cubi (`from`, `to`, `rotation`, facce/UV) per ogni parte, stessi bone, `parent` e `pivot` nel rig.
- [ ] **Step 2:** `python -I -m pytest tools/test_pistol_export.py -v` — atteso: FAIL (modulo inesistente).
- [ ] **Step 3:** Implementare l'esportatore: outliner → gerarchia dei bone (pivot = `origin` del gruppo); un modello di parte per ogni gruppo con cubi propri; `display` copiato dal modello attuale; errore esplicito se un cubo ha rotazione non ammessa dai modelli Java (un solo asse, multipli di 22,5°).
- [ ] **Step 4:** Rieseguire il test — atteso: PASS. Poi `python tools/pistol_export.py && python tools/police_pistol_check.py` — atteso: nessun errore.
- [ ] **Step 5:** Commit `feat(police): esportatore della pistola dal .bbmodel`.

### Task A2: Esportatore delle animazioni ed eventi in Blockbench

**Files:**
- Modify: `tools/pistol_export.py`, `tools/test_pistol_export.py`
- Create: `tools/pistol_migrate_events.py` (uso una tantum)
- Delete: `tools/police_pistol_anim.py`, `tools/police_pistol_pose.py`, `tools/police_pistol_preview.py`
- Modify: `design/police-pistol/police_pistol.bbmodel` (eventi nel canale "timeline" dell'animatore effetti)

**Interfaces:**
- Produces: `assets/qrm_police/animations/police_pistol.json`, formato `qrm.anim/2`: `{format, animations: {<clip>: {length, loop, bones: {<bone>: {position|rotation|scale: [{t, v:[x,y,z], interp}]}}, events: [{t, type, ...}]}}}` con `interp` ∈ `linear | step | catmullrom`.
- Produces: eventi come istruzioni di timeline Blockbench, una per riga: `sound <id>`, `commit <mag_out|mag_in|slide|check|inspect_mag>`, `muzzle_flash`, `eject_shell`, `camera_kick <pitch> <yaw>`, `arm <pose>`.

- [ ] **Step 1:** Test `test_animation_keys_and_events`: dato un `.bbmodel` minimo scritto nel test (un bone, keyframe `linear`/`step`/`catmullrom`, istruzioni `sound pistol.fire` e `commit mag_out`), l'esportazione produce i keyframe e gli eventi attesi con i tempi esatti; un keyframe `bezier` fa fallire l'esportazione con il nome della clip.
- [ ] **Step 2:** Eseguire — atteso: FAIL.
- [ ] **Step 3:** Implementare l'esportazione delle animazioni. Scrivere `pistol_migrate_events.py`: copia gli eventi dell'attuale `qrm.anim/1` nelle istruzioni di timeline del `.bbmodel`, e aggiunge `commit mag_out`/`commit mag_in` ai tempi dei suoni `pistol.mag_out`/`pistol.mag_in` delle due ricariche e `commit slide` al suono `pistol.slide_forward` di `reload_empty`.
- [ ] **Step 4:** Test — atteso: PASS. Eseguire la migrazione una volta, poi l'esportazione completa.
- [ ] **Step 5:** Commit `feat(police): animazioni ed eventi esportati dal .bbmodel`.

### Task A3: Spostamento degli asset nel namespace di Police

**Files:**
- Delete: `qrm-neoforge/src/main/resources/assets/qrm/{animations,rigs}/police_pistol.json`, `.../models/item/police_pistol.json`, `.../models/item/police_pistol_parts/`, `.../items/police_pistol.json`, `.../textures/item/police_pistol.png`, `tools/qrm-neoforge/` (copia orfana)
- Create: (generati da A1/A2) sotto `qrm-police/src/main/resources/assets/qrm_police/`

- [ ] **Step 1:** `grep -rn "qrm:item/police_pistol\|assets/qrm/rigs" --include=*.java --include=*.json .` — atteso: nessun uso fuori dai file rimossi.
- [ ] **Step 2:** Rimuovere, rigenerare con `python tools/pistol_export.py`, eseguire `police_pistol_check.py` — atteso: nessun errore.
- [ ] **Step 3:** Commit `refactor(police): asset della pistola nel namespace qrm_police`.

---

## Parte B — Regole pure (JUnit, senza Minecraft)

Tutti i file in `qrm-police/src/main/java/dev/qrm/police/weapon/logic/`, i test in `qrm-police/src/test/java/dev/qrm/police/weapon/logic/`. Verifica: `./gradlew :qrm-police:test`.

### Task B1: Stato dell'arma, caricatori e fasce

**Files:**
- Create: `MagazineState.java`, `WeaponState.java`, `AmmoBand.java`
- Test: `AmmoBandTest.java`, `MagazineStateTest.java`

**Interfaces:**
- Produces: `record MagazineState(UUID id, int rounds, int capacity, Set<String> knownBy)` con `withRounds(int)`, `knownTo(String characterId)`, `boolean isKnownBy(String characterId)`, `static MagazineState full(UUID id, int capacity)`.
- Produces: `record WeaponState(UUID id, Optional<MagazineState> magazine, boolean chambered, boolean slideLocked)`, `static WeaponState empty(UUID id)`.
- Produces: `enum AmmoBand { FULL, MORE_THAN_HALF, LESS_THAN_HALF, ALMOST_EMPTY, EMPTY; static AmmoBand of(int rounds, int capacity) }`.

- [ ] **Step 1:** `AmmoBandTest`: con capienza 17 → 17 `FULL`; 16 e 9 `MORE_THAN_HALF`; 8 e 4 `LESS_THAN_HALF`; 3 e 1 `ALMOST_EMPTY`; 0 `EMPTY`. Regola: `ALMOST_EMPTY` se `1 ≤ rounds ≤ max(1, capacity/5)`. `MagazineStateTest`: `rounds` fuori da `0..capacity` → `IllegalArgumentException`; `knownTo` non modifica l'originale.
- [ ] **Step 2:** Eseguire — atteso: FAIL. **Step 3:** Implementare. **Step 4:** Eseguire — atteso: PASS.
- [ ] **Step 5:** Commit `feat(police): stato dell'arma e fasce di munizioni`.

### Task B2: Regole della pistola (sparo, carrello, suggerimenti)

**Files:**
- Create: `PistolRules.java`, `FireResult.java`, `Hint.java`, `SlideAction.java`
- Test: `PistolRulesTest.java`

**Interfaces:**
- Produces: `record FireResult(boolean fired, WeaponState state)`; `PistolRules.fire(WeaponState) -> FireResult`.
- Produces: `enum SlideAction { RELEASE, RACK, NONE }`; `PistolRules.slideActionFor(WeaponState) -> SlideAction`; `PistolRules.applySlide(WeaponState) -> WeaponState`.
- Produces: `enum Hint { RELEASE_SLIDE, RACK_SLIDE, RELOAD, NO_SPARE, NONE }`; `PistolRules.hint(WeaponState, boolean hasLoadedSpare) -> Hint`.
- Produces: `PistolRules.magOut(WeaponState) -> WeaponState` (senza caricatore), `PistolRules.magIn(WeaponState, MagazineState) -> WeaponState`.

- [ ] **Step 1:** Test con queste attese esatte:
  - colpo in canna e caricatore con 5 → sparato, caricatore 4, camera piena, carrello chiuso;
  - colpo in canna e caricatore con 0 → sparato, camera vuota, `slideLocked = true`;
  - colpo in canna senza caricatore → sparato, camera vuota, carrello **chiuso**;
  - camera vuota → `fired = false`, stato invariato;
  - `slideLocked` e caricatore con 17 → `RELEASE`, dopo `applySlide` caricatore 16 e camera piena;
  - carrello chiuso, camera vuota, caricatore con 3 → `RACK`, dopo `applySlide` caricatore 2 e camera piena;
  - camera piena → `NONE`;
  - `slideLocked` senza caricatore → `RELEASE`, dopo `applySlide` carrello chiuso e camera vuota;
  - `hint`: carrello aperto con caricatore che ha colpi → `RELEASE_SLIDE`; carrello chiuso, camera vuota, caricatore che ha colpi → `RACK_SLIDE`; senza caricatore o caricatore vuoto con scorta → `RELOAD`, senza scorta → `NO_SPARE`; camera piena → `NONE`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementare (funzioni pure, nessuno stato statico). **Step 4:** PASS.
- [ ] **Step 5:** Commit `feat(police): regole di sparo e carrello`.

### Task B3: Tempi delle azioni

**Files:**
- Create: `PistolAction.java`, `CommitPoint.java`, `PistolTimings.java`
- Test: `PistolTimingsTest.java`

**Interfaces:**
- Produces: `enum CommitPoint { MAG_OUT, MAG_IN, SLIDE, CHECK, INSPECT_MAG }`.
- Produces: `enum PistolAction { DRAW, RELOAD_TACTICAL, RELOAD_EMPTY, SLIDE_RELEASE, SLIDE_RACK, AMMO_CHECK, INSPECT, INSPECT_MAG }` con `String clip()`, `int durationTicks()`, `List<Map.Entry<Integer, CommitPoint>> commits()` e `boolean blocksFire()` (vero per `DRAW`, ricariche e carrello).
- Produces: `PistolTimings.FIRE_COOLDOWN_TICKS = 5`, `HOLD_THRESHOLD_TICKS = 6`, `AIM_TICKS = 4`; `PistolTimings.canFire(long lastShotTick, long nowTick) -> boolean`.

- [ ] **Step 1:** `PistolTimingsTest`: `canFire(100, 104)` falso e `canFire(100, 105)` vero; `blocksFire()` vero per `DRAW`, ricariche e carrello, falso per `AMMO_CHECK`, `INSPECT`, `INSPECT_MAG` (Review Focus 4). `timingsMatchExportedAnimation`: legge `src/main/resources/assets/qrm_police/animations/police_pistol.json` con Gson; per ogni `PistolAction` con clip esportata, durata = `ceil(length × 20)` e ogni commit al tick `round(t × 20)` dell'evento `commit` corrispondente. Le clip non ancora presenti (Parte F) sono saltate con `assumeTrue`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementare i valori dal file esportato (tattica: 50 tick, `MAG_OUT` 16, `MAG_IN` 32). **Step 4:** PASS.
- [ ] **Step 5:** Commit `feat(police): tempi delle azioni della pistola`.

### Task B4: Sessione d'azione e invarianti delle munizioni

**Files:**
- Create: `MagazineInventory.java`, `ActionSession.java`, `SessionOutcome.java`
- Test: `ActionSessionTest.java`, `AmmoConservationTest.java`, `FakeInventory.java` (test)

**Interfaces:**
- Consumes: B1–B3.
- Produces: `interface MagazineInventory { Optional<MagazineState> takeFullest(UUID excluding); boolean store(MagazineState m); void drop(MagazineState m); }` (`store` restituisce `false` se l'inventario è pieno).
- Produces: `final class ActionSession` con `static Optional<ActionSession> start(PistolAction, WeaponState, MagazineInventory, String characterId)` (vuoto se l'azione non è possibile ora, per esempio ricarica senza scorta carica), `static ActionSession inspectMagazine(MagazineState inHand, String characterId)` per `INSPECT_MAG`, `SessionOutcome tick(boolean sameWeapon, boolean incapacitated)`, `WeaponState state()`, `Optional<MagazineState> inspectedMagazine()`.
- Produces: `record SessionOutcome(Status status, WeaponState state, Optional<AmmoBand> revealedBand)`, con `enum Status { RUNNING, DONE, CANCELLED }`.

- [ ] **Step 1:** `ActionSessionTest`:
  - tattica completa: al tick 16 il caricatore vecchio è in inventario, al tick 32 entra il più pieno, al tick 50 `DONE`;
  - annullata al tick 20 (`sameWeapon=false`) → pistola senza caricatore, camera invariata;
  - a vuoto annullata dopo `MAG_IN` → carrello aperto, caricatore inserito, camera vuota;
  - `incapacitated=true` annulla come `sameWeapon=false`;
  - il caricatore tolto non viene reinserito anche se è il più pieno (Review Focus 3);
  - nessuna scorta carica al tick 32 → resta senza caricatore (Review Focus 1);
  - `store` restituisce `false` → `drop` chiamato con quel caricatore (Review Focus 5);
  - `AMMO_CHECK` al commit `CHECK` marca il caricatore inserito come noto al personaggio e restituisce la fascia;
  - `inspectMagazine` al commit `INSPECT_MAG` restituisce il caricatore in mano marcato noto, con lo stesso numero di colpi.
- [ ] **Step 2:** `AmmoConservationTest.randomSequencesNeverCreateOrDestroyRounds`: 10 000 sequenze casuali con seme fisso (avvio di azioni, spari tramite `PistolRules.fire`, annullamenti a tick casuali) → `colpi totali iniziali = colpi totali finali + colpi sparati`, contando pistola, inventario e caricatori lasciati a terra.
- [ ] **Step 3:** FAIL. **Step 4:** Implementare. **Step 5:** PASS.
- [ ] **Step 6:** Commit `feat(police): sessioni d'azione con conservazione delle munizioni`.

### Task B5: Balistica e armeria

**Files:**
- Create: `Ballistics.java`, `ArmoryRules.java`
- Test: `BallisticsTest.java`, `ArmoryRulesTest.java`

**Interfaces:**
- Produces: `Ballistics.damage(boolean headshot) -> float` (4.0 o 6.0); `Ballistics.spreadDegrees(boolean aiming) -> double` (3.0 o 0.5); `Ballistics.deviate(double yaw, double pitch, boolean aiming, RandomGenerator rnd) -> double[]` (yaw, pitch entro il cono); `Ballistics.RANGE = 64.0`.
- Produces: `ArmoryRules.restock(List<MagazineState> carried, int target, int capacity, String characterId, Supplier<UUID> ids) -> List<MagazineState>`: riempie tutti a `capacity`, li marca noti, aggiunge caricatori nuovi pieni fino a `target`. Proposta: `target = 3`.

- [ ] **Step 1:** Test: 5 colpi da 4.0 portano 20 PV a 0 e 4 no; deviazione sempre entro il cono su 10 000 estrazioni; `restock` con 1 caricatore da 5 e target 3 → 3 caricatori da 17 tutti noti; con 4 caricatori → nessuno tolto.
- [ ] **Step 2:** FAIL. **Step 3:** Implementare. **Step 4:** PASS.
- [ ] **Step 5:** Commit `feat(police): balistica e regole dell'armeria`.

### Task B6: Valutatore delle animazioni

**Files:**
- Create: `qrm-police/src/main/java/dev/qrm/police/weapon/logic/anim/{AnimClip,AnimChannel,AnimKey,BoneTransform,AnimEvaluator,AnimFormat}.java`
- Test: `qrm-police/src/test/java/dev/qrm/police/weapon/logic/anim/AnimEvaluatorTest.java`

**Interfaces:**
- Produces: `AnimFormat.parse(Reader json) -> Map<String, AnimClip>` (`qrm.anim/2`); `record AnimEvent(double t, String type, List<String> args)`; `AnimClip.events()`.
- Produces: `AnimEvaluator.sample(AnimClip, double t) -> Map<String, BoneTransform>`; `record BoneTransform(float[] position, float[] rotationDeg, float[] scale)` con `REST`.
- Produces: `AnimEvaluator.blend(Map<String,BoneTransform> a, Map<String,BoneTransform> b, float w)`.

- [ ] **Step 1:** Test: `linear` a metà tra 0 e 10 → 5; `step` resta al valore precedente fino al keyframe; `catmullrom` passa esattamente per i keyframe e a metà tra (0,0) (1,10) (2,10) (3,0) vale 11,25; prima del primo keyframe → riposo; dopo l'ultimo → ultimo valore; canale assente → riposo; `blend` con w=0,5 a metà.
- [ ] **Step 2:** FAIL. **Step 3:** Implementare (Catmull-Rom uniforme come in Blockbench). **Step 4:** PASS.
- [ ] **Step 5:** Commit `feat(police): valutatore delle animazioni qrm.anim/2`.

---

## Parte C — Server (NeoForge)

### Task C1: Stato "a terra" in `qrm-api`

**Files:**
- Create: `qrm-api/src/main/java/dev/qrm/api/emergency/IncapacitationRegistry.java`
- Create: `qrm-core/src/main/java/dev/qrm/core/emergency/InMemoryIncapacitationRegistry.java`
- Test: `qrm-core/src/test/java/dev/qrm/core/emergency/InMemoryIncapacitationRegistryTest.java`
- Modify: `qrm-neoforge/src/main/java/dev/qrm/neoforge/QrmMod.java` (registrazione accanto a `POINT_TYPES`), `qrm-medical/src/main/java/dev/qrm/medical/mc/MedicalMod.java`, `gradle.properties`, `qrm-api/src/main/java/dev/qrm/api/QrmVersion.java`

**Interfaces:**
- Produces: `interface IncapacitationRegistry { void register(String moduleId, Predicate<UUID> check); boolean isIncapacitated(UUID playerId); }`.

- [ ] **Step 1:** Test: nessun controllo registrato → `false`; un controllo vero per un UUID → `true` solo per quello; due registrazioni con lo stesso `moduleId` → `IllegalArgumentException`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementare; registrare in `QrmMod`; in `MedicalMod` registrare un controllo che usa `MedicalServer.incapacitated` sul giocatore online con quell'UUID. Portare la versione a `0.29.0`.
- [ ] **Step 4:** `./gradlew :qrm-core:test :qrm-medical:build :qrm-neoforge:build` — atteso: BUILD SUCCESSFUL.
- [ ] **Step 5:** Commit `feat(api): registro dello stato a terra (0.29.0)`.

### Task C2: Oggetti, data component, tipo di danno

**Files:**
- Create: `qrm-police/src/main/java/dev/qrm/police/weapon/mc/{WeaponComponents,PistolItem,MagazineItem,WeaponCodecs}.java`
- Modify: `qrm-police/src/main/java/dev/qrm/police/mc/{PoliceItems,PoliceMod}.java`, `assets/qrm_police/lang/{it_it,en_us}.json`
- Create: `assets/qrm_police/items/{police_pistol,police_magazine}.json`, `models/item/police_magazine.json`, `data/qrm_police/damage_type/pistol.json`, `data/minecraft/tags/damage_type/is_projectile.json`
- Test: `qrm-police/src/test/java/dev/qrm/police/LangCoverageTest.java` (se non esiste, sullo schema di quello del Locker)

**Interfaces:**
- Produces: `WeaponComponents.PISTOL_STATE : DataComponentType<WeaponState>`, `WeaponComponents.MAGAZINE : DataComponentType<MagazineState>` (codec persistente + `StreamCodec` di rete), registrati con `DeferredRegister.DataComponents`.
- Produces: `PoliceItems.PISTOL`, `PoliceItems.MAGAZINE`; un nuovo oggetto nasce con `WeaponState.empty(randomUUID)` e `MagazineState.full(randomUUID, 17)`.
- Produces: `PistolDamage.source(ServerLevel, Player shooter) -> DamageSource` di tipo `qrm_police:pistol`, nel tag `minecraft:is_projectile`.

- [ ] **Step 1:** Il test delle lingue con le nuove chiavi (nomi degli oggetti, suggerimenti, fasce, tasti) — FAIL.
- [ ] **Step 2:** Implementare. Tooltip del caricatore: fascia solo se `isKnownBy(personaggio del client)` (Task D5 fornisce l'id); nessuna barra. Scheda creativa Combattimento.
- [ ] **Step 3:** `./gradlew :qrm-police:test :qrm-police:build` — PASS.
- [ ] **Step 4:** Commit `feat(police): pistola e caricatore come oggetti`.

### Task C3: Rete della pistola

**Files:**
- Create: `qrm-police/src/main/java/dev/qrm/police/weapon/net/PistolPayloads.java`
- Modify: `qrm-police/src/main/java/dev/qrm/police/net/PoliceNetwork.java` (`PROTOCOL_VERSION = "3"`)

**Interfaces:**
- Produces, client → server: `Input(Kind kind)` con `enum Kind { FIRE, AIM_ON, AIM_OFF, RELOAD, INTERACT_TAP, INTERACT_HOLD }`.
- Produces, server → client:
  - `ActionStarted(int entityId, PistolAction action)` a chi osserva e a sé;
  - `ActionEnded(int entityId, boolean cancelled)`;
  - `Shot(int entityId, Vec3 from, Vec3 to, Optional<BlockPos> block, boolean hitEntity)` a chi osserva;
  - `Aim(int entityId, boolean aiming)`;
  - `Rejected(Kind kind)` a sé;
  - `Identity(String characterId)` a sé.

- [ ] **Step 1:** Implementare i payload sullo schema di `PolicePayloads`; i gestori lato server chiamano `PistolServer.onInput(ServerPlayer, Kind)` (Task C4), quelli lato client un ponte `PistolClientBridge`, caricato solo all'arrivo del pacchetto.
- [ ] **Step 2:** `./gradlew :qrm-police:build` — PASS.
- [ ] **Step 3:** Commit `feat(police): canale di rete della pistola`.

### Task C4: Server della pistola

**Files:**
- Create: `qrm-police/src/main/java/dev/qrm/police/weapon/mc/{PistolServer,PlayerMagazines,PistolRaycast}.java`
- Modify: `qrm-police/src/main/java/dev/qrm/police/mc/PoliceMod.java` (tick, logout, `CharacterSelectedEvent` → `Identity`)

**Interfaces:**
- Consumes: B2–B5, C1–C3.
- Produces: `PistolServer.onInput(ServerPlayer, Kind)`, `PistolServer.tick(MinecraftServer)`, `PistolServer.forget(UUID)`, `PistolServer.isAiming(UUID)`.
- Produces: `PlayerMagazines implements MagazineInventory` sull'inventario vero (`drop` usa `Player.drop`).

- [ ] **Step 1:** Implementare.
  - `FIRE`: rifiuta se è passato meno di `FIRE_COOLDOWN_TICKS`, se c'è una sessione con `blocksFire()` o se `isIncapacitated`; annulla `AMMO_CHECK`/`INSPECT`; applica `PistolRules.fire`; raggio da `getEyePosition` con `Ballistics.deviate` fino a `RANGE`; testa se il punto colpito è entro 0,3 dall'altezza degli occhi del bersaglio; suono `pistol.fire` per tutti tranne il tiratore (`level.playSound(player, …)`); `Shot` a chi osserva; stato riscritto nel componente.
  - `RELOAD`: tattica se c'è il colpo in canna, altrimenti a vuoto.
  - `INTERACT_TAP`: `slideActionFor` ha la precedenza, poi `AMMO_CHECK`; con un caricatore in mano, `INSPECT_MAG`.
  - `INTERACT_HOLD`: `INSPECT`.
  - Nel tick, `sameWeapon` = slot selezionato invariato e componente con lo stesso `WeaponState.id`.
  - Quando lo slot selezionato passa a una pistola, parte `DRAW` (blocca lo sparo per la sua durata).
- [ ] **Step 2:** `./gradlew :qrm-police:build` — PASS. Prova in gioco nel Task G1.
- [ ] **Step 3:** Commit `feat(police): sparo e ricarica autoritativi`.

### Task C5: Punto armeria

**Files:**
- Modify: `qrm-police/src/main/java/dev/qrm/police/mc/PoliceMod.java`, lingue
- Create: `qrm-police/src/main/java/dev/qrm/police/weapon/mc/ArmoryPoint.java`

**Interfaces:**
- Produces: `PointType("armory", "qrm_police.point.armory", Set.of(JOB, ORGANIZATION), false, ArmoryPoint::open)`: applica `ArmoryRules.restock` ai caricatori dell'inventario con target 3 e lo comunica in chat.

- [ ] **Step 1:** Implementare e registrare come fa `LockerMod`. **Step 2:** Build — PASS.
- [ ] **Step 3:** Commit `feat(police): punto armeria per i caricatori`.

---

## Parte D — Client

### Task D1: Libreria delle animazioni e mesh per parti

**Files:**
- Create: `qrm-police/src/main/java/dev/qrm/police/weapon/client/{PistolAssets,RigMesh}.java`

**Interfaces:**
- Produces: `PistolAssets` (listener di ricarica delle risorse): `clips() -> Map<String, AnimClip>`, `rig() -> Rig` (bone, parent, pivot), `mesh(String bone) -> RigMesh`.
- Produces: `RigMesh.render(PoseStack, VertexConsumer, int light, int overlay)`: emette i quad dei cubi della parte (`from`/`to`/UV/rotazione) dal JSON esportato, con texture `qrm_police:textures/item/police_pistol.png`.

- [ ] **Step 1:** Implementare. Le parti si disegnano dai loro cubi, senza passare dal baking dei modelli: non dipende dalle API dei modelli di 26.2.
- [ ] **Step 2:** Prova: in gioco, con un comando di debug client temporaneo o un breakpoint, `PistolAssets` contiene tutte le clip e 14 bone; nessun errore nel log.
- [ ] **Step 3:** Commit `feat(police): caricamento di rig, mesh e animazioni`.

### Task D2: Rendering in prima persona con braccia

**Files:**
- Create: `qrm-police/src/main/java/dev/qrm/police/weapon/client/{PistolFirstPerson,PistolAnimator}.java`
- Modify: `qrm-police/src/main/java/dev/qrm/police/client/PoliceClient.java`

**Interfaces:**
- Consumes: B6, D1.
- Produces: `PistolAnimator` (uno per il giocatore locale): `play(PistolAction)`, `stop()`, `setAiming(boolean)`, `setSprinting(boolean)`, `pose(float partialTick) -> Map<String, BoneTransform>`, `drainEvents() -> List<AnimEvent>`. Strati: base (`idle` / `sprint` / mira, con fusione in `AIM_TICKS`) e azione, con dissolvenza di 2 tick.
- Produces: `PistolFirstPerson.onRenderHand(RenderHandEvent)`: per la mano principale con la pistola annulla l'evento, applica la gerarchia dei bone, disegna le parti e le braccia (bone `arm_right`/`arm_left` se presenti, Parte F) tramite il disegno vanilla della mano del giocatore, così Wardrobe applica la sua skin con `RenderArmEvent`.

- [ ] **Step 1:** Implementare; senza bone delle braccia nel rig, disegnare solo il braccio destro alla posa vanilla.
- [ ] **Step 2:** Prova in gioco: estrazione, a riposo, corsa, sparo, ricarica tattica e a vuoto coincidono con l'anteprima di Blockbench; con una divisa di Wardrobe il braccio ha la sua skin.
- [ ] **Step 3:** Commit `feat(police): pistola animata in prima persona`.

### Task D3: Input, mira, previsione, terza persona

**Files:**
- Create: `qrm-police/src/main/java/dev/qrm/police/weapon/client/{PistolInput,PistolKeys,PistolCamera,PistolClientBridge}.java`
- Modify: `PoliceClient.java`

**Interfaces:**
- Produces: `PistolKeys.RELOAD` (`R`), `PistolKeys.INTERACT` (`V`), categoria `qrm_police:main`.
- Produces: con la pistola in mano, `InputEvent.InteractionKeyMappingTriggered` per "attacca" → `Input(FIRE)` senza fendente; "usa" tenuto → `AIM_ON`/`AIM_OFF`; `INTERACT` rilasciato prima di `HOLD_THRESHOLD_TICKS` → `INTERACT_TAP`, dopo → `INTERACT_HOLD`.
- Produces: `ComputeFovModifierEvent` ×0,85 in mira; `ViewportEvent.ComputeCameraAngles` aggiunge il contraccolpo dell'evento `camera_kick`, che rientra in 6 tick (H6); `IClientItemExtensions.getArmPose` → `BOW_AND_ARROW` se mira (anche per gli altri tramite `Aim`), altrimenti `ITEM`.
- Previsione: su `FIRE` il client applica `PistolRules.fire` alla propria copia; se `fired`, avvia `fire`/`fire_last` subito; altrimenti `dry_fire` (E6), solo in locale; `Rejected` ferma la clip. Per le altre azioni il client aspetta `ActionStarted`.

- [ ] **Step 1:** Implementare. **Step 2:** Prova in gioco: mira, sparo rapido limitato a 4 colpi al secondo, nessun fendente o blocco rotto cliccando.
- [ ] **Step 3:** Commit `feat(police): comandi, mira e previsione dello sparo`.

### Task D4: Suggerimenti vicino alla pistola

**Files:**
- Create: `qrm-police/src/main/java/dev/qrm/police/weapon/client/PistolHints.java`

**Interfaces:**
- Consumes: `PistolRules.hint`, componenti sincronizzati, `PistolKeys`.
- Produces: testo disegnato nello spazio della prima persona, ancorato al bone `slide` con uno scostamento a destra, rivolto alla camera; ingresso e uscita in 6 tick.
  - `RELEASE_SLIDE` → `qrm_police.hint.release_slide` ("[%s] Sblocca il carrello");
  - `RACK_SLIDE` → "[%s] Arretra il carrello";
  - `RELOAD` → "[%s] Ricarica";
  - `NO_SPARE` → "Nessun caricatore carico".
  - Risultato del controllo munizioni (evento `commit check`) per 40 tick: "Caricatore: <fascia> · Colpo in canna: sì/no".
- Proposta per Q4c: il suggerimento resta finché dura lo stato; `NO_SPARE` compare per 40 tick dopo un clic a vuoto.

- [ ] **Step 1:** Implementare. **Step 2:** Prova in gioco per ognuno dei 4 stati e per il controllo munizioni.
- [ ] **Step 3:** Commit `feat(police): suggerimenti vicino alla pistola`.

### Task D5: Tooltip personale ed effetti

**Files:**
- Create: `qrm-police/src/main/java/dev/qrm/police/weapon/client/{ClientIdentity,PistolEffects}.java`

**Interfaces:**
- Produces: `ClientIdentity.characterId() -> Optional<String>` (da `Identity`, azzerato al logout).
- Produces: `PistolEffects.onShot(Shot)` e gli eventi `muzzle_flash`/`eject_shell` per il giocatore locale.
  - Vampa: particella alla canna.
  - Bossolo espulso: già nella clip; per gli altri, una particella.
  - Impatto: particelle `BlockParticleOption` del blocco colpito, più il suono del gruppo di materiale ricavato da `SoundType`: metallo, legno, pietra, morbido.

- [ ] **Step 1:** Implementare. **Step 2:** Prova con due client: lo sparo si sente e si vede dallo stesso punto; il tooltip di un caricatore controllato da A è vuoto per B.
- [ ] **Step 3:** Commit `feat(police): effetti di sparo e ricordo dei caricatori`.

---

## Parte E — Audio

### Task E1: Eventi sonori, segnaposto e fonti

**Files:**
- Create: `assets/qrm_police/sounds.json`, `qrm-police/src/main/java/dev/qrm/police/weapon/mc/PistolSounds.java`, `qrm-police/AUDIO_SOURCES.md`
- Test: `qrm-police/src/test/java/dev/qrm/police/weapon/SoundCoverageTest.java`

**Interfaces:**
- Produces: un `SoundEvent` per ogni id usato dagli eventi `sound` delle animazioni (12 attuali + `pistol.slide_release`, `pistol.slide_rack`, `pistol.mag_inspect`) e `impact.{metal,wood,stone,soft}`.

- [ ] **Step 1:** `SoundCoverageTest`: ogni `sound <id>` del JSON esportato esiste in `sounds.json`; ogni file `.ogg` sotto `sounds/` compare in `AUDIO_SOURCES.md` con fonte, autore e licenza. FAIL.
- [ ] **Step 2:** Implementare con segnaposto vanilla (voci `"type": "event"`), così il gioco funziona prima dei suoni veri. PASS.
- [ ] **Step 3:** Commit `feat(police): eventi sonori della pistola`.

### Task E2: Suoni veri (produzione)

- [ ] **Step 1:** Cercare in librerie CC0 o royalty-free (per esempio Freesound con filtro CC0, Sonniss GDC); per ogni file registrare fonte, autore e licenza in `AUDIO_SOURCES.md` **prima** di usarlo.
- [ ] **Step 2:** Tagliare, sovrapporre e normalizzare; mono, OGG Vorbis; 3 varianti per sparo e impatti; maneggio più basso, con `attenuation_distance` ridotta in `sounds.json`.
- [ ] **Step 3:** `./gradlew :qrm-police:test` — PASS. Ascolto in gioco a 2, 16 e 48 blocchi.
- [ ] **Step 4:** Commit `feat(police): suoni della pistola`.

---

## Parte F — Grafica in Blockbench (in parallelo a B–E)

Il codice funziona anche se queste clip mancano (il test B3 le salta; D2 non le anima). Ogni task finisce con `python tools/pistol_export.py`, `police_pistol_check.py` e `./gradlew :qrm-police:test`.

### Task F1: Pulizia delle animazioni esistenti

- [ ] **Step 1:** Ridurre ogni clip a pochi keyframe con curve `linear`/`catmullrom`, mantenendo durate e istanti degli eventi `commit`. **Step 2:** Confrontare in Blockbench con la versione campionata. **Step 3:** Commit `art(police): animazioni ripulite`.

### Task F2: Braccia in prima persona

- [ ] **Step 1:** Aggiungere i bone `arm_right` e `arm_left` (proporzioni del braccio del giocatore, con il segnaposto della skin), animati in tutte le clip; le pose `arm <nome>` diventano keyframe. **Step 2:** Esportare. **Step 3:** Commit `art(police): braccia nelle animazioni`.

### Task F3: Clip nuove

- [ ] **Step 1:** Creare, con eventi e `commit`:
  - `aim_in`/`aim` (posa di mira, ~0,2 s);
  - `slide_release` (~0,4 s, `commit slide`);
  - `slide_rack` (~0,5 s, `commit slide`);
  - `ammo_check` (~1 s, `commit check`);
  - `inspect_mag` (~1 s, `commit inspect_mag`);
  - varianti di `fire`/`fire_last`/`dry_fire` dalla posa di mira.
- [ ] **Step 2:** Esportare; il test B3 ora verifica anche queste clip. **Step 3:** Commit `art(police): clip di mira, carrello e controllo`.

### Task F4: Texture e caricatore

- [ ] **Step 1:** Passaggio di qualità sulla texture 64×64 (separazione tra polimero, metallo, mire, comandi), nel `.bbmodel`. **Step 2:** Icona e modello di `police_magazine` dalla parte `magazine`. **Step 3:** Commit `art(police): texture e caricatore`.

---

## Parte G — Collaudo e documentazione

### Task G1: Collaudo manuale e wiki

**Files:**
- Create: `docs/manual-tests/pistol.md`
- Modify: pagina Police della wiki in `website/` (sezione "Pistola d'ordinanza", controllata da check-docs), `docs/moduli.md`

- [ ] **Step 1:** Scrivere il collaudo con due giocatori, uno per ogni criterio di accettazione della specifica (sezione 9) e per le voci del Review Focus 2 e 4.
- [ ] **Step 2:** Eseguirlo con `dev-start.bat` e annotare l'esito.
- [ ] **Step 3:** Aggiornare wiki e `docs/moduli.md`; build con check-docs verde.
- [ ] **Step 4:** Commit `docs(police): pistola d'ordinanza`.

---

## Ordine e dipendenze

```
A1 → A2 → A3 ─┐
B1 → B2 → B3 → B4 → B5     B6
C1 ───────────┤
              ├→ C2 → C3 → C4 → C5
              └→ D1 → D2 → D3 → D4 → D5 → G1
E1 (dopo A2 e C2) → E2 (in qualunque momento dopo E1)
F1–F4 in parallelo dopo A2
```

- La Parte B e il Task C1 non dipendono dagli asset e si possono fare subito.
- La Parte F è grafica e può andare avanti in parallelo.
- **Rischio principale:** D2 (rendering in prima persona). Mitigazione: D1 disegna le parti dai cubi, senza dipendere dalle API di baking dei modelli di 26.2.

## Handoff — scelte del piano da confermare

Sono dettagli che la specifica lasciava aperti; il piano propone:

1. **Q6a** Asset spostati nel namespace `qrm_police` (Task A3).
2. **Q4e** Caricatori non impilabili: ogni caricatore ha un id, che serve a non reinserire quello appena tolto.
3. **Q5a** Testa ×1,5 (6 danni), gittata 64 blocchi, stesso danno alle creature, dispersione 3,0° dal fianco e 0,5° in mira.
4. **Q2d** Ricarica `R`, interazione `V`; mira con il tasto destro, sparo con il sinistro.
5. **Q4c** Suggerimento visibile finché dura lo stato; "Nessun caricatore carico" per 2 s dopo un clic a vuoto.
6. **Armeria** Riempie i caricatori portati e ne aggiunge fino ad averne 3.
7. **Riposizionamento (`holster`)** In Minecraft il cambio di slot è immediato, quindi la clip `holster` non ha il tempo di essere vista. Proposta: escluderla dalla prima versione; resta `draw`.
