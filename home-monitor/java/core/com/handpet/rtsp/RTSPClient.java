package com.handpet.rtsp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import com.handpet.jlibrtp.DataFrame;
import com.handpet.jlibrtp.Participant;
import com.handpet.jlibrtp.RTPAppIntf;
import com.handpet.jlibrtp.RTPSession;

public class RTSPClient extends Thread implements IEvent, RTPAppIntf {
	private static final String VERSION = " RTSP/1.0\r\n";
	private static final String RTSP_OK = "RTSP/1.0 200 OK";
	private static final String RTSP_AUTH = "RTSP/1.0 401 Unauthorized";

	/** */
	/** * 连接通道 */
	private SocketChannel socketChannel;

	/** */
	/** 发送缓冲区 */
	private ByteBuffer sendBuf;

	/** */
	/** 接收缓冲区 */
	private ByteBuffer receiveBuf;

	private int client_port;

	private static final int BUFFER_SIZE = 10240;

	/** */
	/** 端口选择器 */
	private Selector selector;
	private final String address;
	private final String dir;
	private final long max;
	private final String username;
	private final String password;
	private final String remote_ip;
	private final int remote_port;
	private final INotify notify;
	private final int offset;
	private final long timeout;

	private RTPSession rtpSession;
	private Status sysStatus;
	private String sessionid;
	private int seq = 1;
	private String trackInfo;
	private String config;
	private String realm;
	private String nonce;
	private long temp;
	private FileOutputStream fos;
	private boolean start = false;
	private long start_time;

	private String dirPath;
	private int size;
	private long receive;
	private long lost;

	private enum Status {
		init, options, describe, setup, play
	}

	public RTSPClient(String address, String dir, int mb, long second,
			final INotify notify) {
		start_time = System.currentTimeMillis();
		this.address = address;
		this.dir = dir;
		this.timeout = second;
		this.max = mb * 1024L * 1024L;
		this.client_port = new Random().nextInt(10000) + 10000;
		this.offset = new Random().nextInt(100);
		this.notify = notify;
		int a = address.indexOf("@");
		int b = address.indexOf(":", a);
		int c = address.indexOf("/", b);
		int d = address.indexOf(":", 7);
		this.remote_ip = address.substring(a + 1, b);
		this.remote_port = Integer.parseInt(address.substring(b + 1, c));
		this.username = address.substring(7, d);
		this.password = address.substring(d + 1, a);
		// 初始化缓冲区
		this.sendBuf = ByteBuffer.allocateDirect(BUFFER_SIZE);
		this.receiveBuf = ByteBuffer.allocateDirect(BUFFER_SIZE);

		UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread arg0, Throwable arg1) {
				arg1.printStackTrace();
				notify.shutdown();
			}
		};
		Thread.setDefaultUncaughtExceptionHandler(handler);
	}

	@Override
	public void read(SelectionKey key) throws IOException {
		int len = 0;
		int readBytes = 0;
		synchronized (receiveBuf) {
			receiveBuf.clear();
			try {
				while ((len = socketChannel.read(receiveBuf)) > 0) {
					readBytes += len;
				}
			} finally {
				receiveBuf.flip();
			}
			if (readBytes > 0) {
				final byte[] msg = new byte[readBytes];
				receiveBuf.get(msg);
				String receive = new String(msg);
				System.out.println("返回内容：{" + receive + "}");
				handle(receive);
			} else {
				System.out.println("接收到数据为空,重新启动连接");
				shutdown();
			}
		}
	}

	@Override
	public void write(SelectionKey key) throws SocketException {
		String send = null;
		switch (sysStatus) {
		case init:
			send = doOption();
			break;
		case options:
			send = doDescribe();
			break;
		case describe:
			send = doSetup();
			break;
		case setup:			
			send = doPlay();
			break;
		default:
			break;
		}
		if (send != null) {
			synchronized (sendBuf) {
				System.out.println("发送内容：{" + send + "}");
				sendBuf.clear();
				sendBuf.put(send.getBytes());
				sendBuf.flip();

				// 发送出去
				try {
					while (sendBuf.hasRemaining()) {
						socketChannel.write(sendBuf);
					}
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void shutdown() {
		try {
			if (rtpSession != null) {
				rtpSession.endSession();
				rtpSession = null;
			}
			disConnected();
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (notify != null) {
			notify.shutdown();
		}
	}

	@Override
	public void run() {
		try {
			DatagramSocket rtpSocket = new DatagramSocket(client_port);
			rtpSession = new RTPSession(rtpSocket);
			rtpSession.RTPSessionRegister(this);
			Participant p = new Participant(remote_ip, 6970);
			rtpSession.addParticipant(p);
			
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			// 绑定到本地端口
			socketChannel.socket().setSoTimeout(30000);
			socketChannel.configureBlocking(false);
			socketChannel
					.connect(new InetSocketAddress(remote_ip, remote_port));
			socketChannel.register(selector, SelectionKey.OP_READ
					| SelectionKey.OP_WRITE, this);

			while (!socketChannel.finishConnect()) {
				Thread.sleep(100);
			}
			sysStatus = Status.init;
			System.out.println("启动主流程");
			while (selector != null && selector.isOpen()) {
				int n = selector.select(60000);
				System.out.println("n:" + n+" "+System.currentTimeMillis()/1000);
				if (n > 0) {
					Set<SelectionKey> keys = selector.selectedKeys();
					Iterator<SelectionKey> iter = keys.iterator();
					while (iter.hasNext()) {
						SelectionKey sk = iter.next();
						iter.remove();
						if (!sk.isValid()) {
							continue;
						}

						// 处理事件
						final IEvent handler = (IEvent) sk.attachment();
						if (sk.isReadable()) {
							handler.read(sk);
							socketChannel.register(selector,
									SelectionKey.OP_WRITE, this);
						} else if (sk.isWritable()) {
							handler.write(sk);
							socketChannel.register(selector,
									SelectionKey.OP_READ, this);
						}
					}
				} else {
					sysStatus=Status.setup;
					socketChannel.register(selector,
							SelectionKey.OP_WRITE, this);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// shutdown();
		}
	}

	public boolean checkTimeout() {
		long time = (System.currentTimeMillis() - start_time) / 1000;
		System.out.println("ip:" + remote_ip + " time:" + time + " timeout:"
				+ timeout);
		return time > timeout;
	}

	public void disConnected() throws IOException {
		if (socketChannel != null && socketChannel.isConnected()) {
			socketChannel.close();
			socketChannel = null;
		}
		if (selector != null && selector.isOpen()) {
			selector.close();
			selector = null;
		}
	}

	private void handle(String tmp) {
		if (tmp.startsWith(RTSP_OK)) {
			switch (sysStatus) {
			case init:
				sysStatus = Status.options;
				break;
			case options:
				sysStatus = Status.describe;
				int config_index = tmp.indexOf("config=");
				config = tmp.substring(config_index + 7 + 8,
						config_index + 7 + 16);
				trackInfo = tmp.substring(tmp.indexOf("track")).replaceAll(
						"\r\n", "");
				break;
			case describe:
				sysStatus = Status.setup;
				sessionid = tmp.substring(tmp.indexOf("Session: ") + 9)
						.replaceAll("\r\n", "");
				break;
			case setup:
				sysStatus = Status.play;
				break;
			case play:
				break;
			default:
				break;
			}
		} else if (tmp.startsWith(RTSP_AUTH)) {
			int a = tmp.indexOf("realm=");
			int b = tmp.indexOf("\"", a + 7);
			realm = tmp.substring(a + 7, b);
			int c = tmp.indexOf("nonce=");
			int d = tmp.indexOf("\"", c + 7);
			nonce = tmp.substring(c + 7, d);
		} else {
			System.out.println("返回错误：" + tmp);
		}

	}

	private String doPlay() {
		StringBuilder sb = new StringBuilder();
		sb.append("PLAY ");
		sb.append(this.address);
		sb.append(VERSION);
		sb.append("CSeq: ");
		sb.append(seq++);
		sb.append("\r\n");
		sb.append("Session: ");
		sb.append(sessionid);
		sb.append("\r\n");
		sb.append("Range: ");
		sb.append("npt=0.000-");
		sb.append("\r\n");
		sb.append("\r\n");
		return sb.toString();
	}

	private String doSetup() {
		StringBuilder sb = new StringBuilder();
		sb.append("SETUP ");
		sb.append(this.address);
		sb.append("/");
		sb.append(trackInfo);
		sb.append(VERSION);
		sb.append("CSeq: ");
		sb.append(seq++);
		sb.append("\r\n");
		sb.append("Transport: RTP/AVP;UNICAST;client_port=" + client_port + "-"
				+ (client_port + 1) + ";mode=play");
		sb.append("\r\n");
		sb.append("\r\n");
		return sb.toString();
	}

	private String doOption() {
		StringBuilder sb = new StringBuilder();
		sb.append("OPTIONS ");
		sb.append(this.address);
		sb.append(VERSION);
		sb.append("CSeq: ");
		sb.append(seq++);
		sb.append("\r\n");
		sb.append("\r\n");
		return sb.toString();
	}

	private String doDescribe() {
		StringBuilder sb = new StringBuilder();
		sb.append("DESCRIBE ");
		sb.append(this.address);
		sb.append(VERSION);
		sb.append("CSeq: ");
		sb.append(seq++);
		sb.append("\r\n");
		if (realm != null && nonce != null) {
			sb.append("Authorization: Digest ");
			sb.append("username=\"").append(username).append("\"");
			sb.append(",realm=\"").append(realm).append("\"");
			sb.append(",nonce=\"").append(nonce).append("\"");
			sb.append(",uri=\"").append(address).append("\"");
			String response = md5(md5(username + ":" + realm + ":" + password)
					+ ":" + nonce + ":" + md5("DESCRIBE:" + this.address));
			sb.append(",response=\"").append(response);
			sb.append("\"");
		}
		sb.append("\r\n");
		sb.append("\r\n");
		return sb.toString();
	}

	public static String md5(String str) {
		try {
			MessageDigest messageDigest = null;
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.reset();
			messageDigest.update(str.getBytes("UTF-8"));
			byte[] byteArray = messageDigest.digest();
			StringBuffer md5StrBuff = new StringBuffer();
			for (int i = 0; i < byteArray.length; i++) {
				if (Integer.toHexString(0xFF & byteArray[i]).length() == 1) {
					md5StrBuff.append("0").append(
							Integer.toHexString(0xFF & byteArray[i]));
				} else {
					md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
				}
			}
			return md5StrBuff.toString();
		} catch (Exception e) {
			return "";
		}
	}

	private void refreshFile() throws IOException {
		long time = System.currentTimeMillis();
		long index = time / (1800 * 1000);
		if (index == temp) {
			return;
		} else {
			temp = index;
		}
		if (fos != null) {
			fos.close();
		}
		dirPath = remote_ip
				+ new SimpleDateFormat("/MMdd/HH/MMdd-HHmm", Locale.CHINA)
						.format(new Date(time)) + "-" + offset + ".h264";
		size = checkSize(dir + remote_ip, max);
		File desc = new File(dir + dirPath);
		System.out.println("create file:" + desc.getPath() + " size:" + size);
		desc.getParentFile().mkdirs();
		fos = new FileOutputStream(desc);
	}

	@Override
	public int frameSize(int payloadType) {
		return 1;
	}

	@Override
	public void error(Throwable throwable) {
		shutdown();
	}

	public int checkSize(String path, long mb) {
		return (int) (getFolderSize(new File(path), 0, mb) / 1048576);
	}

	public String getTitle() {
		long second = (System.currentTimeMillis() - start_time) / 1000;
		return "R:" + receive + " L:" + lost + " " + size + "MB " + second
				+ "S";
	}

	public String getText() {
		return dirPath;
	}

	public static long getFolderSize(File dir, long size, final long max) {
		if (!dir.isDirectory()) {
			return 0;
		}
		File[] fileList = dir.listFiles();
		Comparator<File> comparator = new Comparator<File>() {

			@Override
			public int compare(File lhs, File rhs) {
				return -lhs.getName().compareTo(rhs.getName());
			}
		};
		Arrays.sort(fileList, comparator);
		for (int i = 0; i < fileList.length; i++) {
			if (fileList[i].isDirectory()) {
				size = getFolderSize(fileList[i], size, max);
			} else {
				size = size + fileList[i].length();
			}
			if (size > max) {
				boolean result = fileList[i].delete();
				System.out
						.println("delete:" + fileList[i].getPath() + " result:"
								+ result + " size:" + size + " max:" + max);
			}
		}
		return size;
	}

	@Override
	public void receiveData(DataFrame frame, Participant p) throws Exception {
		receive = p.getReceivedPktCount();
		lost = p.getLostPktCount();
		start_time = System.currentTimeMillis();
		byte[] data = frame.getConcatenatedData();
		byte[] temp = Arrays.copyOf(data, 6);
		String d = new BigInteger(temp).toString(16);
		if (d.startsWith(config)) {
			refreshFile();
			if (notify != null) {
				notify.notify(getTitle(), getText());
			}
			System.out.println(remote_ip + " start record");
			start = true;
		} else if (d.startsWith("7c")) {
			if (start) {
				if ((data[1] & 0x80) > 0) {
					fos.write(new byte[] { 0, 0, 0, 1 });
					fos.write((data[0] & 0xe0) | (data[1] & 0x1f));
				}
				fos.write(data, 2, data.length - 2);
				fos.flush();
			}
			return;
		}
		if(d.startsWith("61")||d.startsWith("67")||d.startsWith("68")){
			
		}else {			
			System.out.println(d);
		}
		if (start) {
			fos.write(new byte[] { 0, 0, 0, 1 });
			fos.write(data, 0, data.length);
			fos.flush();
		}
	}

	@Override
	public void userEvent(int type, Participant[] participant) {
		System.out.println("nbg:event" + type);
	}

	public static void main(String[] args) {
		try {
			INotify monitor = new INotify() {

				@Override
				public void shutdown() {
				}

				@Override
				public void notify(String title, String text) {
					System.out.println("title:" + title);
					System.out.println("text:" + text);
				}
			};
//			RTSPClient client1 = new RTSPClient(
//					"rtsp://xiaoni:dugudao3721@192.168.168.7:7001/mpeg4",
//					"./HOME/", 102, 10, monitor);
//			client1.start();
			RTSPClient client2 = new RTSPClient(
					"rtsp://xiaoni:dugudao3721@192.168.168.8:8001/mpeg4",
					"./HOME/", 102, 10, null);
			client2.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
