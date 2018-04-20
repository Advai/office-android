package com.acm.concert;

import java.io.Serializable;

class Queue implements Serializable {

    private Integer duration;
    private String title;
    private String mrl;
    private String mid;
    private String playedby;
    private String thumbnail;

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMrl() {
        return mrl;
    }

    public void setMrl(String mrl) {
        this.mrl = mrl;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public String getPlayedby() {
        return playedby;
    }

    public void setPlayedby(String playedby) {
        this.playedby = playedby;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }
}