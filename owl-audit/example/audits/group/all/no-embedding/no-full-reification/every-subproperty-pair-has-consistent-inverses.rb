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

name 'every subproperty pair has consistent inverses'

query %q{

  <%= @namespace_defs %>

  select distinct ?sub_prop ?sup_prop ?inverse_ok
  
  <%= @from_clauses_by_group['named'] %>
  <%= @from_named_clauses_by_group['named'] %>
  <%= @from_clauses_by_group['imported'] %>
  
  where {
    
    graph ?graph { ?sub_prop rdfs:subPropertyOf ?sup_prop }
    
    { ?sub_prop owl:inverseOf ?sub_inverse } union { ?sub_inverse owl:inverseOf ?sub_prop }
    { ?sup_prop owl:inverseOf ?sup_inverse } union { ?sup_inverse owl:inverseOf ?sup_prop }
      
    bind (exists { ?sub_inverse rdfs:subPropertyOf ?sup_inverse } as ?inverse_ok)
      
    filter (
         <%= @ontologies_by_group['named'].map { |o| o.to_uriref }.equal_any?('?graph') %>
      && !isblank(?sub_inverse)
      && !isblank(?sup_inverse)
      && ?sub_prop != ?sup_prop
    )
  }
}

predicate { |r| r.inverse_ok.true? ? [true, nil] : [false, 'inconsistent inverses.'] }
  
case_name { |r| "#{r.sub_prop.to_qname(@namespace_by_prefix)} subclass of #{r.sup_prop.to_qname(@namespace_by_prefix)}" }