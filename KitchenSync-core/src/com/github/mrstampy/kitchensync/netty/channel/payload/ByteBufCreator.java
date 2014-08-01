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
package com.github.mrstampy.kitchensync.netty.channel.payload;

import io.netty.buffer.ByteBuf;

import java.net.InetSocketAddress;

import com.github.mrstampy.kitchensync.netty.channel.AbstractKiSyChannel;

/**
 * Implementations determine how to create a ByteBuf for the given message and
 * intended recipient (if applicable).
 * 
 * @see AbstractKiSyChannel#setByteBufCreator(ByteBufCreator)
 * @see AbstractKiSyChannel#usesDefaultByteBufCreators()
 */
public interface ByteBufCreator {

	/**
	 * Creates the ByteBuf from the given message and intended recipient (if applicable).
	 *
	 * @param <MSG>
	 *          the generic type
	 * @param message
	 *          the message
	 * @param recipient
	 *          the recipient
	 * @return the byte buf
	 */
	<MSG extends Object> ByteBuf createByteBuf(MSG message, InetSocketAddress recipient);
}
