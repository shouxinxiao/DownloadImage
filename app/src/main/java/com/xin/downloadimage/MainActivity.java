package com.xin.downloadimage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;



import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView mImageView;
    private Button mButton;

    // data
    private static final String TAG = "TAG";
    private static final int TAKE_BIG_PICTURE = 1;
    private static final int CROP_BIG_PICTURE = 3;
    private static final int GET_BIG_PHOTO = 4;
    private static final String IMAGE_FILE_LOCATION = "file:///sdcard/temp.jpg";	//默认的路径

    private Uri imageUri;// to store the big bitmap
    private String srcPath = ""; // 需上传的文件路径

    /** Activity life cycle */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.imageView);
        mButton = (Button) findViewById(R.id.button);

        mImageView.setOnClickListener(this);
        mButton.setOnClickListener(this);
        imageUri = Uri.parse(IMAGE_FILE_LOCATION);
    }

    /** Handle click events */
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageView:
                changeHeadIcon();
                break;
            case R.id.button:
                // sd卡上保存的位置是：/storage/emulated/0/dong/image/20161029054250.jpg
                // 获取照片裁剪后的Uri file:///sdcard/temp.jpg
                // 上传时的路径： /mnt/sdcard/temp.jpg
                String s = Environment.getExternalStorageDirectory().toString()
                        + srcPath.substring(14);
                srcPath = s;
                Log.d("TAG", "上传时的路径：" + srcPath);
                setNetDate();
                break;
            default:
                break;
        }
    }

    private void changeHeadIcon() {
        final CharSequence[] items = { "拍照", "相册" };
        AlertDialog dlg = new AlertDialog.Builder(MainActivity.this)
                .setTitle("选娶照片方式")
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        // 这里item是根据选择的方式，
                        Intent intent = null;
                        if (item == 0) { // 拍照
                            if (imageUri == null)
                                Log.e(TAG, "image uri can't be null");
                            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                            startActivityForResult(intent, TAKE_BIG_PICTURE);
                        } else { // 相册
                            intent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(intent, GET_BIG_PHOTO);
                        }
                    }
                }).create();
        dlg.show();
    }
    /**
     * 裁剪
     */
    private void cropImageUri(Uri uri, int outputX, int outputY, int requestCode) {
        // 裁剪图片意图
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        // 下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);// 裁剪框的比例，1：1 // 输出是X方向的比例
        intent.putExtra("aspectY", 1.5);
        intent.putExtra("outputX", outputX);// 裁剪后输出图片的尺寸大小,不能太大500程序崩溃
        intent.putExtra("outputY", outputY);
        intent.putExtra("scale", true);// 去黑边
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);// 同一个地址下 裁剪的图片覆盖拍照的图片
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());// 图片格式
        intent.putExtra("noFaceDetection", true); // 取消人脸识别
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {// result is not correct
            Log.e(TAG, "requestCode = " + requestCode);
            Log.e(TAG, "resultCode = " + resultCode);
            Log.e(TAG, "data = " + data);
            return;
        } else {
            switch (requestCode) {
                case TAKE_BIG_PICTURE: // 拍照
                    Log.d(TAG, "TAKE_BIG_PICTURE: data = " + data);// it seems to be
                    // TODO sent to crop
                    cropImageUri(imageUri, 200, 250, CROP_BIG_PICTURE);
                    // or decode as bitmap and display it
                    if (imageUri != null) {
                        Bitmap bitmap = decodeUriAsBitmap(imageUri);
                        mImageView.setImageBitmap(bitmap);
                    }
                    break;
                case CROP_BIG_PICTURE:// from crop_big_picture //裁剪后
                    Log.d(TAG, "CROP_BIG_PICTURE: data = " + data);// it seems to be
                    // null
                    if (imageUri != null) {
                        Bitmap bitmap = decodeUriAsBitmap(imageUri);
                        Log.d(TAG, "拍照后裁剪后的Uri" + imageUri);
                        srcPath = imageUri + "";
                        mImageView.setImageBitmap(bitmap);
                    }
                    break;
                case GET_BIG_PHOTO: // 相册上获取照片
                    cropImageUri(data.getData(), 200, 250, CROP_BIG_PICTURE);
                    Bitmap bitmap = decodeUriAsBitmap(imageUri);
                    mImageView.setImageBitmap(bitmap);
                    break;
                default:
                    break;
            }
        }
    }
    private Bitmap decodeUriAsBitmap(Uri uri) {	//
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(getContentResolver()
                    .openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

//    private String uri = "http://192.168.191.111:8080/ImageViewServer/ImageView";   //上传地址
	 private String uri = "http://10.158.76.142:8080/ImageViewServer/ImageView";

    @SuppressWarnings("unchecked")
    private void setNetDate() {
        final String url = uri;
        File file  = new File(srcPath);
        RequestBody fileBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "test.jpg", fileBody)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();


        final okhttp3.OkHttpClient.Builder httpBuilder = new OkHttpClient.Builder();
        OkHttpClient okHttpClient  = httpBuilder
                //设置超时
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "uploadMultiFile() e=" + e);
            }


            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i(TAG, "uploadMultiFile() response=" + response.body().string());
            }
        });
    }
}
