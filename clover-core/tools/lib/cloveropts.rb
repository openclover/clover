require "optparse"
require "ostruct"

def cloveroptions(args = ARGV)
  options = OpenStruct.new
  OptionParser.new do |opts|
    opts.on('-j', '--cloverjar JARFILE', 'Load Clover classes from JARFILE') do |jar|
      if File.exists?(jar)
        options.cloverjar = jar
      else
        raise OptionParser::ParseError, "#{jar} does not exist"
      end
    end
    opts.on('-d', '--database DATABASE', 'Load the Clover DATABASE') do |db|
      if File.exists?(db)
        options.database = db
      else
        raise OptionParser::ParseError, "#{db} does not exist"
      end
    end
    opts.on_tail("-h", "--help", "Show this message") do
      puts opts
      exit
    end
  end.parse!(args)
  options
end