package dev.qrm.police.client.mdt.ui;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formati di data, ora e denaro condivisi dalle pagine. */
public final class MdtText {
    public static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm").withZone(ZoneId.systemDefault());
    public static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private MdtText() {}

    public static String money(long v) {
        return "$" + String.format(Locale.ROOT, "%,d", v);
    }
}
