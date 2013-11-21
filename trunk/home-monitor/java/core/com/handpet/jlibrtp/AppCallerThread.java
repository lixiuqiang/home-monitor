package com.handpet.jlibrtp;

import java.util.Enumeration;

public class AppCallerThread extends Thread {
	RTPSession rtpSession;
	RTPAppIntf appl;

	protected AppCallerThread(RTPSession session, RTPAppIntf rtpApp) {
		rtpSession = session;
		appl = rtpApp;
	}

	public void run() {
		try {
			while (rtpSession.endSession == false) {
				rtpSession.pktBufLock.lock();
				try {
					try {
						rtpSession.pktBufDataReady.await();
					} catch (Exception e) {
						System.out.println("AppCallerThread:" + e.getMessage());
					}
					Enumeration<Participant> enu = rtpSession.partDb
							.getParticipants();

					while (enu.hasMoreElements()) {
						Participant p = enu.nextElement();

						boolean done = false;
						while (!done
								&& (!p.unexpected || rtpSession.naiveReception)
								&& p.pktBuffer != null
								&& p.pktBuffer.length > 0) {

							DataFrame aFrame = p.pktBuffer.popOldestFrame();
							if (aFrame == null) {
								done = true;
							} else {
								appl.receiveData(aFrame, p);
							}
						}
					}

				} finally {
					rtpSession.pktBufLock.unlock();
				}
			}
		} catch (Exception e) {
			rtpSession.appIntf.error(e);
		}
	}

}
