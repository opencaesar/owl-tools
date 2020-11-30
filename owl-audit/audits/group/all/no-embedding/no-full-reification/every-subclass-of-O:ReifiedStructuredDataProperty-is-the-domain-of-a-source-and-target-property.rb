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

name 'every subclass of O:ReifiedStructuredDataProperty is the domain of a source and target property'

query %q{

  <%= @namespace_defs %>

  select distinct ?klass ?src_prop_ok ?trg_prop_ok

  <%= @from_clauses_by_group['named'] %>
  <%= @from_clauses_by_group_by_type['named']['ClassEntailments'] %>
  <%= @from_clauses_by_group_by_type['named']['PropertyEntailments'] %>

  where {
    
    <%= @top_rsdp.map { |t| "{ ?klass rdfs:subClassOf #{t} }" }.join(' union ') %>

    bind(exists {
      <%= @top_rsdps.map { |t| "{ ?src_prop rdfs:subPropertyOf #{t} }" }.join(' union ') %>
      ?src_prop rdfs:domain ?klass .
    } as ?src_prop_ok)

    bind(exists {
      <%= @top_rsdpt.map { |t| "{ ?trg_prop rdfs:subPropertyOf #{t} }" }.join(' union ') %>
      ?trg_prop rdfs:domain ?klass .
    } as ?trg_prop_ok)

    filter (
         ?klass != owl:Nothing
      && ! <%= @top_rsdp.equal_any?('?klass') %>
    )

  }
}

prologue do
  backbones = @ontologies_by_group['named'].map do |o|
      o.sub(/(http:\/\/)(.*)/, '\1imce.jpl.nasa.gov/backbone/\2')
  end
  @top_rsdp = backbones.map { |b| "<#{b}#ReifiedStructuredDataProperty>" }
  @top_rsdps = backbones.map { |b| "<#{b}#topReifiedStructuredDataPropertySource>" }
  @top_rsdpt = backbones.map { |b| "<#{b}#topReifiedStructuredDataPropertyTarget>" }
  @rules = {
    :src_prop_ok => 'no source property',
    :trg_prop_ok => 'no target property',
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

case_name { |r| r.klass.to_qname(@namespace_by_prefix) }
