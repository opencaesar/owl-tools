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

name 'every datatype property in ontology O is a subproperty of O:topDataProperty'

query %q{

  <%= @namespace_defs %>

  select distinct ?graph ?property ?topDataProperty ?audit_case_ok
  
  <%= @from_named_clauses_by_group['named'] %>
  <%= @from_named_clauses_by_group_by_type['named']['PropertyEntailments'] %>
  
  where {
    graph ?graph {
      ?property rdf:type owl:DatatypeProperty .
    }
    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topDataProperty")) as ?topDataProperty)
    bind(exists { graph ?any { ?property rdfs:subPropertyOf ?topDataProperty } } as ?audit_case_ok)
    filter (
         <%= @ontologies_by_group['named'].map { |o| o.to_uriref }.equal_any?('?graph') %>
      && ?property != ?topDataProperty
      && ?property != owl:topDataProperty
      && ?property != owl:bottomDataProperty
    )
  }
  order by ?property
}

case_name { |r| r.property.to_qname(@namespace_by_prefix) }