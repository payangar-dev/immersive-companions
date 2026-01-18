package com.payangar.immersivecompanions.entity;

import net.minecraft.util.RandomSource;

/**
 * Combat type determines the companion's fighting style and equipped weapon.
 * MELEE companions use swords or axes, RANGED companions use bows or crossbows.
 */
public enum CompanionType {
    MELEE,
    RANGED;

    public static CompanionType random(RandomSource random) {
        return random.nextBoolean() ? MELEE : RANGED;
    }

    public boolean isRanged() {
        return this == RANGED;
    }
}
