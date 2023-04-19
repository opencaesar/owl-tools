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
		Assert.assertNotNull(tu.getEdge(va, vb));
		Assert.assertNotNull(tu.getEdge(va, vc));
		Assert.assertNotNull(tu.getEdge(va, vi));
		Assert.assertNotNull(tu.getEdge(vb, vc));
		Assert.assertNotNull(tu.getEdge(vb, vd));
		Assert.assertNotNull(tu.getEdge(vb, ve));
		Assert.assertNotNull(tu.getEdge(vb, vf));
		Assert.assertNotNull(tu.getEdge(vc, vg));
		Assert.assertNotNull(tu.getEdge(vc, vh));
		Assert.assertNotNull(tu.getEdge(ve, vj));
		Assert.assertNotNull(tu.getEdge(vf, vi));
		Assert.assertNotNull(tu.getEdge(vg, vi));
		Assert.assertNotNull(tu.getEdge(vg, vj));
		Assert.assertNotNull(tu.getEdge(vh, vj));
		Assert.assertNotNull(tu.getEdge(vi, vj));
	}
	

	@Test public void testChildrenOf() {
		Assert.assertEquals(0, tu.childrenOf(vj).size());
		Assert.assertEquals(Stream.of(vb, vc, vi).collect(Collectors.toSet()), tu.childrenOf(va));
		Assert.assertEquals(Stream.of(vj).collect(Collectors.toSet()), tu.childrenOf(ve));
	}
	
}
