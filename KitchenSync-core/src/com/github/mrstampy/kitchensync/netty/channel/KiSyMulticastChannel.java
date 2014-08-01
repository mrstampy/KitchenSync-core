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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;

/**
 * {@link KiSyChannel} implementations interface the <a
 * href="http://netty.io">Netty</a> specifics, allowing ease of socket creation
 * and use. Multicast channel implementations provide additional methods which
 * are specific to multicast channels.<br>
 * <br>
 * 
 * KitchenSync multicast channels use <a
 * href="http://en.wikipedia.org/wiki/Multicast_address">IPv6 addresses</a>.
 * Ranges and uses can be found at the <a href=
 * "http://www.iana.org/assignments/ipv6-multicast-addresses/ipv6-multicast-addresses.xhtml"
 * >IPv6 Multicast Address Space Registry</a>
 */
public interface KiSyMulticastChannel extends KiSyChannel {

	/**
	 * Broadcast a message to the multicast channel. If {@link #joinGroup()} has
	 * been called the message will appear on this channel.
	 *
	 * @param <MSG>
	 *          the generic type
	 * @param message
	 *          the message
	 * @return the channel future
	 */
	<MSG> ChannelFuture broadcast(MSG message);

	/**
	 * Join the multicast group to receive broadcast messages sent to this
	 * address.
	 *
	 * @return true, if join group
	 */
	boolean joinGroup();

	/**
	 * Leave group to stop receiving broadcast messages..
	 *
	 * @return true, if leave group
	 */
	boolean leaveGroup();

	/**
	 * Gets the multicast address.
	 *
	 * @return the multicast address
	 */
	InetSocketAddress getMulticastAddress();

	/**
	 * Gets the network interface.
	 *
	 * @return the network interface
	 */
	NetworkInterface getNetworkInterface();

	/**
	 * Blocks messages from the specified source.
	 *
	 * @param sourceToBlock the source to block
	 * @return true if successful
	 */
	boolean block(InetAddress sourceToBlock);
}
