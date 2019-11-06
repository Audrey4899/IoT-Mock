#!/usr/bin/env ruby

require 'json'

class Rule
    attr_reader :type
    attr_reader :request_method, :request_path, :request_headers, :request_body
    attr_reader :response_status, :response_headers, :response_body

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
        @host = gets.chomp
        while !paths.include? host
            puts "Error: the chosen adress doesn't exist.\nChoose the host adress: "
            paths.each { |line| puts line }
            puts ""
            @host = gets.chomp
            puts ""
        end
        puts "=> " + host + " has been chosen like host adress."
    end

    def get_request_composition(requests)
        request = requests[0].match(/(Host=(([0-9]{1,3}\.)+[0-9]{1,3})).*(Dest=(([0-9]{1,3}\.)+[0-9]{1,3})).*(Verb=([A-Z]+)).([^\s]+)/)
        @request_host = request[2]
        @request_dest = request[5]
        @request_method = request[8]
        @request_path = request[9]

        unless requests[0].match(/HTTP\/.+\s\((.*)\)/).nil?
            request = requests[0].match(/HTTP\/.+\s\((.*)\)/)
            @request_headers = request[1]
        end
        unless requests[0].match(/;;(.*)/).nil?
            request = requests[0].match(/;;(.*)/)
            @request_body = request[1]
        end
    end

    def get_response_composition(responses)
        response = responses[0].match(/(Host=(([0-9]{1,3}\.)+[0-9]{1,3})).*(Dest=(([0-9]{1,3}\.)+[0-9]{1,3})).*(status=([0-9]{3}))/)
        @response_host = response[2]
        @response_dest = response[5]
        @response_status = response[8]

        unless responses[0].match(/HTTP\/.+\s\((.*)\)/).nil?
            response = responses[0].match(/HTTP\/.+\s\((.*)\)/)
            @response_headers = response[1]
        end
        unless responses[0].match(/;;(.*)/).nil?
            response = responses[0].match(/;;(.*)/)
            @response_body = response[1]
        end
    end

    def rules_to_json
        rules_hash = Array.new
        rules.each do |rule|
            hash = {}
            rule.instance_variables.each { |var|
                hash[var.to_s.delete("@")] = rule.instance_variable_get(var)
            }
            rules_hash << hash
        end
        puts JSON.generate(rules_hash)
    end

    def create_rules
        @host = "192.168.43.76" #To test code(only with "brut" file)
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
        idx = 0
        while requests.length != 0 && responses.length != 0
            get_request_composition(requests)
            get_response_composition(responses)
            if request_dest == host && response_host == host #=> rule inOut
                rule_type = "inOut"
            elsif request_host == host #=> rule out
                rule_type = "outIn"
                response_status = nil
                response_headers = nil
                response_body = nil
            else
                puts "Error when creating rules(rule type unknown)."
                exit
            end
            rule = Rule.new(rule_type, request_method, request_path, request_headers, request_body, response_status, response_headers, response_body)
            unless rules.include? rule
                @rules << rule
            end
            requests.shift
            responses.shift
        end
        rules_to_json
    end

    def start(path)
        read_file(path)
        #ask_hostadress
        create_rules
    end
end


=begin
if ARGV.length != 3
    if ARGV.length == 0
        generator = Generator.new
    else
        puts "Use: generator.rb dest_filename dest_filetype"
        puts "Or generator.rb => for default file: rules.json"
        exit
    end
else
    generator = Generator.new(:file_name => ARGV[0], :file_type => ARGV[1])
end
=end


generator = Generator.new
generator.start("./brut")
