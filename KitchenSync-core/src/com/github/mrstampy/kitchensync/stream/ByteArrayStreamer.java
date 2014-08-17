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

import static com.github.mrstampy.kitchensync.util.KiSyUtils.await;
import io.netty.channel.ChannelFuture;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
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

/**
 * Encapsulates a {@link BufferedInputStreamStreamer} and uses an encapsulated
 * {@link PipedInputStream} to provide data for the {@link Streamer}. Interface
 * methods delegate to the encapsulated {@link Streamer} as appropriate.
 * 
 * @author burton
 *
 */
public class ByteArrayStreamer implements Streamer<byte[]> {
	private static final Logger log = LoggerFactory.getLogger(ByteArrayStreamer.class);

	/** The Constant PIPE_SIZE. */
	public static final int PIPE_SIZE = 1024 * 2000; // 2 mb

	private BufferedInputStreamStreamer streamer;
	private BufferedInputStream inputStream;
	private BufferedOutputStream outputStream;

	private AtomicBoolean cancelled = new AtomicBoolean(false);
	private AtomicBoolean eom = new AtomicBoolean(false);

	private Scheduler svc = Schedulers.from(Executors.newSingleThreadExecutor());

	private CountDownLatch latch;

	private int pipeSize = PIPE_SIZE;
	private int halfPipeSize = pipeSize / 2;

	/**
	 * The Constructor, using a pipe size of {@value #PIPE_SIZE}.
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
	 * @param destination
	 * @param pipeSize
	 * @throws Exception
	 */
	public ByteArrayStreamer(KiSyChannel channel, InetSocketAddress destination, int pipeSize) throws Exception {
		init(channel, destination, pipeSize);
	}

	/**
	 * Exposed to facilitate reuse.
	 * 
	 * @param channel
	 * @param destination
	 * @param pipeSize
	 * @throws Exception
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

		if (inputStream != null) inputStream.close();
		PipedInputStream pis = new PipedInputStream(getPipeSize());
		inputStream = new BufferedInputStream(pis);

		if (outputStream != null) outputStream.close();
		outputStream = new BufferedOutputStream(new PipedOutputStream(pis));

		if (streamer != null && streamer.isStreaming()) cancel();
		streamer = new BufferedInputStreamStreamer(inputStream, channel, destination);
		streamer.setFinishOnEmptyStream(false);

		cancelled.set(false);
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
			streamer.init();
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
		int messageLength = message.length;
		latch = new CountDownLatch(1);

		try {
			for (int from = 0; from < messageLength; from += halfPipeSize) {
				if (cancelled.get()) return;

				int to = getTo(messageLength, from);

				writeAndFlush(Arrays.copyOfRange(message, from, to));
			}
		} finally {
			latch.countDown();
		}
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

		if (!isStreaming()) stream();

		awaitFlush();
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
		CountDownLatch cdl = new CountDownLatch(1);
		Subscription sub = startAwaitThread(cdl);
		boolean ok = await(cdl, 10, TimeUnit.SECONDS);
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
	 * @param cdl
	 *          the cdl
	 * @return the subscription
	 */
	protected Subscription startAwaitThread(final CountDownLatch cdl) {
		return Schedulers.computation().createWorker().schedulePeriodically(new Action0() {

			@Override
			public void call() {
				try {
					if (inputStream.available() > halfPipeSize) return;
				} catch (IOException e) {
					log.error("Unexpected exception", e);
				}

				cdl.countDown();
			}
		}, 0, 5, TimeUnit.MILLISECONDS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#pause()
	 */
	@Override
	public void pause() {
		streamer.pause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#ackRequired()
	 */
	@Override
	public void ackRequired() {
		streamer.ackRequired();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#setChunkSize(int)
	 */
	@Override
	public void setChunkSize(int chunkSize) {
		streamer.setChunkSize(chunkSize);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#fullThrottle()
	 */
	@Override
	public void fullThrottle() {
		streamer.fullThrottle();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#cancel()
	 */
	@Override
	public void cancel() {
		streamer.cancel();
		cancelled.set(true);
	}

	/**
	 * Not implemented.
	 *
	 * @param newPosition
	 *          the new position
	 */
	@Override
	public void resetPosition(int newPosition) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#stream()
	 */
	@Override
	public ChannelFuture stream() {
		return streamer.stream();
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
	 * The value returned is the current number of bytes within the pipe, will be
	 * a value &lt; 2 mb.
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#remaining()
	 */
	@Override
	public long remaining() {
		return streamer.remaining();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#sent()
	 */
	@Override
	public long sent() {
		return streamer.sent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isComplete()
	 */
	@Override
	public boolean isComplete() {
		return streamer.isComplete();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isStreaming()
	 */
	@Override
	public boolean isStreaming() {
		return streamer.isStreaming();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getFuture()
	 */
	@Override
	public ChannelFuture getFuture() {
		return streamer.getFuture();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getChannel()
	 */
	@Override
	public KiSyChannel getChannel() {
		return streamer.getChannel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setChannel(com.github.mrstampy
	 * .kitchensync.netty.channel.KiSyChannel)
	 */
	@Override
	public void setChannel(KiSyChannel channel) {
		streamer.setChannel(channel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getChunkSize()
	 */
	@Override
	public int getChunkSize() {
		return streamer.getChunkSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getDestination()
	 */
	@Override
	public InetSocketAddress getDestination() {
		return streamer.getDestination();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setDestination(java.net
	 * .InetSocketAddress)
	 */
	@Override
	public void setDestination(InetSocketAddress destination) {
		streamer.setDestination(destination);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#reset()
	 */
	@Override
	public void reset() {
		streamer.reset();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#resetSequence(long)
	 */
	@Override
	public void resetSequence(long sequence) {
		streamer.resetSequence(sequence);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setChunksPerSecond(int)
	 */
	@Override
	public void setChunksPerSecond(int chunksPerSecond) {
		streamer.setChunksPerSecond(chunksPerSecond);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getChunksPerSecond()
	 */
	@Override
	public int getChunksPerSecond() {
		return streamer.getChunksPerSecond();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isChunksPerSecond()
	 */
	@Override
	public boolean isChunksPerSecond() {
		return streamer.isChunksPerSecond();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isFullThrottle()
	 */
	@Override
	public boolean isFullThrottle() {
		return streamer.isFullThrottle();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isAckRequired()
	 */
	@Override
	public boolean isAckRequired() {
		return streamer.isAckRequired();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#ackReceived(long)
	 */
	@Override
	public void ackReceived(long sumOfBytesInChunk) {
		streamer.ackReceived(sumOfBytesInChunk);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setConcurrentThreads(int)
	 */
	@Override
	public void setConcurrentThreads(int concurrentThreads) {
		streamer.setConcurrentThreads(concurrentThreads);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getConcurrentThreads()
	 */
	@Override
	public int getConcurrentThreads() {
		return streamer.getConcurrentThreads();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#setUseHeader(boolean)
	 */
	@Override
	public void setUseHeader(boolean useHeader) {
		streamer.setUseHeader(useHeader);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isUseHeader()
	 */
	@Override
	public boolean isUseHeader() {
		return streamer.isUseHeader();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getSequence()
	 */
	@Override
	public long getSequence() {
		return streamer.getSequence();
	}

	/**
	 * Send end of message.
	 * 
	 * @see EndOfMessageRegister
	 * @see EndOfMessageListener
	 * @see EndOfMessageInboundMessageHandler
	 */
	public void sendEndOfMessage() {
		await(latch, 100, TimeUnit.MILLISECONDS);

		streamer.sendEndOfMessage();

		eom.set(true);
	}

	/**
	 * Returns the size of the pipe used to move bytes around.
	 * 
	 * @return
	 */
	public int getPipeSize() {
		return pipeSize;
	}

}
