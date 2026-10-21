package dev.qrm.police.weapon.logic.anim;

import java.util.List;

/** Evento di una clip (suono, punto di commit, lampo...) a un istante in secondi. */
public record AnimEvent(double t, String type, List<String> args) {
    public AnimEvent {
        args = List.copyOf(args);
    }
}
