# Contribuire a QRM

Grazie per l'interesse. Per bug e proposte apri una issue; per le modifiche una pull request verso `main`.

## Ambiente

- JDK 25
- `./gradlew build` compila tutti i moduli ed esegue i test.
- Non usare `./gradlew clean` nel flusso normale: cancella gli argomenti di avvio generati da ModDevGradle. Se li hai persi, rigenerali con `./gradlew prepareClientRun prepareServerRun`.

## Prima di aprire una PR

- `./gradlew build` deve passare.
- Aggiungi o aggiorna i test per il comportamento che cambi.
- Modifiche all'API pubblica (`qrm-api`): aggiorna `API_VERSION` e `qrm_version` in `gradle.properties` quando la modifica lo richiede.
- Modifiche allo schema del database: aggiungi una nuova migrazione `V<n>__nome.sql` in `qrm-core/src/main/resources/db/migration`; non modificare quelle esistenti.
- Una PR, un argomento. Messaggi di commit nello stile `tipo(ambito): descrizione` (`feat`, `fix`, `docs`, `ci`, `test`).

## Stile

Codice e identificatori in inglese; commenti e messaggi per l'utente in italiano, come nel resto del progetto. Indentazione e fine riga sono descritte in `.editorconfig`.

## Licenza dei contributi

Inviando una modifica accetti che sia distribuita con la licenza del progetto (Apache-2.0).
