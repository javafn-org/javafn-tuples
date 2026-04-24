package org.javafn.tuple;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A tuple similar to the Pair type in <a href="https://www.javatuples.org/">javatuples</a> that supports better
 * fluent functional usage.
 * <pre>{@code
 * IntStream.range(0, 10).mapToObj(i -> org.javatuples.Pair.with(i, i*2))
 *     .filter(pair -> pair.getValue0() % 2 == 0)
 *     .map(pair -> pair.getValue0() + pair.getValue1())
 *     .forEach(System.out::println);
 * }</pre>
 * versus
 * <pre>{@code
 * IntStream.range(0, 10).mapToObj(i -> Pair.of(i, i*2))
 *     .filter(pair -> pair.filter1(i -> i % 2 == 0)
 *     .map(pair -> pair.map( (l, r) -> l + r))
 *     .forEach(System.out::println);
 * }</pre>
 * Or better yet,
 * <pre>{@code
 * IntStream.range(0, 10).mapToObj(i -> Pair.of(i, i*2))
 *     .filter(Pair.Filter1(i -> i % 2 == 0)
 *     .map(Pairs.map( (l, r) -> l + r))
 *     .forEach(System.out::println);
 * }</pre>
 * Namely, we get to name the parameters, similar to destructuring in languages that support pattern matching.
 * Notice that there are two versions of most functions, lowercase methods and uppercase static methods.
 * The lowercase methods operate directly on the Pair, while the uppercase methods return a function
 * that accepts a pair and operates on that.  The latter are useful in streams to avoid creating a
 * variable named `pair` just to call a function on it.
 * <pre>{@code
 * stream.map(pair -> pair.map( (l, r) -> ...));
 * stream.map(Pairs.map( (l, r) -> ...));
 * }</pre>
 * <p>
 * We don't want to add any dependencies to this project, so we can't create a tool for converting from javatuples
 * if your code is already using them.  You can make one yourself, though, for example,
 * <pre>{@code
 * package org.javafn.tuple;
 *
 * public class Tuples {
 *     public static <A, B> Pair<A, B>  from(final org.javatuples.Pair<A, B>  jtPair) {
 *         return Pair.of(jtPair.getValue0(), jtPair.getValue1());
 *     }
 *     public static <A, B, C> Trio<A, B, C> from(final org.javatuples.Triplet<A, B, C>  jtTrio) {
 *         return Trio.of(jtTrio.getValue0(), jtTrio.getValue1(), jtTrio.getValue2());
 *     }
 *     public static <A, B, C, D> Quad<A, B, C, D>  from(final org.javatuples.Quartet<A, B, C, D>  jtQuad) {
 *         return Quad.of(jtQuad.getValue0(), jtQuad.getValue1(), jtQuad.getValue2(), jtQuad.getValue3());
 *     }
 *     public static <A, B> org.javatuples.Pair<A, B> to(final Pair<A, B> pair) {
 *         return new org.javatuples.Pair<>(pair.v1(), pair.v2());
 *     }
 *     public static <A, B, C> org.javatuples.Triplet<A, B, C>  to(final Trio<A, B, C>  trio) {
 *         return new org.javatuples.Triplet<>(trio.v1(), trio.v2(), trio.v3());
 *     }
 *     public static <A, B, C, D> org.javatuples.Quartet<A, B, C, D>  to(final Quad<A, B, C, D>  quad) {
 *         return new org.javatuples.Quartet<>(quad.v1(), quad.v2(), quad.v3(), quad.v4());
 *     }
 *     private Tuples() { throw new IllegalStateException("This is a static class and should not be instantiated."); }
 * }
 * }</pre>
 */
public record Pair<V1, V2>(V1 v1, V2 v2) {

    /**
     * Create a Pair from an {@link Map.Entry}where the entry key is
     * the first element and the value is the second element.
     */
    public static <KEY, VAL> Pair<KEY, VAL> from(final Map.Entry<KEY, VAL> e) {
        return new Pair<>(e.getKey(), e.getValue());
    }

    /**
     * Create a Pair with the supplied values.
     */
    public static <A, B> Pair<A, B> of(final A a, final B b) {
        return new Pair<>(a, b);
    }

    /**
     * Return a new pair using the supplied argument as the first element
     * and this pair's second element as the new pair's second element.
     * Note that the type signature can change.
     * <pre>{@code
     * final Pair<Integer, String> a = Pair.of(42, "forty-two");
     * final Pair<String, String> b = a.v1("42");
     * }</pre>
     */
    public <NV1> Pair<NV1, V2> v1(final NV1 nv1) {
        return Pair.of(nv1, v2);
    }

    /**
     * Return a new pair using the supplied argument as the second element
     * and this pair's first element as the new pair's first element.
     * Note that the type signature can change.
     * <pre>{@code
     * final Pair<Integer, String> a = Pair.of(42, "forty-two");
     * final Pair<Integer, Long> b = a.v2(42L);
     * }</pre>
     */
    public <NV2> Pair<V1, NV2> v2(final NV2 nv2) {
        return Pair.of(v1, nv2);
    }

    /**
     * Execute the supplied function on this Pair's values and return the result.
     * <pre>{@code
     * final Pair<Integer, String> pair = Pair.of(42, "forty-two");
     * System.out.println(pair.map( (i, s) -> Integer.toString(i) + ", " + s));
     * // Prints
     * // 42, forty-two
     * }</pre>
     */
    public <R> R map(final BiFunction<V1, V2, R> fn) {
        return fn.apply(v1, v2);
    }

    /**
     * Execute the supplied function on this Pair's first value and return a new pair where the
     * first element is the result of the mapping function and the second is this pair's second element.
     * <pre>{@code
     * final Pair<Integer, String> a = Pair.of(42, "forty-two");
     * final Pair<String, String> b = a.map1(Integer::toString);
     * }</pre>
     */
    public <NV1> Pair<NV1, V2> map1(final Function<V1, NV1> fn) {
        return Pair.of(fn.apply(v1), v2);
    }

    /**
     * Execute the supplied function on this Pair's second value and return a new pair where the
     * second element is the result of the mapping function and the first is this pair's first element.
     * <pre>{@code
     * final Pair<Integer, String> a = Pair.of(42, "forty-two");
     * final Pair<Integer, CustomValue> b = a.map1(key -> customDatabase.get(key));
     * }</pre>
     */
    public <NV2> Pair<V1, NV2> map2(final Function<V2, NV2> fn) {
        return Pair.of(v1, fn.apply(v2));
    }

    /**
     * Execute the supplied function on this Pair's values and return a new pair where the
     * first element is the result of the mapping function and the second is this pair's second element.
     * <pre>{@code
     * final Pair<Integer, String> a = Pair.of(42, "forty-two");
     * System.out.println(a.map1( (i, s) -> Integer.toString(i) + ", " + s));
     * // Prints
     * // Pair("42, forty-two", "forty-two")
     * }</pre>
     */
    public <NV1> Pair<NV1, V2> map1(final BiFunction<V1, V2, NV1> fn) {
        return Pair.of(fn.apply(v1, v2), v2);
    }

    /**
     * Execute the supplied function on this Pair's values and return a new pair where the
     * second element is the result of the mapping function and the first is this pair's first element.
     * <pre>{@code
     * final Pair<Integer, String> a = Pair.of(42, "forty-two");
     * final Pair<Integer, CustomValue> b = a.map1(
     *          (intKey, propName) -> customDatabase.get(intKey).getProperty(propName)));
     * }</pre>
     */
    public <NV2> Pair<V1, NV2> map2(final BiFunction<V1, V2, NV2> fn) {
        return Pair.of(v1, fn.apply(v1, v2));
    }

    /**
     * TODO: Document
     */
    public <NV1> Stream<Pair<NV1, V2>> flatMap1(final BiFunction<V1, V2, Stream<NV1>> fn) {
        return fn.apply(v1, v2).map(nv1 -> Pair.of(nv1, v2));
    }

    public <NV2> Stream<Pair<V1, NV2>> flatMap2(final BiFunction<V1, V2, Stream<NV2>> fn) {
        return fn.apply(v1, v2).map(nv2 -> Pair.of(v1, nv2));
    }

    public <NV1> Stream<Pair<NV1, V2>> flatMap1(final Function<V1, Stream<NV1>> fn) {
        return fn.apply(v1).map(nv1 -> Pair.of(nv1, v2));
    }

    public <NV2> Stream<Pair<V1, NV2>> flatMap2(final Function<V2, Stream<NV2>> fn) {
        return fn.apply(v2).map(nv2 -> Pair.of(v1, nv2));
    }

    /**
     * Execute the supplied consumer on this Pair's values and return this Pair unmodified.
     * Useful for inspections.
     * <pre>{@code
     * pair.peek( (l, r) -> System.out.println("Left: " + l + ", Right: " + r));
     * }</pre>
     */
    public Pair<V1, V2> peek(final BiConsumer<V1, V2> fn) {
        fn.accept(v1, v2);
        return this;
    }

    /**
     * Execute the supplied consumer on this Pair's values and return this Pair unmodified.
     * Useful for inspections.
     * <pre>{@code
     * pair.peek1(l -> System.out.println("Left: " + l));
     * }</pre>
     */
    public Pair<V1, V2> peek1(final Consumer<V1> fn) {
        fn.accept(v1);
        return this;
    }

    /**
     * Execute the supplied consumer on this Pair's values and return this Pair unmodified.
     * Useful for inspections.
     * <pre>{@code
     * pair.peek2(r -> System.out.println("Right: " + r));
     * }</pre>
     */
    public Pair<V1, V2> peek2(final Consumer<V2> fn) {
        fn.accept(v2);
        return this;
    }

    /**
     * Return the result of executing the supplied predicate on this pair's values.
     */
    public boolean matches(final BiPredicate<V1, V2> fn) {
        return fn.test(v1, v2);
    }

    /**
     * Return the result of executing the supplied predicate on this pair's first value.
     */
    public boolean matches1(final Predicate<V1> fn) {
        return fn.test(v1);
    }

    /**
     * Return the result of executing the supplied predicate on this pair's second value.
     */
    public boolean matches2(final Predicate<V2> fn) {
        return fn.test(v2);
    }

    /**
     * Execute the supplied consumer on this Pair's values.
     * Similar to {@link #peek(BiConsumer)}, except no value is returned.
     * Useful to ensure inspections will point out an unused value with peek but not for forEach.
     */
    public void consume(final BiConsumer<V1, V2> fn) {
        fn.accept(v1, v2);
    }
}
