package com.mani.speaker;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button media,connect;
    TextView mediastat,stat;
    ListView listView;
    String TAG="ak47";
    //
    private static final int READ_REQUEST_CODE = 42;
    //
    //frombts
    SendRecieve sendRecieve;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice[];
    static final int STATE_LISTNING=1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSEGE_RECIVED=5;
    int REQUEST_ENABLE_BLUETOOTH=1;
    public static final String APP_NAME="bluetooth_signal";
    public static final UUID MY_UUID= UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66");
    //bts
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        setListner();
    }

    private void setListner() {
        media.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, READ_REQUEST_CODE);
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "onItemClick: "+position+bluetoothDevice[position].getName());
                ClientClass clientClass=new ClientClass(bluetoothDevice[position]);
                clientClass.start();
            }
        });
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!bluetoothAdapter.isEnabled())
                {

                    Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent,REQUEST_ENABLE_BLUETOOTH);
                }
                //
                Log.e(TAG, "onClick: " );
                Set<BluetoothDevice> bluetoothDevices=bluetoothAdapter.getBondedDevices();
                Log.e(TAG, "onClick: bounded devices" );
                String btd[]=new String[bluetoothDevices.size()];
                Log.e(TAG, "onClick: string done" );
                int index=0;
                Log.e(TAG, "onClick: log done" );
                if(bluetoothDevices.size()>0)
                {
                    Log.e(TAG, "onClick: size>0" );
                    bluetoothDevice=new BluetoothDevice[bluetoothDevices.size()];
                    Log.e(TAG, "onClick: size set" );
                    for(BluetoothDevice bt: bluetoothDevices)
                    {
                        Log.e(TAG, "onClick: loop" );
                        bluetoothDevice[index]=bt;
                        Log.e(TAG, "onClick: name of device" );
                        btd[index++]=bt.getName();
                        Log.e(TAG, "onClick: all process done" );
                    }
                }
                Log.e(TAG, "onClick: loop done" );
                ArrayAdapter<String> arrayAdapter=new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,btd);
                Log.e(TAG, "onClick: arry adpter set" );
                listView.setAdapter(arrayAdapter);
                Log.e(TAG, "onClick: all done" );
                //
            }
        });

    }
    Uri uri;
    MediaPlayer mediaPlayer;
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.
        Log.i(TAG, "hello");

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            uri = null;
            if (resultData != null) {

                connect.setEnabled(true);
                uri = resultData.getData();
                Log.i(TAG, "Uri: " + ((Uri) uri).toString());
                Log.e(TAG, "onActivityResult: "+new File(uri.getPath()) );
                mediaPlayer=MediaPlayer.create(MainActivity.this,uri);
                mediastat.setText(uri.getLastPathSegment()+"is set as audio to play");

            }

        }
    }

    private void init() {
        media=findViewById(R.id.Media);
        connect=findViewById(R.id.Listdevice);
        connect.setEnabled(false);
        mediastat=findViewById(R.id.textView);
        stat=findViewById(R.id.textView2);
        listView=findViewById(R.id.list);
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
    }
    //handler
    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what)
            {
                case STATE_LISTNING:
                    stat.setText("listning");
                    break;
                case STATE_CONNECTING:
                    stat.setText("connecting");
                    break;
                case STATE_CONNECTED:
                    stat.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    stat.setText("failed");
                    break;
                case STATE_MESSEGE_RECIVED:
                    byte[] readBuffer=(byte[])msg.obj;
                    String tempMes=new String(readBuffer,0,msg.arg1);

                    if(tempMes.equals("CALL RECIVED")) {
                        Toast.makeText(MainActivity.this,tempMes,Toast.LENGTH_LONG).show();
                        mediaPlayer.start();
                        Log.i(TAG, "handleMessage: play music");
                    
                    }
                    else {
                        mediaPlayer.pause();
                    }
                    //rec.setText(tempMes);
                    //later
                    break;

            }
            return false;
        }
    });

    //handler
    //btm
    private class serverclass extends Thread{
        private BluetoothServerSocket bluetoothServerSocket;
        public serverclass()
        {
            try {
                bluetoothServerSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();
            BluetoothSocket socket=null;
            while (socket==null)
            {
                try {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTING;
                    handler.sendMessage(message);
                    socket=bluetoothServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }
                if(socket!=null)
                {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);
                    sendRecieve=new SendRecieve(socket);
                    sendRecieve.start();
                    break;
                }
            }

        }
    }
    private class ClientClass extends Thread{
        private BluetoothDevice bluetoothDevice;
        private BluetoothSocket bluetoothSocket;
        public ClientClass(BluetoothDevice device1)
        {
            bluetoothDevice=device1;
            Log.i(TAG, "ClientClass: "+device1.getName());
            try {
                bluetoothSocket=bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                Log.i(TAG, "ClientClass: "+"bluetoothSocket set");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void run()
        {
            try {
                bluetoothSocket.connect();
                Message message=Message.obtain();
                message.what=STATE_CONNECTED;
                handler.sendMessage(message);
                sendRecieve=new SendRecieve(bluetoothSocket);
                sendRecieve.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message=Message.obtain();
                message.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(message);

            }
        }
    }
    private class SendRecieve extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        public SendRecieve(BluetoothSocket socket)
        {
            bluetoothSocket=socket;
            InputStream tempIn=null;
            OutputStream tempOut=null;
            try {
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream=tempIn;
            outputStream=tempOut;

        }

        @Override
        public void run() {
            super.run();
            byte[] buffer =new byte[1024];
            int bytes;
            while (true)
            {
                try {
                    bytes=inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSEGE_RECIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //server client and send-recieve
}
