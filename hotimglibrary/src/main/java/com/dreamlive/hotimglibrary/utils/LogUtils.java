package com.dreamlive.hotimglibrary.utils;

import android.text.TextUtils;
import android.util.Log;

/**
 * 日志打印帮助类
 * Created by dreamlivemeng on 2016/6/7.
 */
public final class LogUtils {
	
	private final static boolean IS_DEBUG = true;

	private LogUtils() throws InstantiationException {
		throw new InstantiationException("This utility class is not created for instantiation");
	}

	public static void e(String tag, String msg) {
		if(IS_DEBUG) {
			Log.e(tag, checkMsg(msg));
		}
	}
	
	public static void i(String tag, String msg) {
		if(IS_DEBUG) {
			Log.i(tag, checkMsg(msg));
		}
	}
	
	public static void d(String tag, String msg) {
		if(IS_DEBUG) {
			Log.d(tag, checkMsg(msg));
		}
	}
	
	public static void e(String tag, String msg,  Throwable t) {
		if(IS_DEBUG) {
			Log.e(tag, checkMsg(msg), checkThrowable(t));
		}
	}
	
	public static void  d(String tag, String msg, Throwable t) {
		if(IS_DEBUG) {
			Log.d(tag, checkMsg(msg), checkThrowable(t));
		}
	}
	
	public static void i(String tag, String msg, Throwable t) {
		if(IS_DEBUG) {
			Log.d(tag, checkMsg(msg), checkThrowable(t));
		}
	}
	
	public static String checkMsg(String msg) {
		return TextUtils.isEmpty(msg) ? "" : msg;
	} 
	
	public static Throwable checkThrowable (Throwable t) {
		return null == t ? new Throwable("") : t;
	}
}
