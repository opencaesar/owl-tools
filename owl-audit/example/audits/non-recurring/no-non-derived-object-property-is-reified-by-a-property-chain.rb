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

name 'no non-derived object property is reified by a property chain'

query %q{

  <%= @namespace_defs %>

  select distinct ?oprop ?srcprop ?trgprop

  <%= @from_clauses_by_group['named'] %>

  where {

    ?oprop rdf:type owl:ObjectProperty .

    optional {
        ?oprop owl:propertyChainAxiom [
          rdf:first [
            owl:inverseOf ?srcprop ;
          ] ;
          rdf:rest [
            rdf:first ?trgprop ;
          ]
        ] .
    }
    
    filter(not exists { ?oprop annotation:isDerived true })
  }
}

case_name { |r| "#{r.oprop.to_qname(@namespace_by_prefix)}" }
  
predicate do |r|
  case r.srcprop.nil?
  when false
    s = r.srcprop.to_qname(@namespace_by_prefix)
    t = r.trgprop.to_qname(@namespace_by_prefix)
    [false, "source #{s}. target #{t}."]
  else
    [true, nil]
  end
end