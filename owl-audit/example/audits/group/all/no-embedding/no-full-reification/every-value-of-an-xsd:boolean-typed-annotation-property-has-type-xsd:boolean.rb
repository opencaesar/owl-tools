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

name 'every value of an xsd:boolean-typed annotation property has type xsd:boolean'

query %q{

  <%= @namespace_defs %>

  select distinct ?entity ?property ?datatype ?audit_case_ok

  <%= @from_clauses_by_group['named'] %>
  <%= @from_named_clauses_by_group['named'] %>
  <%= @from_clauses_by_group['imported'] %>
  
  where {

    graph ?graph { ?entity ?property ?value }
    ?property rdf:type owl:AnnotationProperty .
    ?property rdfs:range xsd:boolean .

    bind(datatype(?value) as ?datatype)
    bind(?datatype = xsd:boolean as ?audit_case_ok)
    
    filter(<%= @ontologies_by_group['named'].map { |o| o.to_uriref }.equal_any?('?graph') %>)
  }
  order by ?entity ?property
}
      
case_name { |r| "#{r.entity.to_qname(@namespace_by_prefix)} #{r.property.to_qname(@namespace_by_prefix)}" }