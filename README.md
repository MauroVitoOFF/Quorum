# QRM (Quorum)

Framework per server Minecraft di roleplay e mondo persistente, per NeoForge (Minecraft 26.2, Java 25).
QRM fornisce la base comune (personaggi, lavori e gradi, permessi, organizzazioni, economia, eventi, persistenza) su cui altri mod possono costruire il proprio ecosistema di gioco.

> Stato: versione `0.x`. L'API pubblica (`qrm-api`) può ancora cambiare fra versioni minori.

## Documentazione

La documentazione per utenti e sviluppatori di addon è nel sito: <https://maurovitooff.github.io/QRM-docs/>

## Moduli

| Modulo | Contenuto |
|---|---|
| `qrm-api` | API pubblica per gli addon (nessuna dipendenza da Minecraft) |
| `qrm-core` | Implementazione dei servizi e persistenza SQLite (migrazioni in `db/migration`) |
| `qrm-client-api` | API lato client |
| `qrm-neoforge` | Il mod: integrazione con NeoForge, comandi, GUI |
| `qrm-bank` | Mod addon: conti e operazioni bancarie |
| `qrm-example` | Addon minimo di esempio |

Gli script in `tools/` generano le texture della GUI; `docs/samples/` contiene un resource pack di esempio.

## Compilare

Servono un JDK 25 e una connessione di rete (il wrapper scarica Gradle e le dipendenze).

```bash
./gradlew build
```

Il jar del mod si trova in `qrm-neoforge/build/libs/`. Per avviare client o server di sviluppo si usano le run configuration generate da ModDevGradle (`./gradlew :qrm-neoforge:runClient`, `:runServer`).

## Contribuire e sicurezza

- Linee guida per i contributi: [CONTRIBUTING.md](CONTRIBUTING.md)
- Segnalazione di vulnerabilità: [SECURITY.md](SECURITY.md)

## Licenza

Apache License 2.0, vedi [LICENSE](LICENSE) e [NOTICE](NOTICE).
