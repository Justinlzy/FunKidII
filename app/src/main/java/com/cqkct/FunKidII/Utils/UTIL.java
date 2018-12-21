package com.cqkct.FunKidII.Utils;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.cqkct.FunKidII.App.App;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Properties;

public class UTIL {
	public static int dp2px(Context context, int dpValue) {
		return (int) context.getResources().getDisplayMetrics().density * dpValue;
	}
	public static final String TAG = UTIL.class.getName();

	public static final String FILEPATH = Environment
			.getExternalStorageDirectory()
			+ File.separator
			+ "bracelet"
			+ File.separator;
	public static final String FILENAME = "share.png";

	public static byte[] read(InputStream fis){
		try {
			System.out.println(2);
			int len = 0;
			byte[]data = new byte[1024];
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			while((len = fis.read(data))!=-1){
				bos.write(data, 0, len);
			}
			fis.close();
			byte arrData[] = bos.toByteArray();
			return arrData;
		} catch (Exception e) {
			return null;
		}
	}

    /**
	 *
	 * @return
	 */
	public static String getCountry() {
		Locale locale = Locale.getDefault();
        String country = locale.getCountry();
		return country; 
	}
	
	public static String getSubtractDay(String s){
		String date[] = s.split("-");
		int year = Integer.parseInt(date[0]);
		int month= Integer.parseInt(date[1]);
		int day= Integer.parseInt(date[2]);
		String s1 = getPreDay(year, month, day);
		String date1[] = s1.split("-");
		String monthStr = date1[1];
		String dayStr = date1[2];
		if(monthStr.length()==1){
			monthStr = "0"+monthStr;
		}
		if(dayStr.length()==1){
			dayStr = "0"+dayStr;
		}
		return date1[0] + "-" + monthStr + "-" + dayStr;
	}
	
	public static String getAddDay(String s){
		String date[] = s.split("-");
		int year = Integer.parseInt(date[0]);
		int month= Integer.parseInt(date[1]);
		int day= Integer.parseInt(date[2]);
		String s1 = getNextDay(year, month, day);
		String date1[] = s1.split("-");
		String monthStr = date1[1];
		String dayStr = date1[2];
		if(monthStr.length()==1){
			monthStr = "0"+monthStr;
		}
		if(dayStr.length()==1){
			dayStr = "0"+dayStr;
		}
		return date1[0] + "-" + monthStr + "-" + dayStr;
	}
	
	/**
	 *
	 * 
	 * @param year
	 * @param month
	 * @return
	 */
	public static String getPreDay(int year, int month, int day)
	{

		if (day != 1)
		{
			day--;
		}
		else
		{
			if (month != 1)
			{
				month--;
				day = getMaxDay(year, month);
			}
			else
			{
				year--;
				month = 12;
				day = 31;
			}
		}
		return year + "-" + month + "-" + day;
	}
	
	/**
	 *
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @return
	 */
	public static String getNextDay(int year, int month, int day)
	{
		if (day != getMaxDay(year, month))
		{
			day++;
		}
		else
		{
			if (month != 12)
			{
				month++;
				day = 1;
			}
			else
			{
				year++;
				month = day = 1;
			}
		}
		return year + "-" + month + "-" + day;
	}
	
	/**
	 *
	 * 
	 * @param year
	 * @param month
	 * @return
	 */
	public static int getMaxDay(int year, int month)
	{
		switch (month)
		{
			case 1:
			case 3:
			case 5:
			case 7:
			case 8:
			case 10:
			case 12:
				return 31;
			case 4:
			case 6:
			case 9:
			case 11:
				return 30;
			case 2:
				return (IsLeapYear(year) ? 29 : 28);
			default:
				return -1;
		}
	}
	
	/**
	 *
	 * 
	 * @param year
	 * @return
	 */
	public static boolean IsLeapYear(int year)
	{
		//
		return ((year % 400 == 0) || ((year % 4 == 0) && (year % 100 != 0)));
	}
	
	public static double decimalTo2(double f, int weishu) {
		BigDecimal bg = new BigDecimal(f);
		double f1 = bg.setScale(weishu, BigDecimal.ROUND_HALF_UP).doubleValue();
		return f1;
	}
	
	//
		public static boolean isGpsEnabled(LocationManager locationManager) {
			boolean isOpenGPS = locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);
			// boolean isOpenNetwork = locationManager
			// .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			if (isOpenGPS) {
				return true;
			}
			return false;
		}								   				
		    
		    public final static double x_pi = 3.14159265358979324 * 3000.0 / 180.0;
		    static double pi = 3.14159265358979324;
		    static double a = 6378245.0;
		    static double ee = 0.00669342162296594323;
		    
		    public static double[] wgs2bd(double lat, double lon) {
		        double[] wgs2gcj = wgs2gcj(lat, lon);
		        double[] gcj2bd = gcj2bd(wgs2gcj[0], wgs2gcj[1]);
		        return gcj2bd;
		 }
		    
		    private static double transformLat(double lat, double lon) {
		        double ret = -100.0 + 2.0 * lat + 3.0 * lon + 0.2 * lon * lon + 0.1 * lat * lon + 0.2 * Math.sqrt(Math.abs(lat));
		        ret += (20.0 * Math.sin(6.0 * lat * pi) + 20.0 * Math.sin(2.0 * lat * pi)) * 2.0 / 3.0;
		        ret += (20.0 * Math.sin(lon * pi) + 40.0 * Math.sin(lon / 3.0 * pi)) * 2.0 / 3.0;
		        ret += (160.0 * Math.sin(lon / 12.0 * pi) + 320 * Math.sin(lon * pi  / 30.0)) * 2.0 / 3.0;
		        return ret;
		 }

		 private static double transformLon(double lat, double lon) {
		        double ret = 300.0 + lat + 2.0 * lon + 0.1 * lat * lat + 0.1 * lat * lon + 0.1 * Math.sqrt(Math.abs(lat));
		        ret += (20.0 * Math.sin(6.0 * lat * pi) + 20.0 * Math.sin(2.0 * lat * pi)) * 2.0 / 3.0;
		        ret += (20.0 * Math.sin(lat * pi) + 40.0 * Math.sin(lat / 3.0 * pi)) * 2.0 / 3.0;
		        ret += (150.0 * Math.sin(lat / 12.0 * pi) + 300.0 * Math.sin(lat / 30.0 * pi)) * 2.0 / 3.0;
		        return ret;
		 }
		    
		    public static double[] wgs2gcj(double lat, double lon) {
		        double dLat = transformLat(lon - 105.0, lat - 35.0);
		        double dLon = transformLon(lon - 105.0, lat - 35.0);
		        double radLat = lat / 180.0 * pi;
		        double magic = Math.sin(radLat);
		        magic = 1 - ee * magic * magic;
		        double sqrtMagic = Math.sqrt(magic);
		        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
		        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
		        double mgLat = lat + dLat;
		        double mgLon = lon + dLon;
		        double[] loc = { mgLat, mgLon };
		        return loc;
		 }
		    
		    public static double[] gcj2bd(double lat, double lon) {
		        double x = lon, y = lat;
		        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * x_pi);
		        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * x_pi);
		        double bd_lon = z * Math.cos(theta) + 0.0065;
		        double bd_lat = z * Math.sin(theta) + 0.006;
		        return new double[] { bd_lat, bd_lon };
		 }
		    
		    public static String null2String(String str)
			{
				if(null==str)
					return "";
				return str;
			}
		    
		    /*
			 * @param str
			 */
			public static String get(String str)
			{	
				InputStream inputStream = UTIL.class.getClassLoader().getResourceAsStream("com/szkct/funrun/util/configure.properties");
				Properties pro = new Properties();
				try {
					pro.load(inputStream);
				} catch (IOException e) {
					try {
						inputStream.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					e.printStackTrace();
				}
				return pro.getProperty(str);
			}

	/**
	 * 把字节数组转换成16进制字符串
	 * @param bArray
	 * @return justin
	 */
	public static final String bytesToHexString(byte[] bArray) {
		if(bArray == null)
			return null;
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<bArray.length;i++){
			sb.append(String.format("0x%02X", bArray[i]));
			if(i != bArray.length -1) {
				sb.append(",");
			}
		}
		return sb.toString();
	}
	/**
	 * 获取系统默认语言
	 *
	 * @return
	 */
	public static String getLanguage() {
		// 获取系统当前使用的语言
		Locale locale = Locale.getDefault();
		String language = locale.getLanguage();
		return language;
	}
	/**
	 * 获取程序code 1
	 *
	 * @param context
	 * @return
	 */
	public static int getVerCode(Context context) {
		int verCode = -1;
		try {
			verCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
		} catch (Exception e) {
			e.printStackTrace();
			// L.e("error", "获取版本出错");
		}
		return verCode;
	}

	public static String getVersion(Context context) {
		try {
			return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static Context mContext  = App.getInstance().getBaseContext();;
	public static void call(String phoneNum){

		if (TextUtils.isEmpty(phoneNum)){ //电话号码为空
			return;
		}
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_CALL);
		Uri uri = Uri.parse("tel:"+phoneNum);   //设置要操作界面的具体内容  拨打电话固定格式： tel：
		intent.setData(uri);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);
	}
}
