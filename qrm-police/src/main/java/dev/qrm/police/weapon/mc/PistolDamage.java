package dev.qrm.police.weapon.mc;

import dev.qrm.police.mc.PoliceMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

/** Tipo di danno dei colpi di pistola ({@code qrm_police:pistol}, nel tag {@code minecraft:is_projectile}). */
public final class PistolDamage {
    private PistolDamage() {}

    public static final ResourceKey<DamageType> PISTOL =
            ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(PoliceMod.MOD_ID, "pistol"));

    /** Danno di un colpo sparato da {@code shooter}: è lui sia la causa diretta sia il responsabile. */
    public static DamageSource source(ServerLevel level, Player shooter) {
        return level.damageSources().source(PISTOL, shooter, shooter);
    }
}
