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

import com.github.mrstampy.kitchensync.stream.Streamer;

/**
 * Implementations are used by {@link Streamer}s when
 * {@link Streamer#isEomOnFinish()}.
 * 
 * @see Streamer#setFooter(Footer)
 */
public interface Footer {

	/**
	 * Return true if the message has been created from {@link #createFooter()}.
	 * 
	 * @param message
	 *          to check if created by {@link #createFooter()}
	 * @return true if the message was created by {@link #createFooter()}
	 */
	boolean isFooter(byte[] message);

	/**
	 * Creates the footer.
	 *
	 * @return the byte[]
	 */
	byte[] createFooter();

	/**
	 * Reset, invoked on {@link Streamer} initialization.
	 */
	void reset();
}
