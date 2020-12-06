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

name 'every entity, aspect, or reified object property is a characterized element'

query %q{

  <%= @namespace_defs %>

  select distinct ?klass ?is_ce ?is_ae

  <%= @from_clauses_by_group['named'] %>
  <%= @from_clauses_by_group_by_type['named']['ClassEntailments'] %>
  <%= @from_clauses_by_group['imported'] %>
  <%= @from_clauses_by_group_by_type['imported']['ClassEntailments'] %>

  where {

    ?klass rdfs:subClassOf ?super .

    bind(exists { ?klass rdfs:subClassOf analysis:CharacterizedElement } as ?is_ce)
    bind(exists { ?klass rdfs:subClassOf analysis:AnalyzedElement } as ?is_ae)

    filter(
         ?klass != analysis:AnalyzedElement
      && ?klass != analysis:CharacterizedElement
      && ?super != analysis:AnalyzedElement
      && ?super != analysis:CharacterizedElement
      && regex(str(?super), "/backbone/.*#(Entity|Aspect|ReifiedObjectProperty)$")
      && !regex(str(?klass), "/backbone/.*#")
    )
  }
}

predicate do |r|
  msg = []
  msg << 'Not analysis:CharacterizedElement.' unless r.is_ce.true?
  msg << 'Not analysis:AnalyzedElement.' unless r.is_ae.true?
  msg.empty? ? [true, nil] : [false, msg.join(' ')]
end


case_name { |r| r.klass.to_qname(@namespace_by_prefix) }
