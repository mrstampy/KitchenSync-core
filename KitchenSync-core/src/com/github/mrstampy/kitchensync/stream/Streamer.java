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

import java.net.InetSocketAddress;

import com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMesssageHandler;
import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;
import com.github.mrstampy.kitchensync.stream.footer.Footer;
import com.github.mrstampy.kitchensync.stream.header.ChunkProcessor;
import com.github.mrstampy.kitchensync.stream.header.NoProcessChunkProcessor;
import com.github.mrstampy.kitchensync.stream.inbound.StreamAckInboundMessageHandler;

/**
 * The Interface Streamer defines the methods to stream data to a remote socket
 * in chunks. The default size for chunks is 1024 bytes. Three modes of
 * streaming are supported: {@link #fullThrottle()} (default),
 * {@link #ackRequired()} and {@link #setChunksPerSecond(int)}.
 * {@link #setConcurrentThreads(int)} will set the number of concurrent chunks
 * being sent at any one time.
 *
 * @param <MSG>
 *          the generic type
 */
public interface Streamer<MSG> {

	/** The Constant DEFAULT_CHUNK_SIZE, 2048. */
	public static final int DEFAULT_CHUNK_SIZE = 2048;

	/** Value 2, the default number of concurrent message threads. */
	public static final int DEFAULT_CONCURRENT_THREADS = 2;

	/**
	 * Resets the position in the stream for sending. If invoked while streaming
	 * an {@link IllegalStateException} will be thrown.
	 *
	 * @param newPosition
	 *          the new position
	 */
	void resetPosition(int newPosition);

	/**
	 * Streams the message. The message must be set either in the implementation's
	 * constructor or via the {@link #stream(Object)} method. Will throw an
	 * {@link IllegalStateException} if {@link #isComplete()}.
	 *
	 * @return the channel future
	 */
	ChannelFuture stream();

	/**
	 * Sets the message for streaming and invokes {@link #stream()}.
	 *
	 * @param message
	 *          the message
	 * @return the channel future
	 * @throws Exception
	 *           the exception
	 */
	ChannelFuture stream(MSG message) throws Exception;

	/**
	 * Pauses streaming. Streaming is unpaused when {@link #stream()} is invoked.
	 * If already paused this method will do nothing.
	 */
	void pause();

	/**
	 * Returns the size of the message for streaming.
	 *
	 * @return the long
	 */
	long size();

	/**
	 * Returns the number of bytes left for streaming.
	 *
	 * @return the long
	 */
	long remaining();

	/**
	 * Returns the number of bytes streamed for the message.
	 *
	 * @return the long
	 */
	long sent();

	/**
	 * This method will return true when the current message has been completely
	 * sent, failed with an error or streaming has been cancelled. Further state
	 * information can be obtained from the ChannelFuture associated with the
	 * streaming ({@link #getFuture()}).
	 * 
	 * @return true if complete
	 * @see #getFuture()
	 */
	boolean isComplete();

	/**
	 * Returns true if the Streamer is currently streaming.
	 * 
	 * @return true if streaming
	 */
	boolean isStreaming();

	/**
	 * Gets the future.
	 *
	 * @return the future
	 */
	ChannelFuture getFuture();

	/**
	 * Gets the channel.
	 *
	 * @return the channel
	 */
	KiSyChannel getChannel();

	/**
	 * Sets the channel.
	 *
	 * @param channel
	 *          the channel
	 */
	void setChannel(KiSyChannel channel);

	/**
	 * Returns the size of chunks for streaming.
	 *
	 * @return the chunk size
	 */
	int getChunkSize();

	/**
	 * Sets the chunk size. For best performance when at full throttle the value
	 * should be a power of two.
	 *
	 * @param chunkSize
	 *          the chunk size
	 */
	void setChunkSize(int chunkSize);

	/**
	 * Gets the destination.
	 *
	 * @return the destination
	 */
	InetSocketAddress getDestination();

	/**
	 * Sets the destination.
	 *
	 * @param destination
	 *          the destination
	 */
	void setDestination(InetSocketAddress destination);

	/**
	 * Cancels streaming and {@link #reset()}s.
	 */
	void cancel();

	/**
	 * Resets the position to the start of the message. Will throw an
	 * {@link IllegalStateException} should the message be streaming.
	 */
	void reset();

	/**
	 * Resets the position to the specified sequence, if possible.
	 *
	 * @param sequence
	 *          the sequence
	 */
	void resetSequence(long sequence);

	/**
	 * If this method is invoked prior to either {@link #stream()} or
	 * {@link #stream(Object)} then the message will be sent at the specified
	 * rate. Throws an {@link IllegalStateException} if already streaming.<br>
	 * <br>
	 * 
	 * During testing some packet loss occurs for values &gt; 1000. If message
	 * fidelity is a top priority then {@link #ackRequired()} is more suitable.<br>
	 * <br>
	 * 
	 * Note that this value is multiplicative with {@link #getConcurrentThreads()}
	 * , so that at each sending {@link #getConcurrentThreads()} chunks are sent.
	 *
	 * @param chunksPerSecond
	 *          the chunks per second
	 * @see #fullThrottle()
	 * @see #ackRequired()
	 */
	void setChunksPerSecond(int chunksPerSecond);

	/**
	 * Returns the number of chunks to send per second. Will return -1 if either
	 * {@link #fullThrottle()} or {@link #ackRequired()} has been previously
	 * invoked.
	 * 
	 * @return the chunks per second
	 */
	int getChunksPerSecond();

	/**
	 * Returns true if the value of {@link #getChunksPerSecond()} is greater than
	 * zero.
	 * 
	 * @return true if the mode is chunks per second
	 * @see #isAckRequired()
	 * @see #isFullThrottle()
	 */
	boolean isChunksPerSecond();

	/**
	 * If this method is invoked prior to either {@link #stream()} or
	 * {@link #stream(Object)} then the message will be sent at the maximum speed
	 * possible. This is the default streaming type. Throws an
	 * {@link IllegalStateException} if already streaming.<br>
	 * <br>
	 * 
	 * Packet loss can be expected when at full throttle, on the order of 0.05%
	 * using packets of 2048 bytes. If message fidelity is a top priority then
	 * {@link #ackRequired()} is more suitable.<br>
	 * <br>
	 * 
	 * Throughput at full throttle on a single host using 2048 byte packets is on
	 * the order of 70 megabytes/sec.
	 * 
	 * @see #setChunksPerSecond(int)
	 * @see #ackRequired()
	 */
	void fullThrottle();

	/**
	 * Returns true if the streamer is running at full throttle.
	 *
	 * @return true, if is full throttle
	 */
	boolean isFullThrottle();

	/**
	 * If this method is invoked prior to either {@link #stream()} or
	 * {@link #stream(Object)} then each chunk sent will require an
	 * acknowledgement consisting of the sum of the bytes in the sent chunk. If an
	 * acknowledgement has not been received within 10 seconds the last chunk is
	 * resent, continuing to do so until {@link #ackReceived(long)} or the
	 * streaming is {@link #cancel()}led. Throws an {@link IllegalStateException}
	 * if already streaming.<br>
	 * <br>
	 * 
	 * If speed is a top priority then one of {@link #fullThrottle()} or
	 * {@link #setChunksPerSecond(int)} is more appropriate. And this probably
	 * isn't a good fit for {@link KiSyChannel#isMulticastChannel()}s...<br>
	 * <br>
	 * 
	 * Throughput on a single host using 2048 byte packets is on the order of 40
	 * megabytes/sec using 5 concurrent threads.
	 * 
	 * @see #ackReceived(long)
	 * @see #fullThrottle()
	 * @see #setChunksPerSecond(int)
	 * @see StreamAckInboundMessageHandler
	 */
	void ackRequired();

	/**
	 * Returns true if acknowledgement is required for each chunk sent.
	 *
	 * @return true, if is ack required
	 * @see #ackRequired()
	 */
	boolean isAckRequired();

	/**
	 * This method is to be invoked when {@link #isAckRequired()}. The receiver
	 * will send an acknowledgement consisting of the sum of the bytes in the
	 * chunk received. This method should be invoked by a
	 * {@link KiSyInboundMesssageHandler} on the sender-side. If this method is
	 * not invoked when {@link #isAckRequired()} then the same chunk will be
	 * streamed every second - probably not desired behaviour.
	 *
	 * @param sumOfBytesInChunk
	 *          the sum of bytes in chunk
	 * @see StreamerAckRegister#convertToLong(byte[])
	 */
	void ackReceived(long sumOfBytesInChunk);

	/**
	 * Set the number of concurrent message threads.
	 *
	 * @param concurrentThreads
	 *          the number of concurrent message threads
	 */
	void setConcurrentThreads(int concurrentThreads);

	/**
	 * Returns the number of concurrent message threads.
	 *
	 * @return the number of threads
	 */
	int getConcurrentThreads();

	/**
	 * Gets the current chunk number.
	 *
	 * @return the sequence
	 */
	long getSequence();

	/**
	 * If true then the {@link #getChunkProcessor()} is used to process each chunk
	 * acquired. {@link NoProcessChunkProcessor} is provided for convenience when
	 * this value is false.
	 * 
	 * @return
	 */
	boolean isProcessChunk();

	/**
	 * Set to true to use {@link #getChunkProcessor()} to process each chunk.
	 * 
	 * @param processChunk
	 */
	void setProcessChunk(boolean processChunk);

	/**
	 * Returns the {@link ChunkProcessor} used when {@link #isProcessChunk()}.
	 * 
	 * @return
	 */
	ChunkProcessor getChunkProcessor();

	/**
	 * Sets the {@link ChunkProcessor} used when {@link #isProcessChunk()}.
	 * 
	 * @param chunkProcessor
	 */
	void setChunkProcessor(ChunkProcessor chunkProcessor);

	/**
	 * If true then an end of message message will be sent upon completion of
	 * streaming. Defaults to false.
	 *
	 * @return true, if checks if is eom on finish
	 * @see #sendEndOfMessage()
	 * @see #getFooter()
	 */
	boolean isEomOnFinish();

	/**
	 * Set to true to send an end of message message upon completion of streaming.
	 *
	 * @param eomOnFinish
	 *          the eom on finish
	 * @see #sendEndOfMessage()
	 * @see #getFooter()
	 */
	void setEomOnFinish(boolean eomOnFinish);

	/**
	 * Returns the {@link Footer} used when {@link #isEomOnFinish()}.
	 * 
	 * @return
	 * @see #sendEndOfMessage()
	 */
	Footer getFooter();

	/**
	 * Sets the {@link Footer} to use when {@link #isEomOnFinish()}.
	 * 
	 * @param footer
	 * @see #sendEndOfMessage()
	 */
	void setFooter(Footer footer);

}