---
title: Manifesto del tema
description: Il formato del file che definisce colori, misure e immagini dell'interfaccia di QRM.
---

Il manifesto del tema è il file `assets/qrm/theme/default.json` di un resource pack. Per un primo esempio vedi [Cambiare l'aspetto dell'interfaccia](/Quorum/docs/guide/aspetto-interfaccia/); questa pagina elenca tutto ciò che il file può contenere.

## Struttura

```json sample="docs/samples/theme/manifest.json"
{
  "version": 1,
  "colors": { "accent": "#3EC9E6" },
  "metrics": { "padding": 8 },
  "textures": {
    "panel": { "id": "qrm:textures/gui/common/panel.png", "size": [48, 48], "slice": [6, 6, 6, 6] }
  }
}
```

`version` è obbligatoria e vale `1`. Le tre sezioni sono facoltative, e dentro ognuna si scrivono solo le chiavi da cambiare.

## Come vengono trattati gli errori

| Errore | Effetto |
| --- | --- |
| JSON non valido, o `version` assente o diversa da `1` | Tutto il manifesto viene scartato e vale il tema predefinito. |
| Un valore non valido | Solo quel valore torna predefinito. |
| Una chiave che QRM non conosce | Viene ignorata. |

Ogni scarto compare nel log del client con il prefisso `QRM theme`.

## Colori

Un colore si scrive `#RRGGBB`, oppure `#AARRGGBB` con la trasparenza.

| Chiave | Predefinito | Uso |
| --- | --- | --- |
| `text` | `#E6EDF5` | Testo. |
| `muted` | `#8A98A8` | Testo secondario. |
| `accent` | `#3EC9E6` | Evidenziazione. |
| `accent2` | `#F2B13C` | Seconda evidenziazione. |
| `error` | `#FF6B6B` | Messaggi di errore. |
| `scrim` | `#A00F141B` | Velo dietro le schermate. |
| `worldDim` | `#660F141B` | Oscuramento del mondo. |
| `worldFrost` | `#1CDCEBFF` | Velatura chiara sul mondo. |
| `statusOn` | `#5EE08A` | Indicatore "in servizio". |
| `statusOff` | `#FF6B6B` | Indicatore "fuori servizio". |
| `staffGold` | `#F5C75B` | Pannello staff. |
| `staffGoldDark` | `#B07A1E` | Pannello staff. |
| `staffCream` | `#F6EBD2` | Pannello staff. |
| `staffMuted` | `#A89BB8` | Pannello staff. |
| `staffDanger` | `#FF7B7B` | Pannello staff, azioni pericolose. |

## Misure

Una misura è un numero maggiore di zero.

| Chiave | Predefinito |
| --- | --- |
| `padding` | `8` |
| `cardWidth` | `120` |
| `cardHeight` | `160` |
| `carouselScale` | `0.82` |
| `carouselGap` | `12` |
| `idCardWidth` | `280` |
| `hudMaxWidth` | `150` |

## Immagini

Ogni immagine è un oggetto con questi campi:

| Campo | Regola |
| --- | --- |
| `id` | Il percorso dell'immagine, nella forma `spazio:percorso`, per esempio `miopack:textures/gui/panel.png`. |
| `size` | Larghezza e altezza del file in pixel, `[larghezza, altezza]`. Fra 1 e 4096. |
| `slice` | Facoltativo. I bordi che non si deformano quando l'immagine viene allargata: `[sinistra, alto, destra, basso]`. La somma dei bordi opposti deve essere minore della dimensione. |

Le immagini che fanno da sfondo, come pannelli e pulsanti, usano `slice`. Le icone no.

Le chiavi disponibili sono 60, raggruppate così:

| Gruppo | Chiavi |
| --- | --- |
| Comuni | `panel`, `button.normal`, `button.hover`, `button.pressed`, `button.disabled` |
| Pulsanti di conferma e di pericolo | `button.confirm.*` e `button.danger.*`, ciascuno con `normal`, `hover`, `pressed` |
| Icone | `icon.fingerprint`, `icon.play`, `icon.plus`, `icon.lock`, `icon.archive` |
| Schermata dei personaggi | `character.card`, `character.card.selected`, `character.card.new`, `character.card.locked`, `character.idcard`, `character.photo`, `character.avatar`, `character.field`, `character.field.focus`, `character.toggle`, `character.toggle.selected` |
| HUD e hub | `hud.panel`, `hub.tab`, `hub.tab.selected` |
| Pannello staff | Le chiavi che iniziano per `staffpanel.` e per `staff.` |

L'elenco completo, con i valori predefiniti, è il file `assets/qrm/theme/default.json` dentro il jar di QRM.

## I Modules

Ogni Module ha un proprio file, `assets/<id del Module>/theme/qrm.json`, con lo stesso formato. Un resource pack lo sostituisce mettendo un file allo stesso percorso. Le chiavi di un Module iniziano tutte con il suo `id`:

| Module | File | Prefisso delle chiavi |
| --- | --- | --- |
| Bank | `assets/qrm_bank/theme/qrm.json` | `qrm_bank.` |
| Locker | `assets/qrm_locker/theme/qrm.json` | `qrm_locker.` |
| Boss | `assets/qrm_boss/theme/qrm.json` | `qrm_boss.` |
