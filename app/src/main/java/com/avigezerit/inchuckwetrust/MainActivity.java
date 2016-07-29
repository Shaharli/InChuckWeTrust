package com.avigezerit.inchuckwetrust;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    private static final String REQUEST_JOKE = "http://api.icndb.com/jokes/random/";

    //permission
    private static final int REQUEST_PERMISSION = 101;
    private boolean permission_granted = false;
    String[] myPermissions;

    TextView quoteTV;
    TextView creditTV;
    Button getRandBtn;
    getRandomJoke getRandomJoke;
    String randJoke = null;

    View viewToSave;
    View viewClickedShareMethod;
    Uri path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        quoteTV = (TextView) findViewById(R.id.quoteTV);
        requestRandJoke();

        creditTV = (TextView) findViewById(R.id.creditTV);

        getRandBtn = (Button) findViewById(R.id.getRandBtn);

        //xml ref
        ImageButton shareBtn = (ImageButton) findViewById(R.id.sBtnShare);
        ImageButton sBtnWhap = (ImageButton) findViewById(R.id.sBtnWhap);
        ImageButton sBtnFace = (ImageButton) findViewById(R.id.sBtnFace);

        getRandBtn.setOnClickListener(this);
        shareBtn.setOnClickListener(this);
        sBtnFace.setOnClickListener(this);
        sBtnWhap.setOnClickListener(this);

        viewToSave = findViewById(R.id.viewToSave);
    }

    public static Bitmap getBitmapFromView(View view) {

        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background
        Drawable bgDrawable = view.getResources().getDrawable(R.drawable.bg_main);
        view.setBackground(bgDrawable);
        // draw the view on the canvas
        view.draw(canvas);
        //return the bitmap
        return returnedBitmap;
    }

    private void SaveImageToStorage(Bitmap finalBitmap) {

        //create and get directory
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        File myDir = new File(root + "/ChuckJoke/images");
        if (!myDir.exists()) {
            if (myDir.mkdirs()) {
                Log.e(TAG, "Directory created");
            }
        }

        //set name of file
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fname = "Chuck_Joke_" + "_" + timeStamp + ".jpg";

        //new file based on name and directory
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();

        //output saving file
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            Toast.makeText(this, "Image saved!", Toast.LENGTH_SHORT).show();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        path = Uri.parse("file://" + file.getAbsolutePath());

    }


    private void requestRandJoke() {

        quoteTV.setText("Chucking...");

        String jokeSB = null;

        getRandomJoke = new getRandomJoke();
        getRandomJoke.execute(REQUEST_JOKE);

        try {
            jokeSB = getRandomJoke.get();
        } catch (InterruptedException e) {
            Log.d(TAG, e.getMessage());
        } catch (ExecutionException e) {
            Log.d(TAG, e.getMessage());
        }

        parseJSON(jokeSB);
    }

    private void parseJSON(String jokeSB) {

        if (jokeSB == null) {
            quoteTV.setText("There seems to be a problem, Try again");
        }

        try {
            JSONObject allData = new JSONObject(jokeSB);

            JSONObject jokeValue = allData.getJSONObject("value");

            //check for quotation marks and replace
            randJoke = jokeValue.getString("joke");

            if (randJoke.contains("&quot;")) {
                randJoke = randJoke.replace("&quot;", "\"");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        quoteTV.setText(randJoke);
    }

    @Override
    public void onClick(View v) {

        viewClickedShareMethod = v;

        if (v.getId() == R.id.getRandBtn) {
            requestRandJoke();
        } else {
            checkForPermissions();
            if (permission_granted) {
                shareJokeImage();
            }
        }
    }

    private void shareJokeImage() {

        creditTV.setText("Created by 'In Chuck We Trust' App, Now on Google play!");
        SaveImageToStorage(getBitmapFromView(viewToSave));

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/*");

        switch (viewClickedShareMethod.getId()) {
            case R.id.sBtnShare:
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Chuck Joke");
                intent.putExtra(Intent.EXTRA_TEXT, "Have you heard this one before?\nCheck out 'In Chuck We Trust' App!");
                intent.putExtra(Intent.EXTRA_STREAM, path);
                startActivity(Intent.createChooser(intent, "Share Chuck Joke"));
                break;
            case R.id.sBtnWhap:
                intent.setPackage("com.whatsapp");
                intent.putExtra(Intent.EXTRA_TEXT, "Have you heard this one before?\nCheck out 'In Chuck We Trust' App!");
                intent.putExtra(Intent.EXTRA_STREAM, path);
                startActivity(Intent.createChooser(intent, "Share Chuck Joke"));
                break;
            case R.id.sBtnFace:
                intent.setPackage("com.facebook.katana");
                intent.putExtra(Intent.EXTRA_STREAM, path);
                startActivity(Intent.createChooser(intent, "Share Chuck Joke"));
                break;
        }

        clearBgCredit();
    }


    private void checkForPermissions() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            //permission Dialog
            myPermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            requestPermissions(myPermissions, REQUEST_PERMISSION);

            if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showMessageOKCancel("Please allow access to storage so you can share the Chuck Joke.",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkForPermissions();
                            }
                        });
                return;
            }


        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            permission_granted = true;
            shareJokeImage();

        } else {
            permission_granted = false;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void clearBgCredit() {
        creditTV.setText("");
        viewToSave.setBackground(null);
    }

    private class getRandomJoke extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {


            //establishing reading process from http request
            BufferedReader quoteReader;
            HttpURLConnection connectingToApi = null;

            //building a string of result based on the url data
            StringBuilder jokeSB = new StringBuilder();

            try {
                //creating a url and open an connection
                URL url = new URL(params[0]);
                connectingToApi = (HttpURLConnection) url.openConnection();

                //making sure the connection is ok
                if (connectingToApi.getResponseCode() != HttpURLConnection.HTTP_OK || connectingToApi == null) {
                    //no connection
                    Toast.makeText(MainActivity.this, "No connection, Try again", Toast.LENGTH_SHORT).show();
                }
                //setting a input stream reader to read the data stream as chars
                InputStreamReader searchResultStream = new InputStreamReader(connectingToApi.getInputStream());
                quoteReader = new BufferedReader(searchResultStream);

                //going over the input, line by line, using a temp string
                String line;

                //loop as long as the input has another line to read
                while ((line = quoteReader.readLine()) != null) {

                    //append the line to the string builder, new line
                    jokeSB.append(line + '\n');
                }


            }
            //catch exceptions
            catch (MalformedURLException e) {
                Log.d(TAG, e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, e.getMessage());
            } finally {
                //close connection
                connectingToApi.disconnect();
            }
            //passing the string result to on post execute func
            return jokeSB.toString();
        }

    }


}
