package io.opencaesar.closetaxonomy;

import io.opencaesar.closetaxonomy.ClassExpression.Singleton;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestEmptyTaxonomy {
	private Singleton a;
	private Taxonomy t;
	private Taxonomy tA;

	@BeforeClass public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass public static void tearDownAfterClass() throws Exception {
	}

	@Before public void setUp() throws Exception {
		a = new Singleton("a");
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
