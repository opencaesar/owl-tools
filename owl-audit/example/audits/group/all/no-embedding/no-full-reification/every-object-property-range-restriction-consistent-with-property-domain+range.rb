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

name 'every object property range restriction consistent with property domain/range'

query %q{

 <%= @namespace_defs %>

  select distinct ?source ?property ?rest_type ?rest_range ?domain_ok ?range_ok

  <%= @from_clauses_by_group['named'] %>
  <%= @from_clauses_by_group['imported'] %>
  <%= @from_named_clauses_by_group['named'] %>
  <%= @from_clauses_by_group_by_type['named']['ClassEntailments'] %>
  <%= @from_clauses_by_group_by_type['imported']['ClassEntailments'] %>
  
  where {
    
    ?property rdf:type owl:ObjectProperty .
    ?property rdfs:domain ?domain .
    ?property rdfs:range ?range .
    graph ?g2 {
      ?rest rdf:type owl:Restriction .
      ?rest owl:onProperty ?property .
      ?rest ?rest_type ?rest_range .
      ?source rdfs:subClassOf ?rest .
    }   

    bind(exists { ?source rdfs:subClassOf ?domain } as ?domain_ok)
    bind(exists { ?rest_range rdfs:subClassOf ?range } as ?range_ok)
      
    filter (
         <%= @ontologies_by_group['named'].map { |o| o.to_uriref }.equal_any?('?g2') %>
      && (?rest_type = owl:allValuesFrom || ?rest_type = owl:someValuesFrom)
    )
  }
  order by ?source ?rest ?property
}

prologue {
  @rules = {
    :domain_ok => 'invalid domain',
    :range_ok => 'invalid range'
  }
}

predicate do |r|
  msg = @rules.inject([]) do |memo, kv|
    method, msg = *kv
    memo << msg + '.' unless r.send(method).true?
    memo
  end.join(' ')
  msg.empty? ? [true, nil] : [false, msg]
end

case_name do |r|
  "#{r.source.to_qname(@namespace_by_prefix)} #{r.property.to_qname(@namespace_by_prefix)} #{r.rest_type.to_qname(@namespace_by_prefix)} #{r.rest_range.to_qname(@namespace_by_prefix)}"
end