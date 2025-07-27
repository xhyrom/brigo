package dev.xhyrom.brigo.platform;

import dev.xhyrom.brigo.BrigoClient;
import dev.xhyrom.brigo.platform.services.Agnos;

import java.util.ServiceLoader;

public class Services {
    public static final Agnos AGNOS = load(Agnos.class);

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz)
                .iterator().next();

        if (loadedService == null) {
            throw new NullPointerException("Failed to load service for " + clazz.getName());
        }

        BrigoClient.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
