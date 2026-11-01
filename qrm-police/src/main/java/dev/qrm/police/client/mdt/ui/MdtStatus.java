package dev.qrm.police.client.mdt.ui;

import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.GREEN;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.ORANGE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.RED;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.YELLOW;

/** Come si chiamano e di che colore sono gli stati degli agenti e le priorità delle chiamate. */
public final class MdtStatus {
    /** Gli stati che l'agente sceglie da solo, nell'ordine dei pulsanti. */
    public static final String[] CHOICES = {"PATROL", "BUSY", "OFF"};
    public static final String[] CHOICE_LABELS = {"Disponibile", "Occupato", "Fuori servizio"};
    public static final int[] CHOICE_COLORS = {GREEN, ORANGE, DIM};

    // Tutti gli stati che un'unità può mostrare, compresi quelli che seguono la chiamata in carico.
    private static final String[] ALL = {"PATROL", "BUSY", "OFF", "EN_ROUTE", "ON_SCENE", "INTERVENING"};
    private static final String[] ALL_SHORT = {"Disponibile", "Occupato", "Fuori serv.", "In viaggio", "Sul posto",
            "In intervento"};
    private static final int[] ALL_COLORS = {GREEN, ORANGE, DIM, BLUE, GREEN, RED};
    /** Posizione in elenco: prima chi è in intervento, in fondo chi è fuori servizio. */
    private static final int[] ORDER = {4, 3, 5, 2, 1, 0};

    /** Priorità P1-P4 (l'indice 0 non è usato). */
    public static final int[] PRIORITY_COLORS = {DIM, RED, ORANGE, YELLOW, BLUE};

    private MdtStatus() {}

    private static int indexOf(String status) {
        for (int i = 0; i < ALL.length; i++) if (ALL[i].equals(status)) return i;
        return 2;
    }

    public static int color(String status) {
        return ALL_COLORS[indexOf(status)];
    }

    public static String shortLabel(String status) {
        return ALL_SHORT[indexOf(status)];
    }

    public static int order(String status) {
        return ORDER[indexOf(status)];
    }
}
