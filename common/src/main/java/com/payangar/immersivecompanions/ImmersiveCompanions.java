package com.payangar.immersivecompanions;

import com.payangar.immersivecompanions.platform.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImmersiveCompanions {
    public static final String MOD_ID = "immersivecompanions";
    public static final String MOD_NAME = "Immersive Companions";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        LOGGER.info("Initializing {} on {}", MOD_NAME, Services.get().getLoaderName());
    }
}
