package com.techprd.webintent;

import java.util.HashMap;
import java.util.Map;

import org.apache.cordova.CordovaActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.PluginResult;
import com.eclipsesource.tabris.android.TabrisActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.util.Log;

import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WebIntent extends CordovaPlugin {
    private static final String LOG_TAG = "WebIntent";
    private static String installReferrer = null;
    private CallbackContext onNewIntentCallbackContext = null;
    private String mCustomTabsBrowser;
    private boolean mFindCalled = false;

    /**
     * The service we expect to find on a web browser that indicates it supports custom tabs.
     */
    private static final String ACTION_CUSTOM_TABS_CONNECTION =
            "android.support.customtabs.action.CustomTabsService";


    /**
     * @return true iff if the action was executed successfully, else false.
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        try {
            if ("startActivity".equals(action)) {
                if (args.length() != 1) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }
                // Parse the arguments
                final CordovaResourceApi resourceApi = webView.getResourceApi();
                JSONObject obj = args.getJSONObject(0);
                String type = obj.has("type") ? obj.getString("type") : null;
                Uri uri = obj.has("url") ? resourceApi.remapUri(Uri.parse(obj.getString("url"))) : null;
                JSONObject extras = obj.has("extras") ? obj.getJSONObject("extras") : null;
                Map<String, String> extrasMap = new HashMap<String, String>();

                // Populate the extras if any exist
                if (extras != null) {
                    JSONArray extraNames = extras.names();
                    for (int i = 0; i < extraNames.length(); i++) {
                        String key = extraNames.getString(i);
                        String value = extras.getString(key);
                        extrasMap.put(key, value);
                    }
                }

                startActivity(obj.getString("action"), uri, type, extrasMap, callbackContext);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
                return true;
            } else if ("hasExtra".equals(action)) {
                if (args.length() != 1) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }
                Intent i = ((TabrisActivity) this.cordova.getActivity()).getIntent();
                String extraName = args.getString(0);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, i.hasExtra(extraName)));
                return true;
            } else if ("getExtra".equals(action)) {
                if (args.length() != 1) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }
                Intent i = ((TabrisActivity) this.cordova.getActivity()).getIntent();
                String extraName = args.getString(0);
                if (i.hasExtra(extraName)) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, i.getStringExtra(extraName)));
                    return true;
                } else {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
                    return false;
                }
            } else if ("getUri".equals(action)) {
                if (args.length() != 0) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }
                Intent i = ((TabrisActivity) this.cordova.getActivity()).getIntent();
                String uri = i.getDataString();
                if (uri == null && installReferrer != null) {
                    uri = installReferrer;  // App just installed, received play store referrer intent.
                    Log.i(LOG_TAG, String.format("URI is an install referrer: %s", installReferrer));
                }
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, uri));
                return true;
            } else if ("onNewIntent".equals(action)) {
                // Save reference to the callback; will be called on "new intent" events.
                this.onNewIntentCallbackContext = callbackContext;

                if (args.length() != 0) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }

                PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
                result.setKeepCallback(true); // Reuse the callback on intent events.
                callbackContext.sendPluginResult(result);
                return true;
            } else if ("sendBroadcast".equals(action)) {
                if (args.length() != 1) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }

                // Parse the arguments
                JSONObject obj = args.getJSONObject(0);

                JSONObject extras = obj.has("extras") ? obj.getJSONObject("extras") : null;
                Map<String, String> extrasMap = new HashMap<String, String>();

                // Populate the extras if any exist
                if (extras != null) {
                    JSONArray extraNames = extras.names();
                    for (int i = 0; i < extraNames.length(); i++) {
                        String key = extraNames.getString(i);
                        String value = extras.getString(key);
                        extrasMap.put(key, value);
                    }
                }

                sendBroadcast(obj.getString("action"), extrasMap);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
                return true;
            }
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
            return false;
        } catch (JSONException e) {
            final String errorMessage = e.getMessage();
            Log.e(LOG_TAG, errorMessage);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, errorMessage));
            return false;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {

        if (this.onNewIntentCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, intent.getDataString());
            result.setKeepCallback(true);
            this.onNewIntentCallbackContext.sendPluginResult(result);
        }
    }

    void startActivity(String action, Uri uri, String type, Map<String, String> extras, CallbackContext callbackContext) {
        if (type != null && type.equals("CUSTOM_TAB")) {
            openCustomTab(uri, callbackContext);
        } else {
            startActivity(action, uri, type, extras);
        }
    }

    void startActivity(String action, Uri uri, String type, Map<String, String> extras) {

        Intent i = uri != null ? new Intent(action, uri) : new Intent(action);

        if (type != null && uri != null) {
            i.setDataAndType(uri, type); //Fix the crash problem with android 2.3.6
        } else {
            if (type != null) {
                i.setType(type);
            }
        }

        for (Map.Entry<String, String> entry : extras.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            // If type is text/html, the extra text must be sent as HTML.
            if (key.equals(Intent.EXTRA_TEXT) && "text/html".equals(type)) {
                i.putExtra(key, Html.fromHtml(value));
            } else if (key.equals(Intent.EXTRA_STREAM)) {
                // Allows sharing of images as attachments.
                // `value` in this case should be the URI of a file.
                final CordovaResourceApi resourceApi = webView.getResourceApi();
                i.putExtra(key, resourceApi.remapUri(Uri.parse(value)));
            } else if (key.equals(Intent.EXTRA_EMAIL)) {
                // Allows adding the email address of the receiver.
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{value});
            } else {
                i.putExtra(key, value);
            }
        }
        ((TabrisActivity) this.cordova.getActivity()).startActivity(i);
    }

    void sendBroadcast(String action, Map<String, String> extras) {
        Intent intent = new Intent();
        intent.setAction(action);
        for (Map.Entry<String, String> entry : extras.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }
        ((TabrisActivity) this.cordova.getActivity()).sendBroadcast(intent);
    }

    // Receiver that listens for com.android.vending.INSTALL_REFERRER, an intent sent by the
    // Play Store on installation when the referrer parameter of the install URL is populated:
    // https://play.google.com/store/apps/details?id=|APP_ID|&referrer=|REFERRER|
    public static class ReferralReceiver extends BroadcastReceiver {
        private static final String LOG_TAG = "ReferralReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            // Store the install referrer so we can return it when getUri is called.
            installReferrer = intent.getStringExtra("referrer");
            Log.i(LOG_TAG, String.format("Install referrer: %s", installReferrer));
        }
    }


    private void openCustomTab(Uri uri, CallbackContext callbackContext) {

        String customTabsBrowser = findCustomTabBrowser();
        if (customTabsBrowser == null) {
            Log.d(LOG_TAG, "openUrl: no in app browser tab available");
            callbackContext.error("no in app browser tab implementation available");
        }

        Intent customTabsIntent = new CustomTabsIntent.Builder().build().intent;
        customTabsIntent.setData(uri);
        customTabsIntent.setPackage(mCustomTabsBrowser);
        cordova.getActivity().startActivity(customTabsIntent);

        Log.d(LOG_TAG, "in app browser call dispatched");
        callbackContext.success();
    }

    private String findCustomTabBrowser() {
        if (mFindCalled) {
            return mCustomTabsBrowser;
        }

        PackageManager pm = cordova.getActivity().getPackageManager();
        Intent webIntent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://www.example.com"));
        List<ResolveInfo> resolvedActivityList =
                pm.queryIntentActivities(webIntent, PackageManager.GET_RESOLVED_FILTER);

        for (ResolveInfo info : resolvedActivityList) {
            if (!isFullBrowser(info)) {
                continue;
            }

            if (hasCustomTabWarmupService(pm, info.activityInfo.packageName)) {
                mCustomTabsBrowser = info.activityInfo.packageName;
                break;
            }
        }

        mFindCalled = true;
        return mCustomTabsBrowser;
    }

    private boolean isFullBrowser(ResolveInfo resolveInfo) {
        // The filter must match ACTION_VIEW, CATEGORY_BROWSEABLE, and at least one scheme,
        if (!resolveInfo.filter.hasAction(Intent.ACTION_VIEW)
                || !resolveInfo.filter.hasCategory(Intent.CATEGORY_BROWSABLE)
                || resolveInfo.filter.schemesIterator() == null) {
            return false;
        }

        // The filter must not be restricted to any particular set of authorities
        if (resolveInfo.filter.authoritiesIterator() != null) {
            return false;
        }

        // The filter must support both HTTP and HTTPS.
        boolean supportsHttp = false;
        boolean supportsHttps = false;
        Iterator<String> schemeIter = resolveInfo.filter.schemesIterator();
        while (schemeIter.hasNext()) {
            String scheme = schemeIter.next();
            supportsHttp |= "http".equals(scheme);
            supportsHttps |= "https".equals(scheme);

            if (supportsHttp && supportsHttps) {
                return true;
            }
        }

        // at least one of HTTP or HTTPS is not supported
        return false;
    }

    private boolean hasCustomTabWarmupService(PackageManager pm, String packageName) {
        Intent serviceIntent = new Intent();
        serviceIntent.setAction(ACTION_CUSTOM_TABS_CONNECTION);
        serviceIntent.setPackage(packageName);
        return (pm.resolveService(serviceIntent, 0) != null);
    }
}
