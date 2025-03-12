package com.dhugs.thefallen;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dhugs.NoteBlockAPI.NoteBlockAPI;
import com.dhugs.NoteBlockAPI.model.Song;
import com.dhugs.NoteBlockAPI.utils.NBSDecoder;

import java.io.InputStream;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import net.minecraft.entity.Entity;

public class TheFallenMod implements ModInitializer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static Song theFallenSong;
    private static NoteBlockAPI notaInstance;
    private static final Map<ItemDisplayEntity, Long> activeDisplayEntities = new ConcurrentHashMap<>();

    public static Song getTheFallenSong() {
        return theFallenSong;
    }

    public static void trackDisplayEntity(ItemDisplayEntity entity) {
        activeDisplayEntities.put(entity, System.currentTimeMillis());
    }

    public static void untrackDisplayEntity(ItemDisplayEntity entity) {
        activeDisplayEntities.remove(entity);
    }

    @Override
    public void onInitialize() {
        // Initialize Nota first
        notaInstance = new NoteBlockAPI();
        notaInstance.onInitialize();
        
        // Add server shutdown handler
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // Clean up any remaining display entities
            activeDisplayEntities.keySet().forEach(entity -> {
                if (entity != null && entity.isAlive()) {
                    entity.remove(Entity.RemovalReason.DISCARDED);
                }
            });
            activeDisplayEntities.clear();
        });

        // Load the NBS song from resources
        try {
            InputStream songStream = TheFallenMod.class.getClassLoader()
                .getResourceAsStream("assets/thefallen/songs/the_fallen.nbs");
            
            if (songStream != null) {
                theFallenSong = NBSDecoder.parse(songStream);
                LOGGER.info("Successfully loaded the_fallen.nbs");
            } else {
                LOGGER.error("Could not find the_fallen.nbs in mod resources");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load the_fallen.nbs: " + e.getMessage());
            e.printStackTrace();
            theFallenSong = null;
        }
    }
}
