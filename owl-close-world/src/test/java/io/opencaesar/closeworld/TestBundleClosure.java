package io.opencaesar.closeworld;

import io.opencaesar.closeworld.ClassExpression.Unitary;

import org.jgrapht.graph.DefaultEdge;
import org.junit.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.opencaesar.closeworld.Axiom.AxiomType.DISJOINT_CLASSES;
import static io.opencaesar.closeworld.Axiom.AxiomType.DISJOINT_UNION;

@SuppressWarnings("all")
public class TestBundleClosure {

	HashMap<String, ClassExpression> vertexMap = new HashMap<String, ClassExpression>();

	Taxonomy tu;
	Taxonomy tv;
	Taxonomy tr;
	ClassExpression va;
	ClassExpression vb;
	ClassExpression vc;
	ClassExpression vd;
	ClassExpression ve;
	ClassExpression vf;
	ClassExpression vg;
	ClassExpression vh;
	ClassExpression vi;
	ClassExpression vj;
	Set<ClassExpression> initialVertexSet;
	List<ClassExpression> initialEdgeList;

	@BeforeClass public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass public static void tearDownAfterClass() throws Exception {
	}

	@Before public void setUp() throws Exception {
		
		// Initial Taxonomy

		va = new Unitary("a");
		vb = new Unitary("b");
		vc = new Unitary("c");
		vd = new Unitary("d");
		ve = new Unitary("e");
		vf = new Unitary("f");
		vg = new Unitary("g");
		vh = new Unitary("h");
		vi = new Unitary("i");
		vj = new Unitary("j");
		
		initialVertexSet = Stream.of(
				va, vb, vc, vd, ve, vf, vg, vh, vi, vj).collect(Collectors.toSet());
		
		initialEdgeList = Stream.of(
				va, vb,  va, vc,  va, vi,  vb, vc,  vb, vd,  vb, ve,  vb, vf,  vc, vg,
				vc, vh,  ve, vj,  vf, vi,  vg, vi,  vg, vj,  vh, vj,  vi, vj
			).collect(Collectors.toList());
				
				
		tu = new Taxonomy(initialEdgeList);

		tv = (Taxonomy) tu.clone();
		
		tr = tu.transitiveReduction();
	}

	@After public void tearDown() throws Exception {
	}

	@Test public void testConstructors() {
		Assert.assertEquals(initialVertexSet, tu.vertexSet());
		Assert.assertEquals(15, tu.edgeSet().size());
		Assert.assertTrue(tu.containsEdge(va, vb));
		Assert.assertTrue(tu.containsEdge(va, vc));
		Assert.assertTrue(tu.containsEdge(va, vi));
		Assert.assertTrue(tu.containsEdge(vb, vc));
		Assert.assertTrue(tu.containsEdge(vb, vd));
		Assert.assertTrue(tu.containsEdge(vb, ve));
		Assert.assertTrue(tu.containsEdge(vb, vf));
		Assert.assertTrue(tu.containsEdge(vc, vg));
		Assert.assertTrue(tu.containsEdge(vc, vh));
		Assert.assertTrue(tu.containsEdge(ve, vj));
		Assert.assertTrue(tu.containsEdge(vf, vi));
		Assert.assertTrue(tu.containsEdge(vg, vi));
		Assert.assertTrue(tu.containsEdge(vg, vj));
		Assert.assertTrue(tu.containsEdge(vh, vj));
		Assert.assertTrue(tu.containsEdge(vi, vj));
	}
	

	@Test public void testChildren() {
		Assert.assertEquals(0, tu.childrenOf(vj).size());
		Assert.assertEquals(Stream.of(vb, vc, vi).collect(Collectors.toSet()), tu.childrenOf(va));
		Assert.assertEquals(Stream.of(vj).collect(Collectors.toSet()), tu.childrenOf(ve));
	}
	
	@Test public void testDescendants() {
		Assert.assertEquals(0, tu.descendantsOf(vj).size());
		Assert.assertEquals(Stream.of(vj).collect(Collectors.toSet()), tu.descendantsOf(vh));
		Assert.assertEquals(Stream.of(vj).collect(Collectors.toSet()), tu.descendantsOf(ve));
		Assert.assertEquals(Stream.of(vg, vh, vi, vj).collect(Collectors.toSet()), tu.descendantsOf(vc));
		Assert.assertEquals(Stream.of(vb, vc, vd, ve, vf, vg, vh, vi, vj).collect(Collectors.toSet()), tu.descendantsOf(va));
	}
	
	@Test public void testDirectChildren() {
		Assert.assertEquals(0, tu.directChildrenOf(vj).size());
		Assert.assertEquals(Stream.of(vb).collect(Collectors.toSet()), tu.directChildrenOf(va));
	}
	
	@Test public void testParents() {
		Assert.assertEquals(0, tu.parentsOf(va).size());
		Assert.assertEquals(Stream.of(va, vf, vg).collect(Collectors.toSet()), tu.parentsOf(vi));
		Assert.assertEquals(Stream.of(ve, vg, vh, vi).collect(Collectors.toSet()), tu.parentsOf(vj));
	}
	
	@Test public void testAncestors() {
		Assert.assertEquals(0, tu.ancestorsOf(va).size());
		Assert.assertEquals(Stream.of(va).collect(Collectors.toSet()), tu.ancestorsOf(vb));
		Assert.assertEquals(Stream.of(va, vb, vc, vf, vg).collect(Collectors.toSet()), tu.ancestorsOf(vi));
		Assert.assertEquals(Stream.of(va, vb, vc, ve, vf, vg, vh, vi).collect(Collectors.toSet()), tu.ancestorsOf(vj));
	}
	
	@Test public void testDirectParents() {
		Assert.assertEquals(0, tu.directParentsOf(va).size());
		Assert.assertEquals(Stream.of(vf, vg).collect(Collectors.toSet()), tu.directParentsOf(vi));
	}
	
	@Test public void testMultiParentChild() {
		Assert.assertTrue(Stream.of(Optional.of(vi), Optional.of(vj)).collect(Collectors.toSet()).contains(tu.multiParentChild()));
	}
	
	@Test public void testMultiTransitiveClosure() {
		Assert.assertTrue(tr.containsEdge(vg, vi));
		Assert.assertTrue(tr.containsEdge(vi, vj));
		Assert.assertFalse(tr.containsEdge(vg, vj));
	}
	
	@Test public void testExciseVertex() {
		Taxonomy g = tr.exciseVertex(vi);
		Assert.assertFalse(g.containsVertex(vi));
		Assert.assertFalse(g.containsEdge(vg, vi));
		Assert.assertFalse(g.containsEdge(vi, vj));
		Assert.assertTrue(g.containsEdge(vg, vj));
	}
	
}
