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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Arrays;

/**
 * The Class StreamerHeader provides the ability to determine if a message
 * {@link #isHeaderMessage(byte[])}, can prepend a header via
 * {@link #addHeader(long, byte[])}, and can parse a chunk with a header via its
 * constructor.
 * 
 * @see Streamer#setUseHeader(boolean)
 */
public class StreamerHeader implements Comparable<StreamerHeader>{

	/** The Constant SEQUENCE_HEADER, 'Sequence:'. */
	public static final String SEQUENCE_HEADER = "Sequence:";

	/** The convenience constant SEQUENCE_HEADER_BYTES. */
	public static final byte[] SEQUENCE_HEADER_BYTES = SEQUENCE_HEADER.getBytes();

	/**
	 * The Constant HEADER_LENGTH, = SEQUENCE_HEADER_BYTES.length + 8 to contain
	 * the long value of the sequence
	 */
	public static final int HEADER_LENGTH = SEQUENCE_HEADER_BYTES.length + 8;

	private long sequence;

	private byte[] message;

	/**
	 * The Constructor.
	 *
	 * @param chunk
	 *          the chunk
	 */
	public StreamerHeader(byte[] chunk) {
		if (!isHeaderMessage(chunk)) throw new IllegalArgumentException("Message chunk has no header");

		setSequence(Arrays.copyOfRange(chunk, SEQUENCE_HEADER_BYTES.length, HEADER_LENGTH));
		setMessage(Arrays.copyOfRange(chunk, HEADER_LENGTH, chunk.length));
	}

	/**
	 * Gets the sequence of the chunk.
	 *
	 * @return the sequence
	 */
	public long getSequence() {
		return sequence;
	}

	/**
	 * Gets the message chunk.
	 *
	 * @return the message
	 */
	public byte[] getMessage() {
		return message;
	}

	@Override
	public int compareTo(StreamerHeader o) {
		return (int)(getSequence() - o.getSequence());
	}

	private void setSequence(byte[] seqBytes) {
		ByteBuf seq = Unpooled.copiedBuffer(seqBytes);

		sequence = seq.getLong(0);
	}

	private void setMessage(byte[] message) {
		this.message = message;
	}

	/**
	 * Checks if is header message.
	 *
	 * @param chunk
	 *          the chunk
	 * @return true, if checks if is header message
	 */
	public static boolean isHeaderMessage(byte[] chunk) {
		if (chunk == null || chunk.length < HEADER_LENGTH) return false;

		byte[] b = Arrays.copyOfRange(chunk, 0, SEQUENCE_HEADER_BYTES.length);

		return Arrays.equals(b, SEQUENCE_HEADER_BYTES);
	}

	/**
	 * Adds the header. The sequence is encoded as an 8 byte chunk.
	 *
	 * @param sequence
	 *          the sequence
	 * @param chunk
	 *          the chunk
	 * @return the byte[]
	 */
	public static byte[] addHeader(long sequence, byte[] chunk) {
		ByteBuf buf = Unpooled.buffer(HEADER_LENGTH + chunk.length);

		buf.writeBytes(SEQUENCE_HEADER_BYTES);
		buf.writeLong(sequence);
		buf.writeBytes(chunk);

		return buf.array();
	}

}
