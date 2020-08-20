package io.opencaesar.closetaxonomy;

import io.opencaesar.closetaxonomy.ClassExpression.Singleton;
import org.junit.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestSingleVertexTaxonomy {
	private Taxonomy t;
	private Taxonomy e;
	private Singleton a;

	@BeforeClass public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass public static void tearDownAfterClass() throws Exception {
	}

	@Before public void setUp() throws Exception {
		t = new Taxonomy();
		e = new Taxonomy();
		a = new Singleton("a");
		t.addVertex(a);
	}

	@After public void tearDown() throws Exception {
	}

	@Test public void testChildrenOf() {
		if (!((t.childrenOf(a).isEmpty()))) {
			throw new AssertionError();
		}
	}

	public void testDirectChildrenOf() {
		if (!((t.directChildrenOf(a).isEmpty()))) {
			throw new AssertionError();
		}
	}

	@Test public void testDescendantsOf() {
		if (!((t.descendantsOf(a).isEmpty()))) {
			throw new AssertionError();
		}
	}

	@Test public void testParentsOf() {
		if (!((t.parentsOf(a).isEmpty()))) {
			throw new AssertionError();
		}
	}

	@Test public void testDirectParentsOf() {
		if (!((t.directParentsOf(a).isEmpty()))) {
			throw new AssertionError();
		}
	}

	@Test public void testAncestorsOf() {
		if (!((t.ancestorsOf(a).isEmpty()))) {
			throw new AssertionError();
		}
	}

	@Test public void testExciseVertex() {
		assertEquals(e, t.exciseVertex(a));
	}

	@Test public void testExciseVertices() {
		final Set<ClassExpression> setA = Stream.of(a).map(e -> (ClassExpression)e).collect(Collectors.toSet());
		assertEquals(e, t.exciseVertices(setA));
	}

	@Test public void testExciseVerticesIf() {
		assertEquals(e, t.exciseVerticesIf(v -> v == a));
	}

	@Test public void testRootAt() {
		assertEquals(t, e.rootAt(a));
	}

	@Test public void testMultiParentChild() {
		assertFalse(t.multiParentChild().isPresent());
	}

	@Test public void testTreeify() {
		assertEquals(t, t.treeify());
	}
}
