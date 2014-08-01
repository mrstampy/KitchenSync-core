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
package com.github.mrstampy.kitchensync.test;

import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.UnknownHostException;

import com.github.mrstampy.kitchensync.netty.channel.KiSyMulticastChannel;
import com.github.mrstampy.kitchensync.test.channel.StringMulticastChannel;

/**
 * Tests multicast broadcasting by creating 3 channels and having one leave the
 * group to send a message to the other two. This test will not run on a
 * computer not connected to a network.<br>
 * <br>
 * 
 * <a href="http://en.wikipedia.org/wiki/Multicast_address">IPv6 addresses</a>
 * must be used. See the <a href=
 * "http://www.iana.org/assignments/ipv6-multicast-addresses/ipv6-multicast-addresses.xhtml"
 * >IPv6 Multicast Address Space Registry</a> for more information.
 */
public class BroadcastTester extends AbstractTester {
	private static final String MULTICAST_IP = "FF05:0:0548:c4e6:796c:0:de66:FC";
	private static final int MULTICAST_PORT = 57962;

	private KiSyMulticastChannel michael;
	private KiSyMulticastChannel robert;
	private KiSyMulticastChannel paul;

	private void init() throws UnknownHostException {
		michael = createMulticastChannel();
		robert = createMulticastChannel();
		paul = createMulticastChannel();
	}

	/**
	 * Execute.
	 */
	public void execute() {
		michael.leaveGroup();

		ChannelFuture cf = michael.broadcast("A good day to you all!");

		cf.addListener(new GenericFutureListener<ChannelFuture>() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				michael.joinGroup();
			}
		});
	}

	/**
	 * Creates the multicast channel.
	 *
	 * @return the default ki sy multicast channel
	 * @throws UnknownHostException
	 *           the unknown host exception
	 */
	protected KiSyMulticastChannel createMulticastChannel() throws UnknownHostException {
		StringMulticastChannel channel = new StringMulticastChannel(MULTICAST_IP, MULTICAST_PORT);

		channel.bind();
		channel.joinGroup();

		return channel;
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *          the args
	 * @throws Exception
	 *           the exception
	 */
	public static void main(String[] args) throws Exception {
		BroadcastTester tester = new BroadcastTester();

		tester.init();

		tester.execute();
	}

}
