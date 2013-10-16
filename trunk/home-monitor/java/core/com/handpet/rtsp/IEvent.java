package com.handpet.rtsp;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/** */
/**
 * IEvent.java �����¼�����������Selector���Խ��в���ʱ����������ӿ��еķ���. 2007-3-22 ����03:35:51
 * 
 * @author sycheng
 * @version 1.0
 */
public interface IEvent {
	/** */
	/**
	 * ��channel�ɶ�ʱ�����������.
	 * 
	 * @param key
	 * @throws IOException
	 */
	void read(SelectionKey key) throws IOException;

	/** */
	/**
	 * ��channel��дʱ�����������.
	 * 
	 * @throws IOException
	 */
	void write(SelectionKey key) throws IOException;
}
