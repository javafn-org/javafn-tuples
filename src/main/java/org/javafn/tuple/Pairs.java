package org.javafn.tuple;

import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.*;
import java.util.stream.*;

public class Pairs {

    /**
     * Return a new stream that forms pairs from the supplied streams.  The length of the new stream is
     * the shorter of the two.
     * <pre>{@code
     * final IntStream left = IntStream.range(0, 100);
     * final LongStream right = LongStream.range(1000, 2000);
     * final List<Pair<Integer, Long>> zipped = Pair.zip(left, right).collect(Collectors.toList());
     * System.out.println(zipped.size());
     * // 100
     * System.out.println(zipped.get(0));
     * // Pair(0, 1000)
     * }</pre>
     */
    public static <A, B> Stream<Pair<A, B>> zip(final Stream<A> streamA, final Stream<B> streamB) {
        final Iterator<A> itrA = streamA.iterator();
        final Iterator<B> itrB = streamB.iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Pair<A, B>>() {
            @Override public boolean hasNext() { return itrA.hasNext() && itrB.hasNext(); }
            @Override public Pair<A, B> next() { return Pair.of(itrA.next(), itrB.next()); }
        }, Spliterator.ORDERED), false);
    }

    /**
     * Partition the supplied stream using the supplied predicate.  True instances will be returned in the first
     * element and false instances will be returned in the second element.
     * <pre>{@code
     * final Pair<Stream<Integer> Stream<Integer>> evenOdd =  Pair.partition(IntStream.range(0, 100), i -> i % 2 == 0);
     * final List<Integer> even = evenOdd.v1().collect(Collectors.toList());
     * final List<Integer> odd = evenOdd.v2().collect(Collectors.toList());
     * System.out.println(even.size() + ", " + odd.size());
     * // 50, 50
     * }</pre>
     * TODO: Turn this into a collector similar to {@link Collectors#partitioningBy(Predicate)}
     */
    public static <X> Pair<Stream<X>, Stream<X>> partition(final Stream<X> stream, final Predicate<X> fn) {
        final Stream.Builder<X> trueStream = Stream.builder();
        final Stream.Builder<X> falseStream = Stream.builder();
        stream.forEach(x -> {
            if (fn.test(x)) trueStream.add(x);
            else falseStream.add(x);
        });
        return Pair.of(trueStream.build(), falseStream.build());
    }

    /**
     * Return a function that accepts a Pair and calls {@link Pair#map(BiFunction)} on it.
     */
    public static <A, B, R> Function<Pair<A, B>, R> map(final BiFunction<A, B, R> fn) { return pair -> pair.map(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#map1(Function)} on it.
     */
    public static <A, B, R> Function<Pair<A, B>, Pair<R, B>> map1(final Function<A, R> fn)
    { return pair -> pair.map1(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#map2(Function)} on it.
     */
    public static <A, B, R> Function<Pair<A, B>, Pair<A, R>> map2(final Function<B, R> fn)
    { return pair -> pair.map2(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#map1(BiFunction)} on it.
     */
    public static <A, B, R> Function<Pair<A, B>, Pair<R, B>> map1(final BiFunction<A, B, R> fn)
    { return pair -> pair.map1(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#map2(BiFunction)} on it.
     */
    public static <A, B, R> Function<Pair<A, B>, Pair<A, R>> map2(final BiFunction<A, B, R> fn)
    { return pair -> pair.map2(fn); }

    /* TODO: document */
    /* TODO: Duplicate in Trio and Quad */
    public static <A, B, R> Function<Pair<A, B>, Stream<Pair<R, B>>> flatMap1(final Function<A, Stream<R>> fn)
    { return pair -> pair.flatMap1(fn); }
    public static <A, B, R> Function<Pair<A, B>, Stream<Pair<A, R>>> flatMap2(final Function<B, Stream<R>> fn)
    { return pair -> pair.flatMap2(fn); }
    public static <A, B, R> Function<Pair<A, B>, Stream<Pair<R, B>>> flatMap1(final BiFunction<A, B, Stream<R>> fn)
    { return pair -> pair.flatMap1(fn); }
    public static <A, B, R> Function<Pair<A, B>, Stream<Pair<A, R>>> flatMap2(final BiFunction<A, B, Stream<R>> fn)
    { return pair -> pair.flatMap2(fn); }

    /**
     * Return a function that accepts a Pair and calls {@link Pair#peek(BiConsumer)} on it.
     */
    public static <A, B> Consumer<Pair<A, B>> peek(final BiConsumer<A, B> fn) { return pair -> pair.peek(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#peek1(Consumer)} on it.
     */
    public static <A, B> Consumer<Pair<A, B>> peek1(final Consumer<A> fn) { return pair -> pair.peek1(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#peek2(Consumer)} on it.
     */
    public static <A, B> Consumer<Pair<A, B>> peek2(final Consumer<B> fn) { return pair -> pair.peek2(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#matches(BiPredicate)} on it.
     */
    public static <A, B> Predicate<Pair<A, B>> matches(final BiPredicate<A, B> fn) { return pair -> pair.matches(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#matches1(Predicate)} on it.
     */
    public static <A, B> Predicate<Pair<A, B>> matches1(final Predicate<A> fn) { return pair -> pair.matches1(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#matches2(Predicate)} on it.
     */
    public static <A, B> Predicate<Pair<A, B>> matches2(final Predicate<B> fn) { return pair -> pair.matches2(fn); }
    /**
     * Return a function that accepts a Pair and calls {@link Pair#consume(BiConsumer)} on it.
     */
    public static <A, B> Consumer<Pair<A, B>> consume(final BiConsumer<A, B> fn) { return pair -> pair.consume(fn); }

    /**
     * Return a function that accepts a pair and uses the supplied function to generate a third element,
     * then creates a Trio from the two elements of the pair and the generated value.
     * <pre>{@code
     * final Pair<String, Integer> pair = Pair.of("42", 42);
     * final Trio<String, Integer, UUID> trio = Pair.ToTrio(UUID::randomUUID).apply(pair);
     * }</pre>
     * Useful in streams, i.e., you shouldn't need to call apply directly.
     * <pre>{@code
     * stream.map(Pair.ToTrio(() -> "A third value"));
     * }</pre>
     */
    public static <A, B, C> Function<Pair<A, B>, Trio<A, B, C>> toTrio(final Supplier<C> fn)
    { return pair -> Trio.of(pair.v1(), pair.v2(), fn.get()); }

    /**
     * Return a function that accepts a pair and uses the supplied bi-function to generate a third element,
     * then creates a Trio from the two elements of the pair and the generated value.
     * <pre>{@code
     * final Pair<String, Integer> pair = Pair.of("42", 42);
     * final Trio<String, Integer, String> trio = Pair.ToTrio((s, i) -> s + ", " + Integer.toString(i)).apply(pair);
     * }</pre>
     * Useful in streams, i.e., you shouldn't need to call apply directly.
     * <pre>{@code
     * stream.map(Pair.ToTrio((key, newValue) -> someDatabase.get(key).replaceAndReturnPrevious(newValue)));
     * // Trio("somekey", "the new value", "the old value")
     * }</pre>
     */
    public static <A, B, C> Function<Pair<A, B>, Trio<A, B, C>> toTrio(final BiFunction<A, B, C> fn) {
        return pair -> {
            final A a = pair.v1();
            final B b = pair.v2();
            return Trio.of(a, b, fn.apply(a, b));
        };
    }

    /**
     * Turn the supplied array into a stream of pairs where each element in the array appears once in the stream.
     * If the length of a is odd, the last element will contain null for the second pair element.
     * <pre>{@code
     * final Integer[] a = {1, 2, 3, 4, 5};
     * Pair.chunks(a).forEach(System.out::println);
     * // Prints
     * // Pair(1, 2)
     * // Pair(3, 4)
     * // Pair(5, null)
     * }</pre>
     * The elements are not copied, so changes made to the objects within the pairs will be reflected in
     * the supplied array.
     * @param a the array to chunk
     * @return the entries in the supplied array, chunked
     */
    public static <A> Stream<Pair<A, A>> chunks(A[] a) {
        final int len = a.length;
        return Stream.concat(
                IntStream.range(0, len / 2).map(i -> i * 2).mapToObj(i -> Pair.of(a[i], a[i+1])),
                len % 2 == 0 ? Stream.empty() : Stream.of(Pair.of(
                        a[len - 1], null
                )));
    }
    /** @see #chunks(Object[]) */
    public static Stream<Pair<Double, Double>> chunks(double[] a) {
        final int len = a.length;
        return Stream.concat(
                IntStream.range(0, len / 2).map(i -> i * 2).mapToObj(i -> Pair.of(a[i], a[i+1])),
                len % 2 == 0 ? Stream.empty() : Stream.of(Pair.of(
                        a[len - 1], null
                )));
    }
    /** @see #chunks(Object[]) */
    public static Stream<Pair<Long, Long>> chunks(long[] a) {
        final int len = a.length;
        return Stream.concat(
                IntStream.range(0, len / 2).map(i -> i * 2).mapToObj(i -> Pair.of(a[i], a[i+1])),
                len % 2 == 0 ? Stream.empty() : Stream.of(Pair.of(
                        a[len - 1], null
                )));
    }
    /** @see #chunks(Object[]) */
    public static Stream<Pair<Integer, Integer>> chunks(int[] a) {
        final int len = a.length;
        return Stream.concat(
                IntStream.range(0, len / 2).map(i -> i * 2).mapToObj(i -> Pair.of(a[i], a[i+1])),
                len % 2 == 0 ? Stream.empty() : Stream.of(Pair.of(
                        a[len - 1], null
                )));
    }

    /**
     * Turn the supplied array into a stream of pairs where each element in the array appears twice in the stream,
     * once as the second element of a pair and then again as the first element of a pair,
     * except for the first and last element, which appear once, as the first and last element respectively.
     * This function creates a "sliding window" of two elements which moves forward one element at a time.
     * <pre>{@code
     * final Integer[] a = {1, 2, 3, 4};
     * Pair.chunks(a).forEach(System.out::println);
     * // Prints
     * // Pair(1, 2)
     * // Pair(2, 3)
     * // Pair(3, 4)
     * }</pre>
     * The elements are not copied, so changes made to the objects within the pairs will be reflected in
     * the supplied array.
     * @param a the array over which is created a sliding window
     * @return a stream of pairs forming a sliding window over the supplied array
     */
    public static <A> Stream<Pair<A, A>> windows(A[] a)
    { return IntStream.range(0, a.length - 1).mapToObj(i -> Pair.of(a[i], a[i+1])); }
    /** @see #windows(Object[]) */
    public static Stream<Pair<Double, Double>> windows(double[] a)
    { return IntStream.range(0, a.length - 1).mapToObj(i -> Pair.of(a[i], a[i+1])); }
    /** @see #windows(Object[]) */
    public static Stream<Pair<Long, Long>> windows(long[] a)
    { return IntStream.range(0, a.length - 1).mapToObj(i -> Pair.of(a[i], a[i+1])); }
    /** @see #windows(Object[]) */
    public static Stream<Pair<Integer, Integer>> windows(int[] a)
    { return IntStream.range(0, a.length - 1).mapToObj(i -> Pair.of(a[i], a[i+1])); }

    /**
     * Return a function that accepts a Pair whose elements are the same type and returns a stream
     * of two elements which are the first and second elements of the pair respectively.
     */
    public static <A> Function<Pair<A,A>, Stream<A>> stream() { return Pairs::stream; }

    /**
     * Accept a Pair whose elements are the same type and return a stream
     * of two elements which are the first and second elements of the pair respectively.
     */
    public static <A> Stream<A> stream(final Pair<A, A> pair) { return Stream.of(pair.v1(), pair.v2()); }

    /**
     * Return a {@link Collector} that maps pairs to map entries where the first entry is the key and the second is the value.
     */
    public static <KEY, VAL> Collector<Pair<? extends KEY, ? extends VAL>, ?, Map<KEY, VAL>> toMap() {
        return Collectors.toMap(Pair::v1, Pair::v2);
    }

    /** Return a function that calls {@link Pair#from(Map.Entry)}. */
    public static <KEY, VAL> Function<Map.Entry<KEY, VAL>, Pair<KEY, VAL>> from() { return Pair::from; }

    private Pairs() { throw new AssertionError("This is a static class and should never be instantiated"); }
}
