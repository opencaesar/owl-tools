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
	 *  Returns  change to an ontology resulting from adding an axiom
	 *  
	 * @param o an ontology
	 * @param a an axiom
	 * @return ChangedApplied
	 */
	public ChangeApplied addAxiom(OWLOntology o, OWLAxiom a) {
		return manager.addAxiom(o, a);
	}

	/**
	 * Returns the OWL thing class
	 * 
	 * @return OwlClass
	 */
	public OWLClass getOWLThing() {
		return factory.getOWLThing();
	}

	/**
	 * Returns the OWL nothing class
	 * 
	 * @return OwlClass
	 */
	public OWLClass getOWLNothing() {
		return factory.getOWLNothing();
	}

	/**
	 *  Returns a class for the given IRI
	 *  
	 * @param iri An IRI
	 * @return OwlClass.
	 */
	public OWLClass getOWLClass(IRI iri) {
		return factory.getOWLClass(iri);
	}

	/**
	 * Returns a data property for the given IRI
	 * 
	 * @param iri An IRI
	 * @return OWLDataProperty.
	 */
	public OWLDataProperty getOWLDataProperty(IRI iri) {
		return factory.getOWLDataProperty(iri);
	}

	/**
	 * Returns an object property for the given IRI
	 * 
	 * @param iri An IRI
	 * @return OWLObjectProperty.
	 */
	public OWLObjectProperty getOWLObjectProperty(IRI iri) {
		return factory.getOWLObjectProperty(iri);
	}

	/**
	 * Returns a named individual for the given IRI
	 * 
	 * @param iri An IRI
	 * @return OWLNamedIndividual.
	 */
	public OWLNamedIndividual getOWLNamedIndividual(IRI iri) { return factory.getOWLNamedIndividual(iri); }

	/**
	 * Returns an inverse object property expression involving the given object property
	 * 
	 * @param property an object property
	 * @return OWLObjectInverseOf
	 */
	public OWLObjectInverseOf getOWLObjectInverseOf(OWLObjectProperty property) {
		return factory.getOWLObjectInverseOf(property);
	}

	/**
	 * Returns a class expression assertion axion involving the given class expression and individual.
	 * 
	 * @param ce a class expression
	 * @param i an individual
	 * @return OWLClassAssertionAxiom
	 */
	public OWLClassAssertionAxiom getOWLClassAssertionAxiom(OWLClassExpression ce, OWLIndividual i) {
		return factory.getOWLClassAssertionAxiom(ce, i);
	}

	/**
	 * Returns an object complement for the given class expression
	 * 
	 * @param e a class expression
	 * @return OWLObjectComplementOf
	 */
	public OWLObjectComplementOf getOWLObjectComplementOf(OWLClassExpression e) {
		return factory.getOWLObjectComplementOf(e);
	}

	/**
	 * Returns an object intersection for the given set of class expressions
	 * 
	 * @param operands a stream of class expressions.
	 * @return OWLObjectIntersectionOf
	 */
	public OWLObjectIntersectionOf getOWLObjectIntersectionOf(Stream<OWLClassExpression> operands) {
		return factory.getOWLObjectIntersectionOf(operands);
	}

	/**
	 * Returns an object union for the given set of class expressions.
	 * 
	 * @param operands a stream of class expressions.
	 * @return OWLObjectUnionOf
	 */
	public OWLObjectUnionOf getOWLObjectUnionOf(Stream<OWLClassExpression> operands) {
		return factory.getOWLObjectUnionOf(operands);
	}

	/**
	 * Returns a disjoint class axiom for the given set of class expressions.
	 * 
	 * @param operands a stream of class expressions.
	 * @return OWLDisjointClassesAxiom
	 */
	public OWLDisjointClassesAxiom getOWLDisjointClassesAxiom (Stream<OWLClassExpression> operands) {
		return factory.getOWLDisjointClassesAxiom(operands);
	}

	/**
	 * Returns an equivalent class axiom for the given set of class expressions.
	 * 
	 * @param operands a stream of class expressions.
	 * @return OWLEquivalentClassesAxiom
	 */
	public OWLEquivalentClassesAxiom getOWLEquivalentClassesAxiom (Stream<OWLClassExpression> operands) {
		return factory.getOWLEquivalentClassesAxiom(operands);
	}

	/**
	 * Returns a disjoint union axiom asserting the given class is the disjoint union of the given class expressions.
	 * 
	 * @param c a class.
	 * @param operands a stream of class expressions.
	 * @return OWLDisjointUnionAxiom
	 */
	public OWLDisjointUnionAxiom getOWLDisjointUnionAxiom (OWLClass c, Stream<OWLClassExpression> operands) {
		return factory.getOWLDisjointUnionAxiom(c, operands);
	}

	/**
	 * Returns a data max cardinality for the given property expression and cardinality bound.
	 * 
	 * @param cardinality a cardinality bound.
	 * @param pe a data property expression.
	 * @return OWLDataMaxCardinality
	 */
	public OWLDataMaxCardinality getOWLDataMaxCardinality(int cardinality, OWLDataPropertyExpression pe) {
		return factory.getOWLDataMaxCardinality(cardinality, pe);
	}

	/**
	 * Returns an object max cardinality for the given property expression and cardinality bound.
	 * 
	 * @param cardinality a cardinality bound.
	 * @param pe a object property expression.
	 * @return OWLObjectMaxCardinality
	 */
	public OWLObjectMaxCardinality getOWLObjectMaxCardinality(int cardinality, OWLObjectPropertyExpression pe) {
		return factory.getOWLObjectMaxCardinality(cardinality, pe);
	}
}