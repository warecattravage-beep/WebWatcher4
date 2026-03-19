package com.webwatcher.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private static final String TARGET_URL = "https://06c7986a-71b1-420c-b544-33e265b64739-00-17v9tmtd8zwc7.janeway.replit.dev/";
    private static final int NOTIF_PERMISSION_CODE = 1001;

    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private NotificationHelper notificationHelper;

    // JS: MutationObserver로 새 메시지/알림 실시간 감지
    private static final String JS_OBSERVER =
        "(function() {" +
        "  if (window._watcherInjected) return;" +
        "  window._watcherInjected = true;" +
        "  var lastMsgCount = -1;" +
        "  var lastBadgeVal = -1;" +
        "  function countMessages() {" +
        "    var total = 0;" +
        // 메시지/채팅/알림 관련 요소 탐색
        "    var selectors = [" +
        "      '[class*=\"message\"]'," +
        "      '[class*=\"msg\"]'," +
        "      '[class*=\"chat\"]'," +
        "      '[class*=\"notification\"]'," +
        "      '[class*=\"notif\"]'" +
        "    ];" +
        "    var els = document.querySelectorAll(selectors.join(','));" +
        "    return els.length;" +
        "  }" +
        "  function countBadges() {" +
        "    var total = 0;" +
        "    var badges = document.querySelectorAll('[class*=\"badge\"],[class*=\"unread\"],[class*=\"count\"],[class*=\"bubble\"]');" +
        "    badges.forEach(function(el) {" +
        "      var n = parseInt(el.textContent.trim(), 10);" +
        "      if (!isNaN(n)) total += n;" +
        "    });" +
        "    return total;" +
        "  }" +
        "  function check() {" +
        "    var msgs = countMessages();" +
        "    var badges = countBadges();" +
        "    if (lastMsgCount >= 0 && msgs > lastMsgCount) {" +
        "      Android.onNewMessage();" +
        "    }" +
        "    if (lastBadgeVal >= 0 && badges > lastBadgeVal) {" +
        "      Android.onNewMessage();" +
        "    }" +
        "    lastMsgCount = msgs;" +
        "    lastBadgeVal = badges;" +
        "  }" +
        "  var observer = new MutationObserver(function() { check(); });" +
        "  observer.observe(document.body, {childList:true, subtree:true, characterData:true});" +
        "  setInterval(check, 2000);" +
        "  check();" +
        "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationHelper = new NotificationHelper(this);
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        setupWebView();
        requestNotificationPermission();

        swipeRefresh.setOnRefreshListener(() -> {
            webView.reload();
            swipeRefresh.setRefreshing(false);
        });
        swipeRefresh.setColorSchemeResources(android.R.color.holo_blue_bright);
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setTextZoom(100);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // JS → Android 브릿지
        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (!url.contains("janeway.replit.dev")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(android.view.View.GONE);
                view.evaluateJavascript(JS_OBSERVER, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setVisibility(android.view.View.VISIBLE);
                progressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    progressBar.setVisibility(android.view.View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (getSupportActionBar() != null && title != null && !title.isEmpty()) {
                    getSupportActionBar().setSubtitle(title);
                }
            }
        });

        webView.loadUrl(TARGET_URL);
    }

    private class WebAppInterface {
        @JavascriptInterface
        public void onNewMessage() {
            runOnUiThread(() -> notificationHelper.sendMessageNotification());
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIF_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIF_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "알림 권한을 허용해야 새 메시지 알림을 받을 수 있습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_refresh) {
            webView.reload();
            return true;
        } else if (id == R.id.menu_open_browser) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(TARGET_URL)));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        webView.evaluateJavascript(JS_OBSERVER, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.destroy();
    }
}
