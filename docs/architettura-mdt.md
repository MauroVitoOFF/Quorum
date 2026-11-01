# Architettura dell'MDT (client)

L'MDT è un tablet disegnato in codice (nessuna texture). Il codice è in
`qrm-police/src/main/java/dev/qrm/police/client/mdt/`. Il server decide tutto: il client mostra lo stato ricevuto e
manda azioni (`PolicePayloads.Action`).

## Struttura

```
client/
  ClientPolice, PoliceClient, PoliceClientBridge, PoliceHud   rete, waypoint, note locali, HUD
  mdt/
    PoliceMdtScreen     guscio: ciclo di vita, menu laterale, modulo, input; implementa MdtHost
    MdtChrome           cornice: scocca, barra di stato, intestazione, riflesso
    MdtHost             ciò che una pagina può chiedere alla schermata
    MdtPage             base di ogni pagina e vista: misure, dati, helper di disegno
    MdtForm, MdtLayout  modulo testuale; misure fisse del tablet
    ui/                 MdtWidgets (Btn, Field, colori), MdtStatus (stati e priorità), MdtText (formati)
    dashboard/          DashboardPage
    citizens/           CitizensPage + FineWizard, ArrestWizard (procedure guidate)
    dispatch/           DispatchPage (instradatore) + CallListView, UnitsView, HistoryView, CallDetailView,
                        NewCallView; CallFormat (lettura delle chiamate), CallRow (riga condivisa con la Dashboard)
    glossary/           GlossaryPage
```

## Regole

- Una pagina estende `MdtPage` e implementa `build()` (widget) e `render(g)` (sfondi e testo). Conosce la schermata solo
  tramite `MdtHost`; non vede le altre pagine, salvo `host.open(Altra.class)` per navigare.
- Lo stato ricevuto è in `data` (JSON del server). Lo stato di interfaccia (scorrimento, ricerca, vista aperta) è della
  pagina che lo usa e non esce da lì.
- Le pagine con più viste (Dispatch, Cittadini) hanno un instradatore che tiene lo stato condiviso e viste che lo
  leggono attraverso `owner`.
- Ciò che serve a più pagine sta in `ui/` (stati, colori, formati) o in una classe di supporto del pacchetto che lo
  possiede (`CallRow` per la riga di chiamata).
- Niente logica di gioco nel client: le decisioni (permessi, assegnazione unica di una chiamata, filtri dello storico)
  le prende `PoliceServer`.

## Aggiungere una pagina

1. Creare `client/mdt/<nome>/<Nome>Page.java` che estende `MdtPage`.
2. Registrarla in `PoliceMdtScreen` con `addPage("Titolo", new NomePage(this))`.
3. Se serve un'azione nuova, aggiungerla in `PoliceServer.onAction` (con i controlli sui permessi) e in
   `docs/manual-tests/police.md`.

## Cicli

- `sync()` aggiorna font, dati e misure; la schermata lo chiama prima di costruire, disegnare e di ogni tick.
- `onEnter()` entrando da un'altra scheda, `onReselect()` con un secondo clic sulla scheda già aperta.
- `onReply(kind, righe)` per le risposte del server a una richiesta (posizioni delle unità, storico); ritorna true se la
  pagina va ricostruita.
- `onScroll(...)` instrada la rotella alla vista visibile; false se la pagina vuole lasciarla a un suo widget.
