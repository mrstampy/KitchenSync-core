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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;
import com.github.mrstampy.kitchensync.stream.inbound.EndOfMessageInboundMessageHandler;

/**
 * The Register for end of message messages.
 * 
 * @see EndOfMessageListener
 * @see EndOfMessageInboundMessageHandler
 */
public class EndOfMessageRegister {
	private static final Logger log = LoggerFactory.getLogger(EndOfMessageRegister.class);

	/** The Constant END_OF_MESSAGE. */
	public static final String END_OF_MESSAGE = "EOM:";

	private static List<EndOfMessageListener> eomListeners = new ArrayList<EndOfMessageListener>();
	private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private static ReadLock readLock = lock.readLock();
	private static WriteLock writeLock = lock.writeLock();

	/**
	 * Adds the eom listeners.
	 *
	 * @param listeners
	 *          the listeners
	 */
	public static void addEOMListeners(EndOfMessageListener... listeners) {
		if (listeners == null || listeners.length == 0) return;

		writeLock.lock();
		try {
			for (EndOfMessageListener l : listeners) {
				eomListeners.add(l);
			}
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Removes the eom listeners.
	 *
	 * @param listeners
	 *          the listeners
	 */
	public static void removeEOMListeners(EndOfMessageListener... listeners) {
		if (listeners == null || listeners.length == 0) return;

		writeLock.lock();
		try {
			for (EndOfMessageListener l : listeners) {
				eomListeners.remove(l);
			}
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Notify eom listeners.
	 *
	 * @param channel
	 *          the channel
	 * @param sender
	 *          the sender
	 */
	public static void notifyEOMListeners(KiSyChannel channel, InetSocketAddress sender) {
		readLock.lock();
		try {
			boolean notified = false;
			for (EndOfMessageListener l : eomListeners) {
				if (!l.isForChannelAndSender(channel, sender)) continue;

				l.endOfMessage();
				notified = true;
			}

			if (!notified) log.warn("No listener for end of message from {} on channel {}", sender, channel.getPort());
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Returns true if the specified message is an end of message message.
	 *
	 * @param message
	 *          the message
	 * @return true, if checks if is end of message
	 * @see EndOfMessageInboundMessageHandler
	 */
	public static boolean isEndOfMessage(byte[] message) {
		if (message == null || message.length < END_OF_MESSAGE.length()) return false;

		byte[] b = Arrays.copyOfRange(message, 0, END_OF_MESSAGE.length());

		return Arrays.equals(b, END_OF_MESSAGE.getBytes());
	}

	private EndOfMessageRegister() {
	}
}
