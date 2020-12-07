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

name 'every subproperty domain/range consistent with superproperty'

query %q{

  <%= @namespace_defs %>

  select distinct ?sub_prop ?sup_prop ?domain_ok ?range_ok

  <%= @from_named_clauses_by_group['named'] %>
  <%= @from_clauses_by_group['named'] %>
  <%= @from_clauses_by_group_by_type['named']['ClassEntailments'] %>
  <%= @from_clauses_by_group['imported'] %>
  <%= @from_clauses_by_group_by_type['imported']['ClassEntailments'] %>
  
  where {
    
    graph ?graph { ?sub_prop rdfs:subPropertyOf ?sup_prop }

    optional {
      ?sub_prop rdfs:domain ?sub_domain .
    }
    optional {
      ?sub_prop rdfs:range ?sub_range .
    }

    optional {
      ?sup_prop rdfs:domain ?sup_domain .
    }
    optional {
      ?sup_prop rdfs:range ?sup_range .
    }

    bind(exists { ?sub_prop rdf:type owl:ObjectProperty } as ?object_property)
    bind(!bound(?sup_domain) || (bound(?sub_domain) && exists { ?sub_domain rdfs:subClassOf ?sup_domain }) as ?domain_ok)
    bind(
      !?object_property || (
           !bound(?sup_range)
        || (bound(?sub_range)  && exists { ?sub_range  rdfs:subClassOf ?sup_range  })
      ) as ?range_ok
    )
  }
  order by ?sub_prop ?sup_prop
}

prologue {
  @rules = {
    :domain_ok => 'invalid domain',
    :range_ok => 'invalid range'
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