package com.yacikgoz.concentration;

import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private int colNumber = 4;
    private int rowNumber = 4;
    private Cell[][] buttons;
    private static final String TAG = "Thread";
    private TableRow tableRow;
    private TableLayout table;
    private JSONArray jsonArray;
    private JSONObject json;
    private ArrayList<String> urlList;
    private ArrayList<Bitmap> bitmapList;
    private ArrayList<Integer> threadOrder;
    private Cell lastClicked = null;
    private TextView scoreText, mistakeText, chanceText;
    private int score =0, mistake = 0, size, toNexLevel, downloadCount=0;
    private Chronometer chronometer;
    private boolean jsonFlag = false, downloadFlag = false, firstClick = true;
    private final Object mutex = new Object();
    public int getSize() {
        return size;
    }
    public void setSize(int size) {
        this.size = size;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttons = new Cell[rowNumber][colNumber];
        table = (TableLayout) findViewById(R.id.table);
        scoreText = (TextView) findViewById(R.id.score);
        mistakeText = (TextView) findViewById(R.id.mistake);
        setSize(colNumber * rowNumber);
        toNexLevel = 2*getSize();
        buttons = new Cell[rowNumber][colNumber];
        urlList = new ArrayList<>(getSize());
        bitmapList = new ArrayList<>(getSize());
        threadOrder = new ArrayList<>(getSize());
        chanceText = (TextView)findViewById(R.id.chance);
        chronometer = (Chronometer) findViewById(R.id.chronometer);
    }
    @Override
    protected void onStart() {
        super.onStart();
        generateButtons();
        try {
            downloadAndParseJSON();
            downloadImages();
            setImages2Buttons();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * JSON dosyasini indiren thread fonksiyonu
     * Parse edilen URL'leri url listesine ekler
     * @throws InterruptedException exception
     */
    private void downloadAndParseJSON() throws InterruptedException {

        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                changeButtonClickable(false);
                String url;
                String jsonURL = "https://pixabay.com/api/?key=5516663-dc2322454a65418c44907a054&q=dog&image_type=photo&pretty=true&per_page=" + getSize() / 2;
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "JSON DOWNLOADING...", Toast.LENGTH_SHORT).show();
                        }
                    });
                    json = readJsonFromUrl(jsonURL);
                    jsonArray = json.getJSONArray("hits");

                    for (int i = 0; i < getSize() / 2; ++i) {
                        JSONObject record;
                        record = jsonArray.getJSONObject(i);
                        url = record.getString("previewURL");
                        urlList.add(i,url);
                    }
                    for(int i = getSize() / 2; i < getSize(); ++i) {
                        JSONObject record;
                        record = jsonArray.getJSONObject(i-(getSize() / 2));
                        url = record.getString("previewURL");
                        urlList.add(i,url);
                    }
                    jsonFlag = true;
                    long stopTime = System.currentTimeMillis();
                    long elapsedTime = stopTime - startTime;
                    Log.i(TAG, "JSON download and parse duration " + elapsedTime + " ms.");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toNexLevel = 2*getSize();
                            String prompt = "You Have "+ toNexLevel + " Chance to Go Next Level!";
                            chanceText.setText(prompt);
                            String s = "" + mistake;
                            mistakeText.setText(s);
                            s = "" + score;
                            scoreText.setText(s);
                        }
                    });
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /**
     * Resimleri indiren fonksiyon
     * Her resim icin ayri thread acarak resimlerin tutuldugu
     * listenin sonuna random sekilde ekleme yapar
     */
    private void downloadImages(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    if(jsonFlag) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "\tJSON DOWNLOADED\nIMAGES DOWNLOADING...", Toast.LENGTH_SHORT).show();
                            }
                        });
                        for ( int i = 0; i < getSize(); ++i) {
                            final int index = i;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    long startTime;
                                    synchronized (mutex) {
                                        startTime = System.currentTimeMillis();
                                        bitmapList.add(getBitmapFromURL(urlList.get(index)));
                                        threadOrder.add(index);
                                    }
                                    long stopTime = System.currentTimeMillis();
                                    long elapsedTime = stopTime - startTime;
                                    Log.i(TAG, "Image [" + index + "] downloaded. Duration is: " + elapsedTime + " ms.");
                                    ++downloadCount;
                                    if (downloadCount == getSize()) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(getApplicationContext(), "IMAGES DOWNLOADED", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                        downloadFlag = true;
                                        downloadCount = 0;
                                    }
                                }
                            }).start();
                        }
                        break;
                    }
                }
            }
        }).start();
    }
    /**
     * Indirilen resimleri butonlarin altina yerleştiren thread.
     */
    private void setImages2Buttons(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if(downloadFlag){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ViewGroup.LayoutParams params = null;
                                for (int i = 0; i < rowNumber; ++i) {
                                    for (int j = 0; j < colNumber; ++j) {
                                        if(i==0){
                                            params = buttons[i][j].getLayoutParams();
                                            int buttonSize = buttons[i][j].getWidth();
                                            params.width = buttonSize;
                                            params.height = buttonSize;
                                        }
                                        buttons[i][j].setId(i * colNumber + j);
                                        buttons[i][j].setCell(false, bitmapList.get(i*colNumber+j));
                                        buttons[i][j].setOnClickListener(MainActivity.this);
                                        buttons[i][j].setLayoutParams(params);
                                        changeButtonBackground(i, j, false);
                                    }
                                }
                            }
                        });
                        for(int i=0; i<getSize(); ++i) {
                            int num = threadOrder.get(i);
                            buttons[i/colNumber][i%colNumber].setUrl(urlList.get(num));
                        }
                        changeButtonClickable(true);
                        break;
                    }
                }
            }
        }).start();
    }

    /**
     * Indirme islemi sirasinda butonlarin clickable durumunu
     * degistiren fonksiyon
     * @param status click status
     */
    public void changeButtonClickable(boolean status){
        for (int i=0; i<rowNumber; ++i){
            for (int j =0; j<colNumber; ++j){
                buttons[i][j].setClickable(status);
            }
        }
    }
    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    /**
     * Verilen URL'den JSON objesi indiren fonksiyon
     * @param url URL
     * @return Json objeck
     * @throws IOException IOException
     * @throws JSONException JSONException
     */
    public JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    /**
     * URL'den bitmap indiren fonksiyon
     * @param src url
     * @return indirilen bitmap objesi
     */
    public Bitmap getBitmapFromURL(String src) {
            try {

                java.net.URL url = new java.net.URL(src);
                HttpURLConnection connection = (HttpURLConnection) url
                        .openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);

                return myBitmap;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
    }

    /**
     * Butonları olusturan fonksiyon
     */
    private void generateButtons() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                table.removeAllViews();
                for (int i = 0; i < rowNumber; ++i) {
                    tableRow = new TableRow(MainActivity.this);
                    tableRow.setLayoutParams(new TableLayout.LayoutParams(
                            TableLayout.LayoutParams.MATCH_PARENT,
                            TableLayout.LayoutParams.MATCH_PARENT,
                            1.0f
                    ));
                    table.addView(tableRow);
                    for(int j = 0; j < colNumber; ++j){
                        buttons[i][j] = new Cell(MainActivity.this);
                        buttons[i][j].setLayoutParams(new TableRow.LayoutParams(
                                TableRow.LayoutParams.MATCH_PARENT,
                                TableRow.LayoutParams.MATCH_PARENT,
                                1.0f
                        ));
                        tableRow.addView(buttons[i][j]);
                        buttons[i][j].setBackgroundResource(R.drawable.loading);
                        buttons[i][j].status = false;
                    }
                }
            }
        });
    }

    /**
     * Hamle sirasinda butonlarin icerigini degistiren fonksiyon
     * @param row button row
     * @param col button col
     * @param mode mode
     */
    private void changeButtonBackground(final int row, final int col, final boolean mode){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mode)
                    buttons[row][col].setBackground(new BitmapDrawable(getResources(), buttons[row][col].getBitmap()));

                else{
                    buttons[row][col].setBackgroundResource(android.R.drawable.btn_default);
                }
            }
        });
    }

    /**
     * on click listener
     * @param v tiklanan buton objesi
     */
    @Override
    public void onClick(View v) {
    final int row = v.getId()/rowNumber;
    final int col = v.getId()%rowNumber;
    final Handler handler = new Handler();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                 //GAME ALGORITHM

                if(!buttons[row][col].status && lastClicked == null){
                    if(firstClick){
                        chronometer.setBase(SystemClock.elapsedRealtime());
                        chronometer.start();
                        firstClick = false;
                    }
                    flipView(buttons[row][col], 100);
                    lastClicked = buttons[row][col];
                    lastClicked.status=true;
                    changeButtonBackground(row,col, buttons[row][col].status);
                }
                else if(lastClicked != null  && buttons[row][col]!=null && !buttons[row][col].status && lastClicked.status){
                    // resimler ayniysa
                    if(lastClicked.url.equals(buttons[row][col].url)){
                        flipView(buttons[row][col], 100);
                        buttons[row][col].status = true;
                        buttons[lastClicked.getId()/rowNumber][lastClicked.getId()%rowNumber].status = true;
                        changeButtonBackground(row,col, buttons[row][col].status);
                        ++score;
                        String s = "" + score;
                        scoreText.setText(s);
                        lastClicked = null;
                        if(score == getSize()/2){
                            colNumber +=2;
                            rowNumber +=2;
                            restartGame();
                        }
                    }
                    // resimler farkliysa
                    else{
                        ++mistake;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String s = "" + mistake;
                                mistakeText.setText(s);
                            }
                        });
                        lastClicked.status=false;
                        changeButtonBackground(row,col, true);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                changeButtonBackground(row,col, false);
                            }
                        },200);
                        changeButtonBackground(lastClicked.getId()/rowNumber, lastClicked.getId()%rowNumber, false);
                        lastClicked = null;
                        if(mistake == toNexLevel){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String s = "You are fail!";
                                    Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
                                    restartGame();
                                }
                            });
                        }
                    }
                }
            }
        });
    }

    /**
     * Restart game fonksiyonu
     */
    private void restartGame(){
        setSize(colNumber * rowNumber);
        buttons = new Cell[rowNumber][colNumber];
        generateButtons();
        urlList = new ArrayList<>(getSize());
        bitmapList = new ArrayList<>(getSize());
        threadOrder = new ArrayList<>(getSize());
        downloadFlag=false;
        jsonFlag=false;
        firstClick = true;
        score = 0;
        mistake = 0;
        chronometer.setBase(SystemClock.elapsedRealtime());
        try {
            downloadAndParseJSON();
            downloadImages();
            setImages2Buttons();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        restartGame();
    }

    private void flipView(final View viewToFlip, int duration) {
        ObjectAnimator flip = ObjectAnimator.ofFloat(viewToFlip, "rotationY", 0f, 360f);
        flip.setDuration(duration);
        flip.start();

    }
}
