package com.example.jeff.pm25new;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private Button button_paired;
    private Button button_find;
    private TextView show_data;
    private ListView event_listView;
    private TextView DangerText;
    private ImageView image;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> deviceName;
    private ArrayAdapter<String> deviceID;
    private Set<BluetoothDevice> pairedDevices;
    private String choseID;
    private BluetoothDevice bleDevice;
    private BluetoothSocket bluesoccket;
    private InputStream mmInputStream;
    private OutputStream mmOuputStream;
    Thread workerThread;
    private boolean stopWorker;
    private int readBufferPosition;
    private byte[] readBuffer;
    private String uid;
    //private static final int REQUEST_FINE_LOCATION_PERMISSION = 102;
    private int PMvalue = 0;
    public Resources res;
    public Bitmap a;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        res = getResources();
        a = BitmapFactory.decodeResource(res, R.drawable.a000);
        getView();
        setListener();
        deviceName = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1);
        deviceID = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1);
        DangerText.setText(" ");
        image.setImageResource(R.drawable.a000);
    }

    private void getView() {
        button_paired = (Button) findViewById(R.id.btn_paired);
        show_data = (TextView) findViewById(R.id.txtShow);
        event_listView = (ListView) findViewById(R.id.Show_B_List);
        button_find = (Button) findViewById(R.id.btn_conn);
        DangerText = (TextView) findViewById(R.id.DangerText);
        image = (ImageView) findViewById(R.id.pmimage);
    }

    private void setListener() {
        button_paired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findPBT();
            }
        });
//        button_paired.setOnClickListener(new Button.OnClickListener() {
//        @Override
//            public void onClick(View v)
//            {
//                findPBT();
//            }
//        });
        button_find.setOnClickListener(new View.OnClickListener() {
        @Override
            public void onClick(View v) {
            findPBT();
        }
    });

        event_listView.setAdapter(deviceName);
        event_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> partent, View view, int position, long id) {
                choseID = deviceID.getItem(position);
                try {
                    openBT();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(MainActivity.this, "choose :" + choseID, Toast.LENGTH_SHORT).show();
                deviceName.clear();
            }
        });
    }

    private void findPBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            show_data.setText("No bluetooth adapter available");
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 1);
        }
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String str = "Paired device :" + device.getName() + " " + device.getAddress() + "\n";
                uid = device.getAddress();
                bleDevice = device;
                deviceName.add(str);
                deviceID.add(uid);
            }
            event_listView.setAdapter(deviceName);
        }
    }

    private void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        if (bleDevice != null) {
            bluesoccket = bleDevice.createRfcommSocketToServiceRecord(uuid);
            bluesoccket.connect();
            mmOuputStream = bluesoccket.getOutputStream();
            mmInputStream = bluesoccket.getInputStream();

            beginListenForData();

            View b1 = findViewById(R.id.btn_conn);
            View b2 = findViewById(R.id.btn_paired);
            View b3 = findViewById(R.id.Show_B_List);

            b1.setVisibility(View.INVISIBLE);
            b2.setVisibility(View.INVISIBLE);
            b3.setVisibility(View.INVISIBLE);
        }
    }

    private void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10;

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];

        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                readBufferPosition++;
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    //System.arraycopy(readBuffer,i-readBufferPosition,encodedBytes,0,encodedBytes.length);
                                    System.arraycopy(readBuffer,i-readBufferPosition,encodedBytes,0,encodedBytes.length);
                                    final String data = new String(encodedBytes);
                                    PMvalue = 0;
                                    for (int j = 0; j < encodedBytes.length - 1; j++) {
                                        PMvalue = PMvalue * 10 + encodedBytes[j] - 48;
                                    }
                                    String tmp1 = String.valueOf(PMvalue);
                                    Log.d("value", tmp1);
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            long date = System.currentTimeMillis();
                                            TextView tvDisplayDate = (TextView) findViewById(R.id.DATE);
                                            SimpleDateFormat sdf = new SimpleDateFormat("MMM MM dd, yyyy h:mm a");
                                            String dateString = sdf.format(date);
                                            tvDisplayDate.setText("Update time :" + dateString);

                                            if (PMvalue < 36) {
                                                if (PMvalue > 23)
                                                    a = BitmapFactory.decodeResource(res, R.drawable.a03);
                                                else if (PMvalue > 11)
                                                    a = BitmapFactory.decodeResource(res, R.drawable.a02);
                                                else if (PMvalue > 0)
                                                    a = BitmapFactory.decodeResource(res, R.drawable.a01);
                                                image.setImageBitmap(a);
                                                DangerText.setText("良好");
                                                show_data.setText(data);
                                            } else if (PMvalue < 54) {
                                                if (PMvalue > 47)
                                                    a = BitmapFactory.decodeResource(res, R.drawable.a04);
                                                else if (PMvalue > 41)
                                                    a = BitmapFactory.decodeResource(res, R.drawable.a05);
                                                else if (PMvalue > 35)
                                                    a = BitmapFactory.decodeResource(res, R.drawable.a06);
                                                image.setImageBitmap(a);
                                                DangerText.setText("警戒");
                                                show_data.setText(data);
                                            } else if (PMvalue < 71) {
                                                if (PMvalue > 64)
                                                    a = BitmapFactory.decodeResource(res, R.drawable.a09);
                                                else if (PMvalue > 58)
                                                    a = BitmapFactory.decodeResource(res, R.drawable.a08);
                                                else if (PMvalue > 53)
                                                    a = BitmapFactory.decodeResource(res, R.drawable.a07);
                                                image.setImageBitmap(a);
                                                DangerText.setText("過量");
                                                show_data.setText(data);
                                            } else if (PMvalue > 70) {
                                                a = BitmapFactory.decodeResource(res, R.drawable.a10);
                                                image.setImageBitmap(a);
                                                DangerText.setText("危險");
                                                show_data.setText(data);
                                            }
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }

        });
        workerThread.start();
    }
}
