package com.yunzhi.tcpscclient;

import java.io.UnsupportedEncodingException;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class TcpChatActivity extends ActionBarActivity {
    // Debugging
    private static final String TAG = "TcpChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_REMOTE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
	private int HEX_FORMAT=0;
	private int TEXT_FORMAT = 1;
	private int sendForamtSelect;
	private int recvForamtSelect;
    // Key names received from the BluetoothChatService Handler
    public static final String REMOTE_NAME = "remote_name";
    public static final String TOAST = "toast";

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    // Name of the connected device
    private String mConnectedRemoteName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    
    // Local Bluetooth adapter
    // Member object for the chat services
    private TcpChatService mTcpService = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		 if(D) Log.e(TAG, "+++ ON CREATE +++");
	        // Set up the window layout
	        setContentView(R.layout.chat_view);
	
//		if (savedInstanceState == null) {
//			getSupportFragmentManager().beginTransaction()
//					.add(R.id.container, new PlaceholderFragment()).commit();
//		}
        sendForamtSelect = TEXT_FORMAT;
        recvForamtSelect = TEXT_FORMAT;
        //Log.v("hex to byte "+ Integer.valueOf(0x5a),"hex to byte " );
        setupChat();
	}

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        // If the adapter is null, then Bluetooth is not supported       
    }
    
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mTcpService = new TcpChatService(this, mHandler);
        mTcpService.start();
    }
    
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
//        if (mTcpService != null) {
//            // Only if the state is STATE_NONE, do we know that we haven't started already
//            if (mTcpService.getState() == TcpChatService.STATE_NONE) {
//              // Start the Bluetooth chat services
//            	mTcpService.start();
//            }
//        }
    }

 

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mTcpService != null) mTcpService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }


    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    byte[] sendBuf = new byte[1024];
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mTcpService.getState() != TcpChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
        	if(sendForamtSelect == HEX_FORMAT)
        	{
        		String[] strArray= null;
        		if(message.contains(","))
        		{
        			strArray = message.split(",");
        		}
        		else if(message.contains(" "))
        		{
        			strArray = message.split(" ");
        		}
        		for(int i =0;i< strArray.length;i++)
        		{
        		
        			String temp = strArray[i].trim();
        			if(temp==null)
        			{
        				Toast.makeText(this, "please input right format data, like 0x1e 0x2e 0x45 ... or 0x2e,0x2e,0x45", Toast.LENGTH_SHORT).show();
        			}
        			if(!temp.contains("0x"))
        				sendBuf[i+4] = Integer.valueOf("0x"+temp).byteValue();
        			
        			intToBigEndianArray(sendBuf,strArray.length,0,4);
        			mTcpService.write(sendBuf,strArray.length+4);
        			Log.v("send data is "+printHexOutput(sendBuf,strArray.length+4),"send data");
        		}
        		
        		
        	}
        	else
        	{
        		  
				try {
					byte[] send = message.getBytes("UTF-8");
					intToBigEndianArray(sendBuf,send.length,0,4);
					System.arraycopy(send, 0, sendBuf, 4, send.length);
					mTcpService.write(sendBuf,send.length+4);
					Log.v("send data is "+printHexOutput(sendBuf,send.length+4),"send data");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		 
                 
        	}
        
            mOutEditText.setText("");
        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

    private final void setStatus(int resId) {
        getActionBar().setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
    	getActionBar().setSubtitle(subTitle);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case TcpChatService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedRemoteName));
                    mConversationArrayAdapter.clear();
                    break;
                case TcpChatService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    break;
                case TcpChatService.STATE_NONE:
                    setStatus(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                if(sendForamtSelect == HEX_FORMAT)
                {
                	String s = "";
                	for(int i = 0;i< msg.arg1;i++)
                		s = s+" "+Integer.toHexString(writeBuf[i]);
                	mConversationArrayAdapter.add("Me:  " + s);
                }
                else
                {
                	String s = "";
					try {
						s = new String(writeBuf,0,msg.arg1,"UTF-8");
						mConversationArrayAdapter.add("Me:  " + s);
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                	
                }
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                //String readMessage = new String(readBuf, 0, msg.arg1);
                //mConversationArrayAdapter.add(mConnectedRemoteName+":  " + readMessage);
                if(recvForamtSelect == HEX_FORMAT)
                {
                	String s = "";
                	for(int i = 0;i< msg.arg1;i++)
                		s = s+" "+Integer.toHexString(readBuf[i]);
                	mConversationArrayAdapter.add(mConnectedRemoteName+":  " + s);
                }
                else
                {
                	String s = "";
					try {
						s = new String(readBuf,0,msg.arg1,"UTF-8");					
						mConversationArrayAdapter.add(mConnectedRemoteName+":  " + s);
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                	
                }
                break;
            case MESSAGE_REMOTE_NAME:
                // save the connected device's name
            	mConnectedRemoteName = msg.getData().getString(REMOTE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedRemoteName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
   

    private void connectDevice() {
        // Get the device MAC address
        mTcpService.start();
    }

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
        case R.id.connect_to_remote:
            // Launch the DeviceListActivity to see devices and do scan
        	 connectDevice();
            return true;
        case R.id.select_send_format:
            // Ensure this device is discoverable by others
        	selectSendFormat();
            return true;
        case R.id.select_receive_format:
            // Ensure this device is discoverable by others
        	selectRecvFormat();
            return true;
        }
        return false;
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
//	public static class PlaceholderFragment extends Fragment {
//
//		public PlaceholderFragment() {
//		}
//
//		@Override
//		public View onCreateView(LayoutInflater inflater, ViewGroup container,
//				Bundle savedInstanceState) {
//			View rootView = inflater.inflate(R.layout.fragment_main, container,
//					false);
//			return rootView;
//		}
//	}

    private void selectSendFormat()
    {
    	new AlertDialog.Builder(this).setTitle("select Send Format").setIcon(
    			android.R.drawable.ic_dialog_info).setSingleChoiceItems(
    					new String[] { "Hex Format", "Text Format" }, sendForamtSelect,
    					new DialogInterface.OnClickListener() {
    						public void onClick(DialogInterface dialog, int which) {
    							Log.v("radio box select is "+ which,"radio box select is "+ which);
    							if (which == 0) {
    								sendForamtSelect = HEX_FORMAT;
    			                     
    			                 } else if(which == 1){
    			                     
    			                	 sendForamtSelect = TEXT_FORMAT;
    			                 }
    							dialog.dismiss();
    							}
    						}).show();
    }
    
    private void selectRecvFormat()
    {
    	new AlertDialog.Builder(this).setTitle("select Receive Format").setIcon(
    			android.R.drawable.ic_dialog_info).setSingleChoiceItems(
    					new String[] { "Hex Format", "Text Format" }, recvForamtSelect,
    					new DialogInterface.OnClickListener() {
    						public void onClick(DialogInterface dialog, int which) {
    							if (which == 0) {
    								recvForamtSelect = HEX_FORMAT;
    			                     //DisplayToast("正确答案："+mRadioButton_2.getText()+"，恭喜你，回答正确");
    			                 } else if(which == 1){
    			                     //DisplayToast("回答错误！");
    			                	 recvForamtSelect = TEXT_FORMAT;
    			                 }
    							dialog.dismiss();
    							}
    						}).show();
    }
    
	public static long bigEndianArrayToInt(byte[] val, int offset, int size) 
	{
		long rtn = 0;
		for (int i = 0; i < size; i++)
		{ 
			rtn = (rtn << Byte.SIZE) | ((long) val[offset + i] & 0xff); 
		} 
		return rtn;
	}
	
	public static int intToBigEndianArray(byte[] dst, long val, int offset, int size) 
	{ 
		for (int i = 0; i < size; i++) 
		{ 
			dst[offset++] = (byte) (val >> ((size - i - 1) * Byte.SIZE));
		} 
		return offset; 
	}
	public static String printHexOutput(byte[] a,int size)
	{
		String s = "";
		for(int i = 0;i<size;i++)
			s = s+" "+Integer.toHexString(a[i]);
		return s;
	}
}
