package com.example.contigoinventory;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.text.AllCapsTransformationMethod;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;


import org.bson.Document;


import io.realm.Realm;
import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;
import io.realm.mongodb.mongo.MongoCollection.*;
import io.realm.mongodb.mongo.iterable.FindIterable;
import io.realm.mongodb.mongo.iterable.MongoCursor;
import io.realm.mongodb.mongo.options.FindOptions;

import android.os.Build;


public class MainActivity extends AppCompatActivity {
    private SurfaceView surfaceView;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private static final int REQUEST_CAMERA_PERMISSION = 201;
    private ToneGenerator toneGen1;
    private TextView barcodeText;
    private TextView barcodeCount;
    private String barcodeData;
    private Button addStock;
    private Button sell;
    private Button clearBarcodes;
    private Button stockAudit;
    private Button manualAdd;
    private Button saleReverse;
    ArrayList<String> barcode_list = new ArrayList<String>();
    List<Document> barcode_docs = new ArrayList<>();
    private App realmAppCon;
    String RealmAppId = "contigorealm-tsygt";
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        /*
        Realm.init(this);
        realmAppCon =  new App(new AppConfiguration.Builder(RealmAppId).build());
        User user = realmAppCon.currentUser();
        //Credentials credentials = Credentials.emailPassword("contigojeans@gmail.com","contigo123!@#");
        realmAppCon.loginAsync(Credentials.anonymous(), new App.Callback<User>() {
            @Override
            public void onResult(App.Result<User> result) {
                if(result.isSuccess())
                {
                    Toast.makeText(getApplicationContext(),"Database Connection Successful!",Toast.LENGTH_LONG).show();
                    Log.v("MongoDB_CON","Logged In Successfully");
                }
                else
                {
                    Toast.makeText(getApplicationContext(),"Database Connection Failed",Toast.LENGTH_LONG).show();
                    System.out.println(result.getError());
                    Log.v("MongoDB_CON","Failed to Login");
                }
            }
        });
         */
        DatabaseConnection dbcon = new DatabaseConnection();
        //User user = dbcon.getDatabaseConnection(this);
        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
        surfaceView = findViewById(R.id.surface_view);
        barcodeText = findViewById(R.id.barcode_text);
        barcodeCount = findViewById(R.id.barcode_count);
        addStock = (Button) findViewById(R.id.add_stock);
        addStock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View AddStock) {
                Log.i("onclick Add Stock","Clicking the Add Stock button");
                AlertDialog.Builder builder= new AlertDialog.Builder(MainActivity.this);
                User user = dbcon.getDatabaseConnection(getApplicationContext());
                builder.setCancelable(true);
                builder.setTitle("Adding Stock");
                builder.setMessage("Add "+barcodeCount.getText().toString()+" pcs into Stock?");
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(getApplicationContext(),"Adding Stock...",Toast.LENGTH_SHORT).show();
                        MongoClient mongoClient = user.getMongoClient("mongodb-atlas");
                        MongoDatabase database = mongoClient.getDatabase("contigojeans");
                        MongoCollection<Document> collection = database.getCollection("stockinward");
                        barcode_docs.forEach(barcode -> barcode.append("timestamp",simpleDateFormat.format(new Date())).append("device_name",Build.PRODUCT));
                        collection.insertMany(barcode_docs).getAsync(result -> {
                            if(result.isSuccess()){
                                Log.v("Add_stock_status","Stock Added Successfully");
                                Toast.makeText(getApplicationContext(),"Success: Stock Added!",Toast.LENGTH_SHORT).show();
                                barcodeText.setText("Scan Barcode");
                                sell.setEnabled(false);
                                addStock.setEnabled(false);
                                clearBarcodes.setEnabled(false);
                                stockAudit.setEnabled(false);
                                saleReverse.setEnabled(false);
                                barcode_list.clear();
                                barcode_docs.clear();
                                barcodeCount.setText("0");
                            }
                            else {
                                Toast.makeText(getApplicationContext(),"Add Stock Failed: Try Again!",Toast.LENGTH_SHORT).show();
                                Log.v("Add_stock_status",result.getError().toString());}
                        });
                    }
                });
                builder.show();

            }
        });
        sell = (Button) findViewById(R.id.sell);
        sell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View sellStock) {
                Log.i("onclick sell", "Clicking the sell button");
                Toast.makeText(getApplicationContext(), "Selling Stock", Toast.LENGTH_SHORT).show();
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setCancelable(true);
                builder.setTitle("Selling stock");
                final EditText buyerNameTextBox= new EditText(MainActivity.this);
                buyerNameTextBox.setHint("Please Enter Buyer Name");
                builder.setMessage("Sell " + barcodeCount.getText().toString() + " pcs?");
                builder.setView(buyerNameTextBox);
                User user = dbcon.getDatabaseConnection(MainActivity.this);
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MongoClient mongoClient = user.getMongoClient("mongodb-atlas");
                        MongoDatabase database = mongoClient.getDatabase("contigojeans");
                        MongoCollection<Document> collection = database.getCollection("stockoutward");
                        String buyerName=buyerNameTextBox.getText().toString();
                        barcode_docs.forEach(barcode -> barcode.append("timestamp",simpleDateFormat.format(new Date())).append("buyer_name",buyerNameTextBox.getText().toString()).append("device_name",Build.PRODUCT));
                        collection.insertMany(barcode_docs).getAsync(result -> {
                            if (result.isSuccess()) {
                                Log.v("Sell_stock_status", "Stock Sold Successfully");
                                Toast.makeText(getApplicationContext(), "Success: Stock Sold to "+buyerName, Toast.LENGTH_SHORT).show();
                                barcodeText.setText("Scan Barcode");
                                sell.setEnabled(false);
                                addStock.setEnabled(false);
                                clearBarcodes.setEnabled(false);
                                stockAudit.setEnabled(false);
                                saleReverse.setEnabled(false);
                                barcode_list.clear();
                                barcode_docs.clear();
                                barcodeCount.setText("0");
                            } else {
                                Toast.makeText(getApplicationContext(), "Sell Stock Failed: Try Again!", Toast.LENGTH_SHORT).show();
                                Log.v("Sell_stock_status", result.getError().toString());
                            }
                        });

                    }
                });
                final AlertDialog dialog = builder.create();
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                buyerNameTextBox.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }
                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if(charSequence.length()!=0){
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }else {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        }
                    }
                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                //builder.show();
            }
        });

        //pop up or alert message before clearing barcodes
        clearBarcodes = (Button) findViewById(R.id.clear_barcodes);
        clearBarcodes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clearBarcodes) {
                Log.i("oncclick clear","Clicking the clear barcodes button");
                AlertDialog.Builder builder= new AlertDialog.Builder(MainActivity.this);
                builder.setCancelable(true);
                builder.setTitle("Clearing All Scanned Barcodes");
                builder.setMessage("Are you sure you want to clear "+barcodeCount.getText().toString()+" barcodes?");
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        barcodeText.setText("Scan Barcode");
                        sell.setEnabled(false);
                        addStock.setEnabled(false);
                        clearBarcodes.setEnabled(false);
                        stockAudit.setEnabled(false);
                        saleReverse.setEnabled(false);
                        barcode_list.clear();
                        barcode_docs.clear();
                        barcodeCount.setText("0");
                        Toast.makeText(getApplicationContext(),"Cleared Barcodes",Toast.LENGTH_SHORT).show();
                    }
                });
                builder.show();
            }
        });
        stockAudit = (Button) findViewById(R.id.stock_audit);
        stockAudit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View StockAudit) {
                Log.i("onclick Stock Audit","Clicking the Stock Audit button");
                AlertDialog.Builder builder= new AlertDialog.Builder(MainActivity.this);
                User user = dbcon.getDatabaseConnection(MainActivity.this);
                builder.setCancelable(true);
                builder.setTitle("Auditing Stock");
                builder.setMessage("Add "+barcodeCount.getText().toString()+" pcs into Audit?");
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int year = Calendar.getInstance().get(Calendar.YEAR);
                        int month = Calendar.getInstance().get(Calendar.MONTH);
                        //String collectionName = "stockaudit_"+year+"_"+month;
                        Toast.makeText(getApplicationContext(),"Adding Stock...",Toast.LENGTH_SHORT).show();
                        MongoClient mongoClient = user.getMongoClient("mongodb-atlas");
                        MongoDatabase database = mongoClient.getDatabase("contigojeans");
                        MongoCollection<Document> collection = database.getCollection("stockaudit");
                        barcode_docs.forEach(barcode -> barcode.append("timestamp",simpleDateFormat.format(new Date())).append("device_name",Build.PRODUCT));
                        collection.insertMany(barcode_docs).getAsync(result -> {
                            if(result.isSuccess()){
                                Log.v("stock_audit_status","Stock Added Successfully");
                                Toast.makeText(getApplicationContext(),"Success: Added stock to Audit!",Toast.LENGTH_SHORT).show();
                                barcodeText.setText("Scan Barcode");
                                sell.setEnabled(false);
                                addStock.setEnabled(false);
                                clearBarcodes.setEnabled(false);
                                stockAudit.setEnabled(false);
                                saleReverse.setEnabled(false);
                                barcode_list.clear();
                                barcode_docs.clear();
                                barcodeCount.setText("0");
                            }
                            else {
                                Toast.makeText(getApplicationContext(),"Stock Audit Failed: Try Again!",Toast.LENGTH_SHORT).show();
                                Log.v("stock_audit_status",result.getError().toString());}
                        });
                    }
                });
                builder.show();
            }
        });
        manualAdd = (Button) findViewById(R.id.manual_add_stock);
        manualAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder= new AlertDialog.Builder(MainActivity.this);
                builder.setCancelable(true);
                builder.setTitle("ADD STOCK MANUALLY");
                Context context = view.getContext();
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);
                final EditText barcodeSeries = new EditText(context);
                barcodeSeries.setHint("Barcode Series ex: CJ01MG");
                barcodeSeries.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
                barcodeSeries.setFilters(new InputFilter[] {new InputFilter.LengthFilter(6)});
                layout.addView(barcodeSeries);
                final EditText start = new EditText(context);
                start.setHint("LOT START NO eg: 1");
                start.setInputType(InputType.TYPE_CLASS_NUMBER);
                start.setFilters(new InputFilter[] {new InputFilter.LengthFilter(4)});
                layout.addView(start);
                final EditText end = new EditText(context);
                end.setHint("LOT END NO eg: 100");
                end.setInputType(InputType.TYPE_CLASS_NUMBER);
                end.setFilters(new InputFilter[] {new InputFilter.LengthFilter(4)});
                layout.addView(end);
                final EditText psswd = new EditText(context);
                psswd.setHint("PASSWORD");
                psswd.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD );
                psswd.setTransformationMethod(new PasswordTransformationMethod());
                layout.addView(psswd);
                builder.setView(layout);
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                builder.setPositiveButton("ADD", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        List<Document> tempBarcodeDocs = new ArrayList<>();
                        String barcodeSerialNo= barcodeSeries.getText().toString().toUpperCase(Locale.ROOT);
                        int lotStartNo = Integer.parseInt(start.getText().toString());
                        int lotEndNo = Integer.parseInt(end.getText().toString());
                        for (int j =lotStartNo; j<=lotEndNo;j++){
                            String formattedLotNo=String.format("%04d", j);
                            String barcodeBuilder=barcodeSerialNo+formattedLotNo.substring(formattedLotNo.length()-4);
                            tempBarcodeDocs.add(new Document("barcode_id",barcodeBuilder).append("timestamp",simpleDateFormat.format(new Date())).append("device_name",Build.PRODUCT));
                        }
                        User user = dbcon.getDatabaseConnection(MainActivity.this);
                        MongoClient mongoClient = user.getMongoClient("mongodb-atlas");
                        MongoDatabase database = mongoClient.getDatabase("contigojeans");
                        MongoCollection<Document> collection = database.getCollection("stockinward");
                        collection.insertMany(tempBarcodeDocs).getAsync(result -> {
                            if(result.isSuccess()){
                                Log.v("m_add_stock_status","Stock Added Successfully");
                                Toast.makeText(getApplicationContext(),"Success: Stock Added to Inward!",Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(getApplicationContext(),"Adding Stock Failed: Try Again!",Toast.LENGTH_SHORT).show();
                                Log.v("m_add_stock_status",result.getError().toString());}
                        });
                    }
                });
                final AlertDialog dialog = builder.create();
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                psswd.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if(barcodeSeries.getText().toString().length()==6 && psswd.getText().toString().equals("mt3861") && (Integer.parseInt(start.getText().toString()) < Integer.parseInt(end.getText().toString()))){
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }
                        else{
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        }

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
                barcodeSeries.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if(barcodeSeries.getText().toString().length()==6 && psswd.getText().toString().equals("mt3861") && (Integer.parseInt(start.getText().toString()) < Integer.parseInt(end.getText().toString()))){
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        }
                        else{
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        }

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
                //builder.show();
            }

        });
        saleReverse = (Button) findViewById(R.id.sale_reverse);
        saleReverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View StockAudit) {
                Log.i("onclick sale reverse","Clicking Sale reverse button");
                AlertDialog.Builder builder= new AlertDialog.Builder(MainActivity.this);
                User user = dbcon.getDatabaseConnection(MainActivity.this);
                builder.setCancelable(true);
                builder.setTitle("Reversing Sale");
                builder.setMessage("Reverse "+barcodeCount.getText().toString()+" pcs from sale?");
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(getApplicationContext(),"Reversing Sale...",Toast.LENGTH_SHORT).show();
                        MongoClient mongoClient = user.getMongoClient("mongodb-atlas");
                        MongoDatabase database = mongoClient.getDatabase("contigojeans");
                        MongoCollection<Document> stockOutwardCollection = database.getCollection("stockoutward");
                        MongoCollection<Document> saleReverseCollection = database.getCollection("salereverse");
                        List<Document> tmpBarcodeDocs =barcode_docs;
                        for (int j = 0; j < barcode_docs.size(); j++) {
                            RealmResultTask<MongoCursor<Document>> cursor = stockOutwardCollection.find(tmpBarcodeDocs.get(j)).iterator();
                            int finalJ = j;
                            cursor.getAsync(task -> {
                                if(task.isSuccess()){
                                    //System.out.println("found record"+tmpBarcodeDocs.get(finalJ).toString());
                                    MongoCursor<Document> results = task.get();
                                    while (results.hasNext()){
                                        Document doc = results.next();
                                        System.out.println(doc.toString());
                                        stockOutwardCollection.deleteOne(doc).getAsync(del ->{

                                        });
                                        saleReverseCollection.insertOne(doc).getAsync(ins ->{

                                        });
                                        System.out.println("record inserted to sale revers "+doc.toString());
                                    }
                                }
                                else {
                                    System.out.println("Record not found in Sale");
                                }
                            });
                        }
                        barcodeText.setText("Scan Barcode");
                        sell.setEnabled(false);
                        addStock.setEnabled(false);
                        clearBarcodes.setEnabled(false);
                        stockAudit.setEnabled(false);
                        saleReverse.setEnabled(false);
                        barcode_list.clear();
                        barcode_docs.clear();
                        barcodeCount.setText("0");

                    }
                });
                builder.show();
            }
        });


        initialiseDetectorsAndSources();
    }

    private void initialiseDetectorsAndSources() {

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.CODE_128)
                .build();

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true) //you should add this feature
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraSource.start(surfaceView.getHolder());
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this, new
                                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });


        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                // Toast.makeText(getApplicationContext(), "To prevent memory leaks barcode scanner has been stopped", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {


                    barcodeText.post(new Runnable() {

                        @Override
                        public void run() {

                            barcodeText.removeCallbacks(null);
                            barcodeData = barcodes.valueAt(0).displayValue;
                            if(!barcode_list.contains(barcodeData) && (barcodeData.startsWith("CJ") || barcodeData.startsWith("CT") || barcodeData.startsWith("CS") || barcodeData.startsWith("CH") || barcodeData.startsWith("CA")) && barcodeData.matches(".*?\\d{4}$") && (barcodeData.length()==8 || barcodeData.length()==10) ){
                                barcode_list.add(barcodeData);
                                Document doc = new Document("barcode_id",barcodeData);
                                barcode_docs.add(new Document("barcode_id",barcodeData));
                                System.out.println(barcodes);
                                System.out.println("printingdisplayvalue"+barcodes.valueAt(0).displayValue);
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                                String input= barcodeText.getText().toString();
                                String output="";
                                if(input.contains("Scan Barcode")){
                                    //enabling buttons after first scan of the barcodes
                                    output = barcodeData;
                                    addStock.setEnabled(true);
                                    sell.setEnabled(true);
                                    clearBarcodes.setEnabled(true);
                                    stockAudit.setEnabled(true);
                                    saleReverse.setEnabled(true);
                                    barcodeText.setGravity(Gravity.BOTTOM);
                                }
                                else{
                                    output = input+" "+barcodeData;
                                }
                                System.out.println(output);
                                System.out.println(barcode_list);
                                barcodeText.setText(output);
                                barcodeText.setMovementMethod(ScrollingMovementMethod.getInstance());
                                Integer brcdecnt = barcode_list.size();
                                barcodeCount.setText(brcdecnt.toString());
                            }
                        }
                    });

                }
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        getSupportActionBar().hide();
        cameraSource.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportActionBar().hide();
        initialiseDetectorsAndSources();
    }

}