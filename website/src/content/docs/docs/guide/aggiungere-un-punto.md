---
title: Aggiungere un punto di interazione
description: Trasformare un blocco in un punto dove un lavoro entra e esce dal servizio.
---

Questa guida mette in una caserma un blocco che fa entrare e uscire dal servizio gli agenti. Servono i permessi di operatore e un lavoro già definito: se non ne hai uno, parti da [Creare un lavoro](/Quorum/docs/guide/creare-un-lavoro/).

Negli esempi il lavoro si chiama `police`. Le regole complete sono in [Punti di interazione](/Quorum/docs/sistemi/punti-di-interazione/).

## 1. Piazza il blocco

Un punto si appoggia a un blocco che esiste già. Piazza quello che preferisci, per esempio un leggio, nel posto dove vuoi il punto.

## 2. Crea il punto

Guarda il blocco da un lato, da non più di 6 blocchi, e scrivi:

```text
/qrm admin point create duty job police
```

La risposta riporta il numero del punto e la posizione:

```text
Punto #1 (duty) creato per job police in minecraft:overworld 120 64 -35, icona sulla faccia nord.
```

L'icona compare sulla faccia che stavi guardando.

## 3. Regola chi lo vede

Appena creato, l'icona è visibile solo a chi ha il lavoro `police`. Per mostrarla a tutti, guarda il blocco e scrivi:

```text
/qrm admin point set public true
```

Gli altri vedranno l'icona, ma non potranno usare il punto.

## 4. Dagli un nome

Facoltativo:

```text
/qrm admin point set name Timbratrice
```

## Verifica

1. Con un personaggio che ha il lavoro `police`, fai clic destro sul blocco: leggi `Sei in servizio.` Un secondo clic risponde `Sei fuori servizio.`
2. Con un personaggio che non ha quel lavoro, il clic risponde `Non puoi usare questo punto.`
3. Guardando il blocco, `/qrm admin point info` mostra proprietario, grado minimo, faccia dell'icona e `Stato: attivo`.

## Riservarlo a un grado

Per lasciare il punto solo ai gradi da un certo livello in su, guarda il blocco e indica il grado minimo:

```text
/qrm admin point set grade chief
```

`/qrm admin point set grade none` lo riapre a tutti i gradi.

## Spostarlo o toglierlo

Un blocco che ospita un punto non si può rompere. Per spostare il punto guarda il blocco nuovo e scrivi `/qrm admin point move 1`, dove `1` è il numero del punto. Per toglierlo guarda il blocco e scrivi `/qrm admin point remove`.

## Problemi comuni

| Cosa vedi | Causa |
| --- | --- |
| `Inquadra un blocco (entro 6 blocchi).` | Non stai guardando un blocco, o è troppo lontano. |
| `Proprietario inesistente o archiviato: job police` | Il lavoro non è definito. Controlla con `/job list`. |
| `Il tipo duty non accetta proprietari di tipo org.` | `duty` funziona solo con un lavoro. |
| `Il blocco ospita già il punto #…` | Su quel blocco c'è già un punto. |
| L'icona non si vede | Il giocatore non ha QRM sul client, oppure è a più di 32 blocchi, oppure non può usare il punto e `public` è `false`. |
