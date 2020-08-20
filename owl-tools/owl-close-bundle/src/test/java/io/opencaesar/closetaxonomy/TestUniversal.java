package io.opencaesar.closetaxonomy;

import io.opencaesar.closetaxonomy.ClassExpression.Empty;
import io.opencaesar.closetaxonomy.ClassExpression.Singleton;
import io.opencaesar.closetaxonomy.ClassExpression.Universal;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestUniversal {
	private Singleton sa;
	private Empty empty;
	private Universal universal1;
	private Universal universal2;

	@BeforeClass public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass public static void tearDownAfterClass() throws Exception {
	}

	@Before public void setUp() throws Exception {
		sa = new Singleton("a");
		empty = new Empty();
		universal1 = new Universal();
		universal2 = new Universal();
	}

	@After public void tearDown() throws Exception {
	}

	@Test public void testHashCode() {
		assertEquals(universal1.hashCode(), universal2.hashCode());
		assertNotEquals(universal1.hashCode(), empty.hashCode());
	}

	@Test public void testComplement() {
		// Theorem 1
		// Theorem 17
		assertEquals(empty, universal1.complement());
	}

	@Test public void testIntersection() {
		// Theorem 14
		assertEquals(sa, universal1.intersection(sa));
		assertEquals(empty, universal1.intersection(empty));
	}

	@Test public void testUnion() {
		// Theorem 15
		assertEquals(universal1, universal1.union(sa));
		assertEquals(universal1, universal1.union(empty));
	}

	@Test public void testToAtom() {
		assertEquals("U", universal1.toAtom());
	}

	@Test public void testEqualsObject() {
		assertEquals(universal1, universal2);
		assertNotEquals(universal1, empty);
	}

	@Test public void testToString() {
		assertEquals("U", universal1.toString());
	}
}
