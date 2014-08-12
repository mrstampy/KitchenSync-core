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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

import com.github.mrstampy.kitchensync.message.inbound.KiSyInboundMesssageHandler;
import com.github.mrstampy.kitchensync.stream.inbound.StreamAckInboundMessageHandler;

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

	/**
	 * The prefix used to identify return messages when {@link Streamer#isAckRequired()}:
	 * 'StreamAck:'. The suffix is the sum of the btyes of the message being
	 * acknowledged.
	 * 
	 * @see #convertToLong(byte[])
	 * @see Streamer#ackRequired()
	 */
	public static final String ACK_PREFIX = "StreamAck:";

	/**
	 * Convenience constant for the byte array from {@link #ACK_PREFIX}.
	 */
	public static final byte[] ACK_PREFIX_BYTES = ACK_PREFIX.getBytes();

	private static Map<RegKey, List<RegisterContainer>> awaiting = new ConcurrentHashMap<RegKey, List<RegisterContainer>>();

	private static Scheduler cleanupSvc = Schedulers.from(Executors.newCachedThreadPool());

	private static ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
	private static ReadLock readLock = rwLock.readLock();
	private static WriteLock writeLock = rwLock.writeLock();

	/**
	 * Adds the {@link Streamer} and the chunk to the register.
	 *
	 * @param chunk
	 *          the chunk
	 * @param acker
	 *          the acker
	 * @return the long
	 */
	public static long add(byte[] chunk, Streamer<?> acker) {
		if (!acker.isAckRequired()) return -1l;

		long sumOfBytes = convertToLong(chunk);
		int port = acker.getChannel().getPort();

		final RegKey rk = new RegKey(sumOfBytes, port);

		List<RegisterContainer> containers = getContainers(rk);

		Subscription sub = cleanupSvc.createWorker().schedule(new Action0() {

			@Override
			public void call() {
				removeAckAwaiter(rk);
			}
		}, 10, TimeUnit.SECONDS);

		RegisterContainer rc = new RegisterContainer(acker, sub, chunk, sumOfBytes);

		writeLock.lock();
		try {
			int idx = containers.indexOf(rc);
			if (idx == -1) {
				containers.add(rc);
			} else {
				RegisterContainer existing = containers.get(idx);
				existing.add(sub);
			}
		} finally {
			writeLock.unlock();
		}

		return sumOfBytes;
	}

	/**
	 * Gets the {@link Streamer} for the specified acknowledgement value.
	 *
	 * @param sumOfBytes
	 *          the sum of bytes
	 * @param port
	 *          the port
	 * @return the ack awaiter
	 */
	public static Streamer<?> getAckAwaiter(long sumOfBytes, int port) {
		List<RegisterContainer> containers = getContainers(new RegKey(sumOfBytes, port));

		RegisterContainer reg = getContainer(sumOfBytes, containers);

		return reg == null ? null : reg.streamer;
	}

	private static RegisterContainer removeAckAwaiter(RegKey rk) {
		List<RegisterContainer> containers = getContainers(rk);

		RegisterContainer reg = getContainer(rk.sumOfBytes, containers);

		if (reg != null) removeRegisterContainer(rk, containers, reg);

		return reg;
	}

	/**
	 * Gets the chunk associated with the acknowledgement value.
	 *
	 * @param sumOfBytes
	 *          the sum of bytes
	 * @param port
	 *          the port
	 * @return the chunk
	 */
	public static byte[] getChunk(long sumOfBytes, int port) {
		RegisterContainer reg = removeAckAwaiter(new RegKey(sumOfBytes, port));

		return reg == null ? null : reg.chunk;
	}

	private static List<RegisterContainer> getContainers(RegKey rk) {
		List<RegisterContainer> containers = awaiting.get(rk);

		if (containers == null) {
			containers = new ArrayList<RegisterContainer>();
			awaiting.put(rk, containers);
		}

		return containers;
	}

	private static RegisterContainer getContainer(long sumOfBytes, List<RegisterContainer> containers) {
		readLock.lock();
		try {
			RegisterContainer reg = null;
			for (RegisterContainer rc : containers) {
				if (rc == null || !rc.isAck(sumOfBytes)) continue;

				reg = rc;
				break;
			}
			return reg;
		} finally {
			readLock.unlock();
		}
	}

	private static void removeRegisterContainer(RegKey rk, List<RegisterContainer> containers, RegisterContainer rc) {
		if (containers.isEmpty()) return;

		writeLock.lock();
		try {
			Subscription sub = rc.sub();
			unsubscribe(sub);
			if (sub == null || rc.count() == 0) {
				containers.remove(rc);
				if(containers.isEmpty()) awaiting.remove(rk);
			}
		} finally {
			writeLock.unlock();
		}
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
		if (message.length < ACK_PREFIX_BYTES.length) return false;

		byte[] b = Arrays.copyOfRange(message, 0, ACK_PREFIX_BYTES.length);

		return Arrays.equals(b, ACK_PREFIX_BYTES);
	}

	/**
	 * Creates the ack response to send back to the
	 * {@link Streamer#isAckRequired()}.
	 *
	 * @param sumOfBytes
	 *          the sum of bytes
	 * @return the byte[]
	 */
	public static byte[] createAckResponse(long sumOfBytes) {
		String s = ACK_PREFIX + sumOfBytes;

		return s.getBytes();
	}

	/**
	 * Convenience method to sum the bytes of the given byte array.
	 *
	 * @param chunk
	 *          the chunk
	 * @return the long
	 * @see Streamer#ackReceived(long)
	 */
	public static long convertToLong(byte[] chunk) {
		int hash = 0;
		for (byte b : chunk) {
			hash += b;
		}
		return hash;
	}

	private static void unsubscribe(Subscription sub) {
		if (sub != null) sub.unsubscribe();
	}

	private static class RegisterContainer {
		Streamer<?> streamer;
		List<Subscription> subs = new ArrayList<Subscription>();
		byte[] chunk;
		Long ackKey;

		public RegisterContainer(Streamer<?> streamer, Subscription sub, byte[] chunk, Long ackKey) {
			this.streamer = streamer;
			this.chunk = chunk;
			this.ackKey = ackKey;
			subs.add(sub);
		}

		public int count() {
			return subs.size();
		}

		public Subscription sub() {
			return count() > 0 ? subs.remove(0) : null;
		}

		public void add(Subscription sub) {
			subs.add(sub);
		}

		public boolean isAck(Long key) {
			return ackKey.equals(key);
		}
	}

	private static class RegKey {
		long sumOfBytes;
		int port;

		public RegKey(long sumOfBytes, int port) {
			this.sumOfBytes = sumOfBytes;
			this.port = port;
		}

		public boolean equals(Object o) {
			// bcos its private...
			RegKey rk = (RegKey) o;
			return rk.port == port && rk.sumOfBytes == sumOfBytes;
		}

		public int hashCode() {
			return ((int) sumOfBytes * 17) + port * 23;
		}
	}

	private StreamerAckRegister() {
	}
}
