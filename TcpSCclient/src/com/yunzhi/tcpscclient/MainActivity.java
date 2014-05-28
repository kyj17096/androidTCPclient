package com.yunzhi.tcpscclient;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity  extends FragmentActivity /*implements ActionBar.TabListener*/ {
	
    private ViewPager mPager;
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
	FragmentManager mFragmentManger;
	FragmentTransaction mFragmentTransaction;
	Fragment chatFragment;
	Fragment remoteListFragment;
	Fragment settingFragment;
	private String CHAT_FRAGMENT_TAG = "chat_fragment";
	private String REMOTE_LIST_FRAGMENT_TAG = "remote_device_fragment";
	private String SETTING_FRAGMENT_TAG = "setting_fragment";
    // Key names received from the BluetoothChatService Handler
    public static final String REMOTE_NAME = "remote_name";
    public static final String TOAST = "toast";
    public ArrayAdapter<String> mConversationArrayAdapter;
    
    private TcpChatService mTcpService = null;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		if(D) Log.e(TAG, "+++ ON CREATE +++");
        sendForamtSelect = TEXT_FORMAT;
        recvForamtSelect = TEXT_FORMAT;
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        
/*        mFragmentManger = getSupportFragmentManager();
        mFragmentTransaction = mFragmentManger.beginTransaction();
        chatFragment = mFragmentManger.findFragmentByTag(CHAT_FRAGMENT_TAG);
        if (chatFragment == null) {
        	chatFragment = new ChatFragment();
        	mFragmentTransaction.add(chatFragment, CHAT_FRAGMENT_TAG);
        }
        remoteListFragment = mFragmentManger.findFragmentByTag(REMOTE_LIST_FRAGMENT_TAG);
        if (remoteListFragment == null) {
        	remoteListFragment = new RemoteListFragment();
        	mFragmentTransaction.add(remoteListFragment, REMOTE_LIST_FRAGMENT_TAG);
        }
        settingFragment = mFragmentManger.findFragmentByTag(SETTING_FRAGMENT_TAG);
        if (settingFragment == null) {
        	settingFragment = new SettingFragment();
        	mFragmentTransaction.add(settingFragment, SETTING_FRAGMENT_TAG);
        }
        mFragmentTransaction.commit();
*/
        
        PagerAdapter adapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return new RemoteListFragment();
                  
                    case 1:
                        return new ChatFragment();
                  
                    case 2:
                    	return new SettingFragment();
                  
                }
                return null;
            }

            @Override
            public int getCount() {
                return 1;
            }

//            @Override
//            public CharSequence getPageTitle(int position) {
//                switch (position) {
//                    case 0:
//                        return getString(R.string.default_fragment);
//                    case 1:
//                        return getString(R.string.animation_fragment);
//                    case 2:
//                    	return getString(R.string.footer_fragment);
//                }
//                return null;
//            }
        };

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(adapter);
        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                //getActionBar().setSelectedNavigationItem(position);
            }
        });
        mPager.setCurrentItem(1);
        mPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.page_margin));

        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

//        for (int position = 0; position < adapter.getCount(); position++) {
//            getActionBar().addTab(getActionBar().newTab()
//                    .setText(adapter.getPageTitle(position))
//                    .setTabListener(this));
//        }

        getActionBar().setDisplayShowHomeEnabled(false);
        getActionBar().setDisplayShowTitleEnabled(true);
        
        
    }
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
        mTcpService = new TcpChatService(this, mHandler);
        mTcpService.start();

    }
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
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

    byte[] sendBuf = new byte[1024];
    void sendMessage(String message) {
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
        	((ChatFragment)(mFragmentManger.findFragmentByTag("chat_fragment"))).cleanOutEditView();
           
        }
    }
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
            String s = "";
            ChatFragment cf = (ChatFragment)(mFragmentManger.findFragmentByTag(CHAT_FRAGMENT_TAG));
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case TcpChatService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, cf.getConnectedRemoteName()));
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
                	
                	for(int i = 0;i< msg.arg1;i++)
                		s = s+" "+Integer.toHexString(writeBuf[i]);
             	
                }
                else
                {
					try {
						s = new String(writeBuf,0,msg.arg1,"UTF-8");					
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                	
                }
                cf.conversationAddRecords(s,true);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
            
                // construct a string from the valid bytes in the buffer
                //String readMessage = new String(readBuf, 0, msg.arg1);
                //mConversationArrayAdapter.add(mConnectedRemoteName+":  " + readMessage);
                if(recvForamtSelect == HEX_FORMAT)
                {
                	
                	for(int i = 0;i< msg.arg1;i++)
                		s = s+" "+Integer.toHexString(readBuf[i]);
                	
                	
                }
                else
                {
					try {
						s = new String(readBuf,0,msg.arg1,"UTF-8");					
						
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                	
                }
                cf.conversationAddRecords(s,false);
                break;
            case MESSAGE_REMOTE_NAME:
                // save the connected device's name
            	String remoteName = msg.getData().getString(REMOTE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + remoteName, Toast.LENGTH_SHORT).show();
                if(cf == null)Log.v("cf is == null", "cf is null");
                cf.setConnectedRemoteName(remoteName);
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
   
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
//	}
//
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		// Handle action bar item clicks here. The action bar will
//		// automatically handle clicks on the Home/Up button, so long
//		// as you specify a parent activity in AndroidManifest.xml.
//        switch (item.getItemId()) {
//        case R.id.connect_to_remote:
//            // Launch the DeviceListActivity to see devices and do scan
//        	 connectDevice();
//            return true;
//        case R.id.select_send_format:
//            // Ensure this device is discoverable by others
//        	selectSendFormat();
//            return true;
//        case R.id.select_receive_format:
//            // Ensure this device is discoverable by others
//        	selectRecvFormat();
//            return true;
//        }
//        return false;
//	}
    private void connectDevice() {
        // Get the device MAC address
        mTcpService.start();
    }
    
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
//    @Override
//    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//        mPager.setCurrentItem(tab.getPosition());
//    }
//
//    @Override
//    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//    }
//
//    @Override
//    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//    }
	
	
}

