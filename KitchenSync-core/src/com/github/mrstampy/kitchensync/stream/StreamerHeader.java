package com.github.mrstampy.kitchensync.stream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Arrays;

public class StreamerHeader {

	public static final String SEQUENCE_HEADER = "Sequence:";
	public static final byte[] SEQUENCE_HEADER_BYTES = SEQUENCE_HEADER.getBytes();
	public static final int HEADER_LENGTH = SEQUENCE_HEADER_BYTES.length + 8;

	private long sequence;

	private byte[] message;

	public StreamerHeader(byte[] chunk) {
		if (!isHeaderMessage(chunk)) throw new IllegalArgumentException("Message chunk has no header");

		setSequence(Arrays.copyOfRange(chunk, SEQUENCE_HEADER_BYTES.length, HEADER_LENGTH));
		setMessage(Arrays.copyOfRange(chunk, HEADER_LENGTH, chunk.length));
	}

	public long getSequence() {
		return sequence;
	}

	public byte[] getMessage() {
		return message;
	}

	private void setSequence(byte[] seqBytes) {
		ByteBuf seq = Unpooled.copiedBuffer(seqBytes);

		sequence = seq.getLong(0);
	}

	private void setMessage(byte[] message) {
		this.message = message;
	}

	public static boolean isHeaderMessage(byte[] chunk) {
		if (chunk == null || chunk.length < HEADER_LENGTH) return false;

		byte[] b = Arrays.copyOfRange(chunk, 0, SEQUENCE_HEADER_BYTES.length);

		return Arrays.equals(b, SEQUENCE_HEADER_BYTES);
	}
	
	public static byte[] addHeader(long sequence, byte[] chunk) {
		ByteBuf buf = Unpooled.buffer(HEADER_LENGTH + chunk.length);
		
		buf.writeBytes(SEQUENCE_HEADER_BYTES);
		buf.writeLong(sequence);
		buf.writeBytes(chunk);
		
		return buf.array();
	}

}
