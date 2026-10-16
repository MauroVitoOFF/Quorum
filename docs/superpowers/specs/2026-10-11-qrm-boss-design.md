# qrm-boss — specifica di progetto

- Data: 2026-10-11
- Stato: da rivedere (spec scritta, nessuna implementazione). La grafica è già pronta: vedi 6.
- Versione di partenza: QRM 0.15.0 (Minecraft 26.2, NeoForge 26.2.0.88, Java 25)
- Versione di arrivo: QRM 0.16.0
- Si appoggia a: [Interaction Points](2026-10-10-interaction-points-design.md) e [qrm-locker](2026-10-10-qrm-locker-design.md)

## 1. Obiettivo

Un Module esterno, `qrm-boss`, con il **menu del boss aziendale**: una schermata a tema QRM con cui chi ha i permessi
giusti gestisce la propria organizzazione senza comandi: vede lo stato dell'azienda, gestisce membri e rank,
deposita e preleva dal conto aziendale.

Si apre in due modi: il comando `/boss [azienda]` e un nuovo tipo di punto di interazione, la **scrivania del boss**
(`boss_desk`), che lo staff piazza nel mondo.

Criteri di riuscita:

1. Un Character con `boss.menu` apre il menu da `/boss` o dalla scrivania e vede solo le sezioni per cui ha un nodo.
2. Dal menu può aggiungere, promuovere e licenziare membri, nei limiti del proprio rank (4.4).
3. Può depositare e prelevare dal conto dell'azienda; ogni operazione è una transazione registrata dal Core.
4. Un client modificato non ottiene nulla senza il nodo: il server ricontrolla tutto a ogni azione.
5. `qrm-boss` compila solo contro `qrm-api` e `qrm-client-api`, come `qrm-bank` e `qrm-locker`. Il Core non cambia.

## 2. Decisioni prese

| Tema | Decisione |
|---|---|
| Sezioni | Panoramica, Membri, Conto. Fuori: punti dell'azienda, log, rank editor (8). |
| Chi è "boss" | Chi ha i nodi giusti, tramite i rank: nessun concetto nuovo. Nodi `boss.*` (4.1). |
| Prelievo | Riusa il nodo esistente `org.account.withdraw` richiesto da `OrganizationBankService`. |
| Apertura | `/boss [azienda]` e punto `boss_desk` (proprietario solo `ORGANIZATION`). Niente accesso dall'Hub. |
| Più aziende | `/boss` senza argomento: una sola azienda con `boss.menu` la apre; più di una apre un selettore. |
| Dati | Nessuna tabella e nessun `ModDataService`: il modulo non ha stato proprio. |
| Fonte di verità | Il server. Il client mostra l'ultimo snapshot e manda richieste. |
| Valuta | Una sola in v1 (4.5). |

## 3. Architettura

```
qrm-boss (modId qrm_boss)       dipende solo da
        ▼
qrm-api          OrganizationService, OrganizationBankService, AccountService,
                 TransactionService, CharacterService, PointTypeRegistry, PermissionService
qrm-client-api   QrmScreen, ThemeRenderer
```

Nuovo sottoprogetto Gradle `qrm-boss`, aggiunto a `settings.gradle` e a `qrm-dev`, con lo stesso `build.gradle` di
`qrm-locker` (`compileOnly` su `qrm-api` e `qrm-client-api`, `runtimeOnly(project(':qrm-neoforge')) { transitive = false }`)
e un `neoforge.mods.toml` che dipende da `qrm` e `neoforge`.

Pacchetti sotto `dev.qrm.boss`:

| Pacchetto | Contenuto |
|---|---|
| `logic/` | Regole senza Minecraft. `BossPermissions` (nodi, sezioni visibili), `BossService` (snapshot e azioni), `BossSnapshot` e record collegati, `BossResult`, `AmountSpec` (500 / 25% / 50% / TUTTO / libero), limitatore di frequenza. |
| `net/` | `BossNetwork` (protocollo `"1"`, `optional()`), payload di stato e di azione, codec. |
| `server/` | `BossCommands` (`/boss`), `BossServer` (apre e applica le azioni), `BossDeskPoint` (handler del punto). |
| `client/` | `BossClient` (`@Mod(dist = CLIENT)`, tema), `BossClientBridge`, `BossScreen` e le sezioni, `OrgPickerScreen`, widget grafici. |
| `mc/` | `BossMod` (registra rete e tipo di punto). |

`BossService` riceve i servizi dell'API nel costruttore, come `BankOperations`, ed è testabile con l'ambiente di test
del Core (come `BankEnv`).

## 4. Comportamento

### 4.1 Permessi

| Nodo | Effetto |
|---|---|
| `boss.menu` | Apre il menu e mostra la Panoramica. |
| `boss.members.manage` | Mostra la sezione Membri e abilita le azioni. Senza, la sezione non compare. |
| `boss.bank.view` | Mostra il saldo nella sezione Conto. |
| `boss.bank.deposit` | Abilita il deposito. |
| `org.account.withdraw` | Abilita il prelievo (nodo esistente del Core). |

La sezione Conto compare se c'è almeno uno tra `boss.bank.view`, `boss.bank.deposit` e `org.account.withdraw`; le azioni
non permesse restano visibili ma disabilitate (`button_disabled`). I nodi si assegnano ai rank con i pattern già
supportati: il rank del boss riceve `boss.*` e `org.account.*`.

Tutti i controlli usano `OrganizationService.can(character, orgId, node)`, che già esclude aziende archiviate e
personaggi non attivi.

### 4.2 Apertura

1. `/boss [azienda]` risolve il Character attivo del giocatore. Senza argomento, calcola le aziende con `boss.menu`:
   zero → messaggio di errore, una → apre, più → invia l'elenco e il client mostra `OrgPickerScreen`. Con argomento,
   completa solo le aziende dell'utente; l'argomento non valido dà errore senza dettagli sulle aziende altrui.
2. La scrivania passa da `PointHandler`: il Core ha già verificato l'accesso al punto; il modulo ricontrolla `boss.menu`
   sull'azienda proprietaria del punto e apre quella.
3. Il server invia `BossStatePayload` e il client apre `BossScreen`. L'apertura sul client usa un ponte
   (`BossClientBridge`) come negli altri moduli, così il server dedicato non carica classi client.

### 4.3 Stato e azioni

`BossStatePayload` è uno snapshot completo: id e nome dell'azienda, stato, rank e livello di chi guarda, sezioni
abilitate, numero di membri, saldo (assente senza `boss.bank.view`), valuta, elenco membri con rank e flag
"modificabile", rank assegnabili e candidati aggiungibili. Dopo ogni azione il server rimanda lo snapshot aggiornato
con un esito (chiave di traduzione). Il client non applica modifiche in locale.

`BossActionPayload` ha un'azione e gli argomenti: `ADD_MEMBER`, `SET_RANK`, `REMOVE_MEMBER`, `DEPOSIT`, `WITHDRAW`,
`REFRESH`. Il server, per ogni richiesta: verifica il Character attivo e l'azienda, ricontrolla il nodo, applica le
regole di dominio, esegue, rimanda lo stato. Una frequenza massima per giocatore (come `ActionLimiter` della banca)
scarta le raffiche.

### 4.4 Membri

- Si può agire solo su membri con livello ≤ al proprio e mai su se stessi.
- Si assegnano solo rank con livello ≤ al proprio.
- **Azienda con un Job collegato** (ogni Job dei file ha la sua azienda con lo stesso id; altrimenti l'unico Job con
  `orgId` uguale all'azienda): i membri sono i Character con quel Job, anche fuori servizio, e i "rank" sono i gradi.
  Chi guarda deve avere un grado di quel Job (e il nodo, che gli arriva dal grado se è in servizio). `ADD_MEMBER` assegna
  il grado più basso con `JobService.assign`, `SET_RANK` cambia il grado con `setGrade`, `REMOVE_MEMBER` licenzia con
  `dismiss`. I livelli sono quelli dei gradi. Si può assumere solo chi non ha lavoro (o ha un lavoro non più definito):
  chi ha un altro lavoro dà `HAS_JOB`, perché assegnare un job diverso lo toglierebbe all'altra azienda.
  Gli `OrganizationService.addMember/setRank/removeMember` non vengono chiamati.
- **Azienda senza Job** (gang, gruppi): serve un'appartenenza **esplicita** con un rank; chi arriva al menu solo con un
  Job collegato a un'altra azienda non ha un livello e vede la sezione in sola lettura.
- I candidati da aggiungere sono i Character attivi dei giocatori online che non sono già membri (e, per un Job, senza
  lavoro); vengono aggiunti con il rank o grado assegnabile di livello più basso, poi si cambiano con `SET_RANK`.
- Licenziare richiede una seconda conferma nell'interfaccia (il pulsante diventa "Conferma?").
- Senza Job le chiamate sono `addMember`, `setRank` e `removeMember` di `OrganizationService`, con gli eventi di
  appartenenza del Core; con un Job sono `assign`, `setGrade` e `dismiss` di `JobService`, con i suoi eventi.

### 4.5 Conto

- Il saldo è quello del conto dell'azienda (`Organization.account()`), nella valuta del menu. In v1 la valuta è quella
  registrata in `CurrencyRegistry`; se ce ne sono più di una, si usa la prima, e il selettore di valuta è fuori dalla v1.
- **Deposito:** trasferimento dal conto personale del Character al conto dell'azienda con `TransactionService`
  (nessuna eccezione di permesso nel Core: chiunque può trasferire verso il conto). La causa è "Deposito aziendale".
- **Prelievo:** `OrganizationBankService.transfer(by, orgId, contoPersonale, importo, chiave)`.
- Le chiavi di idempotenza sono `org:<orgId>:<deposit|withdraw>:<uuid>`, con un uuid per ogni clic, come richiede il
  contratto del servizio.
- Gli importi sono 500, 25%, 50%, TUTTO e un valore libero da un campo di testo. Le percentuali si calcolano sul saldo
  di partenza (conto personale per il deposito, aziendale per il prelievo) e si arrotondano per difetto; importi
  nulli, negativi o oltre il saldo danno `INVALID_AMOUNT` o `INSUFFICIENT_FUNDS` senza eseguire.
- Il conto personale viene letto con `accountsOf(OwnerRef.character(id))`; se non esiste il deposito dà errore e non lo
  crea (lo apre la banca al primo uso).

### 4.6 Scrivania del boss

Tipo di punto `boss_desk`: `owners = {ORGANIZATION}`, `requiresDuty = false`, `labelKey = "qrm_boss.point.desk"`.
Un punto di proprietà di un job o di un Character non è ammesso dal Core. L'icona è `icon_desk`.

## 5. Errori e casi limite

| Caso | Esito |
|---|---|
| Nessun Character attivo | Errore, non si apre. |
| Azienda archiviata o inesistente | Errore; se il menu era aperto, si chiude con messaggio. |
| Perdita del nodo a menu aperto | La prossima azione è rifiutata e lo snapshot ricalcolato (sezione sparisce). |
| Cambio di Character a menu aperto | Le azioni sono rifiutate (il Character attivo non è più quello dello snapshot). |
| Doppio clic su deposito/prelievo | Chiavi diverse per clic; il limitatore di frequenza scarta le raffiche. |
| Retry dello stesso pacchetto | Stessa chiave: il Core risponde `Duplicate` e non duplica. |
| Rank o membro rimosso da un altro mentre si agisce | `UnknownRank` / `NotMember` del Core, mostrati come errore. |
| Il proprietario della scrivania non è più valido | Gestito dal Core (punto inattivo). |

I messaggi sono chiavi di traduzione e non rivelano dettagli di altre aziende.

## 6. Grafica

Già realizzata, in 29 texture (cornice, placca titolo, linguette, riquadri, righe, pulsanti in tre varianti, barra
saldo, icone). I sorgenti sono in `design/pixel-art/boss/` (`qrm_boss_ui.bbmodel`, `boss_gen.js`, anteprime).

- In implementazione le texture si spostano da `qrm-neoforge/.../textures/gui/boss/` (dove sono ora, come bozza) a
  `qrm-boss/src/main/resources/assets/qrm_boss/textures/gui/`, come fa `qrm-bank` per i suoi asset.
- Slice dei 9-slice: `frame` 8, `title_plate` 6, `inset` 6, `row*` 4, `button*` 4, `balance_plate` 6.
- Le sezioni usano le linguette a sinistra della cornice (28×20, ognuna con la propria icona); la selezionata è
  `tab_selected`. Gli stati hover e premuto usano le texture dedicate.
- **Mascotte "il Boss"** (`boss_mascot.png`, sorgente `design/pixel-art/boss/mascot_gen.py`): un personaggio chibi in stile
  pixel art (testa grande, occhi grandi), con corona, completo scuro e cravatta rossa; saluta con una mano e lancia una moneta con l'altra.
  È **animata**: uno spritesheet verticale di 8 frame da 32×40 px (immagine 32×320), con saluto ampio, moneta che
  sale, gira e ricade, corpo che si abbassa quando la riprende, battito di ciglia, bocca che si apre e piede che batte
  il tempo. Il ciclo dura circa 1,1 s (140 ms per frame). La texture non usa
  il `.mcmeta` di Minecraft: la schermata sceglie il frame dal tempo, con `v = frame * 40`, e lo disegna senza scalarlo.
  Sta in piedi sul bordo superiore della cornice, a destra della placca del titolo, con i piedi sul legno (circa 2 px
  di sovrapposizione); non occupa spazio dell'area contenuti. È solo decorativa.
- Il tema è quello di QRM per il fondo (`QrmScreen`); le texture del modulo coprono il resto.

## 7. Compatibilità

- Il canale di rete ha protocollo proprio, versionato separatamente da QRM, ed è `optional()`: un client senza il
  modulo non è obbligato a installarlo, ma il menu non si apre senza.
- Nessuna migrazione dati e nessuna modifica al Core.
- Se `qrm-boss` è assente, l'azienda funziona come oggi, senza menu.

## 8. Fuori dalla prima versione

- Punti di interazione dell'azienda (lista, creazione, rimozione) nel menu.
- Editor di rank e permessi dei rank.
- Storico delle transazioni e log delle azioni del boss.
- Selettore di valuta e conti multipli.
- Apertura dall'Hub (scartata nel design).
- Trasferimenti da azienda ad altri conti diversi dal personale.

## 9. Da verificare durante l'implementazione

1. Se `qrm-client-api` espone widget a tema riusabili o se il modulo deve disegnare i propri con `GuiGraphics`
   (probabile, come fa `qrm-bank`).
2. ~~Il limite "membri solo con appartenenza esplicita"~~ Risolto: con un Job collegato i membri sono i titolari del Job
   e il livello è quello del grado (4.4).
3. Come ottenere i nomi dei Character per l'elenco membri e per i candidati (`CharacterService.find`).
4. Il comportamento di `can()` con pattern (`boss.*`) sui rank: va coperto da un test dedicato.
5. La resa delle texture in gioco (la verifica in anteprima non è stata possibile a risoluzione reale).

## 10. Test

- **Unit (`BossService`, `BossPermissions`):** sezioni visibili per ogni combinazione di nodi; regole su livelli e
  su se stessi; deposito e prelievo (importi, percentuali, saldo insufficiente, chiavi duplicate); azienda archiviata;
  personaggio non attivo.
- **Rete:** round-trip di ogni payload (come `StaffBankPayloadsTest`).
- **Traduzioni:** `LangCoverageTest` su `it_it.json` e `en_us.json`, come negli altri moduli.
- **Asset:** un test che verifica la presenza e le dimensioni delle texture elencate (come `StaffBankAssetsTest`).
- **Manuale in gioco:** `docs/manual-tests/boss.md` con `/boss`, scrivania, due giocatori (il boss e un manager con
  nodi parziali) e i casi limite di 5.

## 11. Dopo l'implementazione

- Aumento di versione a 0.16.0 (`qrm_version` e `API_VERSION`).
- Aggiunta al comando di sviluppo (`dev-start`) e a `qrm-dev`, come per `qrm-locker`.
- Pagina nella wiki `QRM-docs` (sito pubblico separato), con i nodi `boss.*` e lo stato Stable.
