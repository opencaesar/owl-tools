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
#    Adaptation of Application library for use with Jena.
#
#++

require 'Application/Application'

class JenaApplication < Application
  
  require 'set'
  require 'tsort'
  require 'yaml'
  require 'ruby-jena/jena'
  
  include Jena
  
  BUILTIN_NAMESPACES = {
    'rdf' => 'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
    'rdfs' => 'http://www.w3.org/2000/01/rdf-schema#',
    'owl' => 'http://www.w3.org/2002/07/owl#',
    'xsd' => 'http://www.w3.org/2001/XMLSchema#',
    'xml' => 'http://www.w3.org/XML/1998/namespace',
    'dc' => 'http://purl.org/dc/elements/1.1/',
    'swrl' => 'http://www.w3.org/2003/11/swrl#',
    'swrlb' => 'http://www.w3.org/2003/11/swrlb#',
  }

  DEFAULT_IMPORTS_FILE = nil
  DEFAULT_ENTAILMENT_TYPES = 'ClassEntailments,PropertyEntailments'
  DEFAULT_PREFIX_FILE = nil

  IMCE_JPL_NASA_GOV = 'http:\/\/imce\.jpl\.nasa\.gov/(foundation|discipline|application)'
  IMCE_JPL_NASA_GOV_RE = Regexp.new(IMCE_JPL_NASA_GOV)

  ANNOTATION_IRI = 'http://imce.jpl.nasa.gov/foundation/annotation/annotation'

  EMBEDDING_STRING = '-embedding'
  METAMODEL_STRING = '-metamodel'
  VIEW_STRING = '-view'

  def run

    add_options

    super

    # Load dataset information if specified.
   
    if @options.host && @options.port && @options.dataset
      @service_uri = get_service_uris
      @data_service = open_data_service(@service_uri['data'])
      @namespace_by_prefix = get_namespaces
      @namespace_defs = create_namespace_defs
      @imports_by_ontology, @closure_by_ontology = load_imports(argv)
      @closure_by_ontology.extend(TSortMethods)
      @sorted_ontologies = @closure_by_ontology.tsort.reverse
      named_ontologies, imported_ontologies = *collect_ontologies(argv)
      @ontologies_by_group = partition_by_group(named_ontologies, imported_ontologies)
      @ontologies_by_group_by_type = construct_entailment_uris(@options.entailment_types)
      @from_clauses_by_group, @from_clauses_by_group_by_type = *construct_from_clauses
      @from_named_clauses_by_group, @from_named_clauses_by_group_by_type = *construct_from_clauses(true)
    end

  end
  
  private


  def add_options
  
    @options.host = DEFAULT_HOST
    @option_parser.on('--host HOST', "Jena host (#{DEFAULT_HOST})") do |v|
      @options.host = v
    end
    @options.port = DEFAULT_PORT 
    @option_parser.on('--port PORT', "Jena port (#{DEFAULT_PORT})") do |v|
      @options.port = v
    end
    @options.dataset = DEFAULT_DATASET
    @option_parser.on('--dataset DATASET', "Jena dataset (#{DEFAULT_DATASET})") do |v|
      @options.dataset = v
    end
    @options.imports_file = DEFAULT_IMPORTS_FILE
    @option_parser.on('--imports-file FILE', "ontology imports file (#{DEFAULT_IMPORTS_FILE})") do |v|
      @options.imports_file = v
    end
    @options.entailment_types = DEFAULT_ENTAILMENT_TYPES
    @option_parser.on('--entailment-types LIST', "entailment types (#{DEFAULT_ENTAILMENT_TYPES})") do |v|
      @options.entailment_types = v
    end
    @options.prefix_file = DEFAULT_PREFIX_FILE
    @option_parser.on('--prefix-file FILE', "prefix file (#{DEFAULT_PREFIX_FILE})") do |v|
      @options.prefix_file = v
    end
  
  end
  
  # Get service URIs.
    
  def get_service_uris(host = @options.host, port = @options.port, dataset = @options.dataset)
    service_uri = FUSEKI_SERVICES.inject({}) do |m, o|
      if o == 'query'
        m[o] = "http://#{host}:#{port}/#{dataset}"
      else
        m[o] = "http://#{host}:#{port}/#{dataset}/#{o}"
      end
      log(INFO, "m[#{o}] = #{m[o]}")
      m
    end
  end
  
  # Open data service.
  
  def open_data_service(uri)
    log(DEBUG, "open_data_service: uri=#{uri}")
    DatasetAccessorFactory.create_http(uri)
  end
  
  # Get namespace definitions.

  def get_namespaces
    log(INFO, 'get namespace definitions')
    if @options.prefix_file
      namespace_by_prefix = YAML.load(File.open(@options.prefix_file))
      namespace_by_prefix.merge!(BUILTIN_NAMESPACES)
    else
      # This code uses the @data_service to populate namespace prefixes; however, set_ns_prefix may not have any effect.
      # BUILTIN_NAMESPACES.each do |p, n|
      #   log(DEBUG, "set nsPrefix: #{p} = #{n}")
      #   @data_service.get_model.set_ns_prefix(p, n)
      # end
      # namespace_by_prefix = @data_service.get_model.get_ns_prefix_map.to_hash
      namespace_by_prefix = BUILTIN_NAMESPACES
    end
    log(DEBUG, "namespace_by_prefix: #{namespace_by_prefix.inspect}")
    namespace_by_prefix
  end

  # Construct SPARQL prefixes.

  def create_namespace_defs(namespace_by_prefix = @namespace_by_prefix)
    log(INFO, 'construct sparql prefixes')
    namespace_defs = namespace_by_prefix.map do |prf, ns|
      "PREFIX #{prf}:<#{ns}>"
    end.join("\n")
    log(DEBUG, "namespace_defs: #{namespace_defs}")
    namespace_defs
  end
  
  # Load imports graph.
  
  def load_imports(named_uris, imports_file = @options.imports_file)
    log(INFO, "load imports graph")
    imports_by_ontology = {}
    closure_by_ontology = Hash.new { |h, k| h[k] = Set.new }
    case imports_file
    when nil
      log(DEBUG, 'load imports graph from repository models')
      to_do = named_uris.dup
      seen = Set.new
      while importer = to_do.shift
        unless seen.include?(importer)
          seen << importer
          imports_by_ontology[importer] = Set.new
          query = %Q{
            select ?imported
            from <#{importer}>
            where { <#{importer}> <http://www.w3.org/2002/07/owl#imports> ?imported }
          }
          run_select_query(query) do |s|
            imported = s.imported.to_string
            imports_by_ontology[importer] << imported
            to_do << imported
          end
        end
      end
      imports_by_ontology.extend(Closable)
      imports_by_ontology.each_key do |k|
        closure_by_ontology[k] = imports_by_ontology.close(k)
      end
    else
      log(DEBUG, "load imports graph from #{imports_file}")
      y = YAML.load(File.open(imports_file))
      imports_by_ontology.merge!(y['imports'])
      closure_by_ontology.merge!(y['closure'])
    end
    log(DEBUG, "imports_by_ontology: #{imports_by_ontology.inspect}")
    log(DEBUG, "closure_by_ontology: #{closure_by_ontology.inspect}")
    [imports_by_ontology, closure_by_ontology]
  end
  
  # Collect ontology URIs.
  
  def collect_ontologies(named_uris, closure_by_ontology = @closure_by_ontology)
    log(INFO, 'collect ontology uris')
    named_ontologies = Set.new(named_uris)
    imported_ontologies = named_uris.inject(Set.new) do |m, u|
      m += closure_by_ontology[u]
      m
    end
    imported_ontologies -= named_ontologies
    ontologies = named_ontologies + imported_ontologies
    log(DEBUG, "named_ontologies: #{named_ontologies.inspect}")
    log(DEBUG, "imported_ontologies: #{imported_ontologies.inspect}")
    
    [named_ontologies, imported_ontologies]
  end
   
  # Partition ontologies by group: imce/omg and named/imported.
  
  def partition_by_group(named_ontologies, imported_ontologies)
    log(INFO, 'partition ontologies by group')
    ontologies_by_group = {}
    ontologies = named_ontologies + imported_ontologies
    { 'imce' => IMCE_JPL_NASA_GOV_RE, }.each do |group, re|
      ontologies_by_group[group] = ontologies.select { |o| o.to_s =~ re }
    end
    { 'named' => named_ontologies, 'imported' => imported_ontologies,
      'annotation' => [ANNOTATION_IRI] }.each do |group, olist|
      ontologies_by_group[group] = olist
    end
    ontologies_by_group.keys.each do |group|
      g = group + '-imce'
      ontologies_by_group[g] = ontologies_by_group[group] & ontologies_by_group['imce']
    end
    ontologies_by_group.keys.each do |group|
      [EMBEDDING_STRING, METAMODEL_STRING, VIEW_STRING].each do |string|
        g = group + string
        ontologies_by_group[g] = ontologies_by_group[group].select { |o| o.to_s =~ /#{string}\z/ }
        g = group + "-no#{string}"
        ontologies_by_group[g] = ontologies_by_group[group].reject { |o| o.to_s =~ /#{string}\z/ }
      end
    end
    ontologies_by_group.each do |group, olist|
      log(DEBUG, "ontologies_by_group['#{group}'] = #{olist.inspect}")
    end
    ontologies_by_group
  end

  # Construct ontology URIs for entailments.
  
  def construct_entailment_uris(types, ontologies_by_group = @ontologies_by_group)
    log(INFO, 'construct entailment uris')
    entailment_types = types.split(/\s*,\s*/)
    ontologies_by_group_by_type = Hash.new { |h, k| h[k] = Hash.new { |l, m| l[m] = [] } }
    ontologies_by_group.each do |group, olist|
      entailment_types.each do |etype|
        ontologies_by_group_by_type[group][etype] = olist.map { |o| o + "/#{etype}" }
        log(DEBUG, "ontologies_by_group_by_type['#{group}']['#{etype}'] = #{ontologies_by_group_by_type[group][etype].inspect}")
      end
    end
    ontologies_by_group_by_type
  end

  # Construct SPARQL 'from' and 'from named' clauses.
  
  def construct_from_clauses(named = false, ontologies_by_group = @ontologies_by_group,
    ontologies_by_group_by_type = @ontologies_by_group_by_type)
  
    if named
      n1 = ' named'
      n2 = '_named'
    else
      n1 = n2 = ''
    end
    log(INFO, "construct 'from#{n1}' clauses")
    from_clauses_by_group = {}
    ontologies_by_group.each do |group, list|
      from_clauses_by_group[group] = list.map { |ont| "from#{n1} <#{ont}>" }.join("\n")
      log(DEBUG, "from#{n2}_clauses_by_group['#{group}'] = #{from_clauses_by_group[group].inspect}")
    end
  
    from_clauses_by_group_by_type = Hash.new { |h, k| h[k] = Hash.new { |l, m| l[m] = [] } }
    ontologies_by_group_by_type.each do |group, hash|
      hash.each do |etype, list|
        from_clauses_by_group_by_type[group][etype] = list.map { |ont| "from#{n1} <#{ont}>" }.join("\n")
        log(DEBUG, "from#{n2}_clauses_by_group_by_type['#{group}']['#{etype}'] = #{from_clauses_by_group_by_type[group][etype].inspect}")
      end
    end
    [from_clauses_by_group, from_clauses_by_group_by_type]
  end

  public
  
  # Prepare query.
  
  def prepare_query(qstring, binding)
    pss = ParameterizedSparqlString.new(qstring)
    if binding
      map = QuerySolutionMap.new
      map.addAll(binding)
      pss.setParams(map)
    end
    log(DEBUG, "query: #{pss.toString}")
    pss
  end
  
  # Run SELECT query.
  
  def run_select_query(qstring, binding = nil, service_uri = @service_uri['query'], &block)
    query = prepare_query(qstring, binding).asQuery
    log(DEBUG, "SPARQL endpoint: #{service_uri}")
    log(DEBUG, "SPARQL query: #{query}")
    query_exec = QueryExecutionFactory.sparqlService(service_uri, query)
    
    solns = []
    begin
      results = query_exec.execSelect
      while results.has_next
        soln = results.next
        log(DEBUG, "solution: #{soln.to_s}")
        if block_given?
          yield soln
        else
          solns << soln
        end
      end
    ensure
      query_exec.close
    end
    solns
  end
  
  # Run UPDATE query.
  
  def run_update_query(qstring, binding = nil, service_uri = @service_uri['update'], &block)
    request = prepare_query(qstring, binding).asUpdate
    update_exec = UpdateExecutionFactory.createRemote(request, service_uri)
    update_exec.execute
  end
  
end

#  Helper module for topological sorting.

module TSortMethods
  include TSort
  def tsort_each_node(&block)
    each_key(&block)
  end
  def tsort_each_child(node, &block)
    begin
      self.fetch(node).each(&block)
    rescue IndexError
    end
  end
end

# Close method for imports hash.

module Closable
  def close(k)
    if self.include?(k) 
      sk = self[k]
      sk.inject(Set.new) do |m, o|
        m << o
        m += self.close(o)
        m
      end
    else
      raise "no values for #{k} in close()"
    end
  end
end
