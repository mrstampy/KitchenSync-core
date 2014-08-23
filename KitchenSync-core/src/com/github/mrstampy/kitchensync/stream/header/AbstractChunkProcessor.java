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

import static io.netty.buffer.Unpooled.buffer;
import io.netty.buffer.ByteBuf;

import com.github.mrstampy.kitchensync.stream.AbstractStreamer;
import com.github.mrstampy.kitchensync.stream.Streamer;

/**
 * Convenience abstract implementation of a {@link ChunkProcessor}.
 */
public abstract class AbstractChunkProcessor implements ChunkProcessor {

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.header.ChunkProcessor#process
	 * (com.github.mrstampy.kitchensync.stream.Streamer, byte[])
	 */
	@Override
	public final byte[] process(Streamer<?> streamer, byte[] message) {
		ByteBuf buf = processImpl(streamer, message);

		return buf.array();
	}

	/**
	 * Implement to add just the header detail to the supplied {@link ByteBuf}.
	 *
	 * @param streamer
	 *          the streamer
	 * @param message
	 *          the message
	 * @return the ByteBuf populated with the transformed message.
	 */
	protected abstract ByteBuf processImpl(Streamer<?> streamer, byte[] message);

	/**
	 * Convenience method to return a ByteBuf initialized to the specified size.
	 * 
	 * @param size
	 *          of the ByteBuf
	 * @return ByteBuf of size 'size'
	 */
	protected ByteBuf createByteBuf(int size) {
		return buffer(size);
	}

	/**
	 * Blank implementation, override as necessary. Invoked when the
	 * {@link AbstractStreamer} is initialized
	 */
	@Override
	public void reset() {

	}

}
