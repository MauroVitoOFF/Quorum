# qrm-medical — specifica di progetto

- Data: 2026-10-16
- Stato: implementata in 0.25.0 (qrm-medical). Resta da fare la prova in gioco (docs/manual-tests/medical.md).
- Versione di partenza: QRM 0.24.0 (Minecraft 26.2, NeoForge 26.2.0.88, Java 25)
- Versione di arrivo: QRM 0.25.0

## 1. Obiettivo

Un Module esterno, `qrm-medical`, che sostituisce la morte vanilla con un flusso a stadi che coinvolge cittadini, polizia
e medici. A zero vita il giocatore non muore: cade a terra, perde i sensi, e se nessuno lo aiuta viene ricoverato.

## 2. Decisioni prese

| Tema | Decisione |
| --- | --- |
| Dove | Module separato `qrm-medical` (modId `qrm_medical`), compila solo contro `qrm-api` e `qrm-client-api`. Il Core non cambia. |
| Dati | Stato medico legato alla Character, in `ModDataService` (namespace `qrm_medical`, chiave `profile`). L'ospedale è del sistema (chiave `hospital`). |
| Rete | Canale `optional()`; un solo pacchetto S→C (stato del giocatore stesso) e uno C→S (resa, allerta). Gli altri giocatori vedono la posa grazie alla sincronizzazione vanilla. |
| Posa | `Pose.SWIMMING` imposta a ogni tick sul server (e sul client locale). Nessun mixin. |
| Allerta | Nodo di permesso `medical.alert.receive`. |
| Ispezione | Aperta a tutti; il nodo `medical.treat` aggiunge il tempo rimasto. |
| Oggetti | Defibrillatore, Kit medico, Benda: oggetti semplici, l'effetto sta negli eventi. Nessun blocco di ruolo (vedi "Niente blocchi RP meccanici"). |
| Comandi | Sotto la radice esistente `/qrm medical ...` (la radice è `qrm`, non `quorum`); `/carry` per tutti. |

## 3. Stati e tempi

- `HEALTHY` → `DOWNED`: un colpo che porterebbe a zero o meno. Vita a 1.0, timer 3600 tick, il colpo viene annullato.
- `DOWNED` → `UNCONSCIOUS`: timer sotto 1200 tick, oppure colpo diretto di una creatura.
- `UNCONSCIOUS`/`DOWNED` → `CLINICALLY_DEAD`: timer a 0, oppure resa (G tenuto 100 tick) con almeno 2400 tick di tempo a terra (la RCP non conta).
- Morte clinica = ricovero: stato torna sano, vita al 50%, effetti tolti, convalescenza (debolezza e lentezza, 1200 tick), teletrasporto all'ospedale se impostato. L'inventario non cambia.
- RCP: il timer avanza di un tick ogni cinque (-80%); il tempo a terra non avanza.
- Defibrillatore: 80 tick di carica, poi `HEALTHY` con 3 cuori. Kit sul ferito: timer a pieno, resta a terra.
- Bloccato per chi è a terra: attacco, uso di oggetti, rottura e posa di blocchi, clic su blocchi, contenitori, lancio di oggetti, salto (attributo), velocità (-90%, incosciente -100%).
- `/kill` e il vuoto (`BYPASSES_INVULNERABILITY`) uccidono davvero; lo stato medico viene azzerato.

## 4. Pacchetti

```
dev.qrm.medical
├─ logic/   MedicalState, MedicalProfile, MedicalRules, InjuryRecord, InjuryType, MedicalStorage, ModDataMedicalStorage
├─ mc/      MedicalMod, MedicalServer, MedicalEvents, MedicalCommand, MedicalItems
├─ net/     MedicalPayloads, MedicalNetwork
└─ client/  MedicalClient, MedicalHud, MedicalKeys, ClientMedical, MedicalClientBridge
```

## 5. Fuori dalla prima versione

- Rimozione degli oggetti illegali al ricovero.
- Durata o usura degli oggetti medici (defibrillatore e kit non si consumano, tranne kit e bende che si usano).
- Trasporto con tasto dedicato o animazione; ora è un comando e un teletrasporto dietro al trasportatore.
- Modello di rendering personalizzato per la posa a terra.
