package com.anyan.headerdemo;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "MainActivity";

    private ImageView ivHeader;
    private String userId = "30986";//模拟用户唯一标志
    private String filePath;
    private static final int CAMERA_REQUEST_CODE = 0;//调用系统相机请求妈
    private static final int ALBUM_REQUEST_CODE = 1;//调用相册请求码
    private static final int CROP_REQUEST_CODE = 2;//裁剪图片的请求码
    private static final int CAMERA_PERMISSION_CODE = 3;//相机权限请求码
    private static final int ALBUM_PERMISSION_CODE = 4;//相册权限请求码

    private String[] cameraPerms = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private String[] capturePerms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        ivHeader = findViewById(R.id.iv_header);
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        //首次进入先查找文件中是否有之前保存的头像文件，有则显示，然后调用后台接口刷新图片
        File file = new File(filePath + "/header/" + userId + ".jpg");
        if (file.exists()) {
            Log.e(TAG, "====initView: 文件存在");
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (bitmap != null) {
                ivHeader.setImageBitmap(bitmap);
            }
        }
        //请求服务器，下载头像图片，刷新UI，以服务器的为准（因为可能在另一部手机做了修改，这时本地缓存的就不准了）
    }

    public void onClick(View view) {
        switch (view.getId()) {
            //拍照修改头像
            case R.id.btn_camera:
                //需要相机，读写存储权限
                //检查是否已经有了权限
                if (EasyPermissions.hasPermissions(this, cameraPerms)) {
                    //已经有了相关权限，发起系统相机调用
                    requestCamera();
                } else {
                    //没有权限先申请权限
                    requestCameraPerms(cameraPerms);
                }
                break;
            //从相册中选择
            case R.id.btn_capture:
                //读写存储权限
                //检查是否已经有了权限
                if (EasyPermissions.hasPermissions(this, capturePerms)) {
                    //已经有了相关权限，发起系统相册调用
                    requestAlbum();
                } else {
                    //没有权限先申请权限
                    requestAlbumPerms();
                }
                break;
        }
    }

    //调用系统相机
    private void requestCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File file = new File(filePath + "/camera/" + userId + ".jpg");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (file.exists()) {
            file.delete();
        }
        file = new File(filePath + "/camera/" + userId + ".jpg");
        //兼容7.0的写法
        Uri desUri = FileProvider.getUriForFile(this, "com.anyan.headerdemo.fileprovider", file);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//授予目标应用临时权限
        intent.putExtra(MediaStore.EXTRA_OUTPUT, desUri);//保存在指定的位置
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    //调用系统相册
    private void requestAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, ALBUM_REQUEST_CODE);
    }

    //调用裁剪功能
    private void crop(Uri fromUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(fromUri, "image/*");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 400);
        intent.putExtra("outputY", 400);
        intent.putExtra("scale", true);
        //将剪切的图片保存到目标Uri中
        File file = new File(filePath + "/crop/" + userId + ".jpg");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (file.exists()) {
            file.delete();
        }
        file = new File(filePath + "/crop/" + userId + ".jpg");
        Uri desUri = Uri.fromFile(file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, desUri);
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
        startActivityForResult(intent, CROP_REQUEST_CODE);
    }

    private void updateHeader() {
        String url = "http://pubmanage.sgwzone.com:8088/jianshen/interface/uploadHeadPic";
        final File file = new File(filePath + "/crop/" + userId + ".jpg");
        Map<String, String> map = new HashMap<>();
        map.put("token", "17494881517801699987");
        map.put("picName", "30986.jpg");
        //使用张鸿阳封装的okHttpUtil网络框架，这里根据自己的项目改变
        OkHttpUtils.post().addFile("image", "123.jpg", file).url(url).params(map).build().execute(new StringCallback() {
            @Override
            public void onError(okhttp3.Call call, Exception e) {

            }

            @Override
            public void onResponse(String response) {
                Log.e(TAG, "====onResponse: " + response);
                //上传成功后，修改本地保存
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String resultCode = jsonObject.optString("resultCode");
                    if (resultCode.equals("0")) {
                        File headerFile = new File(filePath + "/header/" + userId + ".jpg");
                        if (!headerFile.getParentFile().exists()) {
                            headerFile.getParentFile().mkdirs();
                        }
                        if (headerFile.exists()) {
                            headerFile.delete();
                        }
                        headerFile = new File(filePath + "/header/" + userId + ".jpg");
                        File cropFile = new File(filePath + "/crop/" + userId + ".jpg");
                        FileInputStream fileInputStream = new FileInputStream(cropFile);
                        FileOutputStream fileOutputStream = new FileOutputStream(headerFile);
                        byte[] b = new byte[1024];
                        while (fileInputStream.read(b) != -1) {
                            fileOutputStream.write(b);
                        }
                        fileOutputStream.flush();
                        fileOutputStream.close();
                        fileInputStream.close();
                        ivHeader.setImageBitmap(BitmapFactory.decodeFile(headerFile.getAbsolutePath()));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAMERA_REQUEST_CODE:
                    //调用相机回调
                    Uri fromUri = FileProvider.getUriForFile(this, "com.anyan.headerdemo.fileprovider", new File(filePath + "/camera/" + userId + ".jpg"));
                    crop(fromUri);
                    break;
                case ALBUM_REQUEST_CODE:
                    //调用相册回调
                    Uri uri = data.getData();
                    crop(uri);
                    break;
                case CROP_REQUEST_CODE:
                    //调用裁剪回调
                    //请求服务器上传图片
                    updateHeader();
                    break;
            }
        }
    }

    //申请相机相关权限
    private void requestCameraPerms(String... perms) {
        EasyPermissions.requestPermissions(this, "需要相机和存储权限，否则无法使用该功能", CAMERA_PERMISSION_CODE, perms);
    }

    //申请相册相关权限
    private void requestAlbumPerms(String... perms) {
        EasyPermissions.requestPermissions(this, "需要存储权限，否则无法使用该功能", ALBUM_PERMISSION_CODE, perms);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);

    }


    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.e(TAG, "=======onPermissionsGranted: ");
        switch (requestCode) {
            case CAMERA_PERMISSION_CODE:
                if (EasyPermissions.hasPermissions(this,cameraPerms)){
                    requestCamera();
                }
                break;
            case ALBUM_PERMISSION_CODE:
                if (EasyPermissions.hasPermissions(this,capturePerms)){
                    requestAlbum();
                }
                break;
        }

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.e(TAG, "=======onPermissionsDenied: ");
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            //点击了不再询问
            Log.e(TAG, "=======onPermissionsDenied:内内 ");
            new AppSettingsDialog.Builder(this).build().show();
        } else {
            switch (requestCode) {
                case CAMERA_PERMISSION_CODE:
                    EasyPermissions.requestPermissions(this, "需要相机和存储权限，否则无法使用拍照功能", CAMERA_PERMISSION_CODE, cameraPerms);
                    break;
                case ALBUM_PERMISSION_CODE:
                    EasyPermissions.requestPermissions(this, "需要存储权限，否则无法使用相册功能", ALBUM_PERMISSION_CODE, capturePerms);
                    break;
            }

        }
    }
}
