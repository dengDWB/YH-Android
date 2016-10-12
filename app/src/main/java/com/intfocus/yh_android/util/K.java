package com.intfocus.yh_android.util;

import java.io.Serializable;

/**
 * api链接，宏
 *
 * @author jay
 * @version 1.0
 * @created 2016-01-06
 * Created by lijunjie on 16/9/22.
 */
public class K extends PrivateURLs implements Serializable {
  /**
   *  API#paths
   */
  public final static String kUserAuthenticateAPIPath = "%s/api/v1/%s/%s/%s/authentication";
  public final static String kReportDataAPIPath       = "%s/api/v1/group/%s/template/%s/report/%s/zip";
  public final static String kCommentAPIPath          = "%s/api/v1/user/%d/id/%d/type/%d";
  public final static String kScreenLockAPIPath       = "%s/api/v1/user_device/%s/screen_lock";
  public final static String kDeviceStateAPIPath      = "%s/api/v1/user_device/%d/state";
  public final static String kRsetPwdAPIPath          = "%s/api/v1/update/%s/password";
  public final static String kActionLogAPIPath        = "%s/api/v1/android/logger";
  public final static String kPushDeviceTokenAPIPath  = "%s/api/v1/device/%s/push_token/%s";
  public final static String kBarCodeScanAPIPath      = "%s/api/v1/group/%s/role/%s/user/%s/store/%s/barcode_scan?code_info=%s&code_type=%s";
  public final static String kDownloadAssetsAPIPath   = "%s/api/v1/download/%s.zip";
  public final static String kUploadGravatarAPIPath   = "%s/api/v1/device/%s/upload/user/%s/gravatar";

  /**
   *  Mobile#View Path
   */
  public final static String kKPIMobilePath            = "%s/mobile/%s/group/%s/role/%s/kpi";
  public final static String kMessageMobilePath        = "%s/mobile/%s/role/%s/group/%s/user/%s/message";
  public final static String kAppMobilePath            = "%s/mobile/%s/role/%s/app";
  public final static String kAnalyseMobilePath        = "%s/mobile/%s/role/%s/analyse";
  public final static String kCommentMobilePath        = "%s/mobile/%s/id/%s/type/%s/comment";
  public final static String kResetPwdMobilePath       = "%s/mobile/%s/update_user_password";
  public final static String kThursdaySayMobilePath    = "%s/mobile/%s/thursday_say";

  /**
   *  Config#Application
   */
  public final static String kConfigDirName            = "Configs";
  public final static String kSharedDirName            = "Shared";
  public final static String kCachedDirName            = "Cached";
  public final static String kHTMLDirName              = "HTML";
  public final static String kAssetsDirName            = "Assets";
  public final static String kReportDataFileName       = "group_%s_template_%s_report_%s.js";
  public final static String kUserConfigFileName       = "user.json";
  public final static String kPushConfigFileName       = "push_message.json";
  public final static String kSettingConfigFileName    = "setting.json";
  public final static String kTabIndexConfigFileName   = "page_tab_index.json";
  public final static String kGesturePwdConfigFileName = "gesture_password.json";
  public final static String kLocalNotificationConfigFileName = "local_notification.json";
  public final static String kCachedHeaderConfigFileName      = "cached_header.json";
  public final static String kPgyerVersionConfigFileName      = "pgyer_version.json";
  public final static String kGravatarConfigFileName          = "gravatar.json";
  public final static String kBetaConfigFileName              = "beta.json";;
  public final static String kBarCodeResultFileName    = "barcode_result.json";
  public final static String kScanBarCodeHTMLName      = "scan_bar_code.html";
  public final static String kCurrentVersionFileName   = "current_version.txt";
  public final static String kBehaviorConfigFileName   = "behavior.json";
}
