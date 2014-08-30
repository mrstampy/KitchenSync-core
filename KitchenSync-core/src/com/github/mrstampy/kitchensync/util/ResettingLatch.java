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
package com.github.mrstampy.kitchensync.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encapsulates an {@link AtomicReference} to a
 * {@link CountDownLatch} which is replaced when an await returns. It acts as a
 * gate awaiting a the {@link #getCount()} number of {@link #countDown()}s for
 * cyclic operations. It is designed to be used with a single awaiter ie. a
 * thread which awaits for data to be available from a buffer in a recursive
 * action, awaiting the thread which sets the data to check for conditions and
 * if right trigger a {@link #countDown()}.
 */
public class ResettingLatch {
	private static final Logger log = LoggerFactory.getLogger(ResettingLatch.class);

	private final AtomicReference<CountDownLatch> latch;

	private int count;

	/**
	 * The Constructor.
	 */
	public ResettingLatch() {
		this(1);
	}

	/**
	 * The Constructor.
	 *
	 * @param count
	 *          the count
	 */
	public ResettingLatch(int count) {
		if (count < 1) throw new IllegalArgumentException("Count must be > 0: " + count);

		this.count = count;
		this.latch = new AtomicReference<CountDownLatch>(new CountDownLatch(count));

		log.debug("Initialized ResettableLatch for {} count{}", count, (count > 1) ? "s" : "");
	}

	/**
	 * Returns the outstanding number of {@link #countDown()}s.
	 *
	 * @return the long
	 */
	public long remaining() {
		return latch.get().getCount();
	}

	/**
	 * Await.
	 */
	public void await() {
		try {
			latch.get().await();
		} catch (InterruptedException e) {
			log.error("Unexpected exception", e);
		} finally {
			reset();
		}
	}

	/**
	 * Await.
	 *
	 * @param timeout
	 *          the timeout
	 * @param unit
	 *          the unit
	 * @return true, if await
	 */
	public boolean await(int timeout, TimeUnit unit) {
		try {
			return KiSyUtils.await(latch.get(), timeout, unit);
		} finally {
			reset();
		}
	}

	/**
	 * Count down.
	 */
	public void countDown() {
		latch.get().countDown();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return latch.get().toString();
	}

	private void reset() {
		latch.set(new CountDownLatch(getCount()));
	}

	/**
	 * Gets the count.
	 *
	 * @return the count
	 */
	public int getCount() {
		return count;
	}
}
