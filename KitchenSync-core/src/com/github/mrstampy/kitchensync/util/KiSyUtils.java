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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The Class KiSyUtils.
 */
public class KiSyUtils {

	/** The Constant ONE_MILLION. */
	public static final BigDecimal ONE_MILLION = new BigDecimal(1000000);

	/**
	 * Converts the specified time in nanoseconds to its string value in
	 * milliseconds, to 3 decimal places.
	 *
	 * @param nanos
	 *          the nanos
	 * @return the string representation in milliseconds, to 3 decimals.
	 */
	public static String toMillis(Long nanos) {
		return new BigDecimal(nanos).divide(ONE_MILLION, 3, RoundingMode.HALF_UP).toPlainString();
	}

	/**
	 * Sleep for the specified number of milliseconds. Interrupted exceptions are
	 * ignored.
	 * 
	 * @param time
	 *          in millis
	 */
	public static void snooze(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Sleep for the specified number of milli and (up to one million)
	 * nanoseconds. Interrupted exceptions are ignored.
	 * 
	 * @param time
	 *          in millis
	 * @param nanos
	 *          the nanoseconds
	 */
	public static void snooze(long time, int nanos) {
		try {
			Thread.sleep(time, nanos);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Awaits on the specified latch, absorbing any {@link InterruptedException}s.
	 * 
	 * @param latch
	 *          may be null
	 * @param value
	 *          the amount to wait
	 * @param unit
	 *          the wait units
	 * @return true if latch was counted down, false otherwise
	 */
	public static boolean await(CountDownLatch latch, int value, TimeUnit unit) {
		try {
			return latch == null ? false : latch.await(value, unit);
		} catch (InterruptedException e) {
		}

		return false;
	}

	private KiSyUtils() {
	}

}
