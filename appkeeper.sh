#!/usr/bin/env bash

##############################################################################
##
##  Ruby switch YH-Android apps for UN*X
##
##############################################################################

case "$1" in
  yonghui|shengyiplus|qiyoutong)
    # bundle exec ruby config/app_keeper.rb --app=shengyiplus --gradle --mipmap --manifest --res --java --apk --pgyer
    bundle exec ruby config/app_keeper.rb --app="$1" --gradle --mipmap --manifest --res --java
  ;;
  pgyer)
     bundle exec ruby config/app_keeper.rb --app="$(cat .current-app)" --apk --pgyer
  ;;
  all)
    echo 'TODO'
  ;;
  *)
  test -z "$1" && echo "current app: $(cat .current-app)" || echo "unknown argument - $1"
  ;;
esac