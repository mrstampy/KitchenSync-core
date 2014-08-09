package com.github.mrstampy.kitchensync.stream;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class StreamerAckRegister {

	private static Map<Integer, Streamer<?>> ackAwaiters = new ConcurrentHashMap<Integer, Streamer<?>>();
	private static Map<Integer, Subscription> subscriptions = new ConcurrentHashMap<Integer, Subscription>();

	private static Scheduler cleanupSvc = Schedulers.from(Executors.newCachedThreadPool());

	public static void add(final Integer sumOfBytes, Streamer<?> acker) {
		if (!acker.isAckRequired()) return;

		ackAwaiters.put(sumOfBytes, acker);

		Subscription sub = cleanupSvc.createWorker().schedule(new Action0() {

			@Override
			public void call() {
				getAckAwaiter(sumOfBytes);
			}
		}, 10, TimeUnit.SECONDS);
		
		subscriptions.put(sumOfBytes, sub);
	}

	public static Streamer<?> getAckAwaiter(Integer sumOfBytes) {
		Subscription sub = subscriptions.get(sumOfBytes);
		unsubscribe(sub);
		
		return ackAwaiters.remove(sumOfBytes);
	}

	public static boolean isAckMessage(byte[] message) {
		if(message.length < Streamer.ACK_PREFIX_BYTES.length) return false;
		
		byte[] b = new byte[Streamer.ACK_PREFIX_BYTES.length];
		System.arraycopy(message, 0, b, 0, b.length);
		
		return Arrays.equals(b, Streamer.ACK_PREFIX_BYTES);
	}
	
	public static byte[] createAckResponse(int sumOfBytes) {
		String s = Streamer.ACK_PREFIX + sumOfBytes;
		
		return s.getBytes();
	}
	
	/**
	 * Convenience method to sum the bytes of the given byte array.
	 * 
	 * @param chunk
	 * @return
	 * @see Streamer#ackReceived(int)
	 */
	public static int convertToInt(byte[] chunk) {
		int id = 0;
		
		for(byte b : chunk) {
			id += b;
		}
		
		return id;
	}

	private static void unsubscribe(Subscription sub) {
		if(sub != null) sub.unsubscribe();
	}

	private StreamerAckRegister() {
	}
}
