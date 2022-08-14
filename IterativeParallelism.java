package info.kgeorgiy.ja.shemyakin.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.Math.min;

/**
 * implementation for interface {@link ScalarIP}
 */
public class IterativeParallelism implements ScalarIP {

    private ParallelMapper parallelMapper;
    private boolean isParallelMapper = false;

    /**
     * constructor that will use {@link ParallelMapper}
     * @param parallelMapper {@link ParallelMapper} mapper, that will be used to apply to block of argument
     */
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
        isParallelMapper = true;
    }

    /**
     * default constructor
     */
    public IterativeParallelism() {

    }

    private <T, E> E parallelism(int threads, List<? extends T> values,
                                 Function<List<? extends T>, ? extends E> streamFunT,
                                 Function<List<? extends E>, E> streamFunE) throws InterruptedException {
        if (threads < 1) {
            throw new IllegalArgumentException();
        }
        final int sz = values.size();
        final int minThread = min(threads, sz);
        final int block = sz / minThread;
        final int residue = sz % minThread;
        int count_residue = 0;
        List<E> ans = new ArrayList<>();
        final List<Thread> threadList = new ArrayList<>();
        List<List<? extends T>> blocksThread = new ArrayList<>();
        for (int countThread = 0; countThread < minThread; countThread++) {
            // :NOTE: naming
            int finalCount_residue = count_residue;
            final List<? extends T> blockThread = values.subList(block * countThread + finalCount_residue,
                    min(block * (countThread + 1)
                            + finalCount_residue + ((finalCount_residue != residue) ? 1 : 0), sz));
            blocksThread.add(blockThread);
            ans.add(null);
            if (count_residue < residue) {
                count_residue++;
            }
        }
        if (isParallelMapper) {
            return streamFunE.apply(parallelMapper.map(streamFunT, blocksThread));
        } else {
            for (int countThread = 0; countThread < minThread; countThread++) {
                final int finalCountThread = countThread;
                final Thread thread = new Thread(()
                        -> ans.set(finalCountThread, streamFunT.apply(blocksThread.get(finalCountThread))));
                thread.start();
                threadList.add(thread);
            }
        }
        for (Thread thread : threadList) {
            // :NOTE: утечка
            thread.join();
        }
        return streamFunE.apply(ans);
    }


    /**
     * Returns maximum value.
     *
     * @param threads    number or concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return maximum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if no values are given.
     * @throws IllegalArgumentException         if treads less than 1
     */
    @Override
    // :NOTE: min -> max
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return parallelism(threads,
                values,
                streamMax(comparator),
                streamMax(comparator));
    }

    private <T> Function<List<? extends T>, T> streamMax(Comparator<? super T> comparator) {
        return ts -> ts.stream().max(comparator).orElse(null);
    }

    /**
     * Returns minimum value.
     *
     * @param <T>        value type.
     * @param threads    number or concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @return minimum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if no values are given.
     * @throws IllegalArgumentException         if treads less than 1
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return parallelism(threads,
                values,
                streamMin(comparator),
                streamMin(comparator));
    }

    private <T> Function<List<? extends T>, T> streamMin(Comparator<? super T> comparator) {
        return ts -> ts.stream().min(comparator).orElse(null);
    }

    /**
     * Returns whether all values satisfy predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether all values satisfy predicate or {@code true}, if no values are given
     * @throws InterruptedException     if executing thread was interrupted.
     * @throws IllegalArgumentException if treads less than 1
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelism(threads,
                values,
                streamAll(predicate),
                streamAll(t -> t));
    }

    private <T> Function<List<? extends T>, Boolean> streamAll(Predicate<? super T> predicate) {
        return ts -> ts.stream().allMatch(predicate);
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether any value satisfies predicate or {@code false}, if no values are given
     * @throws InterruptedException     if executing thread was interrupted.
     * @throws IllegalArgumentException if treads less than 1
     */
    @Override
    // :NOTE: any -> all
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelism(threads,
                values,
                streamAny(predicate),
                streamAny(t -> t));
    }


    private <T> Function<List<? extends T>, Boolean> streamAny(Predicate<? super T> predicate) {
        return ts -> ts.stream().anyMatch(predicate);
    }

}
