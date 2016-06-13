require 'nokogiri'

xml_path = 'app/src/main/AndroidManifest.xml'
doc = Nokogiri.XML(File.read(xml_path))
meta_data = doc.xpath('//meta-data[@android:name="PGYER_APPID"]').first
meta_data_value = meta_data.attributes['value']
