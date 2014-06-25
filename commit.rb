#!/usr/bin/env ruby

require 'js_base'
require 'trollop'
require 'js_base/text_editor'

class ProgramException < Exception; end

class Commit

  ANDROID_PROJECTS = "RBuddyApp"
  GIT_STATE_FILENAME = ".commit_state"
  GIT_MESSAGE_FILENAME = ".commit_git_message"
  COMMIT_MESSAGE_FILENAME = ".commit_editor_message"
  COMMIT_MESSAGE_TEMPLATE=<<-EOS

Issue #

# Enter a commit message above, including an issue number on its own line (e.g., Issue #42)
#
#
  EOS

  def initialize
    @options = nil
  end

  def run(argv)

    @options = parse_arguments(argv)
    @detail = @options[:detail]
    @verbose = @options[:verbose] || @detail
    @current_git_state = nil
    @previous_git_state = nil
    @saved_directory = Dir.pwd

    begin

      read_old_git_state
      determine_current_git_state

      passed_tests = false

      # puts "...comparing current to previous states" if @verbose

      # puts "equal=",(@current_git_state.eql?(@previous_git_state))
      # puts "equal=",(@current_git_state.strip ==@previous_git_state.strip)

      if @current_git_state != @previous_git_state
        puts "...states differ, running unit tests" if @verbose

        run_java_tests if @options[:java]
        run_android_tests if @options[:android]

        # Only update the state if it passed all tests
        update_old_git_state
        passed_tests = true

        update_old_git_state
      else
        passed_tests = true
      end

      if commit_required
        perform_commit
      end

    rescue ProgramException => e
      puts "*** Aborted!  #{e.message}"
      exit 1
    ensure
      Dir.chdir(@saved_directory)
    end
  end

  def commit_required
    return !(@current_git_state.empty?)
  end

  def read_old_git_state
    if @options[:clean]
      FileUtils.rm(GIT_STATE_FILENAME)
    end
    @previous_git_state = FileUtils.read_text_file(GIT_STATE_FILENAME,"")
  end

  def determine_current_git_state

    # Use full diff to determine if previous results are still valid
    @current_git_state,_ = scall("git diff -p")

    # Use brief status to test for untracked files and to report to user
    state,_= scall("git status -s")

    if state.include?('??')
      state,_ = scall("git status")
      raise ProgramException,"Unexpected repository state:\n#{state}"
    end
  end

  def update_old_git_state
    FileUtils.write_text_file(GIT_STATE_FILENAME,@current_git_state)
  end

  def edit_commit_message
    if !File.exist?(COMMIT_MESSAGE_FILENAME)
      status,_ = scall("git status")
      status = status.split("\n").collect{|x| "# #{x}"}.join("\n")
      FileUtils.write_text_file(COMMIT_MESSAGE_FILENAME,COMMIT_MESSAGE_TEMPLATE+status)
    end

    TextEditor.new(COMMIT_MESSAGE_FILENAME).edit

    m = FileUtils.read_text_file(COMMIT_MESSAGE_FILENAME)
    m = m.strip
    lines = m.split("\n").collect{|x| x.strip}
    lines = lines.keep_if{|x| !(x.empty? ||  x.start_with?('#'))}
    return nil if lines.empty?
    lines.join("\n")
  end


  def perform_commit
    m = edit_commit_message
    raise(ProgramException,"Commit message empty") if !m

    if !(m =~ /Issue #\d+(\n|$|\w*:)/)
      raise(ProgramException,"No 'Issue #' found in commit message: [#{m}]")
    end

    FileUtils.write_text_file(GIT_MESSAGE_FILENAME,m)
    if system("git commit -a --file=#{GIT_MESSAGE_FILENAME}")
      FileUtils.rm(COMMIT_MESSAGE_FILENAME)
    else
       raise(ProgramException,"Commit aborted")
    end
  end

  def parse_arguments(argv)
    p = Trollop::Parser.new do
      banner <<-EOS
      Runs unit tests, generates commit for RBuddy project
      EOS
      opt :clean, "clean projects before running tests"
      opt :detail, "display lots of detail"
      opt :verbose, "display progress"
      opt :java,   "include Java projects", :default=>true
      opt :android,"include Android projects",:default=>true
    end

    Trollop::with_standard_exception_handling p do
      p.parse argv
    end
  end

  def run_android_tests
    warning "Not running android tests"
    return if true
    ANDROID_PROJECTS.split.each do |project_root|
      proj_main = File.join(@saved_directory,project_root)
      proj_test = proj_main + "Test"
      echo "...Android project: #{project_root}"

      Dir.chdir(proj_test)

      runcmd("ant clean","...cleaning") if @options[:clean]
      runcmd("ant debug","...building")
      runcmd("ant debug install","...installing")
      runcmd("ant test","...testing")

      Dir.chdir(@saved_directory)

    end
  end

  def run_java_tests
    runcmd("ant test_all_projects","...running Java project tests")
  end

  def runcmd(cmd,message)
    if !@verbose
      echo message
    else
      echo(sprintf("%-40s (%s)",message,cmd))
    end
    output,success = scall(cmd,false)
    if @detail || !success
      puts output
      puts
    end
    if !success
      s = cmd
      if message
        s = "(#{message}) #{s}"
        raise ProgramException,"Problem executing command: #{s}"
      end
    end
    [output,success]
  end

  def echo(msg)
    puts msg
  end

end


if __FILE__ == $0
  Commit.new.run(ARGV)
end
