package io.opencaesar.closeworld;

import io.opencaesar.closeworld.Axiom.SubClassOfAxiom;
import io.opencaesar.closeworld.Axiom.ClassExpressionSetAxiom;
import io.opencaesar.closeworld.Axiom.ClassExpressionSetAxiom.DisjointClassesAxiom;
import io.opencaesar.closeworld.Axiom.ClassExpressionSetAxiom.DisjointUnionAxiom;
import io.opencaesar.closeworld.Axiom.ClassExpressionSetAxiom.EquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Utility class for converting internal axioms to OWL API axioms.
 */
public class OwlAxiom {

	/**
	 * Creates a new OwlAxiom object
	 */
	public OwlAxiom() {
	}
	
    /**
     * Convert axioms from the internal representation to the OWL API representation
     * @param axiom an internal axiom
     * @param api the utility for operating with the OWL API
     * @return An OWL API axiom representation corresponding to the internal axiom.
     */
    public static OWLAxiom toOwlAxiom(final Axiom axiom, final OwlApi api) {

        if (axiom instanceof ClassExpressionSetAxiom) {
            final ClassExpressionSetAxiom cesa = (ClassExpressionSetAxiom) axiom;
            final Stream<OWLClassExpression> oces = cesa.getSet().stream().map(
                    ce -> OwlClassExpression.toOwlClassExpression(ce, api)
            );
            if (axiom instanceof DisjointClassesAxiom)
                return api.getOWLDisjointClassesAxiom(oces);
            else if (axiom instanceof EquivalentClassesAxiom)
                return api.getOWLEquivalentClassesAxiom(oces);
            else if (axiom instanceof DisjointUnionAxiom) {
                final DisjointUnionAxiom djua = (DisjointUnionAxiom) axiom;
                final OWLClass c = OwlClassExpression.toOwlClassExpression(djua.getC(), api);
                return api.getOWLDisjointUnionAxiom(c, oces);
            } else {
                throw new IllegalArgumentException("Unhandled ClassExpressionSetAxiom type: " + Arrays.asList(axiom));
            }
        } else if (axiom instanceof SubClassOfAxiom) {
        	final OWLClassExpression subclass = OwlClassExpression.toOwlClassExpression(((SubClassOfAxiom) axiom).getSubclass(), api);
        	final OWLClassExpression superclass = OwlClassExpression.toOwlClassExpression(((SubClassOfAxiom) axiom).getSuperclass(), api);
        	return api.getOWLSubClassOfAxiom(subclass, superclass);
        } else {
            throw new IllegalArgumentException("Unhandled ClassExpressionSetAxiom type: " + Arrays.asList(axiom));
        }
    }
}
