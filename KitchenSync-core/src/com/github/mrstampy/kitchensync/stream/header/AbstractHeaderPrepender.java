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
import io.netty.buffer.Unpooled;

import com.github.mrstampy.kitchensync.stream.AbstractStreamer;
import com.github.mrstampy.kitchensync.stream.Streamer;

/**
 * Convenience abstract implementation of a {@link HeaderPrepender}.
 */
public abstract class AbstractHeaderPrepender implements HeaderPrepender {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.header.HeaderPrepender#prependHeader
	 * (com.github.mrstampy.kitchensync.stream.Streamer, byte[])
	 */
	@Override
	public final byte[] prependHeader(Streamer<?> streamer, byte[] message) {
		ByteBuf buf = Unpooled.buffer(sizeInBytes() + message.length);
		addHeaderDetail(streamer, buf);
		buf.writeBytes(message);

		return buf.array();
	}

	/**
	 * Implement to add just the header detail to the supplied {@link ByteBuf}.
	 *
	 * @param streamer
	 *          the streamer
	 * @param buf
	 *          the buf
	 */
	protected abstract void addHeaderDetail(Streamer<?> streamer, ByteBuf buf);

	/**
	 * Blank implementation, override as necessary. Invoked when the
	 * {@link AbstractStreamer} is initialized
	 */
	@Override
	public void reset() {

	}

}
