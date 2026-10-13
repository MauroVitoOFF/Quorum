---
title: Cambiare l'aspetto dell'interfaccia
description: Cambiare i colori dell'interfaccia di QRM con un resource pack.
---

L'interfaccia di QRM prende colori, misure e immagini da un file, il manifesto del tema. Un resource pack può fornirne uno proprio. Questa guida cambia due colori; serve saper creare un resource pack per Minecraft.

## 1. Crea il resource pack

Crea una cartella con questa struttura:

```text
qrm-tema/
  pack.mcmeta
  assets/qrm/theme/default.json
```

In `pack.mcmeta` indica il formato dei resource pack della tua versione di Minecraft, come per qualsiasi altro pack.

## 2. Scrivi il manifesto

In `assets/qrm/theme/default.json` scrivi solo ciò che vuoi cambiare:

```json sample="docs/samples/qrm-sample-pack/assets/qrm/theme/default.json"
{
  "version": 1,
  "colors": {
    "accent": "#F2B13C",
    "scrim": "#C0201408"
  }
}
```

- `version` è obbligatoria e deve valere `1`.
- Un colore si scrive come `#RRGGBB`, oppure `#AARRGGBB` per indicare anche la trasparenza.
- Tutto ciò che non scrivi resta quello predefinito.

`accent` è il colore di evidenziazione; `scrim` è il velo scuro dietro le schermate.

## 3. Attiva il pack

Copia la cartella in `resourcepacks/` del client e attivala dal menu dei resource pack. Per darla a tutti i giocatori, distribuiscila come resource pack del server.

Se più pack forniscono il manifesto, vale quello più in alto nell'elenco.

## Verifica

Apri la schermata dei personaggi con **O**: l'evidenziazione ha il colore nuovo. Dopo una modifica al file, **F3 + T** ricarica le risorse senza riavviare il gioco.

## Se qualcosa non va

QRM scrive nel log del client cosa ha scartato, con il prefisso `QRM theme`.

| Nel log | Causa |
| --- | --- |
| `unsupported theme version (expected 1)` | Manca `version`, o non vale `1`. Viene usato il tema predefinito per intero. |
| `invalid theme json` | Il file non è JSON valido. Viene usato il tema predefinito per intero. |
| `invalid color '…', using the default` | Quel colore non è scritto come `#RRGGBB` o `#AARRGGBB`. Solo quel valore torna predefinito. |

Un manifesto con un errore non blocca il gioco: nel caso peggiore l'interfaccia resta com'era.

Per cambiare anche misure e immagini vedi [Manifesto del tema](/Quorum/docs/riferimento/manifesto-del-tema/).
