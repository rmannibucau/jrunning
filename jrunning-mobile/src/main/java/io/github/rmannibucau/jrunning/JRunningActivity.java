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
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
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

    protected JRunningAgentClient client = new JRunningAgentClient(this);

    @Pref
    protected Configuration_ configuration;

    @AfterViews
    protected void init() {
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
        startSession();
    }

    @Click(R.id.stop)
    public void stop() {
        locationService.removeUpdates(this);
        detail.setText("Stop!");
        start.setEnabled(true);
        stop.setEnabled(false);

        final long time = System.nanoTime();
        session.updateLastLocation(locationService.getLastKnownLocation(LocationManager.GPS_PROVIDER), time);
        update("Stopped", time);
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
        final long time = System.nanoTime();
        session.updateLastLocation(location, time);
        update("Running", time);
    }

    private void update(String state, long time) {
        Location lastLocation = session.getLastLocation();
        if (lastLocation == null) {
            detail.setText(state);
        } else {
            detail.setText(
                    state + ". " +
                            "Run duration " + TimeUnit.NANOSECONDS.toMinutes(session.getDuration()) + "mn, " +
                            "Location = " + lastLocation.getLongitude() + "," + lastLocation.getLatitude() + ", " +
                            "Points = " + session.getPoints());
            post(lastLocation, time);
        }
    }

    @Background(serial = "running")
    protected void startSession() {
        final String sId = client.newSessionId();
        if (sId == null) {
            Toasts.bgText(JRunningActivity.this, "Server can't be contacted");
            return;
        }

        new Handler(Looper.getMainLooper())
                .post(new Runnable() {
                    @Override
                    public void run() {
                        locationService.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                TimeUnit.SECONDS.toMillis(configuration.minSeconds().get()),
                                configuration.minMeters().get(),
                                JRunningActivity.this);

                        start.setEnabled(false);
                        stop.setEnabled(true);

                        session.start(sId);
                        update("Running", session.getStartTime());
                    }
                });
    }

    @Background(serial = "running")
    protected void post(Location lastLocation, long time) {
        if (!client.checkPoint(lastLocation, time)) {
            Toasts.bgText(JRunningActivity.this, "Server can't be contacted");
        }
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
        // no-op: not that relevant, GpsStatus.Listener could be if we need
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
        Toast.makeText(this, "GPS lost!", Toast.LENGTH_LONG).show();
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

    public Configuration_ configuration() {
        return configuration;
    }

    public Session session() {
        return session;
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
