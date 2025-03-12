package com.dhugs.NoteBlockAPI.player;

import net.minecraft.util.math.BlockPos;

import com.dhugs.NoteBlockAPI.NoteBlockAPI;
import com.dhugs.NoteBlockAPI.model.Layer;
import com.dhugs.NoteBlockAPI.model.Note;
import com.dhugs.NoteBlockAPI.model.Playlist;
import com.dhugs.NoteBlockAPI.model.Song;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

/**
 * SongPlayer created at a specified BlockPos
 */
public class PositionSongPlayer extends RangeSongPlayer {
	private BlockPos pos;
	World world;

	public PositionSongPlayer(Song song, World world) {
		super(song);
		this.world = world;
	}

	public PositionSongPlayer(Playlist playlist, World world) {
		super(playlist);
		this.world = world;
	}

	/**
	 * Gets location on which is the PositionSongPlayer playing
	 *
	 * @return {@link BlockPos}
	 */
	public BlockPos getBlockPos() {
		return this.pos;
	}

	/**
	 * Sets location on which is the PositionSongPlayer playing
	 */
	public void setBlockPos(BlockPos pos) {
		this.pos = pos;
	}

	@Override
	public void playTick(ServerPlayerEntity player, int tick) {
		if(!player.getWorld().getDimension().equals(world.getDimension())) {
			return; // not in same world
		}

		byte playerVolume = NoteBlockAPI.getPlayerVolume(player);

		for(Layer layer : song.getLayerHashMap().values()) {
			Note note = layer.getNote(tick);
			if(note == null) continue;

			float volume = ((layer.getVolume() * (int) this.volume * (int) playerVolume * note.getVelocity()) / 100_00_00_00F)
					* ((1F / 16F) * getDistance());

			if(isInRange(player)) {
				this.playerList.put(player.getUuid(), true);
				this.channelMode.play(player, pos, song, layer, note, volume, !enable10Octave);
			}
			else {
				this.playerList.put(player.getUuid(), false);
			}
		}
	}

	/**
	 * Returns true if the Player is able to hear the current PositionSongPlayer
	 *
	 * @param player in range
	 * @return ability to hear the current PositionSongPlayer
	 */
	@Override
	public boolean isInRange(ServerPlayerEntity player) {
		return player.getBlockPos().isWithinDistance(pos, getDistance());
	}
}
