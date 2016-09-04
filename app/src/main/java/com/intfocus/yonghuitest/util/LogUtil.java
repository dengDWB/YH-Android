package com.intfocus.yonghuitest.util;

import android.util.Log;

/**
 * Created by lijunjie on 16/7/22.
 */
public class LogUtil {

  /*
   * Log.d(tag, str, limit)
   */
  public static void d(String tag, String str, int limit) {
    int maxLength = 2000;
    str = str.trim();
    Log.d(tag, str.substring(0, str.length() > maxLength ? maxLength : str.length()));
    if(str.length() > maxLength && limit < 4) {
      str = str.substring(maxLength, str.length());
      LogUtil.d(tag, str, limit);
    }
  }

  /*
   * Log.d(tag, str)
   */
  public static void d(String tag, String str) {
     LogUtil.d(tag, str, 0);
  }
}
