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

import io.netty.channel.ChannelFuture;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.kitchensync.message.inbound.ByteArrayInboundMessageManager;
import com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMesssageHandler;
import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;
import com.github.mrstampy.kitchensync.test.channel.ByteArrayChannel;
import com.github.mrstampy.kitchensync.util.KiSyUtils;

/**
 * The Class StreamerTester streams a large file requiring
 * {@link Streamer#ackRequired()}. The file is sent 3 times and the results are
 * fully logged at debug level.
 */
@SuppressWarnings("unused")
public class StreamerTester {
	private static final Logger log = LoggerFactory.getLogger(StreamerTester.class);

	private KiSyChannel channel1;
	private KiSyChannel channel2;

	private ScheduledExecutorService svc = Executors.newSingleThreadScheduledExecutor();

	private AtomicLong received = new AtomicLong(0);

	private Streamer<?> streamer;

	private String file;

	/**
	 * The Constructor.
	 */
	public StreamerTester(String file) {
		this.file = file;
		init();
	}

	private void init() {
		initInboundManager();

		channel1 = initChannel();
		channel2 = initChannel();
	}

	/**
	 * Message.
	 *
	 * @throws Exception
	 *           the exception
	 */
	public void message() throws Exception {
		streamer = getBufferedInputStreamStreamer();
		// streamer.setChunksPerSecond(10000);
		streamer.ackRequired();
		startMonitorService();

		stream();

		System.exit(0);
	}

	/**
	 * Stream.
	 *
	 * @throws InterruptedException
	 *           the interrupted exception
	 */
	protected void stream() throws InterruptedException {
		for (int i = 0; i < 3; i++) {
			ChannelFuture future = streamer.stream();
			future.awaitUninterruptibly();

			log.info("Success? {}", future.isSuccess());
			BigDecimal packetLoss = (BigDecimal.ONE.subtract(new BigDecimal(received.get()).divide(
					new BigDecimal(streamer.size()), 6, RoundingMode.HALF_UP))).multiply(new BigDecimal(100));
			log.info("Sent: {}, Received: {}, Packet loss: {} %", streamer.size(), received.get(), packetLoss.toPlainString());

			streamer.reset();
			received.set(0);
		}

		Thread.sleep(50);
	}

	private void startMonitorService() {
		svc.scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				log.info("{} bytes received, {} bytes sent", received.get(), streamer.sent());
				log.info("{} bytes remaining", streamer.remaining());
			}
		}, 5, 5, TimeUnit.SECONDS);
	}

	private Streamer<?> getFileStreamer() throws Exception {
		return new FileStreamer(new File(file), channel1, channel2.localAddress(), 2048);
	}

	private Streamer<?> getBufferedInputStreamStreamer() throws Exception {
		return new BufferedInputStreamStreamer(new FileInputStream(file), channel1, channel2.localAddress(), 2048);
	}

	private KiSyChannel initChannel() {
		ByteArrayChannel channel = new ByteArrayChannel();

		channel.bind();

		return channel;
	}

	@SuppressWarnings("serial")
	private void initInboundManager() {
		ByteArrayInboundMessageManager.INSTANCE.addMessageHandlers(new KiSyInboundMesssageHandler<byte[]>() {

			@Override
			public boolean canHandleMessage(byte[] message) {
				return true;
			}

			@Override
			public void messageReceived(byte[] message, KiSyChannel channel, InetSocketAddress sender) throws Exception {
				received.addAndGet(message.length);
				streamer.ackReceived(KiSyUtils.convertToInt(message));
			}

			@Override
			public int getExecutionOrder() {
				return 0;
			}

		});
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *          the args
	 * @throws Exception
	 *           the exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Usage: java com.github.mrstampy.kitchensync.stream.StreamerTester [path to large file]");
			System.out.println("where [path to large file] is the user specified path to a large file.");
			System.exit(0);
		}

		StreamerTester tester = new StreamerTester(args[0]);
		tester.message();
	}

}
