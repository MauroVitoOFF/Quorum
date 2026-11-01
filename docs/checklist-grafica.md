# Checklist grafica — controllo delle GUI di QRM

Per chi cura la grafica e non programma. Si guarda solo come appare e come "si sente" ogni schermata.
Per ogni voce segna **OK** oppure scrivi cosa non va (meglio con uno screenshot: `F2` in gioco, finisce in `run/screenshots`).

## 1. Avviare il gioco

1. Apri la cartella `QRM` e fai **doppio clic su `dev-start.bat`**. Si aprono due schede: server e client.
2. Aspetta che nella scheda "QRM client" si apra Minecraft (la prima volta può volerci qualche minuto).
3. Dal menu: **Multigiocatore → Aggiungi server → indirizzo `localhost`** e entra.
4. Se la scheda client dà errore di file mancanti: chiedi a chi sviluppa. **Non** lanciare mai `gradlew clean`.
5. Per spegnere il server: nella scheda "QRM server" scrivi `stop` e premi Invio.

Per usare i comandi serve essere **operatore**. Nella scheda del server scrivi `op TuoNome` (una volta sola).
I comandi si scrivono in chat premendo `T`.

## 2. Come guardare una schermata (da ripetere per TUTTE)

Per ogni GUI fai questi controlli generali, poi quelli specifici del paragrafo 3.

- [ ] **Scala GUI 2, 3 e 4**: `Esc → Opzioni → Impostazioni video → Scala GUI`. Cambia e riapri la schermata. Nulla deve uscire dai bordi, essere tagliato o sovrapporsi.
- [ ] **Finestra piccola**: trascina il bordo della finestra per rimpicciolirla. La cornice deve restare intera.
- [ ] **Testi leggibili**: nessun testo sbiadito, troppo piccolo o di colore simile allo sfondo. Nessuna ombra strana sul testo.
- [ ] **Nessun testo fuori dal riquadro** (nomi lunghi, importi grandi: prova a scrivere un nome molto lungo).
- [ ] **Pixel nitidi**: nessuna immagine sfocata, stirata o con bordi sfrangiati; i bordi dei pannelli si ripetono senza "cuciture".
- [ ] **Stati dei pulsanti**: passa il mouse sopra (hover), tieni premuto (premuto), guarda quello disattivato. Le quattro versioni devono essere diverse e coerenti.
- [ ] **Campi di testo**: il campo con il cursore (focus) deve distinguersi da quello senza.
- [ ] **Icone**: della stessa famiglia (stesso spessore, stesso stile, stesso margine), centrate nel loro riquadro.
- [ ] **Suggerimenti** (tooltip): passando sulle icone senza testo compare il nome.
- [ ] **Chiusura**: `Esc` chiude; il tasto di chiusura è riconoscibile.
- [ ] **Colori**: coerenti tra le schermate (stessi verdi, ottoni, rossi per "pericolo").
- [ ] **Italiano**: nessun refuso, nessuna parola in inglese rimasta per sbaglio, nessun segno strano (`?`, `□`) al posto di lettere accentate.

## 3. Le schermate, una per una

### Tasti sempre disponibili
| Tasto | Cosa apre |
|---|---|
| `O` | Selezione/creazione personaggio (Character) |
| `K` | Hub (schede) |
| `H` | Mostra/nasconde l'HUD |
| `F7` | Pannello staff (poi `Alt sinistro` libera il cursore) |

### Personaggio (`O`)
- [ ] Giocatore nuovo: la schermata si apre da sola in modalità creazione.
- [ ] Carta d'identità con la tua testa, la firma si scrive mentre digiti, interruttore M/F cambia.
- [ ] Carte dei personaggi: normale, selezionata, bloccata, "nuovo" si distinguono.
- [ ] Fototessera dentro la cornice, senza sbavature.

### Hub (`K`) e HUD (`H`)
- [ ] Linguette (tab) normale e selezionata ben distinguibili.
- [ ] Il pannello dell'HUD non copre elementi di gioco importanti; `H` lo nasconde.

### Pannello staff (`F7`)
- [ ] Le "gemme" delle sezioni (economia, log, moderazione, organizzazioni, giocatori…) sono tutte presenti e della stessa famiglia.
- [ ] Righe: normale, selezionata, pericolo (rossa), "in arrivo" (attenuata).
- [ ] Icone laterali (indietro, chiudi, lavori, organizzazioni, permessi, giocatore, aggiorna).

### ATM (banca)
Comando: `/qrm admin atm give`, piazza il blocco e fai clic destro. Esiste già una lista dettagliata: `docs/manual-tests/atm-gui.md`.
- [ ] Cornice in marmo con targa "ATM"; il cassiere in alto a destra si anima (lancia la moneta).
- [ ] Tre schede: Conto, Contanti, Bonifico. Quella scelta è evidenziata.
- [ ] Registro movimenti: icone per riga, importi verdi/rossi, barra di scorrimento se sono tanti.
- [ ] Bottoni 25% / 50% / 75% / MAX / SEND: numeri nitidi, quattro stati.
- [ ] Targa in basso con esito (spunta verde / croce rossa).
- [ ] Banner dell'azienda/polizia, se presenti, corretti.

### Pannello banca staff e Boss
- [ ] `/boss` (schermata capo/organizzazione) e il selettore di organizzazione: icone (membri, promuovi, licenzia, deposita, scrivania, panoramica) coerenti.

### Armadietto (locker)
Comando: `/qrm admin point create locker job police` guardando una cassa, poi clic destro.
- [ ] Pannello a tema QRM, slot scuri, titolo leggibile.

### Guardaroba divise (wardrobe)
Comando: `/qrm admin point create wardrobe job police`, clic destro sul punto.
- [ ] Lista a sinistra, manichino al centro, pulsante "Indossa". Il manichino ruota/si vede bene.
- [ ] La divisa indossata si vede **sul tuo personaggio** (da fuori con `F5` e il braccio in prima persona): niente texture rovesciate, testa con la tua faccia, nessun pezzo trasparente o nero.
- [ ] Prova sia braccia larghe sia sottili.

### Terminale e tablet di Polizia (MDT)
Comandi: `/qrm mdt seed` per i dati di prova. Tablet dalla scheda strumenti del creativo, oppure il blocco «Terminale di Polizia»; poi clic destro. Lista in `docs/manual-tests/police.md`.
- [ ] Dashboard: 3 card, note rapide.
- [ ] Cittadini: fototessera in lista e nel dettaglio; ricercati evidenziati.
- [ ] Dispatch: `/qrm mdt simulatecall sparatoria` — chiamata in lista, LED in alto che lampeggia rosso/ambra.
- [ ] Il tablet in mano: cornice, spessori e tasto fisico; visto da un altro giocatore (braccio piegato).
- [ ] Codice penale: barra in fondo con i totali leggibile.

### Medico e immersione
- [ ] `/qrm medical down <nome>` e tasto `C` (allerta), `G` (arrendersi): messaggi/indicatori a schermo leggibili.
- [ ] Gesti `X` (mani in alto) e `B` (indicare): solo animazioni, controlla che non "taglino" il modello.

## 4. Come segnalare un problema

Per ogni problema scrivi: **schermata**, **scala GUI**, **cosa vedi**, **cosa ti aspettavi**, e allega lo screenshot (`F2`).
Esempio: «ATM, scala 3: il numero "75%" è tagliato a destra nel tasto premuto».

## 5. Dove sono i file grafici (se vuoi modificarli)

Le immagini delle GUI stanno in `assets/<modulo>/textures/gui/`:
- `qrm-neoforge/.../assets/qrm/textures/gui/` — personaggio, hub, HUD, staff, pulsanti comuni
- `qrm-bank/.../assets/qrm_bank/textures/gui/atm/` — ATM
- `qrm-boss/.../assets/qrm_boss/textures/gui/` — icone Boss

Dopo aver cambiato un PNG, chiudi e riapri il client (o `F3 + T` per ricaricare le texture in gioco).
Non rinominare i file: il programma li cerca per nome.
