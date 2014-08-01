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

import java.util.concurrent.CountDownLatch;

import com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMessageManager;
import com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMesssageHandler;
import com.github.mrstampy.kitchensync.message.outbound.KiSyOutboundMessageHandler;
import com.github.mrstampy.kitchensync.message.outbound.KiSyOutboundMessageManager;

/**
 * Abstract superclass for test classes. Ensures that the various inbound and
 * outbound handlers are set up prior to channel creation.
 * 
 * @see KiSyInitializer
 * @see KiSyInboundMesssageHandler
 * @see KiSyOutboundMessageHandler
 * @see KiSyInboundMessageManager
 * @see KiSyOutboundMessageManager
 */
public abstract class AbstractTester {

	/**
	 * The Constructor.
	 */
	protected AbstractTester() {
		initKiSyHandlers();
	}

	/**
	 * Inits the ki sy handlers.
	 */
	protected void initKiSyHandlers() {
		KiSyInitializer initer = new KiSyInitializer();

		initer.initInboundHandlers();
		initer.initOutboundHandlers();
	}

	/**
	 * Adds the latch listener.
	 *
	 * @param cf
	 *          the cf
	 * @param cdl
	 *          the cdl
	 */
	protected void addLatchListener(ChannelFuture cf, final CountDownLatch cdl) {
		cf.addListener(new GenericFutureListener<ChannelFuture>() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				cdl.countDown();
			}
		});
	}

}
