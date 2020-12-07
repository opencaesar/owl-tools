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

name %q{every literal typed as xsd:boolean has value 'true' or 'false'}

query %q{

  <%= @namespace_defs %>

  select distinct ?entity ?property ?value ?audit_case_ok

  <%= @from_clauses_by_group['named'] %>
  
  where {
    ?entity ?property ?value .

    bind(?value = true || ?value = false as ?audit_case_ok)
      
    filter (datatype(?value) = xsd:boolean)
  }
  order by ?entity ?property
}

case_name { |r| "#{r.entity.to_qname(@namespace_by_prefix)} #{r.property.to_qname(@namespace_by_prefix)}" }