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
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;
import com.github.mrstampy.kitchensync.stream.footer.EndOfMessageFooter;
import com.github.mrstampy.kitchensync.stream.footer.Footer;
import com.github.mrstampy.kitchensync.stream.inbound.EndOfMessageInboundMessageHandler;

/**
 * The Register for end of message messages.
 * 
 * @see EndOfMessageListener
 * @see EndOfMessageInboundMessageHandler
 * @see Footer
 */
public class EndOfMessageRegister {
	private static final Logger log = LoggerFactory.getLogger(EndOfMessageRegister.class);

	/**
	 * The Constant INSTANCE, convenience singleton using
	 * {@link EndOfMessageFooter}.
	 */
	public static final EndOfMessageRegister INSTANCE = new EndOfMessageRegister(new EndOfMessageFooter());

	private List<EndOfMessageListener> eomListeners = new ArrayList<EndOfMessageListener>();
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private ReadLock readLock = lock.readLock();
	private WriteLock writeLock = lock.writeLock();

	private Footer footer;

	private Lock footerLock = new ReentrantLock();

	/**
	 * Instantiate with the {@link Footer} instance used to indicate
	 * {@link #isEndOfMessage(byte[])}.  No nulls allowed.
	 * 
	 * @param footer
	 *          to indicate {@link #isEndOfMessage(byte[])}.
	 * @see Footer#isFooter(byte[])
	 */
	public EndOfMessageRegister(Footer footer) {
		setFooter(footer);
	}

	/**
	 * Adds the eom listeners.
	 *
	 * @param listeners
	 *          the listeners
	 */
	public void addEOMListeners(EndOfMessageListener... listeners) {
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
	public void removeEOMListeners(EndOfMessageListener... listeners) {
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
	 * Notify eom listeners. Assumes that {@link #isEndOfMessage(byte[])} has been
	 * used appropriately.
	 *
	 * @param channel
	 *          the channel
	 * @param sender
	 *          the sender
	 * @param eom
	 *          the end of message message
	 */
	public void notifyEOMListeners(KiSyChannel channel, InetSocketAddress sender, byte[] eom) {
		readLock.lock();
		try {
			boolean notified = false;
			
			for (EndOfMessageListener l : eomListeners) {
				if (!l.isForChannelAndSender(channel, sender)) continue;

				l.endOfMessage(eom);
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
	public boolean isEndOfMessage(byte[] message) {
		footerLock.lock();
		try {
			return getFooter().isFooter(message);
		} finally {
			footerLock.unlock();
		}
	}

	/**
	 * Gets the footer used to determine if {@link #isEndOfMessage(byte[])}. The
	 * default is a {@link EndOfMessageFooter}.
	 *
	 * @return the footer
	 */
	public Footer getFooter() {
		return footer;
	}

	private void setFooter(Footer footer) {
		if (footer == null) throw new IllegalArgumentException("Footer must be specified");

		this.footer = footer;
	}
}
