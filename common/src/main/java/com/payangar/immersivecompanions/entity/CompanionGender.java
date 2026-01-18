package com.payangar.immersivecompanions.entity;

import net.minecraft.util.RandomSource;

/**
 * Gender determines the player model type used for rendering.
 * MALE uses the Steve (normal) model, FEMALE uses the Alex (slim) model.
 */
public enum CompanionGender {
    MALE,
    FEMALE;

    public static CompanionGender random(RandomSource random) {
        return random.nextBoolean() ? MALE : FEMALE;
    }

    public boolean isSlim() {
        return this == FEMALE;
    }
}
