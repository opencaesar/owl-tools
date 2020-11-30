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

name 'every subclass of O:ReifiedObjectProperty is the domain of a source and target property'

query %q{

  <%= @namespace_defs %>

  select distinct ?klass ?src_prop_ok ?trg_prop_ok

  <%= @from_clauses_by_group['named'] %>
  <%= @from_clauses_by_group_by_type['named']['ClassEntailments'] %>
  <%= @from_clauses_by_group_by_type['named']['PropertyEntailments'] %>

  where {
    
    <%= @top_rop.map { |t| "{ ?klass rdfs:subClassOf #{t} }" }.join(' union ') %>

    bind(exists {
      <%= @top_rops.map { |t| "{ ?src_prop rdfs:subPropertyOf #{t} }" }.join(' union ') %>
      ?src_prop rdfs:domain ?klass .
    } as ?src_prop_ok)

    bind(exists {
      <%= @top_ropt.map { |t| "{ ?trg_prop rdfs:subPropertyOf #{t} }" }.join(' union ') %>
      ?trg_prop rdfs:domain ?klass .
    } as ?trg_prop_ok)

    filter (
         ?klass != owl:Nothing
      && ! <%= @top_rop.equal_any?('?klass') %>
    )

  }
}

prologue do
  backbones = @ontologies_by_group['named'].map do |o|
      o.sub(/(http:\/\/)(.*)/, '\1imce.jpl.nasa.gov/backbone/\2')
  end
  @top_rop = backbones.map { |b| "<#{b}#ReifiedObjectProperty>" }
  @top_rops = backbones.map { |b| "<#{b}#topReifiedObjectPropertySource>" }
  @top_ropt = backbones.map { |b| "<#{b}#topReifiedObjectPropertyTarget>" }
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
