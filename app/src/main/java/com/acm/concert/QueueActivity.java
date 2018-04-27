package com.acm.concert;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.poliveira.parallaxrecyclerview.ParallaxRecyclerAdapter;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import co.dift.ui.SwipeToAction;
import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.Transport;
import uk.co.markormesher.android_fab.FloatingActionButton;
import uk.co.markormesher.android_fab.SpeedDialMenuAdapter;
import uk.co.markormesher.android_fab.SpeedDialMenuItem;

public class QueueActivity extends AppCompatActivity {

    SwipeToAction swipeToAction;
    int mainrgb;
    int bodyText;
    int titleText;
    RecyclerView queueList;
    //QueueViewAdapter adapter;
    ParallaxQueueViewAdapter adapter;
    CoordinatorLayout coordinator;
    List<Queue> queues;
    FloatingActionButton floatingActionButton;

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
            queues.removeAll(queues);
            if (status.getQueue() != null){
                queues.addAll(status.getQueue());
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }
    };

    private final Emitter.Listener changed_callback = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            String queueList;
            if (args.length > 1) {
                queueList = (String) args[1];
            } else {
                queueList = (String) args[0];
            }
            Log.e("CHANGED", "call: " + queueList);
            try {
                JSONArray jsonQueue = new JSONArray(queueList);

                List<Queue> curr = new ArrayList<>();
                for (int i = 0; i < jsonQueue.length(); i++) {
                    Queue queue = new Queue();
                    queue.setTitle(jsonQueue.getJSONObject(i).getString("title"));
                    queue.setPlayedby(jsonQueue.getJSONObject(i).getString("playedby"));
                    queue.setMid(jsonQueue.getJSONObject(i).getString("id"));
                    curr.add(queue);
                }
                queues.removeAll(queues);
                queues.addAll(curr);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };



    private final Emitter.Listener cleared_callback = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            int size = queues.size();
            for (int i = 0; i < queues.size(); i++) {
                queues.remove(i);
            }
            adapter.notifyItemRangeRemoved(0, size);
        }
    };

    private final Emitter.Listener error_callback = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Snackbar mySnackbar = Snackbar.make(coordinator, "Could not get URL Text", Snackbar.LENGTH_LONG);
                    mySnackbar.show();
                }
            });
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);
        Window w = getWindow(); // in Activity's onCreate() for instance
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        queueList = findViewById(R.id.queue_list);
        queueList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        queueList.setLayoutManager(linearLayoutManager);
        coordinator = findViewById(R.id.coordinator);

        floatingActionButton = findViewById(R.id.fab);
        floatingActionButton.setSpeedDialMenuAdapter(new fabSpeedDial());
        floatingActionButton.setContentCoverEnabled(true);

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("WEW", "onTouch: TOUCHED");
            }
        });

        floatingActionButton.setButtonBackgroundColour((((Integer)bodyText & 0x00FFFFFF) | 0xFF000000));
        floatingActionButton.setButtonIconResource(R.drawable.ic_add_black_24dp);

        mainrgb = getIntent().getIntExtra("mainRGB", 0);
        bodyText = getIntent().getIntExtra("bodyText", 0);
        titleText = getIntent().getIntExtra("titleText", 0);
        final String cookie = getIntent().getStringExtra("cookie");

        queues = new ArrayList<>();

        mSocket.io().on(Manager.EVENT_TRANSPORT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Transport transport = (Transport)args[0];

                transport.on(Transport.EVENT_REQUEST_HEADERS, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        @SuppressWarnings("unchecked")
                        Map<String, List<String>> headers = (Map<String, List<String>>)args[0];
                        headers.put("Cookie", Collections.singletonList(cookie));
                    }
                });
            }
        });
        mSocket.on("connected", connected_callback);
        mSocket.on("cleared", cleared_callback);
        mSocket.on("removed", changed_callback);
        mSocket.on("queue_change", changed_callback);
        mSocket.on("download_error", error_callback);
        mSocket.connect();

        RelativeLayout mainLayout = findViewById(R.id.mainLayout);
        mainLayout.setBackgroundColor(mainrgb);



        //adapter = new QueueViewAdapter(queues);
        adapter = new ParallaxQueueViewAdapter(queues);
        queueList.setAdapter(adapter);

        adapter.setParallaxHeader(LayoutInflater.from(this).inflate(R.layout.list_header, queueList, false), queueList);

        swipeToAction = new SwipeToAction(queueList, new SwipeToAction.SwipeListener<Queue>() {
            @Override
            public boolean swipeLeft(Queue itemData) {
                mSocket.emit("remove_song", itemData.getMid());
                int pos = queues.indexOf(itemData);
                queues.remove(itemData);
                adapter.notifyItemRemoved(pos);
                Snackbar mySnackbar = Snackbar.make(coordinator, "Removed" + itemData.getTitle(), Snackbar.LENGTH_SHORT);
                mySnackbar.show();
                return true;
            }

            @Override
            public boolean swipeRight(Queue itemData) {
                return true;
            }

            @Override
            public void onClick(Queue itemData) {
            }

            @Override
            public void onLongClick(Queue itemData) {}
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        mSocket.off("connected");
        mSocket.close();
    }

    public class ParallaxQueueViewAdapter extends ParallaxRecyclerAdapter<Queue> {
        List<Queue> queue;

        ParallaxQueueViewAdapter(List<Queue> data) {
            super(data);
            this.queue = data;
        }

        @Override
        public void onBindViewHolderImpl(RecyclerView.ViewHolder holder, ParallaxRecyclerAdapter<Queue> parallaxRecyclerAdapter, int position) {
            ((QueueViewHolder) holder).data = queue.get(position);
            ((QueueViewHolder) holder).playerText.setText(queue.get(position).getPlayedby());
            ((QueueViewHolder) holder).playerText.setTextColor(titleText);
            ((QueueViewHolder) holder).titleText.setText(queue.get(position).getTitle());
            ((QueueViewHolder) holder).titleText.setTextColor(bodyText);
            ((QueueViewHolder) holder).backgroundLayout.setBackgroundColor(mainrgb);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolderImpl(ViewGroup viewGroup, ParallaxRecyclerAdapter<Queue> parallaxRecyclerAdapter, int i) {
            return new QueueViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.queue_item_layout, viewGroup, false));
        }

        @Override
        public int getItemCountImpl(ParallaxRecyclerAdapter<Queue> parallaxRecyclerAdapter) {
            return queue.size();
        }

        class QueueViewHolder extends SwipeToAction.ViewHolder<Queue> {
            TextView titleText;
            TextView playerText;
            RelativeLayout backgroundLayout;

            QueueViewHolder(View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.title_text);
                playerText = itemView.findViewById(R.id.playterText);
                backgroundLayout = itemView.findViewById(R.id.frontLayout);
            }
        }
    }

    public class QueueViewAdapter extends RecyclerView.Adapter<QueueViewAdapter.QueueViewHolder> {
        List<Queue> queue;

        QueueViewAdapter(List<Queue> queue) {
            this.queue = queue;
        }

        @NonNull
        @Override
        public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.queue_item_layout, parent, false);
            return new QueueViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
            holder.data = queue.get(position);
            holder.playerText.setText(queue.get(position).getPlayedby());
            holder.playerText.setTextColor(titleText);
            holder.titleText.setText(queue.get(position).getTitle());
            holder.titleText.setTextColor(bodyText);
            holder.backgroundLayout.setBackgroundColor(mainrgb);
        }

        @Override
        public int getItemCount() {
            return queue.size();
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

        class QueueViewHolder extends SwipeToAction.ViewHolder<Queue> {
            TextView titleText;
            TextView playerText;
            RelativeLayout backgroundLayout;

            QueueViewHolder(View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.title_text);
                playerText = itemView.findViewById(R.id.playterText);
                backgroundLayout = itemView.findViewById(R.id.frontLayout);
            }
        }
    }

    public class fabSpeedDial extends SpeedDialMenuAdapter {

        @Override
        public int getCount() {
            return 2;
        }

        @NotNull
        @Override
        public SpeedDialMenuItem getMenuItem(Context context, int i) {
            switch (i){
                case 0:
                    return new SpeedDialMenuItem(context, R.mipmap.ic_queue_black_24dp, "Add to queue");
                case 1:
                    return new SpeedDialMenuItem(context, R.mipmap.ic_clear_all_black_24dp, "Clear queue");
                default:
                    throw new IllegalArgumentException("Invalid menu item");
            }
        }

        @Override
        public boolean onMenuItemClick(int position) {
            switch (position) {
                case 0:
                    MaterialDialog dialog = new MaterialDialog.Builder(QueueActivity.this)
                            .title("Add a song")
                            .content("Enter a Youtube/Soundcloud URL")
                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI)
                            .input("URL", "", false, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {

                                }
                            })
                            .positiveText("Add")
                            .negativeText("Cancel")
                            .widgetColor(bodyText)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    if (dialog.getInputEditText().getText() != null){
                                        String url = String.valueOf(dialog.getInputEditText().getText());
                                        Log.e("ADDING", "onClick: " +url );
                                        mSocket.emit("download", url);
                                        dialog.dismiss();
                                    } else {
                                        dialog.dismiss();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Snackbar mySnackbar = Snackbar.make(coordinator, "Could not get URL Text", Snackbar.LENGTH_LONG);
                                                mySnackbar.show();
                                            }
                                        });
                                    }

                                }
                            })
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(MaterialDialog dialog, DialogAction which) {
                                    dialog.dismiss();
                                }
                            })
                            .backgroundColor(mainrgb)
                            .titleColor(titleText)
                            .contentColor(bodyText)
                            .build();
                    dialog.show();
                    return true;
                case 1:
                    mSocket.emit("clear");
                    return true;
                default:
                    return super.onMenuItemClick(position);
            }
        }
    }

}
