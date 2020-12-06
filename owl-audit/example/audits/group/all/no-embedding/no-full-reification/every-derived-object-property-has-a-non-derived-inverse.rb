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

name 'every derived object property has a non-derived inverse'

query %q{

  <%= @namespace_defs %>

  select distinct ?prop ?audit_case_ok
  
  <%= @from_clauses_by_group['named'] %>
  
  where {
    
    ?prop rdf:type owl:ObjectProperty .
    ?prop annotation:isDerived true .

    optional {
      { ?invprop owl:inverseOf ?prop } union { ?prop owl:inverseOf ?invprop }
      filter (!isBlank(?invprop))
    }
    bind(bound(?invprop) as ?audit_case_ok)

    filter (!isBlank(?prop))
  }
}

case_name { |r| r.prop.to_qname(@namespace_by_prefix) }