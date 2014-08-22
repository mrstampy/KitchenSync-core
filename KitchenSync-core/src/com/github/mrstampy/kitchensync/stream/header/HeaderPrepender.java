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
 * The Interface HeaderPrepender is used by {@link AbstractStreamer} to prepend
 * a header to a message.
 * 
 * @see AbstractStreamer#setHeaderPrepender(HeaderPrepender)
 */
public interface HeaderPrepender {

	/**
	 * Size in bytes of the header (fixed length).
	 *
	 * @return the int
	 */
	int sizeInBytes();

	/**
	 * Prepend a header to the message. The {@link Streamer} is provided for any
	 * required context.
	 *
	 * @param streamer
	 *          the streamer
	 * @param message
	 *          the message
	 * @return the byte[] with header
	 * @see Streamer#getSequence()
	 */
	byte[] prependHeader(Streamer<?> streamer, byte[] message);

	/**
	 * Reset the state of the HeaderPrepender, called on {@link AbstractStreamer}
	 * initialization.
	 */
	void reset();

}
