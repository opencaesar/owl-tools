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

name 'every inverse property pair has exactly one derived property'

query %q{

  <%= @namespace_defs %>

  select distinct ?prop1 ?prop2 ?audit_case_ok
  
  <%= @from_clauses_by_group['named'] %>
  <%= @from_clauses_by_group_by_type['named']['PropertyEntailments'] %>
  
  where {
    
    ?prop1 owl:inverseOf ?prop2 .

    bind(exists {?prop1 annotation:isDerived true} as ?prop1_derived)
    bind(exists {?prop2 annotation:isDerived true} as ?prop2_derived)
    bind((?prop1_derived &&  !?prop2_derived) || (?prop2_derived && !?prop1_derived) as ?audit_case_ok)

    filter (!isBlank(?prop1) && !isBlank(?prop2))
  }
}

case_name { |r| "#{r.prop1.to_qname(@namespace_by_prefix)} inverse of #{r.prop2.to_qname(@namespace_by_prefix)}" }
