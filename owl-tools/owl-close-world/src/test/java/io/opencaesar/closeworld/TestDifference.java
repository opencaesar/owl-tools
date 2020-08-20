package io.opencaesar.closeworld;

import io.opencaesar.closeworld.ClassExpression.*;
import org.junit.*;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;

public class TestDifference {
	private Singleton sa1;
	private Singleton sa2;
	private Singleton sb;
	private Singleton sc;
	private Difference a1ma2;
	private Difference a2ma1;
	private Difference a1mb;
	private Difference a2mb;
	private Difference bma1;
	private Difference bmb;
	private Difference bmc;
	private Empty empty;
	private Universal universal;

	@BeforeClass public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass public static void tearDownAfterClass() throws Exception {
	}

	@Before public void setUp() throws Exception {
		sa1 = new Singleton("a");
		sa2 = new Singleton("a");
		sb = new Singleton("b");
		sc = new Singleton("c");
		a1ma2 = new Difference(sa1, sa2);
		a2ma1 = new Difference(sa2, sa1);
		a1mb = new Difference(sa1, sb);
		a2mb = new Difference(sa2, sb);
		bma1 = new Difference(sb, sa1);
		bmb = new Difference(sb, sb);
		bmc = new Difference(sb, sc);
		empty = new Empty();
		universal = new Universal();
	}

	@After public void tearDown() throws Exception {
	}

	@Test public void testHashCode() {
		assertEquals(a1ma2.hashCode(), a2ma1.hashCode());
		assertNotEquals(a1ma2.hashCode(), a1mb.hashCode());
		assertNotEquals(a1ma2.hashCode(), a2mb.hashCode());
		assertNotEquals(a1ma2.hashCode(), bma1.hashCode());
		assertNotEquals(a1ma2.hashCode(), bmb.hashCode());
		assertEquals(a2ma1.hashCode(), a1ma2.hashCode());
		assertNotEquals(a2ma1.hashCode(), a1mb.hashCode());
		assertNotEquals(a2ma1.hashCode(), a2mb.hashCode());
		assertNotEquals(a2ma1.hashCode(), bma1.hashCode());
		assertNotEquals(a2ma1.hashCode(), bmb.hashCode());
		assertNotEquals(a1mb.hashCode(), a1ma2.hashCode());
		assertNotEquals(a1mb.hashCode(), a2ma1.hashCode());
		assertEquals(a1mb.hashCode(), a2mb.hashCode());
		assertNotEquals(a1mb.hashCode(), bma1.hashCode());
		assertNotEquals(a1mb.hashCode(), bmb.hashCode());
		assertNotEquals(a2mb.hashCode(), a1ma2.hashCode());
		assertNotEquals(a2mb.hashCode(), a2ma1.hashCode());
		assertEquals(a2mb.hashCode(), a1mb.hashCode());
		assertNotEquals(a2mb.hashCode(), bma1.hashCode());
		assertNotEquals(a2mb.hashCode(), bmb.hashCode());
		assertNotEquals(bma1.hashCode(), a1ma2.hashCode());
		assertNotEquals(bma1.hashCode(), a2ma1.hashCode());
		assertNotEquals(bma1.hashCode(), a1mb.hashCode());
		assertNotEquals(bma1.hashCode(), a2mb.hashCode());
		assertNotEquals(bma1.hashCode(), bmb.hashCode());
		assertNotEquals(bmb.hashCode(), a1ma2.hashCode());
		assertNotEquals(bmb.hashCode(), a2ma1.hashCode());
		assertNotEquals(bmb.hashCode(), a1mb.hashCode());
		assertNotEquals(bmb.hashCode(), a2mb.hashCode());
		assertNotEquals(bmb.hashCode(), bma1.hashCode());
		assertNotEquals(bmb.hashCode(), "dummy".hashCode());
	}

	@Test public void testDifference() {
		assertNotNull(a1ma2);
		assertNotNull(a2ma1);
		assertNotNull(a1mb);
		assertNotNull(a2mb);
		assertNotNull(bma1);
		assertNotNull(bmb);
	}

	@Test public void testDifference1() {
		assertEquals(a1mb, sa1.difference(sb));
		assertEquals(a2mb, sa2.difference(sb));
		assertEquals(bma1, sb.difference(sa1));
		// Theorem 8
		final Difference[] dl = {bma1, bmc};
		final HashSet<ClassExpression> us = new HashSet<ClassExpression>(Arrays.asList(dl));
		final Difference d = new Difference(sa1, new Union(us));
		assertEquals(d, sa1.difference(bma1).difference(bmc));
		assertEquals(d, sa1.difference(bmc).difference(bma1));
		// Theorem 11
		assertEquals(a1mb, a1mb.difference(empty));
		// Theorem 13
		assertEquals(empty, a1mb.difference(a1mb));
		// Theorem 16
		assertEquals(empty, a1mb.difference(universal));
	}

	@Test public void testEqualsObject() {
		assertEquals(a1ma2, a2ma1);
		assertNotEquals(a1ma2, a1mb);
		assertNotEquals(a1ma2, a2mb);
		assertNotEquals(a1ma2, bma1);
		assertNotEquals(a1ma2, bmb);
		assertEquals(a2ma1, a1ma2);
		assertNotEquals(a2ma1, a1mb);
		assertNotEquals(a2ma1, a2mb);
		assertNotEquals(a2ma1, bma1);
		assertNotEquals(a2ma1, bmb);
		assertNotEquals(a1mb, a1ma2);
		assertNotEquals(a1mb, a2ma1);
		assertEquals(a1mb, a2mb);
		assertNotEquals(a1mb, bma1);
		assertNotEquals(a1mb, bmb);
		assertNotEquals(a2mb, a1ma2);
		assertNotEquals(a2mb, a2ma1);
		assertEquals(a2mb, a1mb);
		assertNotEquals(a2mb, bma1);
		assertNotEquals(a2mb, bmb);
		assertNotEquals(bma1, a1ma2);
		assertNotEquals(bma1, a2ma1);
		assertNotEquals(bma1, a1mb);
		assertNotEquals(bma1, a2mb);
		assertNotEquals(bma1, bmb);
		assertNotEquals(bmb, a1ma2);
		assertNotEquals(bmb, a2ma1);
		assertNotEquals(bmb, a1mb);
		assertNotEquals(bmb, a2mb);
		assertNotEquals(bmb, bma1);
		assertNotEquals(bmb, 1);
	}

	@Test public void testToString() {
		assertEquals("a\\a", a1ma2.toString());
		assertEquals("a\\a", a2ma1.toString());
		assertEquals("a\\b", a1mb.toString());
		assertEquals("a\\b", a2mb.toString());
		assertEquals("b\\a", bma1.toString());
		assertEquals("b\\b", bmb.toString());
	}

	@Test public void testComplement() {
		final Complement a1ma2c = new Complement(a1ma2);
		final Complement a1mbc = new Complement(a1mb);
		assertEquals(a1ma2c, a1ma2.complement());
		assertEquals(a1ma2c, a2ma1.complement());
		assertEquals(a1mbc, a1mb.complement());
		assertEquals(a1mbc, a2mb.complement());
		// Theorem 1
		assertEquals(a1mb, a1mb.complement().complement());
	}

	@Test public void testIntersection() {
		final Difference[] sl = {a1ma2, a1mb};
		final HashSet<ClassExpression> s = new HashSet<ClassExpression>(Arrays.asList(sl));
		final Intersection i = new Intersection(s);
		assertEquals(i, a1ma2.intersection(a1mb));
		assertEquals(i, a1mb.intersection(a1ma2));
		// Theorem 2
		assertEquals(a1mb, a1mb.intersection(a1mb));
		// Theorem 3
		assertEquals(a1mb.intersection(bmc), bmc.intersection(a1mb));
		// Theorem 4
		assertEquals((a1mb.intersection(bmb)).intersection(bmc), (a1mb.intersection(bmb)).intersection(bmc));
	}

	@Test public void testUnion() {
		final Difference[] sl = {a1ma2, a1mb};
		final HashSet<ClassExpression> s = new HashSet<ClassExpression>(Arrays.asList(sl));
		final Union u = new Union(s);
		assertEquals(u, a1ma2.union(a1mb));
		assertEquals(u, a1mb.union(a1ma2));
		// Theorem 5
		assertEquals(a1mb, a1mb.union(a1mb));
		// Theorem 6
		assertEquals(a1mb.union(bmc), bmc.union(a1mb));
		// Theorem 7
		assertEquals((a1mb.union(bmb)).union(bmc), (a1mb.union(bmb)).union(bmc));
	}

	@Test 
	public void testToAtom() {
		assertEquals("(a\\a)", a1ma2.toAtom());
		assertEquals("(a\\a)", a2ma1.toAtom());
		assertEquals("(a\\b)", a1mb.toAtom());
		assertEquals("(a\\b)", a2mb.toAtom());
		assertEquals("(b\\a)", bma1.toAtom());
		assertEquals("(b\\b)", bmb.toAtom());
	}
}
