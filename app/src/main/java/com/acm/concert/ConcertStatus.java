package com.acm.concert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ConcertStatus implements Serializable {

    private String currentTrack;
    private String audioStatus;
    private Integer duration;
    private Boolean isPlaying;
    private List<Queue> queue = null;
    private Integer currentTime;
    private String thumbnail;
    private String playedby;
    private Integer volume;
    private String media;

    ConcertStatus(String arg) {
        // Do our JSON parsing here
        try {
            JSONObject jsonObject = new JSONObject(arg);

            this.media = jsonObject.getString("media");
            this.audioStatus = jsonObject.getString("audio_status");
            this.volume = jsonObject.getInt("volume");
            this.isPlaying = jsonObject.getBoolean("is_playing");

            if (media != null) {
                this.currentTrack = jsonObject.getString("current_track");
                this.audioStatus = jsonObject.getString("audio_status");
                this.duration = jsonObject.getInt("duration");
                this.currentTime = jsonObject.getInt("current_time");
                this.thumbnail = jsonObject.getString("thumbnail");
                this.playedby = jsonObject.getString("playedby");
                this.volume = jsonObject.getInt("volume");
                JSONArray queueArray = new JSONArray(jsonObject.getString("queue"));
                this.queue = new ArrayList<>();
                for (int i = 0; i < queueArray.length(); i++) {
                    Queue queue = new Queue();
                    queue.setTitle(queueArray.getJSONObject(i).getString("title"));
                    queue.setPlayedby(queueArray.getJSONObject(i).getString("playedby"));
                    queue.setMid(queueArray.getJSONObject(i).getString("id"));
                    queue.setThumbnail(queueArray.getJSONObject(i).getString("thumbnail"));
                    this.queue.add(queue);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    ConcertStatus() {
        this.audioStatus = "State.NothingSpecial";
        this.currentTime = 0;
        this.isPlaying = false;
        this.currentTrack = "";
        this.duration = 1;
        this.thumbnail = "";
    }

    public List<Queue> getQueue() {
        return queue;
    }

    @Override
    public String toString() {
        return "ConcertStatus{" +
                "currentTrack='" + currentTrack + '\'' +
                ", audioStatus='" + audioStatus + '\'' +
                ", duration=" + duration +
                ", isPlaying=" + isPlaying +
                ", queue=" + queue +
                ", currentTime=" + currentTime +
                ", thumbnail='" + thumbnail + '\'' +
                ", playedby='" + playedby + '\'' +
                ", volume=" + volume +
                ", media='" + media + '\'' +
                '}';
    }

    enum playerStatus {
        NOTHINGSPECIAL,
        OPENING,
        PLAYING,
        PAUSED
    }

    String getCurrentTrack() {
        return currentTrack;
    }

    playerStatus getAudioStatus() {
        switch (audioStatus) {
            case "State.NothingSpecial":
                return playerStatus.NOTHINGSPECIAL;
            case "State.Playing":
                return playerStatus.PLAYING;
            case "State.Opening":
                return playerStatus.OPENING;
            case "State.Paused":
                return playerStatus.PAUSED;
        }
        return playerStatus.NOTHINGSPECIAL;
    }

    public void setAudioStatus(String audioStatus) {
        this.audioStatus = audioStatus;
    }

    Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    Boolean getIsPlaying() {
        return isPlaying;
    }

    Integer getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(Integer currentTime) {
        this.currentTime = currentTime;
    }

    String getThumbnail() {
        return thumbnail;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

}