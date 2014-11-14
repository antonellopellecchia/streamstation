package com.magratheadesign.streamstation;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by ejntoo on 20/10/14.
 */
public class HelpActivity extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        WebView helpPageWebView = (WebView) findViewById(R.id.helpWebView);
        helpPageWebView.setWebViewClient(new WebViewClient());
        helpPageWebView.loadUrl("http://magratheadesign.co.nf/streamstation/manual");
        helpPageWebView.setVisibility(View.VISIBLE);
        findViewById(R.id.helpProgressBar).setVisibility(View.GONE);
    }

}
