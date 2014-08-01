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
package com.github.mrstampy.kitchensync.test.channel;

import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.UnknownHostException;

import com.github.mrstampy.kitchensync.netty.channel.AbstractKiSyMulticastChannel;
import com.github.mrstampy.kitchensync.netty.channel.initializer.StringMessageInitializer;
import com.github.mrstampy.kitchensync.netty.channel.payload.StringByteBufCreator;

/**
 * The Class StringMulticastChannel.
 */
public class StringMulticastChannel extends
		AbstractKiSyMulticastChannel<StringByteBufCreator, StringMessageInitializer, NioDatagramChannel> {

	/**
	 * The Constructor.
	 *
	 * @param multicastIPv6 the multicast i pv6
	 * @param port the port
	 * @throws UnknownHostException the unknown host exception
	 */
	public StringMulticastChannel(String multicastIPv6, int port) throws UnknownHostException {
		super(multicastIPv6, port);
	}

	/* (non-Javadoc)
	 * @see com.github.mrstampy.kitchensync.netty.channel.AbstractKiSyChannel#initializer()
	 */
	@Override
	protected StringMessageInitializer initializer() {
		return new StringMessageInitializer();
	}

	/* (non-Javadoc)
	 * @see com.github.mrstampy.kitchensync.netty.channel.AbstractKiSyChannel#getChannelClass()
	 */
	@Override
	protected Class<NioDatagramChannel> getChannelClass() {
		return NioDatagramChannel.class;
	}

	/* (non-Javadoc)
	 * @see com.github.mrstampy.kitchensync.netty.channel.AbstractKiSyChannel#initByteBufCreator()
	 */
	@Override
	protected StringByteBufCreator initByteBufCreator() {
		return new StringByteBufCreator();
	}

}
