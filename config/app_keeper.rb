#!/usr/bin/env ruby
# encoding: utf-8
#
# ## 调整事项:
# 1. 应用图标
# 2. 应用名称XML档
# 3. Gradle应用ID
# 4. AndroidManifest 友盟、蒲公英配置
# 5. PrivateURLs 服务器域名
# 
# $ bundle exec ruby config/app_keeper.rb -h
# usage: config/app_keeper.rb [options]
#     -h, --help      print help info
#     -g, --gradle    bundle.gradle
#     -m, --mipmap    update mipmap
#     -x, --manifest  AndroidManifest.xml
#     -r, --res       res/strings.xml
#     -j, --java      PrivateURLs.java
#     -f, --apk       whether generate apk
#     -p, --pgyer     whether upload to pgyer
#     -v, --version   print the version
#     -a, --app       current app
#   
require 'json'
require 'slop'
require 'nokogiri'
require 'settingslogic'
require 'active_support'
require 'active_support/core_ext/hash'
require 'active_support/core_ext/string'
require 'active_support/core_ext/numeric'

def exit_when condition, &block
  return unless condition
  yield
  exit
end

slop_opts = Slop.parse do |o|
  o.string '-a', '--app', 'current app', default: 'yonghui'
  o.bool '-g', '--gradle', 'bundle.gradle', default: false
  o.bool '-m', '--mipmap', 'update mipmap', default: false
  o.bool '-x', '--manifest', 'AndroidManifest.xml', default: false
  o.bool '-r', '--res', 'res/strings.xml', default: false
  o.bool '-j', '--java', 'PrivateURLs.java', default: false
  o.bool '-f', '--apk', 'whether generate apk', default: false
  o.bool '-p', '--pgyer', 'whether upload to pgyer', default: false
  o.bool '-b', '--github', 'black private info when commit', default: false
  o.on '-v', '--version', 'print the version' do
    puts Slop::VERSION
    exit
  end
  o.on '-h', '--help', 'print help info' do
    puts o
    exit
  end
end

current_app = slop_opts[:app]
bundle_display_hash = {
  yonghui: '永辉生意人',
  qiyoutong: '企邮通',
  shengyiplus: '生意+'
}
bundle_display_names = bundle_display_hash.keys.map(&:to_s)
exit_when !bundle_display_names.include?(current_app) do
  puts %(Abort: appname should in #{bundle_display_names}, but #{current_app})
end

current_app_name = bundle_display_hash.fetch(current_app.to_sym)

`echo '#{current_app}' > .current-app`
puts %(\n# current app: #{current_app}\n)

NAME_SPACE = current_app # TODO: namespace(variable_instance)
class Settings < Settingslogic
  source 'config/config.yaml'
  namespace NAME_SPACE
end

puts %(\n## modified configuration\n\n)
#
# reset app/build.gradle
#
if slop_opts[:gradle]
  gradle_path = 'app/build.gradle'
  gradle_text = IO.read(gradle_path)
  gradle_lines = gradle_text.split(/\n/)
  application_id_line = gradle_lines.find { |line| line.include?('applicationId') }
  application_id = application_id_line.strip.scan(/applicationId\s+'(com\.intfocus\..*?)'/).flatten[0]
  new_application_id_line = application_id_line.sub(application_id, Settings.application_id)

  puts %(- done: applicationId: #{application_id})
  File.open(gradle_path, 'w:utf-8') do |file|
    file.puts gradle_text.sub(application_id_line, new_application_id_line)
  end
end

#
# reset mipmap and loading.zip
#
if slop_opts[:mipmap]
  puts %(- done: launcher@mipmap)
  `rm -fr app/src/main/res/mipmap-* && cp -fr config/Assets/mipmap-#{current_app}/mipmap-* app/src/main/res/`
  puts %(- done: loading zip)
  `cp -f config/Assets/loading-#{current_app}.zip app/src/main/assets/loading.zip`
  puts %(- done: banner_logo)
  `cp -f config/Assets/banner-logo-#{current_app}.png app/src/main/res/drawable/banner_logo.png`
end

#
# reset app/src/main/AndroidManifest.xml
#
android_manifest_path = 'app/src/main/AndroidManifest.xml'
def xml_sub(content, doc, key, value)
  meta_data = doc.xpath(%(//meta-data[@android:name='#{key}'])).first
  meta_data_value = meta_data.attributes['value']
  content.sub(meta_data_value, value)
end

if slop_opts[:manifest]
  android_manifest_content = File.read(android_manifest_path)
  android_manifest_doc = Nokogiri.XML(android_manifest_content)
  android_manifest_content = xml_sub(android_manifest_content, android_manifest_doc, 'PGYER_APPID', Settings.pgyer.android)
  android_manifest_content = xml_sub(android_manifest_content, android_manifest_doc, 'UMENG_APPKEY', Settings.umeng.android.app_key)
  android_manifest_content = xml_sub(android_manifest_content, android_manifest_doc, 'UMENG_MESSAGE_SECRET', Settings.umeng.android.umeng_message_secret)

  puts %(- done: umeng/pgyer configuration)
  File.open(android_manifest_path, 'w:utf-8') do |file|
    file.puts(android_manifest_content)
  end
end

if slop_opts[:github]
  android_manifest_content = File.read(android_manifest_path)
  android_manifest_doc = Nokogiri.XML(android_manifest_content)
  android_manifest_content = xml_sub(android_manifest_content, android_manifest_doc, 'PGYER_APPID', 'pgyer-app-id')
  android_manifest_content = xml_sub(android_manifest_content, android_manifest_doc, 'UMENG_APPKEY', 'umeng-app-key')
  android_manifest_content = xml_sub(android_manifest_content, android_manifest_doc, 'UMENG_MESSAGE_SECRET', 'umeng-message-secret')

  puts %(- done: umeng/pgyer info black for github)
  File.open(android_manifest_path, 'w:utf-8') do |file|
    file.puts(android_manifest_content)
  end
end

#
# reset res/strings.xml
#
if slop_opts[:res]
  strings_erb_path = 'config/strings.xml.erb'
  strings_xml_path = 'app/src/main/res/values/strings.xml'
  puts %(- done: app name: #{current_app_name})
  File.open(strings_xml_path, 'w:utf-8') do |file|
    file.puts ERB.new(IO.read(strings_erb_path)).result
  end
end

if slop_opts[:java]
  puts %(- done: PrivateURLs java class)
  File.open('app/src/main/java/com/intfocus/yh_android/util/PrivateURLs.java', 'w:utf-8') do |file|
    file.puts <<-EOF.strip_heredoc
      //  PrivateURLs.java
      //
      //  `bundle install`
      //  `bundle exec ruby app_kepper.rb`
      //
      //  Created by lijunjie on 16/06/02.
      //  Copyright © 2016年 com.intfocus. All rights reserved.
      //

      // current app: [#{current_app}]
      // automatic generated by app_keeper.rb
      package com.intfocus.yh_android.util;

      public class PrivateURLs {
        public final static String HOST = "#{Settings.server}";
        public final static String HOST1 = "http://10.0.3.2:4567";
      }
      EOF
  end
end

#
# gradlew generate apk
# 
if slop_opts[:apk]
  apk_path = 'app/build/outputs/apk/app-release.apk'
  key_store_path = File.join(Dir.pwd, Settings.key_store.path)
  exit_when !File.exist?(key_store_path) do
    puts %(Abort: key store file not exist - #{apk_path})
  end

  `test -f #{apk_path} && rm -f #{apk_path}`
  `export KEYSTORE=#{key_store_path} KEYSTORE_PASSWORD=#{Settings.key_store.password} KEY_ALIAS=#{Settings.key_store.alias} KEY_PASSWORD=#{Settings.key_store.alias_password} && /bin/bash ./gradlew assembleRelease`

  exit_when !File.exist?(apk_path) do
    puts %(Abort: failed generate apk - #{apk_path})
  end

  puts %(- done: generate apk(#{File.size(apk_path).to_s(:human_size)}) - #{apk_path})
end

if slop_opts[:pgyer]
  response = `curl --silent -F "file=@#{apk_path}" -F "uKey=#{Settings.pgyer.user_key}" -F "_api_key=#{Settings.pgyer.api_key}" http://www.pgyer.com/apiv1/app/upload`

  hash = JSON.parse(response).deep_symbolize_keys[:data]
  puts %(- done: upload apk(#{hash[:appFileSize].to_i.to_s(:human_size)}) to #pgyer#\n\t#{hash[:appName]}\n\t#{hash[:appIdentifier]}\n\t#{hash[:appVersion]}(#{hash[:appVersionNo]})\n\t#{hash[:appQRCodeURL]})
end
