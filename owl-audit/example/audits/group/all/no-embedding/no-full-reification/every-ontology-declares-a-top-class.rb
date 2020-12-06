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

name 'every ontology declares a top class'

query %q{
  
  <%= @namespace_defs %>
  
  select distinct ?ontology ?graph
                  ?thingExists ?thingNoMapping
                  ?aspectExists ?aspectSubThing
                  ?entityExists ?entitySubThing
                  ?ropExists ?ropSubThing
                  ?rsdpExists ?sdtSubThing
                  ?sdtExists ?rsdpSubThing
                  
  
  <%= @from_named_clauses_by_group['named'] %>
  
  where {
    graph ?graph {
      ?ontology rdf:type owl:Ontology
    }
    
    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#Thing")) as ?thing)
    bind(exists { graph ?graph { ?thing rdf:type owl:Class } } as ?thingExists)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#Aspect")) as ?aspect)
    bind(exists { graph ?graph { ?aspect rdf:type owl:Class } } as ?aspectExists)
    bind(?aspectExists && exists { graph ?graph { ?aspect rdfs:subClassOf ?thing } } as ?aspectSubThing)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#Entity")) as ?entity)
    bind(exists { graph ?graph { ?entity rdf:type owl:Class } } as ?entityExists)
    bind(?entityExists && exists { graph ?graph { ?entity rdfs:subClassOf ?thing } } as ?entitySubThing)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#ReifiedObjectProperty")) as ?rop)
    bind(exists { graph ?graph { ?rop rdf:type owl:Class } } as ?ropExists)
    bind(?ropExists && exists { graph ?graph { ?rop rdfs:subClassOf ?thing } } as ?ropSubThing)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#ReifiedStructuredDataProperty")) as ?rsdp)
    bind(exists { graph ?graph { ?rsdp rdf:type owl:Class } } as ?rsdpExists)
    bind(?rsdpExists && exists { graph ?graph { ?rsdp rdfs:subClassOf ?thing } } as ?rsdpSubThing)

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#StructuredDatatype")) as ?sdt)
    bind(exists { graph ?graph { ?sdt rdf:type owl:Class } } as ?sdtExists)
    bind(?sdtExists && exists { graph ?graph { ?sdt rdfs:subClassOf ?thing } } as ?sdtSubThing)

    filter (
      <%= (@ontologies_by_group['named']  - @ontologies_by_group['named-embedding'] - @ontologies_by_group['named-view']).map { |o| o.to_uriref }.equal_any?('?graph')
    %>)
        
  }
  order by ?ontology
}

prologue do
  @rules = {
    :thingExists => 'no Thing',
    :aspectExists => 'no Aspect',
    :aspectSubThing => 'Aspect not subclass of Thing',
    :entityExists => 'no Entity',
    :entitySubThing => 'Entity not subclass of Thing',
    :ropExists => 'no ReifiedObjectProperty',
    :ropSubThing => 'ReifiedObjectProperty not subclass of Thing',
    :rsdpExists => 'no ReifiedStructuredDataProperty',
    :rsdpSubThing => 'ReifiedStructuredDataProperty not subclass of Thing',
    :sdtExists => 'no StructuredDatatype',
    :sdtSubThing => 'StructuredDatatype not subclass of Thing',
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
