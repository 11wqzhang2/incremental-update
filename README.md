# incremental-update
基于bsdiff的apk增量更新
前言
传统的APP更新，每一次产品迭代，都需要用户下载新的完整apk安装包后，重新安装。当apk的体积达到一定程度时，这种更新就会特别的浪费时间和流量，同时也影响用户体验。针对这一问题，目前市场上出现了很多热更新、热修复等技术如阿里的Anfix、腾讯的Tinker框架等。其中，腾讯的Tinker框架实现原理上，就用到了开源的文件差分工具bsdiff/bspatch。我们今天就来介绍一下基于bsdiff差分工具的增量更新技术。

增量更新原理
增量更新的原理是，利用工具（bsdiff工具），将旧版本和新版本的apk安装包二进制文件进行对比，得到差分包（即两个版本的差异文件），然后下发到客户端（已安装旧版本），而这个差分文件的大小肯定是小于新的apk文件大小的。客户端得到这个差分文件之后，本地在使用bspatch工具进行差分文件和本地已经安装的旧apk包进行合并成新的apk包文件，然后在进行升级安装。

在这个过程中，客户端在访问服务端的时候可能需要携带旧apk包的md5，应用包名，版本号等信息，服务端获取到之后会去数据库中查询其对应的本次需要升级的apk包以及旧版本号对应的旧apk包，然后进行差分处理得到差分文件，在下发到客户端即可。



实战
 一、前期准备

前面已经介绍了增量更新的原理，这里采用bsdiff(Binary diff)开源库来实现新旧包差分比较。Binary diff是依赖 bzip 压缩库的开源库，所以也要下bzip库源码。

linux 的相关 diff/patch 下载 http://www.daemonology.net/bsdiff/

windows 上的 bsdiff http://sites.inka.de/tesla/others.html#bsdiff

相关依赖 bzip 文档及下载http://www.bzip.org/downloads.html

二、java服务端实现

分析bsdiff.cpp源码，找到main函数入口，因为这里我们要利用这个开源库通过jni生成动态库，也即是通过jni层来调用这个方法去实现我们的功能，即把main函数重新命名，方便我们调用；
//argc必须为4
//*argv[]传四个参数：argv[0]->任意值 argv[1]->oldfile argv[2]->newfile argv[3]->patchfile
int bsdiffMain(int argc,char *argv[])
{
	int fd;
	u_char *old,*_new;
	off_t oldsize,newsize;
	off_t *I,*V;
	off_t scan,pos,len;
	off_t lastscan,lastpos,lastoffset;
	off_t oldscore,scsc;
	off_t s,Sf,lenf,Sb,lenb;
	off_t overlap,Ss,lens;
	off_t i;
	off_t dblen,eblen;
	u_char *db,*eb;
	u_char buf[8];
	u_char header[32];
	FILE * pf;
	BZFILE * pfbz2;
	int bz2err;
    // argc不等于4则抛出错误
	if(argc!=4) errx(1,"usage: %s oldfile newfile patchfile\n",argv[0]);

    ....
}

 分析完bsdiff.cpp的入口函数后，知道我们这边所需要传的参数，新建web工程生成头文件：
public class BsDiff {
	
	/**
	 * 差分
	 * @param oldFile 久apk路径
	 * @param newFile 新apk路径
	 * @param patchFile 差分文件路径
	 */
	public native static void diff(String oldFile, String newFile, String patchFile);
	

	static {
		System.loadLibrary("bsdiff");
	}
}
通过javah生成头文件，然后添加到VS工程中。

 C++层通过jni函数调用bsdiff的入口函数
//java层调用native 差分方法
JNIEXPORT void JNICALL Java_com_will_bisdiff_BsDiff_diff
(JNIEnv *env, jclass jcls, jstring oldFile, jstring newFile, jstring patchFile){
	int argc = 4;
	char *old_file = (char*)env->GetStringUTFChars(oldFile, NULL);
	char *new_file = (char*)env->GetStringUTFChars(newFile, NULL);
	char *patch_file = (char*)env->GetStringUTFChars(patchFile, NULL);
	char *argv[4];
	argv[0] = "bsdiff";
	argv[1] = old_file;
	argv[2] = new_file;
	argv[3] = patch_file;

	bsdiffMain(argc, argv);

	env->ReleaseStringUTFChars(oldFile, old_file);
	env->ReleaseStringUTFChars(newFile, new_file);
	env->ReleaseStringUTFChars(patchFile, patch_file);
}


生成dll动态链接文件，添加到web工程中，配置方法参见https://blog.csdn.net/u011598031/article/details/85048731。
在编译过程中会遇到两个报错，用了不安全的函数和用了过时的函数，可以在文件开头添加宏定义#define _CRT_SECURE_NO_WARNINGS和#define _CRT_NONSTDC_NO_DEPRECATE 或者 右击项目属性—>配置属性—>C/C++—>命令行 输入-D _CRT_SECURE_NO_WARNINGS -D _CRT_NONSTDC_NO_DEPRECATE

配置完成后再编译，发现了另一个错误：指针变量未初始化

 

Windows环境下会做严格的语法检查，要去掉这个检查 ，将项目属性中SDL检查设置为否。

 三、Android端实现

Android端这边主要实现的功能有两个

1、下载服务端的差分文件

2、利用bspatch工具合并旧apk文件和差分包生成新的apk文件，并安装



 



新建Android工程，勾选Include C/C++ support选项，生成工程文件；
将bzip工具包和bspatch.c文件添加到工程cpp文件目录下，把AS生成的native-lib.cpp干掉。
将bzip的代码头文件添加到bspatch.c依赖
#include "bzip2/bzlib.c"
#include "bzip2/crctable.c"
#include "bzip2/compress.c"
#include "bzip2/decompress.c"
#include "bzip2/randtable.c"
#include "bzip2/blocksort.c"
#include "bzip2/huffman.c"
找到bspatch.c的入口函数，发现传参和bsdiff.cpp的参数是一致的，用同样的套路，在Android端生成头文件，编写jni函数供Android调用：

public class BsPatch {

    public native static void patch(String oldPath, String newPath, String patchPath);

    static {
        System.loadLibrary("bspatch");
    }
}
//java层调用native 合并方法
JNIEXPORT void JNICALL Java_com_will_increateupdate_util_BsPatch_patch
        (JNIEnv *env, jclass jcls, jstring oldFile, jstring newFile, jstring patchFile){

        int argc = 4;
        char *old_file = (char*)(*env)->GetStringUTFChars(env, oldFile, NULL);
        char *new_file = (char*)(*env)->GetStringUTFChars(env, newFile, NULL);
        char *patch_file = (char*)(*env)->GetStringUTFChars(env, patchFile, NULL);
        char *argv[4];
        argv[0] = "bsdiff";
        argv[1] = old_file;
        argv[2] = new_file;
        argv[3] = patch_file;
    //原来的函数重命名为bsPatchMain
    bsPatchMain(argc, argv);

    (*env)->ReleaseStringUTFChars(env, oldFile, old_file);
    (*env)->ReleaseStringUTFChars(env, newFile, new_file);
    (*env)->ReleaseStringUTFChars(env, patchFile, patch_file);

}


编译生成so文件。我这里用的AS3.1.2版本，AS3.1.2使用的CMake工具来编译NDK代码的的，修改CMakeLists.txt文件，加入bspatch.c和生成的库名字，生成so库

访问服务端下载APP，调用so库合并成新的apk并安装：

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



生成新旧两个版本的apk文件，这里的新旧版本的apk做了背景和版本字符串的修改，并在新版本加了一些大图以增加体积。生成的两个apk文件通过刚刚java端编写的代码生成差分包，并通过Tomcat发布。

public class BsPatchMain {

	public static void main(String[] args) {
		// 异步执行，得到差分包
		BsDiff.diff(ConstantsWin.OLD_APK_PATH, ConstantsWin.NEW_APK_PATH, ConstantsWin.PATCH_PATH);
	}

}

手机安装旧apk，自动更新：

 增量更新完成！！
