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
package com.github.mrstampy.kitchensync.netty.channel;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.kitchensync.netty.Bootstrapper;
import com.github.mrstampy.kitchensync.netty.channel.payload.ByteBufCreator;

/**
 * Abstract superclass for {@link KiSyMulticastChannel}s. IPv6 addresses must be
 * used. A Bootstrap is created for each instance of
 * AbstractKiSyMulticastChannel.
 *
 * @param <BBC>
 *          the {@link ByteBufCreator} implementation
 * @param <CI>
 *          the ChannelInitializer implementation
 * @param <DC>
 *          the DatagramChannel implementaion
 */
public abstract class AbstractKiSyMulticastChannel<BBC extends ByteBufCreator, CI extends ChannelInitializer<DatagramChannel>, DC extends DatagramChannel>
		extends AbstractKiSyChannel<BBC, CI, DC> implements KiSyMulticastChannel {
	private static final Logger log = LoggerFactory.getLogger(AbstractKiSyMulticastChannel.class);

	private InetSocketAddress multicastAddress;
	private NetworkInterface networkInterface;

	/**
	 * The Constructor.
	 *
	 * @param multicastIPv6
	 *          the multicast i pv6
	 * @param port
	 *          the port
	 * @throws UnknownHostException
	 *           the unknown host exception
	 */
	public AbstractKiSyMulticastChannel(String multicastIPv6, int port) throws UnknownHostException {
		this(multicastIPv6, port, null);
	}

	/**
	 * The Constructor.
	 *
	 * @param multicastIPv6
	 *          the multicast i pv6
	 * @param port
	 *          the port
	 * @param networkInterface
	 *          the network interface
	 * @throws UnknownHostException
	 *           the unknown host exception
	 */
	public AbstractKiSyMulticastChannel(String multicastIPv6, int port, NetworkInterface networkInterface)
			throws UnknownHostException {
		this(new InetSocketAddress(Inet6Address.getByName(multicastIPv6), port), networkInterface);
	}

	/**
	 * The Constructor.
	 *
	 * @param multicastAddress
	 *          the multicast address
	 */
	public AbstractKiSyMulticastChannel(InetSocketAddress multicastAddress) {
		this(multicastAddress, null);
	}

	/**
	 * The Constructor.
	 *
	 * @param multicastAddress
	 *          the multicast address
	 * @param networkInterface
	 *          the network interface
	 */
	public AbstractKiSyMulticastChannel(InetSocketAddress multicastAddress, NetworkInterface networkInterface) {
		super();
		this.multicastAddress = multicastAddress;
		this.networkInterface = networkInterface == null ? bootstrapper.DEFAULT_INTERFACE : networkInterface;
	}

	/**
	 * Binds to the multicast address specified in the constructor.
	 *
	 * @param port
	 *          the port
	 */
	@Override
	public void bind(int port) {
		bind();
	}

	/**
	 * Binds to the multicast address specified in the constructor.
	 */
	@Override
	public void bind() {
		prebind();

		if (isActive()) closeChannel();

		if (!bootstrapper.containsMulticastBootstrap(getMulticastAddress())) {
			bootstrapper.initMulticastBootstrap(initializer(), getMulticastAddress(), getNetworkInterface(),
					getChannelClass());
		}

		DC channel = bootstrapper.multicastBind(getMulticastAddress());
		setChannel(channel);

		registry.addMulticastChannel(this);
	}

	/**
	 * The address parameter is ignored; messages are sent via
	 * {@link #broadcast(Object)}.
	 *
	 * @param <MSG>
	 *          the generic type
	 * @param message
	 *          the message
	 * @param address
	 *          the address
	 * @return the channel future
	 */
	@Override
	public <MSG extends Object> ChannelFuture send(MSG message, InetSocketAddress address) {
		return broadcast(message);
	}

	/**
	 * Sets the channel.
	 *
	 * @param channel
	 *          the channel
	 */
	protected void setChannel(DC channel) {
		super.setChannel(channel);

		channel.closeFuture().addListener(new GenericFutureListener<Future<Void>>() {

			@Override
			public void operationComplete(Future<Void> future) throws Exception {
				registry.removeMulticastChannel(AbstractKiSyMulticastChannel.this);
			}
		});
	}

	/**
	 * Note that broadcasting bypasses the outbound message handlers. If required
	 * this method can be overridden to invoke
	 * {@link #presend(Object, InetSocketAddress)} prior to
	 * {@link #sendImpl(Object, InetSocketAddress)}.
	 *
	 * @param <MSG>
	 *          the generic type
	 * @param message
	 *          the message
	 * @return the channel future
	 */
	@Override
	public <MSG extends Object> ChannelFuture broadcast(MSG message) {
		return sendImpl(createMessage(message, getMulticastAddress()), getMulticastAddress());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.netty.channel.KiSyMulticastChannel#joinGroup
	 * ()
	 */
	@Override
	public boolean joinGroup() {
		if (!isActive()) return false;

		ChannelFuture cf = getChannel().joinGroup(getMulticastAddress(), getNetworkInterface());

		CountDownLatch latch = new CountDownLatch(1);
		cf.addListener(getJoinGroupListener(getMulticastAddress(), latch));

		await(latch, "Multicast channel join group timed out");

		return cf.isSuccess();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.netty.channel.KiSyMulticastChannel#leaveGroup
	 * ()
	 */
	@Override
	public boolean leaveGroup() {
		if (!isActive()) return false;

		ChannelFuture cf = getChannel().leaveGroup(getMulticastAddress(), getNetworkInterface());

		CountDownLatch latch = new CountDownLatch(1);
		cf.addListener(getLeaveGroupListener(getMulticastAddress(), latch));

		await(latch, "Multicast channel leave group timed out");

		return cf.isSuccess();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.netty.channel.KiSyMulticastChannel#
	 * block(InetAddress)
	 */
	@Override
	public boolean block(InetAddress sourceToBlock) {
		if (!isActive()) return false;

		ChannelFuture cf = getChannel().block(getMulticastAddress().getAddress(), getNetworkInterface(), sourceToBlock);

		CountDownLatch latch = new CountDownLatch(1);
		cf.addListener(getBlockListener(getMulticastAddress(), latch, sourceToBlock));

		await(latch, "Multicast block timed out");

		return cf.isSuccess();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.netty.channel.KiSyMulticastChannel#
	 * getMulticastAddress()
	 */
	public InetSocketAddress getMulticastAddress() {
		return multicastAddress;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.netty.channel.KiSyMulticastChannel#
	 * getNetworkInterface()
	 */
	public NetworkInterface getNetworkInterface() {
		return networkInterface;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.netty.channel.AbstractKiSyChannel#getPort()
	 */
	public int getPort() {
		return getMulticastAddress().getPort();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.netty.channel.KiSyChannel#isMulticastChannel
	 * ()
	 */
	@Override
	public boolean isMulticastChannel() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.netty.channel.KiSyChannel#isPortSpecificChannel
	 * ()
	 */
	@Override
	public boolean isPortSpecificChannel() {
		return true;
	}

	/**
	 * Creates the multicast key from {@link #getMulticastAddress()}.
	 *
	 * @return the string
	 * @see Bootstrapper
	 */
	public String createMulticastKey() {
		return createMulticastKey(getMulticastAddress());
	}

	/**
	 * Creates the multicast key from the specified address.
	 *
	 * @param address
	 *          the address
	 * @return the string
	 * @see Bootstrapper
	 */
	public static String createMulticastKey(InetSocketAddress address) {
		return new String(address.getAddress().getAddress());
	}

	private GenericFutureListener<ChannelFuture> getJoinGroupListener(final InetSocketAddress multicast,
			final CountDownLatch latch) {
		return new GenericFutureListener<ChannelFuture>() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				try {
					if (future.isSuccess()) {
						log.debug("Multicast channel joined group {}", multicast);
					} else {
						Throwable cause = future.cause();
						if (cause == null) {
							log.error("Could not join multicast group for {}", multicast);
						} else {
							log.error("Could not join multicast group for {}", multicast, cause);
						}
					}
				} finally {
					latch.countDown();
				}
			}
		};
	}

	private GenericFutureListener<ChannelFuture> getLeaveGroupListener(final InetSocketAddress multicast,
			final CountDownLatch latch) {
		return new GenericFutureListener<ChannelFuture>() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				try {
					if (future.isSuccess()) {
						log.debug("Multicast channel left group {}", multicast);
					} else {
						Throwable cause = future.cause();
						if (cause == null) {
							log.error("Could not leave multicast group for {}", multicast);
						} else {
							log.error("Could not leave multicast group for {}", multicast, cause);
						}
					}
				} finally {
					latch.countDown();
				}
			}
		};
	}

	private GenericFutureListener<ChannelFuture> getBlockListener(final InetSocketAddress multicast,
			final CountDownLatch latch, final InetAddress sourceToBlock) {
		return new GenericFutureListener<ChannelFuture>() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				try {
					if (future.isSuccess()) {
						log.debug("Multicast channel {} now blocking {}", multicast, sourceToBlock);
					} else {
						Throwable cause = future.cause();
						if (cause == null) {
							log.error("Could not block {} from {}", sourceToBlock, multicast);
						} else {
							log.error("Could not block {} from {}", sourceToBlock, multicast, cause);
						}
					}
				} finally {
					latch.countDown();
				}
			}
		};
	}

}
