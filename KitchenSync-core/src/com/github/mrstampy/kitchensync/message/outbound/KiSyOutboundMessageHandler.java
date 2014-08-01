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
package com.github.mrstampy.kitchensync.message.outbound;

import java.net.InetSocketAddress;

/**
 * KiSyOutboundMessageHandler implementations define the operations to execute
 * and order of execution on outbound messages.
 *
 * @param <MSG>
 *          the generic type
 */
public interface KiSyOutboundMessageHandler<MSG> {

	/** The Constant DEFAULT_EXECUTION_ORDER. */
	public static final int DEFAULT_EXECUTION_ORDER = 100;

	/**
	 * Return true if this handler is required, false otherwise.
	 *
	 * @param message
	 *          the message to send
	 * @param recipient
	 *          the intended recipient
	 * @return true, if checks if is for message
	 */
	boolean isForMessage(MSG message, InetSocketAddress recipient);

	/**
	 * Invoked by the {@link KiSyOutboundMessageManager} prior to sending the
	 * message.
	 *
	 * @param message
	 *          the message to send
	 * @param originator
	 *          the originator
	 * @param recipient
	 *          the intended recipient
	 */
	void presend(MSG message, InetSocketAddress originator, InetSocketAddress recipient);

	/**
	 * Gets the execution order.
	 *
	 * @return the execution order
	 */
	int getExecutionOrder();
}
