#--
#
#    $HeadURL$
#
#    $LastChangedRevision$
#    $LastChangedDate$
#
#    $LastChangedBy$
#
#    Copyright (c) 2008-2014 California Institute of Technology.
#    All rights reserved.
#
#++

name 'every object property in ontology O is an indirect subproperty of O-backbone:topObjectProperty'

query %q{

  <%= @namespace_defs %>

  select distinct ?graph ?property ?topObjectProperty ?audit_case_ok
  
  <%= @from_named_clauses_by_group['named'] %>
  <%= @from_named_clauses_by_group_by_type['named']['PropertyEntailments'] %>
  
  where {
    graph ?graph {
      ?property rdf:type owl:ObjectProperty .
    }
    bind(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://")) as ?backbone_iri)
    bind(iri(concat(?backbone_iri, "#topObjectProperty")) as ?topObjectProperty)
    bind(iri(concat(?backbone_iri, "#topReifiedObjectProperty")) as ?topReifiedObjectProperty)
    bind(iri(concat(?backbone_iri, "#topReifiedObjectPropertySource")) as ?topReifiedObjectPropertySource)
    bind(iri(concat(?backbone_iri, "#topReifiedObjectPropertyTarget")) as ?topReifiedObjectPropertyTarget)
    bind(iri(concat(?backbone_iri, "#topReifiedStructuredDataProperty")) as ?topReifiedStructuredDataProperty)
    bind(iri(concat(?backbone_iri, "#topReifiedStructuredDataPropertySource")) as ?topReifiedStructuredDataPropertySource)
    bind(iri(concat(?backbone_iri, "#topReifiedStructuredDataPropertyTarget")) as ?topReifiedStructuredDataPropertyTarget)
    bind(iri(concat(?backbone_iri, "#topUnreifiedObjectProperty")) as ?topUnreifiedObjectProperty)
    bind(exists { graph ?any { { ?property rdfs:subPropertyOf ?topReifiedObjectProperty } union
                               { ?property rdfs:subPropertyOf ?topReifiedObjectPropertySource } union
                               { ?property rdfs:subPropertyOf ?topReifiedObjectPropertyTarget } union
                               { ?property rdfs:subPropertyOf ?topReifiedStructuredDataProperty } union
                               { ?property rdfs:subPropertyOf ?topReifiedStructuredDataPropertySource } union
                               { ?property rdfs:subPropertyOf ?topReifiedStructuredDataPropertyTarget } union
                               { ?property rdfs:subPropertyOf ?topUnreifiedObjectProperty }
                  } } as ?audit_case_ok)
    filter (
         <%= @ontologies_by_group['named'].map { |o| o.to_uriref }.equal_any?('?graph') %>
      && ?property != ?topObjectProperty
      && ?property != ?topReifiedObjectProperty
      && ?property != ?topReifiedObjectPropertySource
      && ?property != ?topReifiedObjectPropertyTarget
      && ?property != ?topReifiedStructuredDataProperty
      && ?property != ?topReifiedStructuredDataPropertySource
      && ?property != ?topReifiedStructuredDataPropertyTarget
      && ?property != ?topUnreifiedObjectProperty
      && ?property != owl:topObjectProperty
      && ?property != owl:bottomObjectProperty
    )
  }
  order by ?property
}
    
case_name { |r| "#{r.property.to_qname(@namespace_by_prefix)} subproperty of #{r.topObjectProperty.to_qname(@namespace_by_prefix)}" }