package com.olga_o.course_work.musicplayer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RecyclerView_Adapter extends RecyclerView.Adapter<ViewHolder> implements Filterable {

    ArrayList<Track> trackList;
    ArrayList<Track> fullTrackList;
    Context context;

    public ArrayList<Track> getTrackList() {
        return trackList;
    }
    public RecyclerView_Adapter(ArrayList<Track> list, Context context) {
        this.fullTrackList = list;
        this.trackList = new ArrayList<>(list);
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Inflate the layout, initialize the View Holder
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        ViewHolder holder = new ViewHolder(v);
        return holder;

    }
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        //Use the provided View Holder on the onCreateViewHolder method to populate the current row on the RecyclerView
        holder.title.setText(trackList.get(position).getTitle());
        holder.artist.setText(trackList.get(position).getArtist());
    }
    @Override
    public int getItemCount() {
        //returns the number of elements the RecyclerView will display
        return trackList.size();
    }
    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
    @Override
    public Filter getFilter() {
        return filter;
    }


    public boolean file_name = true;
    public boolean title = true;
    public boolean artist = true;
    public boolean album = true;

    private boolean filterOptions(Track track, String filterPattern) {
        boolean return_value = false;
        if (file_name)
            return_value = return_value || track.getFile_name().toLowerCase().contains(filterPattern.toLowerCase());

        if (title)
            return_value = return_value || track.getTitle().toLowerCase().contains(filterPattern.toLowerCase());

        if (artist)
            return_value = return_value || track.getArtist().toLowerCase().contains(filterPattern.toLowerCase());

        if (album)
            return_value = return_value || track.getAlbum().toLowerCase().contains(filterPattern.toLowerCase());

        return return_value;
    }
    private Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Track> newList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                newList.addAll(fullTrackList);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (Track item : fullTrackList) {
                    if (filterOptions(item, filterPattern)) {
                        newList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = newList;

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            trackList.clear();
            trackList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    /*
    Sort
    0 - by file name
    1 - by track title
    2 - by artist
    3 - by date add
     */
    public void setSort(final int sort_type, final boolean less_to_bigger) {
        trackList.sort(new Comparator<Track>() {
            @Override
            public int compare(Track t1, Track t2) {
                switch (sort_type) {
                    case 0: {
                        if (less_to_bigger)
                            return t1.getFile_name().compareTo(t2.getFile_name());
                        return -t1.getFile_name().compareTo(t2.getFile_name());

                    }
                    case 1: {
                        if (less_to_bigger)
                            return t1.getTitle().compareTo(t2.getTitle());
                        return -t1.getTitle().compareTo(t2.getTitle());
                    }
                    case 2: {
                        if (less_to_bigger)
                            return t1.getArtist().compareTo(t2.getArtist());
                        return -t1.getArtist().compareTo(t2.getArtist());
                    }
                    case 3: {
                        if (less_to_bigger)
                            return t1.getCreationDate() - t2.getCreationDate();
                        return -(t1.getCreationDate() - t2.getCreationDate());
                    }
                    default: {
                        return t1.getTitle().compareTo(t2.getTitle());
                    }
                }
            }
        });
        this.fullTrackList = new ArrayList<>(trackList);
        notifyDataSetChanged();
    }
}

class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

    TextView title;
    TextView artist;

    ViewHolder(View itemView) {
        super(itemView);
        title = (TextView) itemView.findViewById(R.id.item_title);
        artist = (TextView) itemView.findViewById(R.id.item_artist);
    }

    @Override
    public boolean onLongClick(View view) {
        // Handle long click
        // Return true to indicate the click was handled
        return true;
    }
}