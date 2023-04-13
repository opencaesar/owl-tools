package io.opencaesar.closeworld;

import io.opencaesar.closeworld.ClassExpression.Unitary;
import org.junit.*;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;

@SuppressWarnings("all")
public class TestDoubleEdgeChainTaxonomy {
	private Taxonomy taxonomyABC;
	private Taxonomy taxonomyBC;
	private Taxonomy taxonomyAC;
	private Taxonomy taxonomyAB;
	private Taxonomy taxonomyA;
	private Taxonomy taxonomyC;
	private Unitary a;
	private Unitary b;
	private Unitary c;
	private HashSet<ClassExpression> setA;
	private HashSet<ClassExpression> setAB;
	private HashSet<ClassExpression> setB;
	private HashSet<ClassExpression> setBC;
	private HashSet<ClassExpression> setC;

	@BeforeClass public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass public static void tearDownAfterClass() throws Exception {
	}

	@Before public void setUp() throws Exception {
		taxonomyABC = new Taxonomy();
		taxonomyBC = new Taxonomy();
		taxonomyAC = new Taxonomy();
		taxonomyAB = new Taxonomy();
		taxonomyA = new Taxonomy();
		taxonomyC = new Taxonomy();
		a = new Unitary("a");
		b = new Unitary("b");
		c = new Unitary("c");
		setA = new HashSet<ClassExpression>(Arrays.asList(a));
		setAB = new HashSet<ClassExpression>(Arrays.asList(a, b));
		setB = new HashSet<ClassExpression>(Arrays.asList(b));
		setBC = new HashSet<ClassExpression>(Arrays.asList(b, c));
		setC = new HashSet<ClassExpression>(Arrays.asList(c));
		taxonomyABC.addVertex(a);
		taxonomyABC.addVertex(b);
		taxonomyABC.addVertex(c);
		taxonomyABC.addEdge(a, b);
		taxonomyABC.addEdge(b, c);
		taxonomyBC.addVertex(b);
		taxonomyBC.addVertex(c);
		taxonomyBC.addEdge(b, c);
		taxonomyAC.addVertex(a);
		taxonomyAC.addVertex(c);
		taxonomyAC.addEdge(a, c);
		taxonomyAB.addVertex(a);
		taxonomyAB.addVertex(b);
		taxonomyAB.addEdge(a, b);
		taxonomyA.addVertex(a);
		taxonomyC.addVertex(c);
	}

	@After public void tearDown() throws Exception {
	}

	@Test public void testChildrenOf() {
		assertEquals(setB, taxonomyABC.childrenOf(a));
		assertEquals(setC, taxonomyABC.childrenOf(b));
		assertTrue(taxonomyABC.childrenOf(c).isEmpty());
	}

	@Test public void testDirectChildrenOf() {
		assertEquals(setB, taxonomyABC.directChildrenOf(a));
		assertEquals(setC, taxonomyABC.directChildrenOf(b));
		assertTrue(taxonomyABC.directChildrenOf(c).isEmpty());
	}

	@Test public void testDescendantsOf() {
		assertEquals(setBC, taxonomyABC.descendantsOf(a));
		assertEquals(setC, taxonomyABC.descendantsOf(b));
		assertTrue(taxonomyABC.descendantsOf(c).isEmpty());
	}

	@Test public void testParentsOf() {
		if (!((taxonomyABC.parentsOf(a).isEmpty()))) {
			throw new AssertionError();
		}
		assertEquals(setA, taxonomyABC.parentsOf(b));
		assertEquals(setB, taxonomyABC.parentsOf(c));
	}

	@Test public void testDirectParentsOf() {
		if (!((taxonomyABC.directParentsOf(a).isEmpty()))) {
			throw new AssertionError();
		}
		assertEquals(setA, taxonomyABC.directParentsOf(b));
		assertEquals(setB, taxonomyABC.directParentsOf(c));
	}

	@Test public void testAncestorsOf() {
		if (!((taxonomyABC.ancestorsOf(a).isEmpty()))) {
			throw new AssertionError();
		}
		assertEquals(setA, taxonomyABC.ancestorsOf(b));
		assertEquals(setAB, taxonomyABC.ancestorsOf(c));
	}

	@Test public void testMultiParentChild() {
		assertFalse(taxonomyABC.multiParentChild().isPresent());
	}

	@Test public void testExciseVertex() {
		assertEquals(taxonomyBC, taxonomyABC.exciseVertex(a));
		assertEquals(taxonomyAC, taxonomyABC.exciseVertex(b));
		assertEquals(taxonomyAB, taxonomyABC.exciseVertex(c));
	}

	@Test public void testExciseVertices() {
		assertEquals(taxonomyA, taxonomyABC.exciseVertices(setBC));
	}

	@Test public void testExciseVerticesIf() {
		assertEquals(taxonomyA, taxonomyABC.exciseVerticesIf(v -> setBC.contains(v)));
	}

	@Test public void testRootAt() {
		assertEquals(taxonomyABC, taxonomyBC.rootAt(a));
		assertEquals(taxonomyABC, taxonomyC.rootAt(b).rootAt(a));
	}

	@Test public void testTransitiveReduction() {
		final Taxonomy t1 = (Taxonomy) taxonomyABC.clone();
		assertEquals(taxonomyABC, taxonomyABC.transitiveReduction());
		t1.addEdge(a, c);
		assertEquals(taxonomyABC, t1.transitiveReduction());
	}

	@Test public void testTreeify() {
		assertEquals(taxonomyABC, taxonomyABC.treeify());
	}
}
