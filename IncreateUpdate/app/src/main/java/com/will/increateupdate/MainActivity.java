package com.will.increateupdate;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.will.increateupdate.util.ApkUtils;
import com.will.increateupdate.util.BsPatch;
import com.will.increateupdate.util.Constants;
import com.will.increateupdate.util.DownloadUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new ApkUpdateTask().execute();
//        Toast.makeText(MainActivity.this, "已经是最新版本", Toast.LENGTH_LONG).show();
    }

    class ApkUpdateTask extends AsyncTask<Void, Void, Boolean>{

        @Override
        protected Boolean doInBackground(Void... voids) {
            try{
                Log.d("update", "开始下载...");
                //下载服务端的差分包
                File patchFile = DownloadUtils.download(Constants.URL_PATCH_DOWNLOAD);
                //获取当前apk位置
                String oldFile = ApkUtils.getSourceApkPath(MainActivity.this, getPackageName());
                String newFile = Constants.NEW_APK_PATH;
                //调用jni合并apk
                BsPatch.patch(oldFile, newFile, patchFile.getAbsolutePath());
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean){
                Toast.makeText(MainActivity.this,"您正在进行增量更新", Toast.LENGTH_LONG).show();
                //安装apk
                ApkUtils.installApk(MainActivity.this, Constants.NEW_APK_PATH);
            }
        }
    }
}
