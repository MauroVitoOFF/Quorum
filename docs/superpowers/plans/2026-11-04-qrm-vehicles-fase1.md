# qrm-vehicles — Fase 1: veicolo guidabile (piano di implementazione)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Un nuovo modulo `qrm-vehicles` con la Jeep Wrangler guidabile in gioco: entità, guida morbida, due sedili, carburante, contachilometri, fari e clacson.

**Architecture:** Logica di guida, carburante e contachilometri in Java puro (`logic`), testata con JUnit. L'entità è **autorevole sul server**: il client guidatore invia l'input a ogni tick, il server simula e il movimento arriva agli altri client con la normale sincronizzazione delle entità. Il modello Bedrock `jeep_wrangler.geo.json` è letto direttamente a runtime da un parser nuovo che riusa le classi `Rig`/`PartMesh`/`BonePose` già usate dalla pistola, spostate in `qrm-client-api` perché le condividano i due moduli.

**Tech Stack:** NeoForge 26.2.0.88, Minecraft 26.2 (nomi Mojang), Java 25, JOML, JUnit 5.11.4, Gson.

**Spec:** `docs/superpowers/specs/2026-11-04-qrm-vehicles-design.md` (fase 1 della sezione "Divisione in fasi").

## Global Constraints

- NeoForge `26.2.0.88`, Minecraft `26.2`, toolchain Java `25`; nessuna nuova dipendenza esterna (niente GeckoLib).
- Struttura del modulo identica a `qrm-police`/`qrm-locker`: pacchetti `logic` (Java puro), `mc`, `client`, `net`, radice `dev.qrm.vehicles`, mod id `qrm_vehicles`, dipende da `qrm-api` e `qrm-client-api` solo a compile time.
- Guida: valori di tuning in un solo posto (`VehicleDef`), mai sparsi nel codice.
- L'usura/manutenzione e le chiavi sono **fuori dalla fase 1**. Nessun danno da incidente: l'entità è invulnerabile.
- Il bagagliaio, la targa e il legame con il Character sono della fase 2. I distributori di carburante sono rimandati.
- Convenzioni del repo: stringhe in italiano e inglese (`lang/it_it.json`, `lang/en_us.json`), test manuali in `docs/manual-tests/`, pagina wiki in `website/src/content/docs/docs/modules/`, `qrm_version` in `gradle.properties` e `API_VERSION` in `qrm-api/.../QrmVersion.java` da aggiornare insieme.
- Gradle: non usare `clean` (cancella gli argomenti di run); per i run rigenerare con `prepareClientRun`/`prepareServerRun`. Gradle si esegue tramite `ctx_execute`.
- Davanti al veicolo = -Z. Il file Bedrock ha l'asse X invertito (lato sinistro = +X nel file): il parser lo specchia.
- Il modello va letto com'è nel file in `design/jeep-wrangler/jeep_wrangler.geo.json`: l'utente lo ritocca in Blockbench, non rigenerarlo.

## Review Focus

1. Il guidatore si scollega, muore o scende mentre il veicolo è in moto: il veicolo smette di accelerare e si ferma, non resta a correre da solo.
2. Riavvio del server o scaricamento del chunk con carburante, chilometri e passeggeri: lo stato torna identico e il veicolo non si duplica.
3. Input del client malevolo o corrotto (NaN, throttle 5.0, pacchetti a raffica, input da chi non è alla guida): clampato o ignorato, mai applicato.
4. Muri, salti nel vuoto, acqua e lava: il veicolo non entra nei blocchi, cade con la gravità, in acqua non si guida.
5. Due giocatori che provano a sedersi sullo stesso sedile, o il carburante che finisce in marcia: nessun doppio guidatore, il motore si spegne e il veicolo rallenta fino a fermarsi.

---

### Task 1: Spostare le classi del rig in `qrm-client-api`

**Files:**
- Move: `qrm-police/src/main/java/dev/qrm/police/weapon/logic/mesh/{Rig,MeshBuilder,PartMesh,BonePose}.java` e `.../logic/anim/BoneTransform.java` → `qrm-client-api/src/main/java/dev/qrm/client/api/rig/` (pacchetto `dev.qrm.client.api.rig`)
- Move: i test corrispondenti (`BonePoseTest`, i test di `MeshBuilder`/`Rig`) → `qrm-client-api/src/test/java/dev/qrm/client/api/rig/`
- Modify: gli import in `qrm-police` (circa 32 file, cercare `logic.mesh` e `logic.anim.BoneTransform`)
- Modify: `qrm-client-api/build.gradle` (aggiungere `testImplementation 'org.joml:joml:1.10.8'`)

**Interfaces:**
- Produces: `dev.qrm.client.api.rig.{Rig, Rig.Bone, MeshBuilder, PartMesh, BonePose, BoneTransform}`, con le stesse firme di oggi. `ArmSolver` e il resto di `anim` restano in police.

- [ ] **Step 1:** Eseguire i test attuali di `qrm-police` e annotare quanti passano (riferimento prima/dopo).
- [ ] **Step 2:** Spostare i cinque file e i loro test con `git mv`, cambiare il `package`, correggere gli import in `qrm-police`. `ArmSolver` importa `BoneTransform` dal nuovo pacchetto.
- [ ] **Step 3:** Compilare `qrm-client-api` e `qrm-police`; se `Rig` o `BonePose` non trovano JOML/le annotazioni a compile time in `qrm-client-api`, aggiungere la dipendenza minima in quel `build.gradle`.
- [ ] **Step 4:** Eseguire i test di `qrm-client-api` e `qrm-police`. Atteso: lo stesso numero di test verdi del passo 1, nessuna regressione.
- [ ] **Step 5: Commit** — `refactor(rig): sposta Rig, MeshBuilder, PartMesh, BonePose e BoneTransform in qrm-client-api`.

---

### Task 2: Parser del modello Bedrock (`GeoModel`)

**Files:**
- Create: `qrm-client-api/src/main/java/dev/qrm/client/api/rig/GeoModel.java`
- Test: `qrm-client-api/src/test/java/dev/qrm/client/api/rig/GeoModelTest.java`

**Interfaces:**
- Consumes: `Rig`, `Rig.Bone(String name, @Nullable String parent, float[] pivot, boolean hasModel)`, `PartMesh` (Task 1).
- Produces: `public record GeoModel(Rig rig, Map<String, PartMesh> meshes, Map<String, float[]> locators, int textureWidth, int textureHeight)` con `public static GeoModel parse(Reader json)`. Unità: Blockbench (16 = 1 blocco), già nello spazio di gioco (X specchiata, davanti = -Z). `locators` è nome → posizione assoluta. Le ossa sono ordinate genitore prima del figlio.

Il file della Jeep usa solo cubi con UV a scatola (`uv: [u, v]`), `inflate` su 102 cubi, nessuna rotazione di cubo né di osso, nessun `mirror`; i `locators` stanno dentro le ossa. Il parser supporta solo questo e rifiuta con un errore chiaro `uv` per faccia, rotazioni e `mirror`.

- [ ] **Step 1: Scrivere i test** in `GeoModelTest`, con un geo minimo scritto inline:
  - `cuboHaSeiFacceConNormaliVersoLEsterno`: un cubo `origin [-1,0,-1] size [2,2,2]` → 6 quad, e per ogni quad il prodotto scalare tra normale e vettore dal centro del cubo al centro del quad è > 0 (dopo lo specchio dell'asse X l'avvolgimento deve restare corretto).
  - `inflateAllargaIlCubo`: con `inflate 0.5` il bounding box dei vertici cresce di 0.5 per lato.
  - `assiXSpecchiato`: un osso con pivot `[11, 6.5, -18]` ha pivot di gioco `[-11, 6.5, -18]`; un locator `[-6.5, 24.5, -1.5]` diventa `[6.5, 24.5, -1.5]`.
  - `uvAScatola`: la faccia nord del cubo `uv [0,0] size [2,2,2]` mappa a `u ∈ [2,4]`, `v ∈ [2,4]` (layout Bedrock standard: west, north, east, south sulla riga sotto la riga di up/down), normalizzati per `texture_width`/`texture_height`.
  - `ossaOrdinateGenitorePrima` e `osso senza cubi ha hasModel=false`.
  - `rifiutaFeatureNonSupportate`: un cubo con `rotation` o con `uv` a oggetto lancia `IllegalArgumentException` il cui messaggio nomina l'osso.
- [ ] **Step 2:** Eseguire i test, atteso FAIL (classe mancante).
- [ ] **Step 3: Implementare** `GeoModel.parse` in `GeoModel.java` con Gson; la geometria dei quad la costruisce `MeshBuilder`/`PartMesh` se già espone un costruttore per quad, altrimenti si costruiscono i `PartMesh.Quad` a mano. Lo specchio X: `x' = -x` per pivot e locator, `x' = -(origin.x + size.x)` come minimo del cubo, scambiando west/east e invertendo l'ordine dei vertici.
- [ ] **Step 4:** Eseguire i test, atteso PASS.
- [ ] **Step 5: Commit** — `feat(rig): parser dei modelli Bedrock (geo.json)`.

---

### Task 3: Scheletro del modulo `qrm-vehicles` e asset della Jeep

**Files:**
- Create: `qrm-vehicles/build.gradle` (copia di `qrm-police/build.gradle`, con `archivesName = 'qrm-vehicles'`, mod `qrm_vehicles`)
- Create: `qrm-vehicles/src/main/resources/META-INF/neoforge.mods.toml` (modello: quello di `qrm_police`, modId `qrm_vehicles`, dipendenza `qrm`)
- Create: `qrm-vehicles/src/main/java/dev/qrm/vehicles/mc/VehiclesMod.java` (`@Mod("qrm_vehicles")`, per ora vuota)
- Create: `qrm-vehicles/src/main/resources/assets/qrm_vehicles/geo/jeep_wrangler.geo.json` e `textures/entity/jeep_wrangler.png` (copie da `design/jeep-wrangler/`)
- Create: `qrm-vehicles/src/main/resources/assets/qrm_vehicles/lang/{it_it,en_us}.json`
- Modify: `settings.gradle` (aggiungere `'qrm-vehicles'`); registrare il modulo dove compare `qrm-locker` in altri `*.gradle` (cercarlo con grep)
- Test: `qrm-vehicles/src/test/java/dev/qrm/vehicles/logic/JeepModelTest.java`

**Interfaces:**
- Produces: modulo compilabile; `JeepModelTest` fissa il contratto tra il modello e il codice.

- [ ] **Step 1: Scrivere `JeepModelTest`** che legge `/assets/qrm_vehicles/geo/jeep_wrangler.geo.json` dal classpath con `GeoModel.parse` e asserisce: 115 ossa; esistono le ossa `body`, `steer_fl`, `steer_fr`, `wheel_fl`, `wheel_fr`, `wheel_rl`, `wheel_rr`, `door_l`, `door_r`; esistono i locator `driver_eye`, `driver_hip`, `passenger_hip`, `engine_sound`, `exhaust_tip`; `locators.get("driver_eye")` vale `{6.5, 24.5, -1.5}` (specchiato rispetto alla memoria del modello).
- [ ] **Step 2:** Creare scheletro, `settings.gradle`, asset e lang (chiave `entity.qrm_vehicles.jeep_wrangler` = "Jeep Wrangler"). Eseguire `:qrm-vehicles:test`, atteso: `JeepModelTest` verde. Se un locator ha un altro nome o manca nel file, correggere il test con il nome reale e **segnalarlo all'utente** (i locator li ha aggiunti lui a mano in Blockbench).
- [ ] **Step 3:** Eseguire `:qrm-vehicles:compileJava` e `prepareClientRun`; il gioco deve avviarsi con il modulo caricato (nel log compare `qrm_vehicles`).
- [ ] **Step 4: Commit** — `feat(vehicles): scheletro del modulo e asset della Jeep`.

---

### Task 4: Modello di guida (`VehicleDef`, `Surface`, `DrivingModel`)

**Files:**
- Create: `qrm-vehicles/src/main/java/dev/qrm/vehicles/logic/{VehicleDef,Surface,DriveInput,DriveState,DrivingModel}.java`
- Test: `qrm-vehicles/src/test/java/dev/qrm/vehicles/logic/DrivingModelTest.java`

**Interfaces:**
- Produces:
  - `record VehicleDef(String id, int seats, double maxSpeed, double reverseMaxSpeed, double accel, double brake, double drag, double maxSteerDeg, double steerRateDeg, double grip, double wheelBase, double fuelCapacity, double fuelPer100Blocks)` con `static final VehicleDef JEEP`. Valori iniziali, tutti da regolare provando in gioco: `maxSpeed 0.75` blocchi/tick, `reverseMaxSpeed 0.25`, `accel 0.02`, `brake 0.06`, `drag 0.005`, `maxSteerDeg 30`, `steerRateDeg 6`, `grip 0.35`, `wheelBase 2.4`, `seats 2`, `fuelCapacity 60`, `fuelPer100Blocks 0.08`.
  - `enum Surface { PAVED(1.0, 1.0), DIRT(0.85, 0.9), ROUGH(0.6, 0.65) }` con `gripFactor()` e `speedFactor()`.
  - `record DriveInput(double throttle, double steer, boolean brake)`, con `throttle` e `steer` in [-1, 1] e un metodo `DriveInput clamped()` che porta fuori intervallo/NaN a 0 o al limite.
  - `record DriveState(double heading, double steerAngle, double vx, double vz)`; `heading` in gradi, 0 = verso +Z; `double speed()` (modulo di (vx, vz)); `double forwardSpeed()` (componente lungo `heading`, negativa in retromarcia); `static DriveState rest(double heading)`.
  - `final class DrivingModel` con `static DriveState step(VehicleDef def, DriveState s, DriveInput in, Surface surface, boolean engineOn)` (un tick).

Regole del modello: l'accelerazione spinge lungo `heading`; il freno e la resistenza riducono la velocità; lo sterzo ruota `heading` in proporzione alla velocità in avanti e a `wheelBase`; la velocità laterale rispetto a `heading` decade con `grip * surface.gripFactor()` per tick, così il veicolo sbanda un poco e si raddrizza da solo. A motore spento l'input non accelera ma freno e resistenza agiscono.

- [ ] **Step 1: Scrivere i test** in `DrivingModelTest`, usando `VehicleDef.JEEP`:
  - `accelerazionePienaSiFermaAllaVelocitaMassima`: 200 tick di `throttle 1` su `PAVED` → `speed() ≤ maxSpeed + 1e-9` e `speed() > 0.95 * maxSpeed`.
  - `lAccelerazioneEMonotona`: la velocità non cala mai nei primi 50 tick con `throttle 1`.
  - `frenataFermaIlVeicolo`: da `maxSpeed`, `brake=true` per 30 tick → `speed() == 0` (sotto 1e-6).
  - `retromarciaLimitata`: `throttle -1` da fermo per 200 tick → `forwardSpeed() ≥ -reverseMaxSpeed - 1e-9` e `< 0`.
  - `terrenoDifficileRallenta`: la velocità finale su `ROUGH` è inferiore a `0.7 * maxSpeed`.
  - `fermoNonSterza`: a velocità 0, 20 tick di `steer 1` lasciano `heading` invariato.
  - `sterzataACorsaCambiaDirezione`: a `0.5 * maxSpeed`, 20 tick di `steer 1` cambiano `heading` di almeno 10 gradi.
  - `sbandataRientra`: dopo una sterzata a velocità alta e 40 tick con `steer 0`, l'angolo tra la velocità e `heading` è < 2 gradi; durante la sterzata è > 0 e < 45.
  - `motoreSpentoNonAccelera`: con `engineOn=false` e `throttle 1`, la velocità da fermo resta 0; da `maxSpeed` scende ogni tick fino a 0.
  - `inputCorrottoNonRompeIlModello`: `DriveInput(NaN, 5.0, false)` dopo `clamped()` produce uno stato con tutti i valori finiti.
- [ ] **Step 2:** Eseguire i test, atteso FAIL.
- [ ] **Step 3: Implementare** le classi. Integrare in coordinate di mondo; lo specchio tra `heading` e `Entity.setYRot` lo decide l'entità (Task 6).
- [ ] **Step 4:** Eseguire i test, atteso PASS.
- [ ] **Step 5: Commit** — `feat(vehicles): modello di guida morbido (logic)`.

---

### Task 5: Carburante e contachilometri

**Files:**
- Create: `qrm-vehicles/src/main/java/dev/qrm/vehicles/logic/{FuelTank,Odometer}.java`
- Test: `qrm-vehicles/src/test/java/dev/qrm/vehicles/logic/FuelOdometerTest.java`

**Interfaces:**
- Produces:
  - `final class FuelTank` con `FuelTank(double capacity, double level)`, `double level()`, `double capacity()`, `boolean isEmpty()`, `void consume(double blocks, double per100Blocks)`, `double add(double liters)` (restituisce i litri davvero aggiunti), `double room()`.
  - `final class Odometer` con `Odometer(double blocks)`, `void add(double blocks)`, `double blocks()`, `double km()` (1 km = 1000 blocchi), `static Odometer fromKm(double km)`.

- [ ] **Step 1: Scrivere i test:** `consumoProporzionaleAllaStrada` (500 blocchi a 0.08 L/100 → -0.4 L), `nonScendeSottoZero`, `aggiungiRispettaLaCapacita` (aggiunge solo fino al pieno e restituisce il valore aggiunto), `contachilometriAccumulaESiConvertePerKm` (2500 blocchi → 2.5 km), `valoriNegativiONanSonoIgnorati` per `add`/`consume`/`Odometer.add`.
- [ ] **Step 2:** Eseguire, atteso FAIL. **Step 3:** Implementare. **Step 4:** Eseguire, atteso PASS.
- [ ] **Step 5: Commit** — `feat(vehicles): serbatoio e contachilometri (logic)`.

---

### Task 6: Entità, registrazione, persistenza e sedili

**Files:**
- Create: `qrm-vehicles/src/main/java/dev/qrm/vehicles/mc/{QrmVehicleEntity,VehicleEntities,VehicleCommand}.java`
- Modify: `qrm-vehicles/.../mc/VehiclesMod.java` (registrare entità e comando)
- Test: `qrm-vehicles/src/test/java/dev/qrm/vehicles/mc/VehicleSaveDataTest.java` (solo la parte di serializzazione che si può provare senza server)

**Interfaces:**
- Consumes: `VehicleDef`, `DrivingModel`, `DriveState`, `DriveInput`, `Surface`, `FuelTank`, `Odometer` (Task 4-5), `GeoModel.locators` (Task 2).
- Produces:
  - `QrmVehicleEntity extends net.minecraft.world.entity.Entity`, costruttore `(EntityType<?>, Level)`, con `void setDriverInput(DriveInput in)` (solo server), `boolean engineOn()`, `double fuelLevel()`, `double odometerKm()`, `double speed()`, `float steerAngle()`, `boolean lightsOn()`, `VehicleDef def()`.
  - `VehicleEntities.JEEP` (`DeferredHolder<EntityType<?>, EntityType<QrmVehicleEntity>>`), `VehicleEntities.ENTITIES.register(modBus)`.
  - Comando `/qrm vehicle spawn <id>` (permesso staff come `PoliceCommand`), che crea il veicolo davanti al giocatore con il pieno.

Dati sincronizzati (via `SynchedEntityData`): motore acceso, fari, carburante, chilometri (in km come float se manca un serializzatore double), velocità, angolo di sterzo. Salvataggio con `addAdditionalSaveData`/`readAdditionalSaveData`: carburante, chilometri, motore spento al caricamento, `def` id.

Comportamento, dall'alto: `hurtServer` restituisce `false` (invulnerabile); `isPickable` vero; `isPushable` falso; `interact` mette il giocatore sul primo sedile libero (autista per primo) e restituisce `InteractionResult.SUCCESS`; `canAddPassenger` rifiuta oltre `def.seats()`; `getControllingPassenger` è il passeggero 0; le posizioni dei sedili vengono da `driver_hip`/`passenger_hip` ruotate dal `yRot`; `getDismountLocationForPassenger` cerca il primo punto libero accanto al veicolo, altrimenti la posizione del veicolo. Nel tick **solo server**: legge l'ultimo `DriveInput` (azzerato se nessun input è arrivato da più di 5 tick o se non c'è un autista), chiama `DrivingModel.step`, converte in `Vec3` con `move(MoverType.SELF, ...)` (la collisione la fa Minecraft; se una componente viene azzerata dagli urti, azzera la stessa componente nello stato), applica gravità, consuma carburante, aggiorna il contachilometri e spegne il motore a serbatoio vuoto. Il terreno (`Surface`) si legge dal blocco sotto le ruote: sabbia, neve, fango, terra battuta e simili ⇒ `ROUGH`/`DIRT`, il resto `PAVED`; in acqua o lava la velocità va a 0 e l'input è ignorato. Gradino: sale i mezzi blocchi (altezza massima del passo 0.6 più il mezzo blocco, regolabile).

- [ ] **Step 1: Scrivere i test di serializzazione** (`VehicleSaveDataTest`): salvare e rileggere carburante 31.5 e chilometri 1234.5 restituisce gli stessi valori, e una lettura di dati vecchi o mancanti usa il pieno e zero km senza eccezioni. Se il salvataggio non è provabile senza un server, estrarre la parte di dati in un record `VehicleSaveData` in `logic` con `toMap()`/`fromMap()` e testare quello.
- [ ] **Step 2:** Eseguire, atteso FAIL. **Step 3:** Implementare entità, registro, comando e salvataggio; la parte non automatizzabile si prova nel Task 10.
- [ ] **Step 4:** Eseguire i test, atteso PASS; compilare l'intero modulo.
- [ ] **Step 5: Commit** — `feat(vehicles): entità del veicolo, registrazione e comando di spawn`.

---

### Task 7: Rete e input del guidatore

**Files:**
- Create: `qrm-vehicles/src/main/java/dev/qrm/vehicles/net/{VehiclePayloads,VehicleNetwork}.java`
- Create: `qrm-vehicles/src/main/java/dev/qrm/vehicles/client/{VehicleInput,VehicleKeys,VehicleClientBridge}.java`
- Modify: `VehiclesMod.java` (registrare i payload)
- Test: `qrm-vehicles/src/test/java/dev/qrm/vehicles/net/DriverInputValidationTest.java`

**Interfaces:**
- Consumes: `QrmVehicleEntity.setDriverInput`, `DriveInput.clamped()` (Task 4, 6).
- Produces: `VehiclePayloads.Drive(double throttle, double steer, boolean brake)` e `VehiclePayloads.Action(Kind kind)` con `enum Kind { IGNITION, LIGHTS, HORN }`, entrambi `playToServer`; `VehicleNetwork.register(RegisterPayloadHandlersEvent)` sul modello di `PoliceNetwork` (canale opzionale, `PROTOCOL_VERSION = "1"`).

Il server applica un pacchetto solo se `ctx.player()` è il `getControllingPassenger()` di un `QrmVehicleEntity` e ignora tutto il resto (un `Action.kind` nullo incluso). Il client invia `Drive` a ogni tick mentre guida: avanti/indietro = tasti di movimento, sinistra/destra = tasti di movimento, freno = salto (spazio). Tasti nuovi: accensione `R`, fari `L`, clacson `H` (verificare in `PistolKeys` e nei tasti vanilla che non ci siano conflitti, altrimenti scegliere liberi e annotarlo nel manuale).

- [ ] **Step 1: Scrivere i test** (`DriverInputValidationTest`) sulla funzione pura `static DriveInput sanitize(double throttle, double steer, boolean brake)` in `VehiclePayloads`: `(5.0, -9.0, true)` → `(1.0, -1.0, true)`; `(NaN, 0, false)` → throttle 0; `(0.4, 0.2, false)` passa invariato.
- [ ] **Step 2:** Eseguire, atteso FAIL. **Step 3:** Implementare payload, registrazione, campionamento del client (evento di tick client post; se il giocatore non guida non si invia nulla) e gestione server con il controllo del guidatore.
- [ ] **Step 4:** Eseguire, atteso PASS. **Step 5: Commit** — `feat(vehicles): input del guidatore e azioni su rete`.

---

### Task 8: Rendering, camera e HUD

**Files:**
- Create: `qrm-vehicles/src/main/java/dev/qrm/vehicles/client/{VehicleAssets,VehicleRenderer,VehicleRenderState,VehiclePose,VehicleHud,VehicleClient}.java`
- Test: `qrm-vehicles/src/test/java/dev/qrm/vehicles/client/VehiclePoseTest.java`

**Interfaces:**
- Consumes: `GeoModel`, `BonePose.compose`, `BoneTransform` (Task 1-2), `RigMesh` (oggi in `qrm-police/.../weapon/client`: copiare la classe in `qrm-vehicles/client` o, se identica, spostarla anch'essa in `qrm-client-api` con lo stesso metodo del Task 1).
- Produces: `VehiclePose.pose(double forwardSpeed, float steerAngleDeg, float wheelRotationDeg, float bodyPitchDeg, float bodyRollDeg) → Map<String, BoneTransform>` (pura, testabile): ruota `steer_fl`/`steer_fr` attorno a Y dell'angolo di sterzo, le quattro `wheel_*` attorno a X di `wheelRotationDeg`, e `body` di pitch/roll. `VehicleAssets` (reload listener sul modello di `WeaponAssets`) carica `geo/jeep_wrangler.geo.json` e una `RigMesh` per ogni osso con cubi; un file rotto produce un solo errore nel log e lascia il precedente.

- [ ] **Step 1: Scrivere i test** (`VehiclePoseTest`): sterzo 20° ⇒ `steer_fl` e `steer_fr` hanno `rotationDeg[1] == 20`; ruota a 90° ⇒ le quattro ruote `rotationDeg[0] == 90`; il pitch si trasferisce a `body`; sterzo e rotazione a zero ⇒ pose a riposo (`BoneTransform.REST`) per tutti i bone elencati.
- [ ] **Step 2:** Eseguire, atteso FAIL. **Step 3:** Implementare `VehiclePose`, assets, renderer (usa `RenderTypes` con texture del veicolo; il vetro e la capote richiedono un render type translucido), camera in prima persona al locator `driver_eye` quando il giocatore guida, e HUD con tachimetro, indicatore del carburante e contachilometri. Rollio e beccheggio nascono dall'accelerazione laterale e longitudinale con smorzamento (valori di tuning in `VehicleDef` o in costanti nominate).
- [ ] **Step 4:** Eseguire i test, atteso PASS. **Step 5: Commit** — `feat(vehicles): rendering, camera e HUD`.

---

### Task 9: Carburante in oggetti, accensione, fari e clacson

**Files:**
- Create: `qrm-vehicles/src/main/java/dev/qrm/vehicles/mc/{VehicleItems,VehicleSounds}.java`
- Create: `qrm-vehicles/src/main/resources/assets/qrm_vehicles/{items,models/item}/jerry_can*.json`, `sounds.json`
- Modify: `QrmVehicleEntity` (gestione di `Action`), `lang/*.json`
- Test: `qrm-vehicles/src/test/java/dev/qrm/vehicles/logic/RefuelRulesTest.java`

**Interfaces:**
- Produces: `RefuelRules.apply(FuelTank tank, double canLiters) → RefuelResult(boolean accepted, double added)` in `logic`; oggetti `VehicleItems.JERRY_CAN` (10 L) e `VehicleItems.JERRY_CAN_EMPTY`; `VehicleSounds` con segnaposto agganciati a suoni vanilla (avviamento, motore, clacson) da sostituire con asset veri.

Usare la tanica piena su un veicolo la consuma e restituisce quella vuota, **solo se nel serbatoio c'è posto per tutti i 10 L**; altrimenti nessun effetto e un messaggio nella barra azioni. Come le taniche entrano nel gioco (craft, negozi) non è deciso: in questa fase solo tab creativa e comando di staff.

- [ ] **Step 1: Scrivere i test** (`RefuelRulesTest`): serbatoio con 50 L su 60 e tanica da 10 → accettata, aggiunti 10; con 55 su 60 → rifiutata, 0 aggiunti, livello invariato; serbatoio pieno → rifiutata.
- [ ] **Step 2:** Eseguire, atteso FAIL. **Step 3:** Implementare `RefuelRules`, oggetti, suoni e l'azione `IGNITION` (non parte a serbatoio vuoto; spegne se già accesa), `LIGHTS` (inverte), `HORN` (suono con ricarica breve lato server).
- [ ] **Step 4:** Eseguire, atteso PASS. **Step 5: Commit** — `feat(vehicles): taniche, accensione, fari e clacson`.

---

### Task 10: Prova in gioco, documentazione e versione

**Files:**
- Create: `docs/manual-tests/vehicles.md`
- Create: `website/src/content/docs/docs/modules/vehicles.md`
- Modify: `gradle.properties` (`qrm_version`), `qrm-api/src/main/java/dev/qrm/api/QrmVersion.java` (`API_VERSION`)

- [ ] **Step 1:** Scrivere `docs/manual-tests/vehicles.md` con il giro completo: `/qrm vehicle spawn jeep_wrangler`, salire, accendere (`R`), guidare su strada, su sabbia e su terra, sterzare a velocità alta, salire un mezzo blocco, frenare, cadere da un dislivello, entrare in acqua, scendere, rifornire con la tanica, finire il carburante in marcia, riavviare il server e controllare carburante e chilometri; con due giocatori, sedersi sul posto del passeggero e provare a prendere il volante.
- [ ] **Step 2:** Eseguire il giro in gioco (client e server dedicato, dopo `prepareClientRun`/`prepareServerRun`) e annotare i risultati nel manuale. Regolare i valori di `VehicleDef` finché la guida è "morbida ma piacevolmente guidabile"; ogni regolazione si riporta in `VehicleDef.JEEP` e nel test che dipende da quei numeri.
- [ ] **Step 3:** Scrivere la pagina wiki (concetti in italiano, niente schema fisso di titoli, sezione "Sperimentale" dove serve) ed eseguire la build del sito per il controllo `check-docs`.
- [ ] **Step 4:** Alzare `qrm_version` e `API_VERSION` insieme (versione minore successiva a quella corrente).
- [ ] **Step 5:** Eseguire `./gradlew test` su tutto il progetto, atteso verde. **Commit** — `feat(vehicles): fase 1, veicolo guidabile (Jeep Wrangler)`.
