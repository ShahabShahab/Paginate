package com.paginate.recycler;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.paginate.R;

/**
 * Created by shahab on 12/4/2018.
 */

/** RecyclerView creator that will be called to create and bind Error list item */

public interface ErrorListItemCreator {
    /**
     * Create new error list item {@link android.support.v7.widget.RecyclerView.ViewHolder}.
     *
     * @param parent   parent ViewGroup.
     * @param viewType type of the error list item.
     * @return ViewHolder that will be used as error list item.
     */
    RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType);

    /**
     * Bind the error list item.
     *
     * @param holder   error list item ViewHolder.
     * @param position error list item position.
     */
    void onBindViewHolder(RecyclerView.ViewHolder holder, int position);

    ErrorListItemCreator DEFAULT = new ErrorListItemCreator() {
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.error_row, parent, false);
            return new RecyclerView.ViewHolder(view) {
            };
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            // No binding for default loading row
        }
    };
}
