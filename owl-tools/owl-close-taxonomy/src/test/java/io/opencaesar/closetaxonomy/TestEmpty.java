package io.opencaesar.closetaxonomy;

import io.opencaesar.closetaxonomy.ClassExpression.Empty;
import io.opencaesar.closetaxonomy.ClassExpression.Singleton;
import io.opencaesar.closetaxonomy.ClassExpression.Universal;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TestEmpty {
	private Singleton sa;
	private Empty empty1;
	private Empty empty2;
	private Universal universal;

	@BeforeClass public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass public static void tearDownAfterClass() throws Exception {
	}

	@Before public void setUp() throws Exception {
		sa = new Singleton("a");
		empty1 = new Empty();
		empty2 = new Empty();
		universal = new Universal();
	}

	@After public void tearDown() throws Exception {
	}

	@Test public void testHashCode() {
		assertEquals(empty1.hashCode(), empty2.hashCode());
		assertNotEquals(empty1.hashCode(), sa.hashCode());
		assertNotEquals(empty1.hashCode(), universal.hashCode());
	}

	@Test public void testComplement() {
		// Theorem 17
		assertEquals(universal, empty1.complement());
	}

	@Test public void testDifference() {
		// Theorem 12
		assertEquals(empty1, empty1.difference(sa));
	}

	@Test public void testIntersection() {
		// Theorem 9
		assertEquals(empty1, empty1.intersection(sa));
		assertEquals(empty1, empty1.intersection(universal));
	}

	@Test public void testUnion() {
		// Theorem 10
		assertEquals(sa, empty1.union(sa));
	}

	@Test public void testToAtom() {
		assertEquals("∅", empty1.toAtom());
	}

	@Test public void testEqualsObject() {
		assertEquals(empty1, empty2);
		assertNotEquals(empty1, sa);
		assertNotEquals(empty1, universal);
	}

	@Test public void testToString() {
		assertEquals("∅", empty1.toString());
	}
}
