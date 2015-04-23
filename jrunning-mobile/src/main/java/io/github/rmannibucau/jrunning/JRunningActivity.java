package io.github.rmannibucau.jrunning;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.api.sharedpreferences.StringPrefField;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

@EActivity(R.layout.activity_main)
public class JRunningActivity extends Activity implements LocationListener {
    @SystemService
    protected LocationManager locationService;

    @SystemService
    protected NotificationManager notificationManager;

    @ViewById
    protected TextView detail;

    @ViewById
    protected Button start;

    @ViewById
    protected Button stop;

    @ViewById
    protected EditText url;

    @ViewById
    protected EditText username;

    @ViewById
    protected EditText password;

    @Bean
    protected Session session;

    @Pref
    protected Configuration_ configuration;

    private DefaultHttpClient client;

    @AfterViews
    protected void init() {
        client = new DefaultHttpClient();
        client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(configuration.retries().get(), true));

        url.setOnEditorActionListener(new StorePreference(configuration.url()));
        username.setOnEditorActionListener(new StorePreference(configuration.username()));
        password.setOnEditorActionListener(new StorePreference(configuration.password()));
        url.setText(configuration.url().get());
        username.setText(configuration.username().get());
        password.setText(configuration.password().get());

        stop.setEnabled(false);

        notificationManager.notify(
                Notifications.ICON,
                new Notification.Builder(this)
                        .setSmallIcon(R.drawable.ic_jrun)
                        .setContentTitle("JRunning")
                        .setContentText("JRunning is running!")
                        .setContentIntent(PendingIntent.getActivity(this, Notifications.ICON, new Intent(this, JRunningActivity_.class), 0))
                        .build());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            notificationManager.cancel(Notifications.ICON);
        }
    }

    @Click(R.id.start)
    public void start() {
        ensureGpsIsActivated();

        String sId;
        try {
            sId = EntityUtils.toString(client.execute(configureSecurity(new HttpGet(url() + "agent/start"))).getEntity(), "UTF-8");
        } catch (Exception e) {
            Toast.makeText(this, "Server can't be contacted", Toast.LENGTH_LONG).show();
            return;
        }

        locationService.requestLocationUpdates(LocationManager.GPS_PROVIDER, TimeUnit.SECONDS.toMillis(configuration.minSeconds().get()), configuration.minMeters().get(), this);
        start.setEnabled(false);
        stop.setEnabled(true);

        session.start(sId);
        update("Running");
    }

    @Click(R.id.stop)
    public void stop() {
        locationService.removeUpdates(this);
        detail.setText("Stop!");
        start.setEnabled(true);
        stop.setEnabled(false);
        update("Stopped");
        session.finish();
    }

    @Click(R.id.quit)
    public void quit() {
        if (session.isRunning()) {
            stop();
        }
        notificationManager.cancel(Notifications.ICON);
        System.exit(0);
    }

    @Override
    public void onLocationChanged(Location location) {
        session.updateLastLocation(location);
        update("Running");
    }

    private void update(String state) {
        Location lastLocation = session.getLastLocation();
        if (lastLocation == null) {
            detail.setText(state);
        } else {
            detail.setText(
                    state + ". " +
                            "Run duration " + TimeUnit.MILLISECONDS.toMinutes(session.getDuration()) + "mn, " +
                            "Location = " + lastLocation.getLatitude() + "," + lastLocation.getLongitude() + ", " +
                            "Points = " + session.getPoints());
            post(lastLocation);
        }
    }

    @Background
    protected void post(Location lastLocation) {
        HttpPost post = configureSecurity(new HttpPost(url() + "agent/point/" + session.getId()));
        try {
            JSONObject object = new JSONObject();
            object.put("timestamp", lastLocation.getTime());
            object.put("altitude", lastLocation.getAltitude());
            object.put("latitude", lastLocation.getLatitude());
            object.put("longitude", lastLocation.getLongitude());
            if (lastLocation.hasSpeed()) {
                object.put("speed", lastLocation.getSpeed());
            }
            post.setEntity(new StringEntity(object.toString()));
            client.execute(post);
        } catch (Exception e) {
            Toast.makeText(this, "Server can't be contacted", Toast.LENGTH_LONG).show();
        }
    }

    private String url() {
        final String u = url.getText().toString();
        return u.endsWith("/") ? u : u + '/';
    }

    private <T extends HttpRequestBase> T configureSecurity(T request) {
        request.setHeader("Authorization", "Basic " + Base64.encodeToString((username.getText() + ":" + password.getText()).getBytes(), Base64.NO_WRAP));
        return request;
    }

    @OnActivityResult(Activities.GPS_SETTINGS)
    protected void checkGpsOrQuit() {
        if (!locationService.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("No GPS")
                    .setMessage("No GPS available, you didn't activate it so quit the application.")
                    .setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if (!LocationManager.GPS_PROVIDER.equals(provider)) {
            return;
        }
        Toast.makeText(this, "GPS lost: " + status + "(0=OUT_OF_SERVICE, 1=TEMPORARILY_UNAVAILABLE, 2=AVAILABLE)", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (!LocationManager.GPS_PROVIDER.equals(provider)) {
            return;
        }
        Toast.makeText(this, "GPS bask!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (!LocationManager.GPS_PROVIDER.equals(provider)) {
            return;
        }
        Toast.makeText(this, "GPS loast!", Toast.LENGTH_LONG).show();
    }

    private void ensureGpsIsActivated() {
        if (!locationService.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("Activate GPS please")
                    .setMessage("No GPS available, please activate it!")
                    .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), Activities.GPS_SETTINGS);
                        }
                    })
                    .setNegativeButton("Back", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            checkGpsOrQuit();
                        }
                    })
                    .show();
        }
    }

    private interface Activities {
        int GPS_SETTINGS = 1;
    }

    private interface Notifications {
        int ICON = 1;
    }

    private static class StorePreference implements TextView.OnEditorActionListener {
        private final StringPrefField key;

        public StorePreference(final StringPrefField key) {
            this.key = key;
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                key.put(v.getText().toString());
                return true;
            }
            return false;
        }
    }
}
