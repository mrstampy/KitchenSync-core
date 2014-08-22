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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;
import com.github.mrstampy.kitchensync.stream.footer.Footer;
import com.github.mrstampy.kitchensync.stream.header.ChunkProcessor;

/**
 * The Class AbstractEncapsulatedStreamer interfaces to a {@link Streamer}
 * implementation. A call to {@link #initializeStreamer()} is required by
 * subclasses; the implementation of {@link #createStreamer()} will be invoked.
 * {@link Streamer} interface methods by default delegate to the encapsulated
 * {@link Streamer} and can be overridden as required.
 *
 * @param <MSG>
 *          the generic type
 * @param <STREAMER>
 *          the generic type
 */
public abstract class AbstractEncapsulatedStreamer<MSG, STREAMER extends Streamer<?>> implements Streamer<MSG> {
	private static final Logger log = LoggerFactory.getLogger(AbstractEncapsulatedStreamer.class);

	private STREAMER streamer;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#resetPosition(int)
	 */
	@Override
	public void resetPosition(int newPosition) {
		getStreamer().resetPosition(newPosition);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#stream()
	 */
	@Override
	public ChannelFuture stream() {
		return getStreamer().stream();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#stream(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ChannelFuture stream(MSG message) throws Exception {
		return ((Streamer<MSG>) getStreamer()).stream(message);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#pause()
	 */
	@Override
	public void pause() {
		getStreamer().pause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#size()
	 */
	@Override
	public long size() {
		return getStreamer().size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#remaining()
	 */
	@Override
	public long remaining() {
		return getStreamer().remaining();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#sent()
	 */
	@Override
	public long sent() {
		return getStreamer().sent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isComplete()
	 */
	@Override
	public boolean isComplete() {
		return getStreamer().isComplete();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isStreaming()
	 */
	@Override
	public boolean isStreaming() {
		return getStreamer().isStreaming();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getFuture()
	 */
	@Override
	public ChannelFuture getFuture() {
		return getStreamer().getFuture();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getChannel()
	 */
	@Override
	public KiSyChannel getChannel() {
		return getStreamer().getChannel();
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
		getStreamer().setChannel(channel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getChunkSize()
	 */
	@Override
	public int getChunkSize() {
		return getStreamer().getChunkSize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#setChunkSize(int)
	 */
	@Override
	public void setChunkSize(int chunkSize) {
		getStreamer().setChunkSize(chunkSize);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getDestination()
	 */
	@Override
	public InetSocketAddress getDestination() {
		return getStreamer().getDestination();
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
		getStreamer().setDestination(destination);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#cancel()
	 */
	@Override
	public void cancel() {
		getStreamer().cancel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#reset()
	 */
	@Override
	public void reset() {
		getStreamer().reset();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#resetSequence(long)
	 */
	@Override
	public void resetSequence(long sequence) {
		getStreamer().resetSequence(sequence);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setChunksPerSecond(int)
	 */
	@Override
	public void setChunksPerSecond(int chunksPerSecond) {
		getStreamer().setChunksPerSecond(chunksPerSecond);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getChunksPerSecond()
	 */
	@Override
	public int getChunksPerSecond() {
		return getStreamer().getChunksPerSecond();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isChunksPerSecond()
	 */
	@Override
	public boolean isChunksPerSecond() {
		return getStreamer().isChunksPerSecond();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#fullThrottle()
	 */
	@Override
	public void fullThrottle() {
		getStreamer().fullThrottle();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isFullThrottle()
	 */
	@Override
	public boolean isFullThrottle() {
		return getStreamer().isFullThrottle();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#ackRequired()
	 */
	@Override
	public void ackRequired() {
		getStreamer().ackRequired();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isAckRequired()
	 */
	@Override
	public boolean isAckRequired() {
		return getStreamer().isAckRequired();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#ackReceived(long)
	 */
	@Override
	public void ackReceived(long sumOfBytesInChunk) {
		getStreamer().ackReceived(sumOfBytesInChunk);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setConcurrentThreads(int)
	 */
	@Override
	public void setConcurrentThreads(int concurrentThreads) {
		getStreamer().setConcurrentThreads(concurrentThreads);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getConcurrentThreads()
	 */
	@Override
	public int getConcurrentThreads() {
		return getStreamer().getConcurrentThreads();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getSequence()
	 */
	@Override
	public long getSequence() {
		return getStreamer().getSequence();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isProcessChunk()
	 */
	@Override
	public boolean isProcessChunk() {
		return getStreamer().isProcessChunk();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setProcessChunk(boolean)
	 */
	@Override
	public void setProcessChunk(boolean processChunk) {
		getStreamer().setProcessChunk(processChunk);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getChunkProcessor()
	 */
	@Override
	public ChunkProcessor getChunkProcessor() {
		return getStreamer().getChunkProcessor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setChunkProcessor(com.github
	 * .mrstampy.kitchensync.stream.header.ChunkProcessor)
	 */
	@Override
	public void setChunkProcessor(ChunkProcessor chunkProcessor) {
		getStreamer().setChunkProcessor(chunkProcessor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#isEomOnFinish()
	 */
	@Override
	public boolean isEomOnFinish() {
		return getStreamer().isEomOnFinish();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setEomOnFinish(boolean)
	 */
	@Override
	public void setEomOnFinish(boolean eomOnFinish) {
		getStreamer().setEomOnFinish(eomOnFinish);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#getFooter()
	 */
	@Override
	public Footer getFooter() {
		return getStreamer().getFooter();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.Streamer#setFooter(com.github.mrstampy
	 * .kitchensync.stream.footer.Footer)
	 */
	@Override
	public void setFooter(Footer footer) {
		getStreamer().setFooter(footer);
	}

	/**
	 * Gets the streamer.
	 *
	 * @return the streamer
	 */
	public STREAMER getStreamer() {
		return streamer;
	}

	/**
	 * Initialize the streamer. Must be explicitly invoked. Throws an
	 * {@link IllegalStateException} should the streamer crash and burn in its
	 * creation.
	 * 
	 * @see #createStreamer()
	 */
	protected void initializeStreamer() {
		try {
			this.streamer = createStreamer();
		} catch (Exception e) {
			log.error("Unexpected exception", e);
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Creates the streamer, invoked from the explicit call to
	 * {@link #initializeStreamer()}.
	 *
	 * @return the streamer
	 * @throws Exception
	 *           the exception
	 */
	protected abstract STREAMER createStreamer() throws Exception;

}
