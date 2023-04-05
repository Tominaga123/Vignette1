package com.example.vignette1;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import androidx.exifinterface.media.ExifInterface;

import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView textView;
    private TextView textView2;
    private TextView textView3;
    private SeekBar seekBar;
    private SeekBar seekBar2;
    private int i = 0; //明るさの度合い
    private double j = 1.1;// 二次関数の切片
    private ImageView imageView;
    private Bitmap bmp;
    private Bitmap bmp2;
    // private Bitmap bmp3;
    private int w; // Bitmapの横幅
    private int h; // Bitmapの縦幅
    private double tapW; // 画像のタップした場所
    private double tapH; // 画像のタップした場所
    private static final int REQUEST_CODE = 100;
    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent resultData  = result.getData();
                    if (resultData  != null) {
                        openImage(resultData);
                    }
                }
            });

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (checkPermission()) {
            Toast.makeText(this, "許可されています", Toast.LENGTH_SHORT).show();
        } else {
            requestPermission();
        }

        progressBar = findViewById(R.id.progressBar);
        textView = findViewById(R.id.text_view);
        Button button = findViewById(R.id.button);
        Button saveButton = findViewById(R.id.button2);
        Button vignetteButton = findViewById(R.id.button3);
        textView2 = findViewById(R.id.text_view2);
        seekBar = findViewById(R.id.seekbar);
        textView3 = findViewById(R.id.text_view3);
        seekBar2 = findViewById(R.id.seekbar2);
        imageView = findViewById(R.id.image_view);

        // 初期値を表示
        textView2.setText("明るさ:0");
        textView3.setText("変化の範囲:最もせまい");

        //「Get Image」を押した場合
        button.setOnClickListener( v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            resultLauncher.launch(intent);
        });

        //「Save Image」を押した場合
        saveButton.setOnClickListener(v -> {
            if (checkPermission()) {
                saveImage();
                reset();
                bmp = bmp2;
                imageView.setImageBitmap(bmp);
            } else {
                requestPermission();
            }
        });

        //「Vignette」を押した場合
        vignetteButton.setOnClickListener(v -> vignette());

        // 「imageView」をタップした場合
        imageView.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                // タップした場所を視覚的に分かるように半透明の円形を作成
                View view = new View(MainActivity.this);
                view.setLayoutParams(new ViewGroup.LayoutParams(100,100));
                view.setBackgroundResource(R.drawable.circle_background);
                view.setX((imageView.getX() + event.getX() - 50));
                view.setY((imageView.getY() + event.getY() - 50));
                // レイアウトに追加
                ViewGroup layout = findViewById(android.R.id.content);
                layout.addView(view);
                // 0.5秒後にビューを削除
                Handler handler = new Handler();
                handler.postDelayed(() -> layout.removeView(view),500);

                // タップした位置におけるBitmap上の座標を求める
                tapW = event.getX() * w / imageView.getWidth();
                tapH = event.getY() * h / imageView.getHeight();
            }
            return true;
        });

        // 「SeekBar」が変化した場合
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // シークバーの値を取得して表示する
                i = seekBar.getProgress();
                String seekBar_text = getString(R.string.seekBar_text, Integer.toString(i));
                textView2.setText(seekBar_text);
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
        });

        // 「SeekBar2」が変化した場合
        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //シークバー2の値を取得してテキストを表示
                int value = seekBar.getProgress();
                switch(value){
                    case 1:
                        textView3.setText("変化の範囲:最もせまい");
                        value = 11;
                        break;
                    case 2:
                        textView3.setText("変化の範囲:かなりせまい");
                        value = 10;
                        break;
                    case 3:
                        textView3.setText("変化の範囲:せまい");
                        value = 9;
                        break;
                    case 4:
                        textView3.setText("変化の範囲:少しせまい");
                        value = 8;
                        break;
                    case 5:
                        textView3.setText("変化の範囲:わずかにせまい");
                        value = 7;
                        break;
                    case 6:
                        textView3.setText("変化の範囲:普通");
                        break;
                    case 7:
                        textView3.setText("変化の範囲:わずかに広い");
                        value = 5;
                        break;
                    case 8:
                        textView3.setText("変化の範囲:少し広い");
                        value = 4;
                        break;
                    case 9:
                        textView3.setText("変化の範囲:広い");
                        value = 3;
                        break;
                    case 10:
                        textView3.setText("変化の範囲:かなり広い");
                        value = 2;
                        break;
                    case 11:
                        textView3.setText("変化の範囲:最も広い");
                        value = 1;
                        break;
                }
                j = (double)value / 10;
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void vignette(){
        //画像があればビネットする
        if(imageView.getDrawable() != null) {
            //時間がかかる処理なのでビジーマークを表示する
            progressBar.setVisibility(View.VISIBLE);
            progressBar.bringToFront();
            progressBar.setLayoutParams(new ViewGroup.LayoutParams(100,100));
            ViewGroup layout = findViewById(android.R.id.content);
            layout.addView(progressBar);

            // 中心点からの最も遠い距離を計算する
            // まず、タップした位置が、画像を4分割した場合のどの範囲にあるか調べる
            int flag = 0;
            if (tapW > (double) w / 2) {
                flag++;
            }
            if (tapH > (double) h / 2) {
                flag += 10;
            }
            double farthest;
            switch (flag) {
                case 0: // タップ位置が画像の左上の場合、最も遠いのは画像の右下
                    System.out.println("case0");
                    farthest = (double) Math.round(Math.sqrt((w - tapW) * (w - tapW) + (h - tapH) * (h - tapH)));
                    break;
                case 1: // タップ位置が画像の右上の場合、最も遠いのは画像の左下
                    System.out.println("case1");
                    farthest = (double) Math.round(Math.sqrt(tapW * tapW + (h - tapH) * (h - tapH)));
                    break;
                case 10: // タップ位置が画像の左下の場合、最も遠いのは画像の右上
                    System.out.println("case10");
                    farthest = (double) Math.round(Math.sqrt((w - tapW) * (w - tapW) + tapH * tapH));
                    break;
                // タップ位置が画像の中心または右下の場合、最も遠いのは画像の左上
                default:
                    System.out.println("case11");
                    farthest = (double) Math.round(Math.sqrt(tapW * tapW + tapH * tapH));
            }
            double ratio;
            int add;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = bmp.getPixel(x, y);
                    //シフト演算してr,g,bごとに0x??の形にそろえる
                    int r = (c & 0x00ff0000) >> 16;
                    int g = (c & 0x0000ff00) >> 8;
                    int b = c & 0x000000ff;

                    // 現在地の中心点からの距離を計算する
                    double distanceW = x - tapW;
                    double distanceH = y - tapH;
                    double distance = Math.round(Math.sqrt(distanceW * distanceW + distanceH * distanceH));
                    //　最も遠い場所と現在地の比率を計産する
                    ratio = distance / farthest;
                    // 比率とシークバーの値から明るさの調整値を決める
                    // BigDecimalを使ってもよいかも
                    // y = (a * x^2 - b) * i
                    if ((3 * ratio * ratio - j) < 0) {
                        add = 0;
                    } else {
                        add = (int) Math.round((3 * ratio * ratio - j) * 1.5 * i);
                    }
                    //r,g,bの値を調整する
                    r += add;
                    if (r > 0xff) {
                        r = 0xff;
                    } else if (r < 0x00) {
                        r = 0x00;
                    }
                    g += add;
                    if (g > 0xff) {
                        g = 0xff;
                    } else if (g < 0x00) {
                        g = 0x00;
                    }
                    b += add;
                    if (b > 0xff) {
                        b = 0xff;
                    } else if (b < 0x00) {
                        b = 0x00;
                    }
                    c = 0xff000000 | (r << 16) | (g << 8) | b;
                    bmp2.setPixel(x, y, c);
                }
            }
            imageView.setImageBitmap(bmp2);
            //処理が終わればビジーマークを非表示にする
            progressBar.setVisibility(View.INVISIBLE);
        }else {
            Toast.makeText(this, "画像が選択されていません", Toast.LENGTH_SHORT).show();
        }
    }

    private void openImage(Intent resultData){
        ParcelFileDescriptor pfDescriptor = null;
        try{
            Uri uri = resultData.getData();
            // Uriを表示
            textView.setText(
                    String.format(Locale.US, "Uri:　%s",uri.toString()));

            pfDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            if(pfDescriptor != null){
                FileDescriptor fileDescriptor = pfDescriptor.getFileDescriptor();
                bmp = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                ExifInterface exif = new ExifInterface(fileDescriptor);
                rotateImage(exif);
                pfDescriptor.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try{
                if(pfDescriptor != null){
                    pfDescriptor.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void rotateImage(ExifInterface exif){
        try {
            // Exif メタデータを取得
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            // 回転角度を計算
            int rotationAngle = 0;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationAngle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationAngle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationAngle = 270;
                    break;
                default:
                    break;
            }

            // 画像を回転させる
            if (rotationAngle != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationAngle);
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ImageView に Bitmap を設定
        imageView.setImageBitmap(bmp);
        // 画像の縦横のサイズを取得
        w = bmp.getWidth();
        h = bmp.getHeight();
        tapW = (double)w/2;
        tapH = (double)h/2;
        // 編集画面用の Bitmap を複製
        bmp2 = bmp.copy(Bitmap.Config.ARGB_8888, true);
        // リセット
        reset();
    }

    private void saveImage() {
        if(imageView.getDrawable() != null){
            BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
            Bitmap bitmap = drawable.getBitmap();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "SampleImage.jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            try (OutputStream os = resolver.openOutputStream(Objects.requireNonNull(uri))) {
                if (os != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                    Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
            }
        } else {
            Toast.makeText(this, "画像が選択されていません", Toast.LENGTH_SHORT).show();
        }
    }

    // リセット
    private void reset(){
        seekBar.setProgress(0);
        seekBar2.setProgress(1);
        textView2.setText("明るさ:0");
        textView3.setText("変化の範囲:最もせまい");
        i = 0;
        j = 1.1;
        tapW = (double)w/2;
        tapH = (double)h/2;
    }


    private boolean checkPermission() {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        int result;
        int flag = 0;
        for (String permission : permissions) {
            result = ContextCompat.checkSelfPermission(this, permission);
            if (result == PackageManager.PERMISSION_GRANTED) {
                flag ++;
                System.out.println(permission + "は許可されています");
            }
        }
        return flag == 2;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_CODE
        );
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            int flag = 0;
            for(int result : grantResults){
                if(result == PackageManager.PERMISSION_GRANTED){
                    flag++;
                }
            }
            if(grantResults.length == flag){
                Toast.makeText(this, "許可されました", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "ストレージへのアクセスが許可されていません", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}