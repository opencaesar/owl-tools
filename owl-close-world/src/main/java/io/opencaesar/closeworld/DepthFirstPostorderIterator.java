package io.opencaesar.closeworld;

import java.util.ArrayDeque;
import java.util.Iterator;

import org.jgrapht.traverse.DepthFirstIterator;

/**
 * An iterator for depth-first post-order traversal of a Taxonomy. Delegates to a DepthFirstIterator
 * with a callback method to push vertices onto a shared queue as they are finished.
 *
 * @author sjenkins
 *
 */
public class DepthFirstPostorderIterator implements Iterator<ClassExpression> {

	private static ArrayDeque<ClassExpression> queue;
	private static DepthFirstIteratorWithFinish dfswf;
	
	/**
	 * Constructs a new DepthFirstPostorderIterator
	 * 
	 * @param t Taxonomy
	 */
	public DepthFirstPostorderIterator(Taxonomy t) {
		queue = new ArrayDeque<ClassExpression>();
		dfswf = new DepthFirstIteratorWithFinish(t);
	}
	
	@Override
	public boolean hasNext() {
		if (!queue.isEmpty()) return true;
		while (dfswf.hasNext()) {
			dfswf.next();
			if (!queue.isEmpty()) return true;
		}
		return false;
	}

	@Override
	public ClassExpression next() {
		return queue.removeFirst();
	}
	
	/**
	 * A DepthFirstIterator that overrides finishVertex() to queue finished vertices.
	 * 
	 * @author sjenkins
	 *
	 */
	private class DepthFirstIteratorWithFinish extends DepthFirstIterator<ClassExpression, Taxonomy.TaxonomyEdge> {
		
		public DepthFirstIteratorWithFinish(Taxonomy t) {
			super(t);
		}
		
		@Override
		protected void finishVertex(ClassExpression vertex) {
			queue.addLast(vertex);
		}
	}
	
}
