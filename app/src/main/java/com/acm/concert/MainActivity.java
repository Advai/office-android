package com.acm.concert;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gelitenight.waveview.library.WaveView;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.makeramen.roundedimageview.RoundedImageView;
import com.nvanbenschoten.motion.ParallaxImageView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.acm.concert.ConcertLoginActivity.JSON;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private RoundedImageView albumArtView;
    private ParallaxImageView backgroundAlbumArt;
    private ProgressBar songPosition;
    private TextView songTitle;
    private TextView currentTime;
    private TextView durationTime;
    private Button playPauseButton;
    private Button nextButton;
    private Button queueButton;
    private Button loginButton;
    private WaveView waveView;
    private DiscreteSeekBar volumeBar;
    private ConcertStatus currStatus;
    private Palette.Swatch previousSwatch;

    private final int LOGIN_REQUEST_CODE = 3;
    private String cookies = "";

    private GoogleApiClient mGoogleApiClient;
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("https://concert.acm.illinois.edu/");
        } catch (URISyntaxException e) {
            Log.e("Socket Error", e.getMessage());
        }
    }

     private final Emitter.Listener connected_callback = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            final ConcertStatus status;
            if (args.length > 1) {
                status = new ConcertStatus((String) args[1]);
            } else {
                status = new ConcertStatus((String) args[0]);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateUI(status);
                }
            });
        }
    };

    private final Emitter.Listener volume_callback = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            try {
                JSONObject jsonObject = new JSONObject((String) args[0]);
                currStatus.setVolume(jsonObject.getInt("volume"));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        volumeBar.setProgress(currStatus.getVolume());
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private final Emitter.Listener pause_callback = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
        for (Object arg : args) {
            Log.e("PAUSE", (String) arg);
        }
        try {
            JSONObject jsonObject = new JSONObject((String) args[0]);
            currStatus.setAudioStatus(jsonObject.getString("audio_status"));
            currStatus.setCurrentTime(jsonObject.getInt("current_time"));
            currStatus.setDuration(jsonObject.getInt("duration"));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    songPosition.setProgress(currStatus.getCurrentTime());
                    if (currStatus.getAudioStatus() == ConcertStatus.playerStatus.PAUSED) {
                        final Drawable[] mDrawable = new Drawable[1];
                        mDrawable[0] = getResources().getDrawable(R.mipmap.ic_play_arrow_black_24dp);
                        mDrawable[0].setColorFilter(new PorterDuffColorFilter(previousSwatch.getBodyTextColor(), PorterDuff.Mode.SRC_IN));
                        playPauseButton.setBackground(mDrawable[0]);
                        ObjectAnimator amplitudeAnim = ObjectAnimator.ofFloat(waveView, "amplitudeRatio", waveView.getAmplitudeRatio(), 0.00001f);
                        amplitudeAnim.setDuration(1000);
                        amplitudeAnim.setInterpolator(new FastOutLinearInInterpolator());
                        amplitudeAnim.start();

                    } else {
                        final Drawable[] mDrawable = new Drawable[1];
                        mDrawable[0] = getResources().getDrawable(R.mipmap.ic_pause_black_24dp);
                        mDrawable[0].setColorFilter(new PorterDuffColorFilter((Integer) previousSwatch.getBodyTextColor(), PorterDuff.Mode.SRC_IN));
                        playPauseButton.setBackground(mDrawable[0]);

                        ObjectAnimator amplitudeAnim = ObjectAnimator.ofFloat(waveView, "amplitudeRatio", waveView.getAmplitudeRatio(), 0.05f);
                        amplitudeAnim.setDuration(1000);
                        amplitudeAnim.setInterpolator(new FastOutLinearInInterpolator());
                        amplitudeAnim.start();

                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
        }
    };

    private void animateElements(Palette.Swatch currentSwatch, final Palette.Swatch swatch, final  Palette.Swatch secondarySwatch) {
        Log.e("ANIM", "Starting Animation");
        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        ValueAnimator mainColorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), currentSwatch.getRgb(), swatch.getRgb());
        ValueAnimator bodyTextAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), currentSwatch.getBodyTextColor(), swatch.getBodyTextColor());
        ValueAnimator titleTextAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), currentSwatch.getTitleTextColor(), swatch.getTitleTextColor());


        mainColorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(final ValueAnimator valueAnimator) {
                waveView.setWaveColor((((Integer)secondarySwatch.getRgb() & 0x00FFFFFF) | 0x44000000), (((Integer)valueAnimator.getAnimatedValue() & 0x00FFFFFF) | 0x44000000));

            }
        });

        final Drawable[] mDrawable = new Drawable[3];
        if (currStatus.getIsPlaying()) {
            mDrawable[0] = getResources().getDrawable(R.mipmap.ic_pause_black_24dp);
        } else {
            mDrawable[0] = getResources().getDrawable(R.mipmap.ic_play_arrow_black_24dp);
        }
        mDrawable[1] = getResources().getDrawable(R.mipmap.ic_queue_music_black_24dp);
        mDrawable[2] = getResources().getDrawable(R.mipmap.ic_skip_next_black_24dp);
        bodyTextAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                songTitle.setTextColor((Integer) valueAnimator.getAnimatedValue());
                currentTime.setTextColor((Integer) valueAnimator.getAnimatedValue());
                durationTime.setTextColor((Integer) valueAnimator.getAnimatedValue());
                songPosition.getProgressDrawable().setColorFilter((Integer) valueAnimator.getAnimatedValue(), PorterDuff.Mode.SRC_IN);
                volumeBar.setTrackColor((Integer)valueAnimator.getAnimatedValue());
                volumeBar.setScrubberColor((Integer)valueAnimator.getAnimatedValue());
                mDrawable[0].setColorFilter(new PorterDuffColorFilter((Integer) valueAnimator.getAnimatedValue(), PorterDuff.Mode.SRC_IN));
                mDrawable[1].setColorFilter(new PorterDuffColorFilter((Integer) valueAnimator.getAnimatedValue(), PorterDuff.Mode.SRC_IN));
                mDrawable[2].setColorFilter(new PorterDuffColorFilter((Integer) valueAnimator.getAnimatedValue(), PorterDuff.Mode.SRC_IN));
                playPauseButton.setBackground(mDrawable[0]);
                queueButton.setBackground(mDrawable[1]);
                nextButton.setBackground(mDrawable[2]);
                loginButton.setTextColor((Integer) valueAnimator.getAnimatedValue());
                volumeBar.setThumbColor((Integer)valueAnimator.getAnimatedValue(), (Integer)secondarySwatch.getRgb());
                volumeBar.setRippleColor((Integer)valueAnimator.getAnimatedValue());
            }
        });
        mainColorAnimation.setDuration(1000);
        titleTextAnimation.setDuration(1000);
        bodyTextAnimation.setDuration(1000);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(mainColorAnimation).with(titleTextAnimation).with(bodyTextAnimation);
        animatorSet.start();

        previousSwatch = swatch;
        Log.e("ANIM", "Done Animation");
    }

    private void updateUI(ConcertStatus status) {
        volumeBar.setProgress(status.getVolume());
        if (status.getAudioStatus() != ConcertStatus.playerStatus.NOTHINGSPECIAL) {
            songPosition.setProgress(status.getCurrentTime());

            long minutes = (status.getCurrentTime() / 1000)  / 60;
            int seconds = (status.getCurrentTime() / 1000) % 60;
            currentTime.setText(String.format(Locale.ENGLISH, "%d:%02d", minutes, seconds));

            minutes = (status.getDuration() / 1000)  / 60;
            seconds = (status.getDuration() / 1000) % 60;
            durationTime.setText(String.format(Locale.ENGLISH,"%d:%02d", minutes, seconds));

            final Drawable[] mDrawable = new Drawable[1];
            if(currStatus.getAudioStatus() == ConcertStatus.playerStatus.PLAYING) {
                ObjectAnimator amplitudeAnim = ObjectAnimator.ofFloat(waveView, "amplitudeRatio", waveView.getAmplitudeRatio(), 0.05f);
                amplitudeAnim.setDuration(1000);
                amplitudeAnim.setInterpolator(new FastOutLinearInInterpolator());
                amplitudeAnim.start();
                mDrawable[0] = getResources().getDrawable(R.mipmap.ic_pause_black_24dp);
                mDrawable[0].setColorFilter(new PorterDuffColorFilter((Integer) previousSwatch.getBodyTextColor(), PorterDuff.Mode.SRC_IN));
                playPauseButton.setBackground(mDrawable[0]);
            } else {
                ObjectAnimator amplitudeAnim = ObjectAnimator.ofFloat(waveView, "amplitudeRatio", waveView.getAmplitudeRatio(), 0.00001f);
                amplitudeAnim.setDuration(1000);
                amplitudeAnim.setInterpolator(new FastOutLinearInInterpolator());
                amplitudeAnim.start();

                mDrawable[0] = getResources().getDrawable(R.mipmap.ic_play_arrow_black_24dp);
                mDrawable[0].setColorFilter(new PorterDuffColorFilter(previousSwatch.getBodyTextColor(), PorterDuff.Mode.SRC_IN));
                playPauseButton.setBackground(mDrawable[0]);
            }

            // Check if we should download new thumb (song change)
            if (!status.getCurrentTrack().equals(currStatus.getCurrentTrack())) {
                songPosition.setMax(status.getDuration());
                songTitle.setText(status.getCurrentTrack());
                Picasso.get().load("https://concert.acm.illinois.edu/" + status.getThumbnail()).into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        albumArtView.setImageBitmap(bitmap);
                        backgroundAlbumArt.setImageBitmap(BlurBuilder.blur(MainActivity.this, bitmap));
                        generateSwatchAndAnimate(bitmap);
                        String fileName = "myImage";
                        try {
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                            FileOutputStream fo = openFileOutput(fileName, Context.MODE_PRIVATE);
                            fo.write(bytes.toByteArray());
                            // remember close file output
                            fo.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                        albumArtView.setImageResource(R.mipmap.acm);
                        backgroundAlbumArt.setImageResource(R.mipmap.acm);
                        generateSwatchAndAnimate(BitmapFactory.decodeResource(getResources(),R.mipmap.acm));
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });
            }
            currStatus = status;
        } else {
            Log.e("UI UP", "Default UI");
            // Reset the UI to default
            albumArtView.setImageResource(R.mipmap.acm);
            backgroundAlbumArt.setImageResource(R.mipmap.acm);
            songPosition.setProgress(0);
            songTitle.setText(R.string.no_play);
            playPauseButton.setBackgroundResource(R.mipmap.ic_play_arrow_black_24dp);
            currStatus = status;
            generateSwatchAndAnimate(BitmapFactory.decodeResource(getResources(),R.mipmap.acm));

            ObjectAnimator amplitudeAnim = ObjectAnimator.ofFloat(waveView, "amplitudeRatio", waveView.getAmplitudeRatio(), 0.00001f);
            amplitudeAnim.setDuration(1000);
            amplitudeAnim.setInterpolator(new FastOutLinearInInterpolator());
            amplitudeAnim.start();

            currentTime.setText(R.string.left_time);
            durationTime.setText(R.string.right_time);
        }

        Log.e("UI UP", "Done UI Update");
    }

    private void generateSwatchAndAnimate(Bitmap bitmap) {
        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            public void onGenerated(Palette p) {
                Palette.Swatch secondarySwatch = p.getDarkVibrantSwatch();
                if (secondarySwatch == null) {
                    secondarySwatch = p.getLightVibrantSwatch();
                    if (secondarySwatch == null) {
                        secondarySwatch = p.getDarkVibrantSwatch();
                        if (secondarySwatch == null) {
                            secondarySwatch = p.getDominantSwatch();
                            if (secondarySwatch == null) {
                                secondarySwatch = p.getMutedSwatch();
                            }
                        }
                    }
                }

                Palette.Swatch swatch = p.getLightVibrantSwatch();
                if (swatch == null) {
                    swatch = p.getLightVibrantSwatch();
                    if (swatch == null) {
                        swatch = p.getDarkVibrantSwatch();
                        if (swatch == null) {
                            swatch = p.getDominantSwatch();
                            if (swatch == null) {
                                swatch = p.getLightMutedSwatch();
                            }
                        }
                    }
                }

                animateElements(previousSwatch , secondarySwatch, swatch);
            }
        });
    }

    private void initWave(WaveView waveView) {
        waveView.setWaterLevelRatio(.8f);
        waveView.setAmplitudeRatio(.0001f);
        waveView.setShapeType(WaveView.ShapeType.SQUARE);


        ObjectAnimator waveShiftAnim = ObjectAnimator.ofFloat(
                waveView, "waveShiftRatio", 1f, 0f);
        waveShiftAnim.setRepeatCount(ValueAnimator.INFINITE);
        waveShiftAnim.setDuration(6000);
        waveShiftAnim.setInterpolator(new LinearInterpolator());
        waveShiftAnim.start();

        waveView.setShowWave(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (!cookies.equals("")) {
            return;
        }
        CredentialRequest request = new CredentialRequest.Builder()
                .setSupportsPasswordLogin(true)
                .build();
        Auth.CredentialsApi.request(mGoogleApiClient, request).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(CredentialRequestResult credentialRequestResult) {
                        Status status = credentialRequestResult.getStatus();
                        if (credentialRequestResult.getStatus().isSuccess()) {
                            // Successfully read the credential without any user interaction, this
                            // means there was only a single credential and the user has auto
                            // sign-in enabled.
                            Log.d("AUTO_LOGIN", "Sign in done!");
                            final Credential credential = credentialRequestResult.getCredential();

                            JSONObject jsonObject = new JSONObject();
                            try {
                                jsonObject.put("username", credential.getId());
                                jsonObject.put("password", credential.getPassword());
                            } catch (JSONException e) {
                                e.printStackTrace();
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

                                }

                                @Override
                                public void onResponse(@NonNull Call call, @NonNull final Response response) throws IOException {
                                    if (response.code() == 400) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(getBaseContext(), "Login failed: Invalid Username/Password", Toast.LENGTH_LONG).show();
                                            }
                                        });
                                        Auth.CredentialsApi.delete(mGoogleApiClient, credential).setResultCallback(new ResultCallback<Status>() {
                                            @Override
                                            public void onResult(Status status) {
                                                if (status.isSuccess()) {
                                                    Log.d("AUTO_LOGIN", "Credential successfully deleted.");
                                                } else {
                                                    // This may be due to the credential not existing, possibly
                                                    // already deleted via another device/app.
                                                    Log.d("AUTO_LOGIN", "Credential not deleted successfully.");
                                                }
                                            }
                                        });
                                        return;
                                    } else if (!response.isSuccessful()) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(getBaseContext(), "Login failed: Server Error", Toast.LENGTH_LONG).show();
                                            }
                                        });
                                        return;
                                    }
                                    List<Cookie> cookieList = cookieJar.loadForRequest(HttpUrl.parse("https://concert.acm.illinois.edu/login"));
                                    StringBuilder cookieString = new StringBuilder();
                                    for (Cookie c : cookieList) {
                                        cookieString.append(c.toString()).append(";");
                                    }
                                    cookies = String.valueOf(cookieString);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            goodLogin();
                                        }
                                    });
                                }
                            });
                        } else if (status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
                            // This is most likely the case where the user does not currently
                            // have any saved credentials and thus needs to provide a username
                            // and password to sign in.
                            Log.d("AUTO_LOGIN", "Sign in required");
                        } else {
                            Log.w("AUTO_LOGIN", "Unrecognized status code: " + status.getStatusCode());
                        }
                    }
                }
        );
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public class DownloadThumbTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... strings) {
            Log.e("DOWNLOAD", "Getting new image");
            final String url = "https://concert.acm.illinois.edu/" + strings[0];
            Bitmap bitmap = null;
            try {
                final InputStream inputStream = new URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            Log.e("DOWNLOAD", "Done new image");
            super.onPostExecute(bitmap);
            if (bitmap == null) {
                albumArtView.setImageResource(R.mipmap.acm);
                backgroundAlbumArt.setImageResource(R.mipmap.acm);
                generateSwatchAndAnimate(BitmapFactory.decodeResource(getResources(),R.mipmap.acm));
            } else {
                String fileName = "myImage";
                try {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                    FileOutputStream fo = openFileOutput(fileName, Context.MODE_PRIVATE);
                    fo.write(bytes.toByteArray());
                    // remember close file output
                    fo.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    fileName = null;
                }
                backgroundAlbumArt.setImageBitmap(BlurBuilder.blur(MainActivity.this, bitmap));
                albumArtView.setImageBitmap(bitmap);
                generateSwatchAndAnimate(bitmap);
            }
        }
    }

    class UpdateProgressTask extends TimerTask {
        @Override
        public void run() {
            if (currStatus.getAudioStatus() == ConcertStatus.playerStatus.OPENING || currStatus.getAudioStatus() == ConcertStatus.playerStatus.PLAYING) {
                currStatus.setCurrentTime(currStatus.getCurrentTime() + 1000);
                songPosition.setProgress(currStatus.getCurrentTime());
                final long minutes = (currStatus.getCurrentTime() / 1000)  / 60;
                final int seconds = (currStatus.getCurrentTime() / 1000) % 60;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentTime.setText(String.format(Locale.ENGLISH, "%d:%02d", minutes, seconds));
                    }
                });
            }
        }
    }

    private void goodLogin() {
        mSocket.disconnect();
        mSocket.io().on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Transport transport = (Transport)args[0];

                transport.on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>)args[0];
                        headers.put("Cookie", Collections.singletonList(cookies));
                    }
                });
            }
        });
        mSocket.connect();
        loginButton.setEnabled(false);
        loginButton.setVisibility(View.INVISIBLE);
        volumeBar.setVisibility(View.VISIBLE);
        playPauseButton.setVisibility(View.VISIBLE);
        queueButton.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.VISIBLE);
        volumeBar.setEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOGIN_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                cookies = data.getStringExtra("cookies");
                goodLogin();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        albumArtView = findViewById(R.id.albumArt);
        songPosition = findViewById(R.id.songPosition);
        songTitle = findViewById(R.id.songTtileView);
        backgroundAlbumArt = findViewById(R.id.backgroundViewArt);
        playPauseButton = findViewById(R.id.playButton);
        waveView = findViewById(R.id.wave);
        volumeBar = findViewById(R.id.volumeSeek);
        queueButton = findViewById(R.id.queueButton);
        nextButton = findViewById(R.id.nextButton);
        currentTime = findViewById(R.id.currentTimeText);
        durationTime = findViewById(R.id.durationTimeText);
        loginButton = findViewById(R.id.loginButton);
        volumeBar.setEnabled(false);
        songTitle.setSelected(true);
        backgroundAlbumArt.registerSensorManager();
        backgroundAlbumArt.setParallaxIntensity(1.075f);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, 0, this)
                .addApi(Auth.CREDENTIALS_API)
                .build();

        Window w = getWindow(); // in Activity's onCreate() for instance
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        volumeBar.setVisibility(View.INVISIBLE);
        playPauseButton.setVisibility(View.INVISIBLE);
        queueButton.setVisibility(View.INVISIBLE);
        nextButton.setVisibility(View.INVISIBLE);

        initWave(waveView);

        currStatus = new ConcertStatus();
        Palette.from(BitmapFactory.decodeResource(getResources(),R.mipmap.acm)).generate(new Palette.PaletteAsyncListener() {
            public void onGenerated(Palette p) {
                Palette.Swatch swatch = p.getVibrantSwatch();
                if (swatch == null) {
                    swatch = p.getLightVibrantSwatch();
                    if (swatch == null) {
                        swatch = p.getDarkVibrantSwatch();
                        if (swatch == null) {
                            swatch = p.getDominantSwatch();
                            if (swatch == null) {
                                swatch = p.getMutedSwatch();
                            }
                        }
                    }
                }
                previousSwatch = swatch;
            }
        });

        try {
            mSocket = IO.socket("https://concert.acm.illinois.edu/");
        } catch (URISyntaxException e) {
            Log.e("Socket Error", e.getMessage());
        }


        // Setup Listeners
        mSocket.on("connected", connected_callback);
        mSocket.on("heartbeat", connected_callback);
        mSocket.on("played", connected_callback);

        mSocket.on("volume_changed", volume_callback );
        mSocket.on("paused", pause_callback);

        // Setup socket
        mSocket.connect();
        Timer progressTimer = new Timer(true);
        TimerTask task = new UpdateProgressTask();
        progressTimer.scheduleAtFixedRate(task, 0, 1000);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent loginIntent = new Intent(MainActivity.this, ConcertLoginActivity.class);
                loginIntent.putExtra("mainRGB", previousSwatch.getRgb());
                loginIntent.putExtra("bodyText", previousSwatch.getBodyTextColor());
                loginIntent.putExtra("titleText", previousSwatch.getTitleTextColor());
                startActivityForResult(loginIntent, LOGIN_REQUEST_CODE);
            }
        });

        queueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent queueIntent = new Intent(MainActivity.this, QueueActivity.class);
                queueIntent.putExtra("mainRGB", previousSwatch.getRgb());
                queueIntent.putExtra("bodyText", previousSwatch.getBodyTextColor());
                queueIntent.putExtra("titleText", previousSwatch.getTitleTextColor());
                queueIntent.putExtra("cookie", cookies);
                queueIntent.putExtra("thumbnail", "https://concert.acm.illinois.edu/" + currStatus.getThumbnail());
                queueIntent.putExtra("currentlyPlaying", currStatus.getCurrentTrack());

                View sharedView = findViewById(R.id.albumArt);
                ActivityOptionsCompat activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this, sharedView, "albumArtTransition");

                startActivity(queueIntent, activityOptions.toBundle());
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSocket.emit("pause");
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSocket.emit("skip");
            }
        });
        volumeBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                mSocket.emit("volume", value);
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        backgroundAlbumArt.unregisterSensorManager();
    }

    @Override
    protected void onResume() {
        super.onResume();
        backgroundAlbumArt.registerSensorManager();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    int vol = currStatus.getVolume() + 5;
                    if (vol > 100) {
                        vol = 100;
                    }
                    currStatus.setVolume(vol);
                    mSocket.emit("volume", currStatus.getVolume());
                    volumeBar.setProgress(vol);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    int vol = currStatus.getVolume() - 5;
                    if (vol < 0) {
                        vol = 0;
                    }
                    currStatus.setVolume(vol);
                    mSocket.emit("volume", currStatus.getVolume());
                    volumeBar.setProgress(vol);
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }
}
