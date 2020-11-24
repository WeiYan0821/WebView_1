package com.tw.android.webview_1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.tw.android.webview_1.file.FileUtils;

import java.io.File;

import static com.tw.android.webview_1.file.File.getCompressBitmap;
import static com.tw.android.webview_1.file.File.getRealPathFromUri;
import static com.tw.android.webview_1.file.File.saveBitmapToFile;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private static final String TAG = "MainActivity";

    Context mContext;
    ProgressBar mProgressBar;
    private ValueCallback<Uri> uploadMessage;
    private ValueCallback<Uri[]> uploadMessageAboveL;
    private final static int FILE_CHOOSER_RESULT_CODE = 10000;
    private String mCameraFilePath = "";
    private boolean dele = false;

    @SuppressLint({"SourceLockedOrientationActivity", "NewApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT); //螢幕保持直向
        mContext = this;

        // 讀取檔案權限
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        // 手機瀏海區
        WindowManager.LayoutParams lpp = getWindow().getAttributes();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lpp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
        }
        getWindow().setAttributes(lpp);

        // 隱藏虛擬按鍵, 並且全屏
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if ((visibility & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) == 0) {
                // 虛擬按鍵出現要做的事情
                hideBottomUIMenu();
            }   // 虛擬按鍵消失要做的事情
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        webView = new WebView(this);
        mProgressBar = findViewById(R.id.progressBar);
        FrameLayout rootView = findViewById(R.id.xxx);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        rootView.addView(webView, layoutParams);


        // 聲明WebSettings子類
        WebSettings webSettings = webView.getSettings();
        // 如果訪問的頁面中要與Javascript交互, 則webview必須設置支持Javascript
        webSettings.setJavaScriptEnabled(true);
        // 若載入的 html 裡有JS 在執行動畫等操作，會造成資源浪費（CPU、電量）
        // 在 onStop 和 onResume 裡分別把 setJavaScriptEnabled() 給設定成 false 和 true 即可

        removeJavascriptInterfaces(webView);

        webSettings.setBlockNetworkImage(false); // 解決圖片不顯示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // 開啟DOM storage API功能（HTML5 提供的一種標準的介面，主要將鍵值對儲存在本地，在頁面載入完畢後可以通過 JavaScript 來操作這些資料。
        webSettings.setDomStorageEnabled(true);

        //設定自適應螢幕，兩者合用
        webSettings.setUseWideViewPort(true); //將圖片調整到適合webview的大小
        webSettings.setLoadWithOverviewMode(true); // 縮放至螢幕的大小

        //縮放操作
        webSettings.setSupportZoom(false); // 支持縮放, 默認為true. 是下面那個的前提.
        webSettings.setBuiltInZoomControls(false); // 設置內置的縮放控件. 若為false, 則該WebView不可縮放
        webSettings.setDisplayZoomControls(false); // 隱藏原生的縮放控件

        // 關閉密碼保存提醒(false)  開啟密碼保存功能(true)
        webSettings.setSavePassword(false);

        // 是否支持多窗口，默認值false
        webSettings.setSupportMultipleWindows(false);
        // 是否可用Javascript(window.open)打開窗口，默認值 false
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
        webSettings.setAllowContentAccess(true); // 是否可訪問Content Provider的資源，默認值 true
        // 設定可以訪問檔案
        webSettings.setAllowFileAccess(true);
        // 是否允許通過file url加載的Javascript讀取本地文件，默認值 false
        webSettings.setAllowFileAccessFromFileURLs(false);
        // 是否允許通過file url加載的Javascript讀取全部資源(包括文件,http,https)，默認值 false
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        // 支援通過JS開啟新視窗
//        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        webView.requestFocus(View.FOCUS_DOWN);

        //自動播放影片
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient( new  Browser_home());
        webView.setWebChromeClient( new MyChrome());

        // 屏蔽WebView的长按事件
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });

        loadWebsite();
        initProgressBar();
//        clearCookies(this);

    }

    @TargetApi(11)
    private static final void removeJavascriptInterfaces(WebView webView) {
        try {
            if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 17) {
                webView.removeJavascriptInterface("searchBoxJavaBridge_");
                webView.removeJavascriptInterface("accessibility");
                webView.removeJavascriptInterface("accessibilityTraversal");
            }
        } catch (Throwable tr) {
            tr.printStackTrace();
        }
    }

    private void initProgressBar() {
        mProgressBar.setMax(100);
    }

    @SuppressLint("ObsoleteSdkInt")
    protected void hideBottomUIMenu() {
        // 隱藏虛擬按鍵, 並且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            // for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    private void loadWebsite() {
//        webView.loadUrl("http://supreme178.com/"); //正式
        webView.loadUrl("http://space.rd-show.com/"); // 測試
//        webView.loadUrl("http://slot.club.tw/"); // 京站
        webView.loadUrl("");
    }

    private class Browser_home extends WebViewClient {
        Browser_home() {

        }

        // 开始载入页面时调用此方法，在这里我们可以设定一个loading的页面，告诉用户程序正在等待网络响应。

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d("view", "開始載入");
            Log.i(TAG, "onPageStarted:　");

            super.onPageStarted(view, url, favicon);

        }

        // 在页面加载结束时调用。我们可以关闭loading 条，切换程序动作。
        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d("view", "載入結束");
            Log.i(TAG, "onPageFinished: ");

            super.onPageFinished(view, url);

        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
        }

        // 連結跳轉都會走這個方法
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.i(TAG, "shouldOverrideUrlLoading : " + url);
//            webView.loadUrl(url);
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            Log.i(TAG, "error : " + error);
            Log.i(TAG, "request : " + request);
            Log.i(TAG, "view : " + view);
        }
    }

    private class MyChrome extends WebChromeClient {
        private View mCustomView;
        private WebChromeClient.CustomViewCallback mCustomViewCallback;
        protected FrameLayout mFullscreenContainer;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;

        MyChrome() {

        }

        public Bitmap getDefaultVideoPoster() {
            if (mCustomView == null) {
                return null;
            }
            return BitmapFactory.decodeResource(getApplicationContext().getResources(), 2130837573);
        }

        public void onHideCustomView() {
            ((FrameLayout) getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            setRequestedOrientation(this.mOriginalOrientation);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
        }

        public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback paramCustomViewCallback) {
            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }
            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            this.mOriginalOrientation = getRequestedOrientation();
            this.mCustomViewCallback = paramCustomViewCallback;
            ((FrameLayout) getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
            getWindow().getDecorView().setSystemUiVisibility(3846);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
            AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
            b.setMessage(message);
            b.setPositiveButton(android.R.string.ok, (dialog, which) -> result.confirm());
            b.setCancelable(false);
            b.create().show();
            return true;
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            Log.i(TAG , "title : " + title);
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);
            Log.i(TAG , "icon : " + icon);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            mProgressBar.setProgress(newProgress);
            if (newProgress == 100) {
                mProgressBar.setVisibility(View.GONE);
            }
        }

        // For Android < 3.0
        public void openFileChooser(ValueCallback<Uri> valueCallback) {
            uploadMessage = valueCallback;
            showFileChooser();
        }

        // For Android  >= 3.0
        public void openFileChooser(ValueCallback valueCallback, String acceptType) {
            uploadMessage = valueCallback;
            showFileChooser();
        }

        //For Android  >= 4.1
        public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String capture) {
            uploadMessage = valueCallback;
            showFileChooser();
        }

        // For Android >= 5.0
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            uploadMessageAboveL = filePathCallback;
            showFileChooser();
            return true;
        }

    }

    /**
     * 打开选择图片/相机
     */
    private void showFileChooser() {
        Intent intent1 = new Intent(Intent.ACTION_PICK, null);
        intent1.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");

        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        chooser.putExtra(Intent.EXTRA_TITLE, "File Chooser");
        chooser.putExtra(Intent.EXTRA_INTENT, intent1);
        this.startActivityForResult(chooser, FILE_CHOOSER_RESULT_CODE);
    }

    //删除图库照片
    private boolean deleteImage(String imgPath) {
        ContentResolver resolver = this.getContentResolver();
        Cursor cursor = MediaStore.Images.Media.query(resolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "=?",
                new String[]{imgPath}, null);
        boolean result = false;
        if (null != cursor && cursor.moveToFirst()) {
            long id = cursor.getLong(0);
            Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Uri uri = ContentUris.withAppendedId(contentUri, id);
            Log.i("--deleteImage--uri:" , String.valueOf(uri));
            int count = this.getContentResolver().delete(uri, null, null);
            result = count == 1;
        } else {
            java.io.File file = new java.io.File(imgPath);
            result = file.delete();
        }
        Log.i("--deleteImage--imgPath:" , imgPath + "--result:" + result);
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        Context context = this;
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == uploadMessage && null == uploadMessageAboveL) {
                return;
            }
            Uri result = data == null || resultCode != RESULT_OK ? null : data.getData();

            String con = "";
            // 壓縮到多少寬度以內
            int maxW = 800;
            // 壓縮到多少大小以內, 1024kb
            int maxSize = 1024;
            if (result == null) {
                // 看是否從相機返回
                File cameraFile = new File(mCameraFilePath);
                if (cameraFile.exists()) {
                    result = Uri.fromFile(cameraFile);
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, result));
                }
            }
            if (result != null) {
                Log.i("resultresult", String.valueOf(result));
                // 根據uri獲取路徑
                String path = FileUtils.getPath(this, result);
                Log.i("pathpath", path);
                if (!TextUtils.isEmpty(path)) {
                    if (path.contains("content://media/external/images/media")) {
                        String aa = getRealPathFromUri(mContext, Uri.parse(path));
                        con = aa;
                        Log.i("srcPath con1", aa);
                        dele = true;
                    } else {
                        con = path;
                        Log.i("srcPath con2", con);
                        dele = false;
                    }
                    File f = new File(con);
                    Log.i("srcPath", String.valueOf(f));
                    if (f.exists() && f.isFile()) {
                        // 按大小和尺寸壓縮圖片
                        Bitmap b = getCompressBitmap(con, maxW, maxW, maxSize);
                        String basePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                        String compressPath = basePath + File.separator + "photos" + File.separator + System.currentTimeMillis() + ".jpg";
                        // 壓縮完保存在文件裡
                        if (saveBitmapToFile(b, compressPath)) {
                            Uri newUri = null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                newUri = FileProvider.getUriForFile(MainActivity.this,
                                        BuildConfig.APPLICATION_ID + ".fileProvider",
                                        new File(compressPath));
                            } else {
                                newUri = Uri.fromFile(new File(compressPath));
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                if (uploadMessageAboveL != null) {
                                    if (newUri != null) {
                                        uploadMessageAboveL.onReceiveValue(new Uri[]{newUri});
                                        uploadMessageAboveL = null;
                                        Log.i("concon", con);
                                        if (dele) {
                                            deleteImage(con);
                                        }
                                        dele = false;
                                        return;
                                    }
                                }
                            } else if (uploadMessage != null) {
                                if (newUri != null) {
                                    uploadMessage.onReceiveValue(newUri);
                                    uploadMessage = null;
                                    Log.i("concon", con);
                                    if (dele) {
                                        deleteImage(con);
                                    }
                                    dele = false;
                                    return;
                                }
                            }
                        }
                    }
                }else {
                    Log.i("srcPath", "srcPathsrcPathsrcPath");
                }
            }
            clearUploadMessage();
            return;
        }
    }

    /**
     *  webview沒有選擇圖片也要傳null, 防止下次無法執行
     */
    private void clearUploadMessage() {
        if (uploadMessageAboveL != null) {
            uploadMessageAboveL.onReceiveValue(null);
            uploadMessageAboveL = null;
        }
        if (uploadMessage != null) {
            uploadMessage.onReceiveValue(null);
            uploadMessage = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        Log.i("VVVVVVV", "onResume: ");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((ViewGroup) webView.getParent()).removeView(webView);
        webView.setTag(null);

        //清除当前webview访问的历史记录
        //只会webview访问历史记录里的所有记录除了当前访问记录
        webView.clearHistory();
        //这个api仅仅清除自动完成填充的表单数据，并不会清除WebView存储到本地的数据
        webView.clearFormData();
        //清除网页访问留下的缓存
        //由于内核缓存是全局的因此这个方法不仅仅针对webview而是针对整个应用程序.
        webView.clearCache(true);
        clearCookies(this);
        webView.destroy();
        webView = null;
    }

    public void clearCookies(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(context);
            cookieSyncMngr.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.startSync();
            cookieSyncMngr.sync();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

}
