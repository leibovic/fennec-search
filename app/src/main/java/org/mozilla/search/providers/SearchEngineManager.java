/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.search.providers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.mozilla.search.SearchPreferenceActivity;

public class SearchEngineManager implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String LOG_TAG = "SearchEngineFactory";

    private final Context appContext;

    private SearchEngine currentEngine;
    private SearchEngineChangeListener changeListener;

    public static enum Engine {
        BING,
        GOOGLE,
        YAHOO
    }

    public SearchEngineManager(Context appContext, SearchEngineChangeListener changeListener) {
        this.appContext = appContext;
        this.changeListener = changeListener;

        // Fetch the user's default search engine from shared prefs. When this finishes, it will
        // alert changeListener of the default engine.
        fetchDefaultSearchEngine();

        // Register to be notified when the default engine changes in shared prefs.
        PreferenceManager.getDefaultSharedPreferences(appContext)
                .registerOnSharedPreferenceChangeListener(this);
    }

    private void fetchDefaultSearchEngine() {
        final AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
                return prefs.getString(SearchPreferenceActivity.PREF_SEARCH_ENGINE_KEY, null);
            }

            @Override
            protected void onPostExecute(String engineName) {
                if (engineName != null) {
                    setSearchEngine(engineName);
                }
            }
        };
        task.execute();
    }

    private void setSearchEngine(String engineName) {
        switch (Engine.valueOf(engineName)) {
            case BING:
                currentEngine = new Bing();
                break;
            case GOOGLE:
                currentEngine = new Google();
                break;
            case YAHOO:
                currentEngine = new Yahoo();
                break;
        }
        if (changeListener != null) {
            changeListener.onSearchEngineChange(currentEngine);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (TextUtils.equals(SearchPreferenceActivity.PREF_SEARCH_ENGINE_KEY, key)) {
            setSearchEngine(sharedPreferences.getString(key, null));
        }
    }

    public static interface SearchEngineChangeListener {
        public void onSearchEngineChange(SearchEngine engine);
    }
}
