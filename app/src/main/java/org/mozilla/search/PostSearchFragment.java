/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.search;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.mozilla.gecko.AppConstants;
import org.mozilla.gecko.Telemetry;
import org.mozilla.gecko.TelemetryContract;
import org.mozilla.search.providers.SearchEngine;

public class PostSearchFragment extends Fragment {

    private static final String LOG_TAG = "PostSearchFragment";

    private ProgressBar progressBar;
    private WebView webview;

    private SearchEngine searchEngine;

    public PostSearchFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mainView = inflater.inflate(R.layout.search_fragment_post_search, container, false);

        progressBar = (ProgressBar) mainView.findViewById(R.id.progress_bar);

        webview = (WebView) mainView.findViewById(R.id.webview);
        webview.setWebChromeClient(new ChromeClient());
        webview.setWebViewClient(new LinkInterceptingClient());
        // This is required for our greasemonkey terror script.
        webview.getSettings().setJavaScriptEnabled(true);

        return mainView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        webview.setWebChromeClient(null);
        webview.setWebViewClient(null);
        webview = null;
        progressBar = null;
    }

    public void startSearch(String query) {
        if (searchEngine != null) {
            setUrl(searchEngine.resultsUriForQuery(query));
        } else {
            Log.e(LOG_TAG, "Unable to start search. Default search engine not initialized yet.");
        }

    }

    private void setUrl(String url) {
        // Only load URLs if they're different than what's already
        // loaded in the webview.
        if (!TextUtils.equals(webview.getUrl(), url)) {
            webview.loadUrl("about:blank");
            webview.loadUrl(url);
        }
    }

    public void setSearchEngine(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    /**
     * A custom WebViewClient that intercepts every page load. This allows
     * us to decide whether to load the url here, or send it to Android
     * as an intent.
     */
    private class LinkInterceptingClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (searchEngine.isExitUrl(url)) {
                Telemetry.sendUIEvent(TelemetryContract.Event.LOAD_URL,
                        TelemetryContract.Method.CONTENT, "search-result");
                view.stopLoading();
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setClassName(AppConstants.ANDROID_PACKAGE_NAME, AppConstants.BROWSER_INTENT_CLASS_NAME);
                i.setData(Uri.parse(url));
                startActivity(i);
            } else {
                super.onPageStarted(view, url, favicon);
            }
        }
    }

    /**
     * A custom WebChromeClient that allows us to inject CSS into
     * the head of the HTML and to monitor pageload progress.
     *
     * We use the WebChromeClient because it provides a hook to the titleReceived
     * event. Once the title is available, the page will have started parsing the
     * head element. The script injects its CSS into the head element.
     */
    private class ChromeClient extends WebChromeClient {

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            view.loadUrl(searchEngine.getInjectableJs());
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress < 100) {
                if (progressBar.getVisibility() == View.INVISIBLE) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                progressBar.setProgress(newProgress);
            } else {
                progressBar.setVisibility(View.INVISIBLE);
            }
        }
    }
}
