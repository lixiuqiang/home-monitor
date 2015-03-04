package com.nbg.web;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.protocol.HttpRequestHandler;

public abstract class AbstractHandler implements HttpRequestHandler {

	protected Map<String, String> getParam(String uri) {
		Map<String, String> params = new HashMap<String, String>();
		try {
			if (uri != null) {
				String subStr = uri.substring(uri.indexOf("?") + 1);
				String[] ary = subStr.split("&");
				for (int i = 0; i < ary.length; i++) {
					String[] temp = ary[i].split("=");
					if (temp.length < 2) {
						params.put(temp[0], "");
					} else {
						params.put(temp[0], temp[1]);
					}
				}
				return params;
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}
}
