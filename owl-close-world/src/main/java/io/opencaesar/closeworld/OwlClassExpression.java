package io.opencaesar.closeworld;

import io.opencaesar.closeworld.ClassExpression.*;
import org.semanticweb.owlapi.model.*;

import java.util.Arrays;

/**
 * Utility class for converting internal class expressions to corresponding OWL API class expressions.
 */
public class OwlClassExpression {

	/**
	 * Creates a new OwlClassExpression object
	 */
	public OwlClassExpression() {
	}
	
	/**
	 * Returns an OWL class expression corresponding to the internal expression.
	 * 
	 * @param c an internal class expression
	 * @param a the utility for operating with the OWL API
	 * @return OWLClassExpression
	 */
	public static OWLClassExpression toOwlClassExpression(final ClassExpression c, final OwlApi a) {
		if (c instanceof Complement) {
			return toOwlClassExpression((Complement) c, a);
		} else if (c instanceof Difference) {
			return toOwlClassExpression((Difference) c, a);
		} else if (c instanceof Intersection) {
			return toOwlClassExpression((Intersection) c, a);
		} else if (c instanceof Union) {
			return toOwlClassExpression((Union) c, a);
		} else if (c instanceof Empty) {
			return toOwlClassExpression((Empty) c, a);
		} else if (c instanceof Unitary) {
			return toOwlClassExpression((Unitary) c, a);
		} else if (c instanceof Universal) {
			return toOwlClassExpression((Universal) c, a);
		} else {
			throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.asList(c, a));
		}
	}

	/**
	 * Returns an OWL class expression corresponding to the internal expression.
	 * 
	 * @param u an internal empty class expression
	 * @param a the utility for operating with the OWL API
	 * @return OWLClass
	 */
	protected static OWLClass toOwlClassExpression(final Universal u, final OwlApi a) {
		return a.getOWLThing();
	}

	/**
	 * Returns an OWL class expression corresponding to the internal expression.
	 * 
	 * @param e an internal empty class expression
	 * @param a the utility for operating with the OWL API
	 * @return OWLClass
	 */
	protected static OWLClass toOwlClassExpression(final Empty e, final OwlApi a) {
		return a.getOWLNothing();
	}

	/**
	 * Returns an OWL API class expression corresponding to the internal expression.
	 * 
	 * @param s an internal class expression
	 * @param a the utility for operating with the OWL API
	 * @return OWLClass
	 */
	protected static OWLClass toOwlClassExpression(final Unitary s, final OwlApi a) {
		return a.getOWLClass(IRI.create((String) s.encapsulatedClass));
	}

	/**
	 * Returns an OWL object complement expression corresponding to the internal expression.
	 * 
	 * @param c an internal class complement expression
	 * @param a the utility for operating with the OWL API
	 * @return OWLObjectComplementOf
	 */
	protected static OWLObjectComplementOf toOwlClassExpression(final Complement c, final OwlApi a) {
		return a.getOWLObjectComplementOf(toOwlClassExpression(c.e, a));
	}

	/**
	 * Returns an OWL Class expression corresponding to the internal expression.
	 * 
	 * @param d an internal class difference expression
	 * @param a the utility for operating with the OWL API
	 * @return OWLClassExpression
	 */
	protected static OWLClassExpression toOwlClassExpression(final Difference d, final OwlApi a) {
		return toOwlClassExpression(d.a.intersection(d.b.complement()), a);
	}

	/**
	 * Returns an OWL ObjectIntersectionOf expression corresponding to the internal expression.
	 * 
	 * @param i an internal class intersection expression
	 * @param a the utility for operating with the OWL API
	 * @return OWLObjectIntersectionOf
	 */
	protected static OWLObjectIntersectionOf toOwlClassExpression(final Intersection i, final OwlApi a) {
		return a.getOWLObjectIntersectionOf(i.s.stream().map(it -> toOwlClassExpression(it, a)));
	}

	/**
	 * Returns an OWL ObjectUnionOf expression corresponding to the internal expression.
	 * 
	 * @param u an internal class union expression
	 * @param a the utility for operating with the OWL API
	 * @return OWLObjectUnionOf
	 */
	protected static OWLObjectUnionOf toOwlClassExpression(final Union u, final OwlApi a) {
		return a.getOWLObjectUnionOf(u.s.stream().map(it -> toOwlClassExpression(it, a)));
	}

}
