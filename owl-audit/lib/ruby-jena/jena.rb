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
#    Jena module for accessing Jena API vi JRuby.
#
#++

require 'java'

require 'Jena/commons-codec-1.6.jar'
require 'Jena/httpclient-4.2.3.jar'
require 'Jena/httpcore-4.2.2.jar'
require 'Jena/jcl-over-slf4j-1.6.4.jar'
require 'Jena/jena-arq-2.10.1.jar'
require 'Jena/jena-core-2.10.1.jar'
require 'Jena/jena-iri-0.9.6.jar'
require 'Jena/jena-tdb-0.10.1.jar'
require 'Jena/log4j-1.2.16.jar'
require 'Jena/slf4j-api-1.6.4.jar'
require 'Jena/slf4j-log4j12-1.6.4.jar'
require 'Jena/xercesImpl-2.11.0.jar'
require 'Jena/xml-apis-1.4.01.jar'

java_import com.hp.hpl.jena.query.DatasetAccessorFactory
java_import com.hp.hpl.jena.query.QueryExecutionFactory
java_import com.hp.hpl.jena.query.ParameterizedSparqlString
java_import com.hp.hpl.jena.update.UpdateExecutionFactory
java_import com.hp.hpl.jena.rdf.model.ModelFactory
java_import com.hp.hpl.jena.rdf.model.ResourceFactory

module Jena
  
  # Define constants.
  
  DEFAULT_HOST = ENV['JENA_HOST'] || 'localhost'
  DEFAULT_PORT = ENV['JENA_PORT'] || '3030'
  DEFAULT_DATASET = ENV['JENA_DATASET'] || 'imce-ontologies'

  FUSEKI_SERVICES = %w{data query update}
  
end

# Helper function for constructing URI references.

class String
  def to_uriref
    "<#{self}>"
  end
end

# Helper function for SPARQL filter clauses.

module Enumerable
  def equal_any?(var)
    '(' + map { |val| "#{var} = #{val}" }.push('false').join(' || ') + ')'
  end
end

# Helper functions.

class Java::ComHpHplJenaSparqlCore::ResultBinding
  def [](key)
    get(key)
  end
  def method_missing(method)
    get(method.to_s)
  end
end

class Java::ComHpHplJenaQuery::QuerySolutionMap
  def [](key)
    get(key)
  end
  def method_missing(method, *args)
    m = method.to_s.match(/([^=]*)(=?)/)
    method = m[1]
    unless m[2] == '='
      get(method)
    else
      add(method, *args)
    end
  end
end

class Java::ComHpHplJenaRdfModelImpl::ResourceImpl
  def eql?(other)
    equals(other)
  end
  def hash
    (is_anon ? get_id : get_uri).hash
  end
  def to_qname(nsm)
    ns = get_name_space
    ln = get_local_name
    prefixes = nsm.keys.select { |p| ns == nsm[p] }
    unless prefixes.empty?
      return "#{prefixes.first}:#{ln}"
    else
      return self
    end
  end
end

class Java::ComHpHplJenaRdfModelImpl::LiteralImpl
  def true?
    to_string == 'true^^http://www.w3.org/2001/XMLSchema#boolean'
  end
end
