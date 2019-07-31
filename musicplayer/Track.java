package com.olga_o.course_work.musicplayer;


import android.content.ContentResolver;
import android.content.ContentUris;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.IOException;
import java.io.Serializable;

public class Track implements Serializable {

    private String path;
    private String title;
    private String album;
    private String artist;
    private int duration;
    private String file_name;
    private int creationDate;
    private Long albumIdcover;


    public Track(String path, String title, String album, String artist, String duration, String file_name, String creationDate, Long albumId) {
        this.path = path;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.duration = Integer.parseInt(duration);
        this.file_name = file_name;
        this.creationDate = Integer.parseInt(creationDate);
        this.albumIdcover = albumId;
    }

    public String getPath() {
        return path;
    }

    public void setData(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }


    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public int getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(int creationDate) {
        this.creationDate = creationDate;
    }

    public Bitmap getCoverImage(ContentResolver cr) {
        try {

            Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
            Uri cover = ContentUris.withAppendedId(sArtworkUri, albumIdcover);

            return  MediaStore.Images.Media.getBitmap(cr, cover);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
