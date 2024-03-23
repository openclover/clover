include Java

import 'org.openclover.core.CloverDatabase'
import 'org.openclover.core.CoverageDataSpec'
import 'org.openclover.core.ProgressListener'

class CloverDatabase
  def self.tick(msg)
    puts "#{msg} [#{java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().to_s}]"
  end

  def self.load(path, &block)
    tick "Loading database at '#{path}'"
    db = CloverDatabase.new(path)
    tick "Loaded database at '#{path}'"
    block.call(db) if not block.nil?
    db
  end

  def self.loadWithCoverage(path, spec = CoverageDataSpec.new)
    progressListener = ProgressListener.new
    class << progressListener
      def handleProgress(desc, pc)
        @progress ||= 0
        @last ||= Time.now
        if ((pc * 100) > @progress)
          now = Time.now
          CloverDatabase.tick "#{(pc * 100).to_i}% in #{now - @last}s"
          @progress += 5
          @last = Time.now
        end
      end
    end

    load(path) do |it|
      tick "Loading coverage"
      @last = Time.now
      begin
        it.loadCoverageData(spec, progressListener)
      rescue ArgumentError => e
      rescue NameError => e
        puts "This version of OpenClover does not support progress tracking - no progress will be displayed while loading coverage data"
        it.loadCoverageData(CoverageDataSpec.new)
      end
      tick "Loaded coverage in #{Time.now - @last}s"
      it
    end
  end
end
