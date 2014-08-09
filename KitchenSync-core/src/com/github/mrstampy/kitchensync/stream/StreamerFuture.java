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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * The Class StreamerFuture.
 */
public class StreamerFuture implements ChannelFuture {
	private static final Logger log = LoggerFactory.getLogger(StreamerFuture.class);

	private boolean success;
	private boolean cancelled;
	private boolean done;
	private Throwable cause;
	private Streamer<?> streamer;

	private List<GenericFutureListener<ChannelFuture>> listeners = new ArrayList<GenericFutureListener<ChannelFuture>>();

	private CountDownLatch latch = new CountDownLatch(1);

	/**
	 * The Constructor.
	 *
	 * @param streamer the streamer
	 */
	public StreamerFuture(Streamer<?> streamer) {
		this.streamer = streamer;
	}

	/**
	 * Finished.
	 *
	 * @param success the success
	 */
	void finished(boolean success) {
		finished(success, null);
	}

	/**
	 * Finished.
	 *
	 * @param success the success
	 * @param t the t
	 */
	void finished(boolean success, Throwable t) {
		setDone(true);
		setSuccess(success);
		setCause(t);

		finish();
	}

	/* (non-Javadoc)
	 * @see io.netty.util.concurrent.Future#isSuccess()
	 */
	@Override
	public boolean isSuccess() {
		return success;
	}

	/* (non-Javadoc)
	 * @see io.netty.util.concurrent.Future#isCancellable()
	 */
	@Override
	public boolean isCancellable() {
		return true;
	}

	/* (non-Javadoc)
	 * @see io.netty.util.concurrent.Future#cause()
	 */
	@Override
	public Throwable cause() {
		return cause;
	}

	/* (non-Javadoc)
	 * @see io.netty.util.concurrent.Future#await(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		return latch.await(timeout, unit);
	}

	/* (non-Javadoc)
	 * @see io.netty.util.concurrent.Future#await(long)
	 */
	@Override
	public boolean await(long timeoutMillis) throws InterruptedException {
		return await(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	/* (non-Javadoc)
	 * @see io.netty.util.concurrent.Future#awaitUninterruptibly(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
		try {
			return await(timeout, unit);
		} catch (InterruptedException e) {
			setCause(e);
			setSuccess(false);
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see io.netty.util.concurrent.Future#awaitUninterruptibly(long)
	 */
	@Override
	public boolean awaitUninterruptibly(long timeoutMillis) {
		return awaitUninterruptibly(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	/* (non-Javadoc)
	 * @see io.netty.util.concurrent.Future#getNow()
	 */
	@Override
	public Void getNow() {
		return null;
	}

	/* (non-Javadoc)
	 * @see io.netty.util.concurrent.Future#cancel(boolean)
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		streamer.cancel();

		return true;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#isCancelled()
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#isDone()
	 */
	@Override
	public boolean isDone() {
		return done;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get()
	 */
	@Override
	public Void get() throws InterruptedException, ExecutionException {
		return null;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return null;
	}

	/* (non-Javadoc)
	 * @see io.netty.channel.ChannelFuture#channel()
	 */
	@Override
	public Channel channel() {
		return streamer.getChannel().getChannel();
	}

	/* (non-Javadoc)
	 * @see io.netty.channel.ChannelFuture#addListener(io.netty.util.concurrent.GenericFutureListener)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ChannelFuture addListener(GenericFutureListener<? extends Future<? super Void>> listener) {
		listeners.add((GenericFutureListener<ChannelFuture>) listener);
		return this;
	}

	/* (non-Javadoc)
	 * @see io.netty.channel.ChannelFuture#addListeners(io.netty.util.concurrent.GenericFutureListener[])
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ChannelFuture addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
		for (GenericFutureListener<? extends Future<? super Void>> l : listeners) {
			this.listeners.add((GenericFutureListener<ChannelFuture>) l);
		}

		return this;
	}

	/* (non-Javadoc)
	 * @see io.netty.channel.ChannelFuture#removeListener(io.netty.util.concurrent.GenericFutureListener)
	 */
	@Override
	public ChannelFuture removeListener(GenericFutureListener<? extends Future<? super Void>> listener) {
		listeners.remove(listener);
		return this;
	}

	/* (non-Javadoc)
	 * @see io.netty.channel.ChannelFuture#removeListeners(io.netty.util.concurrent.GenericFutureListener[])
	 */
	@Override
	public ChannelFuture removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
		for (GenericFutureListener<? extends Future<? super Void>> l : listeners) {
			this.listeners.remove(l);
		}

		return this;
	}

	/* (non-Javadoc)
	 * @see io.netty.channel.ChannelFuture#sync()
	 */
	@Override
	public ChannelFuture sync() throws InterruptedException {
		latch.await();
		return this;
	}

	/* (non-Javadoc)
	 * @see io.netty.channel.ChannelFuture#syncUninterruptibly()
	 */
	@Override
	public ChannelFuture syncUninterruptibly() {
		try {
			return sync();
		} catch (InterruptedException e) {
			setCause(e);
			setSuccess(false);
		}

		return this;
	}

	/* (non-Javadoc)
	 * @see io.netty.channel.ChannelFuture#await()
	 */
	@Override
	public ChannelFuture await() throws InterruptedException {
		return sync();
	}

	/* (non-Javadoc)
	 * @see io.netty.channel.ChannelFuture#awaitUninterruptibly()
	 */
	@Override
	public ChannelFuture awaitUninterruptibly() {
		return syncUninterruptibly();
	}

	private void finish() {
		for (GenericFutureListener<ChannelFuture> l : listeners) {
			try {
				l.operationComplete(this);
			} catch (Exception e) {
				log.error("Error notifying listener of completion", e);
			}
		}

		latch.countDown();
	}

	/**
	 * Sets the cause.
	 *
	 * @param cause the cause
	 */
	public void setCause(Throwable cause) {
		this.cause = cause;
	}

	/**
	 * Sets the success.
	 *
	 * @param success the success
	 */
	public void setSuccess(boolean success) {
		this.success = success;
	}

	/**
	 * Sets the cancelled.
	 *
	 * @param cancelled the cancelled
	 */
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	/**
	 * Sets the done.
	 *
	 * @param done the done
	 */
	public void setDone(boolean done) {
		this.done = done;
	}

}
