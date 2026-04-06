package org.javafn.tuple;

import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class IndexTest {

	@Test
	public void testCorrectIndexEnumerateInline() {
		final int numItems = 100;
		final List<Index<UUID>> indexList =  Stream.generate(UUID::randomUUID).limit(numItems)
				.map(Indexed.index())
				.toList();
		for (int i = 0; i < numItems; i++) {
			assertEquals("Expected the list index and Idx.i() values to be the same",
					i, indexList.get(i).i());
		}
	}

	@Test
	public void testCorrectIndexEnumerateWrapper() {
		final int numItems = 100;
		final List<Index<UUID>> indexList =  Indexed.index(
				Stream.generate(UUID::randomUUID).limit(numItems))
				.toList();
		for (int i = 0; i < numItems; i++) {
			assertEquals("Expected the list index and Idx.i() values to be the same",
					i, indexList.get(i).i());
		}
	}

	@Test
	public void testCorrectIndexMapValOnly() {
		final int numItems = 100;
		final List<Index<String>> indexList =  Stream.generate(UUID::randomUUID).limit(numItems)
				.map(Indexed.index())
				.map(Indexed.map(UUID::toString))
				.toList();
		for (int i = 0; i < numItems; i++) {
			assertEquals("Expected the list index and Idx.i() values to be the same",
					i, indexList.get(i).i());
		}
	}
	@Test
	public void testCorrectIndexMapBoth() {
		final int numItems = 100;
		final List<Index<String>> indexList =  Stream.generate(UUID::randomUUID).limit(numItems)
				.map(Indexed.index())
				.map(Indexed.map( (i, val) -> val.toString()))
				.toList();
		for (int i = 0; i < numItems; i++) {
			assertEquals("Expected the list index and Idx.i() values to be the same",
					i, indexList.get(i).i());
		}
	}
}
