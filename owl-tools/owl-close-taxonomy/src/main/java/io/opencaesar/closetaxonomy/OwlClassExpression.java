package io.opencaesar.closetaxonomy;

import io.opencaesar.closetaxonomy.ClassExpression.*;
import org.semanticweb.owlapi.model.*;

import java.util.Arrays;

public class OwlClassExpression {
	
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
		} else if (c instanceof Singleton) {
			return toOwlClassExpression((Singleton) c, a);
		} else if (c instanceof Universal) {
			return toOwlClassExpression((Universal) c, a);
		} else {
			throw new IllegalArgumentException("Unhandled parameter types: " + Arrays.asList(c, a));
		}
	}

	protected static OWLClass toOwlClassExpression(final Universal u, final OwlApi a) {
		return a.getOWLThing();
	}

	protected static OWLClass toOwlClassExpression(final Empty e, final OwlApi a) {
		return a.getOWLNothing();
	}

	protected static OWLClass toOwlClassExpression(final Singleton s, final OwlApi a) {
		return a.getOWLClass(IRI.create((String) s.encapsulatedClass));
	}

	protected static OWLObjectComplementOf toOwlClassExpression(final Complement c, final OwlApi a) {
		return a.getOWLObjectComplementOf(toOwlClassExpression(c.e, a));
	}

	protected static OWLClassExpression toOwlClassExpression(final Difference d, final OwlApi a) {
		return toOwlClassExpression(d.a.intersection(d.b.complement()), a);
	}

	protected static OWLObjectIntersectionOf toOwlClassExpression(final Intersection i, final OwlApi a) {
		return a.getOWLObjectIntersectionOf(i.s.stream().map(it -> toOwlClassExpression(it, a)));
	}

	protected static OWLObjectUnionOf toOwlClassExpression(final Union u, final OwlApi a) {
		return a.getOWLObjectUnionOf(u.s.stream().map(it -> toOwlClassExpression(it, a)));
	}

}
