package com.yunzhi.tcpscclient;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class ChatFragment extends Fragment {
	
    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private String mConnectedRemoteName = null;
    Activity act; 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.chat_view, container, false);
        act = getActivity();
        mConversationArrayAdapter = new ArrayAdapter<String>(act, R.layout.message);
        mConversationView = (ListView) v.findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) v.findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) v.findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) v.findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                ((MainActivity)act).sendMessage(message);
            }
        });
        return v;
        
    }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}
	
//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        menu.add("Menu 1a").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
//        menu.add("Menu 1b").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
//    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Remember the current text, to restore if we later restart.
       // outState.putCharSequence("text", mTextView.getText());
    }
    
    private TextView.OnEditorActionListener mWriteListener =
            new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                // If the action is a key-up event on the return key, send the message
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                    String message = view.getText().toString();
                    ((MainActivity)act).sendMessage(message);
                }
                
                return true;
            }
        };
        
    public void cleanOutEditView()
    {
    	 mOutEditText.setText("");
    }
    public void cleanConversation()
    {
    	mConversationArrayAdapter.clear();
    }
    public void conversationAddRecords(String s,boolean rOw)
    {
    	if(rOw == true)
    		mConversationArrayAdapter.add("Me:  " + s);
    	else
    		mConversationArrayAdapter.add(mConnectedRemoteName+":  " + s);    
    }
    public void setConnectedRemoteName(String s)
    {
    	mConnectedRemoteName =s ;
    }
    public String getConnectedRemoteName()
    {
    	return mConnectedRemoteName ;
    }
}
