# Nuova wiki di QRM con Astro Starlight

Data: 2026-10-13. Stato: approvato, in implementazione (M0).

Sostituisce la wiki Docusaurus della repo `MauroVitoOFF/QRM-docs`. Questo documento fissa le decisioni prese in fase di progettazione; quando una decisione cambia, si aggiorna qui.

## Decisioni

| # | Decisione | Scelta |
|---|---|---|
| 1 | Asse della navigazione | Per sistema. Ogni sistema ha una sola pagina di riferimento; niente doppia pagina per destinatario. |
| 2 | Sede | Dentro `Quorum`, nel progetto `website/`, pubblicata sotto `/Quorum/docs/`. `QRM-docs` viene ritirata con un rimando. |
| 3 | Lessico | Concetti in italiano, come nell'interfaccia del mod. Identificatori in inglese e in `codice`. |
| 4 | Distribuzione dei jar | GitHub Releases, con la prima release dopo la messa online della wiki. Fino ad allora Installazione descrive la compilazione da sorgente. |
| 5 | Pubblico | Solo chi gestisce un server. La parte per sviluppatori non si migra. Decisione provvisoria: la pagina "Scrivere un Module" è rinviata, non esclusa. |
| 6 | Versioni | Una sola versione documentata, quella corrente. La prima release pubblica è il punto zero dello storico. |
| 7 | Progetto | Un unico sito: la vetrina passa ad Astro 7 e Starlight si aggiunge allo stesso progetto. |
| 8 | Pubblicazione | A ogni push su `main`. Home e Installazione mostrano versione documentata e ultima release. |

Fuori perimetro: la revisione dei contenuti della vetrina (che oggi ha tre sezioni per sviluppatori).

## Guida editoriale

**Lettore.** Chi gestisce un server: sa installare un mod e modificare un file di configurazione, non legge Java.

**Apertura.** Una pagina si apre con il fatto più utile sul sistema. Niente "Questa pagina descrive…", niente indicazione del destinatario.

**Struttura.** Nessuno schema fisso. Titoli italiani che dicono cosa contiene la sezione. Una pagina di sistema risponde di norma, in quest'ordine, a: come si comporta, quali comandi ha, quali chiavi di configurazione lo regolano. Si omette ciò che non c'è.

**Lessico.**

| Concetto | Nella wiki | Identificatore (in `codice`) |
|---|---|---|
| Character | personaggio | `/char`, `characters.*` |
| Player | giocatore | |
| Job, Grade | lavoro, grado | `/job` |
| Duty | servizio | `/duty` |
| Organization, Rank | organizzazione, rank | `/org` |
| Permission | permesso | `/perm`, nodi |
| Account | conto | |
| Currency | valuta | |
| Transaction | transazione | |
| Module | Module (nome proprio invariabile: Bank, Locker, Boss) | |
| Sottoprogetto Gradle | modulo | `qrm-api`, `qrm-core`… |

"QRM" da solo indica il mod principale.

**Cosa si scrive.**
- Solo comportamento verificato nel codice della versione corrente.
- Procedure numerate, chiuse dal modo di controllare che abbiano funzionato.
- Un limite si scrive nel punto in cui il lettore lo incontra, non in un elenco finale.
- Un fatto ha una sola sede. Altrove si linka, dentro la frase che lo richiede.
- Valori predefiniti e intervalli come sono nel codice.
- Persona: "tu" nelle procedure, impersonale nelle descrizioni.

**Cosa non si scrive.**
- Storia interna ("dalla 0.10", "sostituisce il vecchio menu").
- Aggettivi di valore, descrizioni di intenti architetturali.
- Funzioni non presenti. Se serve nominarle: "Non disponibile nella versione corrente."
- "Gratuito" o "open source" come caratteristica del prodotto: lo dice la licenza, in un solo punto.
- Inviti a scrivere Modules o promesse di stabilità dell'API.

**Etichette.** Una sola, "Sperimentale", per una funzione il cui comportamento o formato dei dati può cambiare. Si spiega in "Versioni e compatibilità". Dopo la prima release, una funzione non ancora rilasciata porta "Dalla X.Y".

## Architettura delle pagine

Due livelli: sezione, pagina. Circa 26 pagine.

1. **Per iniziare**: Cos'è QRM · Requisiti e installazione · Primo avvio
2. **Guide**: Creare un lavoro · Dare accesso allo staff · Aggiungere un punto di interazione · Fare un backup e controllare i saldi · Cambiare l'aspetto dell'interfaccia · Risolvere i problemi comuni
3. **Sistemi**: Personaggi · Economia · Lavori · Organizzazioni · Permessi · Punti di interazione · Pannello staff · Moderazione e registro delle azioni · Interfaccia del client
4. **Modules**: Bank · Locker · Boss (una pagina per Module, con una riga "Disponibilità"; si divide solo oltre le 1.200 parole circa)
5. **Riferimento**: Comandi · Configurazione e file · Nodi di permesso · Manifesto del tema · Versioni e compatibilità

Le tre tabelle Comandi, Configurazione e file, Nodi di permesso sono l'unica duplicazione ammessa: la tabella elenca e linka, la pagina di sistema spiega.

Fonti: sempre il codice (`*Commands.java`, `QrmConfig.java`, `BankConfig.java`, `StaffNodes.java`, `KeyBindings.java`, i `neoforge.mods.toml`, `it_it.json`, `gradle.properties`) e le checklist di test manuale in `docs/`. Mai la wiki precedente come prova.

## Scelte tecniche

- Astro 7 e Starlight 0.42 nel progetto `website/`. Node 22.12 o superiore.
- Contenuti Markdown in `website/src/content/docs/docs/<sezione>/`, così le pagine escono sotto `/docs/`. MDX solo dove serve un componente.
- Sidebar dichiarata nella configurazione, cinque gruppi in ordine esplicito.
- Italiano come lingua radice.
- Ricerca: Pagefind, inclusa in Starlight.
- Aspetto: un file `customCss` con i colori di QRM e il logo. Font di sistema. Nessuna sostituzione di componenti.
- Immagini in `src/assets/`. Screenshot solo dove il testo non basta. Nessun plugin per diagrammi.
- Unico plugin: `starlight-links-validator`, per far fallire la build sui link interni rotti.
- Il numero di versione si legge da `gradle.properties` al momento della build.

### Esito di M0 (2026-10-13)

Verificato con una build locale e con un confronto degli stili calcolati fra la vetrina su Astro 5 e quella attuale (1.192 elementi, desktop e mobile, anche con sezioni rivelate e menu aperto): nessuna differenza. Gli stati hover e focus non sono stati misurati.

- Le pagine in `src/content/docs/docs/` escono sotto `/Quorum/docs/`; `src/pages/` continua a servire la pagina iniziale e la 404 (`disable404Route: true`).
- Starlight imposta `scopedStyleStrategy: 'where'` per tutto il progetto. Due regole della vetrina perdevano contro `base.css` e sono state rese più specifiche (`Story.astro`, `Requirements.astro`). Riportare la strategia ad `attribute` altera il selettore del tema e il pulsante del menu di Starlight: non farlo.
- Con Astro 7 `site.ts` gira da `dist/.prerender`: `gradle.properties` si legge rispetto a `process.cwd()`, quindi la build va lanciata da `website/`.
- I link interni della wiki si scrivono assoluti e con la base: `/Quorum/docs/sistemi/personaggi/`. Il validatore rifiuta i link relativi.
- Starlight aggiunge da sé la sitemap: l'integrazione esplicita è stata tolta.
- Il workflow `website.yml` non ha richiesto modifiche: si attiva già su `website/**`, usa Node 22 e la validazione dei link gira dentro `npm run build`.
- `npm audit` segnala 11 avvisi di gravità moderata, tutti dalla catena `expressive-code` → `postcss-selector-parser` (dipendenza di Starlight, usata solo in fase di build). Si risolvono con un aggiornamento a monte.
- Da decidere in M1: il titolo nell'intestazione della wiki porta alla pagina di presentazione, non all'inizio della wiki.

### Esito di M1 (2026-10-13)

Dieci pagine scritte dal codice: Cos'è QRM, Requisiti e installazione, Primo avvio, Personaggi, Economia, Lavori, Organizzazioni, Permessi, Creare un lavoro, Dare accesso allo staff. Colore d'accento in `website/src/styles/docs.css`.

- Le pagine che mostrano numeri di versione sono `.mdx` e li leggono da `src/config/site.ts`.
- Rimandate: la riga con l'ultima release (non esiste ancora una release) e la riga sulla licenza (in attesa della decisione sul modello di distribuzione).
- Non verificato in gioco: nessuna procedura è stata eseguita su un server. Gli esempi JSON sono scritti leggendo `JobLoader` e `CurrencyLoader`, non caricati da un test (previsto in M3).
- Non verificato nel codice: se Locker e Boss siano obbligatori sui client.
- Emerso dal codice: `characters.slots` vale solo per i giocatori nuovi; i nodi `staff.jobs`, `staff.orgs`, `staff.perms` esistono ma non sono controllati da nessuna parte; la vecchia wiki non elencava `staff.player.ban`.

### Esito di M2 (2026-10-13)

Dodici pagine: Punti di interazione, Pannello staff, Moderazione e registro, Interfaccia del client; le guide Aggiungere un punto, Backup e saldi, Aspetto dell'interfaccia, Problemi comuni; il riferimento Comandi, Configurazione e file, Nodi di permesso, Versioni e compatibilità.

- `website/scripts/check-docs.mjs` gira dentro `npm run build`: confronta comandi, chiavi e nodi con il codice di `qrm-neoforge` e `qrm-api`, nei due sensi, e stampa gli avvisi di stile. Il workflow si attiva anche su `qrm-*/src/main/**`.
- Lo script copre solo il Core. Comandi, chiavi e nodi di Bank, Locker e Boss entrano con M3.
- I literal dei comandi costruiti da un metodo di supporto (`permLiteral`, `removeNode`, `characterNode`) sono elencati a mano nello script: un nuovo metodo di supporto va aggiunto lì.
- Non verificato: che `commands.shortAliases` si applichi con `/reload` (lo dice solo il commento nel codice; la pagina indica il riavvio); l'aspetto esatto del pannello staff e dell'hub, descritti dal codice senza averli visti.

### Esito di M3, parte dei contenuti (2026-10-13)

Pagine: Bank, Locker, Boss, Manifesto del tema. La wiki ha 26 pagine.

- Gli esempi JSON sono file in `docs/samples/`. Una pagina li mostra in un blocco ```` ```json sample="docs/samples/…" ````; `check-docs.mjs` controlla che il blocco coincida con il file, e `WikiSamplesTest` (in `qrm-neoforge` e `qrm-bank`) li carica con `CurrencyLoader`, `JobLoader`, `ThemeManifest` e `CashDenominations`.
- `check-docs.mjs` copre anche comandi, chiavi e nodi di Bank e Boss. Locker non ne ha.
- Punti aperti sciolti: le chiavi del tema di un Module iniziano con il suo id (`qrm_bank.`); Locker è facoltativo sui client (si apre come un baule), Boss serve a chi usa il menu.
- Non verificato: la cartella in cui NeoForge mette `qrm_bank-server.toml`.
- Emerso dal codice, da valutare nel mod: il menu di Boss usa la prima valuta registrata e non `economy.defaultCurrency`; un punto `boss_desk` è utilizzabile solo dai membri espliciti dell'organizzazione, quindi non da chi dirige un'azienda di un lavoro senza esserne anche membro.

Restano fuori, perché dipendono dalla pubblicazione e dalla revisione della vetrina: la riscrittura dei link della vetrina verso la wiki (una parte punta a pagine per sviluppatori che non esistono più) e il ritiro di `QRM-docs`.

## Affidabilità

1. Uno script in CI confronta le tre tabelle di riferimento con il codice, nei due sensi, e fa fallire la build se divergono. Si attiva anche sui cambi al codice del mod. I nodi costruiti a runtime (`staff.player.<azione>`, chiavi di Locker) hanno una regola dedicata.
2. Lo stesso script segnala, come avviso, le violazioni di stile: aperture "Questa pagina descrive", titoli fissi in inglese, lessico nella forma sbagliata.
3. Gli esempi di configurazione sono file veri nel repository, caricati da un test con il parser reale. Un esempio non verificabile si intitola "Esempio illustrativo".
4. A ogni pull request: controllo dei tipi, build, validazione dei link interni.
5. Un cambiamento visibile aggiorna la pagina del sistema nello stesso commit. Prima di ogni release si rileggono le pagine dei sistemi toccati.
6. Una funzione rimossa porta via la sua pagina nello stesso commit, con un reindirizzamento se spariva una pagina intera. La rimozione si annota nelle note di release.

Non adottati: versionamento della documentazione, generazione delle pagine dal codice, controllo dei link esterni.

## Piano

**M0. Fondamenta tecniche.** Base `/Quorum` e link alla repo archiviata; Astro 7 con vetrina invariata; Starlight con una pagina di prova sotto `/Quorum/docs/`; workflow con build e validazione dei link.

**M1. Percorso minimo di chi apre un server.** Colori e logo; Cos'è QRM, Requisiti e installazione, Primo avvio; Personaggi, Economia, Lavori, Organizzazioni, Permessi; guide Creare un lavoro e Dare accesso allo staff.

**M2. Resto del Core e riferimento.** Punti di interazione, Pannello staff, Moderazione e registro, Interfaccia del client; le tre tabelle con lo script di controllo; Versioni e compatibilità; le quattro guide rimanenti.

**M3. Modules e ritiro della vecchia wiki.** Bank, Locker, Boss, Manifesto del tema; esempi come file verificati; riscrittura dei link della vetrina; rimando da `QRM-docs` e archiviazione.

**Dopo.** Installazione si aggiorna alla prima release. Screenshot.

Ogni pagina si consegna con l'elenco dei fatti verificati e di quelli non confermati.

## Punti aperti rilevati in ricognizione

- La pagina di Boss dice "dalla 0.19", lo storico diceva 0.20.
- L'esempio del frammento di tema usa l'id `bank`; il codice registra `qrm_bank`. Prefisso delle chiavi da verificare.
- `commands.shortAliases` con `/reload`: è scritto nel commento della configurazione, comportamento non verificato.
- I jar si chiamano `qrm-bank-<versione>.jar` (trattino); l'id del mod è `qrm_bank`.
- Se Locker e Boss siano obbligatori sui client non è verificato.
- Licenza Apache-2.0 e repo pubblica: da rivalutare prima della prima release se il modello di monetizzazione lo richiede.
