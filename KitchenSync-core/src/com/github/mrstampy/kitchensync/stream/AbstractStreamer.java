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
import io.netty.util.concurrent.GenericFutureListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;
import com.github.mrstampy.kitchensync.stream.footer.EndOfMessageFooter;
import com.github.mrstampy.kitchensync.stream.footer.Footer;
import com.github.mrstampy.kitchensync.stream.header.ChunkProcessor;
import com.github.mrstampy.kitchensync.stream.header.NoProcessChunkProcessor;
import com.github.mrstampy.kitchensync.stream.header.SequenceHeaderPrepender;
import com.github.mrstampy.kitchensync.stream.inbound.EndOfMessageInboundMessageHandler;
import com.github.mrstampy.kitchensync.util.KiSyUtils;

/**
 * An abstract implementation of the {@link Streamer} interface.
 *
 * @param <MSG>
 *          the generic type
 */
public abstract class AbstractStreamer<MSG> implements Streamer<MSG> {
	private static final Logger log = LoggerFactory.getLogger(AbstractStreamer.class);

	/** The Constant DEFAULT_ACK_AWAIT, 1 second. */
	public static final int DEFAULT_ACK_AWAIT = 1;

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

	/** The streaming indicator. */
	protected AtomicBoolean streaming = new AtomicBoolean(false);

	/** The complete indicator. */
	protected AtomicBoolean complete = new AtomicBoolean(false);

	/** The sent. */
	protected AtomicLong sent = new AtomicLong(0);

	private long size;

	/** The streamer listener. */
	protected StreamerListener streamerListener = new StreamerListener();

	/** The future. */
	protected StreamerFuture future;

	/** The svc. */
	protected Scheduler svc = Schedulers.from(Executors.newCachedThreadPool());

	/** The active subscription. */
	protected Subscription sub;

	private int chunksPerSecond = -1;

	/** The list of keys awaiting {@link #ackReceived(long)}. */
	protected List<Long> ackKeys = new ArrayList<Long>();

	/** The ack latch. */
	protected CountDownLatch latch;

	/** The start. */
	protected long start;

	/** The type. */
	protected StreamerType type = StreamerType.FULL_THROTTLE;

	private int concurrentThreads = DEFAULT_CONCURRENT_THREADS;

	private int ackAwait = DEFAULT_ACK_AWAIT;
	private TimeUnit ackAwaitUnit = TimeUnit.SECONDS;

	private boolean processChunk = concurrentThreads > 1;

	/** The sequence. */
	protected AtomicLong sequence = new AtomicLong(0);

	private boolean eomOnFinish = false;

	private ChunkProcessor chunkProcessor = new SequenceHeaderPrepender();

	private ChunkProcessor noProcessing = new NoProcessChunkProcessor();

	private Footer footer = new EndOfMessageFooter();

	private int throttle = 0;

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
	 * Gets the chunk with header.
	 *
	 * @return the chunk with header
	 * @throws Exception
	 *           the exception
	 */
	protected byte[] getChunkWithHeader() throws Exception {
		byte[] chunk = getChunk();

		if (chunk == null || chunk.length == 0) return chunk;

		incrementSequence();

		return getEffectiveChunkProcessor().process(this, chunk);
	}

	/**
	 * Returns the effective chunk processor, if {@link #isProcessChunk()} then
	 * {@link #getChunkProcessor()}, else the implementation of
	 * {@link NoProcessChunkProcessor}.
	 *
	 * @return the effective chunk processor
	 */
	protected ChunkProcessor getEffectiveChunkProcessor() {
		return isProcessChunk() ? getChunkProcessor() : noProcessing;
	}

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

		init();
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

		byte[] ackChunk = StreamerAckRegister.getChunk(sumOfBytesInChunk, getChannel().getPort());

		if (ackChunk == null) log.warn("No last chunk to acknowledge");

		if (latch != null) latch.countDown();
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
		streaming.set(true);
		finish(false, null);
	}

	/**
	 * Convenience method for subclasses to create the byte array for the next
	 * chunk given the remaining number of bytes. Factors in the header size if
	 * {@link #isProcessChunk()}.
	 *
	 * @param remaining
	 *          the remaining
	 * @return the byte[]
	 */
	protected byte[] createByteArray(int remaining) {
		int chunkSize = getEffectiveChunkSize();

		chunkSize = remaining > chunkSize ? chunkSize : remaining;

		return new byte[chunkSize];
	}

	/**
	 * The service activated when StreamerType is CHUNKS_PER_SECOND.
	 */
	protected void scheduledService() {
		BigDecimal secondsPerChunk = BigDecimal.ONE.divide(new BigDecimal(chunksPerSecond), 6, RoundingMode.HALF_UP);

		BigDecimal microsPerChunk = secondsPerChunk.multiply(KiSyUtils.ONE_MILLION);

		unsubscribe();
		sub = svc.createWorker().schedulePeriodically(new Action0() {

			@Override
			public void call() {
				if (isStreaming()) {
					fullThrottleServiceImpl();
				}
			}
		}, 0, microsPerChunk.longValue(), TimeUnit.MICROSECONDS);
	}

	/**
	 * The service activated when StreamerType is FULL_THROTTLE.
	 */
	protected void fullThrottleService() {
		unsubscribe();
		if (getThrottle() > 0) {
			sub = svc.createWorker().schedulePeriodically(new Action0() {

				@Override
				public void call() {
					if (isStreaming()) {
						fullThrottleServiceImpl();
					} else {
						sub.unsubscribe();
					}
				}
			}, 0, getThrottle(), TimeUnit.MICROSECONDS);

		} else {
			sub = svc.createWorker().schedule(new Action0() {

				@Override
				public void call() {
					while (isStreaming()) {
						fullThrottleServiceImpl();
					}
				}
			});
		}
	}

	private void fullThrottleServiceImpl() {
		try {
			int latchCount = 0;
			if (isAsync()) {
				latchCount = sendChunksAsync();
			} else {
				byte[] chunk = writeOnce();
				if (chunk != null && chunk.length > 0) latchCount++;
			}

			awaitLatchIfRequired(latchCount);
		} catch (Exception e) {
			log.error("Unexpected exception", e);
			finish(false, e);
		}
	}

	private void awaitLatchIfRequired(int latchCount) {
		if (latchCount > 0 && isFullThrottle()) await(latch, 50, TimeUnit.MILLISECONDS);
	}

	/**
	 * The service activated when StreamerType is ACK_REQUIRED.
	 */
	protected void sendAndAwaitAck() {
		unsubscribe();
		sub = svc.createWorker().schedule(new Action0() {

			@Override
			public void call() {
				try {
					sendAndAwaitAckImpl();
				} catch (Exception e) {
					log.error("Unexpected exception", e);
				}
			}
		});
	}

	private void sendAndAwaitAckImpl() throws Exception {
		if (!isStreaming()) return;

		int count = 0;
		if (isAsync()) {
			count = sendChunksAsync();
		} else {
			byte[] ackChunk = writeOnce();
			if (ackChunk == null) return;
			count++;
		}

		if (count > 0) awaitAck();
	}

	private boolean isAsync() {
		return getConcurrentThreads() > 1;
	}

	private int sendChunksAsync() throws Exception {
		List<byte[]> chunks = new ArrayList<byte[]>();
		for (int i = 0; i < getConcurrentThreads(); i++) {
			byte[] chunk = getChunkWithHeader();

			if (chunk == null || chunk.length == 0) break;

			chunks.add(chunk);
		}

		if (chunks.isEmpty()) {
			pause();
			return 0;
		}

		int latchCount = getLatchCount(chunks);

		if (latchCount > 0) latch = new CountDownLatch(latchCount);
		processChunks(chunks);

		return latchCount;
	}

	/**
	 * The service activated when the last chunk has not been acknowledged.
	 */
	protected void resendLast() {
		if (!isStreaming()) return;
		unsubscribe();
		sub = svc.createWorker().schedule(new Action0() {

			@Override
			public void call() {
				if (!isStreaming() || ackKeys.isEmpty()) return;

				List<byte[]> chunks = new ArrayList<byte[]>();
				for (Long ackKey : ackKeys) {
					byte[] ackChunk = StreamerAckRegister.getChunk(ackKey, getChannel().getPort());
					if (ackChunk == null) {
						log.warn("No last chunk found in the registry");
					} else {
						chunks.add(ackChunk);
						sent.addAndGet(-ackChunk.length);
					}
				}

				int latchCount = getLatchCount(chunks);
				processChunks(chunks);

				if (latchCount > 0) awaitAck();
			}
		});
	}

	/**
	 * The service activated to await acknowledgement and to invoke another send
	 * and wait or a resend of the last message.
	 */
	protected void awaitAck() {
		final boolean ok = await(latch, ackAwait, ackAwaitUnit);

		if (getThrottle() > 0) {
			svc.createWorker().schedule(new Action0() {

				@Override
				public void call() {
					nextAckChunk(ok);
				}
			}, getThrottle(), TimeUnit.MICROSECONDS);
		} else {
			nextAckChunk(ok);
		}
	}

	private void nextAckChunk(final boolean ok) {
		if (ok) {
			sendAndAwaitAck();
		} else {
			log.warn("No ack received for last packet, resending");
			resendLast();
		}
	}

	/**
	 * Unsubscribe.
	 */
	protected void unsubscribe() {
		if (sub != null) sub.unsubscribe();
	}

	/**
	 * Profile.
	 */
	protected void profile() {
		if (!log.isDebugEnabled() || size() == 0) return;

		long elapsed = System.nanoTime() - start;

		BigDecimal megabytesPerSecond = new BigDecimal(size()).multiply(new BigDecimal(1000)).divide(
				new BigDecimal(elapsed), 3, RoundingMode.HALF_UP);

		log.debug("{} bytes sent in {} ms at a rate of {} megabytes/sec", size(), KiSyUtils.toMillis(elapsed),
				megabytesPerSecond.toPlainString());
	}

	/**
	 * Write once.
	 *
	 * @return the byte[]
	 */
	protected byte[] writeOnce() {
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
		byte[] chunk = getChunkWithHeader();

		if (!isStreaming()) return null;

		if (chunk != null && chunk.length > 0) latch = new CountDownLatch(1);

		processChunk(chunk);

		return chunk;
	}

	/**
	 * Process chunks.
	 *
	 * @param chunks
	 *          the chunks
	 */
	protected void processChunks(List<byte[]> chunks) {
		Observable.from(chunks, svc).subscribe(new Action1<byte[]>() {

			@Override
			public void call(byte[] t1) {
				if (isStreaming()) processChunk(t1);
			}
		});
	}

	/**
	 * Gets the latch count.
	 *
	 * @param chunks
	 *          the chunks
	 * @return the latch count
	 */
	protected int getLatchCount(List<byte[]> chunks) {
		int i = 0;
		for (byte[] b : chunks) {
			if (b.length > 0) i++;
		}

		return i;
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
			finish(true, null);
		} else {
			sendChunk(chunk);
		}

		KiSyUtils.snooze(0); // necessary for packet fidelity when @ full throttle
	}

	/**
	 * Writes the bytes to the {@link #getChannel()}. Invoked from
	 * {@link #processChunk(byte[])}, override if necessary.
	 *
	 * @param chunk
	 *          the chunk
	 */
	protected void sendChunk(byte[] chunk) {
		if (isAckRequired()) ackKeys.add(StreamerAckRegister.add(chunk, this));

		ChannelFuture cf = channel.send(chunk, destination);

		if (isFullThrottle()) cf.addListener(streamerListener);

		sent.addAndGet(chunk.length - getEffectiveChunkProcessor().sizeInBytes(this));
	}

	/**
	 * Implementation method to send an end of message message.
	 * 
	 * @see EndOfMessageRegister
	 * @see EndOfMessageInboundMessageHandler
	 * @see EndOfMessageListener
	 * @see #isEomOnFinish()
	 * @see #setEomOnFinish(boolean)
	 * @see #setFooter(Footer)
	 */
	public void sendEndOfMessage() {
		await(latch, 100, TimeUnit.MILLISECONDS);
		latch = new CountDownLatch(1);
		ChannelFuture cf = channel.send(getFooter().createFooter(), destination);
		cf.addListener(streamerListener);
		await(latch, 50, TimeUnit.MILLISECONDS);
	}

	/**
	 * Initialzes the state of the streamer.
	 */
	protected void init() {
		sent.set(0);
		sequence.set(0);
		complete.set(false);
		unsubscribe();
		countdownLatch();
		resetChunkProcessor();
		resetFooter();
	}

	/**
	 * Counts down {@link #latch}.
	 */
	protected void countdownLatch() {
		if (latch != null) latch.countDown();
	}

	/**
	 * Invoked when streaming is complete, an error has occurred or streaming has
	 * been {@link #cancel()}led.
	 *
	 * @param success
	 *          the success
	 * @param t
	 *          the exception, null if not applicable
	 */
	protected synchronized void finish(boolean success, Throwable t) {
		if (!isStreaming()) return;
		streaming.set(false);
		complete.set(true);

		if (isEomOnFinish()) sendEndOfMessage();

		future.finished(success, t);
		future = new StreamerFuture(this);

		if (latch != null) {
			long count = latch.getCount();
			for (long i = 0; i < count; i++) {
				latch.countDown();
			}
			latch = null;
		}
		unsubscribe();
		profile();
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getConcurrentThreads()
	 */
	public int getConcurrentThreads() {
		return concurrentThreads;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setConcurrentThreads(int)
	 */
	public void setConcurrentThreads(int concurrentThreads) {
		if (concurrentThreads < 1) throw new IllegalArgumentException("Must be > 0: " + concurrentThreads);
		this.concurrentThreads = concurrentThreads;

		if (concurrentThreads > 1) setProcessChunk(true);
	}

	/**
	 * If true then {@link #getChunkProcessor()} is used to process chunks, else
	 * the implementation of {@link NoProcessChunkProcessor}.
	 *
	 * @return true, if checks if is process chunk
	 */
	public boolean isProcessChunk() {
		return processChunk;
	}

	/**
	 * If set to true then {@link #getChunkProcessor()} is used to process chunks,
	 * else the implementation of {@link NoProcessChunkProcessor}. Throws an
	 * {@link IllegalStateException} should {@link Streamer#isStreaming()}.
	 *
	 * @param processChunk
	 *          the process chunk
	 */
	public void setProcessChunk(boolean processChunk) {
		if (isStreaming()) throw new IllegalStateException("Cannot change header state when streaming");
		this.processChunk = processChunk;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getSequence()
	 */
	public long getSequence() {
		return sequence.get();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#resetSequence(long)
	 */
	public void resetSequence(long sequence) {
		if (isStreaming()) throw new IllegalStateException("Cannot reset sequence when streaming");

		BigDecimal bd = new BigDecimal(getEffectiveChunkSize()).multiply(new BigDecimal(sequence));

		resetPosition(bd.intValue());
	}

	private int getEffectiveChunkSize() {
		return getChunkSize() - getEffectiveChunkProcessor().sizeInBytes(this);
	}

	/**
	 * Reset sequence from position.
	 *
	 * @param position
	 *          the position
	 */
	protected void resetSequenceFromPosition(int position) {
		sent.set(position);

		if (position == 0) {
			sequence.set(0);
			return;
		}

		BigDecimal bd = new BigDecimal(position).divide(new BigDecimal(getEffectiveChunkSize()), 3, RoundingMode.HALF_UP);

		sequence.set(bd.longValue());
	}

	/**
	 * Next sequence.
	 *
	 */
	protected void incrementSequence() {
		sequence.incrementAndGet();
	}

	/**
	 * Sets the time to await acknowledgements of {@link #isAckRequired()}
	 * messages before resending. Defaults to 1 second.
	 * 
	 * @param time
	 *          the value
	 * @param unit
	 *          the units
	 */
	public void setAckAwait(int time, TimeUnit unit) {
		if (time < 0) throw new IllegalArgumentException("Ack Await must be >= 0: " + time);
		if (unit == null) throw new IllegalArgumentException("unit must be specified");
		this.ackAwait = time;
		this.ackAwaitUnit = unit;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isEomOnFinish()
	 */
	public boolean isEomOnFinish() {
		return eomOnFinish;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setEomOnFinish(boolean)
	 */
	public void setEomOnFinish(boolean eomOnFinish) {
		this.eomOnFinish = eomOnFinish;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getFooter()
	 */
	public Footer getFooter() {
		return footer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setFooter(com.github.mrstampy
	 * .kitchensync.stream.footer.Footer)
	 */
	public void setFooter(Footer footer) {
		this.footer = footer;
	}

	/**
	 * The Class StreamerListener.
	 */
	protected class StreamerListener implements GenericFutureListener<ChannelFuture> {

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

			if (latch != null) latch.countDown();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getChunkProcessor()
	 */
	public ChunkProcessor getChunkProcessor() {
		return chunkProcessor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setChunkProcessor(com.github
	 * .mrstampy.kitchensync.stream.header.ChunkProcessor)
	 */
	public void setChunkProcessor(ChunkProcessor chunkProcessor) {
		this.chunkProcessor = chunkProcessor;
	}

	/**
	 * Invoked from {@link #init()} and invokes {@link ChunkProcessor#reset()}.
	 */
	protected void resetChunkProcessor() {
		if (getChunkProcessor() == null) return;

		getChunkProcessor().reset();
	}

	/**
	 * Invoked from {@link #init()} and invokes {@link Footer#reset()}.
	 */
	protected void resetFooter() {
		if (getFooter() == null) return;

		getFooter().reset();
	}

	/**
	 * Returns the microseconds to wait between receiving an acknowledgement and
	 * sending the next chunk, or between sending chunks when at
	 * {@link #fullThrottle()}. Values &le; 0 indicate no throttling. Default of
	 * zero.
	 * 
	 * @return the value used to throttle
	 * @see #isAckRequired()
	 * @see #awaitAck()
	 */
	public int getThrottle() {
		return throttle;
	}

	/**
	 * Sets the microseconds to wait between receiving an acknowledgement and
	 * sending the next chunk, or between sending chunks when at
	 * {@link #fullThrottle()}. Values &le; 0 indicate no throttling. Setting
	 * while streaming at full throttle has no effect.
	 * 
	 * @param throttle
	 *          the value used to throttle
	 * @see #isAckRequired()
	 * @see #awaitAck()
	 */
	public void setThrottle(int throttle) {
		if (isStreaming() && isFullThrottle()) {
			log.warn("Setting the throttle while streaming at full throttle has no effect");
		}

		this.throttle = throttle;
	}

}