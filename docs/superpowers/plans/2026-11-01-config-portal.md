# Portale di configurazione — piano di implementazione

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Una pagina del sito (`/configuratore/`) in cui il server owner trascina `qrm-common.toml`, `qrm_bank-server.toml` o `cash.json`, li modifica con campi guidati e li riscarica validi.

**Architecture:** Logica pura in moduli `.mjs` (lettore/scrittore TOML "sul posto", schema, validazione, modello `cash.json`, sessione di modifica) testati con `node --test`; l'interfaccia è una pagina Astro con uno script client che usa solo questi moduli. Nessun upload: tutto nel browser.

**Tech Stack:** Astro 7 (sito esistente in `website/`), moduli ES `.mjs` con JSDoc, `node:test` (Node ≥ 22.12, nessuna dipendenza nuova).

**Spec:** `docs/superpowers/specs/2026-11-01-config-portal-design.md`

## Global Constraints

- Nessuna dipendenza npm nuova; il sito resta statico; il file dell'owner non esce dal browser.
- Testi in italiano, stile e token del sito (`website/src/styles/tokens.css`, `.btn`, `.wrap`, `.pix`); pagine del sito sempre scure.
- TOML: si cambiano solo le righe dei valori toccati; commenti, ordine, rientri (tab), fine riga (CRLF o LF) e chiavi sconosciute restano identici. Chiavi assenti compaiono nel portale col predefinito e finiscono nel file solo se modificate.
- `cash.json`: ordine delle chiavi e chiavi sconosciute conservati; rientri normalizzati; file non modificato = testo originale invariato.
- Valori `Long` (`atm.vipBalance`, `limits.maxPerOperation`, `limits.maxPerDay`, fino a `9223372036854775807`) gestiti come `bigint`, mai come `number`.
- Java è la fonte di verità per chiavi e intervalli; il testo descrittivo viene da `website/src/content/docs/docs/riferimento/configurazione.md`.
- Con errori aperti il download è bloccato (gli avvisi non lo bloccano).
- Accessibile da tastiera, usabile a 360 px (anteprima sotto i campi).
- Lo schema mostra la versione `site.version` ("schema per QRM x.y.z").

## Review Focus

- File con fine riga CRLF (i file reali generati dalla mod lo sono): il file scaricato non deve mescolare CRLF e LF.
- `qrm_bank-server.toml` di una versione precedente senza `vipBalance`: caricato e riscaricato senza modifiche = identico byte per byte.
- `namePattern` contiene backslash (`\p{L}`): letto, mostrato e riscritto con l'escape TOML corretto, identico se non toccato.
- Numeri `Long` oltre 2^53 non perdono cifre.
- File che non è nessuno dei tre (JSON qualunque, TOML vuoto, binario): errore chiaro, nessun crash e nessuna modifica allo stato precedente.

---

## File Structure

- `website/src/lib/config/toml.mjs` — lettore/scrittore TOML per righe.
- `website/src/lib/config/schema.mjs` — i tre file, sezioni, campi, predefiniti, limiti, descrizioni.
- `website/src/lib/config/validate.mjs` — validazione campo per campo e regole incrociate.
- `website/src/lib/config/cash.mjs` — modello e serializzazione di `cash.json`.
- `website/src/lib/config/session.mjs` — apertura file, riconoscimento, modifica, rendering, righe cambiate.
- `website/src/pages/configuratore.astro`, `website/src/scripts/configuratore.ts`, `website/src/styles/configuratore.css` — interfaccia.
- `website/scripts/check-config-schema.mjs` — confronto schema ↔ sorgenti Java.
- `website/tests/config/*.test.mjs` e `website/tests/config/fixtures/` — test e file reali di esempio.

### Task 1: Lettore/scrittore TOML

**Files:**
- Create: `website/src/lib/config/toml.mjs`, `website/tests/config/toml.test.mjs`
- Create fixtures (copie dei file reali di `qrm-bank/run/config/`, che è ignorata da git): `website/tests/config/fixtures/qrm-common.toml` (CRLF), `website/tests/config/fixtures/qrm_bank-server.toml` (LF, senza `vipBalance`)
- Modify: `website/package.json` (script `"test": "node --test tests/"`)

**Interfaces:**
- Produces:
  - `type TomlValue = string | boolean | bigint | number | string[]` (interi → `bigint`, decimali → `number`)
  - `parseToml(text: string): TomlDoc` (non lancia: righe non riconosciute restano opache)
  - `getValue(doc: TomlDoc, path: string): TomlValue | undefined` — `path` è `sezione.chiave`, es. `gui.hub.tabs`
  - `hasKey(doc, path): boolean`, `listKeys(doc): string[]`
  - `setValue(doc, path, value, comment?: string): void` — se la chiave esiste cambia solo il testo del valore; altrimenti la inserisce dopo l'ultima chiave della sezione (o crea la sezione in fondo), con rientro dei fratelli e `comment` come riga `#...` sopra
  - `serializeToml(doc): string`

- [ ] **Step 1: Write the failing tests** (`toml.test.mjs`): `serializeToml(parseToml(f)) === f` per entrambe le fixture; `getValue(doc,'characters.slots') === 3n`; `getValue(doc,'atm.maxDistance') === 6`; `getValue(doc,'characters.uniqueNames') === true`; `getValue(doc,'gui.hub.tabs')` deep-equal `['accounts','job','orgs']`; `getValue(doc,'characters.namePattern') === "^[\\p{L}][\\p{L}' -]*$"` (backslash singolo dopo la lettura); `setValue(doc,'characters.slots',5n)` cambia solo quella riga (confronto riga per riga col file originale) e conserva CRLF; `setValue` su `atm.vipBalance` assente (fixture bank) lo aggiunge dopo `transfersEnabled` con rientro a tab e commento; `setValue(doc,'limits.maxPerDay',9223372036854775807n)` si rilegge identico; array vuoto `overrides = []` e valore lista con virgolette/backslash; riscrittura di `namePattern` ripristina i doppi backslash nel file.
- [ ] **Step 2: Run** `cd website && node --test tests/config/toml.test.mjs` — atteso: FAIL (modulo mancante).
- [ ] **Step 3: Implement** il modulo secondo le firme sopra. Approccio: si salva per ogni riga `{raw, kind:'blank'|'comment'|'section'|'key'|'other', section, key, valueText, prefix, suffix}`; le sezioni indentate `[gui.hub]` valgono il loro nome completo; i valori stringa seguono le regole delle stringhe base TOML (escape `\\`, `\"`, `\n`, `\t`, `\uXXXX`) e le letterali `'...'`; fine riga rilevata dal primo `\r\n`.
- [ ] **Step 4: Run** i test — atteso: PASS.
- [ ] **Step 5: Commit** `feat(site): lettore e scrittore TOML per il configuratore`

### Task 2: Schema dei tre file e controllo contro Java

**Files:**
- Create: `website/src/lib/config/schema.mjs`, `website/scripts/check-config-schema.mjs`, `website/tests/config/schema.test.mjs`
- Modify: `website/package.json` (la build esegue `node scripts/check-config-schema.mjs` dopo `check-docs.mjs`; aggiungere anche lo script `"check:config"`)

**Interfaces:**
- Produces:
  - `type Field = { path: string; label: string; description: string; type: 'bool'|'int'|'float'|'string'|'regex'|'list'|'limitOverrides'; default: TomlValue; min?: bigint|number; max?: bigint|number; knownItems?: string[]; itemPattern?: string; emptyMeaning?: string }`
  - `type Section = { id: string; label: string; fields: Field[] }`
  - `type FileSchema = { id: 'common'|'bank'|'cash'; fileName: string; kind: 'toml'|'json'; label: string; sections: Section[] }` (`cash` ha `sections: []`: ha editor dedicato)
  - `SCHEMAS: FileSchema[]`, `schemaFor(id): FileSchema`, `findField(id, path): Field | undefined`
- Dati: tutte le chiavi e i valori della tabella "Regole di validazione" della spec e della pagina `configurazione.md` (etichette e descrizioni in italiano da lì; `database.*` incluso come sezione di sola lettura con nota "non usata").

- [ ] **Step 1: Write the failing tests**: ogni chiave di `qrm-common.toml` e `qrm_bank-server.toml` delle fixture esiste nello schema; `findField('bank','limits.maxPerDay').max === 9223372036854775807n`; `findField('common','characters.slots')` ha `min 1n`, `max 50n`, `default 3n`; `gui.hud.elements.itemPattern === '[a-z0-9_.-]+:[a-z0-9_./-]+'`, `knownItems` `['name','balance','job']`.
- [ ] **Step 2: Run** `node --test tests/config/schema.test.mjs` — atteso: FAIL.
- [ ] **Step 3: Implement** lo schema.
- [ ] **Step 4: Run** — atteso: PASS.
- [ ] **Step 5: Implement `check-config-schema.mjs`**: legge `qrm-neoforge/.../QrmConfig.java` e `qrm-bank/.../BankConfig.java`, ricostruisce i percorsi dai `b.push("x")`/`b.pop()` e dalle chiamate `define*("chiave"`, confronta chiavi e, dove i limiti sono letterali numerici, `min`/`max` con lo schema. Stampa le differenze ed esce con 1 (stesso stile di `check-docs.mjs`). Eseguirlo: `cd website && node scripts/check-config-schema.mjs` — atteso: exit 0. Prova negativa: rinominare temporaneamente una chiave nello schema, atteso exit 1, poi ripristinare.
- [ ] **Step 6: Commit** `feat(site): schema del configuratore e controllo contro il codice Java`

### Task 3: Validazione

**Files:**
- Create: `website/src/lib/config/validate.mjs`, `website/tests/config/validate.test.mjs`

**Interfaces:**
- Consumes: `Field`, `findField` (Task 2); `TomlValue` (Task 1)
- Produces:
  - `type Issue = { path: string; severity: 'error'|'warning'; message: string }` (messaggi in italiano)
  - `validateField(field: Field, value: TomlValue): Issue | null`
  - `validateValues(fileId: 'common'|'bank', values: Map<string, TomlValue>): Issue[]` — campi singoli più regole incrociate
  - `parseOverride(entry: string): { code: string; perOperation: bigint; perDay: bigint } | null` e `formatOverride(o): string` (formato `"CODICE:perOperazione:alGiorno"`, esattamente tre parti separate da `:`, due interi `Long` come in `LimitsProvider.of`)

- [ ] **Step 1: Write the failing tests**: `characters.slots` 0n e 51n → error, 3n → null; `atm.maxDistance` 1.5 e 16.5 → error; `limits.maxPerDay` 0n → error; `9223372036854775808n` → error; `gui.hud.elements` con `'bank:debt'` valido, `'Bank:Debt'` e `'x'` (non nota, senza `:`) → error; lista vuota → valida; `limits.overrides` `'EUR:50000:200000'` valida, `'EUR:5:'` e `'EUR:a:b'` e `'EUR:1:2:3'` → error; `characters.namePattern` `'('` → warning (non compilabile nel browser, può essere valida per Java), `'^[\\p{L}]+$'` → null; `nameMinLength` 30n con `nameMaxLength` 24n → error su min (min > max); `economy.defaultCurrency` stringa vuota → error.
- [ ] **Step 2: Run** `node --test tests/config/validate.test.mjs` — atteso: FAIL.
- [ ] **Step 3: Implement** le firme sopra. L'espressione regolare si prova con `new RegExp(p, 'u')`; un errore produce un *warning*, mai un error.
- [ ] **Step 4: Run** — atteso: PASS.
- [ ] **Step 5: Commit** `feat(site): validazione dei valori del configuratore`

### Task 4: Modello di `cash.json`

**Files:**
- Create: `website/src/lib/config/cash.mjs`, `website/tests/config/cash.test.mjs`

**Interfaces:**
- Consumes: `Issue` (Task 3)
- Produces:
  - `type CashCurrency = { code: string; decimals: number; denominations: number[]; extra: Record<string, unknown> }`
  - `type CashModel = { currencies: CashCurrency[] }` (ordine delle valute = ordine nel file; le chiavi interne sconosciute di una valuta vanno in `CashCurrency.extra`)
  - `parseCash(text: string): CashModel` — lancia `Error` con messaggio italiano se non è un oggetto JSON di valute
  - `serializeCash(model: CashModel): string` — una valuta per riga, forma `"QRM": { "decimals": 2, "denominations": [100, 500] }`, rientro 2 spazi, newline finale
  - `validateCash(model: CashModel): Issue[]` — regole di `CashDenominations`: codice `[A-Z0-9_]{2,16}`, codici distinti, `decimals` intero 0–8, almeno un taglio, tagli interi 1–1_000_000_000_000, distinti, ciascuno multiplo del precedente più piccolo (`path` = `CODICE` o `CODICE.denominations`)

- [ ] **Step 1: Write the failing tests**: `serializeCash(parseCash(DEFAULT_JSON))` è uguale a `DEFAULT_JSON` (il testo di `docs/samples/bank/cash.json`); ordine delle valute e chiave sconosciuta `"note"` dentro una valuta conservati; `validateCash` segnala `[100, 250]` (250 non multiplo di 100), `[100, 100]`, `[]`, `decimals: 9`, `decimals: 1.5`, codice `eur`, taglio `0` e `1_000_000_000_001`; segnala nulla per `[100, 500, 1000, 5000, 10000]`; `parseCash('[]')` e `parseCash('{"A": 5}')` lanciano errore.
- [ ] **Step 2: Run** `node --test tests/config/cash.test.mjs` — atteso: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** — atteso: PASS.
- [ ] **Step 5: Commit** `feat(site): modello e validazione di cash.json per il configuratore`

### Task 5: Sessione di modifica

**Files:**
- Create: `website/src/lib/config/session.mjs`, `website/tests/config/session.test.mjs`

**Interfaces:**
- Consumes: Task 1–4.
- Produces:
  - `class ConfigFileError extends Error` (messaggio italiano, mostrabile all'utente)
  - `detectFile(fileName: string, text: string): 'common'|'bank'|'cash'` — prima il nome (`qrm-common.toml`, `qrm_bank-server.toml`, `cash.json`), poi il contenuto (`[characters]`/`[gui…` → common; `[atm]`/`[limits]` → bank; oggetto JSON → cash); altrimenti lancia `ConfigFileError`
  - `openFile(fileName: string, text: string): Session` (non muta nulla di esterno; lancia `ConfigFileError`); `newSession(id: 'common'|'bank'|'cash'): Session` (da zero, con i predefiniti)
  - `Session = { id; fileName; original: string; values: Map<string, TomlValue>; unknownKeys: string[]; cash?: CashModel }`
  - `getField(s, path): { value: TomlValue; present: boolean; modified: boolean }` — `present` = c'era nel file; `modified` = diverso dall'originale (o dal predefinito se assente)
  - `setField(s, path, value): void`, `resetField(s, path): void` (torna al valore originale, o al predefinito e "non toccata" se la chiave era assente)
  - `issues(s): Issue[]`
  - `render(s): string` — TOML: parte dal testo originale e applica `setValue` solo ai campi modificati; sessione non modificata → `original` identico; cash: `original` se non modificato, altrimenti `serializeCash`; sessione nuova: file completo con tutte le chiavi e i commenti delle descrizioni
  - `changedLines(s): number[]` — numeri di riga (1-based) di `render(s)` diversi o aggiunti rispetto a `original`
  - `downloadName(s): string` — il nome del file caricato (o quello standard per `newSession`)

- [ ] **Step 1: Write the failing tests**: `render(openFile('qrm-common.toml', fixture)) === fixture` e idem per bank (assenza di `vipBalance` non aggiunge nulla); `setField(s,'characters.slots',5n)` → `changedLines` è esattamente `[<riga di slots>]` e il resto è identico; `setField(bank,'atm.vipBalance',500n)` aggiunge una riga con commento, tutte e due in `changedLines`; `resetField` ripristina l'originale; chiave `foo.bar` aggiunta a mano nel file finisce in `unknownKeys` e sopravvive al `render`; `detectFile('x.txt','{"a":1}')` lancia `ConfigFileError`; `openFile('a.toml','')` e `openFile('a.bin','\u0000\u0001')` lanciano `ConfigFileError`; il file con CRLF resta CRLF in `render`; `issues` segnala slot a 80n; `newSession('common')` renderizzato e riletto con `openFile` non dà `issues`.
- [ ] **Step 2: Run** `node --test tests/config/session.test.mjs` — atteso: FAIL.
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run** `node --test tests/` — atteso: tutti PASS.
- [ ] **Step 5: Commit** `feat(site): sessione di modifica del configuratore`

### Task 6: Interfaccia e integrazione nel sito

**Files:**
- Create: `website/src/pages/configuratore.astro`, `website/src/scripts/configuratore.ts`, `website/src/styles/configuratore.css`
- Modify: `website/src/components/Header.astro` (voce "Configuratore" nella nav), `website/src/content/docs/docs/riferimento/configurazione.md` (rimando al configuratore in testa a "Dove stanno i file")

**Interfaccia (come da mockup B):** intestazione con versione dello schema e stato (`N modifiche`, `M errori`); zona di drop (anche pulsante "Scegli file" e "Parti dai valori predefiniti" per ciascuno dei tre file); sotto, tre colonne: sezioni | campi della sezione scelta | anteprima del file con righe in `changedLines` evidenziate. A 360 px le colonne si impilano (sezioni come schede in alto, anteprima in fondo). Controlli: interruttore per `bool`; `<input type=number>` con min/max per `int`/`float` (i `bigint` come testo validato, non `number`); elenco riordinabile da tastiera (pulsanti su/giù e rimuovi, aggiunta da elenco di voci note o testo libero `modulo:riga`) per `list`; righe `valuta / per operazione / al giorno` per `limitOverrides`; tabella di valute con elenco di tagli per `cash.json`. Ogni campo: etichetta, descrizione, predefinito, "Ripristina predefinito", errore/avviso sotto il campo, badge "non nel file" per chiavi assenti. Chiavi in `unknownKeys` elencate come "non riconosciuta". Pulsante **Scarica** (`Blob`, nome da `downloadName`) disabilitato se `issues` contiene errori, con la lista degli errori che spiega perché; errore di apertura (`ConfigFileError`) mostrato in una nota senza toccare la sessione corrente. Un cambio di file con modifiche non scaricate chiede conferma.

- [ ] **Step 1: Implement** la pagina usando `Base.astro` e `Header.astro`; lo script importa solo dai moduli `src/lib/config/*` e non contiene logica di formato; la versione dello schema arriva dal server (`site.version`) in un `data-` attributo.
- [ ] **Step 2: Run** `cd website && npm run build` — atteso: exit 0 (`astro check`, `check-docs`, `check-config-schema`, build; il validatore dei link della wiki accetta il nuovo rimando).
- [ ] **Step 3: Verify nel browser** (`preview_start` sul dev server del sito, `/Quorum/configuratore/`): caricare `website/tests/config/fixtures/qrm-common.toml` con `javascript_tool`/input file, cambiare `characters.slots` a 5 e disattivare `uniqueNames`, controllare che l'anteprima evidenzi solo due righe, che slots a 80 blocchi il download con messaggio, che il download dopo la correzione sia identico all'originale tranne le due righe; ripetere con `qrm_bank-server.toml` (aggiunta di `vipBalance`) e con `cash.json` (aggiungere una valuta `EUR`, tagli 1, 5, 10, 50); schermata a 360 px e navigazione solo da tastiera. Riportare l'esito di ogni controllo.
- [ ] **Step 4: Commit** `feat(site): configuratore visuale per i file di configurazione di QRM`

---

## Self-Review

- **Copertura della spec:** i tre file (T1–T5), controlli per tipo, predefiniti/ripristino/descrizioni (T2, T6), validazione prima del download (T3, T6), anteprima con righe evidenziate (T5 `changedLines`, T6), nome file originale (T5 `downloadName`), partenza dai predefiniti (T5 `newSession`), "sul posto" per TOML e cash normalizzato (T1, T4), tutto nel browser (vincoli), schema ↔ Java (T2), versione schema (T6), `Long` (T1, T3), collegamento da menu e wiki (T6). Fuori scope rispettati: nessun lavoro/valuta/divisa, nessun `qrm-client.toml`.
- **Tipi:** `TomlValue`, `Field`, `Issue`, `Session` usati con gli stessi nomi in tutti i task.
- **Aperto per il builder, come da spec:** struttura dei componenti, copy degli errori, dettagli grafici.
