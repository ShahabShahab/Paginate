package com.paginate.recycler;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

class WrapperAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int ITEM_VIEW_TYPE_LOADING = Integer.MAX_VALUE - 50; // Magic
    private final int ITEM_VIEW_TYPE_ERROR = Integer.MAX_VALUE - 100; // Magic TOO!

    private final RecyclerView.Adapter wrappedAdapter;
    private LoadingListItemCreator loadingListItemCreator;
    private ErrorListItemCreator errorListItemCreator;
    private boolean displayLoadingRow = true;
    private boolean displayErrorRow = true;


    public WrapperAdapter(RecyclerView.Adapter adapter) {
        this.wrappedAdapter = adapter;
    }


    public void setLoadingListItemCreator(LoadingListItemCreator creator) {
        this.loadingListItemCreator = creator;
    }

    public void setErrorListItemCreator(ErrorListItemCreator creator) {
        this.errorListItemCreator = creator;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {
        switch (viewType) {
            case ITEM_VIEW_TYPE_LOADING: {
                return loadingListItemCreator.onCreateViewHolder(parent,
                        viewType);

            }
            case ITEM_VIEW_TYPE_ERROR: {
                return errorListItemCreator.onCreateViewHolder(parent,
                        viewType);

            }
            default: {
                return wrappedAdapter.onCreateViewHolder(parent,
                        viewType);

            }
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (isLoadingRow(position)) {
            loadingListItemCreator.onBindViewHolder(holder, position);
        } else if (isErrorRow(position)) {
            errorListItemCreator.onBindViewHolder(holder, position);
        } else {
            wrappedAdapter.onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return (displayLoadingRow || displayErrorRow)
                ? wrappedAdapter.getItemCount() + 1 : wrappedAdapter.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoadingRow(position)) {
            return ITEM_VIEW_TYPE_LOADING;
        } else if (isErrorRow(position)) {
            return ITEM_VIEW_TYPE_ERROR;
        } else {
            return wrappedAdapter.getItemViewType(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return (isLoadingRow(position) ||
                isErrorRow(position)) ? RecyclerView.NO_ID :
                wrappedAdapter.getItemId(position);
    }

    @Override
    public void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(hasStableIds);
        wrappedAdapter.setHasStableIds(hasStableIds);
    }

    public RecyclerView.Adapter getWrappedAdapter() {
        return wrappedAdapter;
    }

    boolean isDisplayLoadingRow() {
        return displayLoadingRow;
    }

    void displayLoadingRow(boolean displayLoadingRow) {
        if (this.displayLoadingRow != displayLoadingRow) {
            this.displayLoadingRow = displayLoadingRow;
            notifyDataSetChanged();
        }
    }

    boolean isLoadingRow(int position) {
        return displayLoadingRow && position == getLoadingRowPosition();
    }

    private int getLoadingRowPosition() {
        return displayLoadingRow ? getItemCount() - 1 : -1;
    }

    boolean isDisplayErrorRow() {
        return displayErrorRow;
    }

    void displayErrorRow(boolean displayErrorRow) {
        if (this.displayErrorRow != displayErrorRow) {
            this.displayErrorRow = displayErrorRow;
            notifyDataSetChanged();
        }
    }

    boolean isErrorRow(int position) {
        return displayErrorRow && position == getErrorRowPosition();
    }

    private int getErrorRowPosition() {
        return displayErrorRow ? getItemCount() - 1 : -1;
    }

}