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
package com.github.mrstampy.kitchensync.message.inbound;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import com.github.mrstampy.kitchensync.netty.channel.KiSyChannel;
import com.github.mrstampy.kitchensync.util.KiSyUtils;
import com.github.mrstampy.kitchensync.util.SingleThreadExecutorSchedulerProvider;

/**
 * The Class KiSyInboundMessageManager processes inbound messages using the
 * various {@link KiSyInboundMesssageHandler}s that have been added during
 * program initialization via the
 * {@link #addMessageHandlers(KiSyInboundMesssageHandler...)} or
 * {@link #addAllMessageHandlers(Collection)} methods.
 *
 * @param <MSG>
 *          the generic type
 */
public class KiSyInboundMessageManager<MSG> {
	private static final Logger log = LoggerFactory.getLogger(KiSyInboundMessageManager.class);

	private List<KiSyInboundMesssageHandler<?>> messageHandlers = new ArrayList<KiSyInboundMesssageHandler<?>>();

	private HandlerComparator handlerComparator = new HandlerComparator();

	private Scheduler scheduler = Schedulers.computation();

	private SingleThreadExecutorSchedulerProvider singleThreadProvider = new SingleThreadExecutorSchedulerProvider(10);

	/**
	 * Adds the message handlers. Note that this must be explicitly wired up
	 * during program instantiation.
	 *
	 * @param handlers
	 *          the handlers
	 */
	public void addMessageHandlers(KiSyInboundMesssageHandler<?>... handlers) {
		if (handlers == null || handlers.length == 0) return;

		addAllMessageHandlers(Arrays.asList(handlers));
	}

	/**
	 * Adds the all message handlers. Note that this must be explicitly wired up
	 * during program instantiation.
	 *
	 * @param handlers
	 *          the handlers
	 */
	public void addAllMessageHandlers(Collection<KiSyInboundMesssageHandler<?>> handlers) {
		if (handlers == null || handlers.isEmpty()) return;

		messageHandlers.addAll(handlers);
		Collections.sort(messageHandlers, handlerComparator);
	}

	/**
	 * Removes the specified message handler.
	 *
	 * @param handler
	 *          the handler
	 */
	public void removeMessageHandler(KiSyInboundMesssageHandler<?> handler) {
		if (handler == null) return;

		messageHandlers.remove(handler);
	}

	/**
	 * Clear all message handlers.
	 */
	public void clearMessageHandlers() {
		messageHandlers.clear();
	}

	/**
	 * Process the given message.
	 *
	 * @param message          the message
	 * @param channel          the channel on which the message was received
	 * @param sender the sender
	 */
	public void processMessage(MSG message, KiSyChannel channel, InetSocketAddress sender) {
		log.trace("Processing message {}", message);

		long start = System.nanoTime();

		List<KiSyInboundMesssageHandler<MSG>> ordered = getHandlersForMessage(message);

		if (ordered.isEmpty()) {
			log.debug("No messages handlers for {}", message);
			return;
		}

		CountDownLatch cdl = new CountDownLatch(ordered.size());

		scheduleCleanup(message, cdl, start);
		processMessage(ordered, message, channel, cdl, sender);
	}

	private void processMessage(List<KiSyInboundMesssageHandler<MSG>> ordered, final MSG message,
			final KiSyChannel channel, final CountDownLatch cdl, final InetSocketAddress sender) {
		Observable.from(ordered, singleThreadExecutor()).subscribe(new Action1<KiSyInboundMesssageHandler<MSG>>() {

			@Override
			public void call(KiSyInboundMesssageHandler<MSG> t1) {
				try {
					t1.messageReceived(message, channel, sender);
				} catch (Exception e) {
					log.error("Could not process message {} with {}", message, t1, e);
				} finally {
					cdl.countDown();
				}
			}
		});
	}

	private Scheduler singleThreadExecutor() {
		return singleThreadProvider.singleThreadScheduler();
	}

	private void scheduleCleanup(final MSG message, final CountDownLatch cdl, final long start) {
		scheduler.createWorker().schedule(new Action0() {

			@Override
			public void call() {
				try {
					boolean done = cdl.await(10, TimeUnit.SECONDS);
					if (done) {
						if (log.isTraceEnabled()) {
							String millis = KiSyUtils.toMillis(System.nanoTime() - start);
							log.trace("Message {} fully processed in {} ms", message, millis);
						}
					} else {
						log.warn("Message processing > 10 seconds: {}", message);
					}
				} catch (InterruptedException e) {
					log.error("Unexpected exception", e);
				}
			}
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<KiSyInboundMesssageHandler<MSG>> getHandlersForMessage(MSG message) {
		List<KiSyInboundMesssageHandler<MSG>> ordered = new ArrayList<KiSyInboundMesssageHandler<MSG>>();

		ListIterator<KiSyInboundMesssageHandler<?>> it = messageHandlers.listIterator();
		while (it.hasNext()) {
			try {
				KiSyInboundMesssageHandler handler = it.next();
				if (handler.canHandleMessage(message)) ordered.add((KiSyInboundMesssageHandler<MSG>) handler);
			} catch (Exception e) {
				log.debug("Handler not applicable for message {}", message, e);
			}
		}

		return ordered;
	}

	private static class HandlerComparator implements Comparator<KiSyInboundMesssageHandler<?>> {

		@Override
		public int compare(KiSyInboundMesssageHandler<?> kisy1, KiSyInboundMesssageHandler<?> kisy2) {
			return kisy1.getExecutionOrder() - kisy2.getExecutionOrder();
		}

	}

}
