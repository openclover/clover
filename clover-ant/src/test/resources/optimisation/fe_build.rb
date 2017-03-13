require "fe_settings"
require "fe_model"
require "fe_cli"

repo = Repository.new(SVN_URL, USERNAME, PASSWORD)

# (A)
# PERFORM ALL BUILDS BETWEEN START DATE AND FINISH DATE

start = Date.parse("2008-09-03")
finish = Date.parse("2008-09-04")

range = DateRange.new(start, finish)
optimized = repo.checkout("{#{range.start}}", "#{WORKING_DIR}/FE.optimized").ant("clean -Dclover.jar=#{CLOVER_JAR}")
unoptimized = repo.checkout("{#{range.start}}", "#{WORKING_DIR}/FE.unoptimized").ant("clean -Dclover.jar=#{CLOVER_JAR}")
revisions = Enumerable::Enumerator.new(repo, :each_changeset_in, range)

# (B)
# PERFORM ALL BUILDS FOR A KNOWN SET OF REVISIONS
#
# revision = "19572"
# optimized = repo.checkout(revision, "#{WORKING_DIR}/FE.optimized").ant("clean")
# unoptimized = repo.checkout(revision, "#{WORKING_DIR}/FE.unoptimized").ant("clean")
# revisions = [ChangeSet.new(repo, revision)]

comparisons = revisions.inject([]) do |results, changeset|
  results <<
    BuildComparison.new(changeset,
      optimized.build_for(changeset, "with.clover cru clean run.tests test.cru clover.snapshot -Dclover.optimize=true -Dtest.printsummary=yes -Dclover.jar=#{CLOVER_JAR} -Dclover.span=20m -Dp4.exe=#{P4_EXE} -Dp4d.exe=#{P4D_EXE}", &Build::LOG_SUMMARISER),
      unoptimized.build_for(changeset, "clean cru with.clover run.tests test.cru -Dtest.printsummary=yes -Dclover.jar=#{CLOVER_JAR} -Dclover.span=20m -Dp4.exe=#{P4_EXE} -Dp4d.exe=#{P4D_EXE}", &Build::LOG_SUMMARISER))
  puts "Writing updated build comparison data to #{WORKING_DIR}/FE.builds for #{changeset}"
  File.open("#{WORKING_DIR}/FE.builds", "w") { |f| f.puts(Marshal.dump(results)) }
  
  results
end

comparisons.each { |comparison| puts comparison }
