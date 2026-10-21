# Pistola d'ordinanza di QRM Police — documento di progetto

Stato: **specifica (fase C) pronta per l'approvazione, non ancora approvata.**
Sono approvate solo le voci della tabella "Decisioni prese". Tutto ciò che è segnato *proposta* aspetta una conferma: approvando la specifica si accettano anche le proposte, salvo quelle indicate come da cambiare.
Ultimo aggiornamento: 2026-10-20.

## Decisioni prese

| # | Decisione |
|---|---|
| D0 | Nessun controllo di servizio, ruolo o permesso per impugnare o sparare (regola già in vigore in QRM). |
| D1 | La pistola di riferimento è `design/police-pistol/police_pistol.bbmodel`. La revisione grafica non committata non si considera. |
| D2 | I caricatori sono oggetti nell'inventario, ognuno con il proprio conteggio di colpi. |
| D3 | Ricarica in due istanti decisi dal server: a `mag_out` il caricatore vecchio torna nell'inventario (a terra se è pieno), a `mag_in` entra il nuovo. Interrompendo a metà, la pistola resta senza caricatore, con l'eventuale colpo in canna. Il server tiene il proprio tempo dell'azione; il client anima soltanto. |
| D4 | Ricarica a vuoto: il colpo entra in canna in un terzo istante, al rilascio del carrello. Se la ricarica è interrotta dopo `mag_in`, restano caricatore inserito, carrello aperto e camera vuota; serve un'azione breve di rilascio del carrello. |
| D5 | Il carrello ha un tasto dedicato e configurabile. Quando serve, compare un suggerimento scritto in modo immersivo vicino alla pistola, non in un angolo dell'HUD (esempio dato: "[E] Fix"). |
| D6 | Il suggerimento vicino alla pistola è il linguaggio generale dell'arma. Compare in ogni stato che blocca lo sparo e ha una soluzione; quando non ci sono caricatori di scorta, compare come avviso senza tasto. Nessun contatore fisso. |
| D7 | Il controllo munizioni mostra una fascia approssimata per il caricatore inserito e dice con esattezza se c'è un colpo in canna. |
| D8 | Nell'inventario la fascia del caricatore compare nel tooltip solo se il caricatore è stato ispezionato a mano; nessuna barra sull'icona. Il controllo munizioni sulla pistola conta come ispezione del caricatore inserito. |
| D9 | Dopo l'ispezione il tooltip resta aggiornato: la fascia segue il conteggio reale, come se il personaggio ricordasse i colpi sparati. |
| D10 | Il ricordo dell'ispezione è personale: il caricatore registra il personaggio che lo ha ispezionato; per chiunque altro resta sconosciuto finché non lo ispeziona. |
| D11 | La mira (ADS) è nella prima versione: si tiene premuto un tasto, la pistola passa alla posa di mira in ~0,2 s, il campo visivo si stringe leggermente, la precisione è migliore del tiro dal fianco. La precisione la calcola il server. |
| D12 | Letalità bassa: circa 5 colpi al corpo per atterrare un giocatore senza armatura (≈4 danni a colpo). Il danno porta il tag `IS_PROJECTILE`, così Medical lo registra come ferita `BULLET`. |
| D13 | Il `.bbmodel` è la fonte degli asset: un esportatore genera modello, parti, rig e animazioni. I generatori Python smettono di essere la fonte. |
| D14 | Audio da librerie CC0 o royalty-free, rielaborato. Le fonti e le licenze di ogni suono sono elencate in un file nel repository. |
| D16 | Il tasto dedicato del carrello diventa il **tasto di interazione dell'arma** per le azioni "extra". Sul carrello: rilascia se è aperto, lo arretra (clip nuova ~0,5 s) se è chiuso con camera vuota e caricatore con colpi. Risolve Q8. Le altre azioni assegnate al tasto sono in Q11. |
| D17 | Tasto di interazione, in ordine di precedenza: carrello da sistemare → rilascio/arretramento; pressione breve → controllo munizioni; tenuto premuto → ispezione completa; con un caricatore in mano → ispezione del caricatore. Risolve Q11. |
| D18 | Chi è a terra (Medical) non spara e non ricarica; le azioni in corso si interrompono secondo i tre istanti (D3, D4); la pistola resta nell'inventario. Lo stato "a terra" arriva alla pistola tramite `qrm-api`, senza dipendenza diretta di Police da Medical. Risolve Q10. |
| D15 | Rifornimento: si prendono caricatori pieni da un punto (armeria o armadietto), dove si riempiono anche i caricatori vuoti o parziali. Nessuna munizione sciolta. |

## Idee scartate

- Controlli `onDuty` o nodi permesso sull'arma (D0).
- Contatore di munizioni fisso o barra sui caricatori (D6, D8).
- Numero esatto di colpi nel controllo munizioni (D7).
- Scambio dei caricatori in un solo istante (sostituito da D3).
- Colpo in canna insieme all'inserimento del caricatore (sostituito da D4).
- Rilascio del carrello con il tasto di ricarica (sostituito da D5).
- Generatori Python come fonte degli asset, o lettura del `.bbmodel` direttamente dal gioco (D13).

## 1. Esperienza del giocatore

Un agente estrae la pistola con un movimento breve e udibile. Spara dal fianco o mirando, e ogni colpo è un unico evento: suono, vampa, contraccolpo, carrello e bossolo arrivano insieme. Il server decide se il colpo va a segno.

Non c'è un contatore. Per sapere quanti colpi restano si guarda: un controllo rapido dice "più o meno quanto", e la camera dice "sì o no" con certezza. Dopo l'ultimo colpo il carrello resta aperto: si vede e si sente. Quando la pistola non può sparare, accanto all'arma compare una scritta che dice cosa fare.

I caricatori sono oggetti veri: si tolgono, si ripongono, si passano ai colleghi. Un caricatore ricevuto da qualcun altro è sconosciuto finché non lo controlli.

## 2. Funzionalità della prima versione

| ID | Funzionalità | Stato |
|---|---|---|
| E1 | Oggetto pistola | proposta |
| E2 | Stato dell'arma salvato nell'oggetto e deciso dal server | deriva da D3, D4 |
| E3 | Sparo autoritativo: raggio dal server, cadenza controllata dal server, fonte di danno propria | deriva da D12 |
| E4 | Oggetto caricatore con conteggio e ricordo dell'ispezione | D2, D8–D10 |
| E5 | Ricarica tattica e a vuoto, con i tre istanti | D3, D4 |
| E6 | Clic a vuoto | proposta |
| E7 | Riproduttore delle animazioni e rendering per parte in prima persona | proposta (necessaria a quasi tutto) |
| E8 | Feedback di sparo coordinato (suono, vampa, contraccolpo, carrello, bossolo espulso) | proposta |
| E9 | Estrazione e riposizionamento | proposta |
| E10 | Visione dagli altri giocatori: posa delle braccia, suono spaziale, vampa | proposta |
| E11 | Audio | D14 |
| E12 | Tasto di interazione dell'arma: rilascio e arretramento del carrello | D4, D5, D16 |
| E13 | Mira | D11 |
| H1 | Controllo munizioni breve sulla pistola (~1 s) | D7 |
| H5 | Suggerimenti vicino alla pistola | D5, D6 |
| H7 | Ispezione del caricatore tenuto in mano | D8 |
| H4 | Impatti per materiale: particelle dei blocchi vanilla + suoni per 3–4 gruppi di materiali | proposta |
| H6 | Rinculo con ritorno della mira | proposta (il contraccolpo è già nella traccia) |
| E14 | Punto di rifornimento dei caricatori | D15 (forma in Q9a) |

Senza decisione, quindi fuori dalla prima versione: O1–O6 (ricarica d'emergenza, colpi visibili nel caricatore, bossoli a terra, gestione dei muri, coda a distanza, scelta manuale del caricatore). Possono essere ripresi.

## 3. Stati dell'arma

Il server conserva tre informazioni nella pistola:

- **caricatore**: assente, oppure inserito con N colpi (0…capienza) e il ricordo dell'ispezione;
- **camera**: vuota o con un colpo;
- **carrello**: chiuso o aperto (bloccato indietro).

Stati che il giocatore deve riconoscere:

| Stato | Come si riconosce | Suggerimento (D6) |
|---|---|---|
| Pronta (colpo in canna) | Nessun segnale: spara | — |
| Ultimo colpo sparato, caricatore vuoto | Carrello aperto, suono di blocco | Ricarica |
| Senza caricatore | Il caricatore non si vede nel modello | Ricarica (o avviso se non hai scorte) |
| Caricatore inserito, carrello aperto | Carrello aperto con il caricatore dentro (ricarica interrotta, D4) | Tasto del carrello |
| Caricatore inserito, camera vuota, carrello chiuso | Clic a vuoto | Tasto di interazione (arretra il carrello, D16) |
| Nessun caricatore con colpi nell'inventario | Clic a vuoto | Avviso senza tasto |

*Proposta*: capienza 17 colpi, come la pistola reale a cui il modello si ispira. Configurabile.

## 4. Azioni, animazioni e interruzioni

| Azione | Clip | Durata | Stato della clip | Si interrompe con |
|---|---|---|---|---|
| Estrazione | `draw` | 0,8 s | esiste | cambio di slot |
| Riposizionamento | `holster` | 0,55 s | esiste | — |
| A riposo | `idle` | loop | esiste | — |
| Corsa (arma abbassata) | `sprint_in` / `sprint` / `sprint_out` | 0,3 / loop / 0,3 s | esiste | — |
| Mira | posa di mira + transizione | ~0,2 s | **nuova** | — |
| Sparo | `fire`, `fire_last` | 0,62 s | esiste, da adattare alla mira | un nuovo sparo può riavviarla (*proposta*) |
| Clic a vuoto | `dry_fire` | 0,3 s | esiste | — |
| Ricarica tattica | `reload_tactical` | 2,49 s | esiste | cambio di slot, perdita della pistola, atterramento |
| Ricarica a vuoto | `reload_empty` | 2,73 s | esiste | come sopra |
| Rilascio del carrello | nuova | ~0,4 s | **nuova** | come sopra |
| Arretramento del carrello (D16) | nuova | ~0,5 s | **nuova** | come sopra |
| Controllo munizioni | nuova | ~1 s | **nuova** | qualsiasi azione |
| Ispezione del caricatore in mano | nuova | ~1 s | **nuova** | qualsiasi azione |
| Ispezione completa | `inspect` | 4,2 s | esiste | qualsiasi azione (*proposta*: tenerla come gesto estetico) |

*Proposte*:
- La cadenza la stabilisce il server, con un intervallo minimo tra i colpi (~0,2–0,25 s). Un clic, un colpo. La durata della clip di sparo non limita la cadenza.
- Subire danni **non** interrompe la ricarica; un'interruzione per danno sarebbe frustrante e poco leggibile.
- Durante la ricarica non si spara. Lo sparo durante il controllo o l'ispezione interrompe il controllo e spara.

## 5. Audio

Suoni richiesti dalla traccia degli eventi: `draw`, `draw_settle`, `holster`, `fire`, `slide_forward`, `slide_lock`, `dry_fire`, `mag_release`, `mag_out`, `mag_in`, `mag_peek`, `press_check`.
Da aggiungere: rilascio del carrello, eventuale arretramento, ispezione del caricatore in mano, impatti per gruppo di materiali.

Requisiti:
- I suoni sono spaziali e partono dal server, così li sentono tutti i giocatori vicini.
- I suoni frequenti (sparo, impatti) hanno 2–3 varianti.
- I suoni di maneggio sono più bassi e si sentono solo da vicino.
- Ogni file ha la fonte e la licenza registrate nel repository (D14).

## 6. Filosofia dell'HUD

- Nessun elemento fisso dell'arma sullo schermo.
- Una sola voce visiva: il suggerimento vicino alla pistola (D5, D6), che compare solo quando serve un'azione o quando chiedi un'informazione (controllo munizioni).
- Le informazioni sono approssimate dove lo sarebbero nella realtà (fascia del caricatore) ed esatte dove si vedono (camera, carrello aperto).
- L'inventario dice solo ciò che il personaggio sa (D8–D10).

## 7. Multigiocatore e interruzioni

- Lo stato dell'arma e dei caricatori lo decide **solo il server**. Il client chiede ("sparo", "ricarico", "carrello") e anima.
- *Proposta*: per non introdurre ritardo, il client avvia subito l'animazione di sparo se, secondo la sua copia dello stato, la pistola è carica; se il server rifiuta, il client si riallinea.
- I trasferimenti di munizioni avvengono in un solo tick sul server, in ciascuno dei tre istanti. A ogni istante il server verifica di nuovo che:
  - la pistola in mano sia la stessa che ha iniziato l'azione;
  - il caricatore da inserire sia ancora nell'inventario. Si sceglie al momento di `mag_in`, non all'inizio.

  Se un controllo fallisce, l'azione si annulla senza spostare colpi.
- Cambio di slot, lancio della pistola o pistola rimossa: l'azione si interrompe, e lo stato resta quello dell'ultimo istante concluso.
- Disconnessione: l'azione in corso si perde, lo stato salvato è quello dell'ultimo istante concluso.
- Morte vera (`/kill`, vuoto): gli oggetti seguono le regole vanilla, con il loro stato; nessuna duplicazione possibile perché lo stato vive negli oggetti.
- Atterramento (Medical): l'azione in corso si interrompe, sparo e ricarica sono rifiutati dal server finché il giocatore è a terra; la pistola resta nell'inventario (D18).
- Gli altri giocatori ricevono dal server un evento di sparo e mostrano suono, vampa e posa.
- Effetti temporanei (vampa, bossolo espulso, particelle d'impatto) si puliscono da soli e non vengono salvati.
- *Esclusa dalla prima versione*: la compensazione della latenza ("riavvolgere" i bersagli). Il raggio parte dalla posizione del tiratore sul server.

## 8. Asset

- **Esportatore** dal `.bbmodel` verso modello statico, parti, rig e animazioni (D13).
- **Pulizia una tantum** delle animazioni del `.bbmodel`: oggi sono campionate a ~60 fps, vanno riportate a pochi keyframe con le curve di Blockbench.
- **Clip nuove**: posa di mira e transizioni, rilascio del carrello, eventuale arretramento, controllo munizioni, ispezione del caricatore in mano.
- **Braccia in prima persona**: la traccia richiama pose della mano (`support_hand_to_magazine`, …) che oggi non sono definite. *Proposta*: aggiungere le braccia come bone nel `.bbmodel`, così chi anima le vede, e farle rendere con la skin del giocatore o della divisa (compatibile con Wardrobe).
- **Modello e icona del caricatore** come oggetto, ricavati dalla parte `magazine`.
- **Texture**: 64×64, con un passaggio di qualità sulla separazione dei materiali (polimero, metallo, mire, comandi). È un lavoro grafico, non tecnico.
- **Audio** (sezione 5).

## 9. Criteri di accettazione

1. Nessuna sequenza di azioni, interruzioni, cambi di slot, disconnessioni o latenza crea o distrugge colpi. Si verifica con test automatici sulla logica dei trasferimenti.
2. Interrompere la ricarica tra `mag_out` e `mag_in` lascia la pistola senza caricatore; il caricatore tolto è nell'inventario o a terra.
3. Interrompere la ricarica a vuoto dopo `mag_in` lascia il carrello aperto; il tasto del carrello lo chiude e mette il colpo in canna.
4. Con il carrello aperto, senza caricatore o senza scorte, compare il suggerimento giusto vicino alla pistola, mai un contatore.
5. Il controllo munizioni mostra la fascia corretta e la camera esatta; il caricatore controllato mostra poi la fascia nel tooltip, aggiornata a ogni colpo, solo al personaggio che l'ha controllato.
6. Un giocatore senza armatura cade a terra in circa 5 colpi al corpo, e Medical registra ferite da arma da fuoco. Un giocatore a terra non riesce a sparare né a ricaricare.
7. Due client vicini vedono e sentono lo stesso sparo nello stesso punto; il colpo va a segno solo secondo il server.
8. Mirando la precisione è chiaramente migliore che dal fianco.
9. Ogni file audio ha fonte e licenza registrate.
10. Modello, parti, rig e animazioni in gioco derivano dal `.bbmodel` tramite l'esportatore, senza ritocchi a mano dei file generati.

## 10. Escluse esplicitamente dalla prima versione

- Controlli di servizio, ruolo o permesso (D0).
- Compensazione della latenza.
- Usura, manutenzione, inceppamenti.
- Accessori, varianti, caricatori di capienza diversa, altri tipi di arma.
- Matricola collegata al registro armi dell'MDT; bossoli come prove.
- O1–O6 (sezione 2), salvo nuova decisione.

## 11. Domande aperte

Le domande principali (Q1–Q11) sono tutte risolte da D2–D18.

Dettagli con proposta, da confermare insieme alla specifica:

- **Q11a** Soglia tra pressione breve e tenuta del tasto di interazione: ~0,3 s.

- **Q2d** Tasto predefinito del carrello (`E` è l'inventario vanilla) e testo italiano del suggerimento.
- **Q3a** Ricaricare o ispezionare fa uscire dalla mira? Quale tasto mira e quale spara?
- **Q4c** Quando compare il suggerimento e quanto resta visibile.
- **Q4e** I caricatori pieni si impilano?
- **Q4h** I caricatori nuovi e pieni sono già "noti"?
- **Q5a** Moltiplicatore per la testa, armatura, danno alle creature, gittata.
- **Q6a** Namespace degli asset: `qrm` o `qrm_police`.
- **Q9a** Il punto armeria: nuovo tipo di punto di interazione (`PointType` ha già proprietario e `requiresDuty`, che regolano l'accesso al luogo, non l'uso dell'arma) o funzione di qrm-locker? Caricatori illimitati o con una scorta? *Proposta*: nuovo tipo di punto, illimitato nella prima versione; i caricatori riempiti lì sono "noti" come pieni a chi li ritira.

## 12. Priorità proposte

Ordine di rischio, non piano di implementazione:

1. Esportatore, riproduttore delle animazioni e rendering in prima persona: è il rischio tecnico principale.
2. Stato dell'arma, sparo, caricatori e ricarica sul server, con i test sulle munizioni.
3. Suggerimenti, controllo munizioni, ispezione del caricatore.
4. Audio.
5. Impatti, rinculo, rifiniture.

## Fatti tecnici verificati (fase A)

- Minecraft 26.2, NeoForge 26.2.0.88, Java 25, nessuna libreria di animazione esterna.
- Asset esistenti in `qrm-neoforge` (namespace `qrm`): modello statico, 14 modelli di parte, rig, 11 animazioni `qrm.anim/1` con traccia di eventi, texture 64×64. Nessun codice Java li usa.
- Il `.bbmodel` ha la stessa gerarchia del rig (14 bone) e 12 animazioni con le stesse durate.
- Nessun file audio nel repository.
- NeoForge 26.2 offre: `RenderHandEvent`, `RegisterSpecialModelRendererEvent`, proprietà di modello condizionali/select/range, `IClientItemExtensions.getArmPose` e `applyForgeHandTransform`, `ComputeFovModifierEvent`, `ViewportEvent`, `RenderArmEvent` (già usato da Wardrobe).
- Rete: pattern `CustomPacketPayload` + `PayloadRegistrar` già usato in Police.
- Medical: un colpo fatale fa cadere a terra; un danno `IS_PROJECTILE` è registrato come `BULLET`; colpire chi è a terra provoca un colpo critico; chi è a terra non può usare oggetti né attaccare (eventi vanilla).
- Il registro armi dell'MDT (`WeaponEntry`: matricola, tipo, proprietario, stato) esiste già.
