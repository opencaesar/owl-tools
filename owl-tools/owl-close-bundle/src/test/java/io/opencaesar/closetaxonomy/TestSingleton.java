package io.opencaesar.closetaxonomy;

import io.opencaesar.closetaxonomy.ClassExpression.Complement;
import io.opencaesar.closetaxonomy.ClassExpression.Difference;
import io.opencaesar.closetaxonomy.ClassExpression.Empty;
import io.opencaesar.closetaxonomy.ClassExpression.Intersection;
import io.opencaesar.closetaxonomy.ClassExpression.Singleton;
import io.opencaesar.closetaxonomy.ClassExpression.Union;
import io.opencaesar.closetaxonomy.ClassExpression.Universal;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestSingleton {
	private Singleton sa1;
	private Singleton sa2;
	private Singleton sb;
	private Singleton sc;
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
		empty = new Empty();
		universal = new Universal();
	}

	@After public void tearDown() throws Exception {
	}

	@Test public void testHashCode() {
		assertEquals(sa1.hashCode(), sa2.hashCode());
		assertNotEquals(sa1.hashCode(), sb.hashCode());
		assertNotEquals(sa2.hashCode(), sb.hashCode());
	}

	@Test public void testToAtom() {
		assertEquals("a", sa1.toAtom());
		assertEquals("a", sa2.toAtom());
		assertEquals("b", sb.toAtom());
	}

	@Test public void testSingleton() {
		assertNotNull(sa1);
		assertNotNull(sa2);
		assertNotNull(sb);
	}

	@Test public void testEqualsObject() {
		assertEquals(sa1, sa2);
		assertNotEquals(sa1, sb);
		assertNotEquals(sa2, sb);
	}

	@Test public void testToString() {
		assertEquals("a", sa1.toString());
		assertEquals("a", sa2.toString());
		assertEquals("b", sb.toString());
	}

	@Test public void testComplement() {
		Complement ca1 = new Complement(sa1);
		Complement ca2 = new Complement(sa2);
		assertEquals(ca1, sa1.complement());
		assertEquals(ca2, sa2.complement());
		assertEquals(ca1, sa2.complement());
		assertEquals(ca2, sa1.complement());
		// Theorem 1
		assertEquals(sa1, sa1.complement().complement());
	}

	@Test public void testDifference() {
		Difference amb = new Difference(sa1, sb);
		Difference bma = new Difference(sb, sa1);
		assertEquals(amb, sa1.difference(sb));
		assertEquals(bma, sb.difference(sa1));
		// Theorem 8
		Singleton[] sl = {sb, sc};
		HashSet<ClassExpression> s = new HashSet<ClassExpression>(Arrays.asList(sl));
		Union u = new Union(s);
		assertEquals(sa1.difference(u), sa1.difference(sb).difference(sc));
		assertEquals(sa1.difference(u), sa1.difference(sc).difference(sb));
		// Theorem 11
		assertEquals(sa1, sa1.difference(empty));
		// Theorem 13
		assertEquals(empty, sa1.difference(sa1));
		// Theorem 16
		assertEquals(empty, sa1.difference(universal));
	}

	@Test public void testIntersection() {
		Singleton[] sl = {sa1, sb};
		HashSet<ClassExpression> s = new HashSet<ClassExpression>(Arrays.asList(sl));
		Intersection i = new Intersection(s);
		assertEquals(i, sa1.intersection(sb));
		assertEquals(i, sb.intersection(sa1));
		// Theorem 2
		assertEquals(sa1, sa1.intersection(sa1));
		// Theorem 3
		assertEquals(sa1.intersection(sb), sb.intersection(sa1));
		// Theorem 4
		assertEquals((sa1.intersection(sb)).intersection(sc), (sa1.intersection(sb)).intersection(sc));
	}

	@Test public void testUnion() {
		Singleton[] sl = {sa1, sb};
		HashSet<ClassExpression> s = new HashSet<ClassExpression>(Arrays.asList(sl));
		Union u = new Union(s);
		assertEquals(u, sa1.union(sb));
		assertEquals(u, sb.union(sa1));
		// Theorem 5
		assertEquals(sa1, sa1.union(sa1));
		// Theorem 6
		assertEquals(sa1.union(sb), sb.union(sa1));
		// Theorem 7
		assertEquals((sa1.union(sb)).union(sc), (sa1.union(sb)).union(sc)); // Theorem 15
	}
}
