package io.github.rmannibucau.jrunning;

import org.androidannotations.annotations.sharedpreferences.DefaultFloat;
import org.androidannotations.annotations.sharedpreferences.DefaultInt;
import org.androidannotations.annotations.sharedpreferences.DefaultLong;
import org.androidannotations.annotations.sharedpreferences.DefaultString;
import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref(SharedPref.Scope.ACTIVITY_DEFAULT)
public interface Configuration {
    @DefaultString("http://server...")
    String url();

    @DefaultString("username...")
    String username();

    @DefaultString("password")
    String password();

    @DefaultInt(3)
    int retries();

    @DefaultLong(30)
    long minSeconds();

    @DefaultFloat(50)
    float minMeters();
}
