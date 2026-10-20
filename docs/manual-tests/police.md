# Prova in gioco: MDT (terminale operativo)

Basta un mondo singleplayer con `qrm-dev` (in singleplayer l'MDT è aperto a tutti).

1. **Dati**: `/qrm mdt seed`. Atteso: 10 cittadini e 2 BOLO.
2. **Tablet**: «Tablet MDT di Polizia» dalla scheda strumenti, clic destro. Si apre il tablet corazzato; gli altri ti vedono col braccio piegato davanti al petto. Il tasto fisico in basso lo chiude.
3. **Terminale**: il blocco «Terminale di Polizia» (blocchi funzionali), clic destro: stessa schermata.
4. **Dashboard**: tre card (chiamate attive, ricercati, schedati); BOLO a sinistra con `+ Nuovo BOLO` e `Risolvi`; a destra le note rapide, che restano dopo aver chiuso e riaperto (anche dopo il riavvio del gioco).
5. **Cittadini**: fototessera in lista e nel dettaglio (faccia vera per i giocatori online, iniziali colorate per gli altri); `Solo Ricercati` filtra; ricercato/revoca, aggiungi reato, porto, patente, pericolosità; i precedenti scorrono con la rotella.
6. **Dispatch**: `/qrm mdt simulatecall sparatoria` (o `medical`, `police`): doppio beep, chat e chiamata in lista con distanza. `Rispondi` mostra la freccia con la distanza in alto, che sparisce a 8 blocchi. `! Invia Posizione / Supporto` crea subito una chiamata alla tua posizione (rifiutata se ripetuta entro 10 s). Il LED in alto alterna rosso e ambra finché c'è una chiamata.
7. **Medical**: `/qrm medical down <te>`, premi `C`: nasce una chiamata «Persona ferita a terra».
8. **Codice penale**: spunta più reati: la barra in fondo somma reati, multa e mesi. `Azzera Selezione` svuota. `Copia Riepilogo` copia il testo e, con un cittadino aperto, precompila il modulo del reato.
9. **Persistenza**: esci dal mondo e rientra: i dati restano. `/qrm mdt clear` svuota tutto.
