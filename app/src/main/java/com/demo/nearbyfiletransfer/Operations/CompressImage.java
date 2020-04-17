package com.demo.nearbyfiletransfer.Operations;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CompressImage {

    //required empty constructor
    public CompressImage() {
    }

    public static File compress(Context context, File fileToCompress, int quality) {
        /*context = mContext;
        fileToCompress = mFileToCompress;
        quality = mQuality;*/
        //create bitmap from file
        Bitmap originalBitmap = BitmapFactory.decodeFile(fileToCompress.getPath());
        //compress the created bitmap
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, os);
        //decode the compressed bitmap into a file

        Bitmap compressed = BitmapFactory.decodeStream(new ByteArrayInputStream(os.toByteArray()));
        String filename = fileToCompress.getName()
                .substring(0, fileToCompress.getName().lastIndexOf('.')) + "-compressed.jpg";
        File location = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "RecievedImages");
        if(!location.exists()){
            location.mkdir();
        }
        File newFile = new File(location, filename);
        try {
            FileOutputStream fos = new FileOutputStream(newFile);
            fos.write(os.toByteArray());
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newFile;
    }
}