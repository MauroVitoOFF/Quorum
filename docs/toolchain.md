# Toolchain QRM

Fonte: MDK https://github.com/NeoForgeMDKs/MDK-26.2-ModDevGradle (clonato fuori dal progetto, letto, solo wrapper copiato).

| Voce | Valore |
|---|---|
| minecraft_version | 26.2 |
| minecraft_version_range | [26.2] |
| neo_version | 26.2.0.88 |
| moddev_version | 2.0.148 (`id 'net.neoforged.moddev' version '2.0.148'`) |
| Gradle wrapper | 9.2.1 (distribution `gradle-9.2.1-bin.zip`; `./gradlew --version`: Launcher JVM 25.0.4 Oracle) |
| Java | 25 (`java.toolchain.languageVersion = JavaLanguageVersion.of(25)`) |
| Maven plugin portal | `gradlePluginPortal()` (settings.gradle, `pluginManagement.repositories`); plugin `org.gradle.toolchains.foojay-resolver-convention` 1.0.0 in `plugins {}` di settings.gradle |

## Stile build dell'MDK

- `plugins { id 'java-library'; id 'maven-publish'; id 'net.neoforged.moddev' version '2.0.148'; id 'idea' }`
- `version = mod_version`, `group = mod_group_id`, `base { archivesName = mod_id }`
- `repositories {}` vuoto (nessun repo extra; NeoForge e' risolto dal plugin).
- Blocco `neoForge { version = project.neo_version; runs { client { client(); systemProperty 'neoforge.enabledGameTestNamespaces', project.mod_id } server { server(); programArgument '--nogui' } gameTestServer { type = "gameTestServer" } data { clientData(); programArguments.addAll '--mod', project.mod_id, '--all', '--output', file('src/generated/resources/').getAbsolutePath(), '--existing', file('src/main/resources/').getAbsolutePath() } configureEach { systemProperty 'forge.logging.markers','REGISTRIES'; logLevel = org.slf4j.event.Level.DEBUG } }; mods { "${mod_id}" { sourceSet(sourceSets.main) } } }`
  - Nota: `client()`/`server()`/`clientData()` (non `type = "client"`); `gameTestServer` usa `type = "gameTestServer"`.
- AT opzionale: `accessTransformers = project.files(...)` (auto-detect di META-INF/accesstransformer.cfg).
- `configurations { runtimeClasspath.extendsFrom localRuntime }` per dipendenze runtime-only non pubblicate.
- `dependencies {}`: nessuna dipendenza nell'MDK (solo esempi commentati: `compileOnly`, `localRuntime`, `implementation files(...)`, `implementation project(":x")`). Nessun uso di jarJar nell'MDK: va dichiarato dal Task che lo richiede (ModDevGradle: `jarJar(...)` / `additionalRuntimeClasspath`, da verificare allora).
- Metadati: task `generateModMetadata` (ProcessResources) con `expand replaceProperties` da `src/main/templates` verso `build/generated/sources/modMetadata`; `sourceSets.main.resources.srcDir generateModMetadata`; `neoForge.ideSyncTask generateModMetadata`. Proprieta' espanse: minecraft_version, minecraft_version_range, neo_version, mod_id, mod_name, mod_license, mod_version.
- `sourceSets.main.resources { srcDir('src/generated/resources'); exclude("**/*.bbmodel"); exclude("src/generated/**/.cache") }`
- `tasks.withType(JavaCompile) { options.encoding = 'UTF-8' }`
- Template `src/main/templates/META-INF/neoforge.mods.toml` (non in resources/): dipendenze `neoforge` con `versionRange="[${neo_version},)"` e `minecraft` con `${minecraft_version_range}`.

## gradle.properties dell'MDK

```
org.gradle.jvmargs=-Xmx1G
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
minecraft_version=26.2
minecraft_version_range=[26.2]
neo_version=26.2.0.88
mod_id=examplemod
mod_name=Example Mod
mod_license=All Rights Reserved
mod_version=1.0.0
mod_group_id=com.example.examplemod
```

## Differenze API (verificate dai sorgenti dell'MDK ExampleMod/Config)

Confermate uguali alla linea 1.21.x:
- `@Mod(MODID)` con costruttore `(IEventBus modEventBus, ModContainer modContainer)`; import `net.neoforged.fml.common.Mod`.
- `NeoForge.EVENT_BUS` in `net.neoforged.neoforge.common.NeoForge`; `.register(this)` + `@SubscribeEvent` (`net.neoforged.bus.api.SubscribeEvent`); `IEventBus` in `net.neoforged.bus.api`.
- `ServerStartingEvent` in `net.neoforged.neoforge.event.server.ServerStartingEvent` (stesso package della linea 1.21.x; `ServerStoppedEvent` e' nello stesso package, non usato nell'MDK).
- `modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC)`.
- `ModConfigSpec` in `net.neoforged.neoforge.common.ModConfigSpec` (Builder, BooleanValue, IntValue, ConfigValue, `defineInRange`, `defineListAllowEmpty`).
- `@EventBusSubscriber` in `net.neoforged.fml.common.EventBusSubscriber`; `ModConfigEvent` in `net.neoforged.fml.event.config`.

Package esatti (da usare negli import):
- `net.neoforged.fml.ModContainer`  (NON `net.neoforged.fml.common.ModContainer`)
- `net.neoforged.fml.config.ModConfig`  (`ModConfig.Type.COMMON`)
- `net.neoforged.fml.common.Mod`

Differenze Minecraft rilevate:
- `ResourceLocation` e' sostituito da `net.minecraft.resources.Identifier` (`Identifier.parse("minecraft:iron_ingot")`). Usare `Identifier` ovunque.
- `FoodProperties.Builder.saturationModifier(float)`, `DeferredRegister.Blocks/Items`, `registerSimpleBlock/Item` (non rilevanti per QRM).

NON verificabili dall'MDK (l'esempio non li usa; vanno controllati sui jar/sorgenti decompilati al Task 1/10, quando il plugin scarica Minecraft+NeoForge). Ipotesi da linea 1.21.11/26.x, da confermare:
- `RegisterCommandsEvent` (`net.neoforged.neoforge.event.RegisterCommandsEvent`), `ServerStoppedEvent` (`...event.server`), `PlayerEvent.PlayerLoggedInEvent/PlayerLoggedOutEvent` (`...event.entity.player.PlayerEvent`, accessor `getEntity()`).
- Permessi comandi: dalla 1.21.11 `CommandSourceStack.hasPermission(int)` e' stato sostituito da un sistema `Permission`/`PermissionSet`; per richiedere op livello 2 usare `.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTER))` (o `source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)`). NON usare `hasPermission(2)` senza averlo verificato in compilazione.
- `LevelResource.ROOT` / `MinecraftServer.getWorldPath(LevelResource)`: da verificare.
- `sendSuccess(Supplier<Component>, boolean)`, `sendFailure(Component)`, `EntityArgument.player()/getPlayer`, `ServerPlayer.getUUID()`: da verificare (possibile `getUUID()` ancora valido; `Entity#getUUID`).

## Differenze API verificate (Task 10/11, compilazione + avvio server dev su 26.2.0.88)

Compilati senza modifiche (ipotesi confermate): `RegisterCommandsEvent`, `ServerStartingEvent`/`ServerStoppedEvent`, `PlayerEvent.PlayerLoggedInEvent/PlayerLoggedOutEvent` con `getEntity()`, `LevelResource.ROOT` + `getWorldPath`, `sendSuccess(Supplier<Component>, boolean)`, `sendFailure`, `EntityArgument.player()/getPlayer`, `ServerPlayer.getUUID()`, `ModContainer.registerConfig`, `ModConfigSpec`.

Modifiche necessarie:
- Permesso op livello 2: `s.hasPermission(2)` -> `s.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)` (in `QrmCommands`).
- jarJar: nel blocco di una dipendenza di progetto, `{ version { ... } }` chiama `project.version(Closure)` e corrompe la versione del progetto (jar chiamato `qrm-build_...closure...jar`). Per i progetti si usa `jarJar(project(':qrm-api'))` senza closure (range `[versione,)` derivato automaticamente); la closure `version {}` resta solo per `sqlite-jdbc`.
- `org.gradle.configuration-cache` e `org.gradle.caching` restano attivi: compatibili con ModDevGradle 2.0.148 e jarJar.

## Task 12 (qrm-example, mod di terzi che dipende solo da qrm-api)

- Compilati senza modifiche su 26.2.0.88: `RegisterCommandsEvent`, `Commands.literal`, `CommandSourceStack.getPlayerOrException()/sendSuccess/sendFailure`, `ServerPlayer.getUUID()`, `Component.literal`, `NeoForge.EVENT_BUS.addListener`. Nessuna differenza API rispetto alle ipotesi.
- `compileOnly project(':qrm-api')` + `runtimeOnly project(':qrm-neoforge')` funziona in dev: `:qrm-example:runServer` carica il jar `qrm-neoforge/build/libs/qrm-*.jar` (con jar-in-jar) insieme al mod di esempio.
- Metadati jarJar del jar QRM: `qrm-api` e `qrm-core` con range `[0.1.0-SNAPSHOT,)`, `sqlite-jdbc` `[3.46,4)`.
- Visibilita' classi: il mod di esempio (solo `qrm-api` a compile time) costruisce senza `NoClassDefFoundError`/`ClassNotFoundException` con QRM come singolo jar jar-in-jar; il fallback (`tasks.jar { from project(':qrm-api').sourceSets.main.output }`) NON e' stato necessario e non e' applicato.

## Task 13 (japicmp)

- Plugin `me.champeau.gradle.japicmp` versione `0.4.6` (ultima sul Gradle Plugin Portal; la 0.4.4 del piano esiste ma e' superata). Compatibile con Gradle 9.2.1 / Java 25 e con la configuration cache. Il task `japicmp` viene registrato solo con `-PapiBaseline=<versione>`.
