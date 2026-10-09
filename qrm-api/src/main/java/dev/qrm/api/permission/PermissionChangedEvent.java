package dev.qrm.api.permission;

import dev.qrm.api.event.Event;
import dev.qrm.api.id.CharacterId;

public record PermissionChangedEvent(CharacterId character, String node, PermissionEffect effect) implements Event {}
