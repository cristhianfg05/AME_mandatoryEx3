package com.example.ame_mandatory_excersice_cristhianfajardo_arnaupapiol;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;

public class CustomDialog extends Dialog {
    private String dataToShow;
    TextView textView;
    private String url;

    ImageView img;


    public CustomDialog(Context context, String dataToShow) {
        super(context);
        this.dataToShow = dataToShow;
    }

    public CustomDialog(Context context, String dataToShow, String urlPhoto) {
        super(context);
        this.dataToShow = dataToShow;
        this.url = urlPhoto;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_dialog_layout);
        textView = findViewById(R.id.textViewData);
        textView.setText(dataToShow);
        img = findViewById(R.id.imageWeather);
        if (this.url != null) {
            new LoadImageTask(img).execute(this.url);
        }


    }

    private static class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        private WeakReference<ImageView> imageViewReference;

        LoadImageTask(ImageView imageView) {
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                return BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView imageView = imageViewReference.get();
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}
