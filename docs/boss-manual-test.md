# Lista di controllo manuale — qrm-boss (v0.16.0)

Prerequisiti: `dev-start.bat` (server e client con tutti i moduli; mai `clean`). Due giocatori, A e B, ciascuno con un Character. Azienda `acme` con tre rank: `worker` (10), `manager` (20, `boss.menu` + `boss.members.manage`), `boss` (30, `boss.*` e `org.account.*`). A è `boss`, B è `manager`. Un secondo Character C senza azienda. Conto personale di A con del denaro (apri l'ATM almeno una volta).

## Apertura

1. [ ] A: `/boss` apre il menu (una sola azienda). Il titolo mostra "Acme". Compaiono le tre linguette: Panoramica, Membri, Conto.
2. [ ] B: `/boss` apre il menu con Panoramica e Membri, senza Conto.
3. [ ] C: `/boss` dà errore, non si apre nulla. `/boss acme` dà "Azienda sconosciuta." (stesso messaggio per un'azienda che non esiste).
4. [ ] A ha `boss.menu` anche in una seconda azienda: `/boss` apre il selettore; scegliere un'azienda apre il menu. `/boss acme` apre direttamente; il completamento suggerisce solo le sue aziende.
5. [ ] Staff: `/qrm admin point create boss_desk org acme` su un blocco. A fa click destro: si apre il menu di `acme`. C fa click destro: "Non puoi usare questo punto." Il tipo non si crea con `job x` né con un Character.
6. [ ] Un client senza il mod non è obbligato a installarlo; il server non lo scarta e `/boss` non apre nulla per lui.

## Membri

7. [ ] A, sezione Membri: elenco con rank, ordinato dal rank più alto. Il pulsante "Aggiungi" mostra i giocatori online non membri (C).
8. [ ] A aggiunge C: entra con il rank di livello più basso (`worker`). Lo stato dice "Fatto.".
9. [ ] A seleziona C e preme "Promuovi": C diventa `manager`. "Declassa" lo riporta a `worker`. Non si può promuoverlo oltre il livello di A.
10. [ ] "Licenzia" chiede conferma ("Conferma?"); al secondo clic C esce dall'azienda. Il pulsante torna normale cambiando selezione.
11. [ ] B (manager): non può selezionare A (livello più alto) né agire su se stesso: i pulsanti sono disabilitati. Può agire su un `worker`.
12. [ ] Un pacchetto inviato da un client modificato (rank sopra il proprio, bersaglio di livello superiore, se stesso) viene rifiutato con il messaggio corretto.

## Membri di un'azienda con Job

Prerequisiti: `mechanic.json` con i gradi `apprentice` (1), `mechanic` (2) e `boss` (4, `boss.*` e `org.account.*`); dopo il riavvio esiste l'organizzazione `mechanic`. A è `boss` (`/job set A mechanic boss`, poi `/duty on`), B è `apprentice`, C non ha lavoro, D ha il job `taxi`.

26. [ ] A: `/boss` apre il menu senza alcuna appartenenza all'organizzazione (`/org info mechanic` non lista membri espliciti).
27. [ ] Membri: A e B in elenco con i gradi, anche B fuori servizio. "Aggiungi" propone C ma non D.
28. [ ] A aggiunge C: `/job` di C mostra `mechanic/apprentice` (il grado più basso). "Promuovi" e "Declassa" cambiano il grado; non oltre `boss`.
29. [ ] "Licenzia" (con conferma) toglie il job a C: `/job` dice "Disoccupato.".
30. [ ] Un pacchetto modificato che assume D: "Questa persona ha già un lavoro." e D resta `taxi`.
31. [ ] A fuori servizio (`/duty off`): `/boss` non si apre.
32. [ ] Un manager con grado `mechanic` e solo `boss.menu` + `boss.members.manage`: non può agire su A né su se stesso.

## Conto

13. [ ] A, sezione Conto: saldo dell'azienda nella placca. Deposito con 500, 25%, 50%, Tutto e importo libero; dopo ogni operazione il saldo si aggiorna e lo stato dice "Fatto.".
14. [ ] Le percentuali sono sul saldo di partenza (personale per il deposito, aziendale per il prelievo) e arrotondano per difetto.
15. [ ] Importo 0, negativo, "abc", troppi decimali: "Importo non valido.". Importo sopra il saldo: "Fondi insufficienti.". Nessun denaro si muove.
16. [ ] Giocatore senza conto personale: "Non hai ancora un conto bancario personale." e il conto non viene creato.
17. [ ] Doppio clic rapido su Deposita: un solo movimento (il limitatore scarta la raffica; ogni clic ha chiave diversa).
18. [ ] Un rank con solo `boss.menu` + `boss.bank.deposit`: sezione Conto visibile, Preleva disabilitato, saldo nascosto ("Saldo nascosto"). Con solo `boss.bank.view`: saldo visibile, pulsanti disabilitati.
19. [ ] I movimenti compaiono nella cronologia della banca con causa "Deposito aziendale" / prelievo.

## Casi limite

20. [ ] Con il menu aperto, tolgli a B il nodo `boss.members.manage` (`defineRank`): alla prossima azione l'esito è rifiutato e la sezione Membri sparisce.
21. [ ] Con il menu aperto A cambia Character (`/character`): ogni azione è rifiutata con "Il personaggio è cambiato".
22. [ ] `acme` viene archiviata a menu aperto: l'azione successiva chiude il menu con "Questa azienda non è più disponibile.".
23. [ ] B rimosso dall'azienda da un altro a menu aperto: la prossima azione chiude il menu.
24. [ ] Riavvio del server: nessun dato proprio del modulo da verificare (non ha stato).
25. [ ] Resa grafica a risoluzione reale: cornice, placca titolo, linguette, righe, pulsanti (normale, hover, premuto, disabilitato), barra saldo; testi che non escono dai riquadri a scala GUI 2, 3 e 4.
