#!/usr/bin/env ruby

require 'json'

class Rule
    @@array = Array.new
    attr_reader :type, :timeout, :repeat, :interval
    attr_reader :request_method, :request_path, :request_headers, :request_body
    attr_reader :response_status, :response_headers, :response_body

    def self.all_instances
        @@array
    end

    def initialize(type)
        @type = type
        @@array << self
    end
end

class Generator
    attr_reader :file_name, :file_type
    attr_accessor :file_data, :host, :paths
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
            if !paths.include? address1[1]
                @paths << address1[1]
            end
            if !paths.include? address2[1]
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
        @request_host = request[1]
        @request_dest = request[4]
        @request_method = request[7]
        @request_path = request[8]

        if !requests[0].match(/HTTP\/.+\s\((.*)\)/).nil?
            request = requests[0].match(/HTTP\/.+\s\((.*)\)/)
            @request_headers = request[0]
        end
        if !requests[0].match(/;;(.*)/).nil?
            request = requests[0].match(/;;(.*)/)
            @request_body = request[0]
        end
    end

    def get_response_composition(responses)
        response = responses[0].match(/(Host=(([0-9]{1,3}\.)+[0-9]{1,3})).*(Dest=(([0-9]{1,3}\.)+[0-9]{1,3})).*(status=([0-9]{3}))/)
        @response_host = response[1]
        @response_dest = response[4]
        @response_status = response[7]

        if !responses[0].match(/HTTP\/.+\s\((.*)\)/).nil?
            response = responses[0].match(/HTTP\/.+\s\((.*)\)/)
            @response_headers = response[0]
        end
        if !responses[0].match(/;;(.*)/).nil?
            response = responses[0].match(/;;(.*)/)
            @response_body = response[0]
        end
    end

    def create_rules
        requests = Array.new
        responses = Array.new
        file_data.each do |line|
            if line.include? "Verb="
                requests << line
            elsif line.include? "status="
                responses << line
            else
                puts "Error when creating rules."
                exit
            end
        end
        while requests.length != 0 && responses.length != 0
            get_request_composition(requests)
            get_response_composition(responses)
            if request_dest == host && response_host == host #=> rule inOut
                Rule.new("inOut", request_path, request_method, response_status, response_headers, response_body)
            elsif request_host == host && response_dest == host #=> rule outIn
                Rule.new("outIn", request_dest + request_path , request_method)
            end
            requests.shift
            responses.shift
        end
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
