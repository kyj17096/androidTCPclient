package com.yunzhi.tcpscclient;


import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;

	// nav drawer title
	private CharSequence mDrawerTitle;

	// used to store app title
	private CharSequence mTitle;

	// slide menu items
	private String[] navMenuTitles;
	private TypedArray navMenuIcons;

	private ArrayList<NavDrawerItem> navDrawerItems;
	private NavDrawerListAdapter adapter;

	
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
    public String mConnectedRemoteName = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		 
		if(D) Log.e(TAG, "+++ ON CREATE +++");
        sendForamtSelect = TEXT_FORMAT;
        recvForamtSelect = TEXT_FORMAT;
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        setProgressBarIndeterminateVisibility(false);
        
		mTitle = mDrawerTitle = getTitle();

		// load slide menu items
		navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items);

		// nav drawer icons from resources
		navMenuIcons = getResources()
				.obtainTypedArray(R.array.nav_drawer_icons);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.list_slidermenu);

		navDrawerItems = new ArrayList<NavDrawerItem>();

		// adding nav drawer items to array
		// Home
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[0], navMenuIcons.getResourceId(0, -1)));
		// Find People
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[1], navMenuIcons.getResourceId(1, -1)));
		// Photos
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[2], navMenuIcons.getResourceId(2, -1)));
		// Communities, Will add a counter here
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[3], navMenuIcons.getResourceId(3, -1), true, "22"));
		// Pages
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[4], navMenuIcons.getResourceId(4, -1)));
		// What's hot, We  will add a counter here
		navDrawerItems.add(new NavDrawerItem(navMenuTitles[5], navMenuIcons.getResourceId(5, -1), true, "50+"));
		

		// Recycle the typed array
		navMenuIcons.recycle();

		mDrawerList.setOnItemClickListener(new SlideMenuClickListener());

		// setting the nav drawer list adapter
		adapter = new NavDrawerListAdapter(getApplicationContext(),
				navDrawerItems);
		mDrawerList.setAdapter(adapter);

		// enabling action bar app icon and behaving it as toggle button
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, //nav menu toggle icon
				R.string.app_name, // nav drawer open - description for accessibility
				R.string.app_name // nav drawer close - description for accessibility
		) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle);
				// calling onPrepareOptionsMenu() to show action bar icons
				invalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mDrawerTitle);
				// calling onPrepareOptionsMenu() to hide action bar icons
				invalidateOptionsMenu();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState == null) {
			// on first time display view for first nav item
			displayView(0);
		}
	}

	/**
	 * Slide menu item click listener
	 * */
	private class SlideMenuClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// display view for selected nav drawer item
			displayView(position);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// toggle nav drawer on selecting action bar app icon/title
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		// Handle action bar actions click
		switch (item.getItemId()) {
		case R.id.action_settings:
			            
			return true;
		case R.id.connect_to_server:
			connectDevice();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/* *
	 * Called when invalidateOptionsMenu() is triggered
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// if nav drawer is opened, hide the action items
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Diplaying fragment view for selected nav drawer list item
	 * */
	private void displayView(int position) {
		// update the main content by replacing fragments
		Fragment fragment = null;
		switch (position) {
		case 0:
			fragment = new ChatFragment();
			break;
		case 1:
			fragment = new FindPeopleFragment();
			break;
		case 2:
			fragment = new PhotosFragment();
			break;
		case 3:
			fragment = new CommunityFragment();
			break;
		case 4:
			fragment = new PagesFragment();
			break;
		case 5:
			fragment = new WhatsHotFragment();
			break;

		default:
			break;
		}

		if (fragment != null) {
			 getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, fragment).commit();

			// update selected item and title, then close the drawer
			mDrawerList.setItemChecked(position, true);
			mDrawerList.setSelection(position);
			setTitle(navMenuTitles[position]);
			mDrawerLayout.closeDrawer(mDrawerList);
		} else {
			// error in creating fragment
			Log.e("MainActivity", "Error in creating fragment");
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}

	/**
	 * When using the ActionBarDrawerToggle, you must call it during
	 * onPostCreate() and onConfigurationChanged()...
	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
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
//        	if(sendForamtSelect == HEX_FORMAT)
//        	{
//        		String[] strArray= null;
//        		if(message.contains(","))
//        		{
//        			strArray = message.split(",");
//        		}
//        		else if(message.contains(" "))
//        		{
//        			strArray = message.split(" ");
//        		}
//        		for(int i =0;i< strArray.length;i++)
//        		{
//        		
//        			String temp = strArray[i].trim();
//        			if(temp==null)
//        			{
//        				Toast.makeText(this, "please input right format data, like 0x1e 0x2e 0x45 ... or 0x2e,0x2e,0x45", Toast.LENGTH_SHORT).show();
//        			}
//        			if(!temp.contains("0x"))
//        				sendBuf[i+4] = Integer.valueOf("0x"+temp).byteValue();
//        			
//        			intToBigEndianArray(sendBuf,strArray.length,0,4);
//        			mTcpService.write(sendBuf,strArray.length+4);
//        			Log.v("send data is "+printHexOutput(sendBuf,strArray.length+4),"send data");
//        		}
//        		
//        		
//        	}
//        	else
        	{
        		 								
				mTcpService.write(message);                 
        	}
           
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
            JSONObject result = null;
            String jsonInfo = "";
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case TcpChatService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedRemoteName));
                    setProgressBarIndeterminateVisibility(false);
                    mConversationArrayAdapter.clear();
                    break;
                case TcpChatService.STATE_CONNECTING:
                	setProgressBarIndeterminateVisibility(true);
                    setStatus(R.string.title_connecting);
                    break;
                case TcpChatService.STATE_NONE:
                    setStatus(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
//                byte[] writeBuf = (byte[]) msg.obj;
//                // construct a string from the buffer
//                if(sendForamtSelect == HEX_FORMAT)
//                {
//                	
//                	for(int i = 0;i< msg.arg1;i++)
//                		s = s+" "+Integer.toHexString(writeBuf[i]);
//             	
//                }
//                else
//                {
//					try {
//						s = new String(writeBuf,0,msg.arg1,"UTF-8");					
//					} catch (UnsupportedEncodingException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//                	
//                }

                try {
					
                	result = new JSONObject((String)msg.obj);
					String data = (String) result.get("command");
					if(data.equals("connect_status"))
					{
						jsonInfo = "connecting to server";
					}
					else if(data.equals("login_in"))
					{
						jsonInfo = "logining";
					}
					else if(data.equals("data_to_peer"))
					{
						jsonInfo = (String) result.get("command_to_device");
						
					} 
					if(!jsonInfo.equals(""))
						mConversationArrayAdapter.add("Me:  " + jsonInfo);
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
                break;
            case MESSAGE_READ:
//                byte[] readBuf = (byte[]) msg.obj;
//            
//                // construct a string from the valid bytes in the buffer
//                //String readMessage = new String(readBuf, 0, msg.arg1);
//                //mConversationArrayAdapter.add(mConnectedRemoteName+":  " + readMessage);
//                if(recvForamtSelect == HEX_FORMAT)
//                {
//                	
//                	for(int i = 0;i< msg.arg1;i++)
//                		s = s+" "+Integer.toHexString(readBuf[i]);
//                	
//                	
//                }
//                else
//                {
//					try {
//						s = new String(readBuf,0,msg.arg1,"UTF-8");					
//						
//					} catch (UnsupportedEncodingException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//                	
//                }
 
				try {
					result = new JSONObject((String)msg.obj);			
					String data = (String) result.get("command");
					if(data.equals("connect_status"))
					{
						jsonInfo = (String) result.get("connect_status");
						
					}
					else if(data.equals("login_status"))
					{
						jsonInfo = (String) result.get("login_status");
						
					}
					else if(data.equals("data_to_peer"))
					{
						jsonInfo = (String) result.get("command_to_device");
				
					} 
					mConversationArrayAdapter.add(mConnectedRemoteName+":  " + jsonInfo ); 
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
                 
                break;
            case MESSAGE_REMOTE_NAME:
                // save the connected device's name
            	String remoteName = msg.getData().getString(REMOTE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + remoteName, Toast.LENGTH_SHORT).show();
                mConnectedRemoteName = remoteName;
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
	
	   public static boolean isGpsEnabled(Context context) {   
	        LocationManager lm = ((LocationManager) context   
	                .getSystemService(Context.LOCATION_SERVICE));   
	        List<String> accessibleProviders = lm.getProviders(true);   
	        return accessibleProviders != null && accessibleProviders.size() > 0;   
	    } 
	  
	  public static boolean isWifiEnabled(Context context) {   
	      ConnectivityManager mgrConn = (ConnectivityManager) context   
	              .getSystemService(Context.CONNECTIVITY_SERVICE);   
	      TelephonyManager mgrTel = (TelephonyManager) context   
	              .getSystemService(Context.TELEPHONY_SERVICE);   
	      return ((mgrConn.getActiveNetworkInfo() != null && mgrConn   
	              .getActiveNetworkInfo().getState() == NetworkInfo.State.CONNECTED) || mgrTel   
	              .getNetworkType() == TelephonyManager.NETWORK_TYPE_UMTS);   
	  } 
	  
	  public static boolean is3Genable(Context context) {   
	      ConnectivityManager cm = (ConnectivityManager) context   
	              .getSystemService(Context.CONNECTIVITY_SERVICE);   
	      NetworkInfo networkINfo = cm.getActiveNetworkInfo();   
	      if (networkINfo != null   
	              && networkINfo.getType() == ConnectivityManager.TYPE_MOBILE) {   
	          return true;   
	      }   
	      return false;   
	  }  

	  public static boolean isWifi(Context context) {   
	      ConnectivityManager cm = (ConnectivityManager) context   
	              .getSystemService(Context.CONNECTIVITY_SERVICE);   
	      NetworkInfo networkINfo = cm.getActiveNetworkInfo();   
	      if (networkINfo != null   
	              && networkINfo.getType() == ConnectivityManager.TYPE_WIFI) {   
	          return true;   
	      }   
	      return false;   
	  }
}
