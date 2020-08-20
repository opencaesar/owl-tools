package io.opencaesar.closeworld;

import com.google.common.base.Objects;
import org.jgrapht.GraphTests;
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.opencaesar.closeworld.Axiom.ClassExpressionSetAxiom.DisjointClassesAxiom;
import static io.opencaesar.closeworld.Axiom.ClassExpressionSetAxiom.DisjointUnionAxiom;

@SuppressWarnings("serial")
public class Taxonomy extends DirectedAcyclicGraph<ClassExpression, Taxonomy.TaxonomyEdge> {

	public Taxonomy() {
		super(TaxonomyEdge.class);
	}

	public Taxonomy(final List<ClassExpression> edgeList) {
		this(edgeList, edgeList);
	}

	public Taxonomy(final List<ClassExpression> vertexList, final List<ClassExpression> edgeList) {
		super(TaxonomyEdge.class);
		
		vertexList.forEach(this::addVertex);
		
		final Iterator<ClassExpression> i = edgeList.iterator();
		while (i.hasNext()) {
			{
				final ClassExpression p = i.next();
				final ClassExpression c = i.next();
				addEdge(p, c);
			}
		}
	}

	public Set<ClassExpression> childrenOf(final ClassExpression v) {
		return outgoingEdgesOf(v).stream().map(this::getEdgeTarget).collect(Collectors.toSet());
	}

	public Set<ClassExpression> descendantsOf(final ClassExpression v) {
		return getDescendants(v);
	}

	public Set<ClassExpression> directChildrenOf(final ClassExpression v) {
		final Set<ClassExpression> c = childrenOf(v);
		final HashSet<ClassExpression> cd = new HashSet<>();
		c.forEach(e -> cd.addAll(descendantsOf(e)));
		return c.stream().filter(e -> !cd.contains(e)).collect(Collectors.toSet());
	}

	public Set<ClassExpression> parentsOf(final ClassExpression v) {
		return incomingEdgesOf(v).stream().map(this::getEdgeSource).collect(Collectors.toSet());
	}

	public Set<ClassExpression> ancestorsOf(final ClassExpression v) {
		return getAncestors(v);
	}

	public Set<ClassExpression> directParentsOf(final ClassExpression v) {
		final Set<ClassExpression> p = parentsOf(v);
		final HashSet<ClassExpression> pa = new HashSet<>();
		p.forEach(e -> pa.addAll(ancestorsOf(e)));
		return p.stream().filter(e -> !pa.contains(e)).collect(Collectors.toSet());
	}

	public Optional<ClassExpression> multiParentChild() {
		return vertexSet().stream().filter(it -> parentsOf(it).size() > 1).findFirst();
	}

	public Taxonomy exciseVertex(final ClassExpression v) {
		final Taxonomy g = new Taxonomy();
				 
		// Copy all vertices except the specified vertex.
		
		vertexSet().stream().filter(e -> e != v).forEach(g::addVertex);

		// Copy all edges no involving v. Remember parents and children of v.
		
		final Set<ClassExpression> parents = new HashSet<>();
		final Set<ClassExpression> children = new HashSet<>();
		
		edgeSet().forEach(e -> {
			final ClassExpression s = getEdgeSource(e);
			final ClassExpression t = getEdgeTarget(e);
			if (s == v) {
				children.add(t);
			} else if (t == v) {
				parents.add(s);
			} else {
				g.addEdge(s, t);				
			}
		});
		
		// Add edges from parents to children.
		
		parents.forEach(p -> children.forEach(c -> g.addEdge(p, c)));
		
		return g;
	}

	public Taxonomy exciseVertices(final Set<ClassExpression> vs) {
		if (vs.isEmpty()) {
			return this;
		} else {
			ClassExpression first = vs.iterator().next();
			Set<ClassExpression> rest = vs.stream().filter(it -> it != first).collect(Collectors.toSet());
			return exciseVertex(first).exciseVertices(rest);
		}
	}

	public Taxonomy exciseVerticesIf(final Predicate<ClassExpression> predicate) {
		return exciseVertices(vertexSet().stream().filter(predicate).collect(Collectors.toSet()));
	}

	public Taxonomy rootAt(final ClassExpression root) {
		final Taxonomy g = (Taxonomy) clone();

		g.addVertex(root);

		vertexSet().stream().filter(v -> inDegreeOf(v) == 0).forEach(t -> g.addEdge(root, t));

		return g;
	}

	public Taxonomy transitiveReduction() {
		final Taxonomy tr = (Taxonomy) clone();
		TransitiveReduction.INSTANCE.reduce(tr);
		return tr;
	}

	/**
	 * Bypass a single parent of a child.
	 * 
	 * @param child  ClassExpression
	 * @param parent ClassExpression
	 * @return Taxonomy
	 */
	public Taxonomy bypassParent(final ClassExpression child, final ClassExpression parent) {
		final Taxonomy g = new Taxonomy();
				
		// Copy all vertices.
		
		vertexSet().forEach(g::addVertex);
		
		// Copy all edges except that from parent to child.
		
		edgeSet().stream().map(e -> new AbstractMap.SimpleEntry<>(getEdgeSource(e), getEdgeTarget(e)))
			.filter(it -> it.getKey() != parent || it.getValue() != child)
			.forEach(p -> g.addEdge(p.getKey(), p.getValue()));
		
		// Add edges from direct grandparents to child.
		
		directParentsOf(parent).forEach(gp -> g.addEdge(gp, child));
		
		return g;
	}

	/**
	 * Recursively bypass parents of a child.
	 * 
	 * @param child   ClassExpression
	 * @param parents Set
	 * @return Taxonomy
	 */
	public Taxonomy bypassParents(final ClassExpression child, final Set<ClassExpression> parents) {
		if (parents.isEmpty()) {
			return this;
		} else {
			ClassExpression first = parents.iterator().next();
			Set<ClassExpression> rest = parents.stream().filter(it -> it != first).collect(Collectors.toSet());
			return bypassParent(child, first).bypassParents(child, rest);
		}
	}

	/**
	 * Eliminate redundant edges above child.
	 * 
	 * @param child ClassExpression
	 * @return Taxonomy
	 */
	public Taxonomy reduceChild(final ClassExpression child) {
		final Taxonomy g = new Taxonomy();
				
		// Copy all vertices.
		
		vertexSet().forEach(g::addVertex);
		
		// Copy all edges to child.
		
		edgeSet().stream().map(e -> new AbstractMap.SimpleEntry<>(getEdgeSource(e), getEdgeTarget(e)))
			.filter(it -> it.getValue() != child)
			.forEach(p -> g.addEdge(p.getKey(), p.getValue()));
			
		// Eliminate redundant edges above child.
		
		directParentsOf(child).forEach(p -> g.addEdge(p, child));
		
		return g;
	}

	/**
	 * Isolate child from one parent.
	 * 
	 * @param child  ClassExpression
	 * @param parent ClassExpression
	 * @return Taxonomy
	 */
	public Taxonomy isolateChildFromOne(final ClassExpression child, final ClassExpression parent) {
		if (parentsOf(parent).isEmpty()) {
			return this;
		} else {
			final Taxonomy g = new Taxonomy();
			
			final ClassExpression diff = parent.difference(child);

			final HashSet<ClassExpression> newVertices = new HashSet<>(vertexSet());
			newVertices.remove(parent);
			newVertices.add(diff);
			newVertices.forEach(g::addVertex);
			
			edgeSet().forEach(e -> {
				final ClassExpression s = getEdgeSource(e);
				final ClassExpression t = getEdgeTarget(e);
				if (s == parent) {
					if (t != child) g.addEdge(diff, t);
				} else if (t == parent) {
					g.addEdge(s, diff);
				} else {
					g.addEdge(s, t);
				}
			});
			
			return g;
		}
	}

	/**
	 * Recursively isolate child from parents.
	 * 
	 * @param child   ClassExpression
	 * @param parents Set
	 * @return Taxonomy
	 */
	public Taxonomy isolateChild(final ClassExpression child, final Set<ClassExpression> parents) {
		if (parents.isEmpty()) {
			return this;
		} else {
			ClassExpression first = parents.iterator().next();
			Set<ClassExpression> rest = parents.stream().filter(it -> it != first).collect(Collectors.toSet());
			return isolateChildFromOne(child, first).isolateChild(child, rest);
		}
	}

	/**
	 * Recursively bypass, reduce, and isolate until the result is a tree.
	 * 
	 * @return Taxonomy
	 */
	public Taxonomy treeify() {
		final Optional<ClassExpression> co = multiParentChild();
	 		 	
	 	if (co.isPresent()) {
			final ClassExpression child = co.get();
			final Set<ClassExpression> parents = parentsOf(child);
			final Taxonomy bp = bypassParents(child, parents);
			final Taxonomy rd = bp.reduceChild(child);
	 		return rd.isolateChild(child, parents).treeify();
	 	} else {
	 		return this;
	 	}
	}

	/**
	 * Produce a map from each parent ClassExpression to its children (if more than
	 * one).
	 * 
	 * @return HashMap
	 */
	public HashMap<ClassExpression, Set<ClassExpression>> siblingMap() {
		final HashMap<ClassExpression, Set<ClassExpression>> map = new HashMap<>();
		vertexSet().forEach(p -> {
			final Set<ClassExpression> cl = edgesOf(p).stream()
				.filter(e1 -> getEdgeSource(e1) == p)
				.map(this::getEdgeTarget)
				.collect(Collectors.toSet());
			if (cl.size() > 1) map.put(p, cl);
		});
		return map;
	}

	/**
	 * Generate closure axioms.
	 *
	 * @return List<Axiom>
	 */
	public Set<Axiom> generateClosureAxioms(io.opencaesar.closeworld.Axiom.AxiomType axiomType) throws UnconnectedTaxonomyException, InvalidTreeException {

		ensureConnected();
		final Taxonomy tree = treeify();
		tree.ensureTree();
		final HashMap<ClassExpression, Set<ClassExpression>> siblingMap = tree.siblingMap();

		final HashSet<Axiom> axioms = new HashSet<>();

		for (Map.Entry<ClassExpression, Set<ClassExpression>> entry : siblingMap.entrySet()) {
			ClassExpression c = entry.getKey();
			Set<ClassExpression> s = entry.getValue();
			switch (axiomType) {
				case DISJOINT_CLASSES:
					axioms.add(new DisjointClassesAxiom(s));
					break;
				case DISJOINT_UNION:
					axioms.add(
							(c instanceof ClassExpression.Singleton) ?
									new DisjointUnionAxiom((ClassExpression.Singleton) c, s) :
									new DisjointClassesAxiom(s)
					);
					break;
				case EQUIVALENT_CLASSES:
				default:
			}
		}

		return axioms;
	}

	/**
	 * Test whether this Taxonomy is connected.
	 * 
	 * @return boolean
	 */
	public boolean isConnected() {
		return new ConnectivityInspector<>(this).isConnected();
	}

	/**
	 * Throw UnconnectedTaxonomyException unless connected.
	 */
	public void ensureConnected() throws UnconnectedTaxonomyException {
		if (!isConnected()) {
			throw new UnconnectedTaxonomyException("taxonomy is not connected");
		}
	}

	/**
	 * Test whether this Taxonomy is a tree.
	 * 
	 * @return boolean
	 */
	public boolean isTree() {
		final AsUndirectedGraph<ClassExpression, TaxonomyEdge> ug = new AsUndirectedGraph<>(this);
		return GraphTests.isTree(ug);
	}

	/**
	 * Throw InvalidTreeException unless a tree.
	 */
	public void ensureTree() throws InvalidTreeException {
		if (!isTree()) {
			throw new InvalidTreeException("treeify method returned an invalid tree");
		}
	}

	public static class UnconnectedTaxonomyException extends RuntimeException {
		public UnconnectedTaxonomyException(final String s) {
			super(s);
		}
	}

	public static class InvalidTreeException extends RuntimeException {
		public InvalidTreeException(final String s) {
			super(s);
		}
	}

	public static class TaxonomyEdge extends DefaultEdge {
		@Override
		public int hashCode() {
			return Arrays.asList(getSource(), getTarget()).hashCode();
		}

		@Override
		public boolean equals(final Object o) {
			return (((o instanceof TaxonomyEdge)
					&& Objects.equal(((TaxonomyEdge) o).getSource(), getSource()))
					&& Objects.equal(((TaxonomyEdge) o).getTarget(), getTarget()));
		}
	}
}
