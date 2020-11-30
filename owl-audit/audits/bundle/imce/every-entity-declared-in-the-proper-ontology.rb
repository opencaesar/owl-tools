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

name 'every entity declared in the proper ontology'

query %q{
  
  <%= @namespace_defs %>
  
  select distinct ?entity ?audit_case_ok ?audit_case_text
  
  <%= @from_named_clauses_by_group['named'] %>
  <%= @from_named_clauses_by_group['imported'] %>
  
  where {
    graph ?graph { ?entity rdf:type ?type . }
    bind(
      ( strstarts(str(?entity), str(?graph))
        || strstarts(str(?entity), concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://")))
        || ?type = swrl:Variable
      ) as ?audit_case_ok
    )
    bind (concat(concat("declared in ", str(?graph)), ".") as ?audit_case_text)
  
    filter (
         !isblank(?entity)
      && ?type != owl:AnnotationProperty
    )
  }
  order by ?graph ?entity
}

case_name { |r| r.entity.to_qname(@namespace_by_prefix) }
