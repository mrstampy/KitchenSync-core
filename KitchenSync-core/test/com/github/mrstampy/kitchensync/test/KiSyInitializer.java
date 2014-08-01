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

import com.github.mrstampy.kitchensync.message.inbound.ByteArrayInboundMessageManager;
import com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMessageManager;
import com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMesssageHandler;
import com.github.mrstampy.kitchensync.message.inbound.StringInboundMessageManager;
import com.github.mrstampy.kitchensync.message.inbound.logging.LoggingInboundMessageHandler;
import com.github.mrstampy.kitchensync.message.outbound.KiSyOutboundMessageHandler;
import com.github.mrstampy.kitchensync.message.outbound.KiSyOutboundMessageManager;
import com.github.mrstampy.kitchensync.message.outbound.logging.LoggingOutboundMessageHandler;

/**
 * The Class KiSyInitializer ensures that the inbound and outbound managers are
 * initialized with the handlers necessary for testing.
 * 
 * @see KiSyInboundMesssageHandler
 * @see KiSyOutboundMessageHandler
 * @see KiSyInboundMessageManager
 * @see KiSyOutboundMessageManager
 * @see LoggingOutboundMessageHandler
 * @see LoggingInboundMessageHandler
 */
public class KiSyInitializer {

	/**
	 * Inits the outbound handlers.
	 */
	@SuppressWarnings("rawtypes")
	public void initOutboundHandlers() {
		KiSyOutboundMessageManager manager = KiSyOutboundMessageManager.INSTANCE;

		//@formatter:off
		manager.addOutboundHandlers(
				new LoggingOutboundMessageHandler()
				);
		//@formatter:on
	}

	/**
	 * Inits the inbound handlers.
	 */
	@SuppressWarnings("rawtypes")
	public void initInboundHandlers() {
		StringInboundMessageManager.INSTANCE.addMessageHandlers(new LoggingInboundMessageHandler());
		ByteArrayInboundMessageManager.INSTANCE.addMessageHandlers(new LoggingInboundMessageHandler());
		//@formatter:on
	}
}
