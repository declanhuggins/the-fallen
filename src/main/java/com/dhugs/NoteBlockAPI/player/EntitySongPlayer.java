package com.dhugs.NoteBlockAPI.player;

import com.dhugs.NoteBlockAPI.NoteBlockAPI;
import com.dhugs.NoteBlockAPI.model.Layer;
import com.dhugs.NoteBlockAPI.model.Note;
import com.dhugs.NoteBlockAPI.model.Playlist;
import com.dhugs.NoteBlockAPI.model.Song;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

@SuppressWarnings("unused")
public class EntitySongPlayer extends RangeSongPlayer {

	private Entity entity;

	public EntitySongPlayer(Song song) {
		super(song);
	}

	public EntitySongPlayer(Playlist playlist) {
		super(playlist);
	}

	/**
	 * Returns true if the Player is able to hear the current {@link EntitySongPlayer}
	 *
	 * @param player in range
	 * @return ability to hear the current {@link EntitySongPlayer}
	 */
	@Override
	public boolean isInRange(ServerPlayerEntity player) {
		return player.getBlockPos().isWithinDistance(entity.getBlockPos(), getDistance());
	}

	/**
	 * Set entity associated with this {@link EntitySongPlayer}
	 *
	 * @param entity entity
	 */
	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	/**
	 * Get {@link Entity} associated with this {@link EntitySongPlayer}
	 *
	 * @return entity
	 */
	public Entity getEntity() {
		return entity;
	}

	@Override
	public void playTick(ServerPlayerEntity player, int tick) {
		if(!entity.isAlive()) {
			if(autoDestroy) {
				destroy();
			}
			else {
				setPlaying(false);
			}
		}
		if(!player.getWorld().getDimension().equals(entity.getWorld().getDimension())) {
			return; // not in same world
		}

		byte playerVolume = NoteBlockAPI.getPlayerVolume(player);

		for(Layer layer : song.getLayerHashMap().values()) {
			Note note = layer.getNote(tick);
			if(note == null) continue;

			float volume = ((layer.getVolume() * (int) this.volume * (int) playerVolume * note.getVelocity()) / 100_00_00_00F)
					* ((1F / 16F) * getDistance());

			if(isInRange(player)) {
				playerList.put(player.getUuid(), true);
				channelMode.play(player, entity.getBlockPos(), song, layer, note, volume, !enable10Octave);
			}
			else {
				playerList.put(player.getUuid(), false);
			}
		}
	}
}
