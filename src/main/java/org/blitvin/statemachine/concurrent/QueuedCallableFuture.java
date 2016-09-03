/*
 * Copyright (C) 2016 blitvin.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.blitvin.statemachine.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author blitvin
 */
public class QueuedCallableFuture<T> implements Future<T> {

    private volatile boolean isCancelled;
    private volatile Future<T> future;
    private final CountDownLatch latch;

    public QueuedCallableFuture() {
        isCancelled = false;
        future = null;
        latch = new CountDownLatch(1);
    }

    /* called from queue consumer when new future is available */
    void setFuture(Future<T> future) {
        this.future = future;
        latch.countDown();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (isCancelled) {
            latch.countDown();
            return false;
        } else {
            if (future == null) {
                isCancelled = true;
                return true;
            } else {
                isCancelled = future.cancel(mayInterruptIfRunning);
                return isCancelled;
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public boolean isDone() {
        return ((latch.getCount() == 0) && future.isDone());
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        latch.await();
        if (isCancelled)
            throw new CancellationException();
        return future.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        long start = System.nanoTime();
        if (!latch.await(timeout, unit)) {
            throw new TimeoutException();
        }
        long mult = 1L;
        switch (unit) {
            case DAYS:
                mult = 86400000000000L;
                break;
            case HOURS:
                mult = 3600000000L;
                break;
            case MICROSECONDS:
                mult = 1000L;
                break;
            case MILLISECONDS:
                mult = 1000000L;
                break;
            case MINUTES:
                mult = 60000000000L;
                break;
            case SECONDS:
                mult = 1000000000L;
                break;
        }
        if (isCancelled)
            throw new CancellationException();
        long end = System.nanoTime();
        timeout = mult * timeout - end + start;
        return future.get(timeout, TimeUnit.NANOSECONDS);
    }

}
