package org.brewchain.browserAPI.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CallHelper {

	public String remoteCallGet(String reqString, String signature) {
		String ret = "";
		BufferedReader in = null;
		HttpURLConnection conn = null;
		String url = "https://api.ucloud.cn/";
		String urlNameString = url + "?" + reqString + "&Signature=" + signature;
		try {
			URL realUrl = new URL(urlNameString);
			conn = (HttpURLConnection) realUrl.openConnection();
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.connect();
			int httpStatus = conn.getResponseCode();
			if (httpStatus == 200 || httpStatus == 202 || httpStatus == 201) {
				in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				ret = response.toString();
			} else {
				ret = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			ret = null;
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (conn != null) {
					conn.disconnect();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return ret;
	}
	
	public synchronized static String remoteCallPost(String url, String parameter) {
		String ret = "";
		PrintWriter out = null;
		BufferedReader in = null;
		HttpURLConnection conn = null;

		try {
			URL realUrl = new URL(url);
			conn = (HttpURLConnection) realUrl.openConnection();
			conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
			conn.setRequestProperty("Accept", "application/json");
//			conn.setRequestProperty("X-Auth-Token", token);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			out = new PrintWriter(conn.getOutputStream());
			out.print(parameter);
			out.flush();
			int httpStatus = conn.getResponseCode();
			log.debug("http Request Status" + httpStatus);
			log.debug("http Request url===>"+url);
			log.debug("http Request params===>"+parameter);
			if (httpStatus == 200 || httpStatus == 202 || httpStatus == 201 || httpStatus == 204) {
				in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				ret = response.toString();
				if(ret.length()==0 || ret.equals("null")) ret = "{}";
			} else {
				ret = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			ret = null;
		} finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
				if (conn != null) {
					conn.disconnect();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	
		log.debug("api returns====>" + ret);
		return ret;
	}
}
