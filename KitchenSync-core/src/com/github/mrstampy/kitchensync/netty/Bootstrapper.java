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
package com.github.mrstampy.kitchensync.netty;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.oio.OioDatagramChannel;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.kitchensync.netty.channel.AbstractKiSyMulticastChannel;
import com.github.mrstampy.kitchensync.util.KiSyUtils;

/**
 * The Class Bootstrapper is the KitchenSync/<a href="http://netty.io">Netty</a>
 * core class. It instantiates and keeps track of all Netty <a
 * href="http://netty.io/4.0/api/io/netty/bootstrap/Bootstrap.html"
 * >Bootstrap</a>s created and provides the ability to customize <a
 * href="http://netty.io/4.0/api/io/netty/channel/ChannelOption.html"
 * >options</a> on a per-bootstrap basis. This allows for easy creation of
 * channels for separate tasks (ie. one for autonomous program state, one for
 * sending and receiving broadcast messages, one for receiving broadcasted video
 * etc.). The defaults chosen should be fine for small to medium numbers of
 * channels with low to medium traffic. An understanding of Netty and Java
 * Sockets is necessary to optimize the parameters for specific tasks.<br>
 * <br>
 * 
 * Bootstraps are either bound to a port (next available or specified) on the
 * host machine or are bound to multicast addresses, both using the UDP protocol
 * for communication. On startup the default InetAddress and default network
 * interface are determined.
 */
public class Bootstrapper {
	private static final Logger log = LoggerFactory.getLogger(Bootstrapper.class);

	private static final int DEFAULT_BOOTSTRAP_KEY = -1;

	/** The Constant INSTANCE. */
	public static final Bootstrapper INSTANCE = new Bootstrapper();

	/** The default interface. */
	public final NetworkInterface DEFAULT_INTERFACE;

	/** The default address. */
	public final InetAddress DEFAULT_ADDRESS;

	private static Map<Integer, Bootstrap> channelBootstraps = new ConcurrentHashMap<Integer, Bootstrap>();
	private static Map<String, Bootstrap> multicastBootstraps = new ConcurrentHashMap<String, Bootstrap>();

	/**
	 * The Constructor, use if subclassing, otherwise {@link #INSTANCE}.
	 */
	public Bootstrapper() {
		try {
			DEFAULT_ADDRESS = InetAddress.getLocalHost();
			DEFAULT_INTERFACE = NetworkInterface.getByInetAddress(DEFAULT_ADDRESS);
		} catch (Exception e) {
			log.error("Could not determine network interface", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Checks for default bootstrap.
	 *
	 * @return true, if checks for default bootstrap
	 */
	public boolean hasDefaultBootstrap() {
		return containsBootstrap(DEFAULT_BOOTSTRAP_KEY);
	}

	/**
	 * Returns true if a bootstrap for the specified port is available.
	 *
	 * @param port
	 *          the port
	 * @return true, if contains bootstrap
	 */
	public boolean containsBootstrap(int port) {
		return channelBootstraps.containsKey(port);
	}

	/**
	 * Returns true if the bootstrap exists for the specified multicast address.
	 *
	 * @param multicast
	 *          the multicast
	 * @return true, if contains multicast bootstrap
	 */
	public boolean containsMulticastBootstrap(InetSocketAddress multicast) {
		String key = createMulticastKey(multicast);

		return containsMulticastBootstrap(key);
	}

	/**
	 * Inits the default bootstrap. This bootstrap is used should no port-specific
	 * bootstrap exist when the {@link #bind()} or {@link #bind(int)} methods are
	 * called.<br>
	 * <br>
	 * 
	 * The channel initializer allows the many Netty handlers to be specified as
	 * required for the bootstrap ie. one for SSL communication, one for message
	 * encryption/decryption etc etc. Refer to the <a
	 * href="http://netty.io">Netty</a> documentation for more information.
	 *
	 * @param <CHANNEL>
	 *          the generic type
	 * @param initializer
	 *          the initializer
	 * @param clazz
	 *          the clazz
	 */
	public <CHANNEL extends DatagramChannel> void initDefaultBootstrap(ChannelInitializer<CHANNEL> initializer,
			Class<? extends CHANNEL> clazz) {
		initBootstrap(initializer, DEFAULT_BOOTSTRAP_KEY, clazz);
	}

	/**
	 * Inits the bootstrap for a specified port. This allows different bootstraps
	 * for different channels, all configured differently.<br>
	 * <br>
	 * 
	 * The channel initializer allows the many Netty handlers to be specified as
	 * required for the bootstrap ie. one for SSL communication, one for message
	 * encryption/decryption etc etc. Refer to the <a
	 * href="http://netty.io">Netty</a> documentation for more information.
	 *
	 * @param <CHANNEL>
	 *          the generic type
	 * @param initializer
	 *          the initializer
	 * @param port
	 *          the port
	 * @param clazz
	 *          the clazz
	 */
	public <CHANNEL extends DatagramChannel> void initBootstrap(ChannelInitializer<CHANNEL> initializer, int port,
			Class<? extends CHANNEL> clazz) {
		if (containsBootstrap(port)) {
			log.warn("Bootstrap for port {} already initialized", port);
			return;
		}

		log.debug("Initializing bootstrap for port {}", port);
		Bootstrap b = bootstrap(initializer, port, clazz);

		channelBootstraps.put(port, b);
	}

	/**
	 * Inits the multicast bootstrap for the specified address.<br>
	 * <br>
	 * 
	 * The channel initializer allows the many Netty handlers to be specified as
	 * required for the bootstrap ie. one for SSL communication, one for message
	 * encryption/decryption etc etc. Refer to the <a
	 * href="http://netty.io">Netty</a> documentation for more information.
	 *
	 * @param <CHANNEL>
	 *          the generic type
	 * @param initializer
	 *          the initializer
	 * @param multicast
	 *          the multicast
	 * @param clazz
	 *          the clazz
	 */
	public <CHANNEL extends DatagramChannel> void initMulticastBootstrap(ChannelInitializer<DatagramChannel> initializer,
			InetSocketAddress multicast, Class<? extends CHANNEL> clazz) {
		initMulticastBootstrap(initializer, multicast, DEFAULT_INTERFACE, clazz);
	}

	/**
	 * Inits the multicast bootstrap for the specified address.<br>
	 * <br>
	 * 
	 * The channel initializer allows the many Netty handlers to be specified as
	 * required for the bootstrap ie. one for SSL communication, one for message
	 * encryption/decryption etc etc. Refer to the <a
	 * href="http://netty.io">Netty</a> documentation for more information.
	 *
	 * @param <CHANNEL>
	 *          the generic type
	 * @param initializer
	 *          the initializer
	 * @param multicast
	 *          the multicast
	 * @param networkInterface
	 *          the network interface
	 * @param clazz
	 *          the clazz
	 */
	public <CHANNEL extends DatagramChannel> void initMulticastBootstrap(ChannelInitializer<DatagramChannel> initializer,
			InetSocketAddress multicast, NetworkInterface networkInterface, Class<? extends CHANNEL> clazz) {
		String key = createMulticastKey(multicast);
		if (containsMulticastBootstrap(key)) {
			log.warn("Multicast bootstrap for {} already initialized", multicast);
			return;
		}

		log.debug("Initializing multicast bootstrap for {} using network interface {}", multicast, networkInterface);

		Bootstrap b = multicastBootstrap(initializer, multicast, networkInterface, clazz);

		multicastBootstraps.put(key, b);
	}

	private String createMulticastKey(InetSocketAddress multicast) {
		return AbstractKiSyMulticastChannel.createMulticastKey(multicast);
	}

	/**
	 * Removes the bootstrap.
	 *
	 * @param port
	 *          the port
	 * @return the bootstrap
	 */
	public Bootstrap removeBootstrap(int port) {
		return channelBootstraps.remove(port);
	}

	/**
	 * Removes the multicast bootstrap.
	 *
	 * @param address
	 *          the address
	 * @return the bootstrap
	 */
	public Bootstrap removeMulticastBootstrap(InetSocketAddress address) {
		return removeMulticastBootstrap(createMulticastKey(address));
	}

	/**
	 * Removes the multicast bootstrap.
	 *
	 * @param key
	 *          the key
	 * @return the bootstrap
	 */
	public Bootstrap removeMulticastBootstrap(String key) {
		return multicastBootstraps.remove(key);
	}

	/**
	 * Gets the bootstrap keys.
	 *
	 * @return the bootstrap keys
	 */
	public Set<Integer> getBootstrapKeys() {
		return channelBootstraps.keySet();
	}

	/**
	 * Gets the bootstraps.
	 *
	 * @return the bootstraps
	 */
	public Collection<Bootstrap> getBootstraps() {
		return channelBootstraps.values();
	}

	/**
	 * Gets the multicast bootstrap keys.
	 *
	 * @return the multicast bootstrap keys
	 */
	public Set<String> getMulticastBootstrapKeys() {
		return multicastBootstraps.keySet();
	}

	/**
	 * Gets the multicast bootstraps.
	 *
	 * @return the multicast bootstraps
	 */
	public Collection<Bootstrap> getMulticastBootstraps() {
		return multicastBootstraps.values();
	}

	/**
	 * Gets the default bootstrap.
	 *
	 * @return the default bootstrap
	 */
	public Bootstrap getDefaultBootstrap() {
		return getBootstrap(DEFAULT_BOOTSTRAP_KEY);
	}

	/**
	 * Gets the bootstrap.
	 *
	 * @param key
	 *          the key
	 * @return the bootstrap
	 */
	public Bootstrap getBootstrap(int key) {
		return channelBootstraps.get(key);
	}

	/**
	 * Gets the multicast bootstrap.
	 *
	 * @param address
	 *          the address
	 * @return the multicast bootstrap
	 */
	public Bootstrap getMulticastBootstrap(InetSocketAddress address) {
		String key = createMulticastKey(address);

		return getMulticastBootstrap(key);
	}

	/**
	 * Gets the multicast bootstrap.
	 *
	 * @param key
	 *          the key
	 * @return the multicast bootstrap
	 */
	public Bootstrap getMulticastBootstrap(String key) {
		return multicastBootstraps.get(key);
	}

	/**
	 * Clear bootstraps.
	 */
	public void clearBootstraps() {
		channelBootstraps.clear();
	}

	/**
	 * Clear multicast bootstraps.
	 */
	public void clearMulticastBootstraps() {
		multicastBootstraps.clear();
	}

	/**
	 * Bind to the next available port on the host machine using the default
	 * bootstrap.
	 *
	 * @param <CHANNEL>
	 *          the generic type
	 * @return the channel
	 */
	public <CHANNEL extends DatagramChannel> CHANNEL bind() {
		return bind(0);
	}

	/**
	 * Returns a channel using the bootstrap for the specified port. Should none
	 * exist then the default bootstrap is used.
	 *
	 * @param <CHANNEL>
	 *          the generic type
	 * @param port
	 *          the port
	 * @return the channel
	 */
	@SuppressWarnings("unchecked")
	public <CHANNEL extends DatagramChannel> CHANNEL bind(int port) {
		boolean contains = containsBootstrap(port);
		if (!contains && !hasDefaultBootstrap()) {
			log.error("Bootstrap for port {} not initialized", port);
			return null;
		}

		Bootstrap b = channelBootstraps.get(contains ? port : DEFAULT_BOOTSTRAP_KEY);

		ChannelFuture cf = b.bind(port);

		CountDownLatch latch = new CountDownLatch(1);
		cf.addListener(getBindListener(port, latch));

		await(latch, "Channel creation timed out");

		return cf.isSuccess() ? (CHANNEL) cf.channel() : null;
	}

	/**
	 * Bind to the specified multicast address.
	 *
	 * @param <CHANNEL>
	 *          the generic type
	 * @param multicast
	 *          the multicast
	 * @return the channel
	 */
	@SuppressWarnings("unchecked")
	public <CHANNEL extends DatagramChannel> CHANNEL multicastBind(InetSocketAddress multicast) {
		String key = createMulticastKey(multicast);
		if (!containsMulticastBootstrap(key)) {
			log.error("Multicast bootstrap for {} not initialized", multicast);
			return null;
		}

		Bootstrap b = multicastBootstraps.get(key);

		ChannelFuture cf = b.bind();

		CountDownLatch latch = new CountDownLatch(1);
		cf.addListener(getMulticastBindListener(multicast, latch));

		await(latch, "Multicast channel creation timed out");

		return cf.isSuccess() ? (CHANNEL) cf.channel() : null;
	}

	private GenericFutureListener<ChannelFuture> getBindListener(final int port, final CountDownLatch latch) {
		return new GenericFutureListener<ChannelFuture>() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				try {
					if (future.isSuccess()) {
						log.debug("Channel creation successful for {}", future.channel());
					} else {
						Throwable cause = future.cause();
						if (cause == null) {
							log.error("Could not create channel for {}", port);
						} else {
							log.error("Could not create channel for {}", port, cause);
						}
					}
				} finally {
					latch.countDown();
				}
			}
		};
	}

	private GenericFutureListener<ChannelFuture> getMulticastBindListener(final InetSocketAddress multicast,
			final CountDownLatch latch) {
		return new GenericFutureListener<ChannelFuture>() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				try {
					if (future.isSuccess()) {
						log.debug("Multicast channel creation successful for {}", multicast);
					} else {
						Throwable cause = future.cause();
						if (cause == null) {
							log.error("Could not create multicast channel for {}", multicast);
						} else {
							log.error("Could not create multicast channel for {}", multicast, cause);
						}
					}
				} finally {
					latch.countDown();
				}
			}
		};
	}

	private void await(CountDownLatch latch, String error) {
		boolean ok = KiSyUtils.await(latch, 5, TimeUnit.SECONDS);
		if (!ok) log.error(error);
	}

	/**
	 * Creates bootstrap for all purposes. Sets default options, event loop group
	 * and channel class. If the port &gt; 0 (and it should be &gt; 1024) then it
	 * is set in the bootstrap (localAddress).
	 *
	 * @param <CHANNEL>
	 *          the generic type
	 * @param initializer
	 *          the initializer
	 * @param port
	 *          the port
	 * @param clazz
	 *          the clazz
	 * @return the bootstrap
	 */
	protected <CHANNEL extends DatagramChannel> Bootstrap bootstrap(ChannelInitializer<CHANNEL> initializer, int port,
			Class<? extends CHANNEL> clazz) {
		Bootstrap b = new Bootstrap();

		setDefaultBootstrapOptions(b, initializer);
		if (port > 0) b.localAddress(port);

		b.group(getEventLoopGroup(clazz));
		b.channel(clazz);

		return b;
	}

	/**
	 * Gets the event loop group based upon the class name. If the class is
	 * neither an <a href=
	 * "http://netty.io/4.0/api/io/netty/channel/socket/nio/NioDatagramChannel.html"
	 * >NioDatagramChannel</a> nor <a href=
	 * "http://netty.io/4.0/api/io/netty/channel/socket/oio/OioDatagramChannel.html"
	 * >OioDatagramChannel</a> (or on Linux, <a href=
	 * "http://netty.io/4.0/api/io/netty/channel/epoll/EpollDatagramChannel.html"
	 * >EpollDatagramChannel</a>) then an exception is thrown.
	 *
	 * @param <CHANNEL>
	 *          the generic type
	 * @param clazz
	 *          the clazz
	 * @return the event loop group
	 */
	protected <CHANNEL extends DatagramChannel> EventLoopGroup getEventLoopGroup(Class<? extends CHANNEL> clazz) {
		if (NioDatagramChannel.class.equals(clazz)) return new NioEventLoopGroup();

		if (OioDatagramChannel.class.equals(clazz)) return new OioEventLoopGroup();

		// Linux only
		if (EpollDatagramChannel.class.equals(clazz)) return new EpollEventLoopGroup();

		throw new UnsupportedOperationException("No default event loop group defined for " + clazz.getName());
	}

	/**
	 * Sets the default bootstrap options (SO_BROADCAST=true, SO_REUSEADDR=true)
	 * and the initializer as the channel handler.
	 *
	 * @param b
	 *          the b
	 * @param initializer
	 *          the initializer
	 */
	protected void setDefaultBootstrapOptions(AbstractBootstrap<?, ?> b, ChannelInitializer<?> initializer) {
		b.option(ChannelOption.SO_BROADCAST, true);
		b.option(ChannelOption.SO_REUSEADDR, true);
		b.handler(initializer);
	}

	/**
	 * Multicast bootstrap using the default network interface.
	 *
	 * @param <CHANNEL>
	 *          the generic type
	 * @param initializer
	 *          the initializer
	 * @param multicast
	 *          the multicast
	 * @param clazz
	 *          the clazz
	 * @return the bootstrap
	 */
	protected <CHANNEL extends DatagramChannel> Bootstrap multicastBootstrap(
			ChannelInitializer<DatagramChannel> initializer, InetSocketAddress multicast, Class<? extends CHANNEL> clazz) {
		return multicastBootstrap(initializer, multicast, DEFAULT_INTERFACE, clazz);
	}

	/**
	 * Multicast bootstrap, adding the IP_MULTICAST_IF=networkInterface option.
	 *
	 * @param <CHANNEL>
	 *          the generic type
	 * @param initializer
	 *          the initializer
	 * @param multicast
	 *          the multicast
	 * @param networkInterface
	 *          the network interface
	 * @param clazz
	 *          the clazz
	 * @return the bootstrap
	 */
	protected <CHANNEL extends DatagramChannel> Bootstrap multicastBootstrap(
			ChannelInitializer<DatagramChannel> initializer, InetSocketAddress multicast, NetworkInterface networkInterface,
			Class<? extends CHANNEL> clazz) {
		Bootstrap b = bootstrap(initializer, multicast.getPort(), clazz);

		b.option(ChannelOption.IP_MULTICAST_IF, networkInterface);

		return b;
	}

	private boolean containsMulticastBootstrap(String key) {
		return multicastBootstraps.containsKey(key);
	}
}
