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
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

/**
 * Converts a String to a ByteBuf object.
 */
public class StringByteBufCreator implements ByteBufCreator {

	/* (non-Javadoc)
	 * @see com.github.mrstampy.kitchensync.netty.channel.payload.ByteBufCreator#createBytes(java.lang.Object, java.net.InetSocketAddress)
	 */
	@Override
	public <MSG> ByteBuf createByteBuf(MSG message, InetSocketAddress recipient) {
		return Unpooled.copiedBuffer((String)message, CharsetUtil.UTF_8);
	}

}
