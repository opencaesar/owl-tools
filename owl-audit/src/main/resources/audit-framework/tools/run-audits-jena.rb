#!/usr/bin/env ruby

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

require 'lib/Application/JenaApplication'
require 'lib/Audit/JenaAudit'

APPLICATION_NAME = 'run-audits-jena'
GROUP_TYPES = %w{imce omg}

class JenaAuditApplication < JenaApplication
  
  def run
    
    # Extend standard options for audit files, directories, and trees.
    
    @options.audit_files = []
    @option_parser.on('--audit-file FILE', 'audit spec file') do |v|
      @options.audit_files << v
    end
    
    @options.audit_dirs = []
    @option_parser.on('--audit-dir DIRECTORY', 'audit specs directory') do |v|
      @options.audit_dirs << v
    end
    
    @options.audit_trees = []
    @option_parser.on('--audit-tree TREE', 'audit specs tree') do |v|
      @options.audit_trees << v
    end
    
    @options.report = false
    @option_parser.on('--report', 'run as reports') do
      @options.report = true
    end

    # Audit options.
    
    @options.audit_options = OpenStruct.new
    @option_parser.on('--audit-option NAME=VALUE', 'audit option') do |v|
      name, value = v.split(/=/)
      method = "#{name}=".to_sym
      @options.audit_options.send(method, value)
    end

    # Load IRIs from file if specified.

    @options.iri_file = nil
    @option_parser.on('--iri-file FILE', 'input IRI file [nil]') do |v|
      begin
        @options.iri_file = v
        @argv = File.open(@options.iri_file).readlines.map { |l| l.strip }
      rescue
        print("error reading IRI file #{@options.iri_file}\n")
        raise $!
      end
    end

    @options.output_file = nil
    @option_parser.on('--output-file FILE', 'output file [nil]') do |v|
      @options.output_file = v
    end
    # Pass information through in options.
    
    @options.application = self
    @options.binding = binding
    @options.logger = @log

    super

    # Collect audit specifications.

    log(DEBUG, "report? #{@options.report}")

    battery = @options.report ?
      OntologyAudit::ReportBattery.new(@options) : OntologyAudit::AuditBattery.new(@options)
    
    @options.audit_files.each { |f| battery.add_audit_file(f) }
    @options.audit_dirs.each { |d| battery.add_audit_dir(d) }
    @options.audit_trees.each { |t| battery.add_audit_tree(t) }
    
    # Run audits.

    if @argv.empty? && @options.iri_file.nil?
      print('no named ontologies\n')
      return 1
    end

    result = battery.run
    
    # Write output.

    File.open(@options.output_file, 'w') do |fo|
      fo.puts result
    end
    
    # Exit.
        
    return 0
    
  end
  
end
