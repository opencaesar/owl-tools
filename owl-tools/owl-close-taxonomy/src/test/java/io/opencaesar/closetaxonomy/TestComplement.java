package io.opencaesar.closetaxonomy;

import io.opencaesar.closetaxonomy.ClassExpression.*;
import org.junit.*;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;

public class TestComplement {
	private Singleton sa1;
	private Singleton sa2;
	private Singleton sb;
	private Singleton sc;
	private Complement ca1;
	private Complement ca2;
	private Complement cb;
	private Complement cc;
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
		ca1 = new Complement(sa1);
		ca2 = new Complement(sa2);
		cb = new Complement(sb);
		cc = new Complement(sc);
		empty = new Empty();
		universal = new Universal();
	}

	@After public void tearDown() throws Exception {
	}

	@Test public void testHashCode() {
		assertEquals(ca1.hashCode(), ca2.hashCode());
		assertNotEquals(ca1.hashCode(), cb.hashCode());
		assertNotEquals(ca2.hashCode(), cb.hashCode());
	}

	@Test public void testComplement() {
		assertNotNull(ca1);
		assertNotNull(ca2);
		assertNotNull(cb);
	}

	@Test public void testToAtom() {
		final String caa = "a\u2032";
		final String cba = "b\u2032";
		assertEquals(caa, ca1.toAtom());
		assertEquals(caa, ca2.toAtom());
		assertEquals(cba, cb.toAtom());
	}

	@Test public void testComplement1() {
		// Theorem 1
		assertEquals(sa1, ca1.complement());
		assertEquals(sa2, ca2.complement());
		assertEquals(sb, cb.complement());
		assertEquals(sa1, sa1.complement().complement());
	}

	@Test public void testEqualsObject() {
		assertEquals(ca1, ca2);
		assertNotEquals(ca1, cb);
		assertNotEquals(ca2, cb);
	}

	@Test public void testToString() {
		final String caa = "a\u2032";
		final String cba = "b\u2032";
		assertEquals(caa, ca1.toString());
		assertEquals(caa, ca2.toString());
		assertEquals(cba, cb.toString());
	}

	@Test public void testDifference() {
		final Difference amb = new Difference(ca1, cb);
		final Difference bma = new Difference(cb, ca1);
		assertEquals(amb, ca1.difference(cb));
		assertEquals(bma, cb.difference(ca1));
		// Theorem 8
		final Complement[] sl = {cb, cc};
		final HashSet<ClassExpression> s = new HashSet<ClassExpression>(Arrays.asList(sl));
		final Union u = new Union(s);
		assertEquals(ca1.difference(u), ca1.difference(cb).difference(cc));
		assertEquals(ca1.difference(u), ca1.difference(cc).difference(cb));
		// Theorem 11
		assertEquals(ca1, ca1.difference(empty));
		// Theorem 13
		assertEquals(empty, ca1.difference(ca1));
		// Theorem 16
		assertEquals(empty, ca1.difference(universal));
	}

	@Test public void testIntersection() {
		final Complement[] sl = {ca1, cb};
		final HashSet<ClassExpression> s = new HashSet<ClassExpression>(Arrays.asList(sl));
		final Intersection i = new Intersection(s);
		assertEquals(i, ca1.intersection(cb));
		assertEquals(i, cb.intersection(ca1));
		// Theorem 2
		assertEquals(ca1, ca1.intersection(ca1));
		// Theorem 3
		assertEquals(ca1.intersection(cb), cb.intersection(ca1));
		// Theorem 4
		assertEquals((ca1.intersection(cb)).intersection(cc), (ca1.intersection(cb)).intersection(cc));
	}

	@Test public void testUnion() {
		final Complement[] sl = {ca1, cb};
		final HashSet<ClassExpression> s = new HashSet<ClassExpression>(Arrays.asList(sl));
		final Union u = new Union(s);
		assertEquals(u, ca1.union(cb));
		assertEquals(u, cb.union(ca1));
		// Theorem 5
		assertEquals(ca1, ca1.union(ca1));
		// Theorem 6
		assertEquals(ca1.union(cb), cb.union(ca1));
		// Theorem 7
		assertEquals((ca1.union(cb)).union(cc), (ca1.union(cb)).union(cc)); // Theorem 15
	}
}
