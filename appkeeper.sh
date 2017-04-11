#!/usr/bin/env bash

##############################################################################
##
##  Script switch YH-Android apps for UN*X
##
##############################################################################

check_assets() {
    local shared_path="app/src/main/assets"

    if [[ -z "$1" ]];
    then
        echo "ERROR: please offer assets filename"
        exit
    fi

    local filename="$1.zip"
    local configpath="config/Assets/zip-$filedirname"
    local projectfilepath="$shared_path/$1.zip"
    local url="${downloadurl}/api/v1/download/$1.zip"

    echo -e "\n## $filename\n"
    local status_code=$(curl -s -o /dev/null -I -w "%{http_code}" $url)

    if [[ "$status_code" != "200" ]];
    then
        echo "ERROR: $status_code - $url"
        exit
    fi
    echo "- http response 200."

    curl -s -o $filename $url
    echo "- download $([[ $? -eq 0 ]] && echo 'successfully' || echo 'failed')"

    if [[ ! -d "$configpath" ]]; 
    then
      mkdir "$configpath"
    fi

    cp -R $filename "$configpath/$filename"
    test -f $filename && rm $filename

    local md5_server=$(md5 ./"$configpath/$filename" | cut -d ' ' -f 4)
    local md5_local=$(md5 ./$projectfilepath | cut -d ' ' -f 4)

    if [[ "$md5_server" = "$md5_local" ]];
    then
        echo "- not modified."
    else
        cp -R "$configpath/$filename" "$shared_path"
        echo "- $filename updated."
    fi
}

download_assets() {
  check_assets "BarCodeScan"
  check_assets "advertisement"
  check_assets "assets"
  check_assets "fonts"
  check_assets "images"
  check_assets "javascripts"
  check_assets "stylesheets"
  check_assets "loading"
}

case "$1" in
  shengyiplus|qiyoutong|test)
    # bundle exec ruby config/app_keeper.rb --app=shengyiplus --gradle --mipmap --manifest --res --java --apk --pgyer
    bundle exec ruby config/app_keeper.rb --app="$1" --gradle --mipmap --manifest --res --java
  ;;
  yh_android)
    downloadurl="https://yonghui.idata.mobi"
    filedirname="yh_android"
    download_assets
    bundle exec ruby config/app_keeper.rb --app="$1" --gradle --mipmap --manifest --res --java
  ;;
  yonghuitest)
    downloadurl="https://development.shengyiplus.com"
    filedirname="yonghuitest"
    download_assets
    bundle exec ruby config/app_keeper.rb --app="$1" --gradle --mipmap --manifest --res --java
  ;;
  pgyer)
    bundle exec ruby config/app_keeper.rb --app="$(cat .current-app)" --apk --pgyer
  ;;
  github)
    bundle exec ruby config/app_keeper.rb --github
  ;;
  view)
    bundle exec ruby config/app_keeper.rb --view
  ;;
  deploy)
    bash "$0" shengyiplus
    bash "$0" pgyer
    bash "$0" qiyoutong
    bash "$0" pgyer
    bash "$0" yh_android
    bash "$0" pgyer
  ;;
  all)
    echo 'TODO'
  ;;
  *)
    if [[ -z "$1" ]]; then
      bundle exec ruby config/app_keeper.rb --check
    else
      echo "unknown argument - $1"
    fi
  ;;
esac