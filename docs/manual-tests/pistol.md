# Prova in gioco: pistola d'ordinanza (qrm-police 0.29.0)

Serve un server con QRM + qrm-police (con `qrm-medical` per i punti 13-14), due giocatori A e B con un personaggio ciascuno, e un operatore. Per i punti 11 e 12 serve anche un lavoro (es. `police`) assegnato ad A.

Oggetti: scheda «Combattimento» del creativo, oppure `/give @s qrm_police:police_pistol` e `/give @s qrm_police:police_magazine`.
Tasti predefiniti: sparo = clic sinistro, mira = clic destro tenuto, ricarica = `R`, interazione = `V` (pressione breve: arretra il carrello quando serve, altrimenti controllo munizioni; tenuto premuto: ispezione completa; con un caricatore in mano: ispezione del caricatore).

Compila la colonna «Esito» (OK / KO + nota) durante la prova.

| # | Cosa fare | Atteso | Esito |
|---|---|---|---|
| 1 | A prende pistola e 3 caricatori (anche con `/give`), mette la pistola in mano. | Estrazione con animazione e suono; accanto alla pistola compare il suggerimento «Ricarica» perché non c'è caricatore inserito. Mai un contatore di colpi. | |
| 2 | A preme `R`. | Ricarica a vuoto (~2,7 s): il caricatore entra, poi il carrello. Al termine la pistola spara. | |
| 3 | A spara fino a esaurire il caricatore (17 colpi). | Dopo l'ultimo colpo il carrello resta aperto, suono di blocco, suggerimento «Ricarica». Un altro clic sinistro dà il clic a vuoto, nessun colpo. | |
| 4 | A preme `R` con il carrello aperto. | Ricarica a vuoto, la pistola torna a sparare. Il caricatore vuoto tolto è nell'inventario (non sparisce). | |
| 5 | A spara qualche colpo (non tutti) e preme `R`. | Ricarica tattica (~2,5 s): il caricatore parziale torna nell'inventario, ne entra uno nuovo; il colpo in canna resta. Il caricatore appena tolto non viene reinserito nella stessa ricarica. | |
| 6 | Interrompere la ricarica tra `mag_out` e `mag_in`: premere `R` e cambiare slot dopo circa 1 s. | La pistola resta senza caricatore; il caricatore tolto è nell'inventario o a terra; nessun colpo creato né perso. Tornando sulla pistola: suggerimento «Ricarica». | |
| 7 | Ricarica a vuoto interrotta dopo `mag_in` (cambiare slot a circa 2 s dall'inizio), poi tornare sulla pistola. | Caricatore inserito, carrello aperto, suggerimento del tasto del carrello. Premere `V` (breve): il carrello si chiude e il colpo va in canna; la pistola spara. | |
| 8 | Con caricatore inserito, camera vuota e carrello chiuso (se non si riesce a riprodurre, saltare). | Clic a vuoto; suggerimento sul tasto `V`; premendolo il carrello si arretra (~0,5 s) e la pistola spara. | |
| 9 | A non ha caricatori con colpi nell'inventario e prova a sparare o ricaricare. | Clic a vuoto; compare un avviso senza tasto. Mai un contatore. | |
| 10 | A preme brevemente `V` con pistola e caricatore inseriti, poi tiene premuto `V` (oltre 0,3 s). | Breve: controllo munizioni, fascia approssimata del caricatore e indicazione esatta del colpo in canna. Tenuto: ispezione completa (gesto estetico). | |
| 11 | Punto armeria: come operatore, guardando un blocco da un lato, `/qrm admin point create armory job police`, poi `/qrm admin point set public true` se serve vederlo. A (job `police`, in servizio se richiesto), con la pistola non in mano, fa clic destro sul blocco con alcuni caricatori vuoti o parziali. | Messaggio «Armeria: hai 3 caricatori pieni.»; i caricatori portati sono pieni, ne vengono aggiunti fino a 3; quelli inseriti nella pistola non cambiano. B senza il lavoro: «Non puoi usare questo punto.» | |
| 12 | Armeria con inventario pieno. | I caricatori in più cadono ai piedi di A, non spariscono. | |
| 13 | B in piedi, senza armatura e con vita piena: A gli spara al corpo. | B cade a terra dopo circa 5 colpi (≈4 danni a colpo); Medical registra ferite di tipo proiettile (ispezione di A: clic destro a mani vuote su B). | |
| 14 | B a terra (Medical) prova a sparare, mirare e ricaricare con la pistola in inventario. | Il server rifiuta: nessun colpo, nessuna ricarica; la pistola resta nell'inventario. Una ricarica in corso quando B cade si interrompe. Sul client di B nessun suono, lampo o contraccolpo di sparo e nessuna mira (né FOV né posa vista da A); se B mirava quando è caduto, la mira si spegne. | |
| 15 | A e B vicini: A spara in vari punti. | B vede e sente lo stesso sparo nello stesso punto: vampa, suono spaziale, posa del braccio di A. Se A colpisce B, il danno è deciso dal server. | |
| 16 | A spara dal fianco a 30 blocchi contro un bersaglio, poi di nuovo tenendo premuto il clic destro (mira). | In mira il campo visivo si stringe leggermente (~0,2 s di transizione) e la dispersione dei colpi è chiaramente minore che dal fianco. | |
| 17 | Raffica: A tiene premuto o clicca freneticamente il clic sinistro per 5 secondi con caricatore pieno. | Prova del client normale: massimo circa 4 colpi al secondo (5 tick tra i colpi; il server ne accetta uno ogni 4 tick per la latenza); un clic, un colpo; nessun colpo extra. | |
| 18 | A preme `R` per ricaricare e, subito, cambia slot e torna sulla pistola (entro 1 s). | La ricarica è annullata: nessun colpo creato o perso, nessuna azione residua applicata a un altro oggetto; la pistola è nello stato dell'ultimo istante concluso. | |
| 19 | A tiene la pistola con inventario principale pieno e preme `R` con un caricatore di scorta. | Al `mag_out` il caricatore tolto cade a terra vicino ad A, non sparisce. | |
| 20 | A (client normale): sparare durante una ricarica in corso. | Rifiutato: nessun colpo parte finché la ricarica non è conclusa o interrotta. (Un client modificato che invia `FIRE` a raffica o durante la ricarica è respinto anche dal server: coperto dai test automatici.) | |
| 21 | Tooltip dei caricatori: A mette un caricatore nuovo in inventario e passa il mouse sopra. | Nessuna fascia nel tooltip (sconosciuto). | |
| 22 | A ispeziona un caricatore tenendolo in mano e premendo `V` (o usa il controllo munizioni sulla pistola per quello inserito), poi lo ripone e spara con la pistola. | Durante l'ispezione si sente il suono dell'ispezione; alla fine la fascia compare sopra la barra degli oggetti («Caricatore: …»). Il tooltip mostra la fascia e la aggiorna a ogni colpo. | |
| 23 | A passa il caricatore ispezionato a B (lo butta, B lo raccoglie) e B passa il mouse sopra. | Per B la fascia è sconosciuta finché non lo ispeziona; ad A continua a mostrarla. Nessuna barra sull'icona. | |
| 24 | Disconnessione: A esce durante una ricarica e rientra. | Stato dell'ultimo istante concluso; nessun colpo creato o perso; nessuna ricarica che riparte. | |
| 25 | Controllo dei suoni: ascoltare sparo, ricarica, carrello, clic a vuoto. | Funzionano ma sono segnaposto dei suoni di Minecraft: non è un difetto di questa versione (vedi sotto). | |
| 26 | Criteri 9 e 10 della specifica: ogni file audio è elencato in `qrm-police/AUDIO_SOURCES.md` (oggi nessuno: i suoni sono segnaposto); modello e animazioni in gioco derivano dal `.bbmodel` tramite `tools/pistol_export.py`, e rieseguirlo lascia git pulito. | Nessun file audio senza fonte; `git status` pulito dopo l'esportatore. | |

## Limiti noti di questa versione

- I suoni sono segnaposto vanilla finché non si trovano audio CC0 o royalty-free reali (fonti in `qrm-police/AUDIO_SOURCES.md`).
- In prima persona si vede solo il braccio destro: i bone delle braccia non sono ancora disegnati.
- La posa di mira è uno scostamento fisso.
- Con la pistola in mano la mano secondaria e il clic destro sui blocchi non funzionano (il clic destro è la mira): il punto armeria si usa con la pistola non in mano.
- La direzione del contraccolpo della camera e gli scostamenti delle particelle della volata vanno ancora tarati.
- L'ispezione del caricatore non ha animazione (solo suono e fascia sopra la barra degli oggetti).
