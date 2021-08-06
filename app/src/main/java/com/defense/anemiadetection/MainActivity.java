package com.defense.anemiadetection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private Button select,capture,predict;
    private ImageView image;
    private TextView tv;
    private Bitmap img;
    private RadioButton male,female;
    private ProgressDialog progressDialog;
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        select = findViewById(R.id.select);
        capture = findViewById(R.id.capture);
        predict = findViewById(R.id.predict);
        image = findViewById(R.id.iv);
        tv = findViewById(R.id.tv);
        male = findViewById(R.id.male);
        female = findViewById(R.id.female);

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv.setText("Prediction Result:");
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED )
                {
                    selectFile();
                }
                else {
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},10);
                }
            }
        });

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv.setText("Prediction Result:");
                if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                {
                    capture();
                }
                else {
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},20);
                }
            }
        });

        predict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    Bitmap croppedBitmap = Bitmap.createScaledBitmap(img,864,1152,true);

                    int r = 0;
                    int g = 0;
                    int b = 0;

                    int total = 0;

                    int width = croppedBitmap.getWidth();
                    int height = croppedBitmap.getHeight();
                    for (int x = 0; x <width; x++)
                    {
                        for (int y = 0; y < height; y++)
                        {
                            int c = croppedBitmap.getPixel(x,y);
                            total++;
                            r += Color.red(c);
                            g += Color.green(c);
                            b += Color.blue(c);

                        }
                    }

                    int avgRed = (r/total);
                    int avgGreen = (g/total);
                    int avgBlue = (b/total);

                    int gender ;

                    if(male.isChecked()) gender = 0;
                    else gender = 1;




                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("Please wait..."); // Setting Message
                progressDialog.setTitle("Validating"); // Setting Title
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
                progressDialog.show(); // Display Progress Dialog
                progressDialog.setCancelable(false);

                if(isNetworkConnected())
                {
                    queue = Volley.newRequestQueue(MainActivity.this);
                    String url = "https://anemia-detection-web-app.herokuapp.com/get-hemoglobin-result?r="+avgRed+"&g="+avgGreen+"&b="+avgBlue+"&gender="+gender;
                    System.out.println(url);


                    JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                            (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                                @Override
                                public void onResponse(JSONObject response) {
                                    progressDialog.dismiss();
                                    Gson gson = new Gson();
                                    Hb hb = gson.fromJson(response.toString(),Hb.class);
//                                    System.out.println(hb.getHb());
//                                    Log.d("D", "Himoglobin: "+hb.getHb());
                                    tv.setText("Hemoglobin Level: "+hb.getHb());
                                }
                            }, new Response.ErrorListener() {

                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    progressDialog.dismiss();
                                    // TODO: Handle error

                                }
                            });


                    queue.add(jsonObjectRequest);
                }
                else {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this,"No Internet",Toast.LENGTH_SHORT).show();
                }





            }
        });
    }

    private void capture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent,200);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(MainActivity.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if( requestCode == 10 && grantResults[0]==PackageManager.PERMISSION_GRANTED )
        {
            selectFile();
        }
        else if(requestCode == 20 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            capture();
        }
        else {
            Toast.makeText(MainActivity.this, "Please provide permission...", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectFile() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent,100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == 100)
        {
            image.setImageURI(data.getData());
            Uri uri = data.getData();
            try {
                img = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(requestCode == 200)
        {

            img = (Bitmap) data.getExtras().get("data");
            image.setImageBitmap(img);
        }
    }
}

