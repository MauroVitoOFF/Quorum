package dev.qrm.police.weapon.client;

import dev.qrm.police.weapon.logic.WeaponDef;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Quale arma è questo oggetto: la definizione associata al suo id di registro, se ce n'è una. */
public final class HeldWeapon {
    private HeldWeapon() {}

    public static Optional<WeaponDef> of(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        return WeaponDef.forItem(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
}
