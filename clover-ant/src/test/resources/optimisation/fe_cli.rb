require 'date'
require 'enumerator'
require 'tempfile'

class Repository
  def to_s
    "#{@url}"
  end
end

class Workspace
  def to_s
    "Workspace #{@path}"
  end
end

class DateRange
  def to_s
    "Range[#{start} - #{finish}]"
  end
end

class ChangeSet
  def to_s
    "#{@repository}@#{@revision}"
  end
end

class TestResults
  def to_s
    "Tests run: #{@run}, Failures: #{@failures}, Errors: #{@errors}."
  end
end

class Build
  def to_s
    <<-STRINGIFIED
      Build of #{@changeset} (#{@status == FAILED ? "FAILED" : "SUCCEEDED"})
      #{@workspace}
      Duration: #{format('%.1f', (@duration.to_f))} secs
      #{@tests}
      Failures: #{@failures.inject("\n") { |out, f| out << "\t\t\t\t#{f}\n"} unless @failures.empty?}
    STRINGIFIED
  end
end

class BuildComparison
  def to_s
    <<-STRINGIFIED
================================================================================
BUILD COMPARISON FOR REVISION #{@changeset.revision}@#{@changeset.repository.url}
Optimised run:
#{@optimised}

Unoptimised run:
#{@unoptimised}"
================================================================================
  STRINGIFIED
  end
end
