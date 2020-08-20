package io.opencaesar.closetaxonomy;

import io.opencaesar.closetaxonomy.ClassExpression.Singleton;
import io.opencaesar.closetaxonomy.Taxonomy.InvalidTreeException;
import io.opencaesar.closetaxonomy.Taxonomy.UnconnectedTaxonomyException;
import org.junit.*;

import static org.junit.Assert.*;

public class TestTaxonomyProperties {
	
	private Singleton a;
	private Singleton b;
	private Singleton c;
	
	private Taxonomy connectedTree;
	private Taxonomy connectedNotTree;
	private Taxonomy notConnectedNotTree;
	
	@BeforeClass public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass public static void tearDownAfterClass() throws Exception {
	}

	@Before public void setUp() throws Exception {
		a = new Singleton("a");
		b = new Singleton("b");
		c = new Singleton("c");
		
		notConnectedNotTree = new Taxonomy();
		notConnectedNotTree.addVertex(a);
		notConnectedNotTree.addVertex(b);
		notConnectedNotTree.addVertex(c);
		
		connectedTree = (Taxonomy) notConnectedNotTree.clone();
		connectedTree.addEdge(a, b);
		connectedTree.addEdge(a, c);
		
		connectedNotTree = (Taxonomy) connectedTree.clone();
		connectedNotTree.addEdge(b, c);
		
	}

	@After public void tearDown() throws Exception {
	}

	@Test public void testIsConnected() {
		assertFalse(notConnectedNotTree.isConnected());
		assertTrue(connectedTree.isConnected());
		assertTrue(connectedNotTree.isConnected());
	}

	@Test public void testEnsureConnected() {
		try {
			notConnectedNotTree.ensureConnected();
			fail("no UnconnectedTaxonomyException thrown");
		}
		catch (UnconnectedTaxonomyException e) {
			assertTrue(true);
		}
		
		try {
			connectedTree.ensureConnected();
			assertTrue(true);
		}
		catch (UnconnectedTaxonomyException e) {
			fail("UnconnectedTaxonomyException thrown");
		}

		try {
			connectedNotTree.ensureConnected();
			assertTrue(true);
		}
		catch (UnconnectedTaxonomyException e) {
			fail("UnconnectedTaxonomyException thrown");
		}
	}

	@Test public void testIsTree() {
		assertFalse(notConnectedNotTree.isTree());
		assertTrue(connectedTree.isTree());
		assertFalse(connectedNotTree.isTree());
	}

	@Test public void testEnsureTree() {
		try {
			notConnectedNotTree.ensureTree();
			fail("no InvalidTreeException thrown");
		}
		catch (InvalidTreeException e) {
			assertTrue(true);
		}
	
		try {
			connectedTree.ensureTree();
			assertTrue(true);
		}
		catch (InvalidTreeException e) {
			fail("InvalidTreeException thrown");
		}

		try {
			connectedNotTree.ensureTree();
			fail("no InvalidTreeException thrown");
		}
		catch (InvalidTreeException e) {
			assertTrue(true);
		}
	}
}
