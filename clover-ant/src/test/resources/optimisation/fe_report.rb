require 'rubygems'
require 'google_chart'
require 'fe_model'
require 'fe_cli'

# puts "================================================================================"
# puts "Starting optimised/unoptimised test run for ChangeSets to #{SVN_URL} from #{START} to #{FINISH}"
# 
# repo = Repository.new(SVN_URL, USERNAME, PASSWORD)
# range = DateRange.new(START, FINISH)
# optimized = repo.checkout("{#{range.start}}", "#{WORKING_DIR}/FE.optimized").ant("clean")
# unoptimized = repo.checkout("{#{range.start}}", "#{WORKING_DIR}/FE.unoptimized").ant("clean")
# comparisons = Enumerable::Enumerator.new(repo, :each_changeset_in, range).inject([]) do |results, changeset|
#   results <<
#     BuildComparison.new(changeset,
#       optimized.build_for(changeset, "with.clover cru maybe.clean run.tests test.cru clover.snapshot -Dtest.printsummary=yes -Dclover.jar=#{CLOVER_JAR} -Dclover.span=20m", &Build::LOG_SUMMARISER),
#       unoptimized.build_for(changeset, "clean cru with.clover run.tests test.cru -Dtest.printsummary=yes -Dclover.jar=#{CLOVER_JAR} -Dclover.span=20m", &Build::LOG_SUMMARISER))
# end

# repo = Repository.new(SVN_URL, USERNAME, PASSWORD)
# optimized = repo.checkout("19572", "#{WORKING_DIR}/FE.optimized").ant("clean")
# unoptimized = repo.checkout("19572", "#{WORKING_DIR}/FE.unoptimized").ant("clean")
# comparisons = [ChangeSet.new(repo, "19572")].inject([]) do |results, changeset|
#   results <<
#     BuildComparison.new(changeset,
#       optimized.build_for(changeset, "with.clover cru maybe.clean run.tests test.cru clover.snapshot -Dtest.printsummary=yes -Dclover.jar=#{CLOVER_JAR} -Dclover.span=20m", &Build::LOG_SUMMARISER),
#       unoptimized.build_for(changeset, "clean cru with.clover run.tests test.cru -Dtest.printsummary=yes -Dclover.jar=#{CLOVER_JAR} -Dclover.span=20m", &Build::LOG_SUMMARISER))
# end

# puts "Writing comparison data to #{WORKING_DIR}/FE.comparisons"
# File.open("#{WORKING_DIR}/FE.comparisons", "w") { |f| f.puts(Marshal.dump(comparisons)) }
# 
# comparisons.each { |comparison| puts comparison }
# durations = comparisons.inject({}) { |by_revision, comparison| by_revision[comparison.changeset.revision] = [comparison.optimised.duration, comparison.unoptimised.duration] ; by_revision }
# tests_run = comparisons.inject({}) { |by_revision, comparison| by_revision[comparison.changeset.revision] = [comparison.optimised.tests.run, comparison.unoptimised.tests.run] ; by_revision }
# tests_failed = comparisons.inject({}) { |by_revision, comparison| by_revision[comparison.changeset.revision] = [comparison.optimised.tests.failures, comparison.unoptimised.tests.failures] ; by_revision }
# tests_errd = comparisons.inject({}) { |by_revision, comparison| by_revision[comparison.changeset.revision] = [comparison.optimised.tests.errors, comparison.unoptimised.tests.errors] ; by_revision }
# statuses = comparisons.inject({}) { |by_revision, comparison| by_revision[comparison.changeset.revision] = [comparison.optimised.succeeded?, comparison.unoptimised.succeeded?] ; by_revision }
# http://chart.apis.google.com/chart?cht=lxy&chs=500x400&chd=t:0,30,60,70,90,95,100|20,30,40,50,60,70,80|10,30,40,45,52|100,90,40,20,10|-1|5,33,50,55,7&chco=3072F3,ff0000,00aaaa&chls=2,4,1&chm=s,FF0000,0,-1,5|s,0000ff,1,-1,5|s,00aa00,2,-1,5

# optimised_durations = durations.values.map{ |duration| duration[0]}
# unoptimised_durations = durations.values.map{ |duration| duration[1]}
# GoogleChart::LineChart.new('320x200', "Test Duration", false) do |chart|
#   chart.data "Optimised", optimised_durations, '0000ff'
#   chart.data "Unoptimised", unoptimised_durations, '00ff00'
#   chart.axis :y, :range => [0, [optimised_durations.max, unoptimised_durations.max].max/6 ], :color => 'ff00ff', :font_size => 16, :alignment => :center
#   chart.axis :x, :range => [0,6], :color => '00ffff', :font_size => 16, :alignment => :center
# end.to_url

# repo = Repository.new(SVN_URL, USERNAME, PASSWORD)
# optimized = Workspace.new(repo, "#{WORKING_DIR}/FE.optimized")
# puts File.open('/var/folders/J4/J4Lohhs4HVKrcR8-kqcXtk+++TI/-Tmp-/FE.antlog.5092.2') { |file| Build::LOG_SUMMARISER.call(optimized, ChangeSet.new(repo, "12345"), 20*60*1000, file) }

