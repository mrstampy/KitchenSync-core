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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import rx.Scheduler;
import rx.schedulers.Schedulers;

/**
 * A psuedo-pool of single thread executors used to process inbound messages in
 * order.
 */
public class SingleThreadExecutorSchedulerProvider {

	private List<Scheduler> bufferedSchedulers = new ArrayList<Scheduler>();

	private AtomicInteger next = new AtomicInteger(0);

	private int size;

	private Lock lock = new ReentrantLock();

	/**
	 * The Constructor.
	 *
	 * @param size
	 *          the number of executors to create
	 */
	public SingleThreadExecutorSchedulerProvider(int size) {
		this.size = size;
		createSchedulers();
	}

	/**
	 * Returns the next available single thread scheduler.
	 *
	 * @return the scheduler
	 */
	public Scheduler singleThreadScheduler() {
		lock.lock();
		try {
			return getNext();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * The number of single thread executors.
	 *
	 * @return the int
	 */
	public int size() {
		return size;
	}

	private Scheduler getNext() {
		int index = next.getAndIncrement();
		if (index == size - 1) next.set(0);

		return bufferedSchedulers.get(index);
	}

	private void createSchedulers() {
		for (int i = 0; i < size; i++) {
			bufferedSchedulers.add(Schedulers.from(Executors.newSingleThreadExecutor()));
		}
	}
}
