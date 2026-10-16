# Test della banca in due — v0.7.3

Pensato per due persone sullo stesso server (anche a distanza, via Tailscale). Dura circa 60–90 minuti.
Servono i jar **0.7.3** (`qrm-0.7.3.jar` e `qrm-bank-0.7.3.jar`) sia sul server sia da lei: con versioni diverse il canale di rete non è compatibile.
Durante il test scrivete a fianco di ogni punto cosa è successo davvero; i comportamenti inattesi sono la parte più utile.

- **Giocatore A** (operatore): `MrMada_`. Prepara il server, ha i comandi `/bank` e `/qrm admin`.
- **Giocatore B** (giocatore normale, **non operatore**): fa da cliente. Il suo punto di vista serve a trovare ciò che l'operatore non vede.

Personaggi usati negli esempi (potete cambiarli): A = **Luca Bianchi**, B = **Giulia Verdi**. Valuta: **QRM** (2 decimali). Tagli: 1, 5, 10, 50, 100. Limiti predefiniti: 1000.00 per operazione, 5000.00 al giorno, distanza massima dall'ATM 6 blocchi.

Legenda: ☐ da fare · ✔ ok · ✘ problema (annotate cosa).

---

## 0. Connessione e preparazione

| # | Chi | Cosa | Atteso | Esito |
|---|---|---|---|---|
| 0.1 | A | Avvia il server (`./gradlew :qrm-bank:runServer --console=plain`) e controlla il log | `Done`, `QRM started (1 currencies, 0 jobs)`, nessuna eccezione | ☐ |
| 0.2 | B | Si collega a `100.x.x.x:25565` | Entra nel mondo. Se il launcher dice che mancano o differiscono i mod: annotare il messaggio esatto | ☐ |
| 0.3 | A, B | Aprono la lista dei mod | Compaiono `QRM` e `QRM Bank` 0.7.0 per entrambi | ☐ |
| 0.4 | B | Scrive `/bank` | **Comando sconosciuto** o non suggerito: B non è operatore | ☐ |
| 0.5 | B | Scrive `/qrm admin` | Stessa cosa: non disponibile | ☐ |
| 0.6 | A | Controlla che B sia in lista e non abbia permessi (`/list`) | B online | ☐ |

## 1. Personaggi e fondi

| # | Chi | Comando / azione | Atteso | Esito |
|---|---|---|---|---|
| 1.1 | A | `/qrm character create "Luca" "Bianchi" 1995-04-12` | Personaggio creato e selezionato | ☐ |
| 1.2 | B | `/qrm character create "Giulia" "Verdi" 1996-08-30` | Idem | ☐ |
| 1.3 | A, B | `/qrm balance` | Saldo 0 per entrambi (o messaggio equivalente) | ☐ |
| 1.4 | A | `/money give MrMada_ 500 QRM`, poi `/money give <nome di B> 200 QRM` | Accrediti ok. Controllare con `/qrm balance`: A 500.00, B 200.00 | ☐ |
| 1.5 | A | `/bank atm give` e `/bank atm give <nome di B>` | Entrambi ricevono un blocco **ATM** | ☐ |
| 1.6 | A, B | Piazzano un ATM ciascuno, vicini (anche lo stesso ATM va bene più avanti) | Blocco piazzato, textures corrette, nessun cubo viola/nero | ☐ |

## 2. Aprire l'ATM

| # | Chi | Azione | Atteso | Esito |
|---|---|---|---|---|
| 2.1 | A | Click destro sul proprio ATM | Si apre la schermata con le schede **Conto / Contanti / Bonifico** | ☐ |
| 2.2 | A | Guarda il saldo e lo storico | Saldo **500.00 QRM**; storico con l'accredito iniziale | ☐ |
| 2.3 | B | Click destro sul proprio ATM | Si apre; saldo **200.00 QRM** | ☐ |
| 2.4 | A, B | Premono `Esc` | La schermata si chiude, il gioco non si blocca | ☐ |
| 2.5 | A, B | Controllano testi e colori | Tutti in italiano, nessuna chiave grezza tipo `qrm_bank.xxx`, testo non tagliato | ☐ |
| 2.6 | A | Cambia la lingua del gioco in inglese e riapre l'ATM | Testi in inglese, nessuna chiave grezza | ☐ |
| 2.7 | A | Scheda **Contanti**: scrive `10` nel campo importo, poi passa alla scheda **Bonifico** | Il campo importo del Bonifico è **vuoto** (e viceversa): gli importi delle due schede sono separati | ☐ |

## 3. Ritiro e deposito (fatelo entrambi, ognuno al proprio ATM)

| # | Chi | Azione | Atteso | Esito |
|---|---|---|---|---|
| 3.1 | A | Ritira **17.00** | In inventario: 1×10, 1×5, 2×1 (nomi tipo "Banconota 10.00 QRM"). Saldo **483.00**. Lo storico mostra il ritiro | ☐ |
| 3.2 | A | Ritira **1.50** | **Rifiutato** con messaggio chiaro; saldo invariato | ☐ |
| 3.3 | A | Ritira **0** e un importo vuoto/negativo | Rifiutato, nessun crash | ☐ |
| 3.4 | A | Ritira **99999** (più del saldo) | Rifiutato per fondi insufficienti | ☐ |
| 3.5 | A | Riempi l'inventario di terra e prova a ritirare 5.00 | Rifiutato **prima** di addebitare (saldo invariato) | ☐ |
| 3.6 | A | Deposita tutto il contante | Banconote **scomparse**, saldo risale a **500.00** | ☐ |
| 3.7 | B | Ritira **100.00**, poi controlla l'inventario | 2×50 oppure 1×100? Annotate cosa arriva (il sistema usa i tagli più grandi) | ☐ |
| 3.8 | B | Deposita solo una parte: tiene in mano 1 banconota e deposita | Annotate se deposita **tutte** le banconote o solo quelle in mano (la documentazione dice tutte quelle della valuta) | ☐ |

## 4. Bonifici tra voi (il cuore del test a due)

Assumiamo A = 500.00 e B = 200.00 prima di iniziare. Annotate i saldi dopo ogni passo.

| # | Chi | Azione | Atteso | Esito |
|---|---|---|---|---|
| 4.1 | A | Bonifico di **50.00** a `Giulia Verdi` (senza causale) | Ok. A **450.00**, B **250.00**. **B vede subito in chat** "Hai ricevuto 50.00 Q da Luca Bianchi." (senza aprire l'ATM) | ☐ |
| 4.2 | B | Riapre l'ATM **senza** riavviare | Saldo **250.00** aggiornato; lo storico mostra **"Bonifico da Luca Bianchi"** con +50.00, senza riga di causale. Lo storico di A mostra "Bonifico a Giulia Verdi" con −50.00 | ☐ |
| 4.3 | B | Con un **altro personaggio attivo** (`/qrm character select`), A le invia 1.00 | B **non** riceve l'avviso; ritroverà il movimento nello storico del conto di Giulia | ☐ |
| 4.4 | B | Bonifico di **25.00** a `luca bianchi` (tutto minuscolo) | Ok: le maiuscole non contano. B 225.00, A 475.00 | ☐ |
| 4.5 | A | Bonifico a `Mario Inesistente` | Errore chiaro "personaggio non trovato", nessun addebito | ☐ |
| 4.6 | A | Bonifico a se stesso (`Luca Bianchi`) | Errore chiaro, nessun addebito | ☐ |
| 4.7 | A | Bonifico di importo maggiore del saldo | Rifiutato per fondi insufficienti | ☐ |
| 4.8 | A | Bonifico di **0** o negativo | Rifiutato, nessun crash | ☐ |
| 4.9 | A | Bonifico con importo con 3 decimali (`1.234`) | Rifiutato (la valuta ha 2 decimali) | ☐ |
| 4.10 | A, B | `/qrm balance` | I due saldi coincidono con quelli mostrati dall'ATM | ☐ |
| 4.11 | A | Bonifico di 10.00 a `Giulia Verdi` con causale **Affitto ottobre** | Ok. B vede in chat "Hai ricevuto 10.00 Q da Luca Bianchi: «Affitto ottobre»" | ☐ |
| 4.12 | A, B | Aprono lo storico | Sotto il movimento compare la riga con la causale, in grigio, **da entrambe le parti** (con "a" per A e "da" per B) | ☐ |
| 4.13 | A | Prova a scrivere una causale di più di 64 caratteri | Il campo non accetta oltre 64 caratteri | ☐ |
| 4.14 | A | Causale con caratteri speciali: `Cena "da Mario" è ok € \ /` | Il bonifico riesce e la causale si legge uguale nello storico e nell'avviso; nessun errore nel log del server | ☐ |
| 4.15 | A | Bonifico senza causale (campo vuoto) | Nessuna riga extra nello storico, avviso senza due punti finali | ☐ |
| 4.16 | A | Dopo un bonifico riuscito riguarda i campi Importo e Causale | Entrambi si sono svuotati; il destinatario resta | ☐ |

## 5. Contemporaneità (qui si trovano i bug veri)

| # | Chi | Azione | Atteso | Esito |
|---|---|---|---|---|
| 5.1 | A, B | **Stesso ATM**, aperto da entrambi nello stesso momento | Entrambi vedono la propria schermata col proprio saldo, non quello dell'altro | ☐ |
| 5.2 | A, B | Entrambi ritirano **10.00** nello stesso istante (conto alla rovescia 3-2-1) | Due ritiri distinti, nessuna banconota in più o in meno | ☐ |
| 5.3 | A, B | Si inviano **5.00** a vicenda nello stesso istante | Entrambi riusciti, saldi corretti, nessun blocco del server | ☐ |
| 5.4 | A | Clicca "Ritira" **molto velocemente** 5–6 volte | Una sola operazione (rate limit) o poche, mai importi doppi non voluti | ☐ |
| 5.5 | A | Subito dopo i test, `/bank audit` | Nessuna anomalia | ☐ |

## 6. Contante fisico (cosa succede tra due persone)

| # | Chi | Azione | Atteso | Esito |
|---|---|---|---|---|
| 6.1 | A | Ritira **50.00** e le butta a terra (`Q`) | Le banconote sono oggetti raccoglibili | ☐ |
| 6.2 | B | Raccoglie le banconote e le **deposita** | Il deposito riesce: B guadagna **50.00**, A non le ha più. Questo è il comportamento voluto (contante al portatore) | ☐ |
| 6.3 | A | Ritira 20.00, le mette in un baule e chiude | Il saldo non cambia mentre sono nel baule; un deposito non le conta | ☐ |
| 6.4 | A | Ritira 10.00 e le **dà** a B con un baule/scambio | B deposita e il suo saldo sale di 10.00 | ☐ |
| 6.5 | A | Con `/give MrMada_ qrm_bank:cash_qrm_10000 64` (64 banconote da 100.00 = 6400.00 mai emesse dalla banca), poi prova a depositarle | Il contante in circolazione supera la cassa: **rifiutato** ("non emesso dalla banca"), item intatti | ☐ |
| 6.6 | A | `/bank audit` dopo il punto 6.5 | **Segnala l'eccesso** di contante rispetto alla cassa | ☐ |
| 6.7 | A | Elimina le banconote fasulle (buttale nella lava) e rilancia `/bank audit` | Torna senza anomalie | ☐ |

## 7. Limiti

| # | Chi | Azione | Atteso | Esito |
|---|---|---|---|---|
| 7.1 | A | `/money give MrMada_ 5000 QRM` (così ha fondi) | Saldo alto | ☐ |
| 7.2 | A | Ritira **1000.01** | Rifiutato: oltre il limite per operazione | ☐ |
| 7.3 | A | Ritira **1000.00** (l'inventario si riempirà: servono spazio o più giri) | Ok, oppure rifiuto per inventario pieno **senza addebito** | ☐ |
| 7.4 | A | Deposita, poi ritira altre volte fino a superare 5000.00 in una giornata | Il ritiro che supera il limite giornaliero è rifiutato con un messaggio chiaro | ☐ |
| 7.5 | A | Modifica `maxPerOperation` in `qrm_bank-server.toml` e salva (senza riavviare) | Il nuovo limite vale subito (niente riavvio) | ☐ |

## 8. Distanza e uscita dal gioco

| # | Chi | Azione | Atteso | Esito |
|---|---|---|---|---|
| 8.1 | B | Apre l'ATM, poi cammina o vola a più di 8 blocchi **senza premere nulla** | La schermata si **chiude da sola** entro mezzo secondo; nessun addebito | ☐ |
| 8.2 | B | Apre l'ATM e A **distrugge il blocco** con un piccone | La schermata di B si **chiude da sola** entro mezzo secondo (prima restava aperta e inutilizzabile) | ☐ |
| 8.3 | B | Apre l'ATM e si disconnette (chiude il gioco) | Il server non ha errori nel log | ☐ |
| 8.4 | B | Si riconnette | Stesso personaggio e stesso saldo | ☐ |
| 8.5 | B | Durante un ritiro grosso, chiude il gioco con forza (Alt+F4 / task manager) | Nessuna banconota duplicata al rientro; `/bank audit` coerente | ☐ |

## 9. Permessi e congelamento (A comanda, B subisce)

| # | Chi | Azione | Atteso | Esito |
|---|---|---|---|---|
| 9.1 | A | `/bank freeze Giulia Verdi` | Confermato | ☐ |
| 9.2 | B | Prova a ritirare e a fare un bonifico | Entrambi rifiutati ("conto congelato") | ☐ |
| 9.3 | A | Fa un bonifico **a** B (conto congelato) | Annotate se riesce o no | ☐ |
| 9.4 | A | `/bank unfreeze Giulia Verdi` | B può di nuovo operare | ☐ |
| 9.5 | A | `/perm deny <nome di B> bank.atm.use` | B non riesce più ad aprire l'ATM (messaggio chiaro) | ☐ |
| 9.6 | A | `/perm clear <nome di B> bank.atm.use`, poi `deny ... bank.transfer` | L'ATM si apre, ma il bonifico è rifiutato | ☐ |
| 9.7 | A | `/perm clear <nome di B> bank.transfer` | Tutto torna normale | ☐ |

## 10. Più personaggi e organizzazione

| # | Chi | Azione | Atteso | Esito |
|---|---|---|---|---|
| 10.1 | B | Crea un secondo personaggio ("Anna Rossi") e lo seleziona con `/qrm character select 2` | Il conto bancario è **un altro**: saldo 0 | ☐ |
| 10.2 | B | Torna a Giulia (`/qrm character select 1`) | Ritrova il saldo di prima | ☐ |
| 10.3 | A | Crea un secondo personaggio con **lo stesso nome** di Giulia ("Giulia Verdi") | Creazione permessa o rifiutata? Annotate (dipende da `uniqueNames`) | ☐ |
| 10.4 | A | Se permessa: bonifico a `Giulia Verdi` | Rifiutato come **ambiguo**, nessun addebito | ☐ |
| 10.5 | A | `/org create banca Banca Centrale` | Organizzazione creata | ☐ |
| 10.6 | A | Bonifico di 10.00 a `@banca` con causale "Deposito cassa" | Ok; il saldo di A scende; lo storico mostra **"Bonifico a Banca Centrale"** con la causale; `/org info banca` mostra l'organizzazione. Nessun avviso a nessuno | ☐ |
| 10.7 | B | Bonifico verso `@banca` | Ok | ☐ |
| 10.8 | A | Bonifico verso `@inesistente` | Errore chiaro | ☐ |

## 11. Persistenza (richiede di riavviare il server)

| # | Chi | Azione | Atteso | Esito |
|---|---|---|---|---|
| 11.1 | A, B | Annotano saldo e contante in inventario | Saldi: A = ___ , B = ___ | ☐ |
| 11.2 | A | Ferma il server (`stop`) e lo riavvia | Parte senza errori | ☐ |
| 11.3 | A, B | Rientrano e controllano | Saldi **identici** e banconote ancora in inventario | ☐ |
| 11.4 | A | L'ATM piazzato prima del riavvio c'è ancora e funziona | Ok | ☐ |
| 11.5 | A | `/bank audit` | Nessuna anomalia | ☐ |

## 12. Aspetto

| # | Chi | Azione | Atteso | Esito |
|---|---|---|---|---|
| 12.1 | A, B | Guardano banconote e ATM nell'inventario, in mano e a terra | Texture corretta, nome corretto, nessun "missing texture" | ☐ |
| 12.2 | A, B | Cercano l'ATM nella creativa ("Blocchi funzionali") | Presente | ☐ |
| 12.3 | A, B | Provano la schermata con GUI scale diverse (Opzioni → Video) | Testi leggibili, pulsanti non sovrapposti | ☐ |
| 12.4 | A | Rompe un ATM in survival con la mano e con un piccone | Annotate cosa succede (loot table: dovrebbe lasciare l'ATM) | ☐ |

## 13. Togliere denaro (`/money take`)

| # | Chi | Azione | Atteso | Esito |
|---|---|---|---|---|
| 13.1 | A | `/money take <nome di B> 10 QRM` | Conferma "Operazione completata."; il saldo di B scende di 10.00 | ☐ |
| 13.2 | B | Riapre l'ATM | Saldo aggiornato; nello storico compare **"Prelievo amministrativo"** (−10.00), senza controparte; un `grant` compare come "Accredito amministrativo" e `/qrm pay` come "Pagamento" | ☐ |
| 13.3 | A | `/money take <nome di B> 99999 QRM` | **Fallisce** con "Fondi insufficienti."; il saldo di B non cambia | ☐ |
| 13.4 | A | `/money take <nome di B> 1.234 QRM` e `... -5` | Rifiutati con un messaggio sull'importo | ☐ |
| 13.5 | A | `/bank freeze <nome di B>`, poi `take` | Fallisce con "Conto congelato."; poi `/bank unfreeze` | ☐ |
| 13.6 | B | Prova `/money take ...` | Comando non disponibile (B non è operatore) | ☐ |
| 13.7 | A | `/qrm admin audit` e `/bank audit` | Nessuna anomalia dopo i `take` | ☐ |

---

## Se qualcosa va storto

Per ogni problema annotate: **numero del punto**, cosa avete fatto, cosa vi aspettavate, cosa è successo, e se c'è stato un messaggio d'errore.
Allegate le ultime righe del log del server (`qrm-bank/run/logs/latest.log`) e, se il client è andato in crash, il file in `crash-reports/`.

| Punto | Cosa è successo | Log / screenshot |
|---|---|---|
| | | |
| | | |
| | | |

## Dopo il test

- Se tutto è ✔, ditemelo e chiudiamo la lista di controllo (`docs/manual-tests/bank.md`) come per la v0.6.
- Se ci sono ✘, mandatemi la tabella qui sopra e li sistemo.
