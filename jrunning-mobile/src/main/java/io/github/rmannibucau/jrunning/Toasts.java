package io.github.rmannibucau.jrunning;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public final class Toasts {
    public static void bgText(final Context ctx, final String text) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ctx, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private Toasts() {
        // no-op
    }
}
