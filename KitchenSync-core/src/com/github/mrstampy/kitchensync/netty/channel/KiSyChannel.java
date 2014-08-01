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
package com.github.mrstampy.kitchensync.netty.channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.DatagramChannel;

import java.net.InetSocketAddress;

import com.github.mrstampy.kitchensync.netty.channel.payload.ByteArrayByteBufCreator;
import com.github.mrstampy.kitchensync.netty.channel.payload.ByteBufCreator;
import com.github.mrstampy.kitchensync.netty.channel.payload.StringByteBufCreator;

/**
 * KiSyChannel implementations interface the <a href="http://netty.io">Netty</a>
 * specifics, allowing ease of socket creation and use.
 */
public interface KiSyChannel {

	/**
	 * Checks if the underlying channel is active.
	 *
	 * @return true, if checks if is active
	 */
	boolean isActive();

	/**
	 * Returns true if the implementation binds to a multicast address.
	 *
	 * @return true, if checks if is multicast channel
	 */
	boolean isMulticastChannel();

	/**
	 * Returns true if the implementation only binds to a specific port.
	 *
	 * @return true, if checks if is port specific channel
	 */
	boolean isPortSpecificChannel();

	/**
	 * Bind to the specified port.
	 *
	 * @param port
	 *          the port
	 */
	void bind(int port);

	/**
	 * Bind to the next available port.
	 */
	void bind();

	/**
	 * Send the specified message to the specified address.
	 *
	 * @param <MSG>
	 *          the generic type
	 * @param message
	 *          the message
	 * @param address
	 *          the address
	 * @return the channel future
	 */
	<MSG extends Object> ChannelFuture send(MSG message, InetSocketAddress address);

	/**
	 * Close the channel.
	 *
	 * @return the channel future
	 */
	ChannelFuture close();

	/**
	 * Gets the underlying DatagramChannel.
	 *
	 * @return the channel
	 */
	DatagramChannel getChannel();

	/**
	 * Gets the port on which this channel is bound, -1 if unknown.
	 *
	 * @return the port
	 */
	int getPort();

	/**
	 * Returns the local address, null if unknown.
	 *
	 * @return the inet socket address
	 */
	InetSocketAddress localAddress();

	/**
	 * Get the {@link ByteBufCreator}.
	 * 
	 * @return byteBufCreator
	 * @see ByteBufCreator
	 * @see ByteArrayByteBufCreator
	 * @see StringByteBufCreator
	 */
	ByteBufCreator getByteBufCreator();
}
