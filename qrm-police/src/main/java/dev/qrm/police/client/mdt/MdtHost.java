package dev.qrm.police.client.mdt;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

/** Ciò che una pagina può chiedere alla schermata che la ospita. Le pagine non conoscono altro. */
public interface MdtHost {
    Font font();

    /** Il nome dell'agente che usa il terminale. */
    String officer();

    /** L'ultimo stato ricevuto dal server. */
    JsonObject data();

    /** L'area utile delle pagine. */
    int cx();

    int cy();

    int cw();

    int ch();

    <T extends GuiEventListener & Renderable & NarratableEntry> T add(T widget);

    void rebuild();

    void focus(GuiEventListener widget);

    void send(String kind, String... args);

    /** Mostra un modulo al posto della pagina, o lo chiude se è null. Non ricostruisce da solo. */
    void setForm(MdtForm form);

    /** Passa alla pagina indicata e la restituisce, per darle istruzioni. */
    <P extends MdtPage> P open(Class<P> page);
}
