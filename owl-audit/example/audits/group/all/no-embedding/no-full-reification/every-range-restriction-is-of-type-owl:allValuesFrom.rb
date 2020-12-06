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

name 'every range restriction is of type owl:allValuesFrom'

query %q{

  <%= @namespace_defs %>

  select distinct ?klass ?rtype ?property ?audit_case_ok

  <%= @from_clauses_by_group['named'] %>

  where {

    ?klass rdfs:subClassOf ?rest .
    ?rest rdf:type owl:Restriction .
    ?rest owl:onProperty ?property .
    ?rest ?rtype ?value .

    bind(?rtype = owl:allValuesFrom as ?audit_case_ok)

    filter (
         ?rtype = owl:someValuesFrom
      || ?rtype = owl:allValuesFrom
      || ?rtype = owl:hasValue
      || ?rtype = owl:hasSelf
      || ?rtype = owl:minCardinality
      || ?rtype = owl:minQualifiedCardinality
      || ?rtype = owl:maxCardinality
      || ?rtype = owl:maxQualifiedCardinality
      || ?rtype = owl:Cardinality
      || ?rtype = owl:QualifiedCardinality
    )
  }
  order by ?klass ?property ?rtype
}

case_name { |r| "#{r.klass.to_qname(@namespace_by_prefix)} #{r.rtype.to_qname(@namespace_by_prefix)} restriction on #{r.property.to_qname(@namespace_by_prefix)}" }