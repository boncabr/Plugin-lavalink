package com.boncabr.lavasonic;

import dev.arbjerg.lavalink.api.PluginEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** LavaSonic plugin lifecycle bean. */
@Service
public final class LavaSonicPlugin extends PluginEventHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LavaSonicPlugin.class);

    public LavaSonicPlugin() {
        LOG.info("LavaSonic 0.1.0 loaded - Lavalink v4 adaptive audio integrity filter");
    }
}