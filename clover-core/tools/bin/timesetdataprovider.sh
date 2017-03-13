#!/usr/bin/env jruby -J-Xmx768m

$LOAD_PATH << "#{File.dirname(__FILE__)}/../lib"

require "cloveropts"
options = cloveroptions()

require options.cloverjar
require "benchmark"
require "performance/cloverdb"

db = CloverDatabase.loadWithCoverage(options.database)

tests = db.getCoverageData().getTestCases()

model = db.getFullModel
model2 = model.copy()

work = Proc.new do
    tests.each_with_index do |tci,i|
        if i < 500
            model2.setDataProvider(db.getCoverageData().getUniqueCoverageFrom(java.util.Collections.singleton(tci)))
        end
    end
end

puts "Calculated unique coverage for 500 tests, called ProjectInfo.setDataProvider() for each:\n#{Benchmark.measure &work}"

work = Proc.new do
    tests.each_with_index do |tci,i|
        if i < 500
            model2.setDataProvider(db.getCoverageData().getUniqueCoverageFrom(java.util.Collections.singleton(tci)))
            model2.getMetrics()
            model2.getRawMetrics()
        end
    end
end

puts "Calculated unique coverage for 500 tests, called ProjectInfo.setDataProvider() for each and requested raw and filtered metrics:\n#{Benchmark.measure &work}"