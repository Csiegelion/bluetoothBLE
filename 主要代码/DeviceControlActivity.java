/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.appli_ISCAPI;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;


/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private BluetoothGatt mBluetoothGatt;

    private TextView mConnectionState;
    private TextView mDataField;
    private TextView muuidField;
    private TextView mVoltageField; // on s'en servira pour afficher la derni??re valeur de tension lue
    private TextView mUnite;
    private EditText VDC;
    private String mDeviceName;
    private String mDeviceAddress;

    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private String expression = "pressure";
    private int oneread;
    private int longread;
    private int other=1;
    private int DT;
    private int VC;


    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";


    private double startTime = 0.0;
    private ArrayList<Entry> valuesTension = new ArrayList<>(); // permettra de stocker les donn??es re??ues
    private Handler handler = new Handler(); // d??finition de la fonction pour le traitement des donn??es re??ues

    private int updatePeriod = 1000; // initialisation de la p??riode de mise ?? jour (en ms)
    private int RT0=1000;// initialisation de la risistance de capteur
    private int Sd=3850;// initialisation de la sensibilit?? de capteur
    private double Vd=3.3;// initialisation de la tension d'alimentation
    private float Vdrecu;
    private int Rp=1000;// initialisation de la risistance de capteur de ??

    private LineChart chartTension; // d??finition du graphe
    private Switch aSwitchRun; // d??claration du switch Run contenu dans l'interface graphique
    private Button buttonClear; // d??claration du bouton Clear contenu dans l'interface graphique
    private Switch disT;
    private Switch Vcrt;
    private RadioButton P;
    private int maximumDataSet = 20; // permettra de r??cuperer les 20 derni??res valeurs dans la liste valuesTension
    private String receiveBuffer ="";
    private String receivevdd ="";



    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                mConnectionState.setText("Connected (wait)");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                mConnectionState.setText("click \"read service\" et choisir \"temperature sensor\" ou \"VDD sensor\"");
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {



                        if (oneread == 1 && longread == 0) {
                            displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                            oneread = 0;
                        }
                        if(longread == 1&&other==1) { // le handler est lanc?? p??riodiquement


                            displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                            other=0;

                }



                                   /*  try {
                        Thread.sleep(updatePeriod);
                    } catch (InterruptedException e)
                    { e.printStackTrace();
                    }*/

                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));


            }
        }
    };


    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
    };



    // Fonction de remise ?? z??ro:
    private void clearUI() {
        //mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
        mDataField.setText(R.string.no_data);;// efface la trame
        mVoltageField.setText(R.string.no_data);
        muuidField.setText("");// efface le champ Voltage (derni??re valeur de tension affich??e)
            receiveBuffer=""; // vide le buffer
            valuesTension.clear(); // efface le trac?? de graphe
            startTime = 0.0; // red??finit l'??chelle de temps pour l'affichage de donn??es ?? 0
            updateCharts();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.gatt_services_characteristics);
        setContentView(R.layout.button_control);

        // ce bloc permet de g??rer la modification de la p??riode d'??chantillonage
        final EditText updatePeriodModified=(EditText)findViewById(R.id.block_Tsampling);
        final EditText RT0Modified=(EditText)findViewById(R.id.block_RT0);
        final EditText SdModified=(EditText)findViewById(R.id.block_Sd);
        final EditText VdModified=(EditText)findViewById(R.id.block_VD);
        final EditText RpModified=(EditText)findViewById(R.id.block_Rp);

        /*RT0= Integer.parseInt(String.valueOf(RT0Modified.getText()));// initialisation de la risistance de capteur
        Sd= Integer.parseInt(String.valueOf(SdModified.getText()));// initialisation de la sensibilit?? de capteur
        Vd= Integer.parseInt(String.valueOf(VdModified.getText()));
        Rp= Integer.parseInt(String.valueOf(RpModified.getText()));// on fait le lien avec l'objet graphique (pr??sent sur button_control.xml)*/
       updatePeriodModified.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Toast.makeText(DeviceControlActivity.this, "Entrez une valeur enti??re", Toast.LENGTH_LONG).show(); // on pr??cise qu'il faut entrer une valeur enti??re
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String str = updatePeriodModified.getText().toString();
                if (str.equals("")) { // v??rification: au cas o?? la chaine est vide
                    updatePeriod = 1000; // on d??finit par d??faut une p??riode d'??chantillonnage ?? 1s si la case est laiss??e vide
                    Toast.makeText(DeviceControlActivity.this, "Merci de ne pas laisser la case vide, Tsampling =" + updatePeriod + "ms", Toast.LENGTH_LONG).show();
                }
                else {
                    updatePeriod = Integer.parseInt(String.valueOf(updatePeriodModified.getText())); // on s'assure que la nouvelle valeur de p??riode d'??chantillonnage est bien prise en compte
                    Toast.makeText(DeviceControlActivity.this, "Tsampling =" + updatePeriod + "ms", Toast.LENGTH_LONG).show();
                    /*RT0= Integer.parseInt(String.valueOf(RT0Modified.getText()));
                    Sd= Integer.parseInt(String.valueOf(SdModified.getText()));
                    Vd= Integer.parseInt(String.valueOf(VdModified.getText()));
                    Rp= Integer.parseInt(String.valueOf(RpModified.getText()));*/
                }
               /* RT0 = Integer.parseInt(RT0Modified.getText().toString());
                Sd= Integer.parseInt(SdModified.getText().toString());
                Vd= Double.parseDouble(VdModified.getText().toString());
                Rp= Integer.parseInt(RpModified.getText().toString());*/
            }
        });
        RT0Modified.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String str = RT0Modified.getText().toString();
                if (str.equals("")) { // v??rification: au cas o?? la chaine est vide
                    RT0 = 1000; // on d??finit par d??faut une p??riode d'??chantillonnage ?? 1s si la case est laiss??e vide

                }
                else{
                RT0 = Integer.parseInt(RT0Modified.getText().toString());}
                /*Sd= Integer.parseInt(SdModified.getText().toString());
                Vd= Double.parseDouble(VdModified.getText().toString());
                Rp= Integer.parseInt(RpModified.getText().toString());*/
            }
        });
        SdModified.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Toast.makeText(DeviceControlActivity.this, "Entrez une valeur enti??re", Toast.LENGTH_LONG).show(); // on pr??cise qu'il faut entrer une valeur enti??re
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String str = SdModified.getText().toString();
                if (str.equals("")) { // v??rification: au cas o?? la chaine est vide
                    Sd = 3850; // on d??finit par d??faut une p??riode d'??chantillonnage ?? 1s si la case est laiss??e vide

                }
                else{
                    Sd= Integer.parseInt(String.valueOf(SdModified.getText()));}
                    /*Vd= Integer.parseInt(String.valueOf(VdModified.getText()));
                    Rp= Integer.parseInt(String.valueOf(RpModified.getText()));*/
                }
               /* RT0 = Integer.parseInt(RT0Modified.getText().toString());
                Sd= Integer.parseInt(SdModified.getText().toString());
                Vd= Double.parseDouble(VdModified.getText().toString());
                Rp= Integer.parseInt(RpModified.getText().toString());*/

        });
        VdModified.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Toast.makeText(DeviceControlActivity.this, "Entrez une valeur enti??re", Toast.LENGTH_LONG).show(); // on pr??cise qu'il faut entrer une valeur enti??re
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String str = VdModified.getText().toString();
                if (str.equals("")) { // v??rification: au cas o?? la chaine est vide
                    Vd = 3.3; // on d??finit par d??faut une p??riode d'??chantillonnage ?? 1s si la case est laiss??e vide

                }
                else{
                    Vd= Double.parseDouble(String.valueOf(VdModified.getText()));}
                 /*   Rp= Integer.parseInt(String.valueOf(RpModified.getText()));*/

               /* RT0 = Integer.parseInt(RT0Modified.getText().toString());
                Sd= Integer.parseInt(SdModified.getText().toString());
                Vd= Double.parseDouble(VdModified.getText().toString());
                Rp= Integer.parseInt(RpModified.getText().toString());*/
            }
        });
        RpModified.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //Toast.makeText(DeviceControlActivity.this, "Entrez une valeur enti??re", Toast.LENGTH_LONG).show(); // on pr??cise qu'il faut entrer une valeur enti??re
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String str = RpModified.getText().toString();
                if (str.equals("")) { // v??rification: au cas o?? la chaine est vide
                     ; // on d??finit par d??faut une p??riode d'??chantillonnage ?? 1s si la case est laiss??e vide

                }
                else{
                    Rp= Integer.parseInt(String.valueOf(RpModified.getText()));}

               /* RT0 = Integer.parseInt(RT0Modified.getText().toString());
                Sd= Integer.parseInt(SdModified.getText().toString());
                Vd= Double.parseDouble(VdModified.getText().toString());
                Rp= Integer.parseInt(RpModified.getText().toString());*/
            }
        });
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        //mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        //mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);// on fait le lien avec l'objet graphique
        mVoltageField = (TextView) findViewById(R.id.voltage); // on fait le lien avec l'objet graphique
        mUnite=(TextView) findViewById(R.id.head_voltage);
        muuidField = (TextView) findViewById(R.id.uuid);
        VDC=(EditText) findViewById(R.id.block_VD);
        // ??????MultiAutoCompleteTextView
       /* muuidField = (MultiAutoCompleteTextView) findViewById(R.id.uuid);
        rempuuid();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, res);
        muuidField.setAdapter(adapter);
        muuidField.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());*/



        getActionBar().setTitle(mDeviceName); // on affiche le nom de la carte sur l'interface graphique
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        initializeCharts(); // on initialise le graphe

        aSwitchRun = findViewById(R.id.switchRun); // on fait le lien avec l'objet graphique (switch "Run" sur l'interface)

        buttonClear = findViewById(R.id.buttonClear); // on fait le lien avec l'objet graphique (bouton "Clear" sur l'interface)
        disT=findViewById(R.id.switchDT);
        Vcrt=findViewById(R.id.switchVC);
        P=findViewById(R.id.radioVDD);
       // P.setChecked(true);
        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearUI(); // supprime le contenu du trac??
            }
        });
      /* handler.postDelayed(new Runnable() { // le handler est lanc?? p??riodiquement
            @Override
            public void run()
            {
                handler.postDelayed(this, updatePeriod);
                try {
                    if(mConnected && aSwitchRun.isChecked()) // v??rifie que l'on a bien demand?? le lancement de la mesure automatique (et qu'on est connect?? au bon service)
                        // ?? intervalle r??gulier, on enverra une requ??te Read pour recevoir les valeurs de tension
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }, updatePeriod);*/


    }
    // fonction d'initialisation du graphe: on utilise la librairie MPAndroidChart
    private void initializeCharts() {

        chartTension = findViewById(R.id.chartTension); // cette ligne permet de lier l'objet au graphe (d??fini dans button_control.xml)
        chartTension.setDrawGridBackground(false); // on n'affiche pas de grille en fond du trac??
        //chartTension.setBackgroundColor();

        // no description text
        chartTension.getDescription().setEnabled(false);

        // enable touch gestures
        chartTension.setTouchEnabled(true); // activer l'interaction avec le graphe

        // enable scaling and dragging
        chartTension.setDragEnabled(true); // pour pouvoir d??placer le graphe avec les doigts
        chartTension.setScaleEnabled(true); // gestion du zoom
        chartTension.setScaleY(1.00f); // pour afficher les valeurs avec deux chiffres apr??s la virgule (sur le graphe)

        // if disabled, scaling can be done on x- and y-axis separately
        chartTension.setPinchZoom(true); // pour pouvoir zoomer sur les deux axes X et Y en m??me temps

        chartTension.getAxisLeft().setDrawGridLines(false);
        chartTension.getAxisRight().setEnabled(false);

        XAxis xAxis = chartTension.getXAxis();
        xAxis.setEnabled(true);//???????????????????????? ??????????????????????????????????????????
        xAxis.setDrawAxisLine(true);//??????????????????
        xAxis.setDrawGridLines(false);//??????x???????????????????????????
        xAxis.setDrawLabels(true);//????????????  ???x?????????????????????
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

    }

    // onResume, onPause, onDestroy... gestion du cycle de vie de l'application
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        Log.i(TAG, "ReceiveBuffer est non vide");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    // on ne s'en sert pas mais vous pourriez en avoir besoin pour g??rer plusieurs services
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) { // pour afficher l'??tat de connexion au service
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

 /* private void rempuuid(){

        List<BluetoothGattService>  ms;
            List<BluetoothGattCharacteristic> mc;
            ms=mBluetoothGatt.getServices();

            //get list of services
           /* is=ms.size();//number of services
        int a=0;
         for(int i=0; i<is;i++) {
            mc = ms.get(i).getCharacteristics();  //get list of Characteristics
            ic = mc.size();
            res[a]=i + 1 + "e service a " + ic + " characteristic1";
            Log.d(TAG, res[a]);
            a++;
            res[a]="UUID service " + i + ":" + ms.get(i).getUuid();
            Log.d(TAG, res[a]);
            a++;
            for (int x = 0; x < ic; x++)
                { res[a]="\n UUID characteristic " + x + " :" + mc.get(x).getUuid();
                    Log.d(TAG, res[a]);
                a++;
                }
            }*/





    // fonction de r??cup??ration et d'affichage de donn??es brutes
    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data); // on affiche la trame brute re??ue dans le champ mDataField (qu'on a li?? pr??c??demment ?? l'objet graphique de type TextView)
            //displayuuid();
            int longueur = data.length(); //longueur du message re??u
            receiveBuffer = data.substring(longueur - 24, longueur - 1);
            Log.i(TAG, "message datas :" + data.substring(0, longueur - 25) + receiveBuffer);
            String substring = data.substring(longueur - 22, longueur - 9);
            String substring1 = data.substring(longueur - 6, longueur - 1);
            receivevdd = substring1;
           if(expression=="temperature")
           { receiveBuffer = substring; //cette ligne charge le buffer avec les 10 bytes qui nous int??ressent (il y a des espaces ?? prendre en compte)
            Log.i(TAG, "message coup?? : " + receiveBuffer);}
            if(expression=="vdd")
            { receiveBuffer = substring1; //cette ligne charge le buffer avec les 10 bytes qui nous int??ressent (il y a des espaces ?? prendre en compte)
                Log.i(TAG, "message coup?? : " + receiveBuffer);}
            // la chaine contient un whitespace qui ne veut pas ??tre consid??r?? comme un espace (pour l'int??grit?? de la donn??e), on ne peut donc pas le supprimer
            // cependant, on supprime tous les espaces effa??ables (le premier et la dernier) avec la commande suivante:
            receiveBuffer.replaceAll("\\p{Z}", "");

            Log.i(TAG, "message coup?? avec espaces supprim??s :" + receiveBuffer);
            Log.i(TAG, "longueur de ce message :" + receiveBuffer.length());
            vddHandler();
            if (receiveBuffer != null) {
                Log.i(TAG, "ReceiveBuffer est non vide");
                messageHandler();
                // si on re??oit une trame de donn??e correcte on lance la fonction messageHandler() (traitement et trac?? des donn??es)
            }
            receiveBuffer = ""; // on vide le buffer pour qu'il soit pr??t ?? traiter la trame de donn??es suivante

        }
        else{}
    }
    private void vddHandler() {
        if (receivevdd !=null) {

            int l=0;
            String danslordre1="";
            String receipt[] = new String[4]; //on r??cup??re les bytes et on les met dans le bon ordre (cas o?? on travaillait seulement sur data_t,
                receipt[0] = String.valueOf(receivevdd.charAt(3));
                receipt[1] = String.valueOf(receivevdd.charAt(4));
                receipt[2] = String.valueOf(receivevdd.charAt(0));
                receipt[3] = String.valueOf(receivevdd.charAt(1));
                for (l = 0; l < receipt.length; l++) {
                    danslordre1 += receipt[l]; // recollage du message dans le bon ordre
                }
                Integer valeurfois100 = Integer.parseInt(danslordre1, 16); // on recup??re 100* la valeur envoy??e

                 Vdrecu= valeurfois100 / 1000f;
            }
        }


    // fonction de traitement de la trame re??ue
    private void messageHandler() {
        if (receiveBuffer !=null) {
            double currentTime;
            int l=0;
            String danslordre="";
            float Tension = -999.0000f; // on donne une valeur improbable ?? la tension pour v??rifier le bon d??roulement du traitement, l'ajout de f transforme un double en float
            Log.i(TAG,"j'arrive bien ici");

            if(expression=="vdd"){
            String receipt[] = new String[4]; //on r??cup??re les bytes et on les met dans le bon ordre (cas o?? on travaillait seulement sur data_t,
            receipt[0] = String.valueOf(receiveBuffer.charAt(3));
            receipt[1] = String.valueOf(receiveBuffer.charAt(4));
            receipt[2] = String.valueOf(receiveBuffer.charAt(0));
            receipt[3] = String.valueOf(receiveBuffer.charAt(1));
                Log.i(TAG,"message recoll?? en hexa dans le bon ordre:"+ Arrays.toString(receipt));
                for (l=0;l<receipt.length;l++)
                {
                    danslordre+=receipt[l]; // recollage du message dans le bon ordre
                }
                Integer valeurfois100 = Integer.parseInt(danslordre,16); // on recup??re 100* la valeur envoy??e
                //on multiplie par 100 la valeur lors de l'emission pour envoyer une valeur de type entier car on ne peut pas envoyer la donn??e en float
                Tension = valeurfois100; // on ram??ne ?? la bonne valeur
                Log.i(TAG,"j'arrive bien l??, tension vaut : "+Tension);


            }

            if(expression=="temperature") {
                String receipt[] = new String[8]; //on r??cup??re les bytes et on les mets dans le bon ordre: traitement pour 4 bytes (donn??e : millivolts). Attention ?? la position dans la trame!
                receipt[0] = String.valueOf(receiveBuffer.charAt(10));
                receipt[1] = String.valueOf(receiveBuffer.charAt(11));
                receipt[2] = String.valueOf(receiveBuffer.charAt(7));
                receipt[3] = String.valueOf(receiveBuffer.charAt(8));
                receipt[4] = String.valueOf(receiveBuffer.charAt(4));
                receipt[5] = String.valueOf(receiveBuffer.charAt(5));
                receipt[6] = String.valueOf(receiveBuffer.charAt(1));
                receipt[7] = String.valueOf(receiveBuffer.charAt(2));
                Log.i(TAG,"message recoll?? en hexa dans le bon ordre:"+ Arrays.toString(receipt));
                for (l=0;l<receipt.length;l++)
                {
                    danslordre+=receipt[l]; // recollage du message dans le bon ordre
                }
                Integer valeurfois100 = Integer.parseInt(danslordre,16); // on recup??re 100* la valeur envoy??e
                //on multiplie par 100 la valeur lors de l'emission pour envoyer une valeur de type entier car on ne peut pas envoyer la donn??e en float
                Tension = valeurfois100/256000f; // on ram??ne ?? la bonne valeur
                Log.i(TAG,"j'arrive bien l??, tension vaut : "+Tension);

            }

            if (DT==1){
                mUnite.setText("Temperature(??C)");
                if (VC==1){
                    double rt;
                    rt=(Rp*Tension/1000)/(Vdrecu-Tension/1000);
                    VDC.setText(String.valueOf(Vdrecu));
                    Log.i(TAG,"Rt: "+rt);
                    Tension= (float) ((rt-RT0)*1000000/(RT0*Sd));
                   Log.i(TAG,"tension: "+Tension);
                    String donneetraitee = String.valueOf(Tension); // transtypage car on va afficher la valeur dans un TextView
                    mVoltageField.setText(donneetraitee);
                }
                else{
                    double rt;
                    rt=(Rp*Tension/1000)/(Vd-Tension/1000);
                    Log.i(TAG,"Rt: "+rt);
                    Tension= (float) ((rt-RT0)*1000000/(RT0*Sd));
                    Log.i(TAG,"tension: "+Tension);
                    String donneetraitee = String.valueOf(Tension); // transtypage car on va afficher la valeur dans un TextView
                    mVoltageField.setText(donneetraitee);
                }

            }
            else {mUnite.setText("Voltage(mV)");
                String donneetraitee = String.valueOf(Tension); // transtypage car on va afficher la valeur dans un TextView
                mVoltageField.setText(donneetraitee);
            }

            if(Tension != -999.0) //on v??rifie qu'il n'y a pas d'erreur
            {
                if(startTime == 0.0) // boucle de gestion du temps
                {
                    startTime = Calendar.getInstance().getTimeInMillis();
                    currentTime = startTime;
                }else
                {
                    currentTime = Calendar.getInstance().getTimeInMillis();
                }

                double time = (currentTime - startTime) / 1000.0;

                valuesTension.add(new Entry((float)time, Tension)); // on ajoute la valeur sur le trac??
                // si on veut recup??rer la donn??e dans un fichier Log il faudra travailler ici
                updateCharts(); // on met ?? jour le graphe
        }
        }
        receiveBuffer=""; //on r??initialise le buffer pour recevoir la donn??e suivante
    }

    private void updateCharts() { //?? chaque mise ?? jour, on r??-initialise les trackers (m??thode API)
        chartTension.resetTracking();

        setData();
        // redraw
        chartTension.invalidate();
    }

    private void setData() {

        // create a dataset and give it a type
        LineDataSet set1 = new LineDataSet(valuesTension, "Tension");
        // on d??finit ci-dessous les caract??ristiques du trac??
        set1.setColor(Color.RED);
        set1.setLineWidth(1.00f);
        set1.setDrawValues(true);
        //set1.setDrawValues(false);
        set1.setDrawCircles(true);
       // set1.setDrawCircles(false);
        set1.setMode(LineDataSet.Mode.LINEAR);
        set1.setDrawFilled(false);

        // create a data object with the data sets
        LineData data = new LineData(set1);

        // set data
        chartTension.setData(data); // ici, on d??finit un nouvel objet pour le graphe (calcul ?? partir de la base de temps en abcisse et valeur en ordonn??e)

        // get the legend (only possible after setting data)
        Legend l = chartTension.getLegend();
        l.setEnabled(true);
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    /*private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }*/

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }



    public void onClickRead(View v) {//marche
        if (mBluetoothLeService != null) {
            mBluetoothLeService.readCustomCharacteristic();
            Log.i(TAG, "on arrive ?? lire l'information");// appelle la fonction readCustomCharacteristic() definie dans BluetoothLeService.java
        } else Log.i(TAG, "Je n'arrive pas ?? lire l'information");

    }

    public void onClickaffiche(View v) {
        oneread = 1;
    }

    public void onClickVDD(View v) {
            expression="vdd";
        mConnectionState.setText(" choisir option et/ou click \"affiche valeur\" ou \"run\"");


    }
    public void onClickT(View v) {

        mConnectionState.setText(" choisir option et/ou click \"affiche valeur\" ou \"run\"");
        expression="temperature";


    }


    public void onClickAF(View v) {
        if(mBluetoothLeService != null) {
           mBluetoothLeService.writeCustomCharacteristic(muuidField);
          //  rempuuid();
            // mais si vous voulez envoyer un message ?? la carte (autre que read) vous devrez vous servir de cette fonction

        }}
    public void onClickrun(View v) {
        String str1;
        if (aSwitchRun.isChecked()){
            str1 = aSwitchRun.getTextOn().toString();
            Log.i(TAG, "Switch "+str1);
            longread=1;
            handler.postDelayed(new Runnable() { // le handler est lanc?? p??riodiquement
                @Override
                public void run() {

                    handler.postDelayed(this, updatePeriod);
                    other=1;

                    //handler.removeCallbacks(this);
                }


            },updatePeriod);

        }
        else {
            str1 = aSwitchRun.getTextOff().toString();
            Log.i(TAG, "Switch " + str1);
            longread = 0;
        }
    }
        public void onClickDT(View v) {

            String str1;
        if (disT.isChecked()){
            str1 = disT.getTextOn().toString();
            Log.i(TAG, "display T "+str1);
            DT=1;


        }
        else
        { str1 = disT.getTextOff().toString();
            Log.i(TAG, "display "+str1);
            DT=0;
        }


    }
    public void onClickVC(View v) {
        String str1;
        if (Vcrt.isChecked()){
            str1 = Vcrt.getTextOn().toString();
            Log.i(TAG, "V corr "+str1);
            VC=1;


        }
        else
        { str1 = Vcrt.getTextOff().toString();
            mConnectionState.setText("change valeur ");
            Log.i(TAG, " Vcorr "+str1);
            VC=0;
        }

    }


       /* public void showAlertDialogButtonClicked() {
            // setup the alert builder
            AlertDialog.Builder builder = new AlertDialog.Builder(DeviceControlActivity.this);
            builder.setTitle("My title");
            builder.setMessage("This is my message.");
            // add a button
            builder.setPositiveButton("OK", null);
            // create and show the alert dialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }*/


}
