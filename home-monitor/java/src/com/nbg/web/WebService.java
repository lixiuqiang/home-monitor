package com.nbg.web;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import android.content.Intent;

import com.nbg.database.MonitorDB;
import com.nbg.monitor.NotificationService;

public class WebService extends NotificationService {

	public WebService() {
		super(5, CONFIG_WEB);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		new MonitorDB(getApplicationContext());
		try {
			Thread t = new RequestListenerThread(9998);
			t.setDaemon(false);
			t.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		DevicesMonitor.getInstance().checkIP();
		return super.onStartCommand(intent, flags, startId);
	}

	class RequestListenerThread extends Thread {

		private final ServerSocket serversocket;
		private final HttpParams params;
		private final HttpService httpService;

		public RequestListenerThread(int port) throws IOException {
			this.serversocket = new ServerSocket(port);
			WebService.this.notify("WEB", "服务启动,端口:" + port);

			// Set up the HTTP protocol processor
			HttpProcessor httpproc = new ImmutableHttpProcessor(
					new HttpResponseInterceptor[] { new ResponseDate(),
							new ResponseServer(), new ResponseContent(),
							new ResponseConnControl() });

			this.params = new BasicHttpParams();
			this.params
					.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
					.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE,
							8 * 1024)
					.setBooleanParameter(
							CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
					.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
					.setParameter(CoreProtocolPNames.ORIGIN_SERVER,
							"HttpComponents/1.1");

			// Set up request handlers
			HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
			reqistry.register("/protocol", new ProtocolHandler());
			reqistry.register("/monitor", new MonitorHandler());
			reqistry.register("/finish", new FinishHandler());
			reqistry.register("/data", new DataHandler());

			this.httpService = new HttpService(httpproc,
					new DefaultConnectionReuseStrategy(),
					new DefaultHttpResponseFactory());
			httpService.setParams(this.params);
			httpService.setHandlerResolver(reqistry); // 为http服务设置注册好的请求处理器。

		}

		@Override
		public void run() {
			WebService.this.notify("WEB", "Listening on port "
					+ this.serversocket.getLocalPort());
			while (!Thread.interrupted()) {
				try {
					// Set up HTTP connection
					Socket socket = this.serversocket.accept();
					DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
					WebService.this.notify("WEB", "Incoming connection from "
							+ socket.getInetAddress());
					conn.bind(socket, this.params);

					// Start worker thread
					Thread t = new WorkerThread(this.httpService, conn);
					t.setDaemon(true);
					t.start();
				} catch (InterruptedIOException ex) {
					break;
				} catch (IOException e) {
					System.err
							.println("I/O error initialising connection thread: "
									+ e.getMessage());
					break;
				}
			}
		}
	}

	class WorkerThread extends Thread {

		private final HttpService httpservice;
		private final HttpServerConnection conn;

		public WorkerThread(final HttpService httpservice,
				final HttpServerConnection conn) {
			super();
			this.httpservice = httpservice;
			this.conn = conn;
		}

		@Override
		public void run() {
			System.out.println("New connection thread");
			HttpContext context = new BasicHttpContext(null);
			try {
				while (!Thread.interrupted() && this.conn.isOpen()) {
					this.httpservice.handleRequest(this.conn, context);
				}
			} catch (ConnectionClosedException ex) {
				System.err.println("Client closed connection");
			} catch (IOException ex) {
				System.err.println("I/O error: " + ex.getMessage());
			} catch (HttpException ex) {
				System.err.println("Unrecoverable HTTP protocol violation: "
						+ ex.getMessage());
			} finally {
				try {
					this.conn.shutdown();
				} catch (IOException ignore) {
				}
			}
		}
	}
}
