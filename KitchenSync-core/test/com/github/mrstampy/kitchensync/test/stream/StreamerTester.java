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
package com.github.mrstampy.kitchensync.test.stream;

import io.netty.channel.ChannelFuture;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.kitchensync.message.inbound.ByteArrayInboundMessageManager;
import com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMesssageHandler;
import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;
import com.github.mrstampy.kitchensync.stream.BufferedInputStreamStreamer;
import com.github.mrstampy.kitchensync.stream.EndOfMessageListener;
import com.github.mrstampy.kitchensync.stream.EndOfMessageRegister;
import com.github.mrstampy.kitchensync.stream.FileStreamer;
import com.github.mrstampy.kitchensync.stream.Streamer;
import com.github.mrstampy.kitchensync.stream.StreamerAckRegister;
import com.github.mrstampy.kitchensync.stream.header.SequenceHeader;
import com.github.mrstampy.kitchensync.stream.inbound.EndOfMessageInboundMessageHandler;
import com.github.mrstampy.kitchensync.stream.inbound.StreamAckInboundMessageHandler;
import com.github.mrstampy.kitchensync.test.channel.ByteArrayChannel;
import com.github.mrstampy.kitchensync.util.KiSyUtils;

/**
 * The Class StreamerTester streams a large file requiring
 * {@link Streamer#ackRequired()}. The file is sent 3 times and the results are
 * fully logged at debug level.
 * 
 * @see StreamerAckRegister
 * @see StreamAckInboundMessageHandler
 * @see Streamer#ackRequired()
 * @see Streamer#ackReceived(long)
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
	 * Specify the large file to stream.
	 * 
	 * @param file
	 *          the large file's path and name
	 */
	public StreamerTester(String file) {
		this.file = file;
		init();
	}

	private void init() {
		initInboundManager();

		EndOfMessageRegister.INSTANCE.addEOMListeners(new EndOfMessageListener() {

			@Override
			public boolean isForChannelAndSender(KiSyChannel channel, InetSocketAddress sender) {
				return channel.getPort() == channel2.getPort() && sender.equals(channel1.localAddress());
			}

			@Override
			public void endOfMessage(byte[] eom) {
				log.info("End of Message received");
			}
		});

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
		streamer = getFileStreamer();
		streamer.setConcurrentThreads(3);
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
			log.info("Sent: {}, Received: {}, Packet loss: {} %, Concurrent threads: {}", streamer.size(), received.get(),
					packetLoss.toPlainString(), streamer.getConcurrentThreads());
			streamer.reset();
			received.set(0);

			KiSyUtils.snooze(100);
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
		FileStreamer fs = new FileStreamer(new File(file), channel1, channel2.localAddress());

		fs.setEomOnFinish(true);

		return fs;
	}

	private Streamer<?> getBufferedInputStreamStreamer() throws Exception {
		BufferedInputStreamStreamer biss = new BufferedInputStreamStreamer(new FileInputStream(file), channel1,
				channel2.localAddress());

		biss.setEomOnFinish(true);
		biss.setInputStreamResettable(true);

		return biss;
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
				return !StreamerAckRegister.isAckMessage(message) && !EndOfMessageRegister.INSTANCE.isEndOfMessage(message);
			}

			@Override
			public void messageReceived(byte[] message, KiSyChannel channel, InetSocketAddress sender) throws Exception {
				received.addAndGet(streamer.isProcessChunk() ? message.length - SequenceHeader.HEADER_LENGTH : message.length);

				if (streamer.isAckRequired()) {
					long sumOfBytes = StreamerAckRegister.convertToLong(message);
					channel.send(StreamerAckRegister.createAckResponse(sumOfBytes), sender);
				}

				if (streamer.isProcessChunk()) checkHeader(message);
			}

			private void checkHeader(byte[] message) {
				SequenceHeader header = new SequenceHeader(message);
				if (header.getSequence() % 100000 == 0) log.debug("Received sequence {}", header.getSequence());
			}

			@Override
			public int getExecutionOrder() {
				return 1;
			}

		}, new StreamAckInboundMessageHandler(), new EndOfMessageInboundMessageHandler());
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
			System.out.println("Usage: java com.github.mrstampy.kitchensync.test.stream.StreamerTester [path to large file]");
			System.out.println("where [path to large file] is the user specified path to a large file.");
			System.exit(0);
		}

		StreamerTester tester = new StreamerTester(args[0]);
		tester.message();
	}

}
