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

name 'every ontology declares a top datatype property'

query %q{

  <%= @namespace_defs %>

  select distinct ?ontology ?audit_case_ok

  <%= @from_named_clauses_by_group['named'] %>

  where {
    graph ?graph {
      ?ontology rdf:type owl:Ontology .
    }

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topDataProperty")) as ?tdp)
    bind(exists { graph ?graph { ?tdp rdf:type owl:DatatypeProperty } } as ?audit_case_ok)

    filter ( 
      <%= (@ontologies_by_group['named'] -
        @ontologies_by_group['named-embedding'] -
        @ontologies_by_group['named-view']).map { |o| o.to_uriref }.equal_any?('?graph')
    %>)

  }
  order by ?ontology
}

case_name { |r| r.ontology }
