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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;
import com.github.mrstampy.kitchensync.stream.inbound.EndOfMessageInboundMessageHandler;
import com.github.mrstampy.kitchensync.util.KiSyUtils;
import com.github.mrstampy.kitchensync.util.ResettingLatch;

/**
 * Encapsulates a {@link BufferedInputStreamStreamer} and uses an encapsulated
 * {@link PipedInputStream} to provide data for the {@link Streamer}. Interface
 * methods delegate to the encapsulated {@link Streamer} as appropriate.
 * 
 * @author burton
 *
 */
public class ByteArrayStreamer extends AbstractEncapsulatedStreamer<byte[], BufferedInputStreamStreamer> {
	private static final Logger log = LoggerFactory.getLogger(ByteArrayStreamer.class);

	/** The Constant PIPE_SIZE. */
	public static final int PIPE_SIZE = 1024 * 2000; // 2 mb

	private BufferedInputStream inputStream;
	private BufferedOutputStream outputStream;

	private AtomicBoolean cancelled = new AtomicBoolean(false);
	private AtomicBoolean eom = new AtomicBoolean(false);

	private Scheduler svc = Schedulers.from(Executors.newFixedThreadPool(3));

	private ResettingLatch latch = new ResettingLatch();
	private ResettingLatch eomLatch = new ResettingLatch();
	private ResettingLatch flushLatch = new ResettingLatch();

	private int pipeSize = PIPE_SIZE;
	private int halfPipeSize = pipeSize / 2;

	private int waitTime = 10;
	private TimeUnit waitUnits = TimeUnit.SECONDS;

	private KiSyChannel channel;
	private InetSocketAddress destination;

	/**
	 * The Constructor, using the default {@value #PIPE_SIZE} byte pipe.
	 *
	 * @param channel
	 *          the channel
	 * @param destination
	 *          the destination
	 * @throws Exception
	 *           the exception
	 */
	public ByteArrayStreamer(KiSyChannel channel, InetSocketAddress destination) throws Exception {
		this(channel, destination, PIPE_SIZE);
	}

	/**
	 * The Constructor.
	 * 
	 * @param channel
	 *          the channel
	 * @param destination
	 *          the destination
	 * @param pipeSize
	 *          the pipeSize
	 * @throws Exception
	 *           the exception
	 */
	public ByteArrayStreamer(KiSyChannel channel, InetSocketAddress destination, int pipeSize) throws Exception {
		init(channel, destination, pipeSize);
	}

	/**
	 * Exposed to facilitate reuse.
	 * 
	 * @param channel
	 *          the channel
	 * @param destination
	 *          the destination
	 * @param pipeSize
	 *          the pipeSize
	 * @throws Exception
	 *           the exception
	 */
	public void init(KiSyChannel channel, InetSocketAddress destination, int pipeSize) throws Exception {
		this.pipeSize = pipeSize;
		halfPipeSize = pipeSize / 2;

		init(channel, destination);
	}

	/**
	 * Exposed to facilitate resuse.
	 *
	 * @param channel
	 *          the channel
	 * @param destination
	 *          the destination
	 * @throws Exception
	 *           the exception
	 */
	public void init(KiSyChannel channel, InetSocketAddress destination) throws Exception {
		log.debug("Initializing streamer from {} to {}", channel.localAddress(), destination);

		this.channel = channel;
		this.destination = destination;

		if (inputStream != null) inputStream.close();
		PipedInputStream pis = new PipedInputStream(getPipeSize());
		inputStream = new BufferedInputStream(pis);

		if (outputStream != null) outputStream.close();
		outputStream = new BufferedOutputStream(new PipedOutputStream(pis));

		if (getStreamer() != null) cancel();
		initializeStreamer();

		cancelled.set(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.AbstractEncapsulatedStreamer#
	 * createStreamer()
	 */
	@Override
	protected BufferedInputStreamStreamer createStreamer() throws Exception {
		BufferedInputStreamStreamer streamer = new BufferedInputStreamStreamer(inputStream, channel, destination);
		streamer.setFinishOnEmptyStream(false);

		return streamer;
	}

	/**
	 * Delegates to {@link #sendMessage(byte[])} via a single threaded executor.
	 * The {@link ChannelFuture} returned is specific for the message supplied and
	 * will indicate completion when the given message has been fully sent or an
	 * error has occurred.
	 *
	 * @param message
	 *          the message
	 * @return the channel future
	 * @throws Exception
	 *           the exception
	 */
	@Override
	public ChannelFuture stream(final byte[] message) throws Exception {
		final StreamerFuture sf = new StreamerFuture(this);

		if (eom.get()) {
			getStreamer().init();
			eom.set(false);
		}

		if (message == null) {
			log.warn("Null message ignored");
			sf.finished(false);
		} else {
			svc.createWorker().schedule(new Action0() {

				@Override
				public void call() {
					try {
						sendMessage(message);
						sf.finished(!cancelled.get());
					} catch (IOException e) {
						sf.finished(false, e);
						log.error("Unexpected exception", e);
					} finally {
						pause();
					}
				}
			});
		}

		return sf;
	}

	/**
	 * Writes the message in up to {@link #getPipeSize()} / 2 chunks and awaits
	 * until {@link PipedInputStream#available()} returns a value &le;
	 * {@link #getPipeSize()} / 2 for each chunk to apply a constant positive
	 * pressure to the encapsulated {@link BufferedInputStreamStreamer}.
	 *
	 * @param message
	 *          the message
	 * @throws IOException
	 *           the IO exception
	 */
	protected void sendMessage(byte[] message) throws IOException {
		try {
			addToOutputStream(message);

			if (isEomOnFinish()) {
				startAvailableCheck();
				awaitEom();
				sendEndOfMessage();
			}
		} finally {
			latch.countDown();
		}
	}

	private void addToOutputStream(byte[] message) throws IOException {
		int messageLength = message.length;

		for (int from = 0; from < messageLength; from += halfPipeSize) {
			if (cancelled.get()) return;

			int to = getTo(messageLength, from);

			writeAndFlush(Arrays.copyOfRange(message, from, to));
			awaitFlush();
		}
	}

	private void awaitEom() {
		boolean ok = eomLatch.await(10, TimeUnit.MILLISECONDS);
		while (!ok) {
			log.warn("Message await not ok");
			ok = eomLatch.await(10, TimeUnit.MILLISECONDS);
		}
	}

	private void startAvailableCheck() {
		svc.createWorker().schedule(new Action0() {

			@Override
			public void call() {
				try {
					int available = inputStream.available();
					while (available > 0) {
						KiSyUtils.snooze(1);
						if (!isStreaming()) stream();
						available = inputStream.available();
					}
				} catch (IOException e) {
					log.error("Unexpected exception", e);
				} finally {
					eomLatch.countDown();
				}
			}
		});
	}

	private int getTo(int messageLength, int from) {
		int to = from + halfPipeSize;

		return to > messageLength ? messageLength : to;
	}

	/**
	 * Write and flush.
	 *
	 * @param chunk
	 *          the chunk
	 * @throws IOException
	 *           the IO exception
	 */
	protected void writeAndFlush(byte[] chunk) throws IOException {
		outputStream.write(chunk);
		outputStream.flush();
	}

	/**
	 * Awaits for the {@link PipedInputStream#available()} to return a value &le;
	 * {@link #getPipeSize()} / 2. Times out after 10 seconds and if timed out
	 * will recurse until {@link #cancel()}led or successful completion.
	 *
	 * @throws IOException
	 *           the IO exception
	 */
	protected void awaitFlush() throws IOException {
		Subscription sub = startAwaitThread();
		boolean ok = flushLatch.await(waitTime, waitUnits);
		sub.unsubscribe();

		if (!ok && !cancelled.get()) {
			log.warn("Cannot finish sending after 10 seconds, retrying");
			awaitFlush();
		}
	}

	/**
	 * Start await thread for the {@link PipedInputStream} to be cleared by the
	 * {@link Streamer}.
	 *
	 * @return the subscription
	 */
	protected Subscription startAwaitThread() {
		return Schedulers.computation().createWorker().schedulePeriodically(new Action0() {

			@Override
			public void call() {
				try {
					if (inputStream.available() > 0) {
						if (!isStreaming()) stream();
						return;
					}
				} catch (IOException e) {
					log.error("Unexpected exception", e);
				}

				flushLatch.countDown();
			}
		}, 0, 5, TimeUnit.MILLISECONDS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#cancel()
	 */
	@Override
	public void cancel() {
		getStreamer().cancel();
		cancelled.set(true);
	}

	/**
	 * Not implemented.
	 *
	 * @return the long
	 */
	@Override
	public long size() {
		return -1;
	}

	/**
	 * Exposing throttling.
	 *
	 * @return the microseconds to throttle between chunks
	 * @see AbstractStreamer#getThrottle()
	 */
	public int getThrottle() {
		return getStreamer().getThrottle();
	}

	/**
	 * Exposing throttling.
	 *
	 * @param throttle
	 *          the microseconds to throttle between chunks
	 * @see AbstractStreamer#setThrottle(int)
	 */
	public void setThrottle(int throttle) {
		getStreamer().setThrottle(throttle);
	}

	/**
	 * Send end of message.
	 * 
	 * @see EndOfMessageRegister
	 * @see EndOfMessageListener
	 * @see EndOfMessageInboundMessageHandler
	 */
	public void sendEndOfMessage() {
		latch.await(100, TimeUnit.MILLISECONDS);

		getStreamer().sendEndOfMessage();

		eom.set(true);
	}

	/**
	 * Returns the size of the pipe used to move bytes around.
	 * 
	 * @return the size of the pipe
	 * @see #PIPE_SIZE
	 * @see #init(KiSyChannel, InetSocketAddress, int)
	 */
	public int getPipeSize() {
		return pipeSize;
	}

	/**
	 * Specifies the amount of time to wait until the next {@link #getPipeSize()}
	 * / 2 chunk can be written to the output stream. Should the wait time out the
	 * {@link #awaitFlush()} method is recursively called. Defaults to 10 seconds.
	 * 
	 * @param waitTime
	 *          the value to wait
	 * @param waitUnits
	 *          the units to wait
	 */
	public void setWaitTime(int waitTime, TimeUnit waitUnits) {
		if (waitTime < 0) throw new IllegalArgumentException("wait time must be >= 0: " + waitTime);
		if (waitUnits == null) throw new IllegalArgumentException("Units must be specified");
		this.waitTime = waitTime;
		this.waitUnits = waitUnits;
	}

}
