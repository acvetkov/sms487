package com.example.andre487.sms487;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.example.andre487.sms487.messages.MessageContainer;
import com.example.andre487.sms487.messages.MessageStorage;
import com.example.andre487.sms487.services.SmsHandler;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    static class GetMessageParams {
        MessageStorage messageStorage;

        GetMessageParams(MessageStorage messageStorage) {
            this.messageStorage = messageStorage;
        }
    }

    static class GetMessageAction extends AsyncTask<GetMessageParams, Void, ArrayList<MessageContainer>> {
        @Override
        protected ArrayList<MessageContainer> doInBackground(GetMessageParams... params) {
            if (params.length == 0) {
                Log.w("GetMessageAction", "Params length is 0");
                return null;
            }

            GetMessageParams mainParams = params[0];

            return mainParams.messageStorage.getMessagesTail();
        }
    }

    static class NewSmsHandler extends Handler {
        MainActivity activity;

        NewSmsHandler(MainActivity activity) {
            super();
            this.activity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d("MainActivity", "Got incoming SMS: " + msg.toString());
            // TODO: Implement auto refresh
        }

        void destroy() {
            activity = null;
        }
    }

    protected NewSmsHandler incomingSmsHandler = new NewSmsHandler(this);

    protected ServiceConnection smsHandlerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SmsHandler.SmsBridge binder = (SmsHandler.SmsBridge) service;
            smsHandler = binder.getService();
            smsHandler.setNewSmsHandler(MainActivity.this, incomingSmsHandler);
            smsHandlerBound = true;

            Log.d("MainActivity", "SmsHandler bound");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            smsHandlerBound = false;
            smsHandler.removeNewSmsHandler(MainActivity.this);

            Log.d("MainActivity", "SmsHandler unbound");
        }
    };

    protected MessageStorage messageStorage;
    protected SmsHandler smsHandler;
    protected boolean smsHandlerBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("MainActivity", "Activity is started");

        messageStorage = new MessageStorage(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        showMessagesFromDb();

        Intent intent = new Intent(this, SmsHandler.class);
        bindService(intent, smsHandlerConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (smsHandlerBound) {
            unbindService(smsHandlerConnection);
            smsHandlerBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        incomingSmsHandler.destroy();
    }

    protected void showMessagesFromDb() {
        Log.d("MainActivity", "Showing messages from DB");

        ArrayList<MessageContainer> messages = getMessages();
        if (messages == null) {
            Log.w("MainActivity", "Messages are null");
            return;
        }

        Log.d("MainActivity", "Messages are not null");

        for (MessageContainer message : messages) {
            Log.d(
                    "MainActivity",
                    "Message: " + message.getBody() + " " + message.getDateTime() + " " + message.getAddressFrom()
            );
        }
    }

    private ArrayList<MessageContainer> getMessages() {
        GetMessageParams params = new GetMessageParams(messageStorage);
        GetMessageAction action = new GetMessageAction();
        action.execute(params);

        try {
            return action.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.w("MainActivity", "Get messages error: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }
}
