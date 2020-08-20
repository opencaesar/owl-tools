package io.opencaesar.closeworld;

import io.opencaesar.closeworld.ClassExpression.Singleton;
import org.junit.*;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestSingleEdgeTaxonomy {
	
	private Taxonomy tAB;
	private Taxonomy tB;
	private Singleton a;
	private Singleton b;
	private HashSet<ClassExpression> setA;
	private HashSet<ClassExpression> setB;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		tAB = new Taxonomy();
		tB = new Taxonomy();
		a = new Singleton("a");
		b = new Singleton("b");
		setA = new HashSet<ClassExpression>(Arrays.asList(a));
		setB = new HashSet<ClassExpression>(Arrays.asList(b));
		tAB.addVertex(a);
		tAB.addVertex(b);
		tAB.addEdge(a, b);
		tB.addVertex(b);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testChildrenOf() {
		assertEquals(setB, tAB.childrenOf(a));
		if (!((tAB.childrenOf(b).isEmpty()))) {
			throw new AssertionError();
		}
	}

	@Test public void testDirectChildrenOf() {
		assertEquals(setB, tAB.directChildrenOf(a));
		if (!((tAB.directChildrenOf(b).isEmpty()))) {
			throw new AssertionError();
		}
	}

	@Test public void testDescendantsOf() {
		assertEquals(setB, tAB.descendantsOf(a));
		if (!((tAB.descendantsOf(b).isEmpty()))) {
			throw new AssertionError();
		}
	}

	@Test public void testParentsOf() {
		assertEquals(setA, tAB.parentsOf(b));
		if (!((tAB.parentsOf(a).isEmpty()))) {
			throw new AssertionError();
		}
	}

	@Test public void testDirectParentsOf() {
		assertEquals(setA, tAB.directParentsOf(b));
		if (!((tAB.directParentsOf(a).isEmpty()))) {
			throw new AssertionError();
		}
	}

	@Test public void testAncestorsOf() {
		assertEquals(setA, tAB.ancestorsOf(b));
		if (!((tAB.ancestorsOf(a).isEmpty()))) {
			throw new AssertionError();
		}
	}

	@Test public void testExciseVertex() {
		assertEquals(tB, tAB.exciseVertex(a));
	}

	@Test public void testExciseVertices() {
		assertEquals(tB, tAB.exciseVertices(setA));
	}

	@Test public void testExciseVerticesIf() {
		assertEquals(tB, tAB.exciseVerticesIf(v -> v == a));
	}

	@Test public void testRootAt() {
		assertEquals(tAB, tB.rootAt(a));
	}

	@Test public void testMultiParentChild() {
		assertFalse(tAB.multiParentChild().isPresent());
	}

	@Test public void testTreeify() {
		assertEquals(tAB, tAB.treeify());
	}
}
