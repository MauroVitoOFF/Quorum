# Comandi di QRM

Elenco completo dei comandi della mod (versione 0.28.0), ricavato dai sorgenti dei moduli.

- **Op** = permesso gamemaster (livello 2).
- `<arg>` è obbligatorio, `[arg]` è facoltativo.
- I comandi di un modulo esistono solo se quel modulo è caricato.
- **Tutto vive sotto `/qrm`.** Le radici di primo livello sono solo scorciatoie per i comandi dei giocatori.

## Indice

1. [Struttura](#struttura)
2. [Personaggi](#1-personaggi)
3. [Denaro](#2-denaro)
4. [Lavori e servizio](#3-lavori-e-servizio)
5. [Permessi](#4-permessi)
6. [Organizzazioni](#5-organizzazioni)
7. [Amministrazione](#6-amministrazione)
8. [Banca](#7-banca)
9. [Medico](#8-medico)
10. [Polizia](#9-polizia)
11. [Immersione](#10-immersione)
12. [Boss e guardaroba](#11-boss-e-guardaroba)
13. [Registro delle azioni staff](#registro-delle-azioni-staff)
14. [Riepilogo per livello di accesso](#riepilogo-per-livello-di-accesso)
15. [Cambiamenti rispetto alla 0.27](#cambiamenti-rispetto-alla-027)

---

## Struttura

| Radice | Contenuto |
|---|---|
| `/qrm` | **Tutti** i comandi di tutti i moduli caricati |
| `/job`, `/duty`, `/money`, `/bal`, `/pay`, `/char`, `/org` | Scorciatoie per i giocatori. Si spengono con l'opzione `commands.shortAliases` |
| `/boss`, `/carry` | Scorciatoie sempre attive (non dipendono da `commands.shortAliases`) |

Corrispondenza tra scorciatoie e albero `/qrm`:

| Scorciatoia | Equivalente |
|---|---|
| `/char` | `/qrm character` |
| `/job` | `/qrm job` |
| `/duty` | `/qrm duty` |
| `/money` | `/qrm money` |
| `/bal` | `/qrm money` (senza argomenti mostra il saldo) |
| `/pay` | `/qrm money pay` |
| `/org` | `/qrm org` |
| `/boss` | `/qrm boss` |
| `/carry` | `/qrm carry` |

I comandi solo-operatore (`perm`, `bank`, `medical`, `mdt`, `dummy`, `wardrobe`, `admin`) non hanno scorciatoia: si usano sempre con `/qrm`.

---

## 1. Personaggi

Radice: `/qrm character`, scorciatoia `/char`. Per tutti i giocatori.

| Comando | Cosa fa |
|---|---|
| `/char` | Elenca i tuoi personaggi |
| `/char list` | Elenca i tuoi personaggi |
| `/char create <nome> <cognome> <nascita>` | Crea un personaggio (nascita nel formato AAAA-MM-GG) |
| `/char select <n>` | Seleziona il personaggio numero n |
| `/char archive <n>` | Archivia il personaggio numero n |

## 2. Denaro

Radice: `/qrm money`, scorciatoie `/money`, `/bal`, `/pay`.

| Comando | Chi | Cosa fa |
|---|---|---|
| `/money` | tutti | Mostra i tuoi saldi |
| `/bal` | tutti | Come `/money` |
| `/money pay <giocatore> <importo> [valuta]` | tutti | Paga un altro giocatore |
| `/pay <giocatore> <importo> [valuta]` | tutti | Come `/money pay` |
| `/money give <giocatore> <importo> [valuta]` | op | Accredita denaro a un giocatore |
| `/money take <giocatore> <importo> [valuta]` | op | Toglie denaro a un giocatore |

Se non indichi la valuta si usa quella predefinita.

## 3. Lavori e servizio

Radici: `/qrm job`, `/qrm duty` e le scorciatoie `/job`, `/duty`.

| Comando | Chi | Cosa fa |
|---|---|---|
| `/job` | tutti | Il tuo lavoro, il grado, il servizio e i permessi |
| `/job info` | tutti | Come `/job` |
| `/duty` | tutti | Inverte il servizio |
| `/duty on` | tutti | Entra in servizio |
| `/duty off` | tutti | Esce dal servizio |
| `/job list` | op | Elenca i lavori definiti con i loro gradi |
| `/job set <giocatore> <lavoro> <grado>` | op | Assegna un lavoro a un giocatore |
| `/job remove <giocatore>` | op | Licenzia un giocatore |

Se `selfDuty` è disattivato nella configurazione, `/duty` risponde che il servizio si gestisce in azienda.

## 4. Permessi

Radice: `/qrm perm` (nessuna scorciatoia). Solo op. Sono i permessi del **personaggio** attivo; `<nodo>` può contenere il jolly (per esempio `police.*`).

| Comando | Cosa fa |
|---|---|
| `/qrm perm grant <giocatore> <nodo>` | Concede il permesso |
| `/qrm perm deny <giocatore> <nodo>` | Lo nega esplicitamente |
| `/qrm perm clear <giocatore> <nodo>` | Toglie la regola (concessione o negazione) |
| `/qrm perm list <giocatore>` | Mostra i permessi effettivi e le eccezioni (`+nodo` concesso, `-nodo` negato) |

I permessi **staff** del giocatore (accesso al pannello, nodi `staff.*`) sono un'altra cosa: vedi [`/qrm admin staff`](#permessi-staff-qrm-admin-staff).

## 5. Organizzazioni

Radice: `/qrm org`, scorciatoia `/org`.

| Comando | Chi | Cosa fa |
|---|---|---|
| `/org` | tutti | Le tue organizzazioni |
| `/org list` | tutti | Come `/org` |
| `/org info <org>` | tutti (solo se ne fai parte) | Dettagli di un'organizzazione; gli op la vedono sempre |
| `/org create <id> <nome>` | op | Crea un'organizzazione |
| `/org archive <id>` | op | Archivia un'organizzazione |
| `/org unarchive <id>` | op | La ripristina |
| `/org delete <id>` | op | La elimina |
| `/org rank set <org> <rank> <livello> [nodi]` | op | Crea o modifica un grado, con livello e permessi |
| `/org rank remove <org> <rank>` | op | Elimina un grado |
| `/org member set <org> <giocatore> <rank>` | op | Aggiunge il giocatore con quel grado; se è già membro, ne cambia il grado |
| `/org member remove <org> <giocatore>` | op | Rimuove un membro |

## 6. Amministrazione

Radice: `/qrm admin`. Solo op.

### Audit

| Comando | Cosa fa |
|---|---|
| `/qrm admin audit` | Esegue l'audit di **tutti** i moduli caricati, uno dopo l'altro |
| `/qrm admin audit core` | Solo i saldi (coerenza del libro mastro) |
| `/qrm admin audit bank` | Solo la banca (se il modulo è caricato): registro e contante contro cassa |

Ogni modulo aggiunge il proprio nodo sotto `audit`; quello senza argomento li esegue tutti.

### Permessi staff (`/qrm admin staff`)

Accesso del **giocatore** al pannello staff. I nodi hanno la forma `staff.…` (per esempio `staff.jobs`, `staff.*`) oppure `*`.

| Comando | Cosa fa |
|---|---|
| `/qrm admin staff grant <giocatore> <nodo>` | Concede un permesso staff |
| `/qrm admin staff clear <giocatore> <nodo>` | Lo toglie |
| `/qrm admin staff list <giocatore>` | Mostra i permessi staff del giocatore |

### Registro azioni (`/qrm admin log`)

| Comando | Cosa fa |
|---|---|
| `/qrm admin log` | Le ultime 20 azioni registrate |
| `/qrm admin log <giocatore>` | Le ultime 20 azioni riguardanti quel giocatore |
| `/qrm admin log <giocatore> <n>` | Le ultime n azioni (n da 1 a 200) |
| `/qrm admin log prune <giorni>` | Cancella le voci più vecchie di N giorni (da 1 a 36500) |

### Punti di interazione (`/qrm admin point`)

I comandi che agiscono su un punto usano il **blocco che stai guardando**.

| Comando | Cosa fa |
|---|---|
| `/qrm admin point create <tipo> job <lavoro> [grado]` | Crea un punto legato a un lavoro |
| `/qrm admin point create <tipo> org <org> [rank]` | Crea un punto legato a un'organizzazione |
| `/qrm admin point set grade <grado>` | Cambia il grado richiesto |
| `/qrm admin point set face <all\|here\|north\|south\|east\|west>` | Cambia la faccia attiva |
| `/qrm admin point set public <vero\|falso>` | Rende il punto pubblico o no |
| `/qrm admin point set name <nome>` | Rinomina il punto |
| `/qrm admin point move <id>` | Sposta il punto con quell'id sul blocco guardato |
| `/qrm admin point remove` | Elimina il punto guardato |
| `/qrm admin point remove <id>` | Elimina il punto con quell'id |
| `/qrm admin point list` | Elenca tutti i punti |
| `/qrm admin point list job <lavoro>` | Elenca i punti di un lavoro |
| `/qrm admin point list org <org>` | Elenca i punti di un'organizzazione |
| `/qrm admin point info` | Dettagli del punto guardato |

## 7. Banca

Modulo `qrm-bank`. Radice: `/qrm bank`. Solo op. L'audit della banca è `/qrm admin audit bank`.

| Comando | Cosa fa |
|---|---|
| `/qrm bank atm give` | Dà un bancomat a te |
| `/qrm bank atm give <giocatore>` | Dà un bancomat a un giocatore |
| `/qrm bank balance <nome>` | Mostra il saldo di un conto |
| `/qrm bank freeze <nome>` | Blocca un conto |
| `/qrm bank unfreeze <nome>` | Lo sblocca |

## 8. Medico

Modulo `qrm-medical`.

| Comando | Chi | Cosa fa |
|---|---|---|
| `/carry` | tutti | Inizia o smette di portare qualcuno in braccio (solo se sei in salute) |
| `/qrm carry` | tutti | Come `/carry` |
| `/qrm medical down <giocatore>` | op | Mette a terra il giocatore |
| `/qrm medical revive <giocatore>` | op | Lo rianima |
| `/qrm medical inspect <giocatore>` | op | Mostra lo stato e le ferite |
| `/qrm medical sethospital` | op | Imposta la tua posizione come punto ospedale |

## 9. Polizia

Modulo `qrm-police`. Radice: `/qrm mdt`. Op, oppure chiunque in singleplayer.

| Comando | Cosa fa |
|---|---|
| `/qrm mdt seed` | Riempie il database MDT con dati di prova (usa il tuo personaggio) |
| `/qrm mdt clear` | Svuota il database MDT |
| `/qrm mdt simulatecall` | Genera una chiamata di prova casuale vicino a te |
| `/qrm mdt simulatecall <tipo>` | Come sopra, di un tipo preciso: `medical`, `police`, `shooting` |

## 10. Immersione

Modulo `qrm-immersion`. Radice: `/qrm dummy`. Solo op.

I manichini creati con `spawn` portano l'etichetta `qrm_dummy`; `pose`, `remove` e `clear` agiscono **solo** su quelli. I manichini creati prima della 0.28 non hanno l'etichetta e vanno tolti a mano.

| Comando | Cosa fa |
|---|---|
| `/qrm dummy spawn` | Crea un manichino davanti a te (nome "Mario Rossi") |
| `/qrm dummy spawn <nome>` | Crea un manichino con quel nome |
| `/qrm dummy pose hands_up` | Il manichino più vicino (entro 16 blocchi) alza le mani |
| `/qrm dummy pose point` | Il manichino più vicino indica |
| `/qrm dummy pose none` | Il manichino più vicino torna in posizione neutra |
| `/qrm dummy remove` | Toglie il manichino più vicino (entro 16 blocchi) |
| `/qrm dummy clear` | Toglie tutti i manichini entro 16 blocchi |
| `/qrm dummy clear <raggio>` | Toglie tutti i manichini entro il raggio (da 1 a 64 blocchi) |

## 11. Boss e guardaroba

| Comando | Modulo | Chi | Cosa fa |
|---|---|---|---|
| `/boss` · `/qrm boss` | boss | solo giocatori | Apre il pannello capo |
| `/boss <org>` · `/qrm boss <org>` | boss | solo giocatori | Lo apre per l'organizzazione indicata |
| `/qrm wardrobe reload` | wardrobe | op | Ricarica le divise e dice quante ne ha caricate |

---

## Registro delle azioni staff

Le azioni degli operatori che cambiano qualcosa vengono scritte nel registro e si leggono con `/qrm admin log`. Una scrittura che fallisce non blocca mai l'azione: l'errore va nel log del server.

| Area | Azioni registrate |
|---|---|
| Lavori | `job set`, `job remove` |
| Permessi | `perm grant`, `perm deny`, `perm clear` |
| Staff | `admin staff grant`, `admin staff clear` |
| Organizzazioni | `org create`, `archive`, `unarchive`, `delete`, `rank set`, `rank remove`, `member set`, `member remove` |
| Punti | `point create`, `set`, `move`, `remove` |
| Denaro | `money give` (`cmd.money.grant`), `money take` |
| Banca | `bank atm give`, `bank freeze`, `bank unfreeze` |
| Medico | `medical down`, `medical revive` |
| Polizia | `mdt seed`, `mdt clear` |

Non sono registrati, perché non cambiano dati di gioco: `wardrobe reload`, `mdt simulatecall`, `dummy …`, `medical inspect`, `medical sethospital` e i comandi di sola lettura.

## Riepilogo per livello di accesso

### Tutti i giocatori
`/char` (list, create, select, archive) · `/money` · `/bal` · `/pay` · `/job` · `/job info` · `/duty` · `/org` · `/org list` · `/org info` · `/carry` · `/boss`

### Solo operatori
`/money give|take` · `/job list|set|remove` · `/qrm perm` · `/org create|archive|unarchive|delete|rank|member` · `/qrm admin` (audit, staff, log, point) · `/qrm bank` · `/qrm medical` · `/qrm wardrobe reload` · `/qrm dummy`

### Operatori, o chiunque in singleplayer
`/qrm mdt`

## Cambiamenti rispetto alla 0.27

| Prima | Ora |
|---|---|
| `/quorum mdt …` | `/qrm mdt …` |
| `/quorum dummy …` | `/qrm dummy …` |
| `/bank …` | `/qrm bank …` |
| `/bank audit` | `/qrm admin audit bank` (o `/qrm admin audit` per tutti i moduli) |
| `/wardrobe reload` | `/qrm wardrobe reload` |
| `/balance`, `/qrm balance` | `/money` o `/bal` |
| `/qrm pay` | `/qrm money pay` (o `/pay`) |
| `/perm …` | `/qrm perm …` |
| `/org member add …` e `/org member setrank …` | `/org member set …` |
| `/qrm admin staff revoke …` | `/qrm admin staff clear …` |
| (non esisteva) | `/qrm perm list <giocatore>`, `/qrm dummy remove`, `/qrm dummy clear [raggio]`, `/qrm boss`, `/qrm carry`, `/qrm admin audit [core\|bank]` |

Le voci del registro staff scritte prima della 0.28 mantengono i vecchi nomi (`cmd.staff.revoke`, `cmd.org.member_add`, `cmd.org.member_set_rank`).

## Note

- `qrm-locker` non ha comandi: funziona con gli oggetti e le interazioni in gioco.
- `/qrm medical`, `/qrm dummy`, `/qrm bank`, `/qrm mdt` e `/qrm wardrobe` sono registrati da moduli diversi sulla stessa radice `/qrm`: esistono solo se quei moduli sono presenti.
- La chat non ha ancora comandi propri (`/me`, `/do` e simili): non esistono.
- Se `send_command_feedback` è `false` nel mondo, i comandi funzionano ma non mostrano l'esito positivo nella chat.
