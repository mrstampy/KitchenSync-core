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
package com.github.mrstampy.kitchensync.message.outbound.logging;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.kitchensync.message.outbound.KiSyOutboundMessageHandler;

/**
 * The Class LoggingOutboundMessageHandler logs all outbound messages should the
 * log level be debug. Large messages should not be logged by this handler;
 * create a custom handler which trims/ignores the messages appropriately.
 *
 * @param <MSG>
 *          the generic type
 */
public class LoggingOutboundMessageHandler<MSG> implements KiSyOutboundMessageHandler<MSG> {
	private static final Logger log = LoggerFactory.getLogger(LoggingOutboundMessageHandler.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.message.outbound.KiSyOutboundMessageHandler
	 * #isForMessage(java.lang.Object, java.net.InetSocketAddress)
	 */
	@Override
	public boolean isForMessage(MSG message, InetSocketAddress recipient) {
		return log.isDebugEnabled();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.message.outbound.KiSyOutboundMessageHandler
	 * #presend(java.lang.Object, java.net.InetSocketAddress,
	 * java.net.InetSocketAddress)
	 */
	@Override
	public void presend(MSG message, InetSocketAddress originator, InetSocketAddress recipient) {
		log.debug("Sending {} from {} to {}", message, originator, recipient);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.message.outbound.KiSyOutboundMessageHandler
	 * #getExecutionOrder()
	 */
	@Override
	public int getExecutionOrder() {
		return 1;
	}

}
