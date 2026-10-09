# QRM — Quorum

### Framework server-side per mondi roleplay in Minecraft

**QRM (Quorum)** è un framework per Minecraft Java Edition (NeoForge) che fornisce l'infrastruttura comune per server multiplayer persistenti: identità, personaggi, economia, eventi, persistenza e API.

Non è un mod di roleplay. È la **base su cui altri mod costruiscono interi ecosistemi di gioco**, con lo stesso ruolo che ESX e QBCore hanno per FiveM: evitare che ogni sviluppatore reinventi le stesse fondamenta.

> Fuori scope: QRM non si occupa della *scoperta* dei server (server list, registri, siti). È solo infrastruttura per sviluppatori.

---

## Principi

1. **Core minimale.** Il Core non contiene polizia, ambulanze, telefoni, veicoli, case, armi o governo. Sono moduli separati.
2. **Server authoritative.** Il client invia richieste; il server valida e modifica lo stato. Denaro, permessi, lavoro e dati persistenti non dipendono mai dal client.
3. **API stabile, implementazione libera.** Gli sviluppatori dipendono solo da `qrm-api`, versionata con semantic versioning.
4. **Sistemi interconnessi tramite eventi.** I moduli si parlano con eventi pre (annullabili/modificabili) e post, non con dipendenze dirette.
5. **Mod apposta.** QRM è un mod NeoForge: i giocatori installano il client, e questo permette contenuti nativi (blocchi, modelli, UI).

```text
QRM
├── QRM Core (api + core + neoforge)
├── QRM Jobs / Organizations   (roadmap)
├── QRM Police, EMS, Housing…  (moduli separati)
└── Community Mods
```

---

## Cosa include la v0.1

- **Player e Character** — account Minecraft → giocatore QRM → più personaggi (uno attivo alla volta, archiviazione invece di cancellazione).
- **Economy** — conti titolati a un soggetto generico (personaggio, sistema, in futuro organizzazioni), saldi per valuta, transazioni atomiche, idempotenti e immutabili.
- **Multi-valuta** — più valute definite da file o API. Il cambio valuta non è nel Core.
- **Eventi** — `PreEvent` annullabili e `PostEvent`, sincroni, con priorità e isolamento degli errori.
- **Persistenza SQL** — SQLite di default, MySQL/MariaDB/PostgreSQL opzionali; migrazioni versionate.
- **Configurazione data-driven** — valute (e in futuro job/organizzazioni) in JSON, con registrazione anche via API.
- **Mod di esempio** — `qrm-example` mostra come usare conti ed eventi.

### Esempio

Un giocatore entra, sceglie il personaggio. Un modulo esterno paga uno stipendio sul suo conto. Il Core valida la transazione; un modulo fiscale, agganciato al `TransactionPreEvent`, applica una tassa; il Core scrive tutto in modo atomico; un modulo di statistiche reagisce al `TransactionPostEvent`. Nessuno di questi moduli conosce gli altri.

---

## Architettura

```text
qrm-api        interfacce, eventi, modelli (nessuna dipendenza da Minecraft o SQL)
qrm-core       logica di dominio e persistenza (testabile senza Minecraft)
qrm-neoforge   login, comandi, rete client/server, collegamento al gioco
mod di terzi   dipendono SOLO da qrm-api
```

- Per il server c'è **un solo jar** in `mods/`: api e core sono incorporati (jar-in-jar).
- `qrm-api` è pubblicata su Maven per chi sviluppa mod.
- I servizi si ottengono da un registro: `QRM.get(EconomyService.class)`.
- Le classi `…internal` non sono API e possono cambiare.

---

## Economia: garanzie

- Importi interi in unità minime (mai decimali in virgola mobile).
- Addebito, accredito e registro in una sola transazione SQL.
- `idempotency_key` per evitare doppie esecuzioni.
- Registro immutabile; gli errori si correggono con storni.
- Nessun saldo negativo, salvo conti esplicitamente autorizzati.
- Conto `SYSTEM` per creare o distruggere denaro (stipendi, tasse, multe).
- Comando di audit per verificare la coerenza dei saldi.

---

## Roadmap

| Versione | Contenuto |
|---|---|
| **v0.1** | Player, Character, Economy multi-valuta, eventi, API, `qrm-example` |
| **v0.2** | Jobs, Grades, Permessi (generici, definiti da data + API) |
| **v0.3** | Organizations con conti propri |
| **Poi** | Moduli esterni: contante fisico, Exchange, Police, EMS, Housing… |

Non previsti nella v0.1: rete multi-server, adattatore Paper, UI avanzate, discovery dei server.

---

## Tecnologia

```text
Minecraft Java Edition 26.2
NeoForge
Java 25
Gradle
Licenza: Apache-2.0
```

> Da verificare prima di iniziare: disponibilità di NeoForge per Minecraft 26.2 con Java 25. In caso contrario si usa la versione supportata più recente.

---

## Visione

QRM non costruisce il mondo al posto degli sviluppatori. Fornisce le fondamenta comuni con cui costruirlo: un'infrastruttura stabile, documentata e aperta su cui possono nascere server e moduli molto diversi tra loro.
