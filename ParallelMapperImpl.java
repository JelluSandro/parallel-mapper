package info.kgeorgiy.ja.shemyakin.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

/**
 * implementation for interface {@link ParallelMapper}
 */
public class ParallelMapperImpl implements ParallelMapper {

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     * @param f {@link Function} that will apply to args
     * @param args list of arguments
     * @param <T> type of elements args
     * @param <R> returned type
     * @return {@link List<R>} result of applying f to args
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        final CounterInteger counterInteger = new CounterInteger(args.size());
        List<R> list = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            final int finalI = i;
            list.add(null);
            synchronized (listThreads) {
                listThreads.add(new Record(counterInteger, () -> list.set(finalI, f.apply(args.get(finalI)))));
                listThreads.notify();
            }
        }
        counterInteger.myWait();
        return list;
    }

    /** Stops all threads. All unfinished mappings leave in undefined state. */
    @Override
    public void close() {
        arrayListTreads.forEach(Thread::interrupt);
        for (Thread thread : arrayListTreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private record Record(CounterInteger link, Runnable runnable) {
    }

    private static class CounterInteger {
        private int x;
        private final int sz;
        CounterInteger(int sz) {
            x = 0;
            this.sz = sz;
        }

        public void incrementX() {
            synchronized (this) {
                x++;
                if (x == sz) {
                    this.notify();
                }
            }
        }

        public void myWait() throws InterruptedException {
            synchronized (this) {
                while (x != sz) {
                    this.wait();
                }
            }
        }
    }

    private final Queue<Record> listThreads;
    private final List<Thread> arrayListTreads;

    /**
     * constructor that create mapper for threads {@link Thread}
     * @param threads counts of {@link Thread}
     */
    public ParallelMapperImpl(int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException();
        }
        listThreads = new LinkedList<>();
        arrayListTreads = new ArrayList<>();
        for (int countThread = 0; countThread < threads; countThread++) {
            final Thread thread = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        final CounterInteger link;
                        final Runnable runnable;
                        synchronized (listThreads) {
                            while (listThreads.isEmpty()) {
                                listThreads.wait();
                            }
                            Record record = listThreads.poll();
                            runnable = record.runnable;
                            link = record.link;
                        }
                        runnable.run();
                        link.incrementX();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    Thread.currentThread().interrupt();
                }
            });
            thread.start();
            arrayListTreads.add(thread);
        }
    }
}
