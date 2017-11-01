package com.woc.chat.plugin.MatchUser.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class HttpUtil {
	public static String httpGet(String url) throws IOException
	{
		URL u=new URL(url);
		URLConnection urlConnection=u.openConnection();
		HttpURLConnection httpURLConnection=(HttpURLConnection)urlConnection;
		InputStreamReader inputStreamReader=new InputStreamReader( httpURLConnection.getInputStream());
		BufferedReader bufferedReader=new BufferedReader(inputStreamReader);
		StringBuffer stringBuffer=new StringBuffer();
		String line=null;
		while ((line=bufferedReader.readLine())!=null) {
			stringBuffer.append(line);
			
		}
		if(bufferedReader!=null)
		{
			bufferedReader.close();
		}
		
		return stringBuffer.toString();
		
	}
}
