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
package com.github.mrstampy.kitchensync.message.inbound.logging;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.kitchensync.message.inbound.AbstractInboundKiSyHandler;
import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;

/**
 * The Class LoggingInboundMessageHandler logs all inbound messages if the log
 * level is set to debug. Large messages should not be logged by this handler;
 * create a custom handler which trims/ignores the messages appropriately.
 *
 * @param <MSG>
 *          the generic type
 */
public class LoggingInboundMessageHandler<MSG> extends AbstractInboundKiSyHandler<MSG> {
	private static final long serialVersionUID = 345595930033076784L;
	private static final Logger log = LoggerFactory.getLogger(LoggingInboundMessageHandler.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMesssageHandler
	 * #canHandleMessage(java.lang.Object)
	 */
	@Override
	public boolean canHandleMessage(MSG message) {
		return log.isDebugEnabled();
	}

	/*
	 * (non-Javadoc)
	 */
	@Override
	protected void onReceive(MSG message, KiSyChannel channel, InetSocketAddress sender) throws Exception {
		log.debug("Received message {} from {}", message, sender);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMesssageHandler
	 * #getExecutionOrder()
	 */
	@Override
	public int getExecutionOrder() {
		return 1; // logging always first
	}

}
