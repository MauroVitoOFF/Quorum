# Lista di controllo manuale — qrm-boss (v0.23.0)

Prerequisiti: `dev-start.bat` (server e client con tutti i moduli; mai `clean`). Due giocatori, A e B, ciascuno con un Character. Azienda `acme` con tre rank: `worker` (10), `manager` (20, `boss.menu` + `boss.members.manage`), `boss` (30, `boss.*` e `org.account.*`). A è `boss`, B è `manager`. Un secondo Character C senza azienda. Conto personale di A e di un `worker` D (apri l'ATM almeno una volta), e del denaro sul conto di `acme`. Il rank `boss` ha `boss.*`, quindi anche `boss.notice`, `boss.title` e `boss.bonus`.

## Apertura

1. [ ] A: `/boss` apre il menu (una sola azienda). Il titolo mostra "Acme". Compaiono due schede: Panoramica e Membri.
2. [ ] B: `/boss` apre il menu con Panoramica e Membri, senza il campo per scrivere la bacheca.
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

13. [ ] A: `/boss` apre il menu senza alcuna appartenenza all'organizzazione (`/org info mechanic` non lista membri espliciti).
14. [ ] Membri: A e B in elenco con i gradi, anche B fuori servizio. "Aggiungi" propone C ma non D.
15. [ ] A aggiunge C: `/job` di C mostra `mechanic/apprentice` (il grado più basso). "Promuovi" e "Declassa" cambiano il grado; non oltre `boss`.
16. [ ] "Licenzia" (con conferma) toglie il job a C: `/job` dice "Disoccupato.".
17. [ ] Un pacchetto modificato che assume D: "Questa persona ha già un lavoro." e D resta `taxi`.
18. [ ] A fuori servizio (`/duty off`): `/boss` non si apre.
19. [ ] Un manager con grado `mechanic` e solo `boss.menu` + `boss.members.manage`: non può agire su A né su se stesso.

## Bacheca, titoli e bonus

20. [ ] A, Panoramica: scrive un avviso e preme "Pubblica". Compare nella bacheca con nome e data; B (online) lo riceve in chat. "Rimuovi" lo toglie e la bacheca dice che non c'è nessun avviso.
21. [ ] B: vede l'avviso ma non il campo per scriverlo. Un avviso oltre 160 caratteri viene troncato.
22. [ ] A, Membri, selezione di D, scheda Titolo: assegna "Capocantiere". Il titolo compare accanto al nome nell'elenco. "Togli titolo" lo rimuove.
23. [ ] A non può dare un titolo a un membro di livello superiore al proprio (se A ha un rank sotto di lui).
24. [ ] A, scheda Bonus su D: importo 250 e motivo "Ottimo turno". Il saldo di D sale di 250, quello di `acme` scende; D riceve il messaggio in chat col motivo. Nello storico della banca la causa è "Bonus aziendale: Ottimo turno".
25. [ ] Importo 0, negativo, "abc", "25%", "all", troppi decimali o sopra 5000: "Importo non valido.". Importo sopra il saldo aziendale: "Fondi insufficienti.". Nessun denaro si muove.
26. [ ] Bonus a un membro senza conto personale: "Non hai ancora un conto bancario personale." e il conto non viene creato. A sé stessi: non si può.
27. [ ] Doppio clic rapido su "Dai bonus": un solo movimento.
28. [ ] Membri: ogni riga ha la testa della skin. A colori per un membro online (in servizio, se l'azienda è di un lavoro), in grigio per uno offline o fuori servizio. La Panoramica mostra i conteggi Online e In servizio coerenti.
29. [ ] La scheda di un dipendente mostra testa, nome, rank, presenza e il fascicolo con gli ultimi bonus e titoli. Gli importi rapidi 100, 250, 500 riempiono il campo.
30. [ ] La barra in fondo lampeggia di verde quando un'azione riesce e di rosso, con un piccolo tremolio, quando fallisce.
31. [ ] Un rank con solo `boss.menu` + `boss.bonus`: la sezione Membri c'è, ma senza Ruolo né Aggiungi. Con solo `boss.notice`: nessuna sezione Membri.

## Casi limite

32. [ ] Con il menu aperto, tolgli a B il nodo `boss.members.manage` (`defineRank`): alla prossima azione l'esito è rifiutato e la sezione Membri sparisce.
33. [ ] Con il menu aperto A cambia Character (`/character`): ogni azione è rifiutata con "Il personaggio è cambiato".
34. [ ] `acme` viene archiviata a menu aperto: l'azione successiva chiude il menu con "Questa azienda non è più disponibile.".
35. [ ] B rimosso dall'azienda da un altro a menu aperto: la prossima azione chiude il menu.
36. [ ] Riavvio del server: nessun dato proprio del modulo da verificare (non ha stato).
37. [ ] Resa grafica a risoluzione reale: testata, schede, riquadri, righe, pulsanti (normale, hover, premuto, disabilitato), campi di testo; testi che non escono dai riquadri a scala GUI 2, 3 e 4. All'apertura non compare nulla fuori dalla cornice (in particolare in alto a sinistra).
