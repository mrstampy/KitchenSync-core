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

import java.util.concurrent.CountDownLatch;

import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;
import com.github.mrstampy.kitchensync.test.channel.ByteArrayChannel;

/**
 * Sends 100 byte array messages from one channel to another. Logging at debug
 * level will show the message processing.
 */
public class ByteArrayTester extends AbstractTester {

	private KiSyChannel channel;
	private KiSyChannel channel2;

	private void execute() {
		channel = initChannel();
		channel2 = initChannel();
	}

	/**
	 * Send ping.
	 */
	public void sendPing() {
		channel.send("Howdy channel 2!".getBytes(), channel2.localAddress());
	}

	/**
	 * Inits the channel.
	 *
	 * @return the default ki sy channel
	 */
	protected KiSyChannel initChannel() {
		ByteArrayChannel channel = new ByteArrayChannel();

		channel.bind();

		return channel;
	}

	/**
	 * Disconnect.
	 *
	 * @throws InterruptedException
	 *           the interrupted exception
	 */
	public void disconnect() throws InterruptedException {
		CountDownLatch cdl = new CountDownLatch(2);
		ChannelFuture cf = channel.close();
		addLatchListener(cf, cdl);

		ChannelFuture cf2 = channel2.close();
		addLatchListener(cf2, cdl);

		cdl.await();
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
		ByteArrayTester ptpt = new ByteArrayTester();
		ptpt.execute();
		for (int i = 0; i < 100; i++) {
			ptpt.sendPing();
			Thread.sleep(50);
		}

		ptpt.disconnect();
		System.exit(0);
	}

}
