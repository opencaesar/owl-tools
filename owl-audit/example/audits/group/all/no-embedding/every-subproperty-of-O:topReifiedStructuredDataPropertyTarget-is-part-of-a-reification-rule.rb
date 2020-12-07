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

name 'every subproperty of O:topReifiedStructuredDataPropertyTarget is part of a reification rule'

query %Q{

  <%= @namespace_defs %>

  select distinct ?trg_prop ?audit_case_ok

  <%= @from_named_clauses_by_group['named'] %>
  <%= @from_clauses_by_group['named'] %>
  <%= @from_clauses_by_group_by_type['named']['PropertyEntailments'] %>
  
  where {

    graph ?graph {
      ?trg_prop rdf:type owl:ObjectProperty .
    }
      
    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedStructuredDataPropertyTarget")) as ?top)
    ?trg_prop rdfs:subPropertyOf ?top .
    
    optional {
      ?rule swrl:body [ rdf:rest? [ rdf:first [ swrl:propertyPredicate ?trg_prop ] ] ] .
    }

    bind(bound(?rule) as ?audit_case_ok)

    filter (
         ?trg_prop != owl:bottomObjectProperty
      && ?trg_prop != ?top
    )

  }
}

case_name { |r| r.trg_prop.to_qname(@namespace_by_prefix) }