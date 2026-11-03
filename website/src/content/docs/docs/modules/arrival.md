---
title: Arrival
description: Il primo arrivo di un personaggio nuovo, che si risveglia a terra in un vicolo della città. Punto, kit e durate.
---

Arrival fa iniziare una nuova vita come si deve. Quando un giocatore seleziona per la prima volta un personaggio appena creato, quel personaggio non compare semplicemente nel mondo: si risveglia a terra in un vicolo, apre gli occhi e si rialza. Da quel momento è libero. Nessun obiettivo, nessuna freccia, nessun tutorial: ha addosso lo stretto necessario e deve cavarsela da solo in una città che non conosce.

**Disponibilità**: incluso nel progetto. Va installato sul server. Sui client è facoltativo: chi non lo ha entra e gioca, ma non vede il buio e le palpebre che si aprono. Gli altri giocatori lo vedono comunque disteso a terra e poi in piedi.

## Dove si risveglia

Il punto del vicolo lo scegli tu. Mettiti in piedi nel vicolo, nella direzione in cui vuoi che il personaggio si sveglia, e lancia `/qrm arrival setspawn`. Il punto resta salvato in `config/qrm_arrival/spawn.json` e vale per tutti gli arrivi.

Finché il punto non è impostato, nessuno vive l'arrivo: i personaggi nuovi entrano come sempre e il log del server ricorda che manca. Succede lo stesso se la dimensione indicata nel file non esiste più.

## Cosa succede

Il personaggio parte disteso a terra con lo schermo buio e le palpebre chiuse. Il buio si schiarisce, le palpebre si aprono, il corpo passa dall'essere disteso all'accovacciato e poi in piedi. La camera non si muove da sola: è solo il personaggio che si alza. Per tutta la durata il giocatore non può camminare, attaccare, rompere o piazzare blocchi né usare oggetti.

Se il giocatore esce, muore o cambia personaggio a metà, il risveglio si interrompe e non ha dato niente. Selezionando di nuovo quel personaggio ricomincia da capo. Il risveglio vale una volta sola per personaggio: finito, non si ripete più, nemmeno selezionandolo di nuovo.

I personaggi creati prima di installare Arrival non lo vivono: hanno già una storia e non ricevono il kit.

## Il kit

A fine risveglio il personaggio riceve cento in contanti: una banconota da 10000 unità minime della valuta `QRM` di esempio. Se ha l'inventario pieno, i contanti cadono a terra ai suoi piedi. Gli abiti sono quelli civili di sempre, cioè la skin normale del giocatore.

Il kit è nel file `config/qrm_arrival/kit.json`, che viene creato la prima volta. Ogni voce è un oggetto di Minecraft con la sua quantità, da 1 a 64:

```json
{
  "items": [
    { "item": "qrm_bank:cash_qrm_10000", "count": 1 }
  ]
}
```

Se la valuta del tuo server non si chiama `QRM`, cambia l'identificatore del contante: il nome è `cash_` seguito dal codice della valuta in minuscolo e dal taglio. Un oggetto che non esiste viene saltato e il log lo segnala; un file rotto fa tornare il kit predefinito.

La carta d'identità non è nel kit: oggi non è ancora un oggetto, e quando lo diventerà potrà essere aggiunta qui.

## Le durate

`config/qrm_arrival/timing.json` è facoltativo e si legge in tick (venti al secondo):

```json
{
  "lyingTicks": 60,
  "risingTicks": 40,
  "maxLockTicks": 400
}
```

`lyingTicks` è il tempo a terra, `risingTicks` quello per rialzarsi e `maxLockTicks` il limite oltre il quale il giocatore viene comunque liberato, senza kit. I valori mancanti o non positivi tornano a quelli qui sopra. Dopo una modifica lancia `/qrm arrival reload`.

## Comandi

| Comando | Chi | Cosa fa |
| --- | --- | --- |
| `/qrm arrival setspawn` | Operatori, solo giocatori | Imposta il punto del vicolo: la tua posizione e la tua direzione |
| `/qrm arrival reload` | Operatori | Rilegge kit, punto e durate. Gli arrivi in corso si interrompono |
