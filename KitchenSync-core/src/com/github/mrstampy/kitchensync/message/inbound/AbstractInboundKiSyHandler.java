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
package com.github.mrstampy.kitchensync.message.inbound;

import java.net.InetSocketAddress;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;

/**
 * The convenience class AbstractInboundKiSyHandler.
 *
 * @param <MSG>
 *          the generic type
 */
public abstract class AbstractInboundKiSyHandler<MSG> implements KiSyInboundMesssageHandler<MSG> {
	private static final long serialVersionUID = -4745725747804368460L;
	private static final Logger log = LoggerFactory.getLogger(AbstractInboundKiSyHandler.class);

	/** The Constant DEFAULT_EXECUTION_ORDER, 100. */
	public static final int DEFAULT_EXECUTION_ORDER = 100;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMesssageHandler
	 * #messageReceived(java.lang.Object,
	 * com.github.mrstampy.kitchensync.netty.channel.KiSyChannel)
	 */
	@Override
	public final void messageReceived(MSG message, KiSyChannel channel, InetSocketAddress sender) {
		try {
			onReceive(message, channel, sender);
		} catch (Exception e) {
			log.error("Unexpected exception processing message {}", message, e);
		}
	}

	/**
	 * Implement in subclasses to process the message.
	 *
	 * @param message          the inbound message
	 * @param channel          the channel on which the message was received
	 * @param sender the sender
	 * @throws Exception the exception
	 */
	protected abstract void onReceive(MSG message, KiSyChannel channel, InetSocketAddress sender) throws Exception;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public final String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
