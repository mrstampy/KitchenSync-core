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
package com.github.mrstampy.kitchensync.stream.footer;

import java.util.Arrays;

import com.github.mrstampy.kitchensync.stream.AbstractStreamer;
import com.github.mrstampy.kitchensync.stream.Streamer;

/**
 * The Class EndOfMessageFooter creates a message consisting of
 * {@value #END_OF_MESSAGE}. It is the default {@link Footer} used by
 * {@link AbstractStreamer} implementations.
 * 
 * @see Streamer#isEomOnFinish()
 */
public class EndOfMessageFooter implements Footer {

	/** The Constant END_OF_MESSAGE. */
	public static final String END_OF_MESSAGE = "EOM:";

	private static final byte[] END_OF_MESSAGE_BYTES = END_OF_MESSAGE.getBytes();
	private static final int LENGTH = END_OF_MESSAGE_BYTES.length;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.footer.Footer#createFooter()
	 */
	@Override
	public byte[] createFooter() {
		return END_OF_MESSAGE_BYTES;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.footer.Footer#reset()
	 */
	@Override
	public void reset() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.footer.Footer#isFooter(byte[])
	 */
	@Override
	public boolean isFooter(byte[] message) {
		if (message == null || message.length < LENGTH) return false;

		byte[] b = Arrays.copyOfRange(message, 0, LENGTH);

		return Arrays.equals(b, END_OF_MESSAGE_BYTES);
	}

}
