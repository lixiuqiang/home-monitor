package com.nbg.web;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;

import com.nbg.database.FinishBean;
import com.nbg.database.MonitorDB;

public class DataHandler extends AbstractHandler {

	public void handle(final HttpRequest request, final HttpResponse response,
			final HttpContext context) throws HttpException, IOException {
		String method = request.getRequestLine().getMethod()
				.toUpperCase(Locale.ENGLISH);
		if (method.equals("GET")) {
			response.setStatusCode(HttpStatus.SC_OK);
			StringBuilder monitor = new StringBuilder();
			List<FinishBean> finishBeans = MonitorDB.getInstance().load();
			for (FinishBean finishBean : finishBeans) {
				monitor.append(finishBean.getDay());
				monitor.append("\tIP:").append(finishBean.getClient_ip());
				monitor.append("\tNUM:").append(finishBean.getNum());
				monitor.append("\t").append(finishBean.getProduct());
				monitor.append("\t").append(finishBean.getSoft_version());
				monitor.append("\t").append(finishBean.getPkg());
				monitor.append("\t").append(finishBean.getClient_ip());
				monitor.append("\t").append(finishBean.getImei());
				monitor.append("\t").append(
						MonitorHandler.formatTimeAgo(finishBean.getTime()));
				monitor.append("\n");
			}
			StringEntity entity = new StringEntity(monitor.toString(), "utf-8");
			response.setEntity(entity);
		} else {
			throw new MethodNotSupportedException(method
					+ " method not supported");
		}
	}
}
