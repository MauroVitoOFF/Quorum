package dev.qrm.api.org;

import dev.qrm.api.event.Event;
import dev.qrm.api.id.CharacterId;

/** @param before null se non era membro; @param after null se è uscito */
public record MembershipChangedEvent(String orgId, CharacterId character, Membership before, Membership after)
        implements Event {}
