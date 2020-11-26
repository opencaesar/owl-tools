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
#    Adaptation of Ruby Application library with preferred logging behavior.
#
#++

require 'logger'
require 'logger-application'

unless Logger::Application.respond_to?(:logger)
  class Logger::Application
    def logger
      @log
    end
  end
end

class Application < Logger::Application
  
  require 'optparse'
  require 'ostruct'
  
  DEFAULT_LOG_DEVICE = STDERR
  DEFAULT_LOG_LEVEL = 'FATAL'
  
  def initialize(name, argv = ARGV)
    
    @argv = argv
    super(name)
    logger.level = Logger.const_get(DEFAULT_LOG_LEVEL)
    configure_options

  end
  
  attr_reader :argv, :options, :option_parser
  
  def run

    @option_parser.parse!(@argv)
    configure_log
    log(INFO, "Start of #{@appname}.")
    log(DEBUG, "options: #{@options}")
    log(DEBUG, "argv: #{@argv}")
    
  end
  
  def get_binding
    binding
  end
  
  private
  
  def configure_options
    
    @options = OpenStruct.new
    @option_parser = OptionParser.new do |op|
  
      op.banner = "usage: #{@name} [options]"
    
      @options.log_file = nil
      op.on('--log-file FILE','log file') do |file|
        @options.log_file = file
      end
  
      @options.log_level = DEFAULT_LOG_LEVEL
      levels = %w{FATAL INFO WARN DEBUG}
      op.on('--log-level LEVEL', levels, "set log level LEVEL (#{levels.join(' ')}) default #{DEFAULT_LOG_LEVEL}") do |v|
        @options.log_level = v
      end
    
      levels.each do |level|
        op.on("--#{level.downcase}", "set log level to #{level}") do
          @options.log_level = level
        end
      end
      
      op.on_tail("-h", "--help", "show this message") do
        puts opts
        exit
      end
      
    end
    
  end
  
  def configure_log
    
    set_log(@options.log_file || DEFAULT_LOG_DEVICE)
    logger.datetime_format = '%Y-%m-%d %H:%M:%S '
    logger.level = Logger.const_get(@options.log_level)
    
  end
  
end

if __FILE__ == $0
  
  # https://bugs.eclipse.org/bugs/show_bug.cgi?id=323736

  unless defined?(Test::Unit::UI::SILENT)
    module Test
      module Unit
        module UI
          SILENT = false
        end

        class AutoRunner
          def output_level=(level)
            self.runner_options[:output_level] = level
          end
        end
      end
    end
  end
  
  unless Test::Unit::TestCase.respond_to?(:assert_empty)
    class Test::Unit::TestCase
      def assert_empty(x)
        assert(x.empty?)
      end
    end
  end

  class MyApplication < Application
  end
  
  class TestApplication < Test::Unit::TestCase
    
    APPLICATION_NAME = 'test application'
    LOG_FILE = 'log_file.log'
    ARGUMENTS = %w{a b c d e f}

    def test_initialize
      
      @app = MyApplication.new(APPLICATION_NAME, [])
      @app.start
      assert_equal(APPLICATION_NAME, @app.appname)
      assert_instance_of(OpenStruct, @app.options)
      assert_instance_of(OptionParser, @app.option_parser)
      assert_empty(@app.argv)
        
    end
    
     def test_common_option_defaults
      
      @app = MyApplication.new(APPLICATION_NAME, [])
      @app.start
      assert_nil(@app.options.log_file)
      assert_equal('FATAL', @app.options.log_level)
      assert_equal(Logger::FATAL, @app.logger.level)  
      assert_empty(@app.argv)
      
    end
    
    def test_arguments
      @app = MyApplication.new(APPLICATION_NAME, ARGUMENTS)
      @app.start
      assert_equal(ARGUMENTS, @app.argv)
    end
    
    def test_common_option_values_1
      
    @app = MyApplication.new(APPLICATION_NAME, "--log-file #{LOG_FILE} --log-level DEBUG".split + ARGUMENTS)
      @app.start
      assert_equal(LOG_FILE, @app.options.log_file)
      assert_equal('DEBUG', @app.options.log_level)
      assert_equal(Logger::DEBUG, @app.logger.level)
      assert_equal(ARGUMENTS, @app.argv)
      
    end
    
    def test_common_option_values_2
      
      @app = MyApplication.new(APPLICATION_NAME, '--info'.split + ARGUMENTS)
      @app.start
      assert_equal('INFO', @app.options.log_level)
      assert_equal(Logger::INFO, @app.logger.level)
      assert_equal(ARGUMENTS, @app.argv)
     
    end
    
    def test_custom_options
      
      @app = MyApplication.new(APPLICATION_NAME, '--custom-option ABC1234'.split + ARGUMENTS)
      @app.option_parser.on('--custom-option VALUE','custom option') do |v|
        @app.options.custom_option = v
      end
      @app.start
      assert_equal('ABC1234', @app.options.custom_option)
      assert_equal(ARGUMENTS, @app.argv)

    end

  end

end
