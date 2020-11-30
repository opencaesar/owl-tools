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

name 'every object property is completely reified'

query %q{
  
  <%= @namespace_defs %>
  
  select distinct ?oprop ?isDerived ?rule_exists ?relclass_exists ?relclass_embedded
                  ?srcprop_exists ?srcprop_embedded ?trgprop_exists ?trgprop_embedded
                  ?srcprop_range_ok ?trgprop_range_ok ?srcprop ?trgprop ?relclass
                  ?func_ok ?invfunc_ok ?inv_oprop ?toprop ?domain_mapped ?range_mapped
  
  <%= @from_clauses_by_group['named'] %>
  <%= @from_clauses_by_group_by_type['named']['ClassEntailments'] %>
  <%= @from_clauses_by_group_by_type['named']['PropertyEntailments'] %>
  <%= @from_clauses_by_group['imported'] %>
  <%= @from_clauses_by_group_by_type['imported']['ClassEntailments'] %>
  <%= @from_clauses_by_group_by_type['imported']['PropertyEntailments'] %>
  <%= @from_named_clauses_by_group['named'] %>
  <%= @from_named_clauses_by_group_by_type['named']['ClassEntailments'] %>
  <%= @from_named_clauses_by_group_by_type['named']['PropertyEntailments'] %>
  
  where {
    graph ?graph {
       ?oprop rdf:type owl:ObjectProperty
    }

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedObjectProperty")) as ?toprop)
    ?oprop rdfs:subPropertyOf ?toprop .
    
    optional {
      { ?oprop owl:inverseOf ?inv_oprop } union { ?inv_oprop owl:inverseOf ?oprop }
      filter (!isBlank(?inv_oprop))
    }
    
    optional {

      # find property reification rule.
      
      ?prop_pred swrl:propertyPredicate ?oprop ;
                 swrl:argument1 ?s ;
                 swrl:argument2 ?t .
      ?rule swrl:head [ rdf:first ?prop_pred ;
                        rdf:rest rdf:nil
                      ] .
      ?rule rdfs:label ?label

      { 
        ?rule swrl:body [
                        rdf:first [ swrl:propertyPredicate ?srcprop ;
                                    swrl:argument1 ?r ;
                                    swrl:argument2 ?s
                                  ] ;
                        rdf:rest [ rdf:first [ swrl:propertyPredicate ?trgprop ;
                                               swrl:argument1 ?r ;
                                               swrl:argument2 ?t
                                             ];
                                   rdf:rest rdf:nil
                                 ]
                      ] .
      } union { 
        ?rule swrl:body [ rdf:first [ swrl:propertyPredicate ?trgprop ;
                                      swrl:argument1 ?r ;
                                      swrl:argument2 ?t
                                    ] ;
                          rdf:rest [ rdf:first [ swrl:propertyPredicate ?srcprop ;
                                                 swrl:argument1 ?r ;
                                                 swrl:argument2 ?s
                                               ];
                                     rdf:rest rdf:nil
                                   ]
                        ] .
      }
      
      optional {
    
        # find relationship class
    
        ?srcprop rdfs:domain ?relclass .
        ?trgprop rdfs:domain ?relclass .

      }

      # find domain and range of object property, ranges of source and target properties.

      optional {
        ?srcprop rdfs:range ?srcprop_range .
      }
      optional {
        ?trgprop rdfs:range ?trgprop_range .
      }

    }
    optional {
      ?oprop rdfs:domain ?oprop_domain .
    }
    optional {
      ?oprop rdfs:range ?oprop_range .
    }

    optional {
      ?oprop rdfs:domain ?oprop_domain .
    }
    optional {
      ?oprop rdfs:range ?oprop_range .
    }
      
    bind(exists { ?oprop annotation:isDerived true } as ?isDerived)
    bind(exists { ?oprop rdf:type owl:FunctionalProperty } as ?oprop_func)
    bind(exists { ?oprop rdf:type owl:InverseFunctionalProperty } as ?oprop_invfunc)
      
    bind(bound(?rule) as ?rule_exists)
    
    bind(bound(?srcprop) as ?srcprop_exists)
    bind(?srcprop_exists && exists { ?srcprop rdfs:subPropertyOf owl2-mof2-backbone:topReifiedObjectPropertySource } as ?srcprop_fwd_om2_embedded)
    bind(?srcprop_exists && exists { ?srcprop rdfs:subPropertyOf UML-backbone:topReifiedObjectPropertySource } as ?srcprop_fwd_uml_embedded)
    bind(?srcprop_exists && exists { ?srcprop rdfs:subPropertyOf owl2-mof2-backbone:topReifiedObjectPropertyTarget } as ?srcprop_rev_om2_embedded)
    bind(?srcprop_exists && exists { ?srcprop rdfs:subPropertyOf UML-backbone:topReifiedObjectPropertyTarget } as ?srcprop_rev_uml_embedded)
    bind(((?isDerived && (?srcprop_rev_om2_embedded || ?srcprop_rev_uml_embedded)) || ?srcprop_fwd_om2_embedded || ?srcprop_fwd_uml_embedded) as ?srcprop_embedded)
    bind(exists { ?srcprop rdf:type owl:FunctionalProperty } as ?srcprop_func)
    bind(exists { ?srcprop rdf:type owl:InverseFunctionalProperty } as ?srcprop_invfunc)
    
    bind(bound(?trgprop) as ?trgprop_exists)
    bind(?trgprop_exists && exists { ?trgprop rdfs:subPropertyOf owl2-mof2-backbone:topReifiedObjectPropertyTarget } as ?trgprop_fwd_om2_embedded)
    bind(?trgprop_exists && exists { ?trgprop rdfs:subPropertyOf UML-backbone:topReifiedObjectPropertyTarget } as ?trgprop_fwd_uml_embedded)
    bind(?trgprop_exists && exists { ?trgprop rdfs:subPropertyOf owl2-mof2-backbone:topReifiedObjectPropertySource } as ?trgprop_rev_om2_embedded)
    bind(?trgprop_exists && exists { ?trgprop rdfs:subPropertyOf UML-backbone:topReifiedObjectPropertySource } as ?trgprop_rev_uml_embedded)
    bind(((?isDerived && (?trgprop_rev_om2_embedded || ?trgprop_rev_uml_embedded)) || ?trgprop_fwd_om2_embedded || ?trgprop_fwd_uml_embedded) as ?trgprop_embedded)
    bind(exists { ?trgprop rdf:type owl:FunctionalProperty } as ?trgprop_func)
    bind(exists { ?trgprop rdf:type owl:InverseFunctionalProperty } as ?trgprop_invfunc)
        
    bind(bound(?relclass) as ?relclass_exists)
    bind(?relclass_exists && exists { ?relclass rdfs:subClassOf owl2-mof2-backbone:ReifiedObjectProperty } as ?relclass_om2_embedded)
    bind(?relclass_exists && exists { ?relclass rdfs:subClassOf UML-backbone:ReifiedObjectProperty } as ?relclass_uml_embedded)
    bind(?relclass_om2_embedded || ?relclass_uml_embedded as ?relclass_embedded)
  
    bind((!bound(?oprop_domain) && !bound(?srcprop_range)) || (bound(?oprop_domain) && bound(?srcprop_range) && ?oprop_domain = ?srcprop_range) as ?srcprop_range_ok)
    bind((!bound(?oprop_range)  && !bound(?trgprop_range)) || (bound(?oprop_range)  && bound(?trgprop_range) && ?oprop_range  = ?trgprop_range) as ?trgprop_range_ok)
    
    bind((!?srcprop_exists || (?oprop_func = ?srcprop_invfunc)) as ?func_ok)
    bind((!?trgprop_exists || (?oprop_invfunc = ?trgprop_invfunc)) as ?invfunc_ok)
    
    bind(bound(?oprop_domain) as ?domain_mapped)
    bind(bound(?oprop_range) as ?range_mapped)

    # Restrict object properties to embedded properties in IMCE namespace.
    
    filter (
         not exists { ?oprop annotation:isDerived true }
      && <%= @ontologies_by_group['named'].map { |o| o.to_uriref }.equal_any?('?graph') %>
      && ?oprop != ?toprop
    )
  }
  
  order by ?oprop
}

prologue do
  @rules = {
    :rule_exists => 'no property reification rule',
    :relclass_exists => 'no reified object property class',
    :srcprop_exists => 'no source property',
    :trgprop_exists => 'no target property',
    :srcprop_range_ok => 'property domain and source property range inconsistent',
    :trgprop_range_ok => 'property range and target property range inconsistent',
    :func_ok => 'property functional and source property inverse functional inconsistent',
    :invfunc_ok => 'property inverse functional and target property inverse functional inconsistent',
  }
  @rules.merge!({
    :relclass_embedded => 'reified object property class not embedded',
    :srcprop_embedded => 'source property not embedded',
    :trgprop_embedded => 'target property not embedded',
    :domain_mapped => 'domain not mapped',
    :range_mapped => 'range not mapped',
  }) if @options.audit_options.do_embedding == 'true'
end

predicate do |r|
  msgs = @rules.inject([]) do |memo, kv|
    method, msg = *kv
    raise "no binding for #{method} in result" unless r.contains(method)
    memo << msg + '.' unless r.send(method).true?
    memo
  end
  msgs << "derived from #{r.inv_oprop.toString}." if r.isDerived.true? && !msgs.empty?
  msg = msgs.join(' ')
  msg.empty? ? [true, nil] : [false, msg]
end

case_name { |r| r.oprop.to_qname(@namespace_by_prefix) }
