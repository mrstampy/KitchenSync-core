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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;

/**
 * This implementation of the {@link Streamer} interface streams the content of
 * the specified {@link InputStream} to the remote destination. The
 * {@link InputStream} is wrapped by a {@link BufferedInputStream} if necessary
 * for efficiency in reading from the stream.
 */
public class BufferedInputStreamStreamer extends AbstractStreamer<InputStream> {
	private static final Logger log = LoggerFactory.getLogger(BufferedInputStreamStreamer.class);

	protected BufferedInputStream in;

	protected Scheduler svc = Schedulers.from(Executors.newSingleThreadExecutor());
	protected Subscription sub;
	protected CountDownLatch latch;

	private boolean finishOnEmptyStream = true;

	/**
	 * The Constructor.
	 *
	 * @param in
	 *          the input stream
	 * @param channel
	 *          the channel
	 * @param destination
	 *          the destination
	 * @throws Exception
	 *           the exception
	 */
	public BufferedInputStreamStreamer(InputStream in, KiSyChannel channel, InetSocketAddress destination)
			throws Exception {
		this(in, channel, destination, DEFAULT_CHUNK_SIZE);
	}

	/**
	 * The Constructor.
	 *
	 * @param in
	 *          the input stream
	 * @param channel
	 *          the channel
	 * @param destination
	 *          the destination
	 * @param chunkSize
	 *          the chunk size
	 * @throws Exception
	 *           the exception
	 */
	public BufferedInputStreamStreamer(InputStream in, KiSyChannel channel, InetSocketAddress destination, int chunkSize)
			throws Exception {
		super(channel, destination, chunkSize);

		prepareForSend(in);
	}

	/**
	 * If not {@link #isFinishOnEmptyStream()} then if
	 * {@link #isChunksPerSecond()} the streaming is paused, else this method
	 * returns when bytes are next available from the input stream. If
	 * {@link #isChunksPerSecond()} streaming is automatically restarted when
	 * bytes are available.
	 * 
	 * By default if {@link InputStream#available()} returns 0 then finalization
	 * logic is executed.
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.AbstractStreamer#getChunk()
	 */
	protected byte[] getChunk() throws Exception {
		int remaining = (int) remaining();

		if (remaining == 0 && !isFinishOnEmptyStream()) {
			startWaitForMore();

			// stop the scheduler
			if (isChunksPerSecond()) return null;

			// wait till bytes are available, streaming is cancelled
			// or an error occurs
			latch = new CountDownLatch(1);
			latch.await();

			remaining = (int) remaining();
		}

		// Error occurred getting remaining.
		// finalization logic already invoked.
		if (remaining == -1) return null;

		// Blue sky
		byte[] chunk = createByteArray(remaining);

		in.read(chunk);

		return chunk;
	}

	/**
	 * Not implemented.
	 */
	@Override
	public void resetPosition(int newPosition) {
		// not implemented for BufferedInputStreamStreamer
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#reset()
	 */
	@Override
	public void reset() {
		if (isStreaming()) throw new IllegalStateException("Cannot reset when streaming");

		try {
			in.reset();
			init();
		} catch (IOException e) {
			log.error("Could not reset the input stream", e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#remaining()
	 */
	@Override
	public long remaining() {
		try {
			return in.available();
		} catch (IOException e) {
			if (isStreaming()) {
				log.error("Unexpected exception", e);
				finish(false, e);
			}
		}

		return -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.AbstractStreamer#prepareForSend(
	 * java.lang.Object)
	 */
	@Override
	protected void prepareForSend(InputStream message) throws IOException {
		in = (message instanceof BufferedInputStream) ? (BufferedInputStream) message : new BufferedInputStream(message);

		setSize(in.available());
		init();
	}

	/**
	 * Initializes state for the start of streaming.
	 */
	protected void init() {
		in.mark((int) size());
		sent.set(0);
		complete.set(false);
		unsubscribe();
		countdownLatch();
	}

	/**
	 * Unsubscribes from {@link #sub} when not {@link #isFinishOnEmptyStream()}.
	 */
	protected void unsubscribe() {
		if (sub != null) sub.unsubscribe();
	}

	/**
	 * Counts down {@link #latch} when not {@link #isFinishOnEmptyStream()}.
	 */
	protected void countdownLatch() {
		if (latch != null) latch.countDown();
	}

	/**
	 * Starts the wait for more bytes thread when not {@link #isFinishOnEmptyStream()}
	 */
	protected void startWaitForMore() {
		sub = svc.createWorker().schedulePeriodically(new Action0() {

			@Override
			public void call() {
				long remaining = remaining();
				if (remaining == 0) return;

				countdownLatch();
				if (isChunksPerSecond() && !isComplete()) stream();
				unsubscribe();
			}
		}, 10, 10, TimeUnit.MILLISECONDS);
	}

	/**
	 * Returns true if this {@link Streamer} finalizes when the input stream is
	 * empty. If false then finalization will occur via a call to
	 * {@link #cancel()} or on error.
	 * 
	 * @return
	 */
	public boolean isFinishOnEmptyStream() {
		return finishOnEmptyStream;
	}

	/**
	 * Set to false to pause sending if the stream is currently empty. If true
	 * (the default) the message finalization will occur when the stream is empty.
	 */
	public void setFinishOnEmptyStream(boolean finishOnEmptyStream) {
		this.finishOnEmptyStream = finishOnEmptyStream;
	}

}
