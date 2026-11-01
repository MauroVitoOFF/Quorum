package dev.qrm.police.client.mdt.dispatch;

import dev.qrm.police.client.mdt.MdtHost;
import dev.qrm.police.client.mdt.MdtPage;

/** Una vista del Dispatch (elenco, unità, storico, dettaglio, nuova chiamata): lo stato condiviso sta nella pagina. */
abstract class DispatchView extends MdtPage {
    protected final DispatchPage owner;

    DispatchView(MdtHost host, DispatchPage owner) {
        super(host);
        this.owner = owner;
    }
}
