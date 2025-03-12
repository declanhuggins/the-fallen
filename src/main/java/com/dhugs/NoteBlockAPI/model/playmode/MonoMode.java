package com.dhugs.NoteBlockAPI.model.playmode;

import net.minecraft.util.math.BlockPos;
import net.minecraft.sound.SoundCategory;

import com.dhugs.NoteBlockAPI.model.Layer;
import com.dhugs.NoteBlockAPI.model.Note;
import com.dhugs.NoteBlockAPI.model.Song;
import com.dhugs.NoteBlockAPI.utils.InstrumentUtils;
import com.dhugs.NoteBlockAPI.utils.NoteUtils;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * {@link Note} is played inside of {@link Player}'s head.
 */
public class MonoMode extends ChannelMode {

	@Override
	public void play(ServerPlayerEntity player, BlockPos pos, Song song, Layer layer, Note note, float volume, boolean doTranspose) {
		float pitch;
		if(doTranspose) {
			pitch = NoteUtils.getPitchTransposed(note);
		}
		else {
			pitch = NoteUtils.getPitchInOctave(note);
		}
		player.playSoundToPlayer(InstrumentUtils.getInstrument(note.getInstrument()), SoundCategory.RECORDS, volume, pitch);
	}
}
