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

name 'every property has a single domain and range'

query %q{

  <%= @namespace_defs %>

  select distinct ?property ?at_least_one_domain ?at_most_one_domain ?at_least_one_range ?at_most_one_range
                  ?non_blank_domain ?non_blank_range

  <%= @from_clauses_by_group['named'] %>

  where {
    
    { ?property rdf:type owl:ObjectProperty } union { ?property rdf:type owl:DatatypeProperty }

    optional {
      ?property rdfs:domain ?domain1 .
      optional {
        ?property rdfs:domain ?domain2 .
        filter (?domain1 != ?domain2)
      }
    }

    optional {
      ?property rdfs:range ?range1 .
      optional {
        ?property rdfs:range ?range2 .
        filter (?range1 != ?range2)
      }
    }

    bind(bound(?domain1) as ?at_least_one_domain)
    bind(!bound(?domain2) as ?at_most_one_domain)
    bind(bound(?range1) as ?at_least_one_range)
    bind(!bound(?range2) as ?at_most_one_range)

    bind(?at_least_one_domain && ?at_most_one_domain && !isBlank(?domain1) as ?non_blank_domain)
    bind(?at_least_one_range && ?at_most_one_range && !isBlank(?range1) as ?non_blank_range)
    
    filter(!regex(str(?property), "http://imce\\\\.jpl\\\\.nasa\\\\.gov/backbone/.*#"))
  }
  order by ?property
}

prologue do
  @rules = {
    :at_least_one_domain => 'missing domain',
    :at_most_one_domain => 'multiple domains',
    :at_least_one_range => 'missing range',
    :at_most_one_range => 'multiple ranges',
    :non_blank_domain => 'anonymous domain',
    :non_blank_range => 'anonymous range'
  }
end

predicate do |r|
  msg = @rules.inject([]) do |memo, kv|
    method, msg = *kv
    memo << msg + '.' unless r.send(method).true?
    memo
  end.join(' ')
  msg.empty? ? [true, nil] : [false, msg]
end

case_name { |r| "#{r.property.to_qname(@namespace_by_prefix)}" }
