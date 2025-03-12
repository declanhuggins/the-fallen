package com.dhugs.NoteBlockAPI;

import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dhugs.NoteBlockAPI.player.SongPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main class, contains methods for playing and adjusting songs for players.
 */
@SuppressWarnings("unused")
public class NoteBlockAPI implements ModInitializer {
	public static final String MOD_ID = "nota";
	public static final Logger LOGGER = LoggerFactory.getLogger("Nota");

	private static NoteBlockAPI instance;
	public MinecraftServer server;

	Map<UUID, ArrayList<SongPlayer>> playingSongs = new ConcurrentHashMap<>();
	Map<UUID, Byte> playerVolume = new ConcurrentHashMap<>();

	private boolean disabling = false;

	/**
	 * Returns true if a Player is currently receiving a song
	 *
	 * @param player player entity
	 * @return is receiving a song
	 */
	public static boolean isReceivingSong(ServerPlayerEntity player) {
		return isReceivingSong(player.getUuid());
	}

	/**
	 * Returns true if a Player with specified UUID is currently receiving a song
	 *
	 * @param playerUuid player's uuid
	 * @return is receiving a song
	 */
	public static boolean isReceivingSong(UUID playerUuid) {
		ArrayList<SongPlayer> songs = instance.playingSongs.get(playerUuid);
		return (songs != null && !songs.isEmpty());
	}

	/**
	 * Stops the song for a Player
	 *
	 * @param player player entity
	 */
	public static void stopPlaying(ServerPlayerEntity player) {
		stopPlaying(player.getUuid());
	}

	/**
	 * Stops the song for a Player
	 *
	 * @param playerUuid player's uuid
	 */
	public static void stopPlaying(UUID playerUuid) {
		ArrayList<SongPlayer> songs = instance.playingSongs.get(playerUuid);
		if(songs == null) {
			return;
		}
		for(SongPlayer songPlayer : songs) {
			songPlayer.removePlayer(playerUuid);
		}
	}

	/**
	 * Sets the volume for a given Player
	 *
	 * @param player player entity
	 * @param volume volume
	 */
	public static void setPlayerVolume(ServerPlayerEntity player, byte volume) {
		setPlayerVolume(player.getUuid(), volume);
	}

	/**
	 * Sets the volume for a given Player
	 *
	 * @param playerUuid player's uuid
	 * @param volume volume
	 */
	public static void setPlayerVolume(UUID playerUuid, byte volume) {
		instance.playerVolume.put(playerUuid, volume);
	}

	/**
	 * Gets the volume for a given Player
	 *
	 * @param player player entity
	 * @return volume (byte)
	 */
	public static byte getPlayerVolume(ServerPlayerEntity player) {
		return getPlayerVolume(player.getUuid());
	}

	/**
	 * Gets the volume for a given Player
	 *
	 * @param playerUuid player's uuid
	 * @return volume (byte)
	 */
	public static byte getPlayerVolume(UUID playerUuid) {
		if(instance.playerVolume.containsKey(playerUuid)) {
			return instance.playerVolume.get(playerUuid);
		}
		else {
			instance.playerVolume.put(playerUuid, (byte) 100);
			return 100;
		}
	}

	public static ArrayList<SongPlayer> getSongPlayersByPlayer(ServerPlayerEntity player) {
		return getSongPlayersByPlayer(player.getUuid());
	}

	public static ArrayList<SongPlayer> getSongPlayersByPlayer(UUID playerUuid) {
		return instance.playingSongs.get(playerUuid);
	}

	public static void setSongPlayersByPlayer(ServerPlayerEntity player, ArrayList<SongPlayer> songs) {
		setSongPlayersByPlayer(player.getUuid(), songs);
	}

	public static void setSongPlayersByPlayer(UUID playerUuid, ArrayList<SongPlayer> songs) {
		instance.playingSongs.put(playerUuid, songs);
	}

	public boolean isDisabling() {
		return this.disabling;
	}

	public static NoteBlockAPI getAPI() {
		return NoteBlockAPI.instance;
	}

	public MinecraftServer getServer() {
		return this.server;
	}

	@Override
	public void onInitialize() {
		NoteBlockAPI.instance = this;
		ServerLifecycleEvents.SERVER_STARTED.register(server -> NoteBlockAPI.getAPI().server = server);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> NoteBlockAPI.getAPI().disabling = true);
	}
}
