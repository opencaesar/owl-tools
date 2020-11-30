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

name 'every structured data property is completely reified'

query %q{
  
  <%= @namespace_defs %>
  
  select distinct ?sdprop ?isDerived ?rule_exists ?relclass_exists ?relclass_embedded
                  ?srcprop_exists ?srcprop_embedded ?trgprop_exists ?trgprop_embedded
                  ?srcprop_range_ok ?trgprop_range_ok ?rule ?srcprop ?trgprop ?relclass
                  ?func_ok ?invfunc_ok ?inv_sdprop ?toprsdp ?domain_mapped ?range_mapped
  
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
       ?sdprop rdf:type owl:ObjectProperty
    }

    bind(iri(concat("http://imce.jpl.nasa.gov/backbone/", strafter(str(?graph), "http://"), "#topReifiedStructuredDataProperty")) as ?toprsdp)
    ?sdprop rdfs:subPropertyOf ?toprsdp .
    
    optional {
      { ?sdprop owl:inverseOf ?inv_sdprop } union { ?inv_sdprop owl:inverseOf ?sdprop }
      filter (!isBlank(?inv_sdprop))
    }
    
    optional {

      # find property reification rule.
      
      ?prop_pred swrl:propertyPredicate ?sdprop ;
                 swrl:argument1 ?s ;
                 swrl:argument2 ?t .
      ?rule swrl:head [ rdf:first ?prop_pred ;
                        rdf:rest rdf:nil
                      ] .
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
        ?rule swrl:body [
                          rdf:first [ swrl:propertyPredicate ?trgprop ;
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

      # find domain and range of structured data property, ranges of source and target properties.

      optional {
        ?srcprop rdfs:range ?srcprop_range .
      }
      optional {
        ?trgprop rdfs:range ?trgprop_range .
      }
    }
    optional {
      ?sdprop rdfs:domain ?sdprop_domain .
    }
    optional {
      ?sdprop rdfs:range ?sdprop_range .
    }
   
    optional {
      ?sdprop rdfs:domain ?sdprop_domain .
    }
    optional {
      ?sdprop rdfs:range ?sdprop_range .
    }
      
    bind(exists { ?sdprop annotation:isDerived true } as ?isDerived)
    bind(exists { ?sdprop rdf:type owl:FunctionalProperty } as ?sdprop_func)
    bind(exists { ?sdprop rdf:type owl:InverseFunctionalProperty } as ?sdprop_invfunc)
      
    bind(bound(?rule) as ?rule_exists)
    
    bind(bound(?srcprop) as ?srcprop_exists)
    bind(?srcprop_exists && exists { ?srcprop rdfs:subPropertyOf owl2-mof2-backbone:topReifiedStructuredDataPropertySource } as ?srcprop_fwd_om2_embedded)
    bind(?srcprop_exists && exists { ?srcprop rdfs:subPropertyOf UML-backbone:topReifiedStructuredDataPropertySource } as ?srcprop_fwd_uml_embedded)
    bind(?srcprop_exists && exists { ?srcprop rdfs:subPropertyOf owl2-mof2-backbone:topReifiedStructuredDataPropertyTarget } as ?srcprop_rev_om2_embedded)
    bind(?srcprop_exists && exists { ?srcprop rdfs:subPropertyOf UML-backbone:topReifiedStructuredDataPropertyTarget } as ?srcprop_rev_uml_embedded)
    bind(((?isDerived && (?srcprop_rev_om2_embedded || ?srcprop_rev_uml_embedded)) || ?srcprop_fwd_om2_embedded || ?srcprop_fwd_uml_embedded) as ?srcprop_embedded)
    bind(exists { ?srcprop rdf:type owl:FunctionalProperty } as ?srcprop_func)
    bind(exists { ?srcprop rdf:type owl:InverseFunctionalProperty } as ?srcprop_invfunc)
    
    bind(bound(?trgprop) as ?trgprop_exists)
    bind(?trgprop_exists && exists { ?trgprop rdfs:subPropertyOf owl2-mof2-backbone:topReifiedStructuredDataPropertyTarget } as ?trgprop_fwd_om2_embedded)
    bind(?trgprop_exists && exists { ?trgprop rdfs:subPropertyOf UML-backbone:topReifiedStructuredDataPropertyTarget } as ?trgprop_fwd_uml_embedded)
    bind(?trgprop_exists && exists { ?trgprop rdfs:subPropertyOf owl2-mof2-backbone:topReifiedStructuredDataPropertySource } as ?trgprop_rev_om2_embedded)
    bind(?trgprop_exists && exists { ?trgprop rdfs:subPropertyOf UML-backbone:topReifiedStructuredDataPropertySource } as ?trgprop_rev_uml_embedded)
    bind(((?isDerived && (?trgprop_rev_om2_embedded || ?trgprop_rev_uml_embedded)) || ?trgprop_fwd_om2_embedded || ?trgprop_fwd_uml_embedded) as ?trgprop_embedded)
    bind(exists { ?trgprop rdf:type owl:FunctionalProperty } as ?trgprop_func)
    bind(exists { ?trgprop rdf:type owl:InverseFunctionalProperty } as ?trgprop_invfunc)
        
    bind(bound(?relclass) as ?relclass_exists)
    bind(?relclass_exists && exists { ?relclass rdfs:subClassOf owl2-mof2-backbone:ReifiedStructuredDataProperty } as ?relclass_om2_embedded)
    bind(?relclass_exists && exists { ?relclass rdfs:subClassOf UML-backbone:ReifiedStructuredDataProperty } as ?relclass_uml_embedded)
    bind(?relclass_om2_embedded || ?relclass_uml_embedded as ?relclass_embedded)
  
    bind((!bound(?sdprop_domain) && !bound(?srcprop_range)) || (bound(?sdprop_domain) && bound(?srcprop_range) && ?sdprop_domain = ?srcprop_range) as ?srcprop_range_ok)
    bind((!bound(?sdprop_range)  && !bound(?trgprop_range)) || (bound(?sdprop_range)  && bound(?trgprop_range) && ?sdprop_range  = ?trgprop_range) as ?trgprop_range_ok)
    
    bind((!?srcprop_exists || (?sdprop_func = ?srcprop_invfunc)) as ?func_ok)
    bind((!?trgprop_exists || (?sdprop_invfunc = ?trgprop_invfunc)) as ?invfunc_ok)
    
    bind(bound(?sdprop_domain) as ?domain_mapped)
    bind(bound(?sdprop_range) as ?range_mapped)

    # Restrict object properties to embedded properties in IMCE namespace.
    
    filter (
         not exists { ?sdprop annotation:isDerived true }
      && <%= @ontologies_by_group['named'].map { |o| o.to_uriref }.equal_any?('?graph') %>
      && ?sdprop != ?toprsdp
    )
  }
  
  order by ?sdprop
}

prologue do
  @rules = {
    :rule_exists => 'no property reification rule',
    :relclass_exists => 'no reified structured data property class',
    :srcprop_exists => 'no source property',
    :trgprop_exists => 'no target property',
    :srcprop_range_ok => 'property domain and source property range inconsistent',
    :trgprop_range_ok => 'property range and target property range inconsistent',
    :func_ok => 'property functional and source property inverse functional inconsistent',
    :invfunc_ok => 'property inverse functional and target property inverse functional inconsistent',
  }
  @rules.merge!({
    :relclass_embedded => 'reified structured data property class not embedded',
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
  msgs << "derived from #{r.inv_sdprop.toString}." if r.isDerived.true? && !msgs.empty?
  msg = msgs.join(' ')
  msg.empty? ? [true, nil] : [false, msg]
end

case_name { |r| r.sdprop.to_qname(@namespace_by_prefix) }
