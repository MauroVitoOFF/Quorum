package dev.qrm.police.weapon.logic;

/** Esito di un tentativo di sparo: se il colpo è partito e il nuovo stato dell'arma. */
public record FireResult(boolean fired, WeaponState state) {}
