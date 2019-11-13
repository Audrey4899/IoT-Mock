#!/usr/bin/env ruby

require 'json'
require 'yaml'

class Rule
    attr_reader :type
    attr_reader :request_method, :request_path, :request_headers, :request_body
    attr_reader :response_status, :response_headers, :response_body
    attr_accessor :rule_hash

    def initialize(rule_type, request_method, request_path, request_headers, request_body, response_status, response_headers, response_body)
        @type = rule_type
        @request_method = request_method
        @request_path = request_path
        @request_headers = request_headers
        @request_body = request_body
        @response_status = response_status
        @response_headers = response_headers
        @response_body = response_body
    end

    def equals?(rule)
        rule.instance_variables.each do |k|
            return false unless rule.instance_variable_get(k) == self.instance_variable_get(k)
        end
        true
    end

    def get_rule_hash
        @rule_hash = {
            "type"=> @type,
            "req"=> {
                "method"=> @request_method,
                "path"=> @request_path
            }
        }
        unless request_headers.nil?
            @rule_hash["req"].merge!({"headers"=> {}})
            @rule_hash["req"]["headers"].merge!(@request_headers)
        end
        unless request_body.nil? || request_body.empty?
            @rule_hash["req"].merge!({"body"=> @request_body})
        end
        if @type == "inOut"
            @rule_hash.merge!({"res"=> {}})
            @rule_hash["res"].merge!({"status"=> @response_status.to_i}) 
            unless response_headers.nil?
                @rule_hash["res"].merge!({"headers"=> {}})
                @rule_hash["res"]["headers"].merge!(@response_headers)
            end
            unless response_body.nil? || response_body.empty?
                @rule_hash["res"].merge!({"body"=> @response_body})
            end
        end
        return @rule_hash
    end
end

class Generator
    attr_reader :file_name, :file_type
    attr_accessor :file_data, :host, :paths, :rules
    attr_accessor :request_host, :request_dest, :request_method, :request_path, :request_headers, :request_body
    attr_accessor :response_host, :response_dest, :response_status, :response_headers, :response_body

    def initialize(file_name: 'rules', file_type: 'json')
        @file_name = file_name
        @file_type = file_type
    end

    def read_file(path)
        file = File.open(path)
        @file_data = file.readlines.map(&:chomp)
        @paths = Array.new
        file_data.each do |line|
            address1 = line.match(/Host=(([0-9]{1,3}\.)+[0-9]{1,3})/)
            address2 = line.match(/Dest=(([0-9]{1,3}\.)+[0-9]{1,3})/)
            unless paths.include? address1[1]
                @paths << address1[1]
            end
            unless paths.include? address2[1]
                @paths << address2[1]
            end
        end
    end

    def ask_hostadress
        puts "Choose the host adress: "
        paths.each { |line| puts line }
        puts ""
        @host = $stdin.gets.chomp
        while !paths.include? host
            puts "Error: the chosen adress doesn't exist.\nChoose the host adress: "
            paths.each { |line| puts line }
            puts ""
            @host = $stdin.gets.chomp
            puts ""
        end
        puts "=> " + host + " has been chosen like host adress."
    end

    def get_request_composition(requests)
        request = requests[0].split("@")
        @request_object = request[0][/^.*\(/].tr("(","")
        request[0][/^.*\(/] = ""
        request[request.length-1][/\)/] = ""
        key_value = {}
        request.each do |string|
            unless string.split("=")[0] == "Uri"
                key_value[string.split("=")[0]] = string.split("=")[1]
            else
                key_value["Uri"] = string.split("Uri=")[1]
            end
        end
        @request_host = key_value["Host"]
        key_value.delete("Host")
        @request_dest = key_value["Dest"]
        key_value.delete("Dest")
        @request_method = key_value["Verb"]
        key_value.delete("Verb")
        @request_path = key_value["Uri"]
        key_value.delete("Uri")
        @request_headers = {}
        @request_body = nil
        while key_value.size != 0
            if key_value.keys[0] == "contents"
                @request_body = key_value.values[0]
            else
                @request_headers[key_value.keys[0]] = key_value.values[0]
            end
            key_value.delete(key_value.keys[0])
        end
        if @request_headers.size < 1
            @request_headers = nil
        end
    end

    def get_response_composition(responses)
        response = responses[0].split("@")
        @response_object = response[0][/^.*\(/].tr("(","")
        response[0][/^.*\(/] = ""
        response[response.length-1][/\)/] = ""
        key_value = {}
        response.each do |string|
            unless string.split("=")[0] == "contents"
                key_value[string.split("=")[0]] = string.split("=")[1]
            else
                key_value["contents"] = string.split("contents=")[1]
            end
        end
        @response_host = key_value["Host"]
        key_value.delete("Host")
        @response_dest = key_value["Dest"]
        key_value.delete("Dest")
        @response_status = key_value["status"]
        key_value.delete("status")
        @response_response = key_value["response"]
        key_value.delete("response")
        @response_headers = {}
        @response_body = nil
        while key_value.size != 0
            if key_value.keys[0] == "contents"
                @response_body = key_value.values[0]
            else
                @response_headers[key_value.keys[0]] = key_value.values[0]
            end
            key_value.delete(key_value.keys[0])
        end
        if @response_headers.size < 1
            @response_headers = nil
        end
    end

    def save_rule
        rules_hash = Array.new
        rules.each do |rule|
            rules_hash << rule.get_rule_hash
        end
        File.open(file_name + "." + file_type, "w") do |file|
            if file_type == "json"
                file.puts JSON.pretty_generate(rules_hash)
            else
                file.write(rules_hash.to_yaml)
            end
        end
    end

    def create_rules
        #@host = "192.168.43.76"
        requests = Array.new
        responses = Array.new
        file_data.each do |line|
            if line.include? "Verb="
                requests << line
            elsif line.include? "status="
                responses << line
            else
                puts "Error when dispatching resquests/responses."
                exit
            end
        end
        @rules = Array.new
        while requests.length != 0 && responses.length != 0
            get_request_composition(requests)
            get_response_composition(responses)
            if request_dest == host && response_host == host
                rule_type = "inOut"
            elsif request_host == host
                rule_type = "outIn"
                @response_status = nil
                @response_headers = nil
                @response_body = nil
            else
                puts "Error when creating rules(rule type unknown)."
                exit
            end
            if rule_type == "outIn"
                fullpath = request_dest + request_path
                rule = Rule.new(rule_type, request_method, fullpath, request_headers, request_body, response_status, response_headers, response_body)
            else
                rule = Rule.new(rule_type, request_method, request_path, request_headers, request_body, response_status, response_headers, response_body)
            end
            if rules.length > 0
                exist = false
                rules.each do |existing_rule|
                    if existing_rule.equals? rule
                        exist = true
                        break
                    end
                end
                if exist == false
                    @rules << rule
                end
            else
                @rules << rule
            end
            requests.shift
            responses.shift
        end
        save_rule
    end

    def start(path)
        read_file(path)
        ask_hostadress
        create_rules
    end
end

#Main
case ARGV.length
when 1
    generator = Generator.new
    generator.start(ARGV[0])
when 2
    if ARGV[1] == "yaml" || ARGV[1] == "json"
        generator = Generator.new(:file_type => ARGV[1])
    else
        generator = Generator.new(:file_name => ARGV[1])
    end
    generator.start(ARGV[0])
when 3
    if ARGV[2] == "yaml" || ARGV[2] == "json" 
        generator = Generator.new(:file_name => ARGV[1], :file_type => ARGV[2])
        generator.start(ARGV[0]) 
    else
        puts "How to use the script: (default destination file name and type: 'rules.json')\n=> generator.rb <path_to_file> (optional: <dest_file_name> <dest_file_type: 'yaml' or 'json'>)\n"
        exit 
    end
else
    puts "How to use the script: (default destination file name and type: 'rules.json')\n=> generator.rb <path_to_file> (optional: <dest_file_name> <dest_file_type: 'yaml' or 'json'>)\n"
    exit 
end
