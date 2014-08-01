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
package com.github.mrstampy.kitchensync.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMessageManager;
import com.github.mrstampy.kitchensync.netty.channel.DefaultChannelRegistry;
import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;

/**
 * Abstract superclass providing the ability to handle string and byte array
 * messages and providing the ability to specify a custom
 * {@link KiSyInboundMessageManager}.
 *
 * @param <MSG>
 *          the generic type
 */
public abstract class AbstractKiSyNettyHandler<MSG> extends SimpleChannelInboundHandler<DatagramPacket> {
	private static final Logger log = LoggerFactory.getLogger(AbstractKiSyNettyHandler.class);

	private KiSyInboundMessageManager<MSG> inboundMessageManager;

	private DefaultChannelRegistry registry = DefaultChannelRegistry.INSTANCE;

	/**
	 * Specify a custom inbound message manager for this handler.
	 *
	 * @param custom the custom
	 */
	protected AbstractKiSyNettyHandler(KiSyInboundMessageManager<MSG> custom) {
		setInboundMessageManager(custom);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.netty.channel.ChannelInboundHandlerAdapter#exceptionCaught(io.netty.
	 * channel.ChannelHandlerContext, java.lang.Throwable)
	 */
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error("Unexpected exception", cause);
	}

	/**
	 * Gets the channel.
	 *
	 * @param packet
	 *          the packet
	 * @return the channel
	 */
	protected KiSyChannel getChannel(DatagramPacket packet) {
		InetSocketAddress recipient = packet.recipient();
		return isMulticastChannel(recipient) ? getMulticastChannel(recipient) : getChannel(recipient.getPort());
	}

	/**
	 * Checks if is multicast channel.
	 *
	 * @param local
	 *          the local
	 * @return true, if checks if is multicast channel
	 */
	protected boolean isMulticastChannel(InetSocketAddress local) {
		return registry.isMulticastChannel(local);
	}

	/**
	 * Gets the multicast channel.
	 *
	 * @param local
	 *          the local
	 * @return the multicast channel
	 */
	protected KiSyChannel getMulticastChannel(InetSocketAddress local) {
		return registry.getMulticastChannel(local);
	}

	/**
	 * Gets the channel.
	 *
	 * @param port
	 *          the port
	 * @return the channel
	 */
	protected KiSyChannel getChannel(int port) {
		return registry.getChannel(port);
	}

	/**
	 * Process message.
	 *
	 * @param message
	 *          the message
	 * @param msg
	 *          the msg
	 */
	protected void processMessage(MSG message, DatagramPacket msg) {
		if (getInboundMessageManager() == null) {
			log.error("No inbound message manager set");
			return;
		}

		inboundMessageManager.processMessage(message, getChannel(msg), msg.sender());
	}

	/**
	 * Content.
	 *
	 * @param msg
	 *          the msg
	 * @return the string
	 */
	protected String content(DatagramPacket msg) {
		return msg.content().toString(CharsetUtil.UTF_8);
	}

	/**
	 * Bytes.
	 *
	 * @param msg
	 *          the msg
	 * @return the byte[]
	 */
	protected byte[] bytes(DatagramPacket msg) {
		ByteBuf content = msg.content();

		int num = content.readableBytes();
		byte[] message = new byte[num];
		content.readBytes(message);

		return message;
	}

	/**
	 * Gets the inbound message manager.
	 *
	 * @return the custom handler
	 */
	public KiSyInboundMessageManager<MSG> getInboundMessageManager() {
		return inboundMessageManager;
	}

	/*
	 * Sets the inbound message manager.
	 * 
	 * @param inboundMessageManager the custom handler
	 */
	private void setInboundMessageManager(KiSyInboundMessageManager<MSG> inboundMessageManager) {
		this.inboundMessageManager = inboundMessageManager;
	}

}
