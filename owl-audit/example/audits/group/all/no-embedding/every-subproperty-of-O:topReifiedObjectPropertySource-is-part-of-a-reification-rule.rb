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

name 'every subproperty of O:topReifiedObjectPropertySource is part of a reification rule'

query %Q{

  <%= @namespace_defs %>

  select distinct ?src_prop ?audit_case_ok

  <%= @from_named_clauses_by_group['named'] %>
  <%= @from_clauses_by_group['named'] %>
  <%= @from_clauses_by_group_by_type['named']['PropertyEntailments'] %>
  
  where {

    graph ?graph {
      ?src_prop rdf:type owl:ObjectProperty .
    }

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedObjectPropertySource")) as ?top)
    ?src_prop rdfs:subPropertyOf ?top .
    
    optional {
      ?rule swrl:body [ rdf:rest? [ rdf:first [ swrl:propertyPredicate ?src_prop ] ] ] .
    }

    bind(bound(?rule) as ?audit_case_ok)

    filter (
         ?src_prop != owl:bottomObjectProperty
      && ?src_prop != ?top
    )

  }
}

case_name { |r| r.src_prop.to_qname(@namespace_by_prefix) }