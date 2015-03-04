package com.nbg.web;

import java.io.IOException;
import java.util.Locale;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;

public class ProtocolHandler extends AbstractHandler{
	public void handle(final HttpRequest request,
			final HttpResponse response, final HttpContext context)
			throws HttpException, IOException {

		String method = request.getRequestLine().getMethod()
				.toUpperCase(Locale.ENGLISH);
		// get uri
		String target = request.getRequestLine().getUri();
		if (method.equals("GET")) {
			response.setStatusCode(HttpStatus.SC_OK);
			StringEntity entity = new StringEntity(
					"<xml><method>get</method><url>" + target
							+ "</url></xml>");
			response.setEntity(entity);
		} else if (method.equals("POST")) {
			response.setStatusCode(HttpStatus.SC_OK);
			StringEntity entity = new StringEntity(
					"<xml><method>post</method><url>" + target
							+ "</url></xml>");
			response.setEntity(entity);
		} else {
			throw new MethodNotSupportedException(method
					+ " method not supported");
		}
	}
}
