# QRM — Quorum

### Minecraft Living-World & Roleplay Framework

**QRM (Quorum)** è un framework per Minecraft Java Edition progettato per permettere la creazione di server multiplayer persistenti, dinamici e basati su sistemi di gioco interconnessi.

QRM non è semplicemente una mod che aggiunge un sistema di roleplay.

È una **base tecnologica sulla quale altri mod possono costruire interi ecosistemi di gioco**.

L'idea nasce dall'esigenza di portare in Minecraft un concetto simile a quello che framework come ESX e QBCore hanno rappresentato per FiveM: fornire una base comune sulla quale sviluppatori diversi possano costruire sistemi, modalità di gioco e server complessi senza dover reinventare ogni volta le stesse infrastrutture.

---

## Cosa fa concretamente QRM?

QRM introduce nel server una serie di sistemi fondamentali che normalmente Minecraft non fornisce come framework unico.

Tra questi:

- gestione dei giocatori;
- gestione dei personaggi;
- lavori e gradi;
- permessi;
- organizzazioni;
- economia;
- conti;
- transazioni;
- eventi;
- persistenza dei dati;
- configurazione;
- API per altri mod.

Questi sistemi possono poi comunicare tra loro.

Un giocatore può avere un personaggio.

Il personaggio può avere un lavoro.

Il lavoro può determinare un insieme di permessi.

Il personaggio può appartenere a un'organizzazione.

L'organizzazione può possedere uno o più conti.

I conti possono effettuare transazioni.

Le transazioni possono generare eventi.

Altri mod possono reagire a quegli eventi.

Il risultato è un mondo in cui i sistemi non sono isolati, ma fanno parte di un'unica infrastruttura.

---

# Un esempio pratico

Immaginiamo un server che utilizza QRM.

Un giocatore entra nel server e seleziona il proprio personaggio.

Il personaggio è un cittadino del mondo di gioco.

Il personaggio viene assunto da un'azienda come meccanico.

Il suo lavoro gli assegna un determinato grado e determinate autorizzazioni.

L'azienda è un'organizzazione QRM e possiede un conto economico.

Quando il giocatore completa il proprio lavoro, un altro modulo può effettuare un pagamento sul conto del personaggio.

La transazione viene registrata.

Un altro sistema può reagire alla transazione.

Il sistema di statistiche può registrare l'attività.

Un sistema fiscale potrebbe applicare una tassa.

Un sistema governativo potrebbe utilizzare quei dati.

Nessuno di questi sistemi deve essere necessariamente contenuto nel Core di QRM.

QRM fornisce le fondamenta che permettono loro di comunicare.

---

# QRM non è un "Mega Mod"

Uno degli obiettivi principali del progetto è evitare di creare una mod enorme contenente ogni possibile funzionalità.

QRM Core non deve contenere:

- polizia;
- ambulanze;
- telefoni;
- veicoli;
- case;
- criminalità;
- negozi;
- banche con interfaccia grafica;
- armi;
- sistemi governativi;
- meccaniche specifiche di GTA o FiveM.

Queste funzionalità possono essere sviluppate come mod separate.

Ad esempio:

```text
QRM
│
├── QRM Core
│
├── QRM Police
├── QRM EMS
├── QRM Government
├── QRM Housing
├── QRM Phone
├── QRM Vehicles
│
└── Community Mods
```

In questo modo un server può installare soltanto ciò di cui ha bisogno.

---

# Il vero obiettivo: un ecosistema

Il progetto non vuole essere solamente una mod.

L'obiettivo è creare un **ecosistema per server Minecraft**.

Uno sviluppatore potrebbe creare un proprio mod utilizzando QRM come infrastruttura.

Per esempio:

```text
MyPoliceMod
      │
      └── QRM API
```

Il mod potrebbe utilizzare i sistemi di QRM per:

- identificare i giocatori;
- ottenere il loro personaggio;
- verificare i permessi;
- gestire il lavoro;
- gestire l'organizzazione;
- effettuare transazioni;
- ascoltare eventi;
- salvare dati.

Lo sviluppatore non deve quindi implementare nuovamente tutti questi sistemi.

---

# Un mondo realmente interconnesso

La visione a lungo termine di QRM è creare server nei quali le diverse attività dei giocatori producono conseguenze sul mondo.

Un lavoro genera reddito.

Il reddito alimenta l'economia.

L'economia alimenta le aziende.

Le aziende assumono giocatori.

Le organizzazioni hanno responsabilità e risorse.

Le decisioni dei giocatori generano eventi.

Gli eventi possono modificare altri sistemi.

Questo crea un **living world**.

Non semplicemente un insieme di feature.

---

# Player e Character

QRM distingue il giocatore Minecraft dal personaggio all'interno del mondo.

Concettualmente:

```text
Minecraft Account
       │
       ▼
   QRM Player
       │
       ├── Character A
       ├── Character B
       └── Character C
```

Questo permette di supportare in futuro sistemi come:

- più personaggi;
- selezione del personaggio;
- identità differenti;
- progressione separata;
- ruoli differenti.

---

# Jobs

I lavori di QRM sono generici.

Il Core non contiene necessariamente "Police", "Doctor" o "Mechanic".

Esiste invece un sistema di:

```text
Job
 └── Grade
```

Un mod può quindi creare:

```text
Police
├── Cadet
├── Officer
├── Sergeant
└── Chief
```

Un altro può creare:

```text
Mechanic
├── Apprentice
├── Mechanic
└── Manager
```

Il framework rimane indipendente dal contenuto.

---

# Organizations

Le organizzazioni sono uno dei pilastri di QRM.

Un'organizzazione può rappresentare praticamente qualsiasi gruppo strutturato:

- azienda;
- dipartimento di polizia;
- ospedale;
- governo;
- gang;
- associazione;
- negozio;
- corporazione.

Questo permette ai moduli di costruire sistemi complessi senza creare continuamente nuove strutture nel Core.

---

# Economy

L'economia di QRM non è semplicemente un numero chiamato "money".

Il sistema è basato su:

```text
Account
    │
    ▼
Transactions
```

Le operazioni economiche possono quindi essere registrate e analizzate.

Questo permette di costruire successivamente:

- banche;
- stipendi;
- aziende;
- negozi;
- tasse;
- multe;
- commercio;
- sistemi finanziari;
- sistemi economici personalizzati.

---

# API per gli sviluppatori

Uno degli aspetti più importanti di QRM è l'API pubblica.

Un altro sviluppatore deve poter utilizzare QRM senza conoscere come funziona internamente.

Il principio è:

```text
Third-party Mod
       │
       ▼
    QRM API
       │
       ▼
   QRM Core
```

L'implementazione interna può cambiare.

L'API pubblica deve invece essere progettata per essere stabile, documentata e utilizzabile da altri sviluppatori.

---

# Server Authoritative

QRM è progettato principalmente per il lato server.

Il client non deve poter decidere autonomamente informazioni sensibili come:

- denaro;
- permessi;
- lavoro;
- grado;
- appartenenza a un'organizzazione;
- transazioni;
- dati persistenti.

Il client può inviare richieste.

Il server le valida.

Il server modifica lo stato.

Questo rende l'architettura adatta a server multiplayer competitivi e persistenti.

---

# Tecnologia

QRM è sviluppato per:

```text
Minecraft Java Edition
NeoForge
Java 25
Gradle
```

La prima versione è progettata per Minecraft 26.2.

NeoForge è il modding API utilizzato dal progetto.

---

# Visione a lungo termine

L'obiettivo finale di QRM è diventare per Minecraft ciò che un framework server-side è per un ecosistema multiplayer:

**una fondazione comune.**

Non vogliamo creare un singolo server.

Non vogliamo creare una singola modalità di gioco.

Non vogliamo creare una semplice mod RP.

Vogliamo creare gli strumenti con cui altri sviluppatori possano costruire mondi multiplayer complessi.

In prospettiva:

```text
                         QRM
                          │
          ┌───────────────┼───────────────┐
          │               │               │
       Economy          Jobs        Organizations
          │               │               │
          └───────────────┼───────────────┘
                          │
                    QRM Ecosystem
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
      Police             EMS             Housing
        │                 │                 │
        └─────────────────┼─────────────────┘
                          │
                  Minecraft Server
```

**QRM non costruisce il mondo al posto degli sviluppatori.**

**Fornisce gli strumenti per costruirlo.**
