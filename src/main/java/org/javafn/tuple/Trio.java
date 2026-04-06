package org.javafn.tuple;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.javafn.tuple.Trios.TriPredicate;
import org.javafn.tuple.Trios.TriConsumer;
import org.javafn.tuple.Trios.TriFunction;

/**
 * See documentation for {@link Pair}.
 */
public final class Trio<V1, V2, V3> {


    public static <A, B, C> Trio<A, B, C> of(final A a, final B b, final C c) { return new Trio<A, B, C>(a, b, c); }

    private final V1 v1;
    private final V2 v2;
    private final V3 v3;

    private Trio(final V1 _v1, final V2 _v2, final V3 _v3) { v1 = _v1; v2 = _v2; v3 = _v3; }

    public V1 v1() { return v1; }
    public V2 v2() { return v2; }
    public V3 v3() { return v3; }

    public <NV1> Trio<NV1, V2, V3> v1(final NV1 nv1) { return Trio.of(nv1, v2, v3); }
    public <NV2> Trio<V1, NV2, V3> v2(final NV2 nv2) { return Trio.of(v1, nv2, v3); }
    public <NV3> Trio<V1, V2, NV3> v3(final NV3 nv3) { return Trio.of(v1, v2, nv3); }

    public <R> R map(final TriFunction<V1, V2, V3, R> fn) { return fn.apply(v1, v2, v3); }
    public <NV1> Trio<NV1, V2, V3> map1(final Function<V1,NV1> fn) { return Trio.of(fn.apply(v1), v2, v3); }
    public <NV2> Trio<V1, NV2, V3> map2(final Function<V2, NV2> fn) { return Trio.of(v1, fn.apply(v2), v3); }
    public <NV3> Trio<V1, V2, NV3> map3(final Function<V3, NV3> fn) { return Trio.of(v1, v2, fn.apply(v3)); }
    public <NV1> Trio<NV1, V2, V3> map1(final TriFunction<V1, V2, V3, NV1> fn)
    { return Trio.of(fn.apply(v1, v2, v3), v2, v3); }
    public <NV2> Trio<V1, NV2, V3> map2(final TriFunction<V1, V2, V3, NV2> fn)
    { return Trio.of(v1, fn.apply(v1, v2, v3), v3); }
    public <NV3> Trio<V1, V2, NV3> map3(final TriFunction<V1, V2, V3, NV3> fn)
    { return Trio.of(v1, v2, fn.apply(v1, v2, v3)); }

    public Trio<V1, V2, V3> peek(final TriConsumer<V1, V2, V3> fn) { fn.accept(v1, v2, v3); return this; }
    public Trio<V1, V2, V3> peek1(final Consumer<V1> fn) { fn.accept(v1); return this; }
    public Trio<V1, V2, V3> peek2(final Consumer<V2> fn) { fn.accept(v2); return this; }
    public Trio<V1, V2, V3> peek3(final Consumer<V3> fn) { fn.accept(v3); return this; }

    public boolean matches(final TriPredicate<V1, V2, V3> fn) { return fn.test(v1, v2, v3); }
    public boolean matches1(final Predicate<V1> fn) { return fn.test(v1); }
    public boolean matches2(final Predicate<V2> fn) { return fn.test(v2); }
    public boolean matches3(final Predicate<V3> fn) { return fn.test(v3); }

    public void consume(final Trios.TriConsumer<V1, V2, V3> fn) { fn.accept(v1, v2, v3); }

    @Override public int hashCode() { return Objects.hash(v1, v2, v3); }

    @Override public boolean equals(final Object obj) {
        if (obj instanceof Trio<?, ?, ?> that) {
            return Objects.equals(v1, that.v1) && Objects.equals(v2, that.v2) && Objects.equals(v3, that.v3);
        }
        return false;
    }

    @Override public String toString() { return "Trio(" + v1 + ", " + v2 + ", " + v3 + ")"; }
}
