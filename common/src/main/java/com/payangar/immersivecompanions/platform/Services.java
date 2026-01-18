package com.payangar.immersivecompanions.platform;

import java.nio.file.Path;

public interface Services {
    Path getConfigDir();
    boolean isDevelopmentEnvironment();
    String getLoaderName();

    static Services get() {
        return Holder.INSTANCE;
    }

    static void init(Services services) {
        if (Holder.INSTANCE != null) {
            throw new IllegalStateException("Services already initialized!");
        }
        Holder.INSTANCE = services;
    }

    class Holder {
        private static Services INSTANCE;
    }
}
