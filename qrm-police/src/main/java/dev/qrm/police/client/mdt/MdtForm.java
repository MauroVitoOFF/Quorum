package dev.qrm.police.client.mdt;

import java.util.function.Consumer;

/** Un modulo testuale sopra la pagina: titolo, campi e cosa fare al salvataggio. */
public record MdtForm(String title, String[] labels, String[] values, Consumer<String[]> submit) {}
