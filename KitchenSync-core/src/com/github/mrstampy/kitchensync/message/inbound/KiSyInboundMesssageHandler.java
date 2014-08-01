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

import java.io.Serializable;
import java.net.InetSocketAddress;

import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;

/**
 * KiSyInboundMesssageHandler implementations define the operations to execute
 * and order of execution on inbound messages.
 *
 * @param <MSG>
 *          the generic type
 */
public interface KiSyInboundMesssageHandler<MSG> extends Serializable {

	/**
	 * Return true if the implementation can deal with this message, else false.
	 *
	 * @param message
	 *          the message
	 * @return true, if can handle message
	 */
	boolean canHandleMessage(MSG message);

	/**
	 * Message received method, invoked by {@link KiSyInboundMessageManager}.
	 *
	 * @param message
	 *          the message
	 * @param channel
	 *          the channel
	 * @param sender
	 *          the sender
	 * @throws Exception
	 *           the exception
	 */
	void messageReceived(MSG message, KiSyChannel channel, InetSocketAddress sender) throws Exception;

	/**
	 * Gets the execution order for defining the order of execution.
	 *
	 * @return the execution order
	 */
	int getExecutionOrder();
}
