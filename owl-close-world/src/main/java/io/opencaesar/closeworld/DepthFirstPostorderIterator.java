package io.opencaesar.closeworld;

import java.util.ArrayDeque;
import org.jgrapht.traverse.DepthFirstIterator;

/**
 * An iterator for depth-first post-order traversal of a Taxonomy. Delegates to a DepthFirstIterator
 * with a callback method to push vertices onto a shared queue as they are finished.
 *
 * @author sjenkins
 *
 */
public class DepthFirstPostorderIterator {

	private static ArrayDeque<ClassExpression> queue;
	private static DepthFirstIteratorWithFinish dfswf;
	
	/**
	 * Constructor
	 * @param t Taxonomy
	 */
	public DepthFirstPostorderIterator(Taxonomy t) {
		queue = new ArrayDeque<ClassExpression>();
		dfswf = new DepthFirstIteratorWithFinish(t);
	}
	
	public boolean hasNext() {
		if (!queue.isEmpty()) return true;
		while (dfswf.hasNext()) {
			dfswf.next();
			if (!queue.isEmpty()) return true;
		}
		return false;
	}
	
	public ClassExpression next() {
		return queue.removeFirst();
	}
	
	/**
	 * A DepthFirstIterator that overrides finishVertex()) to queue finished vertices.
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
