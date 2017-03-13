#!/usr/bin/env jruby -J-Xmx768m

$LOAD_PATH << "#{File.dirname(__FILE__)}/../lib"

require "cloveropts"
options = cloveroptions()

require options.cloverjar
require "benchmark"
require "performance/cloverdb"

5.times {
  puts Benchmark.measure {CloverDatabase.load(options.database)}
}