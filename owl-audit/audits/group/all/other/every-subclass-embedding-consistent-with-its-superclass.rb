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

name 'every subclass embedding consistent with its superclass'

query %q{

  <%= @namespace_defs %>
  
  select distinct ?subclass ?superclass ?sub_map ?super_map ?audit_case_ok
    
  <%= @from_clauses_by_group['named'] %>
  <%= @from_clauses_by_group['imported'] %>

  <%= @from_named_clauses_by_group['named'] %>
  <%= @from_named_clauses_by_group['imported'] %>
    
  <%= @from_named_clauses_by_group_by_type['named']['ClassEntailments'] %>
  <%= @from_named_clauses_by_group_by_type['imported']['ClassEntailments'] %>

  where {
  
    { select distinct ?subclass ?superclass
      where {
        
        graph ?imce_graph_1 { ?subclass rdf:type owl:Class . }
        ?subclass rdfs:subClassOf ?superclass .
        graph ?imce_graph_2 { ?superclass rdf:type owl:Class . }
        
        filter (
           !isblank(?superclass)
        && ?subclass != ?superclass
        && !regex(str(?subclass), "http://imce\\\\.jpl\\\\.nasa\\\\.gov/backbone/.*#")
        && !regex(str(?superlass), "http://imce\\\\.jpl\\\\.nasa\\\\.gov/backbone/.*#")
        && (
                <%= @ontologies_by_group['named'].map { |o| o.to_uriref }.equal_any?('?imce_graph_1') %>
             && <%= @ontologies_by_group['imce'].map { |o| o.to_uriref }.equal_any?('?imce_graph_2') %>
           )
           || (
                <%= @ontologies_by_group['named'].map { |o| o.to_uriref }.equal_any?('?imce_graph_2') %>
             && <%= @ontologies_by_group['imce'].map { |o| o.to_uriref }.equal_any?('?imce_graph_1') %>
           )
        )
      }
    }
    
    ?subclass rdfs:subClassOf ?sub_map .
    graph ?omg_graph_1 { ?sub_map rdf:type owl:Class }
    ?superclass rdfs:subClassOf ?super_map
    graph ?omg_graph_2 { ?super_map rdf:type owl:Class }
      
    bind(exists { graph ?omg_graph_3 { ?sub_map rdfs:subClassOf ?super_map } } as ?audit_case_ok)

    filter (
          <%= @ontologies_by_group['omg'].map { |o| o.to_uriref }.equal_any?('?omg_graph_1') %>
       && <%= (@ontologies_by_group['omg'] + @ontologies_by_group_by_type['omg']['ClassEntailments']).map { |o| o.to_uriref }.equal_any?('?omg_graph_2') %>
     )
  }

  order by ?subclass ?sub_map ?superclass
}

case_name { |r| "#{r.subclass.to_qname(@namespace_by_prefix)} [#{r.sub_map.to_qname(@namespace_by_prefix)}] subclass of #{r.superclass.to_qname(@namespace_by_prefix)} [#{r.super_map.to_qname(@namespace_by_prefix)}]"}
