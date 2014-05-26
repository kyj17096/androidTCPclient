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
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

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
        mHandler.obtainMessage(TcpChat.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
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
        mHandler.obtainMessage(TcpChat.MESSAGE_STATE_CHANGE,STATE_CONNECTING,-1).sendToTarget();
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
        mHandler.obtainMessage(TcpChat.MESSAGE_STATE_CHANGE,STATE_NONE,-1).sendToTarget();
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
              
        mSendThread.sendHandler.obtainMessage(SEND_DATA_VIA_TCP, size, -1,out).sendToTarget();
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
    	messageForToast("Unable to connect device");
        // Start the service over to restart listening mode
        //TcpChatService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
 
    	messageForToast("Device connection was lost");
        // Start the service over to restart listening mode
       // TcpChatService.this.start();
    }

    public void messageForToast(String m )
    {      
    	Message msg = mHandler.obtainMessage(TcpChat.MESSAGE_TOAST);
	    Bundle bundle = new Bundle();
	    bundle.putString(TcpChat.TOAST, m);
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
					socket.close();					
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
    			return;
    		}

            // Send the name of the connected device back to the UI Activity
            Message msg = mHandler.obtainMessage(TcpChat.MESSAGE_REMOTE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(TcpChat.REMOTE_NAME, "server");
            msg.setData(bundle);
            mHandler.sendMessage(msg);

            setState(STATE_CONNECTED);          
            
            mRecvThread = new TcpRecvThread(socket);
            mSendThread = new TcpSendThread(socket); 
        }

        public void cancel() {
            try {
            	socket.close();
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
            byte[] buffer = new byte[1024];
            int bytes;

            
            while(true)
    		{
	    		try {	    			    	    	
	    	    	if ((bytesRcvd = in.read(buffer, 0, 1)) == -1) 
	    			{
	    				throw new SocketException("Connection closed prematurely"); 
	    			}
	    	    	
	    	    	//length = (int) decodeIntBigEndian(buffer,0,4);
		    		while (totalBytesRcvd < length)
		    		{ 
		    			if ((bytesRcvd = in.read(buffer, totalBytesRcvd, length - totalBytesRcvd)) == -1) 
		    			{
		    				throw new SocketException("Connection closed prematurely"); 
		    			}
		    			totalBytesRcvd += bytesRcvd; 
		    		    mHandler.obtainMessage(TcpChat.MESSAGE_READ, totalBytesRcvd, -1, buffer).sendToTarget();
		    		} // data array is full 
  	    	 	    	
	    		} catch (IOException e) {
					// TODO Auto-generated catch block
	    			 Log.e(TAG, "disconnected", e);
	                    connectionLost();
	                    // Start the service over to restart listening mode
	                    //TcpChatService.this.start();
	                    return;
				} // Get client connection 
	    	}
        }

        public void cancel() {
            try {
            	clntSock.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    
    
    
    public class TcpSendThread implements Runnable 
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
							
		          			  mHandler.obtainMessage(TcpChat.MESSAGE_READ, size, -1, buffer).sendToTarget();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							try {
								out.close();
								clntSock.close();					
								Looper.myLooper().quit();
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
            	out.close();
				clntSock.close();					
				Looper.myLooper().quit();
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
}
