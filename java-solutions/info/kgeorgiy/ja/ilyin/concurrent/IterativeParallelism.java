package info.kgeorgiy.ja.ilyin.concurrent;


import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * The type Iterative parallelism.
 *
 * @author Yaroslav Ilin
 */
public class IterativeParallelism implements AdvancedIP {
    private ParallelMapper mapper = null;
    private boolean isMapper = false;

    /**
     * Create class without mapper
     */
    public IterativeParallelism() {
    }

    /**
     * Create class with mapper
     * @param mapper to mapping
     */
    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
        this.isMapper = true;
    }

    private <T, R> R find(int threads, final List<? extends T> values, Function<? super Stream<? extends T>, R> finder,
                          final Function<? super Stream<R>, R> reducer) throws InterruptedException {
        List<Stream<? extends T>> divided = new ArrayList<>();
        if (threads > values.size()) {
            threads = values.size();
        }
        final int block = values.size() / threads;
        final int reminder = values.size() % threads;
        int left = 0;
        for (int i = 0; i < threads; i++) {
            int right = left + block + (i < reminder ? 1 : 0);
            divided.add(values.subList(left, right).stream());
            left = right;
        }
        final List<R> result = isMapper ? mapper.map(finder, divided) : mapping(finder, divided);
        return reducer.apply(result.stream());
    }

    private <T, R> List<R> mapping(Function<? super Stream<? extends T>, R> finder, List<Stream<? extends T>> divided) throws InterruptedException {
        final List<R> result = new ArrayList<>(Collections.nCopies(divided.size(), null));
        final List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < divided.size(); i++) {
            final int ind = i;
            final Thread thread = new Thread(() -> result.set(ind, finder.apply(divided.get(ind))));
            workers.add(thread);
            thread.start();
        }
        joinThreads(workers);
        return result;
    }

    public static void joinThreads(List<Thread> workers) throws InterruptedException {
        for (int i = 0, workersSize = workers.size(); i < workersSize; i++) {
            Thread thread = workers.get(i);
            try {
                thread.join();
            } catch (InterruptedException e) {
                final InterruptedException exception = new InterruptedException("can't end all threads");
                exception.addSuppressed(e);
                for (int j = i; j < workersSize; j++) {
                    workers.get(j).interrupt();
                }
                for (int j = i; j < workersSize; j++) {
                    try {
                        workers.get(j).join();
                    } catch (InterruptedException e1) {
                        exception.addSuppressed(e1);
                    }
                }
                throw exception;
            }
        }
    }

    /**
     *
     * @param threads number or concurrent threads.
     * @param values values to get maximum of.
     * @param comparator value comparator.
     * @param <T> value type
     * @return maximum value.
     * @throws InterruptedException if threads are interrupted.
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        final Function<Stream<? extends T>, T> maxStream = stream -> stream.max(comparator).orElse(null);
        return find(threads, values, maxStream, maxStream);
    }

    /**
     *
     * @param threads number or concurrent threads.
     * @param values values to get minimum of.
     * @param comparator value comparator.
     * @param <T> value type
     * @return minimum value.
     * @throws InterruptedException if threads are interrupted
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    /**
     *
     * @param threads number or concurrent threads.
     * @param values values to test.
     * @param predicate test predicate.
     * @param <T> value type
     * @return result value.
     * @throws InterruptedException if threads are interrupted
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return find(threads,
                values,
                stream -> stream.allMatch(predicate),
                booleanStream -> booleanStream.allMatch(Boolean::booleanValue));
    }

    /**
     *
     * @param threads number or concurrent threads.
     * @param values values to test.
     * @param predicate test predicate.
     * @param <T> value type.
     * @return result value.
     * @throws InterruptedException if threads are interrupted.
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return find(threads,
                values,
                stream -> stream.anyMatch(predicate),
                booleanStream -> booleanStream.anyMatch(Boolean::booleanValue));
    }

    /**
     *
     * @param threads number of concurrent threads.
     * @param values values to join.
     *
     * @return result string.
     * @throws InterruptedException if threads are interrupted.
     */
    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return find(threads,
                values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stringsStream -> stringsStream.collect(Collectors.joining()));
    }

    /**
     *
     * @param threads number of concurrent threads.
     * @param values values to filter.
     * @param predicate filter predicate.
     * @param <T> value type
     * @return result values.
     * @throws InterruptedException if threads are interrupted.
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return find(threads,
                values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     *
     * @param threads number of concurrent threads.
     * @param values values to filter.
     * @param f mapper function.
     *
     * @param <T> value type
     * @param <U> mapped type
     * @return result values.
     * @throws InterruptedException if threads are interrupted.
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f) throws InterruptedException {
        return find(threads,
                values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    private <T, R> R reduceMonoid(int threads, List<T> values, Function<? super T, R> lift, Monoid<R> monoid) throws InterruptedException {
        Function<? super Stream<R>, R> function =
                rStream -> rStream.reduce(monoid.getOperator()).orElse(monoid.getIdentity());
        return find(threads,
                values,
                stream -> function.apply(stream.map(lift)),
                function);
    }

    /**
     *
     * @param threads number of concurrent threads.
     * @param values values to reduce.
     * @param monoid monoid to use.
     *
     * @param <T> value type
     * @return result value
     * @throws InterruptedException if threads are interrupted
     */
    @Override
    public <T> T reduce(int threads, List<T> values, Monoid<T> monoid) throws InterruptedException {
        return reduceMonoid(threads, values, t -> t, monoid);
    }

    /**
     *
     * @param threads number of concurrent threads.
     * @param values values to reduce.
     * @param lift mapping function.
     * @param monoid monoid to use.
     *
     * @param <T> value type
     * @param <R> map type
     * @return result value
     * @throws InterruptedException if threads are interrupted
     */
    @Override
    public <T, R> R mapReduce(int threads, List<T> values, Function<T, R> lift, Monoid<R> monoid) throws InterruptedException {
        return reduceMonoid(threads, values, lift, monoid);
    }
}
