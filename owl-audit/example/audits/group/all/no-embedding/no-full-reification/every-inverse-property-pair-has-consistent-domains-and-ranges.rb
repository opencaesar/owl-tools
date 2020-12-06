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

name 'every inverse property pair has consistent domains and ranges'

query %q{

  <%= @namespace_defs %>

  select distinct ?prop1 ?prop2 ?audit_case_ok
  
  <%= @from_clauses_by_group['named'] %>
  <%= @from_clauses_by_group_by_type['named']['PropertyEntailments'] %>
  
  where {
    
    ?prop1 owl:inverseOf ?prop2 .

    ?prop1 rdfs:domain ?prop1_d .
    ?prop1 rdfs:range ?prop1_r .
    ?prop2 rdfs:domain ?prop2_d .
    ?prop2 rdfs:range ?prop2_r .

    bind((?prop1_d = ?prop2_r) && (?prop2_d = ?prop1_r) as ?audit_case_ok)

    filter (!isBlank(?prop1) && !isBlank(?prop2))
  }
}

case_name { |r| "#{r.prop1.to_qname(@namespace_by_prefix)} inverse of #{r.prop2.to_qname(@namespace_by_prefix)}" }
