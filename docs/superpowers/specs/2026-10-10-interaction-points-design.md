# Interaction Points — specifica di progetto

- Data: 2026-10-10
- Stato: implementata in QRM 0.14.0 (vedi «Scostamenti» in fondo).
- Versione di partenza: QRM 0.13.1 (Minecraft 26.2, NeoForge 26.2.0.88, Java 25)
- Versione di arrivo: QRM 0.14.0

## 1. Obiettivo

Permettere a un Job o a una Organization di offrire funzionalità in un luogo del mondo. Lo staff va fisicamente
sul posto, marca un blocco esistente e i personaggi autorizzati lo usano con il click destro.

La prima versione consegna l'infrastruttura dei punti e una sola funzionalità: il **punto di servizio**
(On Duty / Off Duty). Le altre funzionalità (armadietto, deposito, gestione del personale) saranno mod separati
costruiti sulla stessa infrastruttura.

Criteri di riuscita:

1. Lo staff crea, modifica, sposta, elenca e rimuove punti con i comandi, senza modificare file.
2. Un personaggio autorizzato entra ed esce dal servizio cliccando un punto di servizio.
3. Un personaggio non autorizzato non vede il punto e non può usarlo.
4. QRM registra il punto di servizio usando solo l'API pubblica: un mod di terze parti può fare lo stesso senza
   modificare il Core.
5. I job e i dati già salvati non cambiano.

## 2. Modello dei concetti

| Concetto | Decisione | Nel codice oggi |
|---|---|---|
| Job | Il mestiere (equivalente a `job1` di FiveM). Invariato. | `JobDefinition`, `JobRegistry`, file `<mondo>/qrm/data/jobs/*.json` |
| Grade | Invariato. | `GradeDefinition` |
| Employment | Non è un nuovo concetto: è l'assegnazione esistente. | `Assignment`, tabella `qrm_character_job` (un job per personaggio) |
| Organization | Il gruppo (equivalente a `job2`): gang, mafie. Invariata. | `Organization`, `Rank`, `Membership`, tabelle `qrm_organization`, `qrm_org_rank`, `qrm_org_member` |
| Workplace | Non introdotto. Più sedi sono più punti dello stesso proprietario. | — |
| Feature | Un tipo di punto registrato da un mod. Non esiste un elenco di feature dentro il job. | — (nuovo: `PointType`) |
| Interaction Point | Unico concetto nuovo. | — (nuovo) |

Esempi di riferimento:

- Polizia, agente: job `police`. I suoi punti appartengono al job.
- Gang creata da un giocatore, boss: solo organizzazione. I suoi punti appartengono all'organizzazione.
- Consegna pizze: job senza organizzazione. I suoi punti appartengono al job.

L'organizzazione collegata a un job tramite `JobDefinition.orgId` resta soltanto la sua cassa: non riceve
automaticamente i punti del job.

## 3. Il punto di interazione

Un punto è un blocco esistente (di qualsiasi mod) marcato dallo staff. QRM non registra blocchi propri.

Dati di un punto:

| Campo | Significato |
|---|---|
| `id` | Intero progressivo, usato nei comandi. |
| `typeId` | Il tipo di punto, es. `duty`. |
| `owner` | Tipo di proprietario (`JOB` o `ORGANIZATION`) e suo id. |
| `position` | Nome della dimensione (es. `minecraft:overworld`) e coordinate intere x, y, z. |
| `minGradeId` | Facoltativo. Id del grado (job) o del rank (organizzazione) minimo. Assente = tutti i membri. |
| `publicVisible` | Se vero, il punto è visibile a tutti. Predefinito: falso. |
| `name` | Facoltativo. Nome scelto dallo staff, es. "Centrale di polizia". |
| `createdAt` | Istante di creazione. |

Vincoli:

- Un blocco ospita al massimo un punto (unicità su dimensione + coordinate).
- Il tipo dichiara quali proprietari accetta. Il punto di servizio accetta solo `JOB`.

### 3.1 Regola di accesso

Un personaggio può usare un punto se tutte queste condizioni sono vere:

1. È il personaggio attivo del giocatore ed è `ACTIVE`.
2. Il punto è attivo (vedi 3.3).
3. Proprietario `JOB`: il personaggio ha quel job, l'assegnazione è `known`, e il `level` del suo grado è maggiore
   o uguale a quello di `minGradeId` (se impostato).
4. Proprietario `ORGANIZATION`: l'organizzazione è `ACTIVE`, il personaggio ne è membro esplicito, e il `level`
   del suo rank è maggiore o uguale a quello di `minGradeId` (se impostato). I membri impliciti (job in servizio
   collegato all'organizzazione) non contano.
5. Se il tipo dichiara `requiresDuty` e il proprietario è un `JOB`: il personaggio è in servizio. Per le
   organizzazioni il requisito non si applica.

Non c'è alcun bypass per staff o operatori nella prima versione.

L'accesso si basa su proprietario e livello, non sui nodi permesso: così lo staff configura tutto sul posto. Un
tipo di punto può comunque controllare nodi propri dentro il suo handler.

### 3.2 Visibilità

- `publicVisible = false`: il punto viene comunicato solo ai client dei personaggi che possono usarlo.
- `publicVisible = true`: viene comunicato a tutti, ma lo usa solo chi soddisfa la regola di accesso.

Il filtro è sul server: un client non riceve mai i punti che non deve vedere.

### 3.3 Punti inattivi

Un punto resta salvato ma inattivo (invisibile ai giocatori, non utilizzabile, segnalato nell'elenco staff) se:

- il tipo non è registrato (mod rimosso);
- il proprietario non esiste più (job tolto dai file, organizzazione archiviata o assente);
- `minGradeId` non esiste più nel proprietario;
- la dimensione non esiste;
- il blocco alla posizione è aria.

Un punto inattivo non viene mai cancellato in automatico, perché un mod può avervi collegato dei dati. È lo
stesso trattamento che `DefaultJobService` riserva oggi a un'assegnazione con `known = false`.

Le prime tre condizioni si valutano nel Core. Le ultime due richiedono il mondo e si valutano in `qrm-neoforge`.

## 4. Architettura

L'infrastruttura sta dentro QRM. Le funzionalità stanno in mod esterni, uno per funzionalità e non per lavoro,
sul modello di `qrm-bank` (che compila solo contro `qrm-api` e `qrm-client-api`).

```
mod esterni (qrm-bank oggi; qrm-locker, terze parti in futuro)
        │  dipendono solo da
        ▼
qrm-api          tipi di punto, punti, servizio, eventi   (nessuna dipendenza da Minecraft)
        ▲
        │  implementato / usato da
qrm-core         tabella, regola di accesso
qrm-neoforge     comandi, click, protezione blocco, sincronizzazione, tipo "duty"
client QRM       etichetta e suggerimento
```

### 4.1 `qrm-api` — nuovo package `dev.qrm.api.point`

`qrm-api` non dipende da Minecraft: niente `BlockPos`, `Level` o `ServerPlayer`. I nomi sono indicativi; la
sessione di implementazione può adattarli allo stile del codice.

- `PointOwner(Kind kind, String id)` con `Kind { JOB, ORGANIZATION }`.
- `PointPosition(String dimension, int x, int y, int z)`.
- `InteractionPoint(...)`: il record con i campi della sezione 3.
- `PointType(String id, String labelKey, Set<PointOwner.Kind> owners, boolean requiresDuty, PointHandler handler)`.
  L'id segue il formato già usato per job e voci staff: `[a-z][a-z0-9_]{0,31}`.
- `PointHandler`: `void onUse(PointUse use)`. Chiamato sul thread del server, solo dopo che il Core ha verificato
  l'accesso.
- `PointUse(PlayerId player, CharacterId character, InteractionPoint point)`. Il mod ricava da sé il
  `ServerPlayer` dall'id del giocatore.
- `PointTypeRegistry`: `register`, `find`, `all`. Stesso comportamento di `JobRegistry`: la stessa definizione è
  ammessa, una diversa con lo stesso id lancia `IllegalArgumentException`. Disponibile in `QRM` fin dal
  costruttore del mod, come `JobRegistry` e `CurrencyRegistry`.
- `PointService`: creare, trovare per id, trovare per posizione, elencare (tutti o per proprietario), modificare
  (`minGradeId`, `publicVisible`, `name`), spostare, rimuovere, e `canUse(CharacterId, InteractionPoint)`.
  Come gli altri servizi, non controlla chi chiama. Gli esiti seguono lo stile esistente (`AssignResult`,
  `OrgResult`): tipi sigillati, niente eccezioni per gli errori previsti.
- Eventi: `PointCreatedEvent`, `PointChangedEvent`, `PointRemovedEvent`.

Sono solo aggiunte: nessun tipo esistente dell'API cambia. `JobDefinition` resta identico.

### 4.2 `qrm-core`

- Migrazione `V7__interaction_points.sql`, aggiunta in coda alla lista di `Migrator`:

```sql
CREATE TABLE qrm_interaction_point (
    id INT PRIMARY KEY,
    type_id VARCHAR(32) NOT NULL,
    owner_kind VARCHAR(16) NOT NULL,
    owner_id VARCHAR(32) NOT NULL,
    dimension VARCHAR(128) NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    z INT NOT NULL,
    min_grade_id VARCHAR(32),
    public_visible INT NOT NULL,
    name VARCHAR(64),
    created_at BIGINT NOT NULL,
    UNIQUE (dimension, x, y, z)
);
CREATE INDEX idx_qrm_interaction_point_owner ON qrm_interaction_point(owner_kind, owner_id);
```

- L'id si assegna nella transazione di creazione (massimo esistente + 1), senza funzioni specifiche di SQLite.
- `InMemoryPointTypeRegistry` e `DefaultPointService`, registrati da `QrmCore.start` e aggiunti a
  `SERVICE_TYPES` per la deregistrazione.
- La regola di accesso (3.1) vive qui, usando `JobService`, `JobRegistry` e `OrganizationService`.
- Validazione alla creazione: tipo registrato, proprietario ammesso dal tipo, proprietario esistente,
  `minGradeId` esistente nel proprietario, posizione libera.

### 4.3 `qrm-neoforge` — lato server

**Comandi**, sotto `/qrm admin point`, quindi già riservati agli operatori (`QrmCommands.isOp`), ciascuno
avvolto in `StaffAudit.audited(...)` come gli altri comandi amministrativi:

| Comando | Effetto |
|---|---|
| `create <tipo> job\|org <proprietario> [grado]` | Crea un punto sul blocco inquadrato. |
| `set grade <grado>\|none` | Cambia il grado minimo del punto inquadrato. |
| `set public on\|off` | Cambia la visibilità. |
| `set name <testo>\|none` | Cambia il nome. |
| `move <id>` | Sposta il punto sul blocco inquadrato. I dati collegati restano. |
| `remove [id]` | Rimuove il punto inquadrato o quello indicato. |
| `list [job\|org <proprietario>]` | Elenca id, tipo, proprietario, posizione e stato (attivo / motivo di inattività). |
| `info` | Dettagli del punto inquadrato. |

La logica dei comandi sta in una classe separata dal parsing, così la futura schermata staff la riusa (è lo
schema di `StaffBankLogic` in `qrm-bank`).

**Interazione.** Al click destro su un blocco, lato server:

1. Se alla posizione non c'è un punto attivo, non succede nulla e il blocco si comporta normalmente.
2. Se c'è, l'uso normale del blocco viene annullato (una cassa marcata non si apre).
3. Controlli nell'ordine già usato da `AtmActions`: rate limit, personaggio attivo, regola di accesso.
4. Se l'accesso è negato, il giocatore riceve un messaggio; altrimenti viene chiamato l'handler del tipo.

Il click va gestito una sola volta, non una per mano.

**Protezione.** Rompere un blocco marcato è impedito a chiunque, con un messaggio che invita a rimuovere prima
il punto. Esplosioni, pistoni e modifiche esterne alla mappa non vengono intercettati: se il blocco sparisce il
punto diventa inattivo (3.3).

**Sincronizzazione.** Un nuovo pacchetto server → client con l'elenco dei punti visibili a quel giocatore: id,
dimensione, posizione, chiave dell'etichetta del tipo, nome facoltativo. L'elenco viene ricalcolato e reinviato
quando cambia qualcosa che influisce sulla visibilità, riusando lo schema di `StatusSync` (marcatura "sporco" e
invio al tick): ingresso del giocatore, `CharacterSelectedEvent`, `JobChangedEvent`, `DutyChangedEvent`,
`MembershipChangedEvent`, `OrganizationArchivedEvent`, e i tre nuovi eventi dei punti. L'elenco ha una
dimensione massima, come gli altri payload. `QrmNetwork.PROTOCOL_VERSION` passa da `"11"` a `"12"`.

**Tipo `duty`.** Registrato da `QrmMod` nel costruttore, tramite `PointTypeRegistry`, come farebbe un mod
qualsiasi: proprietari ammessi `JOB`, `requiresDuty = false`. L'handler inverte il servizio con
`JobService.setOnDuty` e comunica l'esito al giocatore. Funziona anche con `selfDuty = false`: è il caso d'uso
previsto dal messaggio esistente "Il servizio si gestisce in azienda". L'opzione `selfDuty` non cambia
significato: continua a governare solo il comando `/duty` e il pulsante dell'hub.

### 4.4 Client QRM

- Conserva l'elenco ricevuto.
- Disegna un'etichetta sopra i punti vicini, nella dimensione corrente: il nome scelto dallo staff se presente,
  altrimenti il nome tradotto del tipo.
- Quando il giocatore inquadra un punto, mostra il suggerimento "Click destro: <nome>".
- L'aspetto segue il tema esistente. Un client senza il mod non riceve nulla e gioca normalmente: il canale
  resta opzionale.

## 5. Scenari di verifica

Sono i sei scenari discussi. Solo il secondo fa parte della prima versione; gli altri verificano che
l'architettura li regga senza cambiare il Core.

1. **Armadietto della polizia.** Mod `qrm-locker` registra il tipo `locker`. Lo staff marca un mobile in
   centrale con proprietario `job police`. Click → il Core verifica job e grado → handler del mod → schermata e
   dati del mod.
2. **Entrata in servizio.** Tipo `duty` incluso. Click → verifica job → `JobService.setOnDuty` →
   `DutyChangedEvent` → `StatusSync` aggiorna l'HUD e l'elenco dei punti visibili.
3. **Deposito condiviso.** Un mod registra `stash` con `requiresDuty = true`. Il contenuto è salvato dal mod
   con chiave il proprietario, quindi più punti dello stesso proprietario aprono lo stesso deposito.
4. **Gestione del personale.** Un mod registra `management`; lo staff imposta un grado minimo. L'handler apre
   una schermata che chiama `JobService.assign`/`dismiss` oppure `OrganizationService.addMember`/`setRank`,
   secondo il tipo di proprietario. I controlli "non puoi promuovere sopra il tuo grado" stanno nel mod.
5. **Più sedi.** Lo stesso proprietario ha più punti, anche in dimensioni diverse. Nessun concetto aggiuntivo.
6. **Mod di terze parti.** Registra il proprio tipo nel costruttore. Riceve dal Core posizioni, comandi staff,
   accesso ed etichette. Se viene rimosso, i suoi punti restano salvati e inattivi.

## 6. Compatibilità e migrazione

- **Job esistenti:** invariati. `JobDefinition`, i file JSON e `qrm_character_job` non cambiano.
- **Dati salvati:** si aggiunge una tabella; nessun dato esistente viene trasformato.
- **API:** solo aggiunte. `QrmVersion.API_VERSION` e `qrm_version` passano a `0.14.0`.
- **Rete:** l'aumento del protocollo richiede che client e server siano aggiornati insieme, come nei
  cambi precedenti.
- **Mod esistenti:** `qrm-bank` e `qrm-example` non richiedono modifiche.

## 7. Test

- **Core, con SQLite reale** (sul modello di `JobsEnv` e `OrgsEnv`): creazione, duplicato sulla stessa
  posizione, spostamento, rimozione, modifica; matrice della regola di accesso (job giusto/sbagliato, grado
  sotto/sopra il minimo, in servizio o no, membro esplicito o implicito, organizzazione archiviata,
  personaggio non attivo); ogni causa di inattività valutabile nel Core; eventi emessi.
- **Registro dei tipi:** stessa definizione ammessa, definizione diversa rifiutata, id non valido.
- **Migrazione:** `MigratorTest` aggiornato alla versione 7.
- **`qrm-neoforge`, logica pura:** codec del nuovo payload, filtro di visibilità per giocatore, logica dei
  comandi, handler del tipo `duty`.
- **Manuale:** un documento `docs/manual-tests/points.md`, come quelli esistenti per banca e GUI, che copra
  click, blocco protetto, etichetta, due giocatori con job diversi.

## 8. Fuori dalla prima versione

- Armadietto, deposito condiviso, inventari personali, gestione del personale.
- Creazione di gang da parte dei giocatori.
- Limite di una organizzazione per personaggio (previsto come opzione, predefinito 1).
- Schermata staff per i punti e permesso staff dedicato.
- Marker a coordinate senza blocco, interazione con entità o NPC.
- Possibilità per lo staff di cambiare `requiresDuty` sul singolo punto.

## 9. Da verificare durante l'implementazione

Questi punti non sono stati controllati sul codice o sulle API di NeoForge 26.2:

- I nomi esatti degli eventi NeoForge per il click destro su un blocco e per la rottura di un blocco, e come
  annullare l'uso normale del blocco.
- Il modo migliore per ottenere lato server il blocco inquadrato dallo staff.
- Gli agganci di rendering del client per l'etichetta nel mondo e come si integrano con `HudOverlay` e i temi.
- Se `qrm-api` e `qrm-core` sono effettivamente inclusi nel jar di `qrm-neoforge` (dedotto dalla build, non
  ispezionato).
- Se serve un nuovo valore di `DutyReason` per distinguere il servizio preso da un punto, o se basta `MANUAL`.

## 10. Dopo l'implementazione

- Aggiornare la documentazione pubblica nel repository separato `QRM-docs`.
- Primo mod esterno consigliato: `qrm-locker`, perché è il primo caso che richiede una schermata e dati propri.

## 11. Scostamenti dall'implementazione (0.14.0)

- **Icona nel mondo:** non c'è l'etichetta a schermo né il suggerimento sotto il mirino. Il client disegna una
  targhetta 32x32 sulle facce laterali del blocco (`SubmitCustomGeometryEvent`), illuminata dalla luce del mondo.
- **Faccia:** ogni punto ha un campo `face` (`null` = tutte e quattro le laterali). `point create` parte dalla faccia
  inquadrata; `point set face all|here|north|south|east|west` la cambia. Migrazione `V8__interaction_point_face.sql`
  (la V7 era già applicata ai mondi di sviluppo).
- **Servizio:** il tipo `duty` usa `DutyReason.MANUAL`; nessun nuovo valore dell'enum.
- **Avvio di sviluppo:** `qrm-neoforge` ha ora la run `client`, il server di sviluppo legge la tastiera e `seedDevData`
  copia valute e lavori di prova da `qrm-neoforge/dev-data`.
