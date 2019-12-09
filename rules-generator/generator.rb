#!/usr/bin/env ruby

require 'json'
require 'yaml'

class Rule
  attr_reader :rule_type
  attr_reader :request_method, :request_path, :request_headers, :request_body
  attr_reader :response_status, :response_headers, :response_body
  attr_accessor :rule_hash

  def initialize(rule_type, request_method, request_path, request_headers, request_body, response_status, response_headers, response_body)
    @rule_type = rule_type
    @request_method = request_method
    @request_path = request_path
    @request_headers = request_headers
    @request_body = request_body
    @response_status = response_status
    @response_headers = response_headers
    @response_body = response_body
  end

  def equals?(rule)
    rule.instance_variables.each { |k| return false unless rule.instance_variable_get(k) == self.instance_variable_get(k) }
    true
  end

  def rule_to_hash
    @rule_hash = {
      'type' => @rule_type,
      'request' => {
        'method' => @request_method,
        'path' => @request_path
      }
    }
    unless request_headers.nil?
      @rule_hash['request'].merge!('headers' => {})
      @rule_hash['request']['headers'].merge!(@request_headers)
    end
    unless request_body.nil? || request_body.empty?
      @rule_hash['request'].merge!('body' => @request_body)
    end
    if @rule_type == 'inout'
      @rule_hash.merge!('response' => {})
      @rule_hash['response'].merge!('status' => @response_status.to_i)
      unless response_headers.nil?
        @rule_hash['response'].merge!('headers' => {})
        @rule_hash['response']['headers'].merge!(@response_headers)
      end
      unless response_body.nil? || response_body.empty?
        @rule_hash['response'].merge!('body' => @response_body)
      end
    end
    @rule_hash
  end
end

class Generator
  attr_reader :file_name, :file_type
  attr_accessor :file_data, :host, :paths, :rules
  attr_accessor :request_host, :request_dest, :request_method, :request_path, :request_headers, :request_body
  attr_accessor :response_host, :response_dest, :response_status, :response_headers, :response_body

  def initialize(file_name, file_type, host)
    @file_name = file_name
    @file_type = file_type
    @host = host
  end

  def read_file(path)
    @file_data = File.open(path).readlines.map(&:chomp)
    @paths = []
    @file_data.each do |line|
      host_address = line.match(/Source=(([0-9]{1,3}\.)+[0-9]{1,3})/)
      dest_address = line.match(/Dest=(([0-9]{1,3}\.)+[0-9]{1,3})/)
      @paths << host_address[1] unless paths.include?(host_address[1])
      @paths << dest_address[1] unless paths.include?(dest_address[1])
    end
  end

  def ask_hostaddress
    puts 'Choose the host address: '
    @paths.each { |line| puts line }
    puts ''
    @host = $stdin.gets.chomp
    until paths.include?(host)
      puts "Error: the chosen address doesn't exist.\nChoose the host address: "
      paths.each { |line| puts line }
      puts ''
      @host = $stdin.gets.chomp
      puts ''
    end
    puts "=> #{@host} has been chosen as host address."
  end

  def get_request_composition(requests)
    request = requests[0].split('@')
    @request_label = request[0][/^.*\(/].tr('(', '').strip
    request[0][/^.*\(/] = ''
    request[request.length - 1][/\)/] = ''
    key_value = {}
    request.each do |val|
      if val.split('=')[0] == 'Uri'
        key_value['Uri'] = val.split('Uri=')[1]
      else
        vals = val.split('=')
        vals.delete_at(0)
        key_value[val.split('=')[0]] = vals.join('=')
      end
    end
    @request_host = key_value['Source'].strip
    key_value.delete('Source')
    @request_dest = key_value['Dest'].strip
    key_value.delete('Dest')
    @request_method = key_value['Verb'].strip
    key_value.delete('Verb')
    @request_path = key_value['Uri'].strip
    key_value.delete('Uri')
    @request_headers = {}
    @request_body = nil
    until key_value.empty?
      if key_value.keys[0] == 'contents'
        @request_body = key_value.values[0].strip
      else
        @request_headers[key_value.keys[0].strip] = key_value.values[0].strip
      end
      key_value.delete(key_value.keys[0].strip)
    end
    @request_headers = nil if @request_headers.empty?
  end

  def get_response_composition(responses)
    response = responses[0].split('@')
    @response_label = response[0][/^.*\(/].tr('(', '').strip
    response[0][/^.*\(/] = ''
    response[response.length - 1][/\)/] = ''
    key_value = {}
    response.each do |val|
      if val.split('=')[0] == 'contents'
        key_value['contents'] = val.split('contents=')[1]
      else
        vals = val.split('=')
        vals.delete_at(0)
        key_value[val.split('=')[0]] = vals.join('=')
      end
    end
    @response_host = key_value['Source'].strip
    key_value.delete('Source')
    @response_dest = key_value['Dest'].strip
    key_value.delete('Dest')
    @response_status = key_value['status'].strip
    key_value.delete('status')
    @response_response = key_value['response'].strip if !key_value['response'].nil?
    key_value.delete('response')
    @response_headers = {}
    @response_body = nil
    until key_value.empty?
      if key_value.keys[0] == 'contents'
        @response_body = key_value.values[0].strip
      else
        @response_headers[key_value.keys[0].strip] = key_value.values[0].strip
      end
      key_value.delete(key_value.keys[0])
    end
    @response_headers = nil if @response_headers.empty?
  end

  def save_rule
    rules_hash = []
    @rules.each do |rule|
      rules_hash << rule.rule_to_hash
    end
    File.open("#{@file_name}.#{@file_type}", 'w') do |file|
      if @file_type == 'json'
        file.puts JSON.pretty_generate(rules_hash)
      else
        file.write(rules_hash.to_yaml)
      end
    end
  end

  def create_rules
    requests = []
    responses = []
    @file_data.each do |line|
      if line.include?('Verb=')
        requests << line
      elsif line.include?('status=')
        responses << line
      else
        puts 'Error when dispatching resquests/responses.'
        exit
      end
    end
    @rules = []
    while !requests.empty? && !responses.empty?
      get_request_composition(requests)
      get_response_composition(responses)
      if @request_dest == @host && @response_host == @host
        @rule_type = 'inout'
      elsif @request_host == @host
        @rule_type = 'outin'
        @response_status = nil
        @response_headers = nil
        @response_body = nil
      else
        @rule_type = ''
      end
      if @rule_type == 'outin'
        fullpath = "http://#{@request_dest}#{request_path}"
        rule = Rule.new(@rule_type, @request_method, fullpath, @request_headers, @request_body, @response_status, @response_headers, @response_body)
      elsif @rule_type == 'inout'
        rule = Rule.new(@rule_type, @request_method, @request_path, @request_headers, @request_body, @response_status, @response_headers, @response_body)
      end
      if @rule_type != '' && @rules.empty?
        @rules << rule
      elsif @rule_type != '' && !@rules.empty?
        exist = false
        @rules.each do |existing_rule|
          if existing_rule.equals?(rule)
            exist = true
            break
          end
        end
        @rules << rule if exist == false
      end
      requests.shift
      responses.shift
    end
    save_rule
  end

  def start(path)
    read_file(path)
    ask_hostaddress if @host == ''
    create_rules
  end
end

# Main
filepath = nil
hostaddress = nil
filename = nil
filetype = nil
ARGV.each do |arg|
  if arg.include?('-filepath') && filepath.nil?
    filepath = arg.split('=')[1]
  elsif arg.include?('-hostaddress') && hostaddress.nil?
    hostaddress = arg.split('=')[1]
    unless hostaddress.match(/^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$/)
      puts "Error: the address \"#{hostaddress}\" isn't correct."
      exit
    end
  elsif arg.include?('-filename') && filename.nil?
    filename = arg.split('=')[1]
  elsif arg.include?('-filetype') && filetype.nil?
    filetype = arg.split('=')[1]
    unless filetype == 'json' || filetype == 'yaml'
      puts "Error: the existing file types are 'json' or 'yaml'."
      exit
    end
  else
    puts "How to use the script: (default destination file: 'rules.json')\n=> generator.rb -filepath=<path_to_file> [-hostaddress=<host_address>] [-filename=<dest_file_name>] [-filetype=<dest_file_type('yaml' or 'json')>)\n"
    exit
  end
end

if filepath.nil?
  puts "Error: you have to provide a file with formatted frames with the argument'-filepath=...'."
  exit
end

filename = 'rules' if filename.nil?
filetype = 'json' if filetype.nil?
hostaddress = '' if hostaddress.nil?

generator = Generator.new(filename, filetype, hostaddress)
generator.start(filepath)
