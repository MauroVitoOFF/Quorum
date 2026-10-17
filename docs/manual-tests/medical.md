# Prova in gioco: Medical

Serve un server con QRM + qrm-medical, due giocatori A e B con un personaggio ciascuno, e un operatore.

1. **Ospedale**: come operatore, in un punto fisso `/qrm medical sethospital`.
2. **Caduta**: B si fa colpire a morte (o `/qrm medical down B`). Atteso: B non muore, vita a mezzo cuore, A lo vede sdraiato a terra (posa nuoto). B vede il bordo rosso pulsante, il testo «Sei gravemente ferito. Tempo rimanente» e sente il battito.
3. **Blocchi**: B non può saltare, colpire, rompere o piazzare blocchi, usare oggetti, aprire l'inventario. Si muove molto piano.
4. **Ispezione**: A, mani vuote, clic destro su B: stato, polso e ferite con tipo e tempo.
5. **RCP**: A, accovacciato e a mani vuote, tiene premuto il clic destro su B. Atteso: cuori, suono, B vede «Un cittadino sta stabilizzando le tue ferite...», il tempo scende molto più piano. Rilasciando, il tempo torna normale.
6. **Allerta**: dare ad A il nodo `medical.alert.receive`; B preme `C`. A riceve il messaggio con le coordinate. Una seconda pressione entro 30 s è rifiutata.
7. **Trasporto**: A entra in `/carry` vicino a B: B lo segue, A è lento. Di nuovo `/carry` lo lascia.
8. **Incoscienza**: aspettare che manchino meno di 60 s (o colpire B direttamente con un'arma): schermo nero al 95%, B immobile.
9. **Defibrillatore**: A prende il defibrillatore dalla scheda strumenti del creativo, clic destro su B. Dopo 4 s B si rialza con 3 cuori. Allontanarsi o cambiare oggetto interrompe.
10. **Kit e benda**: kit su B a terra: il tempo torna a 3:00 e B resta a terra. Benda su un ferito in piedi: cura 4 cuori.
11. **Morte clinica**: lasciare scadere il tempo, oppure dopo 2 minuti tenere `G` per 5 s. Atteso: B va all'ospedale con metà vita, debolezza e lentezza per 60 s, e tutto l'inventario.
12. **Persistenza**: mettere B a terra, farlo uscire e rientrare: ritrova lo stato e il tempo. Cambiare personaggio: l'altro personaggio è sano.
13. **/kill**: su un giocatore a terra lo uccide davvero, e al ritorno è sano.
