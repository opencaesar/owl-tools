package io.opencaesar.closetaxonomy;

import io.opencaesar.closetaxonomy.ClassExpression.*;
import org.junit.*;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;

@SuppressWarnings("all")
public class TestUnion {
	
	private ClassExpression.Singleton sa1;
	private ClassExpression.Singleton sa2;
	private ClassExpression.Singleton sb;
	private ClassExpression.Singleton sc;
	private ClassExpression.Union a1;
	private ClassExpression.Union a1ua2;
	private ClassExpression.Union a1ua2ub;
	private ClassExpression.Union a1ua2ubuc;
	private ClassExpression.Union a1ub;
	private ClassExpression.Union a1ubuc;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		sa1 = new Singleton("a");
		sa2 = new Singleton("a");
		sb = new Singleton("b");
		sc = new Singleton("c");
		Singleton[] sl1 = { sa1, sa2, sb, sc };
		HashSet<ClassExpression> a1l = new HashSet<ClassExpression>(Arrays.asList(sl1).subList(0, 1));
		a1 = new Union(a1l);
		HashSet<ClassExpression> a1ua2l = new HashSet<ClassExpression>(Arrays.asList(sl1).subList(0, 2));
		a1ua2 = new Union(a1ua2l);
		HashSet<ClassExpression> a1ua2ubl = new HashSet<ClassExpression>(Arrays.asList(sl1).subList(0, 3));
		a1ua2ub = new Union(a1ua2ubl);
		HashSet<ClassExpression> a1ua2ubucl = new HashSet<ClassExpression>(Arrays.asList(sl1).subList(0, 4));
		a1ua2ubuc = new Union(a1ua2ubucl);
		Singleton[] sl2 = { sa1, sb, sc };
		HashSet<ClassExpression> a1ibl = new HashSet<ClassExpression>(Arrays.asList(sl2).subList(0, 2));
		a1ub = new Union(a1ibl);
		HashSet<ClassExpression> a1ibicl = new HashSet<ClassExpression>(Arrays.asList(sl2).subList(0, 3));
		a1ubuc = new Union(a1ibicl);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testHashCode() {
		assertEquals(a1.hashCode(), a1ua2.hashCode());
		assertNotEquals(a1.hashCode(), a1ua2ub.hashCode());
		assertNotEquals(a1.hashCode(), a1ua2ubuc.hashCode());
		assertNotEquals(a1.hashCode(), a1ub.hashCode());
		assertNotEquals(a1.hashCode(), a1ubuc.hashCode());
		assertNotEquals(a1ua2.hashCode(), a1ua2ub.hashCode());
		assertNotEquals(a1ua2.hashCode(), a1ua2ubuc.hashCode());
		assertNotEquals(a1ua2.hashCode(), a1ub.hashCode());
		assertNotEquals(a1ua2.hashCode(), a1ubuc.hashCode());
		assertNotEquals(a1ua2ub.hashCode(), a1ua2ubuc.hashCode());
		assertEquals(a1ua2ub.hashCode(), a1ub.hashCode());
		assertNotEquals(a1ua2ub.hashCode(), a1ubuc.hashCode());
		assertNotEquals(a1ua2ubuc.hashCode(), a1ub.hashCode());
		assertEquals(a1ua2ubuc.hashCode(), a1ubuc.hashCode());
		assertNotEquals(a1ub.hashCode(), a1ubuc.hashCode());
	}

	@Test
	public void testUnion() {
		assertNotNull(a1);
		assertNotNull(a1ua2);
		assertNotNull(a1ua2ub);
		assertNotNull(a1ua2ubuc);
	}

	@Test
	public void testUnion1() {
		assertEquals(a1ua2, a1.union(sa2));
		assertEquals(a1ua2ub, a1.union(sa2).union(sb));
		assertEquals(a1ua2ub, a1.union(sb).union(sa2));
		assertEquals(a1ua2ub, a1ua2.union(sb));
		assertEquals(a1ua2ubuc, a1.union(sa2).union(sb).union(sc));
		assertEquals(a1ua2ubuc, a1ua2.union(sb).union(sc));
		assertEquals(a1ua2ubuc, a1ua2.union(sc).union(sb));
		assertEquals(a1ua2ubuc, a1ua2ub.union(sc));
	}

	@Test
	public void testEqualsObject() {
		assertEquals(a1, a1ua2);
		assertNotEquals(a1, a1ua2ub);
		assertNotEquals(a1, a1ua2ubuc);
		assertNotEquals(a1, a1ub);
		assertNotEquals(a1, a1ubuc);
		assertNotEquals(a1ua2, a1ua2ub);
		assertNotEquals(a1ua2, a1ua2ubuc);
		assertNotEquals(a1ua2, a1ub);
		assertNotEquals(a1ua2, a1ubuc);
		assertNotEquals(a1ua2ub, a1ua2ubuc);
		assertEquals(a1ua2ub, a1ub);
		assertNotEquals(a1ua2ub, a1ubuc);
		assertNotEquals(a1ua2ubuc, a1ub);
		assertEquals(a1ua2ubuc, a1ubuc);
		assertNotEquals(a1ub, a1ubuc);
	}

	@Test
	public void testToString() {
		assertEquals("a", a1.toString());
		assertEquals("a", a1ua2.toString());
		assertEquals("a∪b", a1ua2ub.toString());
		assertEquals("a∪b", a1ub.toString());
		assertEquals("a∪b∪c", a1ua2ubuc.toString());
		assertEquals("a∪b∪c", a1ubuc.toString());
	}

	@Test
	public void testComplement() {
		Complement a1c = new Complement(a1);
		Complement a1ia2c = new Complement(a1ua2);
		Complement a1ia2ibc = new Complement(a1ua2ub);
		Complement a1ia2ibicc = new Complement(a1ua2ubuc);
		assertEquals(a1c, a1.complement());
		assertEquals(a1ia2c, a1ua2.complement());
		assertEquals(a1ia2ibc, a1ua2ub.complement());
		assertEquals(a1ia2ibicc, a1ua2ubuc.complement());
	}

	@Test
	public void testDifference() {
		Empty empty = new Empty();
		Difference diff = new Difference(a1ua2ub, a1ua2ubuc);
		assertEquals(empty, a1.difference(a1ua2));
		assertEquals(diff, a1ua2ub.difference(a1ua2ubuc));
	}

	@Test
	public void testIntersection() {
		Union[] sl = { a1, a1ua2, a1ua2ub };
		HashSet<ClassExpression> s = new HashSet<ClassExpression>(Arrays.asList(sl));
		Intersection u = new Intersection(s);
		assertEquals(u, a1.intersection(a1ua2).intersection(a1ua2ub));
		assertEquals(u, a1ua2.intersection(a1ua2ub).intersection(a1));
	}

	@Test
	public void testToAtom() {
		assertEquals("a", a1.toAtom());
		assertEquals("a", a1ua2.toAtom());
		assertEquals("(a∪b)", a1ua2ub.toAtom());
		assertEquals("(a∪b)", a1ub.toAtom());
		assertEquals("(a∪b∪c)", a1ua2ubuc.toAtom());
		assertEquals("(a∪b∪c)", a1ubuc.toAtom());
	}
}
