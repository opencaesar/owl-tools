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

name 'every entity declared in some ontology'

query %q{

  <%= @namespace_defs %>

  select distinct ?entity ?audit_case_ok

  <%= @from_clauses_by_group['named'] %>
  <%= @from_clauses_by_group['imported'] %>

  where {
          { ?entity ?predicate ?object }
    union { ?subject ?entity ?object }
    union { ?subject ?predicate ?entity }
       
    bind(exists { ?entity rdf:type ?type } as ?audit_case_ok)

    filter (
         isiri(?entity)
      && !regex(str(?entity), "<%= JenaApplication::PURL_ORG_SPARQL_RE %>", "i")
      && !regex(str(?entity), "<%= JenaApplication::WWW_W3_ORG_SPARQL_RE %>", "i")
      && !( isiri(?subject) && ?predicate = owl:versionIRI )
    )
  }
  order by ?entity
}

case_name { |r| r.entity.to_qname(@namespace_by_prefix) }
