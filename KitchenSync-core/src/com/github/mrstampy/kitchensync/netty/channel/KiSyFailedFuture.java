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
package com.github.mrstampy.kitchensync.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Internal class to use if no future available from Netty for failed socket
 * operations. {@link #isSuccess()} always returns false.
 */
public class KiSyFailedFuture implements ChannelFuture {

	private Throwable cause;

	/**
	 * The Constructor.
	 */
	public KiSyFailedFuture() {

	}

	/**
	 * The Constructor.
	 *
	 * @param cause
	 *          the cause
	 */
	public KiSyFailedFuture(Throwable cause) {
		this.cause = cause;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.util.concurrent.Future#isSuccess()
	 */
	@Override
	public boolean isSuccess() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.util.concurrent.Future#isCancellable()
	 */
	@Override
	public boolean isCancellable() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.util.concurrent.Future#cause()
	 */
	@Override
	public Throwable cause() {
		return cause;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.util.concurrent.Future#await(long,
	 * java.util.concurrent.TimeUnit)
	 */
	@Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.util.concurrent.Future#await(long)
	 */
	@Override
	public boolean await(long timeoutMillis) throws InterruptedException {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.util.concurrent.Future#awaitUninterruptibly(long,
	 * java.util.concurrent.TimeUnit)
	 */
	@Override
	public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.util.concurrent.Future#awaitUninterruptibly(long)
	 */
	@Override
	public boolean awaitUninterruptibly(long timeoutMillis) {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.util.concurrent.Future#getNow()
	 */
	@Override
	public Void getNow() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.util.concurrent.Future#cancel(boolean)
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Future#get()
	 */
	@Override
	public Void get() throws InterruptedException, ExecutionException {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public Void get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Future#isCancelled()
	 */
	@Override
	public boolean isCancelled() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Future#isDone()
	 */
	@Override
	public boolean isDone() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.ChannelFuture#channel()
	 */
	@Override
	public Channel channel() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.ChannelFuture#addListener(io.netty.util.concurrent.
	 * GenericFutureListener)
	 */
	@Override
	public ChannelFuture addListener(GenericFutureListener<? extends Future<? super Void>> listener) {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.ChannelFuture#addListeners(io.netty.util.concurrent.
	 * GenericFutureListener[])
	 */
	@Override
	public ChannelFuture addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.netty.channel.ChannelFuture#removeListener(io.netty.util.concurrent.
	 * GenericFutureListener)
	 */
	@Override
	public ChannelFuture removeListener(GenericFutureListener<? extends Future<? super Void>> listener) {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.netty.channel.ChannelFuture#removeListeners(io.netty.util.concurrent
	 * .GenericFutureListener[])
	 */
	@Override
	public ChannelFuture removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.ChannelFuture#sync()
	 */
	@Override
	public ChannelFuture sync() throws InterruptedException {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.ChannelFuture#syncUninterruptibly()
	 */
	@Override
	public ChannelFuture syncUninterruptibly() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.ChannelFuture#await()
	 */
	@Override
	public ChannelFuture await() throws InterruptedException {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.netty.channel.ChannelFuture#awaitUninterruptibly()
	 */
	@Override
	public ChannelFuture awaitUninterruptibly() {
		return this;
	}

}
