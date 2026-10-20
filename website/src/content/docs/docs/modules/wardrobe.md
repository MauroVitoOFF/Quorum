---
title: Wardrobe
description: Divise di lavoro che sono skin, cambiate da un armadietto. Cartella delle divise, regole di grado e comandi.
---

Wardrobe dà a ogni lavoro le sue divise. Una divisa è una skin: il giocatore la indossa da un armadietto e tutti gli altri lo vedono vestito così, con la sua testa e il suo viso al posto di quelli della divisa. Non è un oggetto: non si può perdere, rubare o spostare nell'inventario.

**Disponibilità**: incluso nel progetto. Va installato sul server e sui client: un client senza il mod vede tutti con la skin normale e non può aprire l'armadietto.

## Installare

1. Copia `qrm-wardrobe-<versione>.jar` in `mods/`, sul server e sui client.
2. Avvia il server. Wardrobe crea la cartella `config/qrm_wardrobe/outfits/`, vuota.
3. Aggiungi le divise (vedi sotto) e crea un armadietto per il lavoro.

## Aggiungere una divisa

Per ogni divisa servono due file con lo stesso nome nella cartella `config/qrm_wardrobe/outfits/`: un PNG con la skin e un file JSON che la descrive. Il nome, senza estensione, è l'identificatore della divisa, e segue le stesse regole degli identificatori dei lavori.

```json sample="docs/samples/wardrobe/agente_pattuglia.json"
{
  "label": "Agente di pattuglia",
  "job": "police",
  "minGrade": "officer",
  "model": "classic"
}
```

| Campo | Regola |
| --- | --- |
| `label` | Il nome mostrato nell'armadietto, da 1 a 40 caratteri. |
| `job` | L'identificatore di un lavoro che esiste. |
| `minGrade` | Il grado minimo per indossarla, fra quelli di quel lavoro. Facoltativo: se manca, basta avere il lavoro. |
| `model` | `classic` per le braccia da 4 pixel, `slim` per quelle da 3. Facoltativo, predefinito `classic`. |

Il PNG è una skin nel formato attuale di Minecraft: 64 per 64 pixel, al massimo 32 KB. Non vale il vecchio formato 64 per 32. Le prime 16 righe del PNG sono la testa e il suo strato esterno: vengono sempre sostituite da quelle del giocatore, quindi non serve disegnarle.

Una divisa non valida viene saltata e il motivo finisce nel log del server, con il nome del file. Le altre si caricano comunque. Sono ammesse al massimo 128 divise.

Le divise si leggono all'avvio. Dopo averne cambiata una:

```text
/qrm wardrobe reload
```

Il comando è riservato agli operatori. Rilegge la cartella e manda il nuovo catalogo ai client. Chi indossa una divisa che non c'è più, o che non è più valida, la perde.

## Creare un armadietto

L'armadietto è un [punto di interazione](/Quorum/docs/sistemi/punti-di-interazione/) di tipo `wardrobe`. Guarda un blocco e crea il punto per un lavoro:

```text
/qrm admin point create wardrobe job police
```

Solo un lavoro può possedere un armadietto, e per usarlo bisogna essere in servizio. L'armadietto di un lavoro mostra le divise di quel lavoro, più l'**Abito civile**. Ogni giocatore vede solo quelle per cui il suo grado basta.

## Come funziona

- **Si cambia solo dall'armadietto.** Entrare in servizio non mette la divisa da sola: il giocatore apre l'armadietto e la sceglie, guardando un manichino grigio che la indossa.
- **Si toglie in tre modi.** Scegliendo Abito civile, uscendo di servizio, oppure perdendo i requisiti: cambiare lavoro, essere declassato sotto il grado minimo, o cambiare personaggio.
- **È legata al personaggio.** Due personaggi dello stesso giocatore hanno ciascuno la propria divisa, e vale quella del personaggio attivo.
- **Si ricorda.** La divisa indossata resta anche dopo un riavvio del server, e viene ricontrollata quando il giocatore rientra.
- **È solo estetica.** Una divisa non dà e non toglie nessuna azione: chi la indossa non ha permessi in più.

## Limiti

- Gli oggetti di una divisa, come un casco o un'arma, non fanno parte di Wardrobe.
- In prima persona il braccio ha la skin della divisa, ma la forma (classica o sottile) resta quella del proprio modello.
- Il mantello e le ali del giocatore restano quelli originali.
- Un altro mod che cambia le skin dei giocatori può entrare in conflitto con Wardrobe.

## Problemi comuni

| Cosa vedi | Causa |
| --- | --- |
| `Per cambiare divisa serve il mod QRM Wardrobe anche sul tuo client.` | Il client non ha il mod. |
| Una divisa non compare nell'armadietto | Il grado del giocatore è sotto il minimo, oppure il file è stato saltato: controlla il log del server. |
| Il giocatore ha la testa di Steve o di Alex sotto la divisa | La sua skin non è ancora stata scaricata: si aggiorna da sola. |
