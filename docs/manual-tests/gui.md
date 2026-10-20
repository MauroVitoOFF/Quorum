# Prova in gioco della GUI v0.4 (a carico dell'autore)

Il rendering, la schermata, il tasto, la ricarica del tema e i pacchetti end-to-end non sono verificati in automatico:
questa lista serve a controllarli a mano. Per ogni voce annotare `OK` o le note.
I comandi in gioco (`/qrm ...`, inclusi i comandi v0.3 `/org ...` e `/org ...`) non sono coperti da questa lista e
restano non verificati in gioco.

## Avvio

1. Server: `./gradlew :qrm-neoforge:runServer`
2. Client: `./gradlew :qrm-neoforge:runClient`, poi collegarsi a `localhost`.
3. Se mancano i file `qrm-neoforge/build/moddev/*RunVmArgs.txt` (non usare `clean`, che cancella `build/moddev`):
   `./gradlew :qrm-neoforge:prepareClientRun :qrm-neoforge:prepareServerRun`.

## Lista di controllo

| #   | Controllo                                                                                                                                                                                                                                                         | Esito / note |
| --- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------ |
| 1   | **Giocatore nuovo**: entrando, la schermata si apre da sola in modalità creazione; la carta d'identità mostra la testa del tuo personaggio; la firma ("M. Rossi") si compila mentre si digita; il toggle M/F si può cambiare con mouse e tastiera (solo grafico). | OK |
| 2   | **Dati errati**: campo vuoto, data `31/02/1990`, data futura producono i messaggi giusti (prima dell'invio oppure con l'esito del server). Nessun personaggio creato.                                                                                             | OK |
| 3   | **Creazione riuscita**: si torna al carosello con la nuova carta e il personaggio risulta attivo.                                                                                                                                                                 | OK |
| 4   | **Secondo personaggio**: frecce, rotella e clic su una carta laterale spostano la selezione; Invio = Gioca; il personaggio attivo cambia.                                                                                                                         | OK |
| 5   | **Slot esauriti**: raggiunto il limite, la carta "nuovo" è bloccata (lucchetto) e non permette di creare.                                                                                                                                                         | OK |
| 6   | **Archiviazione con conferma**: Archivia chiede conferma; Annulla non archivia; Conferma sì e la carta sparisce dal carosello.                                                                                                                                    | OK |
| 7   | **ESC e promemoria**: ESC chiude la schermata; senza personaggio attivo e senza schermate aperte, ogni ~30 s compare il promemoria (overlay sopra la hotbar) con il tasto giusto.                                                                                 | ESC OK; promemoria NON comparso dopo l'attesa (senza personaggio attivo): da indagare |
| 8   | **Tasto `O`**: riapre la schermata (azione `key.qrm.characters`, categoria QRM); il tasto si può cambiare in Opzioni > Controlli.                                                                                                                                 | OK |
| 9   | **Tema**: copiare `docs/samples/qrm-sample-pack` in `qrm-neoforge/run-client/resourcepacks/`, attivarlo, premere F3+T: l'accento diventa ambra (`#F2B13C`) e il fondo (scrim) cambia tono, senza riavvio.                                                          | OK |
| 10  | **Manifesto invalido**: rompere il JSON del pack (es. togliere una parentesi) e F3+T: errore nel log (`QRM theme manifest from the active pack is invalid`) e si vede il tema del mod.                                                                            | OK |
| 11  | **Client senza il mod**: entra nel server e può usare `/qrm character list`. Nota: un client con il mod ma con una versione di protocollo diversa viene disconnesso da NeoForge in fase di negoziazione; è atteso e non va segnalato come difetto.                | OK |
| 12  | **Isolamento fra giocatori**: dalla GUI non è possibile alcuna azione sul personaggio di un altro (non esistono comandi o pacchetti esposti per farlo: verificare solo che la GUI non lo permetta).                                                               | OK |
| 13  | **Log del server**: nessun errore né eccezione durante tutta la prova.                                                                                                                                                                                            | OK (nessun errore) |

## Controlli aggiuntivi (limiti noti della v0.4)

| #   | Controllo                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Esito / note |
| --- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------ |
| 14  | **Ingresso nel mondo**: la schermata che si apre da sola all'ingresso potrebbe sostituire la schermata di caricamento del livello di vanilla: verificare che non resti il mondo "a metà" né un caricamento interrotto e che il gioco sia normale dopo la chiusura.                                                                                                                                                                                                                                                                                           | OK |
| 15  | **"Gioca"**: dopo Gioca con esito positivo la schermata si chiude da sola e si torna in gioco; se la selezione fallisce (es. personaggio non utilizzabile) la schermata resta aperta e mostra l'errore. Il roster che segue l'esito non deve riaprirla.                                                                                                                                                                                                                                                                                                      | Non verificato (non so come provare l'errore di selezione); il caso positivo (chiusura) è coperto dal punto 4 |
| 16  | **Altezze della GUI 240 / 256 / 270**: cambiare scala GUI e finestra (o dimensione della finestra) e controllare lista, creazione e conferma: nessun elemento fuori schermo o sovrapposto. In lista le carte si rimpiccioliscono (scala minima 0,7) e il blocco è centrato in verticale. Sotto altezza GUI 229 il suggerimento in basso può sconfinare.                                                                                                                                                                                                      | OK |
| 17  | **Intestazione**: titolo e slot hanno posizione diversa fra lista e conferma; la creazione ha il proprio titolo "Creazione personaggio" sopra la carta e non mostra gli slot; verificare che sia accettabile.                                                                                                                                                                                                                                                                                                                                                | OK |
| 18  | **Nomi lunghi**: nome e cognome della lunghezza massima consentita nella carta (anteprima e carosello) non escono dalla carta.                                                                                                                                                                                                                                                                                                                                                                                                                               | OK |
| 19  | **Archiviazione solo da tastiera**: (1) dal carosello premere Tab finché il pulsante Archivia ha il focus, poi premere Spazio; (2) si deve arrivare al pannello di conferma con il pulsante Annulla col focus (focus visibile); (3) tenendo premuto Spazio/Invio (ripetizione del tasto) NON deve archiviare; (4) Invio/Spazio su Annulla torna al carosello senza archiviare; (5) ripetere e confermare col mouse (oppure Tab fino al pulsante di conferma e Spazio dopo aver rilasciato tutti i tasti) per verificare che l'archiviazione funzioni ancora. | OK |
| 20  | **Invio nei campi di testo**: in un campo di testo Invio non invia il modulo (comportamento noto): usare il pulsante.                                                                                                                                                                                                                                                                                                                                                                                                                                        | OK |
| 21  | **Invio su Archivia**: con il focus sul pulsante Archivia, Invio esegue Gioca (comportamento noto).                                                                                                                                                                                                                                                                                                                                                                                                                                                          | OK |
| 22  | **Carte laterali**: non sono semitrasparenti (il rendering del tema non supporta l'alpha): verificare che l'aspetto sia accettabile.                                                                                                                                                                                                                                                                                                                                                                                                                         | OK |
| 23  | **Ricarica del tema con la schermata aperta**: dopo F3+T a schermata aperta i widget restano nelle posizioni vecchie fino alla prossima ricostruzione (cambio modalità o ridimensionamento): verificare che non crei problemi.                                                                                                                                                                                                                                                                                                                               | OK |
| 24  | **Attesa**: l'indicatore di attesa non ha timeout; se un pacchetto non risponde la schermata resta in attesa (ESC la chiude).                                                                                                                                                                                                                                                                                                                                                                                                                                | OK |
| 25  | **Data di nascita**: il limite superiore usa la data UTC del server; controllare vicino alla mezzanotte UTC se serve.                                                                                                                                                                                                                                                                                                                                                                                                                                        | Non testato |
| 26  | **Tastiera sui pulsanti**: Tab porta il focus su un pulsante; Spazio o Invio lo attivano (suono del clic una volta sola).                                                                                                                                                                                                                                                                                                                                                                                                                                    | Non indicato |
| 27  | **Contenitore subito dopo `O`**: premere `O` e aprire subito una cassa/contenitore: la schermata Personaggi NON deve sostituire la schermata del contenitore.                                                                                                                                                                                                                                                                                                                                                                                                | OK |

## Prova in gioco v0.5 (HUD e hub)

| #   | Controllo | Esito / note |
| --- | --------- | ------------ |
| 28  | **HUD base**: con un personaggio attivo compare in alto a sinistra un pannello con nome, saldo (valuta principale) e lavoro; senza personaggio attivo non compare. | OK |
| 29  | **Valuta principale**: cambiare `economy.defaultCurrency` nella configurazione del server (valuta registrata) e riavviare: l'HUD mostra quella valuta. Con un codice non registrato l'HUD mostra la prima valuta registrata e il log del server riporta un solo avviso. | OK |
| 30  | **Aggiornamento del saldo**: `/money give <giocatore> 10` cambia il saldo nell'HUD entro un secondo; `/qrm pay` aggiorna entrambi i giocatori. | OK |
| 31  | **Lavoro e servizio**: `/job set …` cambia la riga del lavoro; `/qrm duty on` e `/qrm duty off` cambiano il colore del quadratino (verde/rosso); senza lavoro compare "Disoccupato". | OK |
| 32  | **Cambio personaggio**: selezionare un altro personaggio dalla schermata Personaggi aggiorna nome, saldo e lavoro; archiviare l'ultimo personaggio attivo nasconde l'HUD. | OK |
| 33  | **Nascondere l'HUD**: il tasto `H` lo nasconde e lo mostra (la scelta resta dopo il riavvio del client); F1 lo nasconde con il resto dell'interfaccia; con una schermata aperta (inventario, Personaggi) non compare. | OK |
| 34  | **Testi lunghi**: un nome o un lavoro molto lunghi sono troncati con "…" entro la larghezza massima. | OK |
| 35  | **Hub**: il tasto `K` apre la schermata con le schede Conti, Lavoro e Organizzazioni; frecce sinistra/destra e clic cambiano scheda; ESC o `K` chiudono; il gioco non va in pausa. | OK |
| 36  | **Contenuto dell'hub**: Conti mostra tutte le valute (la principale per prima, con "principale"); Lavoro mostra lavoro, grado, servizio e data; Organizzazioni mostra rango o "tramite lavoro"; i casi vuoti mostrano il testo "Nessun …". | OK |
| 37  | **Scorrimento**: con più di 8 righe (molti conti o organizzazioni) la rotella scorre e compare l'indicatore "x-y/n". | Non testato (servono più conti per superare 8 righe) |
| 38  | **Hub senza personaggio**: `K` senza personaggio attivo mostra il promemoria sopra la hotbar e non apre la schermata. | OK |
| 39  | **Hub aggiornato dal vivo**: con l'hub aperto, `/money give` aggiorna i saldi senza chiuderlo. | Non testato |
| 40  | **Protocollo**: un client v0.4 che entra in un server v0.5 viene respinto da NeoForge alla negoziazione (atteso). | Non testato |
| 41  | **Server dedicato**: avviato senza errori di classi client; nessuna eccezione nel log durante la prova. | OK |
| 42  | **Servizio dall'hub (selfDuty = true)**: nella scheda Lavoro, con un lavoro, compare "Entra di servizio" (fuori servizio) o "Esci dal servizio" (in servizio); il clic cambia lo stato e il quadratino dell'HUD cambia colore. | |
| 43  | **Servizio bloccato (selfDuty = false)**: nessun pulsante nella scheda Lavoro; `/qrm duty on` risponde "Il servizio si gestisce in azienda."; `/job set …` e un modulo che chiama `JobService.setOnDuty` continuano a funzionare; il logout mette fuori servizio. | |
| 44  | **Pulsante assente quando non serve**: senza lavoro, o con un lavoro non più definito, il pulsante non compare. | |
| 45  | **Schede configurate**: `gui.hub.tabs = ["orgs", "job"]` mostra solo quelle due, in quell'ordine; frecce e clic valgono solo fra quelle; nomi sconosciuti ignorati. | |
| 46  | **Hub non disponibile**: `gui.hub.tabs = []` fa mostrare "Hub non disponibile su questo server." al tasto dell'hub, senza aprire nulla. | |
| 47  | **HUD configurato**: `gui.hud.elements = ["balance"]` mostra solo il saldo e il pannello si adatta; `[]` nasconde l'HUD. | |
| 48  | **Configurazione dal server**: i valori vengono applicati dopo il riavvio del server e il client li riceve senza modifiche locali. | |

## Prova in gioco v0.6 (API client)

| #   | Controllo | Esito / note |
| --- | --------- | ------------ |
| 49  | **Regressione**: schermata Personaggi, HUD e hub si comportano come nella v0.5 (stesso aspetto, tasti `O`, `K`, `H`). | |
| 50  | _Non più disponibile: richiedeva il Module `qrm-example`, rimosso._ | |
| 51  | _Non più disponibile: richiedeva il Module `qrm-example`, rimosso._ | |
| 52  | _Non più disponibile: richiedeva il Module `qrm-example`, rimosso._ | |
| 53  | _Non più disponibile: richiedeva il Module `qrm-example`, rimosso._ | |
| 54  | **Riga di modulo difettosa**: una riga che lancia un'eccezione non rompe l'HUD e il log la segnala una sola volta. | |
| 55  | **Server dedicato**: parte senza errori di classi client con `qrm-client-api` incorporata. | OK (log: nessun errore di classi client) |
| 56  | **Build Gradle**: `./gradlew build` sulla macchina dell'autore compila `qrm-client-api`, il jar di `qrm-neoforge` contiene le classi di `qrm-client-api` (non come jar-in-jar) e `qrm-bank` compila con la sola dipendenza `compileOnly`. | OK (build Gradle verde, 110 test; classi di qrm-client-api nel jar di qrm) |

## Prova in gioco v0.9 (Banca nel pannello staff)

Servono la banca installata su server e client, un operatore e un giocatore con personaggio attivo e un po' di denaro. La
voce Banca (sezione *Economy* del pannello staff, `F7`) contiene tutta la gestione del denaro. Il bersaglio è la scheda del
giocatore aperta nel pannello o, se non c'è, il giocatore inquadrato.

| #   | Controllo | Esito / note |
| --- | --------- | ------------ |
| 69  | **Voce**: nel pannello (`F7`) la sezione *Economy* contiene "Banca", grigia senza bersaglio e attiva con un bersaglio; con la banca non installata sul server (client con la banca, server senza) la voce non compare e *Economy* resta attenuata. | |
| 70  | **Stato**: aprendola si vedono "Bersaglio: nome", "Conto attivo" (verde), a sinistra i saldi con la valuta (o "Conto vuoto") e a destra gli ultimi movimenti con data, motivo (es. "Accredito amministrativo") e importo con segno (verde le entrate, rosso le uscite). | |
| 71  | **Dai / Togli**: con importo `12.50` e valuta `USD` (il pulsante della valuta le alterna) "Dai" accredita e saldi e movimenti si aggiornano da soli; "Togli" chiede conferma e non porta il saldo sotto zero ("Fondi insufficienti."). Importi non validi (`abc`, `-3`, `0`, troppi decimali) mostrano "Importo o dati non validi."; i pulsanti sono spenti con il campo vuoto; dopo un esito riuscito il campo si svuota. | |
| 72  | **Congela**: la prima pressione dice "Premi di nuovo per confermare", la seconda congela: la riga diventa "Conto congelato" (rossa), "Congela" si spegne e "Scongela" si accende; il bersaglio non può più ritirare all'ATM e "Dai" risponde "Conto congelato.". | |
| 73  | **Scongela**: ripristina il conto e il bersaglio torna a operare. | |
| 74  | **Dai ATM**: l'ATM compare nell'inventario del bersaglio (o a terra se pieno) e lo stato dice "ATM consegnato.". | |
| 75  | **Audit**: con registro e cassa coerenti compare "Audit OK: il registro è coerente." in verde; con contante in gioco superiore alla cassa (es. banconota data da `/give`) compare "Contanti X > cassa Y" in rosso. | |
| 76  | **Navigazione**: "Indietro" riporta alla Home con il bersaglio ancora scelto; "Aggiorna" ricarica lo stato; la rotella scorre saldi o movimenti a seconda della colonna sotto il mouse; Esc chiude. | |
| 77  | **Permesso**: togliere il livello operatore con la schermata aperta e premere un'azione risponde "Non hai il permesso." senza effetto. | |
| 78  | **Coerenza coi comandi**: un accredito dalla schermata compare nell'ATM del bersaglio con lo stesso testo di `/money give`; `/qrm bank freeze|unfreeze|audit|balance` si comportano come prima (stessi messaggi). | |
| 79  | **Log**: dai/togli, congela, scongela e Dai ATM compaiono nel log del server con operatore, bersaglio e (per il denaro) importo e valuta. | |

## Prova in gioco v0.10 (pannello staff HUD)

Servono un operatore, un secondo giocatore con personaggio attivo (meglio con lavoro e organizzazione) e la banca installata.
Tasti di default: `F7` mostra/nasconde il pannello, `Alt sinistro` (tenuto premuto) libera il cursore; entrambi si cambiano in
Opzioni > Controlli > QRM.

| #   | Controllo | Esito / note |
| --- | --------- | ------------ |
| 80  | **Home**: `F7` mostra "QRM STAFF" sotto l'HUD con *Players N* (N = giocatori online) e le sezioni Player Management, Server Management, Moderation, Economy, Organizations, Logs & Transactions, Quick Actions. Le sezioni senza voci sono attenuate e `→`/Invio non le apre. Il personaggio si muove normalmente; un secondo `F7` nasconde il pannello. | |
| 81  | **Tastiera**: `↓`/`↑` (con giro), `→`/Invio entrano, `←`/Backspace tornano indietro tenendo selezionata la riga di provenienza, `←` dalla Home chiude. Chat/inventario/pausa nascondono il pannello e alla chiusura riappare. | |
| 82  | **Players**: elenco ordinato per nome con il personaggio a destra; la lista si aggiorna da sola entro pochi secondi (entra/esce un giocatore). | |
| 83  | **Scheda**: aprendo un giocatore compaiono la testa della skin, nome, *Online: Yes*, *Character*, *Job*, *Organization* (— se mancanti) e le dieci azioni nell'ordine: Teleport to, Bring here, Freeze, Heal, Give item, Set gamemode, Kick, Ban, View character, View permissions. Se il giocatore esce, la scheda si chiude e si torna alla lista. | |
| 84  | **Messaggi**: ogni azione mostra un esito sopra la barra dell'inventario (verde se riuscita, rossa altrimenti); un client che invia un'azione senza il nodo `staff.player.<azione>` riceve "Non hai il permesso." e non succede nulla. | |
| 85  | **Cursore a richiesta**: tenendo `Alt sinistro` compare il cursore e la visuale non ruota; clic su una riga la apre, clic sul titolo torna indietro, la rotella scorre; al rilascio il mouse torna catturato e il clic non lo riprende da solo. | |
| 86  | **Sezioni con voci**: *Player Management* (vuota per ora), *Organizations* (vuota per ora), *Economy* (Banca, solo se il server ha la banca). Aprono la schermata già sul punto giusto; Esc/Indietro tornano al gioco. | |
| 87  | **Non operatore**: senza permessi `F7` non apre nulla. Con `/qrm admin staff grant <p> staff.panel` si vede la Home ma senza azioni nella scheda; con `staff.player.view_permissions` compare solo quella azione; con `staff.*` tutto; con `revoke` sparisce subito. `/qrm admin staff list <p>` elenca i grant. | |
| 88  | **Server autorevole**: da non operatore con solo `staff.jobs`, un client modificato che invia un'azione di organizzazioni o permessi riceve "Non hai il permesso." senza effetto; lo stesso per la banca senza `staff.bank`. | |
| 89  | **Banca da modulo**: un modulo che registra una voce con `StaffEntries.register` la vede nella sezione scelta, filtrata dal suo nodo (`staff.<nodo>`), attenuata se la sezione resta senza voci consentite. | |

### Azioni sul giocatore (v0.10)

| #   | Controllo | Esito / note |
| --- | --------- | ------------ |
| 90  | **Heal**: con il bersaglio ferito e affamato, "Heal" lo riporta a vita e fame piene e spegne il fuoco. | |
| 91  | **Teleport to / Bring here**: "Teleport to" porta lo staff dal giocatore, "Bring here" porta il giocatore dallo staff, anche se sono in dimensioni diverse; su se stessi risponde "Dati non validi.". | |
| 92  | **Set gamemode**: apre una pagina con Survival, Creative, Adventure, Spectator; la scelta cambia la modalità del bersaglio; `←` torna alla scheda. | |
| 93  | **Freeze**: il bersaglio non riesce più a muoversi (né a cambiare dimensione) e riceve un avviso; una seconda pressione lo scongela; al logout si scongela da solo. | |
| 94  | **Give item**: apre l'input box; `diamond 16` dà 16 diamanti (l'eccedenza cade a terra se l'inventario è pieno); `minecraft:apple x3` funziona; Conferma è spento a campo vuoto; `nonesiste` risponde "Oggetto sconosciuto."; Esc annulla senza fare nulla. | |
| 95  | **Kick**: apre l'input box del motivo (facoltativo); Invio espelle il bersaglio, che vede il motivo; senza motivo usa quello standard. | |
| 96  | **Ban**: apre l'input box con durata e motivo; `7d` banna per 7 giorni, vuoto è permanente, `abc` risponde "Dati non validi."; il bersaglio viene espulso e non può rientrare (`/pardon <nome>` lo ripristina). | |
| 97  | **View character**: apre una pagina con nome, nascita, stato, lavoro e grado, in/fuori servizio e organizzazioni con rango (— se mancanti); un giocatore senza personaggio attivo risponde "… non ha un personaggio attivo."; `←` torna alla scheda. | |
| 98  | **View permissions**: apre una pagina con i permessi effettivi e gli override (Concesso +/Negato −); la rotella scorre quando sono tanti. | |
| 99  | **Permessi parziali**: con `/qrm admin staff grant <p> staff.player.heal` la scheda mostra solo "Heal"; con `staff.player.*` tutte le azioni; le altre non compaiono. | |
| 100 | **Log**: ogni azione compare nel log del server con operatore, azione, bersaglio, argomento ed esito. | |

## Prova in gioco v0.12 (registro delle azioni staff)

Servono un operatore, un secondo giocatore con personaggio attivo e la banca installata. Il registro è la tabella `qrm_staff_log`
del database di QRM; si consulta dal pannello (`F7`) e con `/qrm admin log`.

| #   | Controllo | Esito / note |
| --- | --------- | ------------ |
| 101 | **Azioni del pannello**: dopo Heal, Teleport to, Bring here, Freeze, Give item, Set gamemode, Kick e Ban, "Ultime azioni" (*Logs & Transactions*) mostra una riga per azione: data e staff sopra, `panel.<azione> → bersaglio · dettaglio` sotto (es. `panel.give_item → Luca · diamond 16`, `panel.ban → Luca · 7d \| spam`). | |
| 102 | **Letture escluse**: View character, View permissions e Cronologia non aggiungono righe. | |
| 103 | **Tentativi negati**: da un account senza `staff.player.heal` (con altri nodi staff), un'azione Heal non riesce e il registro mostra una riga con `✗` davanti. Bersaglio assente, dati non validi e limite di frequenza non aggiungono righe. | |
| 104 | **Banca**: dai, togli, congela, scongela e Dai ATM dalla schermata Banca registrano `bank.<azione>` con importo e valuta nel dettaglio; stato e audit no. | |
| 105 | **Comandi**: `/money give|take`, `/job set` e `/job remove`, `/qrm perm grant|deny|clear`, `/org create|archive|rank|member`, `/qrm admin staff grant|clear` e `/qrm bank freeze|unfreeze|atm give` registrano `cmd.*` con il testo del comando; `list`, `info`, `balance`, `audit` e `staff list` no. Da console lo staff appare come "Server". | |
| 106 | **Cronologia**: nella scheda di un giocatore, *History* mostra le azioni in cui è staff o bersaglio. | |
| 107 | **Permesso `staff.logs`**: senza il nodo, né "Ultime azioni" né *History* compaiono; con `/qrm admin staff grant <p> staff.logs` compaiono subito; un client modificato che li richiede lo stesso non riceve nulla. | |
| 108 | **Comando**: `/qrm admin log` mostra le ultime 20 azioni, `/qrm admin log <giocatore> [n]` quelle del giocatore (n fino a 200); il registro vuoto risponde "Nessuna azione registrata.". | |
| 109 | **Pulizia**: `/qrm admin log prune <giorni>` cancella le voci più vecchie, risponde con quante e scrive `cmd.log.prune`. Le voci non scadono mai da sole. | |
| 110 | **Persistenza**: dopo un riavvio del server le voci ci sono ancora e i nuovi numeri progressivi continuano. | |
| 111 | **Robustezza**: se la scrittura nel database fallisce (es. database in sola lettura) l'azione dello staff riesce comunque e l'errore compare nel log del server. | |

## Prova in gioco v0.13 (moderazione)

Servono un operatore, un secondo giocatore con personaggio attivo e un terzo account (o lo stesso secondo giocatore dopo un
rientro) per le prove di persistenza. Le azioni stanno nella scheda del giocatore (`F7` → *Players*); le pagine nella sezione
*Moderation*.

| #   | Controllo | Esito / note |
| --- | --------- | ------------ |
| 112 | **Mute**: l'azione apre l'input box con durata e motivo; `30m` o `2h` silenzia per quel tempo, vuoto è permanente, `abc` risponde "Dati non validi."; il bersaglio riceve "Sei stato silenziato (…). Motivo: …". | |
| 113 | **Chat bloccata**: da silenziato, un messaggio in chat non arriva a nessuno e il giocatore vede "Sei silenziato (…). Motivo: …"; anche `/msg`, `/tell`, `/w`, `/me`, `/teammsg`, `/tm` e `/say` (con o senza `minecraft:`) vengono bloccati; gli altri comandi funzionano. | |
| 114 | **Scadenza**: un mute a tempo finisce da solo alla scadenza (il giocatore torna a scrivere) e l'intestazione della scheda torna a "—". | |
| 115 | **Persistenza**: il mute resta dopo un'uscita e un rientro del giocatore e dopo un riavvio del server. | |
| 116 | **Unmute**: toglie il mute e avvisa il giocatore ("Il tuo silenziamento è stato rimosso."); su un giocatore non silenziato risponde "… non è silenziato.". Un nuovo Mute sostituisce il precedente. | |
| 117 | **Warn**: il motivo è obbligatorio (Conferma spento a campo vuoto); il giocatore riceve "Avvertimento dallo staff (n): …" con il numero progressivo; *Warnings* nella scheda elenca gli avvertimenti dal più recente (data e staff sopra, motivo sotto). | |
| 118 | **Note**: il testo è obbligatorio (al massimo 200 caratteri); la nota non compare al giocatore; *Notes* nella scheda le elenca. Un elenco vuoto mostra "Nessuna nota". | |
| 119 | **Intestazione della scheda**: sotto Organization compaiono *Silenziato fino a* (— / data `gg/MM HH:mm` / permanente), *Avvertimenti* e *Note* con i numeri giusti, che si aggiornano da soli entro pochi secondi. | |
| 120 | **Sezione Moderation**: "Mute attivi" elenca i mute in corso (anche di giocatori usciti: nome, scadenza o permanente, motivo, staff) e "Ultimi avvertimenti" gli ultimi 60 del server (data e giocatore, poi staff e motivo). Serve il nodo `staff.moderation`; senza, la sezione resta attenuata. | |
| 121 | **Registro azioni**: Mute, Unmute, Warn e Note compaiono in *Ultime azioni* come `panel.mute`, `panel.unmute`, `panel.warn`, `panel.note` con durata, motivo o testo nel dettaglio; Warnings e Notes non lasciano righe. | |
| 122 | **Permessi**: con `/qrm admin staff grant <p> staff.player.warn` la scheda mostra solo "Warn" (e le azioni di sola lettura consentite); senza `staff.player.mute` un client modificato che invia Mute riceve "Non hai il permesso.", non succede nulla e il tentativo è registrato con `✗`. | |

## Fonte del `pack.mcmeta` del pack di esempio

Il pack di esempio è in `docs/samples/qrm-sample-pack/`. Il `pack.mcmeta` è stato scritto per Minecraft 26.2 leggendo:

- `version.json` dentro `qrm-neoforge/build/moddev/artifacts/minecraft-patched-26.2.0.88.jar`:
  `pack_version.resource_major` = `88`, `resource_minor` = `0` (dati: `data_major` 107).
- `net/minecraft/server/packs/metadata/pack/PackMetadataSection.java` e `PackFormat.java` (nel jar `-sources` dello stesso
  artefatto): per i resource pack il codec richiede `description` più `min_format` / `max_format` (anche interi singoli: `88`
  vale `88.0` per il minimo e `88.*` per il massimo). Il formato 88 è oltre l'ultimo formato "pre-minor" dei resource pack
  (64), quindi `supported_formats` non va indicato (sarebbe un errore: chiave deprecata) e `pack_format` non è necessario.

Risultato: `{"pack": {"description": "...", "min_format": 88, "max_format": 88}}`.
Se si aggiorna Minecraft, rileggere `pack_version.resource_major` e adeguare i due campi.
