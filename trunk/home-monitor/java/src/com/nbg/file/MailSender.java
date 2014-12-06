package com.nbg.file;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import android.util.Log;

public class MailSender extends javax.mail.Authenticator{
    private String user;  
    private String password;  
    private class Text{
    	String subject;
    	String body;
    }
    private ArrayBlockingQueue<Text> queue=new ArrayBlockingQueue<Text>(100);
  
    public MailSender() {  
        this.user = "nibaogang@163.com";  
        this.password = "bonbgook";  
  
        Properties props = new Properties();  
        props.setProperty("mail.transport.protocol", "smtp");  
        props.setProperty("mail.host", "smtp.163.com");  
        props.put("mail.smtp.auth", "true");  
        props.put("mail.smtp.port", "25");  
//        props.put("mail.smtp.socketFactory.port", "465");  
//        props.put("mail.smtp.socketFactory.class",  
//                "javax.net.ssl.SSLSocketFactory");  
//        props.put("mail.smtp.socketFactory.fallback", "false");  
        props.setProperty("mail.smtp.quitwait", "true");  

        final Session session = Session.getDefaultInstance(props, this); 
        new Thread() {
			public void run() {
				while (true) {
					try {
						Text text = queue.take();
						Log.i("nbg", "take text:"+text);
						if(text!=null){							
							String time = new SimpleDateFormat("yyyy-MM-dd|HH:mm:ss")
									.format(new Date());
							MimeMessage message = new MimeMessage(session);
							DataHandler handler = new DataHandler(
									new ByteArrayDataSource(text.body.getBytes(),
											"text/plain"));
							message.setSender(new InternetAddress(
									"nibaogang@163.com"));
							message.setSubject(text.subject + "(" + time+")");
							message.setDataHandler(handler);
							message.setRecipient(Message.RecipientType.TO,
									new InternetAddress("13920294304@139.com"));
							Transport.send(message);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
    }  
  
    protected PasswordAuthentication getPasswordAuthentication() {  
        return new PasswordAuthentication(user, password);  
    }  
  
	public void sendMail(String subject, String body) {
		Text text=new Text();
		if(subject==null){
			subject="空";
		}
		if(body==null){
			body="空";
		}
		text.subject=subject;
		text.body=body;
		queue.offer(text);
	}
  
    public class ByteArrayDataSource implements DataSource {  
        private byte[] data;  
        private String type;  
  
        public ByteArrayDataSource(byte[] data, String type) {  
            super();  
            this.data = data;  
            this.type = type;  
        }  
  
        public ByteArrayDataSource(byte[] data) {  
            super();  
            this.data = data;  
        }  
  
        public void setType(String type) {  
            this.type = type;  
        }  
  
        public String getContentType() {  
            if (type == null)  
                return "application/octet-stream";  
            else  
                return type;  
        }  
  
        public InputStream getInputStream() throws IOException {  
            return new ByteArrayInputStream(data);  
        }  
  
        public String getName() {  
            return "ByteArrayDataSource";  
        }  
  
        public OutputStream getOutputStream() throws IOException {  
            throw new IOException("Not Supported");  
        }  
    }  
}
