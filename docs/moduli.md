# Struttura dei moduli di QRM

Versione 0.28.0. Gradle multi-progetto per Minecraft 26.2 con NeoForge 26.2.0.88. I moduli sono 12.

## Indice

1. [Mappa delle dipendenze](#mappa-delle-dipendenze)
2. [Gli strati di base](#gli-strati-di-base)
3. [Il mod principale](#il-mod-principale)
4. [I moduli opzionali](#i-moduli-opzionali)
5. [Struttura interna di un modulo](#struttura-interna-di-un-modulo)
6. [Come sono collegati](#come-sono-collegati)
7. [Progetto di sviluppo](#progetto-di-sviluppo)
8. [Come aggiungere un modulo](#come-aggiungere-un-modulo)

---

## Mappa delle dipendenze

```
                     ┌────────────────────────────────────────────┐
                     │  qrm-neoforge  (modId "qrm")               │
                     │  il mod principale: comandi, rete, GUI     │
                     │  impacchetta api + core con jarJar         │
                     └───────┬─────────────────┬──────────────────┘
                             │ usa             │ usa
                  ┌──────────▼─────┐   ┌───────▼────────┐   ┌──────────────────┐
                  │   qrm-core     │   │ qrm-client-api │   │   qrm-api        │
                  │ implementazioni│   │ widget, HUD,   │   │ interfacce e     │
                  │ + database     │   │ temi, schermate│   │ tipi (senza MC)  │
                  └───────┬────────┘   └────────────────┘   └──────────────────┘
                          └─────────────► qrm-api ◄─────────────────────────────┘

  Moduli opzionali (ognuno è un mod a sé, "qrm_<nome>"):
  bank · locker · wardrobe · medical · immersion · police · boss
     compileOnly → qrm-api, qrm-client-api      runtimeOnly → qrm-neoforge
     (immersion ha in più compileOnly → qrm-medical)

  qrm-dev: solo per sviluppare. Carica tutti i moduli insieme (runtimeOnly).
```

## Gli strati di base

| Modulo | File | Ruolo |
|---|---|---|
| `qrm-api` | 93 | Interfacce e tipi condivisi, **senza codice di Minecraft**: `character`, `economy`, `job`, `org`, `permission`, `point`, `staff`, `emergency`, `moderation`, `player`, `data`, `event`, `id`. È il contratto che i moduli vedono a compilazione e ha una versione propria (`QrmVersion.API_VERSION`) |
| `qrm-core` | 27 | Implementazioni di quelle interfacce, più il database (`db`). Anch'esso senza Minecraft, quindi si prova nei test (31 test) |
| `qrm-client-api` | 16 | La parte grafica riusabile dai moduli: `widget`, `screen`, `hud`, `theme`, `staff` |

## Il mod principale

**`qrm-neoforge`** (`modId = "qrm"`, 105 file, 40 test) collega tutto a Minecraft:

- i comandi (`QrmCommands`, `JobCommands`, `OrgCommands`, `PointCommands`, `StaffCommands`, `LogCommands`, `ShortCommands`);
- la rete (`net`);
- le schermate e il pannello staff (`client`);
- la logica lato server (`server`), tra cui `StaffAudit` per il registro delle azioni staff.

Contiene `api` e `core` dentro il proprio jar (`jarJar`): per installare QRM basta questo file.

## I moduli opzionali

Ognuno è un mod separato: se lo togli, il resto funziona.

| Modulo | modId | File | Test | Cosa fa |
|---|---|---|---|---|
| `qrm-bank` | `qrm_bank` | 48 | 19 | Conti, bancomat, contante fisico, bonifici, blocco dei conti, audit |
| `qrm-locker` | `qrm_locker` | 17 | 4 | Armadietto personale per personaggio nei punti di tipo locker (nessun comando) |
| `qrm-wardrobe` | `qrm_wardrobe` | 26 | 8 | Divise per lavoro, che sono skin, cambiate da un armadietto |
| `qrm-medical` | `qrm_medical` | 20 | 2 | Atterramento, coma, soccorso (RCP, defibrillatore, kit), ospedale al posto della morte |
| `qrm-immersion` | `qrm_immersion` | 17 | 2 | Nomi nascosti e presentazioni, mani alzate e indicare, bussare |
| `qrm-police` | `qrm_police` | 26 | 1 | MDT portatile e terminale di centrale, anagrafe, registro armi, dispatch |
| `qrm-boss` | `qrm_boss` | 27 | 9 | Menu del boss aziendale: membri, rank e conto |

## Struttura interna di un modulo

I moduli opzionali hanno tutti le stesse cartelle sotto `dev.qrm.<modulo>`:

| Cartella | Contenuto | Minecraft? |
|---|---|---|
| `logic/` | Regole e dati puri, testabili | no |
| `mc/` | Integrazione con Minecraft: oggetti, blocchi, eventi, comandi, il mod stesso (`<Nome>Mod`) | sì |
| `net/` | Messaggi tra client e server | sì |
| `client/` | Schermate, HUD, rendering | sì (solo client) |
| `server/` | Logica lato server (solo `bank` e `boss`) | sì |
| `api/` | Punti d'ingresso per altri mod (solo `bank`) | no |

## Come sono collegati

- I moduli **non dipendono l'uno dall'altro**. L'unica eccezione è `qrm-immersion`, che vede `qrm-medical` a compilazione (`compileOnly`) per il collegamento con lo stato medico (`MedicalLink`).
- Parlano con il resto di QRM solo attraverso `qrm-api`, per esempio con `QRM.get(StaffLogService.class)`.
- Tutti registrano i propri comandi sulla stessa radice `/qrm`, e Brigadier fonde i nodi con lo stesso nome (vedi `comandi.md`).
- L'audit globale (`/qrm admin audit`) scorre i nodi figli presenti, quindi il core non conosce i moduli.
- Un modulo che vuole scrivere nel registro staff chiama `StaffLogService` dall'API: `StaffAudit` sta in `qrm-neoforge` e i moduli non lo vedono.

## Progetto di sviluppo

`qrm-dev` non ha codice: carica `qrm-neoforge` e tutti i moduli con `runtimeOnly` per `runClient` e `runServer`, e prende i dati di prova da `qrm-neoforge/dev-data`. Le cartelle di esecuzione sono `qrm-dev/run` (server) e `qrm-dev/run-client`.

## Come aggiungere un modulo

1. Crea `qrm-<nome>` con un `build.gradle` come quello degli altri moduli: `compileOnly` su `qrm-api` e `qrm-client-api`, `runtimeOnly` su `qrm-neoforge` con `transitive = false`, e `testImplementation` su `qrm-api` e `qrm-core`.
2. Aggiungilo a `settings.gradle` (`include`) e a `qrm-dev/build.gradle` (`runtimeOnly`).
3. Dai al mod un `modId` `qrm_<nome>` in `neoforge.mods.toml`.
4. Metti le regole pure in `logic/`, così sono testabili senza Minecraft.
5. Per i comandi registra un `RegisterCommandsEvent` sotto la radice `qrm`; per il registro staff usa `StaffLogService`.
6. Aggiorna `docs/comandi.md` e questo file.
