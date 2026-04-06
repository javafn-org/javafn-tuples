package org.javafn.tuple;

import org.javafn.tuple.Indexed.IndexedConsumer;
import org.javafn.tuple.Indexed.IndexedFunction;
import org.javafn.tuple.Indexed.IndexedPredicate;

import java.util.Objects;
import java.util.function.Function;

/**
 * Add an index to each element in a stream.  Similar to
 * <a href="https://doc.rust-lang.org/std/iter/trait.Iterator.html#method.enumerate">Rust's enumerate</a>
 * or <a href="https://docs.python.org/3/library/functions.html#enumerate">Python's enumerate</a>.
 * This class can be compared to the JavaFn {@link Pair} except it is specialized to not box the integer index.
 * <pre>{@code
 * someStream.map(Indexed.index())
 *      .peek(Indexed.peek( (i, item) -> println("Completing item " + i ))
 *      ...;
 * }</pre>
 * or equivalently,
 * <pre>{@code
 * Indexed.index(someStream)
 *      .peek(Index.Peek( (i, item) -> println("Completing item " + i ))
 *      ...;
 * }</pre>
 *
 * @param <T>
 */
public record Index<T>(int i, T val) {

	/** Map the value in this Index to a new type and return a new Index with the same index and the new value. */
	public <R> Index<R> map(final IndexedFunction<T, R> fn) {return new Index<>(i, fn.apply(i, val));}

	/** Map the value in this Index to a new type and return a new Index with the same index and the new value. */
	public <R> Index<R> map(final Function<T, R> fn) {return new Index<>(i, fn.apply(val));}

	/** Map the value in this Index to a new type and return the new value, dropping the index. */
	public <R> R mapDropIndex(final IndexedFunction<T, R> fn) {return fn.apply(i, val);}

	/** Inspect this Index but do not update it. */
	public Index<T> peek(final IndexedConsumer<T> fn) {
		fn.accept(i, val);
		return this;
	}

	/** Perform the supplied Predicate on this Index. */
	public boolean matches(final IndexedPredicate<T> fn) {return fn.test(i, val);}

	/** Inspect this Index but do not update it, and do not return it for further processing. */
	public void consume(final IndexedConsumer<T> fn) {fn.accept(i, val);}

	/** Return the wrapped value and drop the index. */
	public T dropIndex() {return val;}

	@Override public boolean equals(final Object obj) {
		if (obj instanceof Index<?> that) {
			return i == that.i && Objects.equals(val, that.val);
		}
		return false;
	}

	@Override public String toString() {return "([" + i + "]:" + val.toString() + ")";}
}
