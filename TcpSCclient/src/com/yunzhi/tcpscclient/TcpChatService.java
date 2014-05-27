/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yunzhi.tcpscclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

/**
 * This class does all the work for setting up and managing tcp
 * connections with remote server or device. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class TcpChatService {
    // Debugging
    private static final String TAG = "TcpChatService";
    private static final boolean D = true;
    
    private static final int MAX_DADA_PAYLOAD = 1024;

    private String remoteIp = "115.28.41.142";
	private int SERVER_PORT = 8000;
	private final int SEND_DATA_VIA_TCP = 1;
    // Member fields
    private final Handler mHandler;
    private TcpConnectThread mconnectToRemoteThread;
    private TcpSendThread mSendThread;
    private TcpRecvThread mRecvThread;
    private int mState;
    private Context ctx;
    private Timer mTimer;
    private KeepliveTimerTask mTimerTask;
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    private final int KEEP_LIVE_INTERVAL = 1000*60*15;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public TcpChatService(Context context, Handler handler) {
    	ctx = context;
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(TcpChatActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        if (isNetworkAvailable() == false) {
        	messageForToast("network is not available, please check if wifi or 3G data tranmit channel open");                     
            return;
        } 
        // Cancel any thread attempting to make a connection
        if (mconnectToRemoteThread != null) {mconnectToRemoteThread.cancel(); mconnectToRemoteThread = null;}

        // Cancel any thread currently running a connection
        if (mSendThread != null) {mSendThread.cancel(); mSendThread = null;}
        if (mRecvThread != null) {mRecvThread.cancel(); mRecvThread = null;}
       
        mconnectToRemoteThread = new TcpConnectThread(remoteIp, SERVER_PORT);
        mconnectToRemoteThread.start();
        setState(STATE_CONNECTING);
        mHandler.obtainMessage(TcpChatActivity.MESSAGE_STATE_CHANGE,STATE_CONNECTING,-1).sendToTarget();
      
    }


    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");

        if (mconnectToRemoteThread != null) {
        	mconnectToRemoteThread.cancel();
        	mconnectToRemoteThread = null;
        }

        if (mSendThread != null) {
        	mSendThread.cancel();
        	mSendThread = null;
        }

        if (mRecvThread != null) {
        	mRecvThread.cancel();
        	mRecvThread = null;
        }

      
        setState(STATE_NONE);
        mHandler.obtainMessage(TcpChatActivity.MESSAGE_STATE_CHANGE,STATE_NONE,-1).sendToTarget();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out,int size) {
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
        }
        // Perform the write unsynchronized
        if(mSendThread == null)Log.v("send thread mSendThread == null","send thread mSendThread == null");
        if(mSendThread.sendHandler == null)Log.v("send thread mSendThread.sendHandler == null ","send thread mSendThread.sendHandler == null");
        if(out == null)Log.v("send thread out == null","send thread out == null");
        
        mSendThread.sendHandler.obtainMessage(SEND_DATA_VIA_TCP, size, -1,out).sendToTarget();
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
    	messageForToast("Unable to connect remote server");
        // Start the service over to restart listening mode
        //TcpChatService.this.start();
    	setState(STATE_NONE);
    	mHandler.obtainMessage(TcpChatActivity.MESSAGE_STATE_CHANGE,STATE_NONE,-1).sendToTarget();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
    	setState(STATE_NONE);
    	messageForToast("Device connection was lost");
    	mHandler.obtainMessage(TcpChatActivity.MESSAGE_STATE_CHANGE,STATE_NONE,-1).sendToTarget();
        // Start the service over to restart listening mode
       // TcpChatService.this.start();
    }

    public void messageForToast(String m )
    {      
    	Message msg = mHandler.obtainMessage(TcpChatActivity.MESSAGE_TOAST);
	    Bundle bundle = new Bundle();
	    bundle.putString(TcpChatActivity.TOAST, m);
	    msg.setData(bundle);
	    mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class TcpConnectThread extends Thread {
    	String IPAddr ;
    	Socket socket;
		int servPort;

        public TcpConnectThread(String IP, int serverPort) {
        	IPAddr = IP;   
    		servPort = serverPort;    

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:"+"IP "+IPAddr+" Port "+ servPort);
            try{
    			
    			Log.v("connect","connect "+ IPAddr);
	    		socket = new Socket(IPAddr, servPort);	    		 		
	    	}    		
    		catch(IOException e){
    			e.printStackTrace();
    			connectionFailed();
    			try {
    				if(socket !=null)
    					socket.close();					
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
    			return;
    		}

            // Send the name of the connected device back to the UI Activity
            Message msg = mHandler.obtainMessage(TcpChatActivity.MESSAGE_REMOTE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(TcpChatActivity.REMOTE_NAME, "server");
            msg.setData(bundle);
            mHandler.sendMessage(msg);

            setState(STATE_CONNECTED);          
            
            mRecvThread = new TcpRecvThread(socket);
            mRecvThread.start();
            mSendThread = new TcpSendThread(socket); 
            mSendThread.start();
            initKeepLiveTimer(KEEP_LIVE_INTERVAL);
        }

        public void cancel() {
            try {
            	if(socket !=null)
            	{
            		socket.close();
            		socket = null;
            	}
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class TcpRecvThread extends Thread {
    	Socket clntSock;
    	int bytesRcvd ;
    	byte[] buffer;
    	int length;
    	int totalBytesRcvd;
    	InputStream in;

        public TcpRecvThread(Socket sk) {
            Log.d(TAG, "create ConnectedThread: ");
            clntSock = sk;
            totalBytesRcvd = 0;
            buffer = new byte[MAX_DADA_PAYLOAD];
            // Get the BluetoothSocket input and output streams
            try {
            	in = clntSock.getInputStream(); 
        		SocketAddress clientAddress = clntSock.getRemoteSocketAddress();
        		Log.v("acceptAndReceiveThread","Handling client at " + clientAddress); 
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
        }

        public void run() {
            Log.i(TAG, "BEGIN mRecvThread");
           
          
            while(true)
    		{
            	
	    		try {	    			    	    	
	    	    	if ((bytesRcvd = in.read(buffer, 0, 4)) == -1) 
	    			{
	    	    		messageForToast("Connection closed prematurely");
	    				throw new SocketException("Connection closed prematurely"); 
	    				
	    			}
	    	    	Log.v("receive length byte is "+TcpChatActivity.printHexOutput(buffer,4),"recv data");
	    	    	length = (int) TcpChatActivity.bigEndianArrayToInt(buffer,0,4);
	    	    	
		    		while (totalBytesRcvd < length)
		    		{ 
		    			if ((bytesRcvd = in.read(buffer, totalBytesRcvd, length - totalBytesRcvd)) == -1) 
		    			{
		    				messageForToast("Connection closed prematurely");
		    				throw new SocketException("Connection closed prematurely"); 
		    			}
		    			totalBytesRcvd += bytesRcvd; 
		    		   
		    		} // data array is full 
		    		Log.v("receive data  is "+TcpChatActivity.printHexOutput(buffer,totalBytesRcvd),"recv data");
		    		
		    		 mHandler.obtainMessage(TcpChatActivity.MESSAGE_READ, totalBytesRcvd, -1, buffer).sendToTarget();
		    		 totalBytesRcvd = 0;
  	    	 	    	
	    		} catch (IOException e) {
					// TODO Auto-generated catch block
	    			 Log.e(TAG, "disconnected", e);
	                    connectionLost();
	                    // Start the service over to restart listening mode
	                    //TcpChatService.this.start();
	                    return;
				} catch(RuntimeException e){
				
					connectionLost();
                    // Start the service over to restart listening mode
                    //TcpChatService.this.start();
                    return;
				}
	    	}// Get client connection 
        }

        public void cancel() {
            try {
            	if(clntSock !=null)
            	{
            		clntSock.close();
            		clntSock = null;
            	}
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    
    
    
    public class TcpSendThread extends Thread 
    { 
    	Socket clntSock;
    	int serverPort;
    	byte[] buffer;
    	int size;
    	OutputStream out = null;
    	public Handler sendHandler;
    	public TcpSendThread(Socket sk) 
    	{ 
    		clntSock = sk;
    	} 
    	           
	    public void run() 
	    {
	        Looper.prepare();   
			try {
				out = clntSock.getOutputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				messageForToast("connect faile "+ e.getMessage());
				try {
					out.close();
					clntSock.close();					
					Looper.myLooper().quit();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				return;
			}
	        
			sendHandler = new Handler() 
	        {
	            public void handleMessage(Message msg) 
	            {	
		          	  switch(msg.what)
		          	  {            	  
		          	  case SEND_DATA_VIA_TCP:              		  		          		  
		          		try {
		          			  buffer = (byte[])msg.obj;
		          			  size = msg.arg1;
		          			  out.write(buffer,0, size);		
							
		          			  mHandler.obtainMessage(TcpChatActivity.MESSAGE_WRITE, size, -1, buffer).sendToTarget();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							try {
								out.close();
								clntSock.close();					
								sendHandler.getLooper().quit();
								connectionLost();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							
						}	
		          		  break;
		          	  }
				  }
	        };
	        Looper.loop();
	    }
	    
	    public void cancel() {
            try {
            	if(out !=null)
            	{
            		out.close();
            		out = null;
            	}
            	if(clntSock !=null)
            	{
            		clntSock.close();
            		clntSock = null;
            	}
            	
				if(sendHandler.getLooper()!=null)
				{					
					sendHandler.getLooper().quit();

				}
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }    
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager 
              = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    private void initKeepLiveTimer(int seconds)
    {
    	if(mTimerTask !=null)
    	{
    		mTimerTask.cancel();
    		mTimerTask=null;
    	
    	}
    	mTimerTask = new KeepliveTimerTask();  
    	
    	if(mTimer !=null)
    	{
    		mTimer.cancel();
    		mTimer=null;
    	}
    	mTimer = new Timer();
        mTimer.schedule(mTimerTask, 5, seconds);

    }
        
    public void stopKeepLiveTimer()
    {
    	mTimer.cancel();
    	mTimer = null;
    	mTimerTask.cancel();
    	mTimerTask = null;
    }
    class KeepliveTimerTask extends TimerTask {
    	private byte[] keepLivePacket = new byte[]{0,0,0,1,0x55};
        public void run() {
        	write(keepLivePacket,keepLivePacket.length);
        }
    }
}
