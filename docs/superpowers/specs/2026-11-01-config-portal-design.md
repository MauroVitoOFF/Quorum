# Portale di configurazione di QRM — progetto

Data: 2026-11-01. Stato: bozza da approvare.

## Intenzione

I server owner che usano QRM modificano a mano file di configurazione (`qrm-common.toml`, `qrm_bank-server.toml`, `cash.json`). Il rischio concreto è rompere il file con una virgola, una virgoletta o una parentesi sbagliata, o inserire un valore fuori limite che la mod rifiuterà all'avvio.

Il portale è una pagina del sito di QRM in cui il server owner **trascina il proprio file**, lo **modifica con campi guidati** e lo **riscarica** valido. Non deve mai dover scrivere sintassi.

Perché: semplificare la vita ai server owner ed eliminare gli errori di sintassi e di valore.

## Obiettivi

- Aprire per drag and drop (o selezione file) questi tre file, riconosciuti dal contenuto:
  - `qrm-common.toml`: sezioni `database`, `characters`, `economy`, `jobs`, `commands`, `gui.hub`, `gui.hud`.
  - `qrm_bank-server.toml`: sezioni `atm`, `limits`.
  - `config/qrm_bank/cash.json`: valute con `decimals` e `denominations`.
- Mostrare ogni chiave con un controllo adatto al tipo:
  - sì/no: interruttore;
  - numeri: campo con minimo e massimo della mod;
  - elenchi di schede dell'hub e di righe dell'HUD: elenco riordinabile con le voci note (`accounts`, `job`, `orgs`; `name`, `balance`, `job`) e voci libere dei moduli nella forma `modulo:riga`;
  - `limits.overrides`: righe strutturate valuta / per operazione / al giorno, che il portale compone nel formato `"CODICE:perOperazione:alGiorno"`;
  - `cash.json`: tabella di valute con elenco di tagli.
- Mostrare i valori predefiniti, il significato di ogni chiave (dalla documentazione esistente) e un pulsante per ripristinare il predefinito di un campo.
- Segnalare i valori non validi sul campo e nel riepilogo, prima del download. Con errori aperti il download resta possibile ma avvisa, oppure è bloccato (da decidere: vedi "Lasciato al builder").
- Disposizione: elenco delle sezioni a sinistra, campi della sezione scelta al centro, **anteprima del file risultante a destra** con le righe modificate evidenziate. Scelta dall'utente fra tre alternative mostrate in mockup (opzione B).
- Scaricare il file con lo stesso nome del file caricato.
- Poter partire senza caricare nulla, dai valori predefiniti.

## Non obiettivi (prima versione)

- Creare o modificare lavori, valute (`data/jobs`, `data/currencies`) e divise di Wardrobe. Sono rimandati a una fase successiva, da decidere insieme.
- `qrm-client.toml` (preferenze del singolo giocatore, una sola chiave): **scelta mia**, fuori perché non riguarda il server owner.
- Account, salvataggio sul sito, condivisione di configurazioni, invio del file a un server.
- Modificare il database SQLite o parametri di connessione esterni (oggi non supportati dalla mod).

## Anti-obiettivi (cosa sarebbe un fallimento anche se "funziona")

- Il file scaricato perde commenti, ordine o chiavi sconosciute del file originale.
- Il file scaricato viene rifiutato dalla mod.
- Il file dell'owner esce dal suo browser. *(Ipotesi mia: se serve più che a un po' di comodità, va confermato.)*
- Il portale diverge dalla mod: un limite o una chiave diversi da quelli del codice.

## Vincoli e scelte già prese

Decise dall'utente:
- È una pagina del sito web esistente (Astro, in `website/`), per i server owner.
- Disposizione B (sezioni, campi e anteprima dal vivo con evidenziazione delle modifiche).
- Prima versione sui file di configurazione principali; lavori ecc. in seguito.

Scelte mie, da verificare:
- **Tutto nel browser**, nessun upload: nessun server aggiuntivo, nessuna riservatezza da gestire. Il sito resta statico.
- **Modifica "sul posto"**: il file originale viene letto per righe e si cambiano solo le righe dei valori toccati. Commenti, ordine, spaziature e chiavi non riconosciute restano identici. Una chiave assente dal file viene aggiunta nella sezione giusta, con il suo commento.
- Lo schema (chiavi, tipi, minimi, massimi, predefiniti, descrizioni) deriva dal codice della mod: `QrmConfig.java`, `BankConfig.java`, `CoreConfig`, `UiConfig`, `LimitsProvider.isValidOverride`, `CashDenominations`. Va scritto in un solo file dati del sito.
- Se in Java si aggiunge o cambia una chiave senza aggiornare lo schema del portale, la build del sito deve accorgersene (controllo simile a `check-docs`).
- I campi `Long` (limiti e saldo VIP, fino a 9223372036854775807) superano i numeri interi sicuri di JavaScript: vanno gestiti senza perdere cifre (BigInt o stringa).
- Lingua italiana, stesso stile e linguaggio visivo del sito (token in `website/src/styles/tokens.css`); pagina raggiungibile dal menu e collegata da `docs/riferimento/configurazione.md`.
- Accessibile da tastiera e leggibile su schermo stretto, perché l'anteprima va sotto i campi.

## Scelte aggiunte dopo la revisione

Marcate come mie, da verificare:
- **`cash.json`**: il JSON non ha commenti, quindi si rilegge e si riscrive con ordine delle chiavi e chiavi sconosciute conservati, ma con rientri normalizzati (2 spazi, come in `docs/samples/bank/cash.json`). L'anteprima mostra il file risultante, quindi la differenza è visibile prima del download. La regola "modifica sul posto" vale solo per i TOML.
- **Versioni della mod**: lo schema del portale segue la versione corrente (`qrm_version` in `gradle.properties`), e la pagina la mostra ("schema per QRM x.y.z"). Chiavi assenti nel file non vengono aggiunte da sole: compaiono nel portale con il valore predefinito e finiscono nel file solo se l'owner le modifica. Così un file di una versione più vecchia non produce un diff enorme né viene "corretto" senza che l'owner lo scelga. Le chiavi del file che lo schema non conosce restano intatte e sono segnalate come "non riconosciuta".
- **Controllo schema/codice**: la build del sito ha sempre l'intero repository (`website/src/config/site.ts` legge già `../gradle.properties`), quindi lo script di controllo può leggere direttamente i sorgenti Java. Per i nomi e gli intervalli delle chiavi, il **codice Java è la fonte di verità**; `docs/riferimento/configurazione.md` fornisce solo il testo descrittivo. Se i due divergono, vince Java e lo script lo segnala.

## Regole di validazione note dal codice

| Chiave | Regola |
| --- | --- |
| `characters.slots` | intero 1–50 |
| `characters.nameMinLength` / `nameMaxLength` | interi 1–64 (min ≤ max da verificare nella mod) |
| `characters.namePattern` | espressione regolare valida (la mod usa Java; il browser usa JavaScript: differenze da segnalare) |
| `characters.uniqueNames`, `jobs.selfDuty`, `commands.shortAliases` | booleani |
| `economy.defaultCurrency` | testo, predefinito `QRM` |
| `gui.hub.tabs` | elenco; le voci note sono `accounts`, `job`, `orgs`; vuoto = hub disattivato |
| `gui.hud.elements` | elenco; voci `name`, `balance`, `job` o `modulo:riga` (`[a-z0-9_.-]+:[a-z0-9_./-]+`); vuoto = HUD nascosto |
| `atm.maxDistance` | decimale 2.0–16.0 |
| `atm.minIntervalMillis` | intero 0–5000 |
| `atm.historyRows` | intero 1–50 |
| `atm.transfersEnabled` | booleano |
| `atm.vipBalance` | intero ≥ 0 (0 = disattivato) |
| `limits.maxPerOperation`, `maxPerDay` | interi ≥ 1 |
| `limits.overrides` | elenco di `"CODICE:perOperazione:alGiorno"` |

## Lasciato al builder

- Come leggere e riscrivere TOML riga per riga (conviene una piccola libreria propria, senza dipendenze nuove, se basta a questi tre file).
- Disegno dei controlli, copy dei messaggi di errore, struttura dei componenti.
- Come strutturare il controllo di coerenza fra schema del portale e codice Java.
- Se, con errori aperti, il download è bloccato o solo avvisato.
- Regole di `cash.json` oltre a quelle del codice (`CashDenominations.parse`), da leggere nel sorgente.
- Test automatici del lettore/scrittore sui file reali generati dalla mod.
