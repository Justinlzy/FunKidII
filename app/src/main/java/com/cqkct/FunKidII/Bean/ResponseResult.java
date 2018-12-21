package com.cqkct.FunKidII.Bean;


import com.cqkct.FunKidII.service.Pkt;
import com.google.protobuf.GeneratedMessage;

import java.io.Serializable;

public class ResponseResult implements Serializable {
	public int cmdName;			 		//指令名称
	public String sourceAddress ;		//源地址
	public String goalAddress; 	    	 //目标地址
//	public String businessNumber;		//流水号
	public byte[] valueContent;  		//指令内容
	public int    valueContentLength;   //指令内容长度
	public int    protocolVersion;		//版本号
	public int    requestOrResponse;	//请求类型
	public int    cmdResult;			//请求 处理结果 eg ) login 0:login failed 1 :login success 2: password error
	public String userNumber;			//用户账号
	public String userPassword;			//用户密码
	public Pkt pkt;

	public GeneratedMessage requestMsg;


	public Object resultObj;//返回的参数字符串（TableData 或字符串）


	@Override
	public String toString() {
		return "ResponseResult{" +
				"sourceAddress" + sourceAddress +
				", goalAddress=" + goalAddress +
				", valueContentLength=" + valueContentLength +
				", businessNumber=" + (pkt != null ? pkt.seq : -1) +
				", protocolVersion=" + protocolVersion +
				", requestOrResponse=" + requestOrResponse +
				", cmdResult=" + cmdResult +
				", cmdName="+cmdName+
				", userNumber="+userNumber+
				", userPassword="+userPassword+
		'}';
	}
}
