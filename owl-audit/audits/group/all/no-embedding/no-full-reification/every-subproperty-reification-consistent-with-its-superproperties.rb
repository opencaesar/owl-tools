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

name 'every subproperty reification consistent with its superproperties'

query %q{

 <%= @namespace_defs %>

  select distinct ?sub_prop ?sup_prop ?rel_ok ?src_ok ?trg_ok
  
 <%= @from_clauses_by_group['named'] %>
 <%= @from_named_clauses_by_group['named'] %>
 <%= @from_clauses_by_group['imported'] %>
  
  where {
    
    graph ?graph { ?sub_prop rdfs:subPropertyOf ?sup_prop }

    ?sub_rule swrl:head [ rdf:first [ swrl:propertyPredicate ?sub_prop ;
                                      swrl:argument1 ?s ;
                                      swrl:argument2 ?t ] ] ;
              swrl:body [ rdf:rest? [ rdf:first [ swrl:propertyPredicate ?sub_src ;
                                                  swrl:argument1 ?r ;
                                                  swrl:argument2 ?s ] ] ;
                          rdf:rest? [ rdf:first [ swrl:propertyPredicate ?sub_trg ;
                                                  swrl:argument1 ?r ;
                                                  swrl:argument2 ?t ] ] ] .
                      
    ?sub_src rdfs:domain ?sub_rel .
    ?sub_trg rdfs:domain ?sub_rel .
    
    ?sup_rule swrl:head [ rdf:first [ swrl:propertyPredicate ?sup_prop ;
                                      swrl:argument1 ?s ;
                                      swrl:argument2 ?t ] ] ;
              swrl:body [ rdf:rest? [ rdf:first [ swrl:propertyPredicate ?sup_src ;
                                                  swrl:argument1 ?r ;
                                                  swrl:argument2 ?s ] ] ;
                          rdf:rest? [ rdf:first [ swrl:propertyPredicate ?sup_trg ;
                                                  swrl:argument1 ?r ;
                                                  swrl:argument2 ?t ] ] ] .
                      
    ?sup_src rdfs:domain ?sup_rel .
    ?sup_trg rdfs:domain ?sup_rel .
    
    bind (exists { ?sub_rel rdfs:subClassOf ?sup_rel } as ?rel_ok)
    bind (exists { ?sub_src rdfs:subPropertyOf ?sup_src } as ?src_ok)
    bind (exists { ?sub_trg rdfs:subPropertyOf ?sup_trg } as ?trg_ok)
      
    filter (
        <%= @ontologies_by_group['named'].map { |o| o.to_uriref }.equal_any?('?graph') %>
      && ?sub_prop != ?sup_prop
    )
  }
  order by ?sub_prop ?sup_prop
}

prologue {
  @rules = {
    :rel_ok => 'invalid reification class',
    :src_ok => 'invalid source property',
    :trg_ok => 'invalid target property'
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

case_name { |r| "#{r.sub_prop.to_qname(@namespace_by_prefix)} subproperty of #{r.sup_prop.to_qname(@namespace_by_prefix)}" }
