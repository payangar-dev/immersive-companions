package com.payangar.immersivecompanions.compat.hardcorerevival;

import com.payangar.immersivecompanions.ImmersiveCompanions;

/**
 * Fabric-specific initialization for Hardcore Revival compatibility.
 *
 * This class is only loaded when Hardcore Revival is present, using isolated
 * class loading to prevent NoClassDefFoundError when Hardcore Revival is absent.
 */
public class HardcoreRevivalCompatFabric {

    /**
     * Initializes the Hardcore Revival compatibility layer.
     * Call this from mod initialization when Hardcore Revival is detected.
     */
    public static void init() {
        ImmersiveCompanions.LOGGER.info("Hardcore Revival detected - enabling companion revival feature");
        HardcoreRevivalCompat.setModLoaded(true);
    }
}
