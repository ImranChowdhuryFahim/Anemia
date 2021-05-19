package com.defense.anemiadetection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.defense.anemiadetection.ml.Model;


import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    private Button select,capture,predict;
    private ImageView image;
    private TextView tv;
    private Bitmap img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        select = findViewById(R.id.select);
        capture = findViewById(R.id.capture);
        predict = findViewById(R.id.predict);
        image = findViewById(R.id.iv);
        tv = findViewById(R.id.tv);

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
                try {
                    img = Bitmap.createScaledBitmap(img,224,224,true);
                    Model model = Model.newInstance(MainActivity.this);


                    TensorImage tensorImage = new TensorImage(DataType.UINT8);
                    tensorImage.load(img);

                    ByteBuffer byteBuffer = tensorImage.getBuffer();

                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.UINT8);
                    inputFeature0.loadBuffer(byteBuffer);
                    // Runs model inference and gets result.
                    Model.Outputs outputs = model.process(inputFeature0);
                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();


                    // Releases model resources if no longer used.
                    model.close();

                    if(outputFeature0.getFloatArray()[0] >= outputFeature0.getFloatArray()[1])
                    {
                        tv.setText("\nYes\n Anemia detected.");
                    }
                    else {
                        tv.setText("\nNo Anemia");
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void capture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent,200);
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