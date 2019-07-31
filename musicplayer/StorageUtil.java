package com.olga_o.course_work.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.lang.reflect.Type;

public class StorageUtil {

    private final String STORAGE = "ru.olga.player.STORAGE";
    private SharedPreferences preferences;
    private Context context;

    public StorageUtil(Context context) {
        this.context = context;
    }

    public void setTrackList(ArrayList<Track> arrayList) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString("trackArrayList", json);
        editor.apply();
    }

    public ArrayList<Track> getTrackList() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("trackArrayList", null);
        Type type = new TypeToken<ArrayList<Track>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public void setTrackIndex(int index) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audioOrderIndex", index);
        editor.apply();
    }

    public int getTrackIndex() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getInt("audioOrderIndex", 0);//return -1 if no data found
    }




    public void setLastSongPath(String path) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("last_song_path", path);
        editor.apply();
    }
    public String getLastSongPath()
    {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getString("last_song_path", null);//return -1 if no data found
    }


    public void clearCachedTrackPlaylist() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }

    /*
    order
    0-linear
    1-shuffle
    2-loop
     */
    public void setOrderSettings(int order) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("orderSettings", order);
        editor.apply();
    }

    public int getOrderSettings() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getInt("orderSettings", -1);//return -1 if no data found
    }

    /*
    Sort
    0 - alphabet
    1 - by track title
    2 - by artist
    3 - by date add
     */
    public void storeSortSettings(int order) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("SortSettings", order);
        editor.apply();
    }

    public int loadSortSettings() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getInt("SortSettings", -1);//return -1 if no data found
    }

    public void setFavTrackList(ArrayList<Track> arrayList) {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString("favTrackArrayList", json);
        editor.apply();
    }

    public ArrayList<Track> getFavTrackList() {
        preferences = context.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("favTrackArrayList", null);
        Type type = new TypeToken<ArrayList<Track>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

}
