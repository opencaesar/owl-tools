package io.opencaesar.closeworld;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;

import java.util.stream.Stream;

public class OwlApi {
	
	protected final OWLOntologyManager manager;
	protected final OWLDataFactory factory;
	
	public OwlApi(OWLOntologyManager manager) {
		this.manager = manager;
		this.factory = manager.getOWLDataFactory();
	}

	public ChangeApplied addAxiom(OWLOntology o, OWLAxiom a) {
		return manager.addAxiom(o, a);
	}

	public OWLClass getOWLThing() {
		return factory.getOWLThing();
	}

	public OWLClass getOWLNothing() {
		return factory.getOWLNothing();
	}
	
	public OWLClass getOWLClass(IRI iri) {
		return factory.getOWLClass(iri);
	}
	
	public OWLObjectComplementOf getOWLObjectComplementOf(OWLClassExpression e) {
		return factory.getOWLObjectComplementOf(e);
	}
	
	public OWLObjectIntersectionOf getOWLObjectIntersectionOf(Stream<OWLClassExpression> operands) {
		return factory.getOWLObjectIntersectionOf(operands);
	}
	
	public OWLObjectUnionOf getOWLObjectUnionOf(Stream<OWLClassExpression> operands) {
		return factory.getOWLObjectUnionOf(operands);
	}

	public OWLDisjointClassesAxiom getOWLDisjointClassesAxiom (Stream<OWLClassExpression> operands) {
		return factory.getOWLDisjointClassesAxiom(operands);
	}

	public OWLEquivalentClassesAxiom getOWLEquivalentClassesAxiom (Stream<OWLClassExpression> operands) {
		return factory.getOWLEquivalentClassesAxiom(operands);
	}

	public OWLDisjointUnionAxiom getOWLDisjointUnionAxiom (OWLClass c, Stream<OWLClassExpression> operands) {
		return factory.getOWLDisjointUnionAxiom(c, operands);
	}

}