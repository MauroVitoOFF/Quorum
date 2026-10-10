# qrm-locker — specifica di progetto

- Data: 2026-10-10
- Stato: approvata a voce, in attesa di rilettura. Nessun codice scritto. L'implementazione parte in un'altra sessione.
- Versione di partenza: QRM 0.14.0 (Minecraft 26.2, NeoForge 26.2.0.88, Java 25)
- Versione di arrivo: QRM 0.15.0
- Si appoggia a: [Interaction Points](2026-10-10-interaction-points-design.md) (già implementati in 0.14.0)

## 1. Obiettivo

Un Module esterno, `qrm-locker`, che offre un **armadietto personale** nei luoghi marcati dallo staff come punti di
interazione. Un Character autorizzato fa click destro sul punto, apre il proprio armadietto, deposita e ritira oggetti.
Il contenuto resta al personaggio: se cambia personaggio, se il server riparte, se il punto viene spostato.

È il primo Module che costruisce una funzionalità sull'infrastruttura dei punti usando solo l'API pubblica. Serve a
verificare che l'API basti a un mod di terzi, e a dare il primo caso con una schermata e dati propri.

Criteri di riuscita:

1. Un Character autorizzato apre il proprio armadietto da un punto di tipo `locker` e ritrova gli oggetti dopo aver
   cambiato personaggio, riavviato il server o spostato il punto.
2. Un Character non autorizzato non apre nulla (lo garantisce il Core, prima di chiamare il Module).
3. Un client senza il mod vede e usa un baule vanilla; un client con il mod vede una schermata a tema QRM. In entrambi
   i casi la logica degli slot è quella vanilla.
4. `qrm-locker` compila solo contro `qrm-api` e `qrm-client-api`, come `qrm-bank`. Il Core non cambia.
5. Il crash del server non duplica oggetti (vedi 4.3).

## 2. Decisioni prese

| Tema | Decisione |
|---|---|
| Di chi è il contenuto | Personale, per Character. Chiave: Character + proprietario del punto. |
| Più punti | Più punti dello stesso proprietario aprono lo **stesso** armadietto del Character. Proprietari diversi (es. due Job) hanno armadietti **separati**. |
| Servizio | Non richiesto (`requiresDuty = false`): serve anche a posare la divisa quando si esce di servizio. |
| Proprietari | `JOB` e `ORGANIZATION`. |
| Dimensione | 27 slot (3 righe), fissa. |
| Schermata | Ibrida: baule vanilla per tutti; il client con il mod ne sostituisce l'aspetto con una schermata a tema. |
| Dati | `ModDataService`, nessuna tabella nuova nel Core. |

## 3. Architettura

```
qrm-locker (modId qrm_locker)   dipende solo da
        ▼
qrm-api          PointTypeRegistry, PointService, ModDataService
qrm-client-api   ThemeRenderer / tema (solo per la schermata)
```

Nuovo sottoprogetto Gradle `qrm-locker`, aggiunto a `settings.gradle`, con la struttura di `qrm-bank`
(`build.gradle` con `compileOnly` su `qrm-api` e `qrm-client-api` e `runtimeOnly(project(':qrm-neoforge')) { transitive = false }`).

Il canale di rete è `optional()` e il mod **non registra blocchi, oggetti o tipi di menu**: così un client senza il mod
entra e gioca, come con QRM. In `neoforge.mods.toml` le dipendenze da `qrm` e `neoforge` sono `required`, `side = BOTH`
come in Bank (valgono solo dove il mod è installato).

Pacchetti (nomi indicativi, adattabili allo stile del codice):

- `dev.qrm.locker.LockerMod`: `@Mod`; registra il tipo di punto, il canale opzionale e gli eventi del server.
- `dev.qrm.locker.logic`: logica pura, senza Minecraft: `LockerKeys` (chiave dei dati), `LockerSlots` (stato in memoria
  come lista di slot), `LockerStorage` (interfaccia di lettura/scrittura), `LockerFlow` (regole di apertura e chiusura).
- `dev.qrm.locker.mc`: `LockerContainer` (un `SimpleContainer` che sa salvarsi e ricontrollarsi), `LockerStore`
  (`ModDataService` + conversione degli oggetti), `LockerMenus` (apre il baule).
- `dev.qrm.locker.net`: `LockerSkinPayload` (S→C, opzionale).
- `dev.qrm.locker.client` (solo client): `LockerClient` (aggancio a `ScreenEvent.Opening`) e `LockerScreen`.

### 3.1 Tipo di punto

```java
new PointType("locker", "qrm_locker.point.locker",
        Set.of(PointOwner.Kind.JOB, PointOwner.Kind.ORGANIZATION), false, handler)
```

registrato nel costruttore del mod tramite `QRM.get(PointTypeRegistry.class)`. Il handler riceve `PointUse` solo dopo
che il Core ha verificato job, grado e proprietario.

## 4. Comportamento

### 4.1 Apertura

1. Il Core autorizza il click e chiama `onUse(PointUse)` sul thread del server.
2. Il handler ricava il `ServerPlayer` dall'id del giocatore (se non è online, non fa nulla).
3. Carica il contenuto con `LockerStore` (vuoto se non esiste ancora).
4. Se il giocatore ha il canale del mod, invia `LockerSkinPayload` **prima** di aprire il menu.
5. Apre un `ChestMenu.threeRows` vanilla sopra il `LockerContainer`. Il titolo è il nome del punto, se c'è, altrimenti
   la traduzione di `labelKey`.

Un giocatore non apre due armadietti insieme: se ne ha uno aperto, il click non fa nulla.

### 4.2 Mentre è aperto

`LockerContainer.stillValid(player)` è vero solo se: il punto esiste ancora ed è lo stesso; `PointService.canUse` è
ancora vero per il Character; il giocatore è a meno di 8 blocchi dal blocco. Altrimenti il menu si chiude da solo.
Questo copre punto rimosso, grado perso, cambio di Job, personaggio archiviato e allontanamento.

`PointService.canUse` non verifica che il Character sia quello attivo del giocatore. Per questo `stillValid` controlla
anche `CharacterService.activeOf(player)`: se il Character attivo non è più quello che ha aperto l'armadietto, il menu si
chiude.

### 4.3 Salvataggio

Ogni modifica del contenuto segna il container come "da scrivere". A fine tick (`ServerTickEvent.Post`) i container da
scrivere vengono salvati; alla chiusura del menu e a `ServerStoppingEvent` si salva subito. Un crash perde al massimo le
modifiche dell'ultimo tick. Il contenuto resta nel database anche se il punto o il proprietario spariscono: il Module non
cancella mai un armadietto da solo.

### 4.4 Formato dei dati

`ModDataService.put("qrm_locker", OwnerRef.character(characterId), "locker.<job|org>.<ownerId>", json)`.

```json
{ "version": 1, "slots": [ { "slot": 0, "item": { ... } }, { "slot": 5, "item": { ... } } ] }
```

Gli slot vuoti non si scrivono. `item` è la serializzazione JSON dell'`ItemStack` con il codec di Minecraft (componenti
inclusi), così nomi, incantesimi e dati di altri mod sopravvivono. Un oggetto che non si legge più (mod rimosso) non
viene scartato in silenzio: il suo JSON resta in un elenco `unreadable` dello stesso documento e si riscrive tale e quale,
così tornando il mod l'oggetto riappare. Il documento ha un campo `version` per le migrazioni future.

### 4.5 Client

`LockerSkinPayload` (S→C, nessun campo) imposta un flag con scadenza di 2 secondi. Se, entro la scadenza, si apre una
schermata di baule da 27 slot, `ScreenEvent.Opening` la sostituisce con `LockerScreen` (un `AbstractContainerScreen`
sullo stesso menu): pannello e colori dal tema QRM, titolo dal menu. Il flag si consuma alla prima apertura; senza flag,
o su un client senza il mod, il baule resta vanilla. Gli slot e il loro comportamento non cambiano.

## 5. Errori e casi limite

- Il giocatore va offline con l'armadietto aperto: `removed()` salva subito.
- Il server si ferma con armadietti aperti: salvataggio in `ServerStoppingEvent`.
- Oggetto non leggibile al caricamento: conservato in `unreadable`, mai perso (4.4).
- Documento non valido (JSON corrotto o `version` sconosciuta): il Module **non apre** l'armadietto e non lo
  sovrascrive. Il giocatore riceve un messaggio che lo invita a contattare lo staff; il log del server riporta chiave e
  Character.
- Eccezione nel handler: la gestisce QRM (blocca il click e lo registra); il Module non apre nulla.

## 6. Compatibilità

- `qrm-api`, `qrm-core`, `qrm-neoforge`: nessuna modifica di codice. Solo versione 0.15.0 (`qrm_version` e
  `QrmVersion.API_VERSION`), per mantenerle allineate.
- Protocollo di `qrm`: invariato. Il canale `qrm_locker` ha il suo protocollo, versione 1, opzionale.
- Database: nessuna migrazione; i dati stanno in `qrm_mod_data`.

## 7. Test

- **Logica pura** (con il Core reale, sul modello di `PointServerTest`): `LockerKeys` per Job e Organization;
  `LockerFlow` con un `LockerStorage` finto: apertura con armadietto nuovo, con contenuto esistente, salvataggio alla
  chiusura, chiusura forzata quando `canUse` diventa falso; più punti dello stesso proprietario → stessa chiave,
  proprietari diversi → chiavi diverse.
- **Documento**: serializzazione e rilettura di `LockerSlots` con slot vuoti, `unreadable` preservato, `version` ignota
  rifiutata senza sovrascrivere.
- **Manuale**: `docs/locker-manual-test.md`: oggetti con nome, incantesimi e componenti; due personaggi dello stesso
  giocatore; riavvio del server; spostamento e rimozione del punto; client con e senza il mod; allontanamento; kill del
  processo durante l'uso per verificare l'assenza di duplicati.

## 8. Fuori dalla prima versione

- Numero di righe configurabile (la scelta è 27 slot fissi).
- Armadietto condiviso per proprietario (`stash`) come secondo tipo.
- Limiti per tipo di oggetto, registro delle operazioni, schermata staff per ispezionare un armadietto altrui.
- Armadietto di un'Organization con permessi di rank diversi per depositare e ritirare.

## 9. Da verificare durante l'implementazione

Non controllati sul codice o sulle API di NeoForge 26.2:

- Costruttori di `ChestMenu` e di `SimpleContainer`, e come si apre un menu con `ServerPlayer.openMenu`.
- `ScreenEvent.Opening` (nome esatto, come sostituire la schermata) e il disegno di un `AbstractContainerScreen` in
  26.2 (`GuiGraphicsExtractor`); imitare `AtmScreen` e `CharacterScreen`.
- Il codec di `ItemStack` e come ottenere i `RegistryOps` lato server per serializzare in JSON.
- Se un mod senza registri e con canale `optional()` è davvero accettato da un client vanilla (come QRM).
- Che `CharacterService.activeOf(player)` sia affidabile nel tick in cui si controlla `stillValid` (4.2).
- Dove viene ricordato lo stato "menu aperto" per impedire due armadietti insieme.

## 10. Dopo l'implementazione

- Wiki (`QRM-docs`): sezione *Modules → Locker* (Overview, Configurazione assente, Comandi assenti, API: tipo `locker` e
  formato dei dati), voce nelle versioni e nel changelog tecnico, aggiornamento di *Punti di interazione*.
- Sito: voce nella hotbar, passo della serata, parte `qrm-locker` nelle parti del progetto.
- Armadietto condiviso (`stash`) come successivo Module o secondo tipo, se serve.
