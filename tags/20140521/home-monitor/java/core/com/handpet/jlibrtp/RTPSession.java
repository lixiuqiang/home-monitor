/**
 * Java RTP Library (jlibrtp)
 * Copyright (C) 2006 Arne Kepp
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.handpet.jlibrtp;

import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The RTPSession object is the core of jlibrtp.
 * 
 * One should be instantiated for every communication channel, i.e. if you send
 * voice and video, you should create one for each.
 * 
 * The instance holds a participant database, as well as other information about
 * the session. When the application registers with the session, the necessary
 * threads for receiving and processing RTP packets are spawned.
 * 
 * RTP Packets are sent synchronously, all other operations are asynchronous.
 * 
 * @author Arne Kepp
 */
public class RTPSession {
	/**
	 * The debug level is final to avoid compilation of if-statements.</br> 0
	 * provides no debugging information, 20 provides everything </br> Debug
	 * output is written to System.out</br> Debug level for RTP related things.
	 */
	final static public int rtpDebugLevel = 0;
	/** RTP unicast socket */
	protected DatagramSocket rtpSock = null;
	/** RTP multicast socket */
	protected MulticastSocket rtpMCSock = null;
	/** Whether this session is a multicast session or not */
	protected boolean mcSession = false;
	/** SSRC of this session */
	protected long ssrc;
	/** Current sequence number */
	protected int seqNum = 0;
	/** The random seed */
	protected Random random = null;
	/** By default we do not return packets from strangers in unicast mode */
	protected boolean naiveReception = false;
	/** Should the library attempt frame reconstruction? */
	public boolean frameReconstruction = true;
	/** Maximum number of packets used for reordering */
	public int pktBufBehavior = 3;
	/** Participant database */
	public ParticipantDatabase partDb = new ParticipantDatabase(this);
	/** Handle to application interface for RTP */
	public RTPAppIntf appIntf = null;
	/** The RTCP session associated with this RTP Session */
	/** The thread for receiving RTP packets */
	private RTPReceiverThread recvThrd = null;
	/** The thread for invoking callbacks for RTP packets */
	private AppCallerThread appCallerThrd = null;
	/** Lock to protect the packet buffers */
	final public Lock pktBufLock = new ReentrantLock();
	/** Condition variable, to tell the */
	final public Condition pktBufDataReady = pktBufLock.newCondition();
	/** endSession */
	public boolean endSession=false;
	
	/** Only one registered application, please */
	private boolean registered = false;
	/** Number of conflicts observed, exessive number suggests loop in network */
	private int conflictCount = 0;
	/** SDES CNAME */
	private String cname = null;


	/**
	 * Returns an instance of a <b>unicast</b> RTP session. Following this you
	 * should adjust any settings and then register your application.
	 * 
	 * The sockets should have external ip addresses, else your CNAME
	 * automatically generated CNAMe will be bad.
	 * 
	 * @param rtpSocket
	 *            UDP socket to receive RTP communication on
	 * @param rtcpSocket
	 *            UDP socket to receive RTCP communication on, null if none.
	 */
	public RTPSession(DatagramSocket rtpSocket) {
		mcSession = false;
		rtpSock = rtpSocket;
		this.generateCNAME();
		this.generateSsrc();

		// The sockets are not always imediately available?
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			System.out.println("RTPSession sleep failed");
		}
	}

	/**
	 * Registers an application (RTPAppIntf) with the RTP session. The session
	 * will call receiveData() on the supplied instance whenever data has been
	 * received.
	 * 
	 * Following this you should set the payload type and add participants to
	 * the session.
	 * 
	 * @param rtpApp
	 *            an object that implements the RTPAppIntf-interface
	 * @param rtcpApp
	 *            an object that implements the RTCPAppIntf-interface (optional)
	 * @return -1 if this RTPSession-instance already has an application
	 *         registered.
	 */
	public int RTPSessionRegister(RTPAppIntf rtpApp) {
		if (registered) {
			System.out
					.println("RTPSessionRegister(): Can\'t register another application!");
			return -1;
		} else {
			registered = true;
			generateSeqNum();
			if (RTPSession.rtpDebugLevel > 0) {
				System.out.println("-> RTPSessionRegister");
			}
			this.appIntf = rtpApp;

			recvThrd = new RTPReceiverThread(this);
			appCallerThrd = new AppCallerThread(this, rtpApp);
			recvThrd.start();
			appCallerThrd.start();
			return 0;
		}
	}

	/**
	 * Add a participant object to the participant database.
	 * 
	 * If packets have already been received from this user, we will try to
	 * update the automatically inserted participant with the information
	 * provided here.
	 * 
	 * @param p
	 *            A participant.
	 */
	public int addParticipant(Participant p) {
		p.unexpected = false;
		return this.partDb.addParticipant(0, p);
	}

	/**
	 * End the RTP Session. This will halt all threads and send bye-messages to
	 * other participants.
	 * 
	 * RTCP related threads may require several seconds to wake up and
	 * terminate.
	 */
	public void endSession() {
		// No more RTP packets, please
		endSession=true;
		if (this.mcSession) {
			this.rtpMCSock.close();
		} else {
			this.rtpSock.close();
		}

		// Signal the thread that pushes data to application
		this.pktBufLock.lock();
		try {
			this.pktBufDataReady.signalAll();
		} finally {
			this.pktBufLock.unlock();
		}
		// Give things a chance to cool down.
		try {
			Thread.sleep(50);
		} catch (Exception e) {
		}

		this.appCallerThrd.interrupt();

		// Give things a chance to cool down.
		try {
			Thread.sleep(50);
		} catch (Exception e) {
		}
	}

	/**
	 * Overrides CNAME, used for outgoing RTCP packets.
	 * 
	 * @param cname
	 *            a string, e.g. username@hostname. Must be unique for session.
	 */
	public void CNAME(String cname) {
		this.cname = cname;
	}

	/**
	 * Get the current CNAME, used for outgoing SDES packets
	 */
	public String CNAME() {
		return this.cname;
	}

	public long getSsrc() {
		return this.ssrc;
	}

	private void generateCNAME() {
		String hostname;

		if (this.mcSession) {
			hostname = this.rtpMCSock.getLocalAddress().getCanonicalHostName();
		} else {
			hostname = this.rtpSock.getLocalAddress().getCanonicalHostName();
		}
		cname = System.getProperty("user.name") + "@" + hostname;
	}

	/**
	 * Initializes a random variable
	 * 
	 */
	private void createRandom() {
		this.random = new Random(System.currentTimeMillis()
				+ Thread.currentThread().getId()
				- Thread.currentThread().hashCode() + this.cname.hashCode());
	}

	/**
	 * Generates a random sequence number
	 */
	private void generateSeqNum() {
		if (this.random == null)
			createRandom();

		seqNum = this.random.nextInt();
		if (seqNum < 0)
			seqNum = -seqNum;
		while (seqNum > 65535) {
			seqNum = seqNum / 10;
		}
	}

	/**
	 * Generates a random SSRC
	 */
	private void generateSsrc() {
		if (this.random == null)
			createRandom();

		// Set an SSRC
		this.ssrc = this.random.nextInt();
		if (this.ssrc < 0) {
			this.ssrc = this.ssrc * -1;
		}
	}

	/**
	 * Resolve an SSRC conflict.
	 * 
	 * Also increments the SSRC conflict counter, after 5 conflicts it is
	 * assumed there is a loop somewhere and the session will terminate.
	 * 
	 */
	protected void resolveSsrcConflict() {
		System.out
				.println("!!!!!!! Beginning SSRC conflict resolution !!!!!!!!!");
		this.conflictCount++;

		if (this.conflictCount < 5) {
			// Generate a new Ssrc for ourselves
			generateSsrc();
			System.out.println("SSRC conflict resolution complete");

		} else {
			System.out
					.println("Too many conflicts. There is probably a loop in the network.");
			this.endSession();
		}
	}
}
