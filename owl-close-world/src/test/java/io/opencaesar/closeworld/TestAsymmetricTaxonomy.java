package io.opencaesar.closeworld;

import io.opencaesar.closeworld.ClassExpression.Unitary;
import org.junit.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.opencaesar.closeworld.Axiom.AxiomType.DISJOINT_CLASSES;
import static io.opencaesar.closeworld.Axiom.AxiomType.DISJOINT_UNION;

@SuppressWarnings("all")
public class TestAsymmetricTaxonomy {

	HashMap<String, ClassExpression> vertexMap = new HashMap<String, ClassExpression>();
	HashMap<ClassExpression, Set<ClassExpression>> siblingMap = new HashMap<ClassExpression, Set<ClassExpression>>();
	HashSet<Axiom> disjointClassesAxioms = new HashSet<Axiom>();
	HashSet<Axiom> disjointUnionAxioms = new HashSet<Axiom>();

	Taxonomy initialTaxonomy;
	Taxonomy redundantEdgeTaxonomy;
	Taxonomy afterExciseVertexTaxonomy;
	Taxonomy afterExciseVerticesTaxonomy;
	Taxonomy unrootedTaxonomy;
	Taxonomy afterBypassOneTaxonomy;
	Taxonomy afterBypassAllTaxonomy;
	Taxonomy afterReduceTaxonomy;
	Taxonomy afterIsolateOneTaxonomy;
	Taxonomy afterIsolateAllTaxonomy;
	Taxonomy afterTreeifyTaxonomy;

	@BeforeClass public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass public static void tearDownAfterClass() throws Exception {
	}

	@Before public void setUp() throws Exception {
		
		// Initial Taxonomy

		final List<String> initialEdgeSpec = Stream.of(
			"a", "b",
			"a", "c",
			"b", "d",
			"b", "e",
			"c", "f",
			"c", "g",
			"c", "i",
			"e", "h",
			"e", "i",
			"f", "k",
			"i", "j",
			"j", "k").collect(Collectors.toList());
		
		Set<String> initialVertexNames = initialEdgeSpec.stream().collect(Collectors.toSet());
		
		initialVertexNames.forEach(vn -> {
			vertexMap.put(vn, new Unitary(vn));
		});
		
		List<ClassExpression> initialEdgeList = initialEdgeSpec.stream().map(e -> vertexMap.get(e)).collect(Collectors.toList());
		
		initialTaxonomy = new Taxonomy(initialEdgeList);

		// Redundant edges.

		final List<String> redundantEdgeSpec = Stream.of(
				"a", "d",
				"a", "e",
				"a", "f",
				"a", "g",
				"a", "h",
				"a", "i",
				"a", "j",
				"a", "k",
				"b", "h",
				"b", "i",
				"b", "j",
				"c", "j",
				"b", "k",
				"c", "k",
				"e", "j",
				"e", "k",
				"i", "k").collect(Collectors.toList());

		List<ClassExpression> redundantEdgeList = redundantEdgeSpec.stream().map(e -> vertexMap.get(e)).collect(Collectors.toList());
		
		redundantEdgeList.addAll(initialEdgeList);
		
		redundantEdgeTaxonomy = new Taxonomy(redundantEdgeList);
		
		// After exciseVertex(i)

		final List<String> afterExciseVertexEdgeSpec = Stream.of(
				"a", "b",
				"a", "c",
				"b", "d",
				"b", "e",
				"c", "f",
				"c", "g",
				"c", "j",
				"e", "h", 
				"e", "j",
				"f", "k",
				"j", "k").collect(Collectors.toList());
				
		final List<ClassExpression> afterExciseVertexEdgeList = afterExciseVertexEdgeSpec.stream()
			.map(e -> vertexMap.get(e)).collect(Collectors.toList());
		
		afterExciseVertexTaxonomy = new Taxonomy(afterExciseVertexEdgeList);
		
		// After exciseVertices({b, d, e, f, g})
		
		final List<String> afterExciseVerticesEdgeSpec = Stream.of(
			"c", "f",
			"c", "i",
			"f", "k",
			"i", "j",
			"j", "k").collect(Collectors.toList());
			
		final List<ClassExpression> afterExciseVerticesEdgeList = afterExciseVerticesEdgeSpec.stream()
			.map(e -> vertexMap.get(e)).collect(Collectors.toList());
		
		afterExciseVerticesTaxonomy = new Taxonomy(afterExciseVerticesEdgeList);
		
		// After exciseVertices({b, d, e, f, g})
		
		final List<String> unrootedEdgeSpec = Stream.of(
			"b", "d",
			"b", "e",
			"c", "f",
			"c", "g",
			"c", "i",
			"e", "h",
			"e", "i",
			"f", "k",
			"i", "j",
			"j", "k").collect(Collectors.toList());
			
		final List<ClassExpression> unrootedEdgeList = unrootedEdgeSpec.stream()
			.map(e -> vertexMap.get(e)).collect(Collectors.toList());
			
		unrootedTaxonomy = new Taxonomy(unrootedEdgeList);
		
		// After bypass(i, c)
		
		final List<String> afterBypassOneEdgeSpec = Stream.of(
			"a", "b",
			"a", "c", 
			"b", "d",
			"b", "e", 
			"c", "f",
			"c", "g",
			"a", "i",
			"e", "h",
			"e", "i",
			"f", "k",
			"i", "j",
			"j", "k").collect(Collectors.toList());
			
		final List<ClassExpression> afterBypassOneEdgeList = afterBypassOneEdgeSpec.stream()
			.map(e -> vertexMap.get(e)).collect(Collectors.toList());
		
		afterBypassOneTaxonomy = new Taxonomy(afterBypassOneEdgeList);
		
		// After bypass(i, {c, e})
		
		final List<String> afterBypassAllEdgeSpec = Stream.of(
			"a", "b",
			"a", "c", 
			"a", "i",
			"b", "d",
			"b", "e",
			"b", "i",
			"c", "f",
			"c", "g",
			"e", "h",
			"f", "k",
			"i", "j",
			"j", "k").collect(Collectors.toList());
			
		final List<ClassExpression> afterBypassAllEdgeList = afterBypassAllEdgeSpec.stream()
			.map(e -> vertexMap.get(e)).collect(Collectors.toList());
		
		afterBypassAllTaxonomy = new Taxonomy(afterBypassAllEdgeList);
		
		// After bypass(i, {c, e})
		
		final List<String> afterReduceEdgeSpec = Stream.of(
			"a", "b",
			"a", "c",
			"b", "d",
			"b", "e",
			"b", "i",
			"c", "f",
			"c", "g",
			"e", "h",
			"f", "k",
			"i", "j",
			"j", "k").collect(Collectors.toList());
			
		final List<ClassExpression> afterReduceEdgeList = afterReduceEdgeSpec.stream()
			.map(e -> vertexMap.get(e)).collect(Collectors.toList());
			
		afterReduceTaxonomy = new Taxonomy(afterReduceEdgeList);
		
		final List<String> afterIsolateOneEdgeSpec = Stream.of(
			"a", "b",
			"a", "c\\i",
			"b", "d",
			"b", "e",
			"b", "i",
			"c\\i", "f",
			"c\\i", "g",
			"e", "h",
			"f", "k",
			"i", "j",
			"j", "k").collect(Collectors.toList());
			
		vertexMap.put("c\\i", vertexMap.get("c").difference(vertexMap.get("i")));
		
		final List<ClassExpression> afterIsolateOneEdgeList = afterIsolateOneEdgeSpec.stream()
			.map(e -> vertexMap.get(e)).collect(Collectors.toList());
		
		afterIsolateOneTaxonomy = new Taxonomy(afterIsolateOneEdgeList);
		
		final List<String> afterIsolateAllEdgeSpec = Stream.of(
			"a", "b",
			"a", "c\\i",
			"b", "d",
			"b", "e\\i",
			"b", "i",
			"c\\i", "f",
			"c\\i", "g",
			"e\\i", "h",
			"f", "k",
			"i", "j",
			"j", "k").collect(Collectors.toList());
			
		vertexMap.put("e\\i", vertexMap.get("e").difference(vertexMap.get("i")));
		
		final List<ClassExpression> afterIsolateAllEdgeList = afterIsolateAllEdgeSpec.stream()
			.map(e -> vertexMap.get(e)).collect(Collectors.toList());
		
		afterIsolateAllTaxonomy = new Taxonomy(afterIsolateAllEdgeList);
		
		final List<String> afterTreeifyEdgeSpec = Stream.of(
			"a", "b",
			"a", "c\\(i∪k)",
			"b", "d",
			"b", "e\\i",
			"b", "i\\k",
			"b", "k",
			"c\\(i∪k)",
			"f\\k", "c\\(i∪k)",
			"g", "e\\i",
			"h", "i\\k",
			"j\\k").collect(Collectors.toList());
			
		vertexMap.put("c\\(i∪k)", vertexMap.get("c").difference((vertexMap.get("i")).union(vertexMap.get("k"))));
		vertexMap.put("f\\k", vertexMap.get("f").difference(vertexMap.get("k")));
		vertexMap.put("i\\k", vertexMap.get("i").difference(vertexMap.get("k")));
		vertexMap.put("j\\k", vertexMap.get("j").difference(vertexMap.get("k")));
		
		final List<ClassExpression> afterTreeifyEdgeList = afterTreeifyEdgeSpec.stream().map(e -> vertexMap.get(e)).
			collect(Collectors.toList());
			
		afterTreeifyTaxonomy = new Taxonomy(afterTreeifyEdgeList);
		
		siblingMap.put(vertexMap.get("a"),
			Stream.of("b", "c\\(i∪k)").map(s -> vertexMap.get(s)).collect(Collectors.toSet()));
		siblingMap.put(vertexMap.get("b"),
			Stream.of("d", "e\\i", "i\\k", "k").map(s -> vertexMap.get(s)).collect(Collectors.toSet()));
		siblingMap.put(vertexMap.get("c\\(i∪k)"),
			Stream.of("f\\k", "g").map(s -> vertexMap.get(s)).collect(Collectors.toSet()));

		siblingMap.forEach((c, s) -> {
			disjointClassesAxioms.add(new Axiom.ClassExpressionSetAxiom.DisjointClassesAxiom(s));
			disjointUnionAxioms.add(
					(c instanceof Unitary) ?
							new Axiom.ClassExpressionSetAxiom.DisjointUnionAxiom((Unitary) c, s) :
							new Axiom.ClassExpressionSetAxiom.DisjointClassesAxiom(s)
			);
		});
	}

	@After public void tearDown() throws Exception {
	}

	@Test public void testChildrenOf() {
		Set<ClassExpression> bc = Stream.of("b", "c").map(vn -> vertexMap.get(vn))
			.collect(Collectors.toSet());
		Assert.assertEquals(bc, initialTaxonomy.childrenOf(vertexMap.get("a")));
	}

	@Test public void testDescendantsOf() {
		Set<ClassExpression> bcdefghijk = Stream.of("b", "c", "d", "e", "f", "g", "h", "i", "j", "k")
			.map(vn -> vertexMap.get(vn)).collect(Collectors.toSet());
		Assert.assertEquals(bcdefghijk, initialTaxonomy.descendantsOf(vertexMap.get("a")));
	}

	@Test public void testDirectChildrenOf() {
		Set<ClassExpression> bc = Stream.of("b", "c").map(vn -> vertexMap.get(vn)).collect(Collectors.toSet());
		Assert.assertEquals(bc, initialTaxonomy.directChildrenOf(vertexMap.get("a")));
	}

	@Test public void testParentsOf() {
		Set<ClassExpression> ce = Stream.of("c", "e").map(vn -> vertexMap.get(vn)).collect(Collectors.toSet());
		Assert.assertEquals(ce, initialTaxonomy.parentsOf(vertexMap.get("i")));
	}

	@Test public void testAncestorsOf() {
		Set<ClassExpression> abce = Stream.of("a", "b", "c", "e").map(vn -> vertexMap.get(vn)).collect(Collectors.toSet());
		Assert.assertEquals(abce, initialTaxonomy.ancestorsOf(vertexMap.get("i")));
	}

	@Test public void testDirectParentsOf() {
		Set<ClassExpression> ce = Stream.of("c", "e").map(vn -> vertexMap.get(vn)).collect(Collectors.toSet());
		Assert.assertEquals(ce, initialTaxonomy.directParentsOf(vertexMap.get("i")));
	}

	@Test public void testMultiParentChild() {
		Optional<ClassExpression> childOption = initialTaxonomy.multiParentChild();
		Assert.assertTrue(childOption.isPresent());
		Assert.assertEquals(vertexMap.get("i"), childOption.get());
	}

	@Test public void testExciseVertex() {
		Assert.assertEquals(afterExciseVertexTaxonomy, initialTaxonomy.exciseVertex(vertexMap.get("i")));
	}

	@Test public void testExciseVertices() {
		Set<ClassExpression> exciseSet = Stream.of("a", "b", "d", "e", "g", "h").map(s -> vertexMap.get(s)).collect(Collectors.toSet());
		Assert.assertEquals(afterExciseVerticesTaxonomy, initialTaxonomy.exciseVertices(exciseSet));
	}

	@Test public void testExciseVerticesIf() {
		final Set<ClassExpression> exciseSet = Stream.of("a", "b", "d", "e", "g", "h")
			.map(s -> vertexMap.get(s)).collect(Collectors.toSet());
		Assert.assertEquals(afterExciseVerticesTaxonomy, initialTaxonomy.exciseVerticesIf(v -> exciseSet.contains(v)));
	}

	@Test public void testRootAt() {
		Assert.assertEquals(initialTaxonomy, unrootedTaxonomy.rootAt(vertexMap.get("a")));
	}

	@Test public void testTransitiveReduction() {
		Assert.assertEquals(initialTaxonomy, redundantEdgeTaxonomy.transitiveReduction());
	}

	@Test public void testBypassParent() {
		ClassExpression c = vertexMap.get("c");
		ClassExpression i = vertexMap.get("i");
		Assert.assertEquals(afterBypassOneTaxonomy, initialTaxonomy.bypassParent(i, c));
	}

	@Test public void testBypassParents() {
		ClassExpression i = vertexMap.get("i");
		Assert.assertEquals(afterBypassAllTaxonomy,
			initialTaxonomy.bypassParents(i, initialTaxonomy.parentsOf(i)));
	}

	@Test public void testReduceChild() {
		ClassExpression i = vertexMap.get("i");
		Assert.assertEquals(afterReduceTaxonomy, afterBypassAllTaxonomy.reduceChild(i));
	}

	@Test public void testIsolateChildFromOne() {
		ClassExpression c = vertexMap.get("c");
		ClassExpression i = vertexMap.get("i");
		Assert.assertEquals(afterIsolateOneTaxonomy, afterReduceTaxonomy.isolateChildFromOne(i, c));
	}

	@Test public void testIsolateChild() {
		ClassExpression i = vertexMap.get("i");
		Assert.assertEquals(afterIsolateAllTaxonomy,
			afterReduceTaxonomy.isolateChild(i, initialTaxonomy.parentsOf(i)));
	}

	@Test public void testTreeify() {
		Assert.assertEquals(afterTreeifyTaxonomy, initialTaxonomy.treeify());
	}

	@Test public void testSiblingMap() {
		Assert.assertEquals(siblingMap, afterTreeifyTaxonomy.siblingMap());
	}

	@Test public void testGenerateClosureAxioms() {
		Assert.assertEquals(disjointClassesAxioms, initialTaxonomy.generateClosureAxioms(DISJOINT_CLASSES));
		Assert.assertEquals(disjointUnionAxioms, initialTaxonomy.generateClosureAxioms(DISJOINT_UNION));
	}
}
