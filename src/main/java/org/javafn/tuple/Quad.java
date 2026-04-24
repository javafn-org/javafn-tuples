package org.javafn.tuple;

import org.javafn.tuple.Quads.QuadConsumer;
import org.javafn.tuple.Quads.QuadFunction;
import org.javafn.tuple.Quads.QuadPredicate;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * See documentation for {@link Pair}.
 */
public record Quad<V1, V2, V3, V4>(V1 v1, V2 v2, V3 v3, V4 v4) {


    public static <A, B, C, D> Quad<A, B, C, D> of(final A a, final B b, final C c, final D d) {
        return new Quad<>(a, b, c, d);
    }

    public <NV1> Quad<NV1, V2, V3, V4> v1(final NV1 nv1) {
        return Quad.of(nv1, v2, v3, v4);
    }

    public <NV2> Quad<V1, NV2, V3, V4> v2(final NV2 nv2) {
        return Quad.of(v1, nv2, v3, v4);
    }

    public <NV3> Quad<V1, V2, NV3, V4> v3(final NV3 nv3) {
        return Quad.of(v1, v2, nv3, v4);
    }

    public <NV4> Quad<V1, V2, V3, NV4> v4(final NV4 nv4) {
        return Quad.of(v1, v2, v3, nv4);
    }

    public <R> R map(final QuadFunction<V1, V2, V3, V4, R> fn) {
        return fn.apply(v1, v2, v3, v4);
    }

    public <NV1> Quad<NV1, V2, V3, V4> map1(final Function<V1, NV1> fn) {
        return Quad.of(fn.apply(v1), v2, v3, v4);
    }

    public <NV2> Quad<V1, NV2, V3, V4> map2(final Function<V2, NV2> fn) {
        return Quad.of(v1, fn.apply(v2), v3, v4);
    }

    public <NV3> Quad<V1, V2, NV3, V4> map3(final Function<V3, NV3> fn) {
        return Quad.of(v1, v2, fn.apply(v3), v4);
    }

    public <NV4> Quad<V1, V2, V3, NV4> map4(final Function<V4, NV4> fn) {
        return Quad.of(v1, v2, v3, fn.apply(v4));
    }

    public <NV1> Quad<NV1, V2, V3, V4> map1(final QuadFunction<V1, V2, V3, V4, NV1> fn) {
        return Quad.of(fn.apply(v1, v2, v3, v4), v2, v3, v4);
    }

    public <NV2> Quad<V1, NV2, V3, V4> map2(final QuadFunction<V1, V2, V3, V4, NV2> fn) {
        return Quad.of(v1, fn.apply(v1, v2, v3, v4), v3, v4);
    }

    public <NV3> Quad<V1, V2, NV3, V4> map3(final QuadFunction<V1, V2, V3, V4, NV3> fn) {
        return Quad.of(v1, v2, fn.apply(v1, v2, v3, v4), v4);
    }

    public <NV4> Quad<V1, V2, V3, NV4> map4(final QuadFunction<V1, V2, V3, V4, NV4> fn) {
        return Quad.of(v1, v2, v3, fn.apply(v1, v2, v3, v4));
    }

    public Quad<V1, V2, V3, V4> peek(final QuadConsumer<V1, V2, V3, V4> fn) {
        fn.accept(v1, v2, v3, v4);
        return this;
    }

    public Quad<V1, V2, V3, V4> peek1(final Consumer<V1> fn) {
        fn.accept(v1);
        return this;
    }

    public Quad<V1, V2, V3, V4> peek2(final Consumer<V2> fn) {
        fn.accept(v2);
        return this;
    }

    public Quad<V1, V2, V3, V4> peek3(final Consumer<V3> fn) {
        fn.accept(v3);
        return this;
    }

    public Quad<V1, V2, V3, V4> peek4(final Consumer<V4> fn) {
        fn.accept(v4);
        return this;
    }

    public boolean matches(final QuadPredicate<V1, V2, V3, V4> fn) {
        return fn.test(v1, v2, v3, v4);
    }

    public boolean matches1(final Predicate<V1> fn) {
        return fn.test(v1);
    }

    public boolean matches2(final Predicate<V2> fn) {
        return fn.test(v2);
    }

    public boolean matches3(final Predicate<V3> fn) {
        return fn.test(v3);
    }

    public boolean matches4(final Predicate<V4> fn) {
        return fn.test(v4);
    }

    public void consume(final QuadConsumer<V1, V2, V3, V4> fn) {
        fn.accept(v1, v2, v3, v4);
    }
}
