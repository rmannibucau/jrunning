package io.github.rmannibucau.jrunning;

import android.location.Location;
import android.util.Base64;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class JRunningAgentClient {
    private DefaultHttpClient client;
    private final JRunningActivity jRunningActivity;

    public JRunningAgentClient(final JRunningActivity jRunningActivity) {
        this.jRunningActivity = jRunningActivity;
    }

    public String newSessionId() {
        ensureClient();
        try {
            return EntityUtils.toString(client.execute(configureSecurity(new HttpGet(url() + "agent/start"))).getEntity(), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    private void ensureClient() {
        if (client == null) {
            final BasicHttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, jRunningActivity.configuration().timeout().get());
            HttpConnectionParams.setSoTimeout(params, jRunningActivity.configuration().timeout().get());
            client = new DefaultHttpClient(params);
            client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(jRunningActivity.configuration().retries().get(), true));
        }
    }

    private <T extends HttpRequestBase> T configureSecurity(T request) {
        request.setHeader("Authorization", "Basic " + Base64.encodeToString((jRunningActivity.configuration().username().get() + ":" + jRunningActivity.configuration().password().get()).getBytes(), Base64.NO_WRAP));
        return request;
    }

    private String url() {
        final String u = jRunningActivity.configuration().url().get();
        return u.endsWith("/") ? u : u + '/';
    }

    public boolean checkPoint(Location lastLocation, long time) {
        try {
            JSONObject object = new JSONObject();
            object.put("timestamp", time); // location.getTime() is not correct most of the time
            object.put("altitude", lastLocation.getAltitude());
            object.put("latitude", lastLocation.getLatitude());
            object.put("longitude", lastLocation.getLongitude());
            if (lastLocation.hasSpeed()) {
                object.put("speed", lastLocation.getSpeed());
            }

            HttpPost post = configureSecurity(new HttpPost(url() + "agent/point/" + jRunningActivity.session().getId()));
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(object.toString()));
            client.execute(post);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
