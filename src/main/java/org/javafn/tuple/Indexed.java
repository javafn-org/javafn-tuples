package org.javafn.tuple;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Indexed {
    @FunctionalInterface public interface IndexedFunction<TT, R> { R apply(int i, TT tt); }
    @FunctionalInterface public interface IndexedConsumer<TT> { void accept(int i, TT tt); }
    @FunctionalInterface public interface IndexedPredicate<TT> { boolean test(int i, TT tt); }

    public static class StreamIndexer<TT> {
        int count = 0;

        public Index<TT> accept(final TT t) {
            return new Index<>(count++, t);
        }
    }

    public static <TT, R> Function<Index<TT>, Index<R>> map(final IndexedFunction<TT, R> fn) { return index -> index.map(fn); }
    public static <TT, R> Function<Index<TT>, Index<R>> map(final Function<TT, R> fn) { return index -> index.map(fn); }
    public static <TT, R> Function<Index<TT>, R> mapDropIndex(final IndexedFunction<TT, R> fn) { return index -> index.mapDropIndex(fn); }
    public static <TT> Consumer<Index<TT>> peek(final IndexedConsumer<TT> fn) { return index -> index.peek(fn); }
    public static <TT> Predicate<Index<TT>> matches(final IndexedPredicate<TT> fn) { return index -> index.matches(fn); }

    public static <TT> Stream<Index<TT>> index(final Stream<TT> stream) {
        return stream.map(new StreamIndexer<TT>()::accept);
    }

    public static <TT> Function<TT, Index<TT>> index() {
        return new StreamIndexer<TT>()::accept;
    }

    private Indexed() { throw new AssertionError("This is a static class and should never be instantiated"); }
}
