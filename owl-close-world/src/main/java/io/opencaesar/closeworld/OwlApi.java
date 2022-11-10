package io.opencaesar.closeworld;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;

import java.util.stream.Stream;

/**
 * A utility class for performing operations with the OWL API.
 */
public class OwlApi {

	/**
	 * The ontology manager needed for performing operations with the OWL API.
	 */
	protected final OWLOntologyManager manager;

	/**
	 * The data factoring needed for performing operations with the OWL API.
	 */
	protected final OWLDataFactory factory;

	/**
	 * Constructor the OwlApi
	 * @param manager The ontology manager needed for performing operations with the OWL API.
	 */
	public OwlApi(OWLOntologyManager manager) {
		this.manager = manager;
		this.factory = manager.getOWLDataFactory();
	}

	/**
	 * @param o an ontology.
	 * @param a an axiom
	 * @return A change to an ontology resulting from adding an axiom.
	 */
	public ChangeApplied addAxiom(OWLOntology o, OWLAxiom a) {
		return manager.addAxiom(o, a);
	}

	/**
	 * @return The OWL thing class
	 */
	public OWLClass getOWLThing() {
		return factory.getOWLThing();
	}

	/**
	 * @return The OWL nothing class
	 */
	public OWLClass getOWLNothing() {
		return factory.getOWLNothing();
	}

	/**
	 * @param iri An IRI
	 * @return A class IRI for the given IRI.
	 */
	public OWLClass getOWLClass(IRI iri) {
		return factory.getOWLClass(iri);
	}

	/**
	 * @param iri An IRI
	 * @return A data property IRI for the given IRI.
	 */
	public OWLDataProperty getOWLDataProperty(IRI iri) {
		return factory.getOWLDataProperty(iri);
	}

	/**
	 * @param iri An IRI
	 * @return A object property IRI for the given IRI.
	 */
	public OWLObjectProperty getOWLObjectProperty(IRI iri) {
		return factory.getOWLObjectProperty(iri);
	}

	/**
	 * @param iri An IRI
	 * @return A named individual IRI for the given IRI.
	 */
	public OWLNamedIndividual getOWLNamedIndividual(IRI iri) { return factory.getOWLNamedIndividual(iri); }

	/**
	 * @param property an object property
	 * @return An inverse object property expression involving the given object property
	 */
	public OWLObjectInverseOf getOWLObjectInverseOf(OWLObjectProperty property) {
		return factory.getOWLObjectInverseOf(property);
	}

	/**
	 * @param ce a class expression
	 * @param i an individual
	 * @return A class expression assertion axion involving the given class expression and individual.
	 */
	public OWLClassAssertionAxiom getOWLClassAssertionAxiom(OWLClassExpression ce, OWLIndividual i) {
		return factory.getOWLClassAssertionAxiom(ce, i);
	}

	/**
	 * @param e a class expression
	 * @return An object complement for the given class expression.
	 */
	public OWLObjectComplementOf getOWLObjectComplementOf(OWLClassExpression e) {
		return factory.getOWLObjectComplementOf(e);
	}

	/**
	 * @param operands a stream of class expressions.
	 * @return An object intersection for the given set of class expressions.
	 */
	public OWLObjectIntersectionOf getOWLObjectIntersectionOf(Stream<OWLClassExpression> operands) {
		return factory.getOWLObjectIntersectionOf(operands);
	}

	/**
	 * @param operands a stream of class expressions.
	 * @return An object union for the given set of class expressions.
	 */
	public OWLObjectUnionOf getOWLObjectUnionOf(Stream<OWLClassExpression> operands) {
		return factory.getOWLObjectUnionOf(operands);
	}

	/**
	 * @param operands a stream of class expressions.
	 * @return A disjoint class axiom for the given set of class expressions.
	 */
	public OWLDisjointClassesAxiom getOWLDisjointClassesAxiom (Stream<OWLClassExpression> operands) {
		return factory.getOWLDisjointClassesAxiom(operands);
	}

	/**
	 * @param operands a stream of class expressions.
	 * @return An equivalent class axiom for the given set of class expressions.
	 */
	public OWLEquivalentClassesAxiom getOWLEquivalentClassesAxiom (Stream<OWLClassExpression> operands) {
		return factory.getOWLEquivalentClassesAxiom(operands);
	}

	/**
	 * @param c a class.
	 * @param operands a stream of class expressions.
	 * @return A disjoint union axiom asserting the given class is the disjoint union of the given class expressions.
	 */
	public OWLDisjointUnionAxiom getOWLDisjointUnionAxiom (OWLClass c, Stream<OWLClassExpression> operands) {
		return factory.getOWLDisjointUnionAxiom(c, operands);
	}

	/**
	 * @param cardinality a cardinality bound.
	 * @param pe a data property expression.
	 * @return An data max cardinality for the given property expression and cardinality bound.
	 */
	public OWLDataMaxCardinality getOWLDataMaxCardinality(int cardinality, OWLDataPropertyExpression pe) {
		return factory.getOWLDataMaxCardinality(cardinality, pe);
	}

	/**
	 * @param cardinality a cardinality bound.
	 * @param pe a object property expression.
	 * @return An object max cardinality for the given property expression and cardinality bound.
	 */
	public OWLObjectMaxCardinality getOWLObjectMaxCardinality(int cardinality, OWLObjectPropertyExpression pe) {
		return factory.getOWLObjectMaxCardinality(cardinality, pe);
	}
}