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

	public OWLDataProperty getOWLDataProperty(IRI iri) {
		return factory.getOWLDataProperty(iri);
	}

	public OWLObjectProperty getOWLObjectProperty(IRI iri) {
		return factory.getOWLObjectProperty(iri);
	}

	public OWLNamedIndividual getOWLNamedIndividual(IRI iri) { return factory.getOWLNamedIndividual(iri); }

	public OWLObjectInverseOf getOWLObjectInverseOf(OWLObjectProperty property) {
		return factory.getOWLObjectInverseOf(property);
	}

	public OWLClassAssertionAxiom getOWLClassAssertionAxiom(OWLClassExpression ce, OWLIndividual i) {
		return factory.getOWLClassAssertionAxiom(ce, i);
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

	public OWLDataMaxCardinality getOWLDataMaxCardinality(int cardinality, OWLDataPropertyExpression pe) {
		return factory.getOWLDataMaxCardinality(cardinality, pe);
	}

	public OWLObjectMaxCardinality getOWLObjectMaxCardinality(int cardinality, OWLObjectPropertyExpression pe) {
		return factory.getOWLObjectMaxCardinality(cardinality, pe);
	}
}