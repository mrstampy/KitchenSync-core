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
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.kitchensync.message.outbound.KiSyOutboundMessageManager;
import com.github.mrstampy.kitchensync.netty.Bootstrapper;
import com.github.mrstampy.kitchensync.netty.channel.payload.ByteBufCreator;

/**
 * Abstract superclass for {@link KiSyChannel} implementations. Note that the
 * first AbstractKiSyChannel instantiated determines the channel initializer for
 * the default bootstrap. To set it explicitly call
 * {@link Bootstrapper#initDefaultBootstrap(ChannelInitializer, Class)} prior to
 * instantiating instances of this class.
 *
 * @param <BBC>
 *          the {@link ByteBufCreator} implementation
 * @param <CI>
 *          the ChannelInitializer implementation
 * @param <DC>
 *          the DatagramChannel implementaion
 */
public abstract class AbstractKiSyChannel<BBC extends ByteBufCreator, CI extends ChannelInitializer<DatagramChannel>, DC extends DatagramChannel>
		implements KiSyChannel {
	private static final Logger log = LoggerFactory.getLogger(AbstractKiSyChannel.class);

	/** The send future. */
	protected static GenericFutureListener<ChannelFuture> SEND_FUTURE = new GenericFutureListener<ChannelFuture>() {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (future.isSuccess()) {
				log.trace("Message successfully sent");
			} else {
				Throwable cause = future.cause();
				if (cause == null) {
					log.error("Could not send message");
				} else {
					log.error("Could not send message", cause);
				}
			}
		}
	};

	/** The connect future. */
	protected static GenericFutureListener<ChannelFuture> CONNECT_FUTURE = new GenericFutureListener<ChannelFuture>() {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (future.isSuccess()) {
				log.debug("Connection to {} successful", future.channel().remoteAddress());
			} else {
				Throwable cause = future.cause();
				if (cause == null) {
					log.error("Could not connect");
				} else {
					log.error("Could not connect", cause);
				}
			}
		}
	};

	/** The close future. */
	protected static GenericFutureListener<ChannelFuture> CLOSE_FUTURE = new GenericFutureListener<ChannelFuture>() {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (future.isSuccess()) {
				log.trace("Channel successfully closed");
			} else {
				Throwable cause = future.cause();
				if (cause == null) {
					log.error("Could not close channel");
				} else {
					log.error("Could not close channel", cause);
				}
			}
		}
	};

	/** The registry. */
	protected DefaultChannelRegistry registry = DefaultChannelRegistry.INSTANCE;

	/** The outbound manager. */
	protected KiSyOutboundMessageManager outboundManager = KiSyOutboundMessageManager.INSTANCE;

	private DC channel;

	/** The bootstrapper. */
	protected Bootstrapper bootstrapper;

	private AtomicBoolean active = new AtomicBoolean(false);

	/**
	 * Implementations return the <a
	 * href="http://netty.io/4.0/api/io/netty/channel/ChannelInitializer.html"
	 * >channel initializer</a> required for this channel. Netty <a
	 * href="http://netty.io/4.0/api/io/netty/channel/ChannelHandler.html">Channel
	 * handlers</a> should be added to the channel's pipeline to control its
	 * behaviour ie. <a
	 * href="http://netty.io/4.0/api/io/netty/handler/ssl/SslHandler.html"
	 * >SslHandler<a> can be added for SSL encryption.
	 * <p>
	 * 
	 * Note that the default bootstrap will use the ChannelInitializer from the
	 * first instantiated {@link AbstractKiSyChannel}, unless it has been
	 * explicitly initialized prior.
	 * 
	 * @return the channel initializer< datagram channel>
	 */
	protected abstract CI initializer();

	/**
	 * Gets the channel class.
	 *
	 * @return the channel class
	 */
	protected abstract Class<DC> getChannelClass();

	private BBC byteBufCreator;

	/**
	 * The Constructor.
	 */
	public AbstractKiSyChannel() {
		setBootstrapper(Bootstrapper.INSTANCE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.netty.channel.KiSyChannel#isActive()
	 */
	@Override
	public boolean isActive() {
		return active.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.netty.channel.KiSyChannel#bind()
	 */
	@Override
	public void bind() {
		bindDefaultBootstrapInit();

		DC channel = bootstrapper.bind();

		setChannel(channel);

		registry.addChannel(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.netty.channel.KiSyChannel#bind(int)
	 */
	@Override
	public void bind(int port) {
		bindDefaultBootstrapInit();

		DC channel = bootstrapper.bind(port);

		setChannel(channel);

		registry.addChannel(this);
	}

	/**
	 * Prebind.
	 */
	protected void prebind() {
		if (getByteBufCreator() == null) throw new IllegalStateException("The ByteBuf creator must be set");
	}

	/**
	 * Creates a DatagramPacket for sending down the socket from the specified
	 * message.
	 *
	 * @param <MSG>
	 *          the generic type
	 * @param message
	 *          the message
	 * @param address
	 *          the address
	 * @return the object
	 * @see ByteBufCreator
	 * @see #setByteBufCreator(ByteBufCreator)
	 * @see #usesDefaultByteBufCreators()
	 */
	protected <MSG extends Object> DatagramPacket createMessage(MSG message, InetSocketAddress address) {
		ByteBufCreator creator = getByteBufCreator();

		if (creator == null) {
			log.error("Cannot determine ByteBufCreator for message {}, recipient {}", message, address);
			return null;
		}

		return new DatagramPacket(creator.createByteBuf(message, address), address);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.netty.channel.KiSyChannel#send(java.lang
	 * .Object, java.net.InetSocketAddress)
	 */
	@Override
	public <MSG extends Object> ChannelFuture send(MSG message, InetSocketAddress address) {
		presend(message, address);
		DatagramPacket msg = createMessage(message, address);
		return sendImpl(msg, address);
	}

	/**
	 * Presend, invoked prior to {@link #sendImpl(Object, InetSocketAddress)}.
	 *
	 * @param <MSG>
	 *          the generic type
	 * @param message
	 *          the message
	 * @param address
	 *          the address
	 * @see KiSyOutboundMessageManager
	 */
	protected <MSG extends Object> void presend(MSG message, InetSocketAddress address) {
		outboundManager.presend(message, localAddress(), address);
	}

	/**
	 * Send impl.
	 *
	 * @param dp
	 *          the dp
	 * @param address
	 *          the address
	 * @return the channel future
	 */
	protected ChannelFuture sendImpl(DatagramPacket dp, InetSocketAddress address) {
		if (!isActive()) {
			log.error("Channel is not active, cannot send {} to {}", dp, address);
			return new KiSyFailedFuture();
		}

		if (dp == null) {
			log.error("No message to send to {}", address);
			return new KiSyFailedFuture();
		}

		ChannelFuture cf = getChannel().writeAndFlush(dp);

		cf.addListener(SEND_FUTURE);

		return cf;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.netty.channel.KiSyChannel#getPort()
	 */
	public int getPort() {
		return isActive() ? getChannel().localAddress().getPort() : -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.netty.channel.KiSyChannel#close()
	 */
	@Override
	public ChannelFuture close() {
		if (!isActive()) return new KiSyFailedFuture();

		ChannelFuture cf = getChannel().close();
		cf.addListener(CLOSE_FUTURE);

		return cf;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.netty.channel.KiSyChannel#getChannel()
	 */
	@Override
	public DatagramChannel getChannel() {
		return channel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.netty.channel.KiSyChannel#localAddress()
	 */
	public InetSocketAddress localAddress() {
		return new InetSocketAddress(bootstrapper.DEFAULT_ADDRESS, getPort());
	}

	/**
	 * Sets the channel.
	 *
	 * @param channel
	 *          the channel
	 */
	protected void setChannel(DC channel) {
		this.channel = channel;

		channel.closeFuture().addListener(new GenericFutureListener<ChannelFuture>() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				registry.removeChannel(AbstractKiSyChannel.this);
				active.set(false);
			}
		});

		active.set(true);
	}

	/**
	 * Bind default bootstrap init.
	 */
	protected void bindDefaultBootstrapInit() {
		prebind();

		if (isActive()) closeChannel();
		if (!bootstrapper.hasDefaultBootstrap()) bootstrapper.initDefaultBootstrap(initializer(), getChannelClass());
	}

	/**
	 * Close channel.
	 */
	protected void closeChannel() {
		ChannelFuture cf = close();
		final CountDownLatch latch = new CountDownLatch(1);
		cf.addListener(new GenericFutureListener<ChannelFuture>() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				latch.countDown();
			}
		});

		await(latch, "Channel close timed out");
	}

	/**
	 * Await.
	 *
	 * @param latch
	 *          the latch
	 * @param error
	 *          the error
	 */
	protected void await(CountDownLatch latch, String error) {
		try {
			boolean ok = latch.await(5, TimeUnit.SECONDS);
			if (!ok) log.error(error);
		} catch (InterruptedException e) {
			log.error("Unexpected interruption for {}", error, e);
		}
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
		return false;
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
		return false;
	}

	/**
	 * Gets the bootstrapper.
	 *
	 * @return the bootstrapper
	 */
	public Bootstrapper getBootstrapper() {
		return bootstrapper;
	}

	/**
	 * Sets the bootstrapper.
	 *
	 * @param bootstrapper
	 *          the bootstrapper
	 */
	public void setBootstrapper(Bootstrapper bootstrapper) {
		this.bootstrapper = bootstrapper;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.netty.channel.KiSyChannel#getByteBufCreator
	 * ()
	 */
	public BBC getByteBufCreator() {
		if (byteBufCreator == null) byteBufCreator = initByteBufCreator();

		return byteBufCreator;
	}

	/**
	 * Inits the byte buf creator.
	 *
	 * @return the bbc
	 */
	protected abstract BBC initByteBufCreator();

	/**
	 * Sets the byte buf creator.
	 *
	 * @param byteBufCreator
	 *          the byte buf creator
	 */
	public void setByteBufCreator(BBC byteBufCreator) {
		this.byteBufCreator = byteBufCreator;
	}

}
