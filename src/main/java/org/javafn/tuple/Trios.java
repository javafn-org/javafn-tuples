package org.javafn.tuple;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Trios {

    @FunctionalInterface public interface TriFunction<A, B, C, R> { R apply(A a, B b, C c); }
    @FunctionalInterface public interface TriConsumer<A, B, C> { void accept(A a, B b, C c); }
    @FunctionalInterface public interface TriPredicate<A, B, C> { boolean test(A a, B b, C c); }

    public static <A, B, C> Stream<Trio<A, B, C>> zip(
            final Stream<A> streamA,
            final Stream<B> streamB,
            final Stream<C> streamC) {
        final Iterator<A> itrA = streamA.iterator();
        final Iterator<B> itrB = streamB.iterator();
        final Iterator<C> itrC = streamC.iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Trio<A, B, C>>() {
            @Override public boolean hasNext() { return itrA.hasNext() && itrB.hasNext() && itrC.hasNext(); }
            @Override public Trio<A, B, C> next() { return Trio.of(itrA.next(), itrB.next(), itrC.next()); }
        }, Spliterator.ORDERED), false);
    }

    public static <A, B, C, R> Function<Trio<A, B, C>, R> map(final TriFunction<A, B, C, R> fn)
    { return trio -> trio.map(fn); }
    public static <A, B, C, NA> Function<Trio<A, B, C>, Trio<NA, B, C>> map1(final Function<A,NA> fn)
    { return trio -> trio.map1(fn); }
    public static <A, B, C, NB> Function<Trio<A, B, C>, Trio<A, NB, C>> map2(final Function<B, NB> fn)
    { return trio -> trio.map2(fn); }
    public static <A, B, C, NC> Function<Trio<A, B, C>, Trio<A, B, NC>> map3(final Function<C, NC> fn)
    { return trio -> trio.map3(fn); }
    public static <A, B, C, NA> Function<Trio<A, B, C>, Trio<NA, B, C>> map1(final TriFunction<A, B, C, NA> fn)
    { return trio -> trio.map1(fn); }
    public static <A, B, C, NB> Function<Trio<A, B, C>, Trio<A, NB, C>> map2(final TriFunction<A, B, C, NB> fn)
    { return trio -> trio.map2(fn); }
    public static <A, B, C, NC> Function<Trio<A, B, C>, Trio<A, B, NC>> map3(final TriFunction<A, B, C, NC> fn)
    { return trio -> trio.map3(fn); }

    public static <A, B, C> Consumer<Trio<A, B, C>> peek(final TriConsumer<A, B, C> fn)
    { return trio -> trio.peek(fn); }
    public static <A, B, C> Consumer<Trio<A, B, C>> peek1(final Consumer<A> fn) { return trio -> trio.peek1(fn); }
    public static <A, B, C> Consumer<Trio<A, B, C>> peek2(final Consumer<B> fn) { return trio -> trio.peek2(fn); }
    public static <A, B, C> Consumer<Trio<A, B, C>> peek3(final Consumer<C> fn) { return trio -> trio.peek3(fn); }

    public static <A, B, C> Predicate<Trio<A, B, C>> matches(final TriPredicate<A, B, C> fn)
    { return trio -> trio.matches(fn); }
    public static <A, B, C> Predicate<Trio<A, B, C>> matches1(final Predicate<A> fn) { return trio -> trio.matches1(fn); }
    public static <A, B, C> Predicate<Trio<A, B, C>> matches2(final Predicate<B> fn) { return trio -> trio.matches2(fn); }
    public static <A, B, C> Predicate<Trio<A, B, C>> matches3(final Predicate<C> fn) { return trio -> trio.matches3(fn); }

    public static <A, B, C> Consumer<Trio<A, B, C>> consume(final TriConsumer<A, B, C> fn)
    { return trio -> trio.consume(fn); }

    public static <A, B, C, D> Function<Trio<A, B, C>, Quad<A, B, C, D>> toQuad(final Supplier<D> fn)
    { return trio -> Quad.of(trio.v1(), trio.v2(), trio.v3(), fn.get()); }

    public static <A, B, C, D> Function<Trio<A, B, C>, Quad<A, B, C, D>> toQuad(final TriFunction<A, B, C, D> fn) {
        return trio -> {
            final A a = trio.v1();
            final B b = trio.v2();
            final C c = trio.v3();
            return Quad.of(a, b, c, fn.apply(a, b, c));
        };
    }

    public static <A, B, C> Function<Trio<A,B,C>, Pair<A, B>> toPair() { return trio -> Pair.of(trio.v1(), trio.v2()); }

    public static <A> Stream<Trio<A, A, A>> chunks(A[] a) {
        final int len = a.length;
        final int nChunks = len / 3;
        final int nChunked = nChunks * 3;
        final int rem = len % 3;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 3).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])),
                rem == 0 ? Stream.empty() : Stream.of(Trio.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, null
                )));
    }
    public static Stream<Trio<Double, Double, Double>> chunks(double[] a) {
        final int len = a.length;
        final int nChunks = len / 3;
        final int nChunked = nChunks * 3;
        final int rem = len % 3;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 3).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])),
                rem == 0 ? Stream.empty() : Stream.of(Trio.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, null
                )));
    }
    public static Stream<Trio<Long, Long, Long>> chunks(long[] a) {
        final int len = a.length;
        final int nChunks = len / 3;
        final int nChunked = nChunks * 3;
        final int rem = len % 3;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 3).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])),
                rem == 0 ? Stream.empty() : Stream.of(Trio.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, null
                )));
    }
    public static Stream<Trio<Integer, Integer, Integer>> chunks(int[] a) {
        final int len = a.length;
        final int nChunks = len / 3;
        final int nChunked = nChunks * 3;
        final int rem = len % 3;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 3).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])),
                rem == 0 ? Stream.empty() : Stream.of(Trio.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, null
                )));
    }

    public static <A> Stream<Trio<A, A, A>> windows(A[] a)
    { return IntStream.range(0, a.length - 2).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])); }
    public static Stream<Trio<Double, Double, Double>> windows(double[] a)
    { return IntStream.range(0, a.length - 2).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])); }
    public static Stream<Trio<Long, Long, Long>> windows(long[] a)
    { return IntStream.range(0, a.length - 2).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])); }
    public static Stream<Trio<Integer, Integer, Integer>> windows(int[] a)
    { return IntStream.range(0, a.length - 2).mapToObj(i -> Trio.of(a[i], a[i+1], a[i+2])); }

    public static <A> Function<Trio<A,A,A>, Stream<A>> stream() { return Trios::stream; }
    public static <A> Stream<A> stream(final Trio<A, A, A> trio) { return Stream.of(trio.v1(), trio.v2(), trio.v3()); }

    private Trios() { throw new IllegalStateException("This is a static class and should never be instantiated"); }
}
