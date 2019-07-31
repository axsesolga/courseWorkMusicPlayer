package com.olga_o.course_work.musicplayer;

//import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {


    public static final String ACTION_PLAY = "ru.olga.ACTION_PLAY";
    public static final String ACTION_PAUSE = "ru.olga.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "ru.olga.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "ru.olga.ACTION_NEXT";
    public static final String ACTION_STOP = "ru.olga..ACTION_STOP";

    private MediaPlayer mediaPlayer;
    private boolean isLoopOrder;

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }
    public boolean isNull() {
        return mediaPlayer == null;
    }


    private int resumePosition;

    private AudioManager audioManager;

    private final IBinder iBinder = new LocalBinder();

    public ArrayList<Track> trackList;
    private ArrayList<Integer> trackOrder;

    // отслеживает что новая песня появилась
    public interface new_trackListener {
        public void trackChangedEvent();
    }

    public static class TrackState {
        private static boolean new_track = false;
        private static final ArrayList<new_trackListener> nextTrackListeners = new ArrayList<>();

        public static void setNextTrack() {
            new_track = !new_track;
            for (new_trackListener ping : nextTrackListeners) {
                ping.trackChangedEvent();
            }
        }

        public void addRotationListener(new_trackListener toadd) {
            nextTrackListeners.add(toadd);
        }
    }


    public Track getCurrentTrack() {
        if (trackList.size() == 0)
            return null;
        return trackList.get(trackOrder.get(storage.getTrackIndex()));
    }
    public int getRealIndex()
    {
        return trackOrder.get(storage.getTrackIndex());
    }


    private StorageUtil storage;
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        callStateListener();
        registerBecomingNoisyReceiver();
        register_playNewAudio();
        storage = new StorageUtil(getApplicationContext());

        updateStorage(null);
        switch (storage.getOrderSettings()) {
            case 0: {
                this.linearTrackOrder(storage.getTrackIndex());
                break;
            }
            case 1: {
                this.shuffleTrackOrder(storage.getTrackIndex());
                break;
            }
            case 2: {
                this.loopTrackOrder(storage.getTrackIndex());
                break;
            }
            default: {
                this.linearTrackOrder(storage.getTrackIndex());
                storage.setOrderSettings(0);
                break;
            }
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            updateStorage(null);
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (requestAudioFocus() == false) {
            stopSelf();
        }


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);
        storage.setLastSongPath(getCurrentTrack().getPath());
        new StorageUtil(getApplicationContext()).clearCachedTrackPlaylist();
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }


    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        if (trackList.size() >= 1)
            skipToNext();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
            }
        }, 1000);

        Intent broadcastIntent = new Intent(MainActivity.Broadcast_PLAY_NEW_AUDIO);
        sendBroadcast(broadcastIntent);
    }

    public void playTrackWithSameIndexInUpdatedList() {
        if (isLoopOrder)
            return;

        if (storage.getTrackIndex() == trackList.size()) {
            storage.setTrackIndex(0);
        }

        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.reset();
        }
        initMediaPlayer();


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                // Actions to do after 10 seconds
            }
        }, 1000);

        Intent broadcastIntent = new Intent(MainActivity.Broadcast_PLAY_NEW_AUDIO);
        sendBroadcast(broadcastIntent);
    }


    public void playNextMainActivity() {
        skipToNext();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                // Actions to do after 10 seconds
            }
        }, 1000);

        Intent broadcastIntent = new Intent(MainActivity.Broadcast_PLAY_NEW_AUDIO);
        sendBroadcast(broadcastIntent);
    }

    public void playPrevMainActivity() {
        skipToPrevious();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                // Actions to do after 10 seconds
            }
        }, 1000);

        Intent broadcastIntent = new Intent(MainActivity.Broadcast_PLAY_NEW_AUDIO);
        sendBroadcast(broadcastIntent);
    }



    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                    TrackState.setNextTrack();
                }
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }


    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }


    private void initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }

        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            if ((new File(trackList.get(trackOrder.get(storage.getTrackIndex())).getPath())).exists())
                mediaPlayer.setDataSource(trackList.get(trackOrder.get(storage.getTrackIndex())).getPath());
            else {
                playNextMainActivity();
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            playNextMainActivity();
            return;
        }
        mediaPlayer.prepareAsync();
    }

    public void playMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            TrackState.setNextTrack();
        }
    }

    public void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            resumePosition = 0;
        }
    }

    public void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
    }

    public void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
            TrackState.setNextTrack();
        }
    }

    public void stop() {
        if (mediaPlayer.isPlaying())
            mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }
    private void skipToNext() {
        if (isLoopOrder)
            return;

        if (storage.getTrackIndex() == trackList.size() - 1) {
            storage.setTrackIndex(0);
        } else {
            storage.setTrackIndex(storage.getTrackIndex() + 1);
        }
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.reset();
        }
        initMediaPlayer();
    }

    private void skipToPrevious() {

        if (isLoopOrder)
            return;

        if (storage.getTrackIndex() == 0) {
            storage.setTrackIndex(trackList.size() - 1);
        } else {
            storage.setTrackIndex(storage.getTrackIndex() - 1);
        }

        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.reset();
        }
        initMediaPlayer();
    }


    /*
    TRACK ORDER
  в очереди  номер песни в дефолтном листе
    0           1
    1           3
    2           5
    3           0
    4           2
    5           4

     */
    public void linearTrackOrder(int start_pos) {
        trackOrder = new ArrayList<>(trackList.size());
        for (int i = 0; i < trackList.size(); ++i)
            trackOrder.add((i + start_pos) % trackList.size());
        isLoopOrder = false;
    }

    public void shuffleTrackOrder(int start_pos) {
        isLoopOrder = false;
        trackOrder = new ArrayList<>(trackList.size());
        Random rd = new Random();
        Set<Integer> used = new HashSet<>();
        used.add(-1);
        trackOrder.add(start_pos);
        used.add(start_pos);
        for (int i = 1; i < trackList.size(); ++i) {
            int next_rd = -1;
            while (used.contains(next_rd)) {
                next_rd = rd.nextInt(trackList.size());
            }
            used.add(next_rd);
            trackOrder.add(next_rd);
        }
        int i = 0;
        i += 1;

    }

    public void loopTrackOrder(int index) {
        trackOrder = new ArrayList<>();
        for (int i = 0; i < trackList.size(); ++i)
            trackOrder.add(index);
        isLoopOrder = true;
    }

    public void updateOrder(int start_index) {
        if (start_index == -1) // значит поменять порядок но первая песня = текущая песня
            start_index = trackOrder.get(storage.getTrackIndex());
        switch (storage.getOrderSettings()) {
            case 0: {
                linearTrackOrder(start_index);
                break;
            }
            case 1: {
                shuffleTrackOrder(start_index);
                break;
            }
            case 2: {
                loopTrackOrder(start_index);
                break;
            }
            default: {
                linearTrackOrder(start_index);
                break;
            }
        }
    }

    public void updateStorage(RecyclerView_Adapter adapter) {
        if (adapter == null)
            trackList = storage.getTrackList();
        else
            trackList = adapter.getTrackList();
    }



    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pauseMedia();
        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int temp_index = storage.getTrackIndex();
            if (temp_index != -1 && temp_index < trackList.size()) {
            } else {
                stopSelf();
            }

            if (requestAudioFocus() == false) {
                //Could not gain focus
                stopSelf();
            }


            if (mediaPlayer == null)
                initMediaPlayer();

            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();
            storage.setLastSongPath(getCurrentTrack().getPath());
        }
    };


    private void register_playNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }




}

