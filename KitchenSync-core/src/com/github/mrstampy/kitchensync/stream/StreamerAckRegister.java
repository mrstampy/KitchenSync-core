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

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMesssageHandler;
import com.github.mrstampy.kitchensync.stream.inbound.StreamAckInboundMessageHandler;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * {@link Streamer}s register themselves when sending a message requiring an
 * acknowledgement. When the acknowledgement is received a
 * {@link KiSyInboundMesssageHandler} (such as the
 * {@link StreamAckInboundMessageHandler}) uses this register to look up the
 * appropriate Streamer for {@link Streamer#ackReceived(long)}.
 * 
 * The acknowledgement is a long, the sum of the bytes of the last message sent.
 */
public class StreamerAckRegister {

	private static Map<Long, Streamer<?>> ackAwaiters = new ConcurrentHashMap<Long, Streamer<?>>();
	private static Map<Long, Subscription> subscriptions = new ConcurrentHashMap<Long, Subscription>();
	private static Map<Long, byte[]> chunks = new ConcurrentHashMap<Long, byte[]>();

	private static Scheduler cleanupSvc = Schedulers.from(Executors.newCachedThreadPool());

	/**
	 * Adds the {@link Streamer} and the chunk to the register.
	 *
	 * @param chunk
	 *          the chunk
	 * @param acker
	 *          the acker
	 * @return the long
	 */
	public static Long add(byte[] chunk, Streamer<?> acker) {
		if (!acker.isAckRequired()) return -1l;

		final Long sumOfBytes = convertToLong(chunk);

		ackAwaiters.put(sumOfBytes, acker);
		chunks.put(sumOfBytes, chunk);

		Subscription sub = cleanupSvc.createWorker().schedule(new Action0() {

			@Override
			public void call() {
				getAckAwaiter(sumOfBytes);
				chunks.remove(sumOfBytes);
			}
		}, 20, TimeUnit.SECONDS);

		subscriptions.put(sumOfBytes, sub);

		return sumOfBytes;
	}

	/**
	 * Gets the {@link Streamer} for the specified acknowledgement value.
	 *
	 * @param sumOfBytes
	 *          the sum of bytes
	 * @return the ack awaiter
	 */
	public static Streamer<?> getAckAwaiter(Long sumOfBytes) {
		Subscription sub = subscriptions.get(sumOfBytes);
		unsubscribe(sub);

		return ackAwaiters.remove(sumOfBytes);
	}

	/**
	 * Gets the chunk associated with the acknowledgement value.
	 *
	 * @param sumOfBytes
	 *          the sum of bytes
	 * @return the chunk
	 */
	public static byte[] getChunk(Long sumOfBytes) {
		return chunks.remove(sumOfBytes);
	}

	/**
	 * Utility method, returns true if the message supplied is an acknowledgement
	 * message.
	 *
	 * @param message
	 *          the message
	 * @return true, if checks if is ack message
	 */
	public static boolean isAckMessage(byte[] message) {
		if (message.length < Streamer.ACK_PREFIX_BYTES.length) return false;

		byte[] b = Arrays.copyOfRange(message, 0, Streamer.ACK_PREFIX_BYTES.length);

		return Arrays.equals(b, Streamer.ACK_PREFIX_BYTES);
	}

	/**
	 * Creates the ack response to send back to the {@link Streamer#isAckRequired()}.
	 *
	 * @param sumOfBytes
	 *          the sum of bytes
	 * @return the byte[]
	 */
	public static byte[] createAckResponse(long sumOfBytes) {
		String s = Streamer.ACK_PREFIX + sumOfBytes;

		return s.getBytes();
	}

	/**
	 * Convenience method to sum the bytes of the given byte array.
	 *
	 * @param chunk
	 *          the chunk
	 * @return the long
	 * @see Streamer#ackReceived(int)
	 */
	public static long convertToLong(byte[] chunk) {
		long id = 0;

		for (byte b : chunk) {
			id += b;
		}

		return id;
	}

	private static void unsubscribe(Subscription sub) {
		if (sub != null) sub.unsubscribe();
	}

	private StreamerAckRegister() {
	}
}
