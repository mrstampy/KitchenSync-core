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
import io.netty.util.concurrent.GenericFutureListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;
import com.github.mrstampy.kitchensync.util.KiSyUtils;

/**
 * An abstract implementation of the {@link Streamer} interface.
 *
 * @param <MSG>
 *          the generic type
 */
public abstract class AbstractStreamer<MSG> implements Streamer<MSG> {
	private static final Logger log = LoggerFactory.getLogger(AbstractStreamer.class);

	/**
	 * The Enum StreamerType.
	 */
	//@formatter:off
	protected enum StreamerType {
		FULL_THROTTLE,
		CHUNKS_PER_SECOND,
		ACK_REQUIRED;
	}
	//@formatter:on

	private KiSyChannel channel;
	private int chunkSize;
	private InetSocketAddress destination;

	/** The streaming. */
	protected AtomicBoolean streaming = new AtomicBoolean(false);

	protected AtomicBoolean complete = new AtomicBoolean(false);

	/** The sent. */
	protected AtomicLong sent = new AtomicLong(0);

	private long size;

	/** The streamer listener. */
	protected StreamerListener streamerListener = new StreamerListener();

	/** The future. */
	protected StreamerFuture future;

	private Scheduler svc = Schedulers.from(Executors.newSingleThreadExecutor());
	private Subscription sub;

	/**
	 * The array returned when {@link #remaining()} &ge; {@link #getChunkSize()}.
	 */
	protected byte[] chunkArray;

	private int chunksPerSecond = -1;

	protected Long ackKey;
	protected CountDownLatch ackLatch;

	protected long start;

	/** The type. */
	protected StreamerType type = StreamerType.FULL_THROTTLE;

	/**
	 * The Constructor.
	 *
	 * @param channel
	 *          the channel
	 * @param destination
	 *          the destination
	 * @param chunkSize
	 *          the chunk size
	 */
	protected AbstractStreamer(KiSyChannel channel, InetSocketAddress destination, int chunkSize) {
		setChannel(channel);
		setChunkSize(chunkSize);
		setDestination(destination);
		future = new StreamerFuture(this);
		chunkArray = new byte[chunkSize];
	}

	/**
	 * Implement to return the next byte array chunk from the message. An empty
	 * array indicates the end of the stream while a null value indicates the
	 * streaming should automatically pause.
	 *
	 * @return the chunk
	 * @throws Exception
	 *           the exception
	 * @see #processChunk(byte[])
	 */
	protected abstract byte[] getChunk() throws Exception;

	/**
	 * Prepare the specified message for streaming.
	 *
	 * @param message
	 *          the message
	 * @throws Exception
	 *           the exception
	 */
	protected abstract void prepareForSend(MSG message) throws Exception;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#pause()
	 */
	@Override
	public void pause() {
		if (!isStreaming()) return;
		if (sub != null) sub.unsubscribe();
		streaming.set(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#size()
	 */
	@Override
	public long size() {
		return size;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#sent()
	 */
	@Override
	public long sent() {
		return sent.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getFuture()
	 */
	@Override
	public ChannelFuture getFuture() {
		return future;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isComplete()
	 */
	@Override
	public boolean isComplete() {
		return complete.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isStreaming()
	 */
	@Override
	public boolean isStreaming() {
		return streaming.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#cancel()
	 */
	@Override
	public void cancel() {
		if (isStreaming()) {
			pause();
			reset();
		}

		cancelFuture();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#stream(java.lang.Object)
	 */
	@Override
	public ChannelFuture stream(MSG message) throws Exception {
		if (isStreaming()) throw new IllegalStateException("Cannot send message, already streaming");

		complete.set(false);
		prepareForSend(message);

		return stream();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#stream()
	 */
	@Override
	public ChannelFuture stream() {
		if (isStreaming()) return getFuture();
		if (isComplete()) throw new IllegalStateException("Streaming is complete");

		streaming.set(true);
		if (sent() == 0) start = System.nanoTime();

		switch (type) {
		case ACK_REQUIRED:
			// Necessary for multiple use in testing
			KiSyUtils.snooze(0);
			sendAndAwaitAck();
			break;
		case CHUNKS_PER_SECOND:
			scheduledService();
			break;
		case FULL_THROTTLE:
			fullThrottleService();
			break;
		default:
			break;
		}

		return getFuture();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#fullThrottle()
	 */
	@Override
	public void fullThrottle() {
		if (isStreaming()) throw new IllegalStateException("Cannot switch to full throttle when streaming");
		this.chunksPerSecond = -1;
		type = StreamerType.FULL_THROTTLE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isFullThrottle()
	 */
	@Override
	public boolean isFullThrottle() {
		return type == StreamerType.FULL_THROTTLE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setChunksPerSecond(int)
	 */
	@Override
	public void setChunksPerSecond(int chunksPerSecond) {
		if (isStreaming()) throw new IllegalStateException("Cannot set chunksPerSecond when streaming");
		if (chunksPerSecond <= 0) throw new IllegalArgumentException("chunksPerSecond must be > 0: " + chunksPerSecond);

		this.chunksPerSecond = chunksPerSecond;

		type = StreamerType.CHUNKS_PER_SECOND;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getChunksPerSecond()
	 */
	@Override
	public int getChunksPerSecond() {
		return chunksPerSecond;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isChunksPerSecond()
	 */
	@Override
	public boolean isChunksPerSecond() {
		return getChunksPerSecond() > 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#ackRequired()
	 */
	@Override
	public void ackRequired() {
		if (isStreaming()) throw new IllegalStateException("Cannot set ackRequired when streaming");

		if (getChannel().isMulticastChannel()) {
			log.warn("Requiring acknowledgement for multicast messages.");
		}

		this.chunksPerSecond = -1;
		type = StreamerType.ACK_REQUIRED;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isAckRequired()
	 */
	@Override
	public boolean isAckRequired() {
		return type == StreamerType.ACK_REQUIRED;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#ackReceived(int)
	 */
	@Override
	public void ackReceived(long sumOfBytesInChunk) {
		if (!isAckRequired()) return;

		byte[] ackChunk = StreamerAckRegister.getChunk(sumOfBytesInChunk);

		if (ackChunk == null) {
			log.warn("No last chunk to acknowledge");
			return;
		}

		ackLatch.countDown();
	}

	/**
	 * Sets the size.
	 *
	 * @param size
	 *          the size
	 */
	protected void setSize(long size) {
		this.size = size;
	}

	/**
	 * Cancel future.
	 */
	protected void cancelFuture() {
		future.setCancelled(true);
		finish(false, null);
	}

	/**
	 * Convenience method for subclasses to create the byte array for the next
	 * chunk given the remaining number of bytes.
	 *
	 * @param remaining
	 *          the remaining
	 * @return the byte[]
	 */
	protected byte[] createByteArray(int remaining) {
		return remaining > getChunkSize() ? chunkArray : new byte[remaining];
	}

	// The service activated when StreamerType is CHUNKS_PER_SECOND
	private void scheduledService() {
		BigDecimal secondsPerChunk = BigDecimal.ONE.divide(new BigDecimal(chunksPerSecond), 6, RoundingMode.HALF_UP);

		BigDecimal microsPerChunk = secondsPerChunk.multiply(KiSyUtils.ONE_MILLION);

		unsubscribe();
		sub = svc.createWorker().schedulePeriodically(new Action0() {

			@Override
			public void call() {
				if (isStreaming()) {
					writeOnce();
				}
			}
		}, 0, microsPerChunk.longValue(), TimeUnit.MICROSECONDS);
	}

	// The service activated when StreamerType is FULL_THROTTLE
	private void fullThrottleService() {
		unsubscribe();
		sub = svc.createWorker().schedule(new Action0() {

			@Override
			public void call() {
				while (isStreaming()) {
					writeOnce();
				}
			}
		});
	}

	// The service activated when StreamerType is ACK_REQUIRED
	private void sendAndAwaitAck() {
		unsubscribe();
		sub = svc.createWorker().schedule(new Action0() {

			@Override
			public void call() {
				if (!isStreaming()) return;

				byte[] ackChunk = writeOnce();

				if (ackChunk == null) return;

				awaitAck();
			}
		});
	}

	// The service activated when the last chunk has not been acknowledged
	private void resendLast() {
		unsubscribe();
		sub = svc.createWorker().schedule(new Action0() {

			@Override
			public void call() {
				if (!isStreaming()) return;
				
				byte[] ackChunk = StreamerAckRegister.getChunk(ackKey);

				if(ackChunk == null) {
					log.error("No last chunk found in the registry");
					return;
				}
				
				sent.addAndGet(-ackChunk.length);

				processChunk(ackChunk);

				awaitAck();
			}
		});
	}

	// The service activated to await acknowledgement and to invoke another send
	// and wait or a resend of the last message
	private void awaitAck() {
		unsubscribe();
		sub = svc.createWorker().schedule(new Action0() {

			@Override
			public void call() {
				boolean ok = false;
				try {
					ok = ackLatch.await(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
				}

				if (ok) {
					sendAndAwaitAck();
				} else {
					log.warn("No ack received for last packet, resending");
					resendLast();
				}
			}
		});
	}

	private void unsubscribe() {
		if (sub != null) sub.unsubscribe();
	}

	/**
	 * Profile.
	 */
	protected void profile() {
		if (!log.isDebugEnabled()) return;

		long elapsed = System.nanoTime() - start;

		BigDecimal megabytesPerSecond = new BigDecimal(size()).multiply(new BigDecimal(1000)).divide(
				new BigDecimal(elapsed), 3, RoundingMode.HALF_UP);

		log.debug("{} bytes sent in {} ms at a rate of {} megabytes/sec", size(), KiSyUtils.toMillis(elapsed),
				megabytesPerSecond.toPlainString());
	}

	private byte[] writeOnce() {
		try {
			return writeImpl();
		} catch (Exception e) {
			log.error("Unexpected exception", e);
			pause();
			finish(false, e);
			return null;
		}
	}

	private byte[] writeImpl() throws Exception {
		byte[] chunk = getChunk();

		if (!isStreaming()) return null;

		processChunk(chunk);

		return chunk;
	}

	/**
	 * Sends the chunk to the destination with two trigger value function
	 * exceptions. A null value will {@link #pause()} streaming and return. A call
	 * to {@link #stream()} will resume streaming at the point of pause. An empty
	 * (length == 0) array indicates that streaming is complete and will invoke
	 * finalization around message streaming.<br>
	 * <br>
	 * 
	 * These functions are triggered from the value returned from the
	 * implementation of {@link #getChunk()}. Override as necessary.
	 *
	 * @param chunk
	 *          the chunk
	 */
	protected void processChunk(byte[] chunk) {
		// null chunk == autopause
		if (chunk == null) {
			pause();
			return;
		}

		// empty chunk == EOM
		if (chunk.length == 0) {
			pause();
			finish(true, null);
			profile();
		} else {
			try {
				sendChunk(chunk);
			} catch (InterruptedException e) {
			}
		}

		KiSyUtils.snooze(0); // necessary for packet fidelity when @ full throttle
	}

	/**
	 * Writes the bytes to the {@link #getChannel()}. Invoked from
	 * {@link #processChunk(byte[])}, override if necessary.
	 * 
	 * @param chunk
	 * @throws InterruptedException
	 */
	protected void sendChunk(byte[] chunk) throws InterruptedException {
		if (isAckRequired()) {
			ackKey = StreamerAckRegister.add(chunk, this);
			ackLatch = new CountDownLatch(1);
		}

		ChannelFuture cf = channel.send(chunk, destination);

		if (isFullThrottle()) {
			CountDownLatch latch = new CountDownLatch(1);
			streamerListener.latch = latch;
			cf.addListener(streamerListener);

			latch.await(1, TimeUnit.SECONDS);
		}

		sent.addAndGet(chunk.length);
	}

	/**
	 * Invoked when streaming is complete, an error has occurred or streaming has
	 * been {@link #cancel()}led.
	 * 
	 * @param success
	 * @param t
	 *          the exception, null if not applicable
	 */
	protected void finish(boolean success, Throwable t) {
		streaming.set(false);
		complete.set(true);
		future.finished(success, t);
		future = new StreamerFuture(this);

		if (ackLatch != null) {
			ackLatch.countDown();
			ackLatch = null;
		}
		unsubscribe();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getChannel()
	 */
	@Override
	public KiSyChannel getChannel() {
		return channel;
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
		this.channel = channel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getChunkSize()
	 */
	@Override
	public int getChunkSize() {
		return chunkSize;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#setChunkSize(int)
	 */
	@Override
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getDestination()
	 */
	@Override
	public InetSocketAddress getDestination() {
		return destination;
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
		this.destination = destination;
	}

	/**
	 * The Class StreamerListener.
	 */
	protected class StreamerListener implements GenericFutureListener<ChannelFuture> {

		/** The latch. */
		CountDownLatch latch;

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * io.netty.util.concurrent.GenericFutureListener#operationComplete(io.netty
		 * .util.concurrent.Future)
		 */
		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (!future.isSuccess()) pause();

			latch.countDown();
		}

	}

}