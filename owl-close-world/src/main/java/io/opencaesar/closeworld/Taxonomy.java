package io.opencaesar.closeworld;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jgrapht.GraphTests;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

import com.google.common.base.Objects;

import io.opencaesar.closeworld.Axiom.ClassExpressionSetAxiom.DisjointClassesAxiom;
import io.opencaesar.closeworld.Axiom.ClassExpressionSetAxiom.DisjointUnionAxiom;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

/**
 * A directed acyclic graph for implementing the bundle closure algorithm
 */
@SuppressWarnings("serial")
public class Taxonomy extends DirectedAcyclicGraph<ClassExpression, Taxonomy.TaxonomyEdge> {

	/**
	 * Empty-graph constructor
	 */
	public Taxonomy() {
		super(TaxonomyEdge.class);
	}

	/**
	 * Graph constructor from a list of adjacent vertices
	 * @param edgeList List of adjacent vertices
	 */
	public Taxonomy(final List<ClassExpression> edgeList) {
		this(edgeList, edgeList);
	}

	/**
	 * Graph constructor from a list of vertices and edges.
	 * @param vertexList a list of vertices
	 * @param edgeList a list of edges as pairs of adjacent vertices
	 */
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

	/**
	 * Returns the set of class expression vertices that have a direct child relationship with the given vertex.
	 * 
	 * @param v a class expression vertex
	 * @return Set of ClassExpressions
	 */
	public Set<ClassExpression> childrenOf(final ClassExpression v) {
		return outgoingEdgesOf(v).stream().map(this::getEdgeTarget).collect(Collectors.toSet());
	}

	/**
	 * Returns the set of class expression vertices that are directly or indirectly a child of the given vertex.
	 * 
	 * @param v a class expression vertex
	 * @return Set of ClassExpressions
	 */
	public Set<ClassExpression> descendantsOf(final ClassExpression v) {
		return getDescendants(v);
	}

	/**
	 * Returns the set of class expression vertices that are topologically the first children of the given vertex.
	 * 
	 * @param v a class expression vertex
	 * @return Set of ClassExpressions
	 */
	public Set<ClassExpression> directChildrenOf(final ClassExpression v) {
		final Set<ClassExpression> c = childrenOf(v);
		final HashSet<ClassExpression> cd = new HashSet<>();
		c.forEach(e -> cd.addAll(descendantsOf(e)));
		return c.stream().filter(e -> !cd.contains(e)).collect(Collectors.toSet());
	}

	/**
	 * Returns the set of class expression vertices that have a direct parent relationship with the given vertex.
	 * 
	 * @param v a class expression vertex
	 * @return Set of ClassExpressions
	 */
	public Set<ClassExpression> parentsOf(final ClassExpression v) {
		return incomingEdgesOf(v).stream().map(this::getEdgeSource).collect(Collectors.toSet());
	}

	/**
	 * Returns the set of class expression vertices that are directly or indirectly a parent of the given vertex.
	 * 
	 * @param v a class expression vertex
	 * @return Set of ClassExpressions
	 */
	public Set<ClassExpression> ancestorsOf(final ClassExpression v) {
		return getAncestors(v);
	}

	/**
	 * Returns the set of class expression vertices that are topologically the first parents of the given vertex.
	 * 
	 * @param v a class expression vertex
	 * @return Set of ClassExpressions
	 */
	public Set<ClassExpression> directParentsOf(final ClassExpression v) {
		final Set<ClassExpression> p = parentsOf(v);
		final HashSet<ClassExpression> pa = new HashSet<>();
		p.forEach(e -> pa.addAll(ancestorsOf(e)));
		return p.stream().filter(e -> !pa.contains(e)).collect(Collectors.toSet());
	}

	/**
	 * Returns the lowest multi-parent child if any exist.
	 * 
	 * @return Optional of ClassExpression
	 */
	public Optional<ClassExpression> multiParentChild() {
		final EdgeReversedGraph<ClassExpression, Taxonomy.TaxonomyEdge> reversedGraph =
				new EdgeReversedGraph<ClassExpression, Taxonomy.TaxonomyEdge>(this);
		final TopologicalOrderIterator<ClassExpression, Taxonomy.TaxonomyEdge> dfi = 
				new TopologicalOrderIterator<ClassExpression, Taxonomy.TaxonomyEdge>(reversedGraph);
		while (dfi.hasNext()) {
			final ClassExpression v = dfi.next();
			if (directParentsOf(v).size() > 1) return Optional.of(v);
		}
		return Optional.empty();
	}

	/**
	 * Returns a new directed graph obtained by removing the given class expression vertex.
	 * 
	 * @param v a class expression
	 * @return Taxonomy
	 */
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

	/**
	 * Returns a new directed graph obtained by removing the vertices corresponding to the given set of class expressions.
	 * 
	 * @param vs a set of class expression vertices.
	 * @return Taxonomy
	 */
	public Taxonomy exciseVertices(final Set<ClassExpression> vs) {
		if (vs.isEmpty()) {
			return this;
		} else {
			ClassExpression first = vs.iterator().next();
			Set<ClassExpression> rest = vs.stream().filter(it -> it != first).collect(Collectors.toSet());
			return exciseVertex(first).exciseVertices(rest);
		}
	}

	/**
	 * Returns a new directed graph obtained by removing the vertices satisfying the predicate.
	 * 
	 * @param predicate a predicate for filtering graph vertices
	 * @return Taxonomy
	 */
	public Taxonomy exciseVerticesIf(final Predicate<ClassExpression> predicate) {
		return exciseVertices(vertexSet().stream().filter(predicate).collect(Collectors.toSet()));
	}

	/**
	 * Returns a new directed graph with the given root added as the parent of all root vertices in the original graph.
	 * 
	 * @param root a class expression vertex.
	 * @return Taxonomy
	 */
	public Taxonomy rootAt(final ClassExpression root) {
		final Taxonomy g = (Taxonomy) clone();

		g.addVertex(root);

		vertexSet().stream().filter(v -> inDegreeOf(v) == 0).forEach(t -> g.addEdge(root, t));

		return g;
	}

	/**
	 * Returns a new directed graph with all transitive edges removed.
	 * 
	 * @return Taxonomy
	 */
	public Taxonomy transitiveReduction() {
		final Taxonomy tr = (Taxonomy) clone();
		TransitiveReduction.INSTANCE.reduce(tr);
		return tr;
	}

	public Taxonomy bypassIsolate(final ClassExpression child) {
		final Taxonomy bit = new Taxonomy();
		
		final Set<ClassExpression> parents = parentsOf(child).stream().collect(Collectors.toSet());
		final Set<ClassExpression> grandparents = parents.stream().flatMap(p -> parentsOf(p).stream()).collect(Collectors.toSet());
		final HashMap<ClassExpression, ClassExpression> replace = new HashMap<ClassExpression, ClassExpression>();
		
		// replacement map from parents to isolated parents
		parents.forEach(parent -> replace.put(parent,  parent.difference(child)));
		
		// substitute isolated parents for parents
		vertexSet().stream().filter(v -> !parents.contains(v)).forEach(bit::addVertex);
		replace.values().forEach(bit::addVertex);
		
		// substitute isolated parents edges for parents edges
		edgeSet().forEach(e -> {
			final ClassExpression s = getEdgeSource(e);
			final ClassExpression t = getEdgeTarget(e);
			if (parents.contains(s)) {
				if (t != child) bit.addEdge(replace.get(s), t);
			} else if (parents.contains(t)) {
				bit.addEdge(s, replace.get(t));
			} else {
				bit.addEdge(s, t);
			}
		});
		
		// move child up to grandparents
		grandparents.forEach(gp -> bit.addEdge(gp,  child));
		
		return bit;
	}
	
	/**
	 * Eliminate redundant edges above child.
	 * 
	 * @param child a class expression vertex
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
	 * @param child  a class expression vertex
	 * @param parent a class expression vertex
	 * @return A new directed graph obtained by replacing the given parent vertex with the difference between parent and child.
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
	 * @param child   a class expression vertex
	 * @param parents a set of class expression vertixes
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
	 		return bypassIsolate(child).reduceChild(child).treeify();
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
	 * @param axiomType The axiom type to generate closure for
	 * @return A set of axioms
	 */
	public Set<Axiom> generateClosureAxioms(Axiom.AxiomType axiomType) throws UnconnectedTaxonomyException, InvalidTreeException {

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
							(c instanceof ClassExpression.Unitary) ?
									new DisjointUnionAxiom((ClassExpression.Unitary) c, s) :
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
		return new ConnectivityInspector<>(this).isGraphConnected();
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

	/**
	 * UnconnectedTaxonomyException thrown if a graph fails the requirement of having a connected topology.
	 */
	public static class UnconnectedTaxonomyException extends RuntimeException {

		/**
		 * UnconnectedTaxonomyException constructor
		 * @param s explanation
		 */
		public UnconnectedTaxonomyException(final String s) {
			super(s);
		}
	}

	/**
	 * InvalidTreeException thrown if a graph fails the requirement of having the topology of a tree.
	 */
	public static class InvalidTreeException extends RuntimeException {

		/**
		 * InvalidTreeException constructor
		 * @param s explanation
		 */
		public InvalidTreeException(final String s) {
			super(s);
		}
	}

	/**
	 * A taxonomy edge from a super class (source) to a subclass (target)
	 */
	public static class TaxonomyEdge extends DefaultEdge {
		
		/**
		 * Creates a new TaxonomyEdge object
		 */
		public TaxonomyEdge() {
		}
		
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
