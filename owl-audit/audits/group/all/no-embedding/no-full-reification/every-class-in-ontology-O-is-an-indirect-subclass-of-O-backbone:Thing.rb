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

name 'every class in ontology O is an indirect subclass of O-backbone:Thing'

query %Q{

  <%= @namespace_defs %>
  
  select distinct ?graph ?klass ?thing ?audit_case_ok
  
  <%= @from_named_clauses_by_group['named'] %>
  <%= @from_named_clauses_by_group_by_type['named']['ClassEntailments'] %>
  
  where {
    graph ?graph {
      ?klass rdf:type owl:Class .
    }
    bind(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://")) as ?backbone_iri)
    bind(iri(concat(?backbone_iri, "#Thing")) as ?thing)
    bind(iri(concat(?backbone_iri, "#Aspect")) as ?aspect)
    bind(iri(concat(?backbone_iri, "#Entity")) as ?entity)
    bind(iri(concat(?backbone_iri, "#ReifiedObjectProperty")) as ?reifiedObjectProperty)
    bind(iri(concat(?backbone_iri, "#ReifiedStructuredDataProperty")) as ?reifiedStructuredDataProperty)
    bind(iri(concat(?backbone_iri, "#StructuredDatatype")) as ?structuredDatatype)
    bind(exists { graph ?any { { ?klass rdfs:subClassOf ?aspect } union
                               { ?klass rdfs:subClassOf ?entity } union
                               { ?klass rdfs:subClassOf ?reifiedObjectProperty } union
                               { ?klass rdfs:subClassOf ?reifiedStructuredDataProperty } union
                               { ?klass rdfs:subClassOf ?structuredDatatype }
                } } as ?audit_case_ok)
    filter (
         <%= @ontologies_by_group['named'].map { |o| o.to_uriref }.equal_any?('?graph') %>
      && ?klass != ?thing
      && ?klass != ?aspect
      && ?klass != ?entity
      && ?klass != ?reifiedObjectProperty
      && ?klass != ?reifiedStructuredDataProperty
      && ?klass != ?structuredDatatype
      && ?klass != owl:Thing
      && ?klass != owl:Nothing
    )
  }
  order by ?klass
}

case_name { |r| "#{r.klass.to_qname(@namespace_by_prefix)} subclass of #{r.thing.to_qname(@namespace_by_prefix)}" }