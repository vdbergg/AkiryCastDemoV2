package com.example.berg.akirycastdemo;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;

public class MainActivity extends AppCompatActivity {
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