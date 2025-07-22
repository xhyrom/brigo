package dev.xhyrom.brigo.platform.services;

import java.nio.file.Path;

public interface Agnos {
    boolean isClient();
    Path configDir();
}
