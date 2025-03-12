package com.dhugs.NoteBlockAPI.model.playmode;

import net.minecraft.util.math.BlockPos;

import com.dhugs.NoteBlockAPI.model.Layer;
import com.dhugs.NoteBlockAPI.model.Note;
import com.dhugs.NoteBlockAPI.model.Song;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Decides how is {@link Note} played to {@link Player}
 */
public abstract class ChannelMode {

	public abstract void play(ServerPlayerEntity player, BlockPos pos, Song song, Layer layer, Note note, float volume, boolean doTranspose);
}
