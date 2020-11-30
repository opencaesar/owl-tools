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

name 'every class embedded in UML'

query %q{

  <%= @namespace_defs %>  select distinct ?klass ?audit_case_ok
  
  <%= @from_clauses_by_group['named'] %>
  <%= @from_named_clauses_by_group['named'] %>
  <%= @from_clauses_by_group['imported'] %>
  <%= @from_clauses_by_group_by_type['named']['ClassEntailments'] %>
  <%= @from_clauses_by_group_by_type['imported']['ClassEntailments'] %>
  <%= @from_named_clauses_by_group['omg'] %>

  where {
  
    graph ?graph { ?klass rdf:type owl:Class . }
  
    bind(exists { ?klass rdfs:subClassOf UML:Element } as ?embedded_element)

    bind(exists { ?klass rdfs:subClassOf owl2-mof2-backbone:ReifiedObjectProperty } as ?reified_op)
    bind(exists { ?klass rdfs:subClassOf owl2-mof2-backbone:ReifiedStructuredDataProperty } as ?reified_sdp)
    bind(?reified_op || ?reified_sdp as ?reified)
    bind((?reified || ?embedded_element) as ?audit_case_ok)
  
    filter (
         <%= @ontologies_by_group['named'].map { |o| o.to_uriref }.equal_any?('?graph') %>
      && !regex(str(?klass), "http://imce\\\\.jpl\\\\.nasa\\\\.gov/backbone/.*#")
      && ?klass != owl:Thing
      && ?klass != owl:Nothing
    )
  }
  order by ?klass
}

case_name { |r| r.klass.to_qname(@namespace_by_prefix) }
