package com.handpet.rtsp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigInteger;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import android.os.Handler;
import android.os.HandlerThread;

import com.handpet.jlibrtp.DataFrame;
import com.handpet.jlibrtp.Participant;
import com.handpet.jlibrtp.RTPAppIntf;
import com.handpet.jlibrtp.RTPSession;

public class RTSPClient implements RTPAppIntf, Runnable {
	private static final String VERSION = " RTSP/1.0\r\n";
	private static final String RTSP_OK = "RTSP/1.0 200 OK";
	private static final String RTSP_AUTH = "RTSP/1.0 401 Unauthorized";
	private boolean tcp = true;
	private byte[] receiveByte = new byte[102400];
	private Socket socket = null;

	private int client_port;

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
	private boolean login = false;

	private String dirPath;
	private File desc;
	// private boolean move;
	private int size;
	private long receive;
	private long lost;
	private Handler handler;

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
		HandlerThread handlerThread=new HandlerThread("refresh_file");
		handlerThread.start();
		handler=new Handler(handlerThread.getLooper());

		UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread thread, Throwable throwable) {
				throwable.printStackTrace();
				notify.shutdown();
			}
		};
		Thread.setDefaultUncaughtExceptionHandler(handler);
	}

	public void shutdown() {
		try {
			if (socket != null) {
				socket.close();
			}
			if (rtpSession != null) {
				rtpSession.endSession();
				rtpSession = null;
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (notify != null) {
				notify.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			checkSize(dir + remote_ip, max);
			if (!tcp) {
				DatagramSocket rtpSocket = new DatagramSocket(client_port);
				rtpSession = new RTPSession(rtpSocket);
				rtpSession.RTPSessionRegister(this);
				Participant p = new Participant(remote_ip, 6970);
				rtpSession.addParticipant(p);
			}

			socket = new Socket();
			socket.connect(new InetSocketAddress(remote_ip, remote_port));
			System.out.println("socket connect");
			sysStatus = Status.init;
			OutputStream os = socket.getOutputStream();
			DataOutputStream dos = new DataOutputStream(os);
			String send = doOption();
			System.out.println("send: {" + send + "}");
			dos.write(send.getBytes());
			InputStream is = socket.getInputStream();
			DataInputStream dis = new DataInputStream(is);
			int l = 0;
			while ((l = dis.read(receiveByte)) != -1) {
				String receive = new String(receiveByte, 0, l);
				int a = receive.indexOf("$");
				if (a != -1) {
					receive = receive.substring(0, a);
				}
				String send2 = handle(receive);
				if (send2 != null) {
					System.out.println("send: {" + send2 + "}");
					dos.write(send2.getBytes());
					dos.flush();
				}
				if (sysStatus == Status.play) {
					break;
				}
				Thread.sleep(100);
			}
			System.out.println("rtp start");
			long time = System.currentTimeMillis();
			while (true) {
				while (dis.readByte() != '$') {
				}
				int channel = dis.read();
				if (channel != 0 && channel != 1) {
					continue;
				}
				int length = dis.readShort();
				if (length < 0) {
					continue;
				}
				byte[] header = null;
				byte[] data = null;

				if (length > 12) {
					header = new byte[12];
					data = new byte[length - 12];
				} else {
					header = new byte[length];
					data = new byte[0];
				}
				if (receive % 100 == 0) {
					System.out.println(remote_ip + " receive:" + receive
							+ " length:" + length + " channel:" + channel);
				}
				dis.readFully(header);
				dis.readFully(data);

				if (channel == 0) {
					receive++;
					handlerData(data);
				} else if (channel == 1) {
					System.out.println("channel 1 length:" + length);
				}
				if (System.currentTimeMillis() - time > 30000) {
					String send2 = doPlay();
					if (send2 != null) {
						System.out.println("send: {" + send2 + "}");
						byte[] bb = send2.getBytes();
						dos.write((byte) '&');
						dos.write((byte) 1);
						dos.writeShort((short) bb.length);
						dos.write(bb);
						dos.flush();
					}
					time = System.currentTimeMillis();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			shutdown();
		} finally {
			System.out.println("rtp end");
		}
	}

	public void start() {
		new Thread(this).start();
	}

	public boolean checkTimeout() {
		// if(zhengli(dir)){
		// move=true;
		// }
		long time = (System.currentTimeMillis() - start_time) / 1000;
		System.out.println("ip:" + remote_ip + " time:" + time + " timeout:"
				+ timeout);
		if (login) {
			return time > timeout;
		} else {
			return time > 120;
		}
	}

	protected boolean zhengli(String path) {
		boolean result = false;
		File dir = new File(path);
		if (dir.isDirectory()) {
			for (File file : dir.listFiles()) {
				if (file.isFile()) {
					long time = file.lastModified();
					if (System.currentTimeMillis() - time < 5000) {
						continue;
					}
					String dirPath = new SimpleDateFormat("MMdd/HH/mm/",
							Locale.CHINA).format(new Date(time));
					File desc = new File(path + dirPath + file.getName());
					System.out.println(desc);
					desc.getParentFile().mkdirs();
					file.renameTo(desc);
					result = true;
				}
			}
		}
		return result;
	}

	private String handle(String receive) {
		System.out.println("receive:{" + receive + "}");
		if (receive.startsWith(RTSP_OK)) {
			switch (sysStatus) {
			case init:
				sysStatus = Status.options;
				return doDescribe();
			case options:
				sysStatus = Status.describe;
				int config_index = receive.indexOf("config=");
				config = receive.substring(config_index + 7 + 8,
						config_index + 7 + 16);
				trackInfo = receive.substring(receive.indexOf("track"))
						.replaceAll("\r\n", "");
				return doSetup();
			case describe:
				sysStatus = Status.setup;
				sessionid = receive.substring(receive.indexOf("Session: ") + 9)
						.replaceAll("\r\n", "");
				login = true;
				return doPlay();
			case setup:
				sysStatus = Status.play;
				break;
			default:
				break;
			}
		} else if (receive.startsWith(RTSP_AUTH)) {
			int a = receive.indexOf("realm=");
			int b = receive.indexOf("\"", a + 7);
			realm = receive.substring(a + 7, b);
			int c = receive.indexOf("nonce=");
			int d = receive.indexOf("\"", c + 7);
			nonce = receive.substring(c + 7, d);
			return doDescribe();
		} else {
			System.out.println("error data:{" + receive + "}");
		}
		return null;
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
		if (tcp) {
			sb.append("Transport: RTP/AVP/TCP;interleaved=0-1");
		} else {
			sb.append("Transport: RTP/AVP/UDP;UNICAST;client_port="
					+ client_port + "-" + (client_port + 1) + ";mode=play");
		}
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
		long index = time / (60 * 1000);
		if (index == temp) {
			return;
		} else {
			temp = index;
		}
		if (fos != null) {
			fos.close();
			// if(!move){
			// System.out.println("delete File");
			// desc.delete();
			// if(desc.getParentFile().listFiles()==null){
			// desc.getParentFile().delete();
			// }
			// }
			fileList.add(0, new FileInfo(desc.getPath(), desc.length()));
		}

		handler.post(new Runnable() {
			
			@Override
			public void run() {
				size = checkSize(dir + remote_ip, max);
				System.out.println("size:" + size);
			}
		});
		dirPath = remote_ip
				+ new SimpleDateFormat("/yyMMdd/HH/yyMMdd-HHmm", Locale.CHINA)
						.format(new Date(time)) + "-" + offset + ".h264";
		// move=false;

		desc = new File(dir + dirPath);
		System.out.println("create file:" + desc.getPath());
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
		if (notify != null) {
			notify.shutdown();
		}
	}

	public int checkSize(String path, long file_max) {
		long time = System.currentTimeMillis();
		if (fileList == null) {
			fileList = new ArrayList<FileInfo>();
			getFolderSize(new File(path));
		}

		long file_total = 0;
		for (int j = 0; j < fileList.size(); j++) {
			FileInfo fileInfo = fileList.get(j);
			file_total += fileInfo.length;
			if (file_total > file_max) {
				File file=new File(fileInfo.path);
				boolean result = file.delete();
				File parent=file.getParentFile();
				System.out.println("delete:" + fileInfo.path + " result:"
						+ result + " file_total:" + file_total + " file_max:"
						+ file_max);
				if(parent!=null){
					if(parent.listFiles()==null){
						parent.delete();
						System.out.println("delete:" + parent.getPath());
					}
					File parent2=parent.getParentFile();
					if(parent2.listFiles()==null){
						parent2.delete();
						System.out.println("delete:" + parent2.getPath());
					}
				}
				fileList.remove(j);
				j--;
			}
		}
		System.out.println("check size use time:"
				+ (System.currentTimeMillis() - time) / 1000 + " fileList:"
				+ fileList.size() + " file_total:" + file_total + " file_max:"
				+ file_max);
		return (int) file_total / 1048576;
	}

	public String getTitle() {
		long second = (System.currentTimeMillis() - start_time) / 1000;
		return "R:" + receive + " L:" + lost + " " + size + "MB " + second
				+ "S";
	}

	public String getText() {
		return dirPath;
	}

	private static List<FileInfo> fileList;

	static class FileInfo {
		private String path;
		private long length;

		FileInfo(String path, long length) {
			this.path = path;
			this.length = length;
		}
	}

	public static void getFolderSize(File dir) {
		if (!dir.isDirectory()) {
			return;
		}
		File[] files = dir.listFiles();
		Comparator<File> comparator = new Comparator<File>() {

			@Override
			public int compare(File lhs, File rhs) {
				return -lhs.getName().compareTo(rhs.getName());
			}
		};
		Arrays.sort(files, comparator);
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				getFolderSize(files[i]);
			} else {
				fileList.add(new FileInfo(files[i].getPath(), files[i].length()));
			}
		}
	}

	@Override
	public void receiveData(DataFrame frame, Participant p) throws Exception {
		receive = p.getReceivedPktCount();
		lost = p.getLostPktCount();
		byte[] data = frame.getConcatenatedData();
		handlerData(data);
	}

	private void handlerData(byte[] data) throws Exception {
		start_time = System.currentTimeMillis();
		byte[] temp = Arrays.copyOf(data, 6);
		String d = new BigInteger(temp).toString(16);
		if (d.startsWith(config)) {
			refreshFile();
			if (notify != null) {
				notify.notify(getTitle(), getText());
			}
			System.out.println(remote_ip + " start record " + d);
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
		if (d.startsWith("61") || d.startsWith("67") || d.startsWith("68")) {
		} else {
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
			// RTSPClient client1 = new RTSPClient(
			// "rtsp://xiaoni:dugudao3721@192.168.168.7:7001/mpeg4",
			// "./HOME/", 102, 10, monitor);
			// client1.start();
			RTSPClient client2 = new RTSPClient(
					"rtsp://xiaoni:dugudao3721@192.168.168.8:8001/mpeg4",
					"./HOME/", 1020, 10, monitor);
			client2.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
