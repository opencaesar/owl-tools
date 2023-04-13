package io.opencaesar.closeworld;

import io.opencaesar.closeworld.ClassExpression.Unitary;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestEmptyTaxonomy {
	private Unitary a;
	private Taxonomy t;
	private Taxonomy tA;

	@BeforeClass public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass public static void tearDownAfterClass() throws Exception {
	}

	@Before public void setUp() throws Exception {
		a = new Unitary("a");
		t = new Taxonomy();
		tA = new Taxonomy();
		tA.addVertex(a);
	}

	@After public void tearDown() throws Exception {
	}

	@Test public void testMultiParentChild() {
		assertFalse(t.multiParentChild().isPresent());
	}

	@Test public void testExciseVerticesIf() {
		assertEquals(t, t.exciseVerticesIf(v -> true));
	}

	@Test public void testRootAt() {
		assertEquals(tA, t.rootAt(a));
	}

	@Test public void testTreeify() {
		assertEquals(t, t.treeify());
	}
}
