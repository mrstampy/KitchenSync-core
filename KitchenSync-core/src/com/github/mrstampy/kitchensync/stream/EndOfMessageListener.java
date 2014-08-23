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
package com.github.mrstampy.kitchensync.stream;

import java.net.InetSocketAddress;

import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;
import com.github.mrstampy.kitchensync.stream.inbound.EndOfMessageInboundMessageHandler;

/**
 * The Interface EndOfMessageListener is implemented by any class wishing to be
 * notified when the remote connection has finished sending a logical unit of
 * data ie. end of file, end of stream etc. Implementations are registered with
 * the {@link EndOfMessageRegister} and the
 * {@link EndOfMessageInboundMessageHandler} notifies the register when an end
 * of message messsage has been received.
 */
public interface EndOfMessageListener {

	/**
	 * Implement to return true to receive notification of end of messages on the
	 * specified channel, from the specified origin.
	 *
	 * @param channel
	 *          the channel
	 * @param sender
	 *          the sender
	 * @return true, if checks if is for channel and sender
	 */
	boolean isForChannelAndSender(KiSyChannel channel, InetSocketAddress sender);

	/**
	 * Invoked by the {@link EndOfMessageRegister} when
	 * {@link #isForChannelAndSender(KiSyChannel, InetSocketAddress)} returns
	 * true.
	 * 
	 * @param eom
	 *          the end of message message
	 */
	void endOfMessage(byte[] eom);
}
