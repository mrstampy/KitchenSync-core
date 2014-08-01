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
package com.github.mrstampy.kitchensync.message.outbound;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Action1;

/**
 * The Class KiSyOutboundMessageManager encapsulates the various
 * {@link KiSyOutboundMessageHandler}s to be invoked prior to message sending.
 */
public class KiSyOutboundMessageManager {
	private static final Logger log = LoggerFactory.getLogger(KiSyOutboundMessageManager.class);

	private List<KiSyOutboundMessageHandler<?>> handlers = new ArrayList<KiSyOutboundMessageHandler<?>>();

	/** The Constant INSTANCE, a singleton. */
	public static final KiSyOutboundMessageManager INSTANCE = new KiSyOutboundMessageManager();

	private HandlerComparator comparator = new HandlerComparator();

	/**
	 * The Constructor.
	 */
	protected KiSyOutboundMessageManager() {
	}

	/**
	 * Adds the outbound handlers. Note that this must be explicitly wired up
	 * during program instantiation.
	 *
	 * @param hndlrs
	 *          the hndlrs
	 */
	public void addOutboundHandlers(KiSyOutboundMessageHandler<?>... hndlrs) {
		if(hndlrs == null || hndlrs.length == 0) return;
		
		addAllOutboundHandlers(Arrays.asList(hndlrs));
	}
	
	/**
	 * Adds the all outbound handlers.
	 *
	 * @param hndlrs the hndlrs
	 */
	public void addAllOutboundHandlers(Collection<KiSyOutboundMessageHandler<?>> hndlrs) {
		if(hndlrs == null || hndlrs.isEmpty()) return;
		
		for (KiSyOutboundMessageHandler<?> handler : hndlrs) {
			if (!handlers.contains(handler)) handlers.add(handler);
		}
		
		Collections.sort(handlers, comparator);
	}

	/**
	 * Removes the outbound handler.
	 *
	 * @param handler
	 *          the handler
	 */
	public void removeOutboundHandler(KiSyOutboundMessageHandler<?> handler) {
		handlers.remove(handler);
	}

	/**
	 * Clears the outbound handlers.
	 */
	public void clearOutboundHandlers() {
		handlers.clear();
	}

	/**
	 * Presend, invoked prior to sending the specified message.
	 *
	 * @param <MSG>
	 *          the generic type
	 * @param message
	 *          the message to be sent
	 * @param originator
	 *          the originator
	 * @param recipient
	 *          the intended recipient
	 */
	public <MSG> void presend(final MSG message, final InetSocketAddress originator, final InetSocketAddress recipient) {
		List<KiSyOutboundMessageHandler<MSG>> relevant = getHandlersForMessage(message, recipient);
		if (relevant.isEmpty()) return;

		Observable.from(relevant).subscribe(new Action1<KiSyOutboundMessageHandler<MSG>>() {

			@Override
			public void call(KiSyOutboundMessageHandler<MSG> t1) {
				t1.presend(message, originator, recipient);
			}
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <MSG> List<KiSyOutboundMessageHandler<MSG>> getHandlersForMessage(MSG message, InetSocketAddress recipient) {
		List<KiSyOutboundMessageHandler<MSG>> relevant = new ArrayList<KiSyOutboundMessageHandler<MSG>>();

		ListIterator<KiSyOutboundMessageHandler<?>> it = handlers.listIterator();

		while (it.hasNext()) {
			try {
				KiSyOutboundMessageHandler handler = it.next();
				if (handler.isForMessage(message, recipient)) relevant.add(handler);
			} catch (Exception e) {
				log.debug("Handler not typed for {}", message, e);
			}
		}

		return relevant;
	}

	private static class HandlerComparator implements Comparator<KiSyOutboundMessageHandler<?>> {

		@Override
		public int compare(KiSyOutboundMessageHandler<?> kisy1, KiSyOutboundMessageHandler<?> kisy2) {
			return kisy1.getExecutionOrder() - kisy2.getExecutionOrder();
		}

	}
}
