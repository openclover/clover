require 'date'
require 'enumerator'
require 'tempfile'
require 'ftools'

class Repository
  attr_reader :url, :username, :password
  
  def initialize(url, username, password)
    @url = url
    @username = username
    @password = password
  end
  
  def each_changeset_in(range, &proc)
    changesets(%x[svn log --username #{@username} --password #{@password} -r {#{range.start}}:{#{range.finish}} #{@url}]).uniq.sort.each { |revision| proc.call(ChangeSet.new(self, revision))}
  end

  def checkout(at, where)
    puts "Checking out #{@url} to #{where} as at start of #{at}"
    command = "svn checkout --username #{@username} --password #{@password} -r #{at} #{@url} #{where}"
    puts "Running \"#{command}\""
    system "svn checkout --username #{@username} --password #{@password} -r #{at} #{@url} #{where} >> #{WORKING_DIR}/FE.svnputs 2>&1"
    system "cp #{BUILD_SCRIPTS_DIR}/build.xml.start #{where}/build.xml > /dev/null 2>&1"
    Workspace.new(self, where)
  end
      
  private

  def changesets(svn_output)
    Enumerable::Enumerator.new(svn_output, :each_line).inject([]) do |list, line|
      match = /^r([0-9]+)\s\|.*/.match(line)
      list << match[1] unless match.nil?
      list
    end
  end
end

class Workspace
  attr_reader :repository, :path
  
  def initialize(repository, path)
    @repository = repository
    @path = path
  end
  
  def build_for(changeset, targets, &proc)
    puts "Updating #{@path} to revision #{changeset.revision}"
    system "svn update --username #{@repository.username} --password #{@repository.password} -r #{changeset.revision} #{@path} >> #{WORKING_DIR}/FE.svnputs 2>&1"
    if File.exists?("#{BUILD_SCRIPTS_DIR}/build.xml.#{changeset.revision}")
        puts "Updating #{@path}/build.xml for revision #{changeset.revision}"
        system "cp #{BUILD_SCRIPTS_DIR}/build.xml.#{changeset.revision} #{@path}/build.xml > /dev/null 2>&1"
    end
    ant(targets) {|workspace, logfile, duration| proc.call(workspace, changeset, logfile, duration)}
  end
  
  def ant(targets, &proc)
    logfile = Tempfile.new("FE.buildlog")
    start = Time.now
    command = "cd #{@path} ; export ANT_OPTS=#{ANT_OPTS} ; #{ANT_171_HOME}/bin/ant #{targets} | tee #{logfile.path} >> #{WORKING_DIR}/FE.antlog 2>&1"
    puts "Running \"#{command}\""
    system "#{command}"
    if (proc.nil?)
      self
    else
      proc.call(self, logfile, Time.now - start)
    end
  end
end

class DateRange
  attr_reader :start, :finish
  
  def initialize(start, finish)
    @start = start
    @finish = finish
  end
end

class ChangeSet
  attr_reader :revision, :repository
  
  def initialize(repository, revision)
    @repository = repository
    @revision = revision
  end
end

class Object
  def log(message)
    puts message
  end
end

class TestResults
  attr_reader :run, :failures, :errors
  
  def initialize
    @run = @failures = @errors = 0
  end
  
  def add(run, failures, errors)
    @run = @run + run
    @failures = @failures + failures
    @errors = @errors + errors
  end
end

class Build
  FAILED = 0
  SUCCEEDED = 1
  
  LOG_LINE_PROCESSOR = Proc.new do |build, line|
    case line
    when /.*Tests run: ([0-9]+), Failures: ([0-9]+), Errors: ([0-9]+).*/
      build.tests.add($1.to_i, $2.to_i, $3.to_i)
    when /.*TEST (\S+) FAILED.*/
      build.failures << $1
    when /.*BUILD SUCCESSFUL.*/
      build.status = Build::SUCCEEDED
    when /.*BUILD FAILED.*/
      build.status = Build::FAILED
    end
    build
  end

  LOG_SUMMARISER = Proc.new do |workspace, changeset, logfile, duration|
    Enumerable::Enumerator.new(logfile, :each_line).inject(Build.new(workspace, changeset, duration), &Build::LOG_LINE_PROCESSOR)
  end
  
  attr_reader :workspace, :changeset, :tests, :duration, :failures
  attr_accessor :status
  
  def initialize(workspace, changeset, duration)
    @workspace = workspace
    @changeset = changeset
    @duration = duration
    @failures = []
    @tests = TestResults.new
    @status = FAILED
  end
  
  def succeeded?
    status == SUCCEEDED
  end
end

class BuildComparison
  attr_reader :changeset, :optimised, :unoptimised
  
  def initialize(changeset, optimised, unoptimised)
    @changeset = changeset
    @optimised = optimised
    @unoptimised = unoptimised
  end
end
