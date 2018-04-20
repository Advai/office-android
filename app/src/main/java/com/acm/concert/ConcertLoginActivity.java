package com.acm.concert;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A login screen that offers login via email/password.
 */
public class ConcertLoginActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private MaterialEditText usernameText;
    private MaterialEditText passwordText;
    private RelativeLayout mainLayout;
    private Button loginButton;
    private CheckBox rememberBox;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_concert_login);

        Window w = getWindow(); // in Activity's onCreate() for instance
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        usernameText = findViewById(R.id.userEdit);
        passwordText = findViewById(R.id.passwordEdit);
        mainLayout = findViewById(R.id.mainLayout);
        loginButton = findViewById(R.id.button);
        rememberBox = findViewById(R.id.rememberBox);

        int mainrgb = getIntent().getIntExtra("mainRGB", 0);
        int bodyText = getIntent().getIntExtra("bodyText", 0);
        int titleText = getIntent().getIntExtra("titleText", 0);

        mainLayout.setBackgroundColor(mainrgb);
        usernameText.setBaseColor(bodyText);
        usernameText.setPrimaryColor(bodyText);
        usernameText.setTextColor(bodyText);
        passwordText.setBaseColor(bodyText);
        passwordText.setPrimaryColor(bodyText);
        passwordText.setTextColor(bodyText);

        loginButton.setBackgroundColor(BlurBuilder.manipulateColor(mainrgb));
        loginButton.setTextColor(bodyText);

        loginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });
        rememberBox.setTextColor(bodyText);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .addApi(Auth.CREDENTIALS_API)
                .build();
    }

    private void login() {
        loginButton.setEnabled(false);
        usernameText.setEnabled(false);
        passwordText.setEnabled(false);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("username", usernameText.getText());
            jsonObject.put("password", passwordText.getText());
        } catch (JSONException e) {
            e.printStackTrace();
            onLoginFailed("Error creating request body.");
            return;
        }

        final CookieJar cookieJar = new CookieJar() {
            private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                cookieStore.put(url.host(), cookies);
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url.host());
                return cookies != null ? cookies : new ArrayList<Cookie>();
            }
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build();
        RequestBody body = RequestBody.create(JSON, jsonObject.toString());
        Request request = new Request.Builder()
                .addHeader("Content-Type", "application/json")
                .post(body)
                .url("https://concert.acm.illinois.edu/login")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onLoginFailed("Error sending login request");
                    }
                });

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
                if (response.code() == 400) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onLoginFailed("Invalid credentials " + response.code());
                        }
                    });
                    return;
                } else if (!response.isSuccessful()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onLoginFailed("Error with code " + response.code());
                        }
                    });

                    return;
                }
                List<Cookie> cookieList = cookieJar.loadForRequest(HttpUrl.parse("https://concert.acm.illinois.edu/login"));
                StringBuilder cookieString = new StringBuilder();
                for (Cookie c : cookieList) {
                    cookieString.append(c.toString()).append(";");
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loginButton.setEnabled(true);
                        usernameText.setEnabled(true);
                        passwordText.setEnabled(true);
                        Toast.makeText(getBaseContext(), "Login success!", Toast.LENGTH_LONG).show();
                    }
                });

                if (rememberBox.isChecked()) {
                    Credential credential = new Credential.Builder(String.valueOf(usernameText.getText()))
                            .setPassword(String.valueOf(passwordText.getText()))
                            .build();
                    requestSaveCredentials(credential);
                }

                Intent intent = getIntent();
                intent.putExtra("cookies", cookieString.toString());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void requestSaveCredentials(Credential credential) {
        Auth.CredentialsApi.save(googleApiClient, credential).setResultCallback(this);
    }

    private void onLoginFailed(String error) {
        Toast.makeText(getBaseContext(), "Login failed: " + error, Toast.LENGTH_LONG).show();

        loginButton.setEnabled(true);
        usernameText.setEnabled(true);
        passwordText.setEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onResult(@NonNull Status result) {
        Status status = result.getStatus();
        if (status.isSuccess()) {
            Toast.makeText(this, "Credentials saved successfully", Toast.LENGTH_LONG).show();
        } else {
            if (status.hasResolution()) {
                try {
                    status.startResolutionForResult(this, 1);
                } catch (IntentSender.SendIntentException e) {
                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Credentials saved successfully", Toast.LENGTH_LONG).show();
            } else {
                Log.d("CONCERT_LOGIN", "Save cancelled by user");
            }
        }
    }
}

