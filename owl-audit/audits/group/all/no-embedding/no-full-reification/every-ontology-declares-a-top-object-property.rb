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

name 'every ontology declares a top object property'

query %q{

  <%= @namespace_defs %>

  select distinct ?ontology ?top_exists 
    ?toprop_exists ?toprop_subprop
    ?toprops_exists ?toprops_subprop
    ?topropt_exists ?topropt_subprop
    ?topuop_exists ?topuop_subprop
    ?toprsdp_exists ?toprsdp_subprop
    ?toprsdps_exists ?toprsdps_subprop
    ?toprsdpt_exists ?toprsdpt_subprop

  <%= @from_named_clauses_by_group['named'] %>

  where {
    graph ?graph {
      ?ontology rdf:type owl:Ontology .
    }

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topObjectProperty")) as ?top)
    bind(exists { graph ?graph { ?top rdf:type owl:ObjectProperty } } as ?top_exists)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedObjectProperty")) as ?toprop)
    bind(exists { graph ?graph { ?toprop rdf:type owl:ObjectProperty } } as ?toprop_exists)
    bind(?toprop_exists && exists { graph ?graph { ?toprop rdfs:subPropertyOf ?top } } as ?toprop_subprop)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedObjectPropertySource")) as ?toprops)
    bind(exists { graph ?graph { ?toprops rdf:type owl:ObjectProperty } } as ?toprops_exists)
    bind(?toprops_exists && exists { graph ?graph { ?toprops rdfs:subPropertyOf ?top } } as ?toprops_subprop)
    
    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedObjectPropertyTarget")) as ?topropt)
    bind(exists { graph ?graph { ?topropt rdf:type owl:ObjectProperty } } as ?topropt_exists)
    bind(?topropt_exists && exists { graph ?graph { ?topropt rdfs:subPropertyOf ?top } } as ?topropt_subprop)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topUnreifiedObjectProperty")) as ?topuop)
    bind(exists { graph ?graph { ?topuop rdf:type owl:ObjectProperty } } as ?topuop_exists)

    bind(?topuop_exists && exists { graph ?graph { ?topuop rdfs:subPropertyOf ?top } } as ?topuop_subprop)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedStructuredDataProperty")) as ?toprsdp)
    bind(exists { graph ?graph { ?toprsdp rdf:type owl:ObjectProperty } } as ?toprsdp_exists)
    bind(?toprsdp_exists && exists { graph ?graph { ?toprsdp rdfs:subPropertyOf ?top } } as ?toprsdp_subprop)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedStructuredDataPropertySource")) as ?toprsdps)
    bind(exists { graph ?graph { ?toprsdps rdf:type owl:ObjectProperty } } as ?toprsdps_exists)
    bind(?toprsdps_exists && exists { graph ?graph { ?toprsdps rdfs:subPropertyOf ?top } } as ?toprsdps_subprop)
    
    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedStructuredDataPropertyTarget")) as ?toprsdpt)
    bind(exists { graph ?graph { ?toprsdpt rdf:type owl:ObjectProperty } } as ?toprsdpt_exists)
    bind(?toprsdpt_exists && exists { graph ?graph { ?toprsdpt rdfs:subPropertyOf ?top } } as ?toprsdpt_subprop)

    filter (
      <%= (@ontologies_by_group['named']  - @ontologies_by_group['named-embedding'] - @ontologies_by_group['named-view']).map { |o| o.to_uriref }.equal_any?('?graph')
    %>)
        
  }
  order by ?ontology
}

prologue do
  @rules = {
    :top_exists => 'no topObjectProperty',
    :toprop_exists => 'no topReifiedObjectProperty',
    :toprop_subprop => 'topReifiedObjectProperty not subproperty of topObjectProperty',
    :toprops_exists => 'no topReifiedObjectPropertySource',
    :toprops_subprop => 'topReifiedObjectPropertySource not subproperty of topObjectProperty',
    :topropt_exists => 'no topReifiedObjectPropertyTarget',
    :topropt_subprop => 'topReifiedObjectPropertyTarget not subproperty of topObjectProperty',
    :topuop_exists => 'no topUnreifiedObjectProperty',
    :topuop_subprop => 'topUnreifiedObjectProperty not subproperty of topObjectProperty',
    :toprsdp_exists => 'no topReifiedStructuredDataProperty',
    :toprsdp_subprop => 'topReifiedStructuredDataProperty not subproperty of topObjectProperty',
    :toprsdps_exists => 'no topReifiedStructuredDataPropertySource',
    :toprsdps_subprop => 'topReifiedStructuredDataPropertySource not subproperty of topObjectProperty',
    :toprsdpt_exists => 'no topReifiedStructuredDataPropertyTarget',
    :toprsdpt_subprop => 'topReifiedStructuredDataPropertyTarget not subproperty of topObjectProperty',
  }
end

predicate do |r|
  text = nil
  msg = @rules.inject([]) do |memo, kv|
    method, msg = *kv
    memo << msg + '.' unless r.send(method).true?
    memo
  end.join(' ')
  msg.empty? ? [true, nil] : [false, msg]
end

case_name { |r|  r.ontology }
