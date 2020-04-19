package com.example.androidfirebasestorage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.shashank.sony.fancytoastlib.FancyToast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int RESULT_LOAD_IMG = 1000;

    ImageView mImageView;
    Button btnUpload;
    EditText edtImageName , edtTmageDownloadLink;
    TextView textUploading , textUUIDImageName;
    ProgressBar mProgressBar;

    private String ImageExtension , mSelectedImageName;

    // Firebase Storage Ref.
    private StorageReference mStorageRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.imageView);
        mImageView.setOnClickListener(MainActivity.this);

        btnUpload = findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(MainActivity.this);

        edtImageName = findViewById(R.id.edtImageName);
        edtImageName.setText("Please Select Your Target Image First !");

        edtTmageDownloadLink = findViewById(R.id.edtImageDownloadLink);

        textUploading = findViewById(R.id.textView3);
        textUUIDImageName = findViewById(R.id.textUUIDImageName);
        textUUIDImageName.setOnClickListener(MainActivity.this);
        textUUIDImageName.setClickable(false);


        mProgressBar = findViewById(R.id.progressBar);

        makeInfoToast(MainActivity.this , "Click On ImageView To Select Target Image First !");

        // Firebase Storage Ref.
        mStorageRef = FirebaseStorage.getInstance().getReference();

    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.imageView) {
            getImageLocation();
        }

        if (v.getId() == R.id.btnUpload) {

            // Show Progress Bar
            textUploading.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);

            // Upload Photo Form ImageView To Firebase Storage
            mImageView.setDrawingCacheEnabled(true);
            mImageView.buildDrawingCache();
            Bitmap bitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG , 100 , outputStream);
            byte[] data = outputStream.toByteArray();

            final StorageReference imageRef = mStorageRef.child(edtImageName.getText().toString()); // Create Firebase Storage Ref. To Upload To It.
            StorageMetadata metaData = new StorageMetadata.Builder() // Get Image / File Meta Data If It Have or Your Want !
                    .setContentType("image/jpg")
                    .build();

            UploadTask uploadTask = imageRef.putBytes(data , metaData);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // Show Error Toast Notification
                    makeErrorToast(MainActivity.this , "Your Image Was Upload UnSuccessful Please Try Again !");
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Show Success Toast Notification
                    makeSuccessToast(MainActivity.this , "Your Image Upload Was Successful !");
                    // Update Download Uri To User
                    taskSnapshot.getMetadata().getReference().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            edtTmageDownloadLink.setText(task.getResult().toString());
                        }
                    });
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    mProgressBar.setProgress((int) progress);
                }
            }).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    // On Complete
                }
            });

        }

        if (v.getId() == R.id.textUUIDImageName) {
            edtImageName.setText(generateUUID() + "." + ImageExtension);
        }

    }

    private void getImageLocation() {

        try {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK || data != null) { // Get Selected Image And Show On ImageView || Update TextView Image Name
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                File file = new File(String.valueOf(imageUri));
                mSelectedImageName = file.getName(); // Get Selected Image Name Form Uri
                ImageExtension = getContentResolver().getType(imageUri);
                ImageExtension = ImageExtension.substring(ImageExtension.lastIndexOf("/") + 1); // SubString File Ext. Only . MIME Type
                mImageView.setImageBitmap(selectedImage); // Set Selected Image On ImageView
                edtImageName.setText(mSelectedImageName + "." + ImageExtension); // Set Current Image Name Appere To User
                textUUIDImageName.setClickable(true); // Allow User Can Click After Selected Image
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void makeInfoToast(Context context , String text) { // Call This Method To Show Info Toast
        FancyToast.makeText(context ,text ,FancyToast.LENGTH_LONG , FancyToast.INFO ,false).show();
    }

    private void makeErrorToast(Context context , String text) {
        FancyToast.makeText(context , text , Toast.LENGTH_LONG , FancyToast.ERROR , false).show();
    }

    private void makeSuccessToast(Context context , String text) {
        FancyToast.makeText(context , text , Toast.LENGTH_LONG , FancyToast.SUCCESS , false).show();
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    private String getImageExtension(Uri uri) { // Not Use In This Project But Create For Example Propuse
        String uriToString = getContentResolver().getType(uri);
        uriToString = uriToString.substring(uriToString.lastIndexOf("/") + 1);
        return uriToString;
    }

}
