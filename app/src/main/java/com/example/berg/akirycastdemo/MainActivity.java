package com.example.berg.akirycastdemo;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private CastDevice selectedDevice;
    private GoogleApiClient apiClient;
    private boolean applicationStarted;

    private void setSelectedDevice(CastDevice device)
    {
        Log.d(TAG, "setSelectedDevice: " + device);

        selectedDevice = device;

        if (selectedDevice != null)
        {
            try
            {
                stopApplication();
                disconnectApiClient();
                connectApiClient();
            }
            catch (IllegalStateException e)
            {
                Log.w(TAG, "Exception while connecting API client", e);
                disconnectApiClient();
            }
        }
        else
        {
            if (apiClient != null)
            {
                disconnectApiClient();
            }

            mediaRouter.selectRoute(mediaRouter.getDefaultRoute());
        }
    }

    private void connectApiClient()
    {
        Cast.CastOptions apiOptions = Cast.CastOptions.builder(selectedDevice, castClientListener).build();
        apiClient = new GoogleApiClient.Builder(this)
                .addApi(Cast.API, apiOptions)
                .addConnectionCallbacks(connectionCallback)
                .addOnConnectionFailedListener(connectionFailedListener)
                .build();
        apiClient.connect();
    }

    private void disconnectApiClient()
    {
        if (apiClient != null)
        {
            apiClient.disconnect();
            apiClient = null;
        }
    }

    private void stopApplication()
    {
        if (apiClient == null) return;

        if (applicationStarted)
        {
            Cast.CastApi.stopApplication(apiClient);
            applicationStarted = false;
        }
    }

    private final Cast.Listener castClientListener = new Cast.Listener()
    {
        @Override
        public void onApplicationDisconnected(int statusCode)
        {
        }

        @Override
        public void onVolumeChanged()
        {
        }
    };

    private final GoogleApiClient.ConnectionCallbacks connectionCallback = new GoogleApiClient.ConnectionCallbacks()
    {
        @Override
        public void onConnected(Bundle bundle)
        {
            try
            {
                Cast.CastApi.launchApplication(apiClient, CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID, false).setResultCallback(connectionResultCallback);
            }
            catch (Exception e)
            {
                Log.e(TAG, "Failed to launch application", e);
            }
        }

        @Override
        public void onConnectionSuspended(int i)
        {
        }
    };

    private final GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener()
    {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult)
        {
            setSelectedDevice(null);
        }
    };

    private final ResultCallback connectionResultCallback = new ResultCallback()
    {
        @Override
        public void onResult(Result result)
        {
            Status status = result.getStatus();
            if (status.isSuccess())
            {
                applicationStarted = true;

                try
                {
                    Cast.CastApi.setMessageReceivedCallbacks(apiClient, "Teste", incomingMsgHandler);
                }
                catch (IOException e)
                {
                    Log.e(TAG, "Exception while creating channel", e);
                }
            }
        }
    };

    private void sendMessage(String message)
    {
        if (apiClient != null)
        {
            try
            {
                Cast.CastApi.sendMessage(apiClient, "Testando", message)
                        .setResultCallback(new ResultCallback<Status>()
                        {
                            @Override
                            public void onResult(Status result)
                            {
                                if (!result.isSuccess())
                                {
                                    Log.e(TAG, "Sending message failed");
                                }
                            }
                        });
            }
            catch (Exception e)
            {
                Log.e(TAG, "Exception while sending message", e);
            }
        }
    }

    public final Cast.MessageReceivedCallback incomingMsgHandler = new Cast.MessageReceivedCallback()
    {
        @Override
        public void onMessageReceived(CastDevice castDevice, String namespace, String message)
        {
        }
    };

    private final MediaRouter.Callback
    mediaRouterCallback = new MediaRouter.Callback() {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            CastDevice device = CastDevice.getFromBundle(route.getExtras());
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            super.onRouteUnselected(router, route);
        }
    };

    private MediaRouter mediaRouter;
    private MediaRouteSelector mediaRouteSelector;

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

        mediaRouter = MediaRouter.getInstance(getApplicationContext());
        mediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID).build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem menuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat.getActionProvider(menuItem);

        mediaRouteActionProvider.setRouteSelector(mediaRouteSelector);

        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mediaRouter == null) {
            mediaRouter = MediaRouter.getInstance(getApplicationContext());
            mediaRouteSelector = new MediaRouteSelector.Builder().addControlCategory(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID).build();
        }
        mediaRouter.addCallback(mediaRouteSelector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    @Override
    protected void onStop() {
        mediaRouter.removeCallback(mediaRouterCallback);
        super.onStop();
    }
}