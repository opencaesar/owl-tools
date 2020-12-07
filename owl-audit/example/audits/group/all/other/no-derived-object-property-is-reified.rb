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

name 'no derived object property is reified'

query %q{

  <%= @namespace_defs %>

  select distinct ?oprop ?audit_case_ok

  <%= @from_clauses_by_group['named'] %>

  where {

    ?oprop rdf:type owl:ObjectProperty ;
           annotation:isDerived true .

    optional {
      ?rule swrl:head [ rdf:first [ swrl:propertyPredicate ?oprop ] ] .
    }
    
    bind(!bound(?rule) as ?audit_case_ok)
  }
}

case_name { |r| "#{r.oprop.to_qname(@namespace_by_prefix)}" }