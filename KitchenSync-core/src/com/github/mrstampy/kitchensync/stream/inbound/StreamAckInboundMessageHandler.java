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

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.kitchensync.message.inbound.AbstractInboundKiSyHandler;
import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;
import com.github.mrstampy.kitchensync.stream.Streamer;
import com.github.mrstampy.kitchensync.stream.StreamerAckRegister;

/**
 * The Class StreamAckInboundMessageHandler deals with acknowledgement messages,
 * obtaining the requesting {@link Streamer#isAckRequired()} {@link Streamer}
 * from the {@link StreamerAckRegister} and notifying it, triggering the sending
 * of the next chunk.
 */
public class StreamAckInboundMessageHandler extends AbstractInboundKiSyHandler<byte[]> {
	private static final long serialVersionUID = 3604458060038395973L;

	private static final Logger log = LoggerFactory.getLogger(StreamAckInboundMessageHandler.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMesssageHandler
	 * #canHandleMessage(java.lang.Object)
	 */
	@Override
	public boolean canHandleMessage(byte[] message) {
		return StreamerAckRegister.isAckMessage(message);
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
		return DEFAULT_EXECUTION_ORDER;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.message.inbound.AbstractInboundKiSyHandler
	 * #onReceive(java.lang.Object,
	 * com.github.mrstampy.kitchensync.netty.channel.KiSyChannel,
	 * java.net.InetSocketAddress)
	 */
	@Override
	protected void onReceive(byte[] message, KiSyChannel channel, InetSocketAddress sender) throws Exception {
		String s = new String(message);
		Long sumOfBytes = parseMessage(s);

		if (sumOfBytes == null) return;

		Streamer<?> streamer = StreamerAckRegister.getAckAwaiter(sumOfBytes, channel.getPort());

		if (streamer == null) {
			log.warn("No streamer for {}", s);
			return;
		}

		streamer.ackReceived(sumOfBytes);
	}

	private Long parseMessage(String message) {
		try {
			String s = message.substring(StreamerAckRegister.ACK_PREFIX.length(), message.length());

			return Long.parseLong(s);
		} catch (Exception e) {
			log.error("Could not obtain sum of bytes from {}", message, e);
		}

		return null;
	}

}
