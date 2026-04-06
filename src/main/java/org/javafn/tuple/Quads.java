package org.javafn.tuple;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Quads {

    @FunctionalInterface public interface QuadFunction<A, B, C, D, R> { R apply(A a, B b, C c, D d); }
    @FunctionalInterface public interface QuadConsumer<A, B, C, D> { void accept(A a, B b, C c, D d); }
    @FunctionalInterface public interface QuadPredicate<A, B, C, D> { boolean test(A a, B b, C c, D d); }

    public static <A, B, C, D> Stream<Quad<A, B, C, D>> zip(
            final Stream<A> streamA,
            final Stream<B> streamB,
            final Stream<C> streamC,
            final Stream<D> streamD) {
        final Iterator<A> itrA = streamA.iterator();
        final Iterator<B> itrB = streamB.iterator();
        final Iterator<C> itrC = streamC.iterator();
        final Iterator<D> itrD = streamD.iterator();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Quad<A, B, C, D>>() {
            @Override
            public boolean hasNext() { return itrA.hasNext() && itrB.hasNext() && itrC.hasNext() && itrD.hasNext(); }
            @Override
            public Quad<A, B, C, D> next() { return Quad.of(itrA.next(), itrB.next(), itrC.next(), itrD.next()); }
        }, Spliterator.ORDERED), false);
    }

    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,R> map(final QuadFunction<A, B, C, D, R> fn)
    { return quad -> quad.map(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<R,B,C,D>> map1(final Function<A, R> fn)
    { return quad -> quad.map1(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<A,R,C,D>> map2(final Function<B, R> fn)
    { return quad -> quad.map2(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<A,B,R,D>> map3(final Function<C, R> fn)
    { return quad -> quad.map3(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<A,B,C,R>> map4(final Function<D, R> fn)
    { return quad -> quad.map4(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<R,B,C,D>> map1(final QuadFunction<A, B, C, D, R> fn)
    { return quad -> quad.map1(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<A,R,C,D>> map2(final QuadFunction<A, B, C, D, R> fn)
    { return quad -> quad.map2(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<A,B,R,D>> map3(final QuadFunction<A, B, C, D, R> fn)
    { return quad -> quad.map3(fn); }
    public static <A,B,C,D,R> Function<Quad<A,B,C,D>,Quad<A,B,C,R>> map4(final QuadFunction<A, B, C, D, R> fn)
    { return quad -> quad.map4(fn); }
    public static <A,B,C,D> Consumer<Quad<A,B,C,D>> peek(final QuadConsumer<A, B, C, D> fn)
    { return quad -> quad.peek(fn); }
    public static <A,B,C,D> Consumer<Quad<A,B,C,D>> peek1(final Consumer<A> fn) { return quad -> quad.peek1(fn); }
    public static <A,B,C,D> Consumer<Quad<A,B,C,D>> peek2(final Consumer<B> fn) { return quad -> quad.peek2(fn); }
    public static <A,B,C,D> Consumer<Quad<A,B,C,D>> peek3(final Consumer<C> fn) { return quad -> quad.peek3(fn); }
    public static <A,B,C,D> Consumer<Quad<A,B,C,D>> peek4(final Consumer<D> fn) { return quad -> quad.peek4(fn); }
    public static <A,B,C,D> Predicate<Quad<A,B,C,D>> matches(final QuadPredicate<A, B, C, D> fn)
    { return quad -> quad.matches(fn); }
    public static <A,B,C,D> Predicate<Quad<A,B,C,D>> matches1(final Predicate<A> fn) { return quad -> quad.matches1(fn); }
    public static <A,B,C,D> Predicate<Quad<A,B,C,D>> matches2(final Predicate<B> fn) { return quad -> quad.matches2(fn); }
    public static <A,B,C,D> Predicate<Quad<A,B,C,D>> matches3(final Predicate<C> fn) { return quad -> quad.matches3(fn); }
    public static <A,B,C,D> Predicate<Quad<A,B,C,D>> matches4(final Predicate<D> fn) { return quad -> quad.matches4(fn); }
    public static <A,B,C,D> Consumer<Quad<A,B,C,D>> consume(final QuadConsumer<A, B, C, D> fn)
    { return quad -> quad.consume(fn); }
    public static <A, B, C, D> Function<Quad<A,B,C,D>, Trio<A, B, C>> toTrio()
    { return quad -> Trio.of(quad.v1(), quad.v2(), quad.v3()); }

    public static <A> Stream<Quad<A, A, A, A>> chunks(A[] a) {
        final int len = a.length;
        final int nChunks = len / 4;
        final int nChunked = nChunks * 4;
        final int rem = len % 4;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 4).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])),
                rem == 0 ? Stream.empty() : Stream.of(Quad.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, rem == 3 ? a[nChunked+2] : null, null
                )));
    }
    public static Stream<Quad<Double, Double, Double, Double>> chunks(double[] a) {
        final int len = a.length;
        final int nChunks = len / 4;
        final int nChunked = nChunks * 4;
        final int rem = len % 4;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 4).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])),
                rem == 0 ? Stream.empty() : Stream.of(Quad.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, rem == 3 ? a[nChunked+2] : null, null
                )));
    }
    public static Stream<Quad<Long, Long, Long, Long>> chunks(long[] a) {
        final int len = a.length;
        final int nChunks = len / 4;
        final int nChunked = nChunks * 4;
        final int rem = len % 4;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 4).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])),
                rem == 0 ? Stream.empty() : Stream.of(Quad.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, rem == 3 ? a[nChunked+2] : null, null
                )));
    }
    public static Stream<Quad<Integer, Integer, Integer, Integer>> chunks(int[] a) {
        final int len = a.length;
        final int nChunks = len / 4;
        final int nChunked = nChunks * 4;
        final int rem = len % 4;
        return Stream.concat(
                IntStream.range(0, nChunks).map(i -> i * 4).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])),
                rem == 0 ? Stream.empty() : Stream.of(Quad.of(
                        a[nChunked], rem == 2 ? a[nChunked + 1] : null, rem == 3 ? a[nChunked+2] : null, null
                )));
    }

    public static <A> Stream<Quad<A, A, A, A>> windows(A[] a)
    { return IntStream.range(0, a.length - 3).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])); }
    public static Stream<Quad<Double, Double, Double, Double>> windows(double[] a)
    { return IntStream.range(0, a.length - 3).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])); }
    public static Stream<Quad<Long, Long, Long, Long>> windows(long[] a)
    { return IntStream.range(0, a.length - 3).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])); }
    public static Stream<Quad<Integer, Integer, Integer, Integer>> windows(int[] a)
    { return IntStream.range(0, a.length - 3).mapToObj(i -> Quad.of(a[i], a[i+1], a[i+2], a[i+3])); }

    public static <A> Function<Quad<A,A,A,A>, Stream<A>> stream() { return Quads::stream; }
    public static <A> Stream<A> stream(final Quad<A,A,A,A> quad)
    { return Stream.of(quad.v1(), quad.v2(), quad.v3(), quad.v4()); }

    private Quads() { throw new AssertionError("This is a static class and should never be instantiated"); }
}
