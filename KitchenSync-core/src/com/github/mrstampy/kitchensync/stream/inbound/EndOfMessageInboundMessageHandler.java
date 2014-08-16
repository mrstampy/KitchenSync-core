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
package com.github.mrstampy.kitchensync.stream.inbound;

import static com.github.mrstampy.kitchensync.stream.EndOfMessageRegister.isEndOfMessage;
import static com.github.mrstampy.kitchensync.stream.EndOfMessageRegister.notifyEOMListeners;

import java.net.InetSocketAddress;

import com.github.mrstampy.kitchensync.message.inbound.AbstractInboundKiSyHandler;
import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;

// TODO: Auto-generated Javadoc
/**
 * The Class EndOfMessageInboundMessageHandler.
 */
public class EndOfMessageInboundMessageHandler extends AbstractInboundKiSyHandler<byte[]> {

	private static final long serialVersionUID = -2312727898381854486L;

	/* (non-Javadoc)
	 * @see com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMesssageHandler#canHandleMessage(java.lang.Object)
	 */
	@Override
	public boolean canHandleMessage(byte[] message) {
		return isEndOfMessage(message);
	}

	/* (non-Javadoc)
	 * @see com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMesssageHandler#getExecutionOrder()
	 */
	@Override
	public int getExecutionOrder() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.github.mrstampy.kitchensync.message.inbound.AbstractInboundKiSyHandler#onReceive(java.lang.Object, com.github.mrstampy.kitchensync.netty.channel.KiSyChannel, java.net.InetSocketAddress)
	 */
	@Override
	protected void onReceive(byte[] message, KiSyChannel channel, InetSocketAddress sender) throws Exception {
		notifyEOMListeners(channel, sender);
	}

}
