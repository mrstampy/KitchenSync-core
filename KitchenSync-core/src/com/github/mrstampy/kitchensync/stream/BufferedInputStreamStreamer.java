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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;
import com.github.mrstampy.kitchensync.util.KiSyUtils;

/**
 * This implementation of the {@link Streamer} interface streams the content of
 * the specified {@link InputStream} to the remote destination. The
 * {@link InputStream} is wrapped by a {@link BufferedInputStream} if necessary
 * for efficiency in reading from the stream.
 */
public class BufferedInputStreamStreamer extends AbstractStreamer<InputStream> {
	private static final Logger log = LoggerFactory.getLogger(BufferedInputStreamStreamer.class);

	/** The in. */
	protected BufferedInputStream in;

	private boolean finishOnEmptyStream = true;

	private boolean inputStreamResettable;

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
	 * @return the chunk
	 * @throws Exception
	 *           the exception
	 * @see com.github.mrstampy.kitchensync.stream.AbstractStreamer#getChunk()
	 */
	protected byte[] getChunk() throws Exception {
		int remaining = (int) remaining();

		if (remaining == 0 && isFinishOnEmptyStream()) finish(true, null);

		if (remaining <= 0) return null;

		// Blue sky
		byte[] chunk = createByteArray(remaining);

		in.read(chunk);

		return chunk;
	}

	/**
	 * Not implemented.
	 *
	 * @param newPosition
	 *          the new position
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
			if (markSupported()) in.reset();
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
		in.mark((int) size());
	}

	/**
	 * Overriding to prevent finishing on an empty chunk when not
	 * {@link #isFinishOnEmptyStream()}.
	 *
	 * @param chunk
	 *          the chunk
	 */
	protected void processChunk(byte[] chunk) {
		if (isPausible(chunk)) {
			pause();
		} else if (chunk.length > 0) {
			sendChunk(chunk);
		} else if (isFinishOnEmptyStream()) {
			finish(true, null);
		}

		KiSyUtils.snooze(0); // necessary for packet fidelity when @ full throttle
	}

	private boolean isPausible(byte[] chunk) {
		return chunk == null || (chunk.length == 0 && !isFinishOnEmptyStream());
	}

	private boolean markSupported() {
		return in != null && in.markSupported() && isInputStreamResettable();
	}

	/**
	 * Returns true if this {@link Streamer} finalizes when the input stream is
	 * empty. If false then finalization will occur via a call to
	 * {@link #cancel()} or on error.
	 * 
	 * @return true if empty stream indicates the end of message
	 */
	public boolean isFinishOnEmptyStream() {
		return finishOnEmptyStream;
	}

	/**
	 * Set to false to pause sending if the stream is currently empty. If true
	 * (the default) the message finalization will occur when the stream is empty.
	 * If false then {@link #setInputStreamResettable(boolean)} is also set to
	 * false.
	 * 
	 * @param finishOnEmptyStream
	 *          true if empty stream indicates the end of message
	 */
	public void setFinishOnEmptyStream(boolean finishOnEmptyStream) {
		this.finishOnEmptyStream = finishOnEmptyStream;

		if (!finishOnEmptyStream) setInputStreamResettable(false);
	}

	/**
	 * If true then {@link InputStream#mark(int)} and {@link InputStream#reset()}
	 * will be invoked appropriately on the input stream.
	 *
	 * @return true, if checks if is input stream resettable
	 */
	public boolean isInputStreamResettable() {
		return inputStreamResettable;
	}

	/**
	 * If set to false will prevent {@link InputStream#mark(int)} and
	 * {@link InputStream#reset()} from being invoked.
	 *
	 * @param inputStreamResettable
	 *          the input stream resettable
	 * @see #isFinishOnEmptyStream()
	 */
	public void setInputStreamResettable(boolean inputStreamResettable) {
		this.inputStreamResettable = inputStreamResettable;
	}

}
