# qrm-wardrobe — specifica di progetto

- Data: 2026-10-16
- Stato: implementata in 0.24.0 (qrm-wardrobe). Resta da fare la prova in gioco (docs/manual-tests/wardrobe.md).
- Versione di partenza: QRM 0.23.0 (Minecraft 26.2, NeoForge 26.2.0.88, Java 25)
- Versione di arrivo: QRM 0.24.0
- Si appoggia a: [Interaction Points](2026-10-10-interaction-points-design.md) e a [qrm-locker](2026-10-10-qrm-locker-design.md) come esempio di Module esterno con dati propri.

## 1. Obiettivo

Un Module esterno, `qrm-wardrobe` (nome mostrato: Wardrobe), che permette a ogni lavoro di avere le **sue divise**.
Una divisa è una **skin**: il giocatore la indossa da un armadietto e tutti gli altri lo vedono vestito così. La testa
resta sempre quella originale del giocatore (faccia e strato esterno).

Serve a far riconoscere a colpo d'occhio chi fa un lavoro, senza oggetti e senza resource pack da distribuire. Nasce per
la polizia ma vale per ogni lavoro: Wardrobe non contiene nulla di specifico di un mestiere.

Criteri di riuscita:

1. Un server owner aggiunge una divisa mettendo un PNG e un file di descrizione in una cartella del server, senza
   toccare i client.
2. Un giocatore in servizio apre l'armadietto del suo lavoro, vede le divise che il suo grado permette, ne sceglie una e
   da quel momento gli altri giocatori lo vedono con quel corpo e con la sua testa.
3. Uscire di servizio, scegliere "Abito civile", cambiare Character o perdere i requisiti riporta alla skin normale.
4. Un client senza il mod entra e gioca: vede tutti con la skin normale.
5. `qrm-wardrobe` compila solo contro `qrm-api` e `qrm-client-api`, come `qrm-bank` e `qrm-locker`. Il Core non cambia.

## 2. Fuori dalla prima versione (anti-obiettivi)

- **Nessun oggetto fisico.** Armature, armi e rifornimenti non fanno parte di una divisa. Sono un progetto a parte
  (equipaggiamento tracciato, con rifornimenti), da progettare dopo.
- **Nessun blocco sulle armi o su altro.** La divisa è solo estetica: non dà né toglie nessuna azione.
- Niente scelta di una divisa "per gioco", fuori da un armadietto; niente cambio automatico all'entrata in servizio.
- Niente mantello o elitre personalizzati: restano quelli del giocatore.
- Niente divise per i non giocatori (NPC, mob).

## 3. Decisioni prese

| Tema | Decisione |
|---|---|
| Che cos'è una divisa | Solo una skin. Nessun oggetto. |
| Testa | Sempre quella originale del giocatore: faccia e strato esterno (cappello). |
| Dove stanno le skin | In una cartella del server; il server le manda ai client. Nessun resource pack. |
| Descrizione | Un file `.json` accanto al `.png`, con lo stesso nome. |
| Chi la può indossare | Chi ha il lavoro della divisa e un grado almeno pari al grado minimo indicato. |
| Dove si cambia | Solo in un armadietto, che è un punto di interazione. Mai in automatico. |
| Armadietto | Appartiene a un lavoro e mostra solo le divise di quel lavoro, più "Abito civile". |
| Servizio | L'armadietto richiede di essere in servizio; uscire di servizio toglie la divisa. |
| Anteprima | Manichino grigio con la divisa evidenziata nella lista. |
| Legame | Alla Character, non al giocatore. |
| Persistenza | La divisa indossata si ricorda fra i riavvii, e viene ricontrollata quando il giocatore entra. |

## 4. Architettura

```
qrm-wardrobe (modId qrm_wardrobe)   dipende solo da
        ▼
qrm-api          PointTypeRegistry, JobService, JobRegistry, ModDataService, EventBus
qrm-client-api   ThemeRenderer / tema (solo per la schermata)
```

Nuovo sottoprogetto Gradle `qrm-wardrobe`, aggiunto a `settings.gradle`, con la struttura di `qrm-locker`. Il canale di
rete è `optional()` e il mod non registra blocchi, oggetti o tipi di menu, perché un client senza il mod deve poter
entrare. In `neoforge.mods.toml` le dipendenze da `qrm` e `neoforge` sono `required`, come negli altri Module.

Pacchetti (indicativi):

- `dev.qrm.wardrobe.WardrobeMod`: `@Mod`; registra il tipo di punto, il canale, gli eventi del server e dell'API.
- `dev.qrm.wardrobe.logic`: logica pura, senza Minecraft: `OutfitDefinition` (una divisa), `OutfitCatalog` (caricamento e
  controllo dei file), `WardrobeRules` (chi può indossare cosa), `WornOutfits` (stato di chi indossa che cosa).
- `dev.qrm.wardrobe.mc`: caricamento dalla cartella, salvataggio con `ModDataService`, apertura della schermata.
- `dev.qrm.wardrobe.net`: i pacchetti (sezione 5.4).
- `dev.qrm.wardrobe.client`: ricomposizione delle texture, aggancio al rendering, schermata e manichino.

### 4.1 Tipo di punto

```java
new PointType("wardrobe", "qrm_wardrobe.point.wardrobe",
        Set.of(PointOwner.Kind.JOB), true, handler)
```

Registrato nel costruttore del mod con `QRM.get(PointTypeRegistry.class)`. Solo i lavori possono possedere un armadietto
(non le organizzazioni senza lavoro): è una scelta mia, perché il grado minimo ha senso solo dentro un lavoro.
`requiresDuty = true`: il Core verifica proprietario, grado e servizio prima di chiamare il handler.

## 5. Comportamento

### 5.1 La cartella delle divise

Sul server, `config/qrm_wardrobe/outfits/`. Per ogni divisa due file con lo stesso nome (il nome, senza estensione, è
l'identificatore):

```
config/qrm_wardrobe/outfits/agente_pattuglia.json
config/qrm_wardrobe/outfits/agente_pattuglia.png
```

```json
{
  "label": "Agente di pattuglia",
  "job": "police",
  "minGrade": "officer",
  "model": "classic"
}
```

| Campo | Regola |
|---|---|
| `label` | Nome mostrato, da 1 a 40 caratteri. |
| `job` | Id di un lavoro che esiste. |
| `minGrade` | Id di un grado di quel lavoro. Facoltativo: se manca, basta avere il lavoro. |
| `model` | `classic` (braccia da 4 pixel) oppure `slim` (3 pixel). Facoltativo, predefinito `classic`. |

Il PNG è una skin 64x64, formato attuale di Minecraft (non il vecchio 64x32), al massimo 32 KB. Le righe e i pixel della
testa del PNG sono ignorati: al loro posto c'è la testa del giocatore.

Il nome del file deve essere un id valido (come gli id dei lavori). Una divisa non valida (JSON errato, PNG di misura
sbagliata, lavoro o grado inesistente, file mancante) viene saltata e segnalata nel log, con file e motivo, senza
impedire l'avvio.

Le divise si leggono all'avvio e con il comando `/wardrobe reload` (riservato agli operatori), che rimanda il catalogo
ai client.

### 5.2 Chi indossa che cosa

Per ogni Character il server tiene al massimo una divisa, con il suo id. La regola di validità, in `WardrobeRules`:

> La divisa esiste, il Character ha il lavoro della divisa, il suo grado ha livello almeno pari a quello di `minGrade`,
> ed è in servizio.

La regola si ricontrolla quando: il giocatore sceglie una divisa; esce di servizio (`DutyChangedEvent`); cambia lavoro o
grado (`JobChangedEvent`); sceglie o cambia Character (`CharacterSelectedEvent`); entra nel server; il catalogo viene
ricaricato. Se non è più valida, la divisa viene tolta e i client sono avvisati. La divisa scelta si salva con
`ModDataService`, chiave per Character.

All'entrata in servizio la divisa **non** si rimette da sola: il giocatore apre l'armadietto e la sceglie.

### 5.3 L'armadietto

Il clic destro sul punto, per un Character autorizzato, apre la schermata di Wardrobe (se il client ha il mod). A
sinistra la lista: "Abito civile" e le divise del lavoro del punto che il grado permette. A destra il manichino grigio con
la divisa evidenziata. Un pulsante conferma la scelta.

Il server riconferma tutto alla conferma: lavoro del punto, regola di validità, distanza. Il client non decide niente.

Un client senza il mod non ha una schermata: il giocatore riceve un messaggio in chat che dice che serve il mod, e non
cambia nulla. È una scelta mia, perché qui non esiste un equivalente vanilla utile come il baule del Locker.

### 5.4 Rete

Tutti i pacchetti sono opzionali (un client senza il mod non li riceve e non si disconnette).

- **Catalogo (S→C).** All'entrata e dopo ogni reload: per ogni divisa id, etichetta, lavoro, grado minimo, modello, PNG.
  I PNG vanno in pacchetti separati e vengono accettati solo se entro 32 KB; il client ignora quelli che non rispettano
  i limiti. Il tetto è di 128 divise.
- **Indossati (S→C).** L'elenco di chi indossa che cosa (giocatore → id di divisa), mandato a chi entra e poi a tutti a
  ogni cambiamento. Vuoto = skin normale.
- **Apri armadietto (S→C)** con l'elenco delle divise scelte per quel punto e quella divisa indossata ora.
- **Scegli (C→S)** con l'id di una divisa o vuoto per "Abito civile". Il server rifà tutti i controlli.

### 5.5 Rendering (solo client)

Il client ascolta `RenderPlayerEvent.Pre`. Se il giocatore disegnato indossa una divisa, sostituisce la skin dello stato
di rendering (`AvatarRenderState.skin`) con una `PlayerSkin` il cui corpo è la texture ricomposta e il cui modello è quello
della divisa. Il giocatore si riconosce con `AvatarRenderState.id` (ID dell'entità); se non basta, con
`RenderStateExtensions` si può copiare dall'entità i dati che servono. Mantello ed elitre restano quelli originali.

La texture ricomposta nasce dalla skin della divisa, sostituendo con i pixel della skin originale del giocatore due
rettangoli: la testa (x 0–31, y 0–15) e lo strato esterno della testa (x 32–63, y 0–15). Si crea una volta per
coppia giocatore + divisa (`DynamicTexture`, registrata nel `TextureManager`) e si riusa. Va rilasciata quando il
giocatore esce dal rendering per molto tempo, cambia divisa o la skin originale cambia.

Il manichino della schermata è lo stesso meccanismo, applicato a una skin grigia con la divisa sul corpo e una testa
neutra, senza leggere la skin del giocatore.

## 6. Errori e casi limite

| Caso | Comportamento |
|---|---|
| Divisa non valida nella cartella | Saltata, riga nel log con file e motivo. |
| Lavoro o grado della divisa inesistenti | Saltata come sopra. Se un lavoro sparisce dopo, chi la indossava la perde. |
| PNG di misura sbagliata o oltre 32 KB | Saltata. |
| Skin originale non ancora scaricata | Il giocatore appare con la divisa e la testa di Steve o Alex finché non è pronta, poi si aggiorna. |
| Skin originale che cambia mentre si indossa | La texture ricomposta si rifà. |
| Un reload cambia o toglie una divisa indossata | Vale il nuovo catalogo; se la divisa non c'è più o non è più valida, la perde. |
| Due giocatori con la stessa divisa | Condividono la divisa, ma ognuno ha la sua texture ricomposta (la testa è diversa). |
| Giocatore lontano o non caricato | Il client non lo disegna, quindi non compone nulla. Al rientro nel raggio si compone. |
| Il giocatore muore o cambia dimensione | Nessun effetto: la divisa è legata alla Character. |
| Client senza il mod | Tutti con la skin normale; per lui non si apre la schermata. |
| Pacchetto malformato da un client | Scartato e ignorato; la scelta si ricontrolla sempre sul server. |

## 7. Compatibilità

- Un mod di cosmetici che cambia la skin (o un altro mod che sostituisce `PlayerSkin`) può confliggere. Non è coperto.
- Nessuna migrazione: i dati sono nuovi, in `ModDataService`.
- Il Core non cambia. Versione 0.24.0 in `gradle.properties` e `QrmVersion.API_VERSION`.

## 8. Test

Automatici (logica pura, senza Minecraft):

- Lettura della descrizione: campi validi, mancanti, tipi sbagliati, id non validi.
- `WardrobeRules`: lavoro giusto/sbagliato, grado pari/sotto/sopra il minimo, assenza di `minGrade`, fuori servizio.
- Ricomposizione: dato un PNG di divisa e uno di skin, i due rettangoli della testa vengono dal secondo e il resto dal
  primo (su immagini sintetiche 64x64, senza OpenGL).
- Persistenza: salvataggio e rilettura, divisa tolta se non più valida all'entrata.

Manuale, in `docs/manual-tests/wardrobe.md`: due giocatori, un lavoro con due gradi e tre divise; chi può indossare cosa;
la testa; l'uscita di servizio; il cambio di Character; un client senza il mod; un reload che toglie una divisa; la
resa del manichino a scala GUI 2, 3 e 4.

## 9. Da verificare durante l'implementazione

1. Come leggere i pixel della skin originale. Risolto: le skin scaricate sono registrate come `DynamicTexture` e tengono i
   pixel in memoria; quelle predefinite si leggono dalle risorse del gioco. Nessuna cache su disco.
2. Che `RenderPlayerEvent.Pre` permetta davvero di cambiare `renderState.skin` prima del disegno, e che lo strato
   esterno e i layer (armatura, oggetti in mano) restino corretti.
3. `AvatarRenderState.id` identifica il giocatore (risolto). In prima persona si usa `RenderArmEvent`: si annulla il disegno
   del gioco e si disegna lo stesso braccio con la skin della divisa (la forma resta quella del modello originale).
4. Il costo della ricomposizione con molti giocatori e la liberazione delle texture.
5. Il comportamento a server in modalità offline, dove la skin è quella predefinita.

## 10. Dopo l'implementazione

- Pagina wiki `Modules → Wardrobe` e voce nei riferimenti (comandi, nodi, cartella), nello stesso commit della funzione.
- File di esempio in `docs/samples/` e caricamento con `WikiSamplesTest`.
- Aggiornare il manuale di prova e la sezione "Sistemi" della vetrina.
- Poi progettare l'**equipaggiamento tracciato** (armi, armature, rifornimenti): è un progetto a parte, che non dipende da
  questo.
