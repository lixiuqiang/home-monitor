package com.nbg.web;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

import com.nbg.database.FinishBean;
import com.nbg.database.MonitorDB;

public class FinishHandler extends AbstractHandler {

	@Override
	public void handle(final HttpRequest request, final HttpResponse response,
			final HttpContext context) throws HttpException, IOException {
		String target = request.getRequestLine().getUri();
		System.out.println(target);
		Map<String, String> params = getParam(target);

		String day = new SimpleDateFormat("yyyyMMdd", Locale.CHINA)
				.format(new Date());
		FinishBean finishBean = new FinishBean();
		finishBean.setDay(Integer.parseInt(day));
		finishBean.setClient_ip(params.get("client_ip"));
		MonitorDB.getInstance().queryOrInsert(finishBean);

		finishBean.setNum(finishBean.getNum() + 1);
		finishBean.setProduct(params.get("product"));
		finishBean.setImei(params.get("imei"));
		finishBean.setSoft_version(params.get("soft_version"));
		finishBean.setPkg(params.get("package"));
		finishBean.setTime(System.currentTimeMillis());
		MonitorDB.getInstance().update(finishBean);
	}

}
