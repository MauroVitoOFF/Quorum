package dev.qrm.api.permission;

/** Effetto di un override. {@code CLEARED} compare solo negli eventi (override rimosso), mai nel database. */
public enum PermissionEffect { GRANT, DENY, CLEARED }
