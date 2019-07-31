package com.olga_o.course_work.musicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
//import android.widget.Toolbar;
import android.support.v7.widget.Toolbar;
import android.widget.LinearLayout;
import android.widget.TextView;



import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SettingsBottomSheet.BottomSheetListener {

    public static final String Broadcast_PLAY_NEW_AUDIO = "ru.olga.PlayNewAudio";
    private static final String LOG_TAG = "log_tag";
    private MediaPlayerService player;
    boolean serviceBound = false;
    ArrayList<Track> defaultTrackList;
    StorageUtil storage;
    ImageView fav;
    boolean sort_less_to_bigger = true;
    private ArrayList<Track> favTrackList;
    boolean favPlaying;

    RecyclerView_Adapter adapter;
    RecyclerView_Adapter adapter_fav;
    DrawerLayout right_side_drawer;
    ImageView play_pause;

    private Bitmap defaultCover;

    public void updateCurrentTrackView() {

        View current_track_view = findViewById(R.id.current_track);
        Track current_track = player.getCurrentTrack();

        ImageView album_cover = (ImageView) current_track_view.findViewById(R.id.track_logo);
        TextView artist = (TextView) current_track_view.findViewById(R.id.track_artist);
        TextView title = (TextView) current_track_view.findViewById(R.id.track_title);

        artist.setText(current_track.getArtist());
        title.setText(current_track.getTitle());

        for (Track track : favTrackList)
            if (track.getPath().equals(current_track.getPath())) // если такая песня уже есть в списке любимых убираем ее отуда и сменяем сердечко
            {
                fav.setImageResource(R.drawable.ic_favorite_true);
                return;
            }

        fav.setImageResource(R.drawable.ic_favorite_false);
        Bitmap track_cover = current_track.getCoverImage(this.getContentResolver());
            if (track_cover != null)
                album_cover.setImageBitmap(Bitmap.createScaledBitmap(track_cover, 360, 360, true));
            else
                album_cover.setImageBitmap(Bitmap.createScaledBitmap(defaultCover, 360, 360, true));



    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storage = new StorageUtil(getApplicationContext());
        String[] permissions = {android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE};
        this.requestPermissions(permissions, LOCATION_PERMISSION);
    }
    int LOCATION_PERMISSION = 2;

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)   {
        if (requestCode == LOCATION_PERMISSION  && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            start_app();
        else {
            this.requestPermissions(permissions, LOCATION_PERMISSION);
        }
    }
    private void start_app() {
        setContentView(R.layout.activity_main);
        loadTracks();
        if (defaultTrackList.size() == 0)
            stop();
        loadFavTracks();
        bounService();

        initFavTracksRecycleView();
        initRecyclerView();

        initButtons(); // так же подгружаются старые настройки
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        setFavListBottomSheet();
        setTrackChangedListener();

        right_side_drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        right_side_drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        right_side_drawer.setScrimColor(Color.TRANSPARENT);

        defaultCover = BitmapFactory.decodeResource(getResources(), R.drawable.default_cover_image);

    }
    private void stop() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();

            storage.setTrackIndex(0);

            int index = 0;
            for (int i = 0; i < defaultTrackList.size(); ++i)
            {
                if (player.trackList.get(i).getPath().equals(storage.getLastSongPath()))
                    index = i;

            }

            player.updateOrder(index);

            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };
    private void bounService() {
        Intent playerIntent = new Intent(this, MediaPlayerService.class);
        startService(playerIntent);
        bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_and_search, menu);

        MenuItem searchItem = menu.findItem(R.id.search_view_track);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.settings:
                SettingsBottomSheet bottomSheet = new SettingsBottomSheet();
                bottomSheet.setAdapter(adapter);
                bottomSheet.show(getSupportFragmentManager(), "exampleBottomSheet");
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putBoolean("serviceStatus", serviceBound);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("serviceStatus");
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (player != null && !player.isNull())
            if (player.isPlaying())
                play_pause.setImageDrawable(getDrawable(R.drawable.image_pause));
            else
                play_pause.setImageDrawable(getDrawable(R.drawable.image_play));

    }

    private void loadFavTracks() {
        if (storage.getFavTrackList() != null) {
            favTrackList = storage.getFavTrackList();
            for (Track track : favTrackList)
                if (!(new File(track.getPath())).exists())
                    favTrackList.remove(track);
        } else
            favTrackList = new ArrayList<>();
    }
    private void loadTracks() {
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);



        defaultTrackList = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                String file_name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                String creationDate = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED));


                Long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));

                defaultTrackList.add(new Track(path, title, album, artist, duration, file_name, creationDate, albumId));
            }
        }
        cursor.close();
    }

    private void updateFavList() {
        adapter_fav.trackList = favTrackList;
        adapter_fav.notifyDataSetChanged();
        storage.setFavTrackList(favTrackList);
    }
    private void initRecyclerView() {
        if (defaultTrackList.size() > 0) {
            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.track_RecyclerView);
            adapter = new RecyclerView_Adapter(defaultTrackList, getApplication());
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.addOnItemTouchListener(new CustomTouchListener(this, new onItemClickListener() {

                // При нажатии на элемент списка устанавливается текущий порядок и размер проигрывания
                @Override
                public void onClick(View view, int index) {
                    favPlaying = false;
                    storage.setTrackIndex(0);
                    player.updateStorage(adapter);
                    player.updateOrder(index);
                    playAudio();

                }
            }));
        }
    }
    private void initFavTracksRecycleView() {
        android.support.v7.widget.RecyclerView fav_RecyclerView = (android.support.v7.widget.RecyclerView) findViewById(R.id.fav_track_RecyclerView);
        adapter_fav = new RecyclerView_Adapter(favTrackList, getApplication());
        fav_RecyclerView.setAdapter(adapter_fav);


        fav_RecyclerView.setLayoutManager(new LinearLayoutManager(this));
        fav_RecyclerView.addOnItemTouchListener(new CustomTouchListener(this, new onItemClickListener() {

            // При нажатии на элемент списка устанавливается текущий порядок и размер проигрывания
            @Override
            public void onClick(View view, int index) {
                favPlaying = true;
                storage.setTrackIndex(0);
                player.updateStorage(adapter_fav);
                player.updateOrder(index);
                playAudio(); // index - порядок в текущем листе и 0
            }
        }));
    }
    BottomSheetBehavior bottomSheetBehavior;
    private void setFavListBottomSheet() {
        // получение вью нижнего экрана
        LinearLayout llBottomSheet = (LinearLayout) findViewById(R.id.include_fav);
        bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet);
        bottomSheetBehavior.setHideable(false);
    }
    private void initButtons() {
        /*
        FAV Button
         */
        fav = (ImageView) findViewById(R.id.do_fav_current_track);

        fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Track current_track = player.getCurrentTrack();


                for (Track track : favTrackList)
                    if (track.getPath().equals(current_track.getPath())) // если такая песня уже есть в списке любимых убираем ее отуда и сменяем сердечко
                    {

                        favTrackList.remove(track);
                        updateFavList();

                        if (favPlaying) {
                            player.stop();
                            if (favTrackList.size() == 0) { // если это был последний трек меняем источник воспроизведения на текущий отображаемый обычный список
                                player.updateStorage(adapter);
                                storage.setTrackIndex(0);
                                player.updateOrder(0);
                                updateCurrentTrackView();
                                favPlaying = false;
                            } else {
                                player.playTrackWithSameIndexInUpdatedList();

                                updateCurrentTrackView();
                                player.updateStorage(adapter_fav);
                            }
                        }

                        fav.setImageResource(R.drawable.ic_favorite_false);
                        //todo сменить сердечко
                        return;
                    }


                for (Track track : defaultTrackList)
                    if (track.getPath().equals(current_track.getPath())) {
                        favTrackList.add(track);
                        fav.setImageResource(R.drawable.ic_favorite_true);
                        updateFavList();
                        return;
                    }

            }
        });

        /*
        sort_less_to_bigger swap
         */
        final ImageView sort_less_to_bigger_button = (ImageView) findViewById(R.id.less_to_bigger);

        sort_less_to_bigger_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sort_less_to_bigger = !sort_less_to_bigger;
                adapter.setSort(storage.getOrderSettings(), sort_less_to_bigger);
                storage.setTrackList(adapter.getTrackList());
            }
        });

        /*
        PLAY PAUSE
         */
        //меняется с play/pause
        play_pause = (ImageView) findViewById(R.id.play_pause);
        play_pause.setImageDrawable(getDrawable(R.drawable.image_play));
        play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (player == null) {

                    bounService();
                    return;
                }
                if (player.isNull()) {
                    player.playTrackWithSameIndexInUpdatedList();
                    play_pause.setImageDrawable(getDrawable(R.drawable.image_pause));                    return;
                }
                if (player.isPlaying()) {
                    player.pauseMedia();
                    play_pause.setImageDrawable(getDrawable(R.drawable.image_play));
                } else {
                    player.resumeMedia();
                    play_pause.setImageDrawable(getDrawable(R.drawable.image_pause));
                }
            }
        });


        /*
        STOP BUTTON

        final Button stop = (Button) findViewById(R.id.stop);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (player == null) {
                    playAudio();
                    return;
                }
                player.stopMedia();
            }
        });

        */
        /*
            play next
         */
        final ImageView play_next = (ImageView) findViewById(R.id.skip_to_next);
        play_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (player == null) {
                    bounService();
                    return;
                }
                play_pause.setImageDrawable(getDrawable(R.drawable.image_pause));
                player.playNextMainActivity();
            }
        });

        /*
        play prev
         */
        final ImageView play_prev = (ImageView) findViewById(R.id.skip_to_previous);
        play_prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (player == null) {
                    bounService();
                    return;
                }
                play_pause.setImageDrawable(getDrawable(R.drawable.image_pause));
                player.playPrevMainActivity();
            }
        });

        /*
        ORDER
         */

        final ImageView fab_order = (ImageView) findViewById(R.id.order);
        //set text from cashed settings

        int old_order = storage.getOrderSettings();
        switch (old_order) {
            case 0: {
                fab_order.setImageDrawable(getDrawable(R.drawable.ic_linear));
                break;
            }
            case 1: {
                fab_order.setImageDrawable(getDrawable(R.drawable.ic_shuffle));
                break;
            }
            case 2: {
                fab_order.setImageDrawable(getDrawable(R.drawable.ic_loop));
                break;
            }
            default: {
                fab_order.setImageDrawable(getDrawable(R.drawable.ic_linear));
                storage.setOrderSettings(0);
                break;
            }
        } // setOldTextSettings
        fab_order.setOnClickListener(new View.OnClickListener() {
            int next_param = storage.getOrderSettings() + 1;

            // 0 - linear
            // 1- shuffle
            // 2 - loop
            @Override
            public void onClick(View view) {

                switch (next_param) {
                    case 0: {
                        fab_order.setImageDrawable(getDrawable(R.drawable.ic_linear));
                        storage.setOrderSettings(0);
                        break;
                    }
                    case 1: {
                        fab_order.setImageDrawable(getDrawable(R.drawable.ic_shuffle));
                        storage.setOrderSettings(1);
                        break;
                        //next_param = -1;
                    }
                    case 2: {
                        next_param = -1;
                        fab_order.setImageDrawable(getDrawable(R.drawable.ic_loop));
                        storage.setOrderSettings(2);
                        break;
                    }
                    default: {
                        fab_order.setImageDrawable(getDrawable(R.drawable.ic_linear));
                        storage.setOrderSettings(0);
                        break;
                    }
                }
                storage.setTrackIndex(0);
                ++next_param;
                player.updateOrder(-1); // все равно внутри надо проверить что порядок тот
            }


        });

        /*
        SORT by
         */
    /*
    Sort
    0 - by file name
    1 - by track title
    2 - by artist
    3 - by date add
     */
        final ImageView botton_sort = (ImageView) findViewById(R.id.sort_by);

        int old_sort = storage.loadSortSettings();
        switch (old_sort) {
            case 0: {
                botton_sort.setImageDrawable(getDrawable(R.drawable.file_sort));
                adapter.setSort(0, sort_less_to_bigger);
                storage.setTrackList(adapter.getTrackList());
                break;
            }
            case 1: {
                botton_sort.setImageDrawable(getDrawable(R.drawable.track_sort));
                adapter.setSort(1, sort_less_to_bigger);
                storage.setTrackList(adapter.getTrackList());
                break;
            }
            case 2: {
                botton_sort.setImageDrawable(getDrawable(R.drawable.artist_sort));
                adapter.setSort(2, sort_less_to_bigger);
                storage.setTrackList(adapter.getTrackList());
                break;
            }
            case 3: {
                botton_sort.setImageDrawable(getDrawable(R.drawable.date_sort));
                adapter.setSort(3, sort_less_to_bigger);
                storage.setTrackList(adapter.getTrackList());
                break;
            }
            default: {
                botton_sort.setImageDrawable(getDrawable(R.drawable.file_sort));
                adapter.setSort(0, sort_less_to_bigger);
                storage.storeSortSettings(0);
                storage.setTrackList(adapter.getTrackList());
                break;
            }
        }
        // setOldTextSettings

        botton_sort.setOnClickListener(new View.OnClickListener() {
            int next_param = storage.loadSortSettings() + 1;

            /*
            Sort
            0 - by file name
            1 - by track title
            2 - by artist
            3 - by date add
             */
            @Override
            public void onClick(View view) {

                switch (next_param) {
                    case 0: {
                        botton_sort.setImageDrawable(getDrawable(R.drawable.file_sort));
                        storage.storeSortSettings(0);
                        adapter.setSort(0, sort_less_to_bigger);
                        break;
                    }
                    case 1: {
                        botton_sort.setImageDrawable(getDrawable(R.drawable.track_sort));
                        storage.storeSortSettings(1);
                        adapter.setSort(1, sort_less_to_bigger);
                        break;
                    }
                    case 2: {
                        botton_sort.setImageDrawable(getDrawable(R.drawable.artist_sort));
                        storage.storeSortSettings(2);
                        adapter.setSort(2, sort_less_to_bigger);
                        break;
                    }
                    case 3: {
                        botton_sort.setImageDrawable(getDrawable(R.drawable.date_sort));
                        storage.storeSortSettings(3);
                        adapter.setSort(3, sort_less_to_bigger);
                        next_param = -1;
                        break;
                    }
                    default: {
                        botton_sort.setImageDrawable(getDrawable(R.drawable.file_sort));
                        storage.storeSortSettings(0);
                        adapter.setSort(0, sort_less_to_bigger);
                        break;
                    }
                }
                ++next_param;
                storage.setTrackList(adapter.getTrackList());

                //player.updateOrder();
            }


        });


        // drag_fav click
        final ImageView drag_fav = (ImageView) findViewById(R.id.drag_fav_list);
        drag_fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                else
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

    }
    private void setTrackChangedListener() {
        MediaPlayerService.TrackState ts = new MediaPlayerService.TrackState();
        MediaPlayerService.new_trackListener new_trackListener = new MediaPlayerService.new_trackListener() {
            @Override
            public void trackChangedEvent() {
                updateCurrentTrackView();
                right_side_drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        };
        ts.addRotationListener(new_trackListener);
    }
    private void playAudio() {

        play_pause.setImageDrawable(getDrawable(R.drawable.image_pause));
        Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
        sendBroadcast(broadcastIntent);

        right_side_drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        storage.setLastSongPath(player.getCurrentTrack().getPath());
        if (serviceBound) {
        	 unbindService(serviceConnection);
            player.stopSelf();
        }
        storage.setTrackIndex(0);
    }

    @Override
    public void onButtonClicked(String text) {
    }

}
