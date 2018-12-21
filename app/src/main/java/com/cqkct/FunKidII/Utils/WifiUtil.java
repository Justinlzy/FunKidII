package com.cqkct.FunKidII.Utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

public class WifiUtil {
	public final static int WIFI_CONNECTED_TIMEOUT = 15 * 1000;
	public final static int SOCKET_HEART_SECOND = 60;
	public final static int SOCKET_SLEEP_SECOND = 1;
	public static final int MSG_ERROR_NETWORK_TIMEOUT = -4; // 网络超时
	private volatile static WifiUtil wifiUtil = null;

	public static WifiUtil getInstance() {
		if (wifiUtil == null) {
			wifiUtil = new WifiUtil();
		}
		return wifiUtil;
	}

	/**
	 * 判断当前Wifi是否有效连接
	 *
	 * @param context
	 * @return true 有效连接 false 无效连接
	 */
	public static boolean isNetConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			return true;
		}
		return false;
	}

	// 获取掩码
	public static String getBroadcastAddress(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (isNetConnected(context)) {
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int ipAddress = wifiInfo.getIpAddress();
			int count = 0;
			while (ipAddress == 0 && count++ < 10) {
				wifiInfo = wifiManager.getConnectionInfo();
				ipAddress = wifiInfo.getIpAddress();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ei) {
					ei.printStackTrace();
				}
			}
			String ip = (ipAddress & 0xFF) + "." + ((ipAddress >> 8) & 0xFF)
					+ "." + ((ipAddress >> 16) & 0xFF) + "."
					+ ((ipAddress >> 24) | 0xFF);
			return ip;
		} else {
			return null;
		}
	}

	/**
	 * 获取当前网络连接类型
	 *
	 * @param context
	 * @return ConnectivityManager.TYPE_WIFI Wifi
	 *         ConnectivityManager.TYPE_MOBILE GPRS
	 */
	public static int getNetType(Context context) {
		ConnectivityManager connectMgr = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = connectMgr.getActiveNetworkInfo();
		// ConnectivityManager.TYPE_WIFI or ConnectivityManager.TYPE_MOBILE

		return info.getType();
	}

	/**
	 * 获取Wifi的mac地址
	 *
	 * @param context
	 * @return 当前连接Wifi连接的Mac地址
	 */
	public static String getLocalMacAddress(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		if (wifiManager.isWifiEnabled()) {
			WifiInfo info = wifiManager.getConnectionInfo();
			return info.getMacAddress();
		} else {
			return null;
		}
	}

	public static String getBSSID(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);

		if (wifiManager.isWifiEnabled()) {
			WifiInfo info = wifiManager.getConnectionInfo();
			return info.getBSSID();
		} else {
			return null;
		}
	}

	public static String getBroadcaseIp(Context context) {
		WifiManager wifii = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		DhcpInfo d = wifii.getDhcpInfo();
		return intToIp((d.ipAddress & d.netmask) | ~d.netmask);
	}

	private static String intToIp(int i) {
		return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + ((i >> 24) & 0xFF);
	}

	/**
	 * 获取Wifi连接的Ip地址
	 *
	 * @param context
	 * @return
	 */
	public static String getLocalIpAddress(Context context) {
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (isNetConnected(context)) {
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			int ipAddress = wifiInfo.getIpAddress();
			int count = 0;
			while (ipAddress == 0 && count++ < 10) {
				wifiInfo = wifiManager.getConnectionInfo();
				ipAddress = wifiInfo.getIpAddress();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ei) {
					ei.printStackTrace();
				}
			}
			String ip = intToIp(ipAddress);
			return ip;
		} else {
			return null;
		}
	}

	/**
	 * 判断是否是数字字符串
	 *
	 * @param str
	 * @return true false
	 */
	public static boolean isNumeric(String str) {
		Pattern pattern = Pattern.compile("[0-9]*");
		return pattern.matcher(str).matches();
	}

	public static String byteArr2HexStr(byte[] arrB) throws Exception {
		int iLen = arrB.length;
		// 每个byte用两个字符才能表示，所以字符串的长度是数组长度的两倍
		StringBuffer sb = new StringBuffer(iLen * 2);
		for (int i = 0; i < iLen; i++) {
			int intTmp = arrB[i];
			// 把负数转换为正数
			while (intTmp < 0) {
				intTmp = intTmp + 256;
			}
			// 小于0F的数需要在前面补0
			if (intTmp < 16) {
				sb.append("0");
			}
			sb.append(Integer.toString(intTmp, 16));
		}
		return sb.toString();
	}

	public static byte[] hexStr2ByteArr(String strIn) throws Exception {
		byte[] arrB = strIn.getBytes();
		int iLen = arrB.length;
		// 两个字符表示一个字节，所以字节数组长度是字符串长度除以2
		byte[] arrOut = new byte[iLen / 2];
		for (int i = 0; i < iLen; i = i + 2) {
			String strTmp = new String(arrB, i, 2);
			arrOut[i / 2] = (byte) Integer.parseInt(strTmp, 16);
		}
		return arrOut;
	}

	// 检查网络连接状态，Monitor network connections (Wi-Vi, GPRS, UMTS, etc.)
	public boolean checkNetWorkStatus(Context context) {
		boolean result;
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netinfo = cm.getActiveNetworkInfo();
		if (netinfo != null && netinfo.isConnected()) {
			result = true;
			L.i("WifiUtil->checkNetWorkStatus: The net was connected");
		} else {
			result = false;
			L.i("WifiUtil->checkNetWorkStatus: The net was bad!");
		}
		return result;
	}

	public boolean checkURL(String url) {
		boolean value = false;
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url)
					.openConnection();
			int code = conn.getResponseCode();
			L.d("WifiUtil->checkURL "+">>>>>>>>>>>>>>>> " + code + " <<<<<<<<<<<<<<<<<<");
			if (code != 200) {
				value = false;
			} else {
				value = true;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
		return value;
	}

	public static final String getCurrentWifiSSID(Context context) {
		String ssidString = null;
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (wifiManager != null
				&& wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			ssidString = wifiInfo.getSSID();
			if (ssidString.startsWith("\"") && ssidString.endsWith("\"")) {
				ssidString = ssidString.substring(1, ssidString.length() - 1);
			}
		}
		return ssidString;
	}
}
