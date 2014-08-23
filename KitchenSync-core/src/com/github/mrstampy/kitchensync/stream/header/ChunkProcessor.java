/*
 * KitchenSync-core Java Library Copyright (C) 2014 Burton Alexander
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * 
 */
package com.github.mrstampy.kitchensync.stream.header;

import com.github.mrstampy.kitchensync.stream.AbstractStreamer;
import com.github.mrstampy.kitchensync.stream.Streamer;

/**
 * The Interface ChunkProcessor is used by {@link AbstractStreamer} to process a
 * chunk prior to sending ie. add a header, encrypt etc. Implementations are
 * invoked when {@link Streamer#isProcessChunk()}, immediately after the latest
 * chunk of the message has been acquired.<br>
 * <br>
 * 
 * @see AbstractStreamer#setChunkProcessor(ChunkProcessor)
 */
public interface ChunkProcessor {

	/**
	 * Size in bytes of any additional meta information added to the message
	 * (fixed length). The value returned will be subtracted from
	 * {@link Streamer#getChunkSize()} when determining the size of chunk to
	 * obtain.
	 *
	 * @return the size
	 */
	int sizeInBytes();

	/**
	 * Process the message. The {@link Streamer} is provided for any required
	 * context.
	 *
	 * @param streamer
	 *          the streamer
	 * @param chunk
	 *          the chunk
	 * @return the byte[] with header
	 * @see Streamer#getSequence()
	 */
	byte[] process(Streamer<?> streamer, byte[] chunk);

	/**
	 * Reset the state of the ChunkProcessor, called on {@link AbstractStreamer}
	 * initialization.
	 */
	void reset();

}
