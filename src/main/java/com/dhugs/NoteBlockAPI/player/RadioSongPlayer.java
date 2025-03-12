package com.dhugs.NoteBlockAPI.player;


import net.minecraft.util.math.BlockPos;

import com.dhugs.NoteBlockAPI.NoteBlockAPI;
import com.dhugs.NoteBlockAPI.model.Layer;
import com.dhugs.NoteBlockAPI.model.Note;
import com.dhugs.NoteBlockAPI.model.Playlist;
import com.dhugs.NoteBlockAPI.model.Song;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * SongPlayer playing to everyone added to it no matter where they are
 */
public class RadioSongPlayer extends SongPlayer {

	public RadioSongPlayer(Song song) {
		super(song);
	}

	public RadioSongPlayer(Playlist playlist) {
		super(playlist);
	}

	@Override
	public void playTick(ServerPlayerEntity player, int tick) {
		byte playerVolume = NoteBlockAPI.getPlayerVolume(player);

		for(Layer layer : song.getLayerHashMap().values()) {
			Note note = layer.getNote(tick);
			if(note == null) {
				continue;
			}

			float volume = (layer.getVolume() * (int) this.volume * (int) playerVolume * note.getVelocity()) / 100_00_00_00F;
			var eyePos = player.getEyePos();
			channelMode.play(player, new BlockPos((int) eyePos.getX(), (int) eyePos.getY(), (int) eyePos.getZ()), song, layer, note, volume, !enable10Octave);
		}
	}
}
