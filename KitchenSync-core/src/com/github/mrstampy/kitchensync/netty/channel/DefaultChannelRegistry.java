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

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Class DefaultChannelRegistry provides the ability to look up active
 * channels.
 */
public class DefaultChannelRegistry {

	private Map<Integer, AbstractKiSyChannel<?,?,?>> channels = new ConcurrentHashMap<Integer, AbstractKiSyChannel<?,?,?>>();
	private Map<String, AbstractKiSyMulticastChannel<?,?,?>> multicastChannels = new ConcurrentHashMap<String, AbstractKiSyMulticastChannel<?,?,?>>();

	/** The Constant INSTANCE. */
	public static final DefaultChannelRegistry INSTANCE = new DefaultChannelRegistry();

	/**
	 * The Constructor.
	 */
	protected DefaultChannelRegistry() {
	}

	/**
	 * Gets the channel.
	 *
	 * @param port
	 *          the port
	 * @return the channel
	 */
	public AbstractKiSyChannel<?,?,?> getChannel(int port) {
		return channels.get(port);
	}

	/**
	 * Gets the multicast channel.
	 *
	 * @param address
	 *          the address
	 * @return the multicast channel
	 */
	public AbstractKiSyMulticastChannel<?,?,?> getMulticastChannel(InetSocketAddress address) {
		return getMulticastChannel(AbstractKiSyMulticastChannel.createMulticastKey(address));
	}

	/**
	 * Gets the multicast channel.
	 *
	 * @param key
	 *          the key
	 * @return the multicast channel
	 */
	public AbstractKiSyMulticastChannel<?,?,?> getMulticastChannel(String key) {
		return multicastChannels.get(key);
	}

	/**
	 * Gets the channel keys.
	 *
	 * @return the channel keys
	 */
	public Set<Integer> getChannelKeys() {
		return channels.keySet();
	}

	/**
	 * Gets the multicast channel keys.
	 *
	 * @return the multicast channel keys
	 */
	public Set<String> getMulticastChannelKeys() {
		return multicastChannels.keySet();
	}

	/**
	 * Checks if is multicast channel.
	 *
	 * @param address
	 *          the address
	 * @return true, if checks if is multicast channel
	 */
	public boolean isMulticastChannel(InetSocketAddress address) {
		return multicastChannels.containsKey(AbstractKiSyMulticastChannel.createMulticastKey(address));
	}

	/**
	 * Adds the channel.
	 *
	 * @param channel
	 *          the channel
	 */
	public void addChannel(AbstractKiSyChannel<?,?,?> channel) {
		if (!channels.containsKey(channel.getPort())) channels.put(channel.getPort(), channel);
	}

	/**
	 * Removes the channel.
	 *
	 * @param channel
	 *          the channel
	 */
	public void removeChannel(AbstractKiSyChannel<?,?,?> channel) {
		removeChannel(channel.getPort());
	}

	/**
	 * Removes the channel.
	 *
	 * @param port
	 *          the port
	 * @return the abstract ki sy channel
	 */
	public AbstractKiSyChannel<?,?,?> removeChannel(int port) {
		return channels.remove(port);
	}

	/**
	 * Clear channels.
	 */
	public void clearChannels() {
		channels.clear();
	}

	/**
	 * Adds the multicast channel.
	 *
	 * @param channel
	 *          the channel
	 */
	public void addMulticastChannel(AbstractKiSyMulticastChannel<?,?,?> channel) {
		String key = channel.createMulticastKey();
		if (!multicastChannels.containsKey(key)) multicastChannels.put(key, channel);
	}

	/**
	 * Removes the multicast channel.
	 *
	 * @param channel
	 *          the channel
	 */
	public void removeMulticastChannel(AbstractKiSyMulticastChannel<?,?,?> channel) {
		removeMulticastChannel(channel.createMulticastKey());
	}

	/**
	 * Removes the multicast channel.
	 *
	 * @param key
	 *          the key
	 * @return the default ki sy multicast channel
	 */
	public AbstractKiSyMulticastChannel<?,?,?> removeMulticastChannel(String key) {
		return multicastChannels.remove(key);
	}

	/**
	 * Clear multicast channels.
	 */
	public void clearMulticastChannels() {
		multicastChannels.clear();
	}
}
