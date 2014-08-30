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

import io.netty.buffer.ByteBuf;

import com.github.mrstampy.kitchensync.stream.Streamer;

/**
 * Prepends the sequence header to the message. This is the default
 * {@link ChunkProcessor} when {@link Streamer#isProcessChunk()}.
 * 
 * @see SequenceHeader
 * @see Streamer#setChunkProcessor(ChunkProcessor)
 */
public class SequenceHeaderPrepender extends AbstractChunkProcessor {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.header.ChunkProcesor#sizeInBytes()
	 */
	@Override
	public int sizeInBytes(Streamer<?> streamer) {
		return SequenceHeader.HEADER_LENGTH;
	}

	/*
	 * (non-Javadoc)
	 */
	@Override
	protected ByteBuf processImpl(Streamer<?> streamer, byte[] message) {
		int headerLength = sizeInBytes(streamer) + message.length;
		ByteBuf buf = createByteBuf(headerLength);

		buf.writeBytes(SequenceHeader.SEQUENCE_HEADER_BYTES);
		buf.writeLong(streamer.getSequence());
		appendToHeader(streamer, buf, headerLength);
		buf.writeBytes(message);

		return buf;
	}

	/**
	 * Hook for subclasses to append data to the header if required. Default impl
	 * does nothing.
	 * 
	 * @param streamer
	 *          supplied for state information
	 * @param buf
	 *          the buffer containing the default header information
	 * @param headerLength
	 *          the total length this header is expected to be
	 * @see #sizeInBytes(Streamer)
	 */
	protected void appendToHeader(Streamer<?> streamer, ByteBuf buf, int headerLength) {
	}

}
