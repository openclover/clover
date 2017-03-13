require 'fe_settings'
require 'fe_model'
require 'fe_cli'

File.open("#{WORKING_DIR}/FE.builds", "r") { |f| puts "Build summary from #{f.path}"; puts(Marshal.load(f)) }
