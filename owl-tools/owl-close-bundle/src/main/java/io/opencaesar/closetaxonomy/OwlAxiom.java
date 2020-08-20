package io.opencaesar.closetaxonomy;

import io.opencaesar.closetaxonomy.Axiom.ClassExpressionSetAxiom;
import io.opencaesar.closetaxonomy.Axiom.ClassExpressionSetAxiom.DisjointClassesAxiom;
import io.opencaesar.closetaxonomy.Axiom.ClassExpressionSetAxiom.DisjointUnionAxiom;
import io.opencaesar.closetaxonomy.Axiom.ClassExpressionSetAxiom.EquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.Arrays;
import java.util.stream.Stream;

public class OwlAxiom {

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
        } else {
            throw new IllegalArgumentException("Unhandled ClassExpressionSetAxiom type: " + Arrays.asList(axiom));
        }
    }
}
