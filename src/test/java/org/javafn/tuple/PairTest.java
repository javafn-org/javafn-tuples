package org.javafn.tuple;

import org.javafn.tuples.DoublePair;
import org.javafn.tuples.Index;
import org.javafn.tuples.IntLongPair;
import org.javafn.tuples.IntPair;
import org.javafn.tuples.LongPair;
import org.javafn.tuples.Pair;
import org.javafn.tuples.Pairs;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PairTest {

    @Test public void testConstruction() {
        final UUID uuid = UUID.randomUUID();
        final String string = UUID.randomUUID().toString();
        final Pair<UUID, String> pair = Pair.of(uuid, string);
        assertEquals("Expecting the first element to be the assigned uuid",
                uuid, pair.v1());
        assertEquals("Expecting the second element to be the assigned string",
                string, pair.v2());
    }

    @Test public void testEqualityAndHash() {
        // Construct the pair values from the string so that we get new objects, i.e., don't test reference equality
        final String s1 = UUID.randomUUID().toString();
        final String s2 = UUID.randomUUID().toString();
        final Pair<UUID, UUID> pair1 = Pair.of(UUID.fromString(s1), UUID.fromString(s2));
        final Pair<UUID, UUID> pair2 = Pair.of(UUID.fromString(s1), UUID.fromString(s2));
        assertEquals("Expected pairs from equal objects to be equal", pair1, pair2);
        assertEquals("Expected equal pairs to return the same hash", pair1.hashCode(), pair2.hashCode());
    }

    @Test public void testPartition() {
        final Random random = new Random();
        final IntStream source = IntStream.generate(() -> random.nextInt(100)).limit(100);
        final Pair<IntStream, IntStream> split = Pairs.partition(source, i -> i > 50);
        assertEquals("Expecting 0 elements less than or equal to 50 in the first partition",
                0, split.v1().filter(i -> i <= 50).count());
        assertEquals("Expecting 0 elements greater than 50 in the second partition",
                0, split.v2().filter(i -> i > 50).count());
    }

    @Test public void testZip() {
        final int numElements = 100;
        final List<IntLongPair> zipped = Pairs.zip(
                IntStream.range(0, numElements), LongStream.range(0, numElements))
                .toList();
        assertEquals("Wrong numer of elements", numElements, zipped.size());
        assertEquals("All elements should be equal",
                numElements, zipped.stream()
                        .map(p -> p.map1ToLong(i -> i))
                        .filter(Pairs.matches((long l, long r) -> l == r))
                        .count());
    }

    @Test public void testZipWithDifferentLengths() {
        final int numElements = 100;
        assertEquals("Wrong numer of elements", numElements, Pairs.zip(
                IntStream.range(0, numElements),
                LongStream.range(0, numElements * 2))
                .count());
        assertEquals("Wrong numer of elements", numElements, Pairs.zip(
                        IntStream.range(0, numElements * 2),
                        LongStream.range(0, numElements))
                .count());
    }

    @Test public void testConsumeAndPeek() {
        class InternalMutability {
            boolean mutated = false;
        }
        {
            final InternalMutability v1 = new InternalMutability();
            final InternalMutability v2 = new InternalMutability();
            final Pair<InternalMutability, InternalMutability> pair = Pair.of(v1, v2);
            pair.consume((value1, value2) -> {
                value1.mutated = true;
                value2.mutated = true;
            });
            assertTrue("Expected v1 to be passed to the forEach method", v1.mutated);
            assertTrue("Expected v2 to be passed to the forEach method", v2.mutated);
        }
        {
            final InternalMutability v1 = new InternalMutability();
            final InternalMutability v2 = new InternalMutability();
            final Pair<InternalMutability, InternalMutability> pair = Pair.of(v1, v2);
            final Pair<InternalMutability, InternalMutability> peekedAt = pair.peek((value1, value2) -> {
                value1.mutated = true;
                value2.mutated = true;
            });
            assertTrue("Expected v1 to be passed to the peek method", v1.mutated);
            assertTrue("Expected v2 to be passed to the peek method", v2.mutated);
            assertEquals("Expecting the returned pair to be the same one that was peeked at",
                    pair, peekedAt);
        }
        {
            final InternalMutability v1 = new InternalMutability();
            final InternalMutability v2 = new InternalMutability();
            final Pair<InternalMutability, InternalMutability> pair = Pair.of(v1, v2);
            final Pair<InternalMutability, InternalMutability> peekedAt = pair.peek1(v -> v.mutated = true);
            assertTrue("Expected v1 to be passed to the peek1 method", v1.mutated);
            assertFalse("Expected v2 to not be passed to the peek1 method", v2.mutated);
            assertEquals("Expecting the returned pair to be the same one that was peeked at",
                    pair, peekedAt);
        }
        {
            final InternalMutability v1 = new InternalMutability();
            final InternalMutability v2 = new InternalMutability();
            final Pair<InternalMutability, InternalMutability> pair = Pair.of(v1, v2);
            final Pair<InternalMutability, InternalMutability> peekedAt = pair.peek2(v -> v.mutated = true);
            assertFalse("Expected v1 to not be passed to the peek2 method", v1.mutated);
            assertTrue("Expected v2 to be passed to the peek2 method", v2.mutated);
            assertEquals("Expecting the returned pair to be the same one that was peeked at",
                    pair, peekedAt);
        }
    }

    @Test public void testMatches() {
        final Pair<Integer, Integer> pair = Pair.of(0, 42);
        assertTrue("Expecting pair.matches((a,b) -> a + b == 42) to return true",
                pair.matches((a, b) -> a + b == 42));
        assertFalse("Expecting pair.matches((a,b) -> a - b == 42) to return false",
                pair.matches((a, b) -> a - b == 42));
        assertTrue("Expecting pair.matches(a -> a == 0) to return true",
                pair.matches1(a -> a == 0));
        assertFalse("Expecting pair.matches(a -> a == 42) to return false",
                pair.matches1(a -> a == 42));
        assertTrue("Expecting pair.matches(a -> a == 42) to return true",
                pair.matches2(a -> a == 42));
        assertFalse("Expecting pair.matches(a -> a == 0) to return false",
                pair.matches2(a -> a == 0));
    }

    @Test public void testMap() {
        final String s = "Forty-Two";
        final Pair<Integer, String> orig = Pair.of(42, s);
        assertEquals("Expecting map1 to have affected the first element",
                Pair.of(-42, s), orig.map1( i -> i - 84));
        assertEquals("Expecting map1 to have affected the first element",
                Pair.of(51, s), orig.map1((i, v) -> i + s.length()));
        assertEquals("Expecting map2 to have affected the second element",
                Pair.of(42, s + "!"), orig.map2( v -> v + "!"));
        assertEquals("Expecting map2 to have affected the second element",
                Pair.of(42,  s + "42"), orig.map2((i, v) -> v + i));
        assertEquals("Expected map to have produced a new element",
                42 + s, orig.map((i, v) -> i + v));
    }

    @Test public void testStream() {
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();
        final List<UUID> collected = Pairs.stream(Pair.of(a, b)).toList();
        assertEquals("Expected exactly two elements", 2, collected.size());
        assertEquals("Expected the first collected element to be the first pair element",
                a, collected.get(0));
        assertEquals("Expected the second collected element to be the second pair element",
                b, collected.get(1));
    }

    @Test public void testChunkedObject() {
        final int len = 100;
        final UUID[] uuidArray = IntStream.range(0, len).mapToObj(i -> UUID.randomUUID()).toArray(UUID[]::new);
        final List<Pair<UUID, UUID>> pairs = Pairs.chunks(uuidArray).toList();
        assertEquals("Expecting the number of pairs to be exactly half the number of original items.",
                len / 2, pairs.size());
        Pairs.zip(IntStream.range(0, len/2).map(i -> i * 2), pairs.stream())
                .forEach(Pairs.peek((int i, Pair<UUID, UUID> pair) -> {
                    assertEquals(uuidArray[i], pair.v1());
                    assertEquals(uuidArray[i+1], pair.v2());
        }));
    }
    @Test public void testChunkedDouble() {
        final Random random = new Random();
        final int len = 20;
        final double[] doubleArray = DoubleStream.generate(random::nextDouble).limit(len).toArray();
        final List<DoublePair> pairs = Pairs.chunks(doubleArray).toList();
        assertEquals("Expecting the number of pairs to be exactly half the number of original items.",
                len / 2, pairs.size());
        Pairs.zip(IntStream.range(0, len/2).map(i -> i * 2), pairs.stream())
                .forEach(Pairs.consume((int i, DoublePair pair) -> {
                    assertEquals(doubleArray[i], pair.v1(), 0.0);
                    assertEquals(doubleArray[i+1], pair.v2(), 0.0);
                }));
    }
    @Test public void testChunkedLong() {
        final Random random = new Random();
        final int len = 20;
        final long[] longArray = LongStream.generate(random::nextLong).limit(len).toArray();
        final List<LongPair> pairs = Pairs.chunks(longArray).toList();
        assertEquals("Expecting the number of pairs to be exactly half the number of original items.",
                len / 2, pairs.size());
        Pairs.zip(IntStream.range(0, len/2).map(i -> i * 2), pairs.stream())
                .forEach(Pairs.consume((int i, LongPair pair) -> {
                    assertEquals(longArray[i], pair.v1());
                    assertEquals(longArray[i+1], pair.v2());
                }));
    }
    @Test public void testChunkedInt() {
        final Random random = new Random();
        final int len = 20;
        final int[] intArray = IntStream.generate(random::nextInt).limit(len).toArray();
        final List<IntPair> pairs = Pairs.chunks(intArray).toList();
        assertEquals("Expecting the number of pairs to be exactly half the number of original items.",
                len / 2, pairs.size());
        Pairs.zip(IntStream.range(0, len/2).map(i -> i * 2), pairs.stream())
                .forEach(Pairs.consume((int i, IntPair pair) -> {
                    assertEquals(intArray[i], pair.v1());
                    assertEquals(intArray[i+1], pair.v2());
                }));
    }

    @Test public void testChunkedObjectOdd() {
        final Object o = new Object();
        final Object[] a = new Object[] { o };
        final List<Pair<Object, Object>> s = Pairs.chunks(a, null).toList();
        assertEquals("Expecting exactly one pair to be generated from a single item", 1, s.size());
        final Pair<Object, Object> pair = s.get(0);
        assertEquals("Expecting the first pair element to be the object in the array.", o, pair.v1());
        assertNull("Expecting the second pair element to be null", pair.v2());
        try {
            Pairs.chunks(a);
        } catch (final IllegalArgumentException x) {
            return;
        }
        fail("Expected to catch exception chunking an odd numbered array with no pad");
    }
    @Test public void testChunkedDoubleOdd() {
        final double[] d = new double[] { 0.0, 1.1, 2.2 };
        final List<DoublePair> s = Pairs.chunks(d, -Double.MAX_VALUE).toList();
        assertEquals("Expecting exactly two pairs to be generated from three items", 2, s.size());
        final DoublePair pair = s.get(1);
        assertEquals("Expecting the first pair element to be the last item from the array.", d[2], pair.v1(), 0.0);
        assertEquals("Expecting the second pair element to be pad value", -Double.MAX_VALUE, pair.v2(), 0.0);
        try {
            Pairs.chunks(d);
        } catch (final IllegalArgumentException x) {
            return;
        }
        fail("Expected to catch exception chunking an odd numbered array with no pad");
    }
    @Test public void testChunkedLongOdd() {
        final long[] longs = new long[] { 0L, 1L, 2L, 3L, 4L };
        final List<LongPair> s = Pairs.chunks(longs, Long.MIN_VALUE).toList();
        assertEquals("Expecting exactly three pairs to be generated from five items", 3, s.size());
        final LongPair pair = s.get(2);
        assertEquals("Expecting the first pair element to be the last item from the array.", longs[4], pair.v1());
        assertEquals("Expecting the second pair element to be pad value", Long.MIN_VALUE, pair.v2(), 0.0);
        try {
            Pairs.chunks(longs);
        } catch (final IllegalArgumentException x) {
            return;
        }
        fail("Expected to catch exception chunking an odd numbered array with no pad");
    }
    @Test public void testChunkedIntOdd() {
        final int[] ints = new int[] { 0, 1, 2, 3, 4 };
        final List<IntPair> s = Pairs.chunks(ints, Integer.MIN_VALUE).toList();
        assertEquals("Expecting exactly three pairs to be generated from five items", 3, s.size());
        final IntPair pair = s.get(2);
        assertEquals("Expecting the first pair element to be the last item from the array.", ints[4], pair.v1());
        assertEquals("Expecting the second pair element to be pad value", Integer.MIN_VALUE, pair.v2(), 0.0);
        try {
            Pairs.chunks(ints);
        } catch (final IllegalArgumentException x) {
            return;
        }
        fail("Expected to catch exception chunking an odd numbered array with no pad");
    }

    @Test public void testWindowObj() {
        final int len = 20;
        final UUID[] uuidArray = IntStream.range(0, len).mapToObj(i -> UUID.randomUUID()).toArray(UUID[]::new);
        final List<Pair<UUID, UUID>> pairs = Pairs.windows(uuidArray).toList();
        assertEquals("Expecting the number of windows to be 1 less than the original array size",
                len - 1, pairs.size());
        Index.of(pairs.stream())
                .forEach(Pairs.consume((int i, Pair<UUID, UUID> pair) -> {
                    assertEquals(uuidArray[i], pair.v1());
                    assertEquals(uuidArray[i+1], pair.v2());
                }));
    }
    @Test public void testWindowDouble() {
        final Random random = new Random();
        final int len = 20;
        final double[] doubleArray = DoubleStream.generate(random::nextDouble).limit(len).toArray();
        final List<DoublePair> pairs = Pairs.windows(doubleArray).toList();
        assertEquals("Expecting the number of windows to be 1 less than the original array size",
                len - 1, pairs.size());
        Index.of(pairs.stream())
                .forEach(Pairs.consume((int i, DoublePair pair) -> {
                    assertEquals(doubleArray[i], pair.v1(), 0.0);
                    assertEquals(doubleArray[i+1], pair.v2(), 0.0);
                }));
    }
    @Test public void testWindowLong() {
        final Random random = new Random();
        final int len = 20;
        final long[] longArray = LongStream.generate(random::nextLong).limit(len).toArray();
        final List<LongPair> pairs = Pairs.windows(longArray).toList();
        assertEquals("Expecting the number of windows to be 1 less than the original array size",
                len - 1, pairs.size());
        Index.of(pairs.stream())
                .forEach(Pairs.consume((int i, LongPair pair) -> {
                    assertEquals(longArray[i], pair.v1());
                    assertEquals(longArray[i+1], pair.v2());
                }));
    }
    @Test public void testWindowInt() {
        final Random random = new Random();
        final int len = 20;
        final int[] intArray = IntStream.generate(random::nextInt).limit(len).toArray();
        final List<IntPair> pairs = Pairs.windows(intArray).toList();
        assertEquals("Expecting the number of windows to be 1 less than the original array size",
                len - 1, pairs.size());
        Index.of(pairs.stream())
                .forEach(Pairs.consume((int i, IntPair pair) -> {
                    assertEquals(intArray[i], pair.v1());
                    assertEquals(intArray[i+1], pair.v2());
                }));
    }
}
