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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;

/**
 * This implementation of the {@link Streamer} interface streams arbitrarily
 * large files to the specified destination. While the same functionality can be
 * accomplished using a {@link FileInputStream} with the
 * {@link BufferedInputStreamStreamer}, testing has shown this implementation to
 * be 5-10% faster at {@link #fullThrottle()}.
 */
public class FileStreamer extends AbstractStreamer<File> {

	private MappedByteBuffer mappedByteBuffer;

	/**
	 * The Constructor.
	 *
	 * @param file
	 *          the file
	 * @param channel
	 *          the channel
	 * @param destination
	 *          the destination
	 * @throws Exception
	 *           the exception
	 */
	public FileStreamer(File file, KiSyChannel channel, InetSocketAddress destination) throws Exception {
		this(file, channel, destination, DEFAULT_CHUNK_SIZE);
	}

	/**
	 * The Constructor.
	 *
	 * @param file
	 *          the file
	 * @param channel
	 *          the channel
	 * @param destination
	 *          the destination
	 * @param chunkSize
	 *          the chunk size
	 * @throws Exception
	 *           the exception
	 */
	public FileStreamer(File file, KiSyChannel channel, InetSocketAddress destination, int chunkSize) throws Exception {
		super(channel, destination, chunkSize);

		prepareForSend(file);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#remaining()
	 */
	@Override
	public long remaining() {
		return mappedByteBuffer.remaining();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#reset()
	 */
	@Override
	public void reset() {
		resetPosition(0);
		complete.set(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.Streamer#resetPosition(int)
	 */
	@Override
	public void resetPosition(int newPosition) {
		if (streaming.get()) throw new IllegalStateException("Cannot reset position when streaming");

		if (newPosition < 0 || newPosition > mappedByteBuffer.limit()) return;

		mappedByteBuffer.position(newPosition);
		
		resetSequenceFromPosition(newPosition);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.github.mrstampy.kitchensync.stream.AbstractStreamer#getChunk()
	 */
	@Override
	protected byte[] getChunk() {
		int remaining = (int) remaining();

		byte[] chunk = createByteArray(remaining);

		mappedByteBuffer.get(chunk);
		
		return chunk;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.mrstampy.kitchensync.stream.AbstractStreamer#prepareForSend(
	 * java.lang.Object)
	 */
	@Override
	protected void prepareForSend(File message) throws IOException {
		FileInputStream fis = new FileInputStream(message);
		try {
			FileChannel fileChannel = fis.getChannel();
			mappedByteBuffer = fileChannel.map(MapMode.READ_ONLY, 0, fileChannel.size());
			setSize(mappedByteBuffer.remaining());
		} finally {
			fis.close();
		}
		sent.set(0);
	}
}
