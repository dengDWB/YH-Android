#!/usr/bin/env ruby
# encoding: utf-8
require 'settingslogic'
require 'active_support/core_ext/string'

bundle_display_hash = {
  yonghui: '永辉生意人',
  shengyiplus: '生意+',
  qiyoutong: '企邮通'
}
bundle_display_names = bundle_display_hash.keys.map(&:to_s)

current_app = ARGV.shift || 'null' # File.read('.current-app').strip.freeze
unless bundle_display_names.include?(current_app)
  puts %(appname should in #{bundle_display_names}, but #{current_app})
  exit
end

`echo '#{current_app}' > .current-app`
puts %(#{'-' * 25}\ncurrent app: #{current_app}\n#{'-' * 25}\n\n)

NAME_SPACE = current_app # TODO: namespace(variable_instance)
class Settings < Settingslogic
  source 'config/config.yaml'
  namespace NAME_SPACE
end

#
# reset app/build.gradle
#
gradle_path='app/build.gradle'
application_id_line = IO.readlines(gradle_path).find { |line| line.include?('applicationId') }
application_id = application_id_line.strip.scan(/applicationId\s+'com\.intfocus\.(.*?)'/).flatten[0]
puts %(applicationId switch <#{application_id}> to <#{current_app}>)
new_application_id_line = application_id_line.sub(application_id, current_app)
puts IO.read(gradle_path).sub(application_id_line, new_application_id_line)


puts %(rewrite private setting done.)

