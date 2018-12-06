package com.paginate.recycler;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import com.paginate.Paginate;

public final class RecyclerPaginate extends Paginate {

    private final RecyclerView recyclerView;
    private final Callbacks callbacks;
    private final int loadingTriggerThreshold;
    private WrapperAdapter wrapperAdapter;
    private WrapperSpanSizeLookup wrapperSpanSizeLookup;

    RecyclerPaginate(RecyclerView recyclerView,
                     Callbacks callbacks,
                     int loadingTriggerThreshold,
                     boolean addLoadingListItem,
                     boolean addErrorListItem,
                     ErrorListItemCreator errorListItemCreator,
                     LoadingListItemCreator loadingListItemCreator,
                     LoadingListItemSpanLookup loadingListItemSpanLookup) {

        this.recyclerView = recyclerView;
        this.callbacks = callbacks;
        this.loadingTriggerThreshold = loadingTriggerThreshold;

        // Attach scrolling listener in order to perform end offset check on each scroll event
        recyclerView.addOnScrollListener(mOnScrollListener);

        bindRecyclerView(recyclerView);

        // Wrap existing adapter with adapter that will add loading row
        if (addLoadingListItem) {
            wrapperAdapter.setLoadingListItemCreator(loadingListItemCreator);
        }

        // Wrap existing adapter with adapter that will add error row
        if (addErrorListItem) {
            wrapperAdapter.setErrorListItemCreator(errorListItemCreator);
        }

        // For GridLayoutManager use separate/customisable span lookup for loading row
        bindGridLayoutManager(recyclerView, loadingListItemSpanLookup);


        // Trigger initial check since adapter might not have any items initially so no scrolling events upon
        // RecyclerView (that triggers check) will occur
        checkEndOffset();
    }

    private void bindRecyclerView(RecyclerView recyclerView) {
        RecyclerView.Adapter adapter = recyclerView.getAdapter();
        wrapperAdapter = new WrapperAdapter(adapter);
        adapter.registerAdapterDataObserver(mDataObserver);
        recyclerView.setAdapter(wrapperAdapter);
    }

    private void bindGridLayoutManager(RecyclerView recyclerView, LoadingListItemSpanLookup loadingListItemSpanLookup) {
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
            wrapperSpanSizeLookup = new WrapperSpanSizeLookup(
                    ((GridLayoutManager) recyclerView.getLayoutManager()).getSpanSizeLookup(),
                    loadingListItemSpanLookup,
                    wrapperAdapter);
            ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanSizeLookup(wrapperSpanSizeLookup);
        }
    }

    @Override
    public void adapterChanged() {
        onAdapterDataChanged();
    }

    @Override
    public void setHasMoreDataToLoad(boolean hasMoreDataToLoad) {
        if (wrapperAdapter != null) {
            wrapperAdapter.displayLoadingRow(hasMoreDataToLoad);
        }
    }

    @Override
    public void unbind() {
        recyclerView.removeOnScrollListener(mOnScrollListener);   // Remove scroll listener
        if (recyclerView.getAdapter() instanceof WrapperAdapter) {
            WrapperAdapter wrapperAdapter = (WrapperAdapter) recyclerView.getAdapter();
            RecyclerView.Adapter adapter = wrapperAdapter.getWrappedAdapter();
            adapter.unregisterAdapterDataObserver(mDataObserver); // Remove data observer
            recyclerView.setAdapter(adapter);                     // Swap back original adapter
        }
        if (recyclerView.getLayoutManager() instanceof GridLayoutManager && wrapperSpanSizeLookup != null) {
            // Swap back original SpanSizeLookup
            GridLayoutManager.SpanSizeLookup spanSizeLookup = wrapperSpanSizeLookup.getWrappedSpanSizeLookup();
            ((GridLayoutManager) recyclerView.getLayoutManager()).setSpanSizeLookup(spanSizeLookup);
        }
    }

    void checkEndOffset() {
        int visibleItemCount = recyclerView.getChildCount();
        int totalItemCount = recyclerView.getLayoutManager().getItemCount();

        int firstVisibleItemPosition;
        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            firstVisibleItemPosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        } else if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            // https://code.google.com/p/android/issues/detail?id=181461
            if (recyclerView.getLayoutManager().getChildCount() > 0) {
                firstVisibleItemPosition = ((StaggeredGridLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPositions(null)[0];
            } else {
                firstVisibleItemPosition = 0;
            }
        } else {
            throw new IllegalStateException("LayoutManager needs to subclass LinearLayoutManager or StaggeredGridLayoutManager");
        }

        // Check if end of the list is reached (counting threshold) or if there is no items at all
        if ((totalItemCount - visibleItemCount) <= (firstVisibleItemPosition +
                loadingTriggerThreshold)
                || totalItemCount == 0) {
            // Call load more only if loading is not currently in progress and if there is more items to load
            if (!callbacks.isLoading() &&
                    !callbacks.hasLoadedAllItems()) {
                callbacks.onLoadMore();
            }

            if (callbacks.hasLoadingErrorOccurred()) {
                callbacks.onLoadMoreFailed();
            }

        }
    }

    private void onAdapterDataChanged() {
        wrapperAdapter.displayLoadingRow(!callbacks.hasLoadedAllItems());
        wrapperAdapter.displayErrorRow(callbacks.hasLoadingErrorOccurred());
        checkEndOffset();
    }

    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            checkEndOffset(); // Each time when list is scrolled check if end of the list is reached
        }
    };

    private final RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            wrapperAdapter.notifyDataSetChanged();
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            wrapperAdapter.notifyItemRangeInserted(positionStart, itemCount);
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            wrapperAdapter.notifyItemRangeChanged(positionStart, itemCount);
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            wrapperAdapter.notifyItemRangeChanged(positionStart, itemCount, payload);
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            wrapperAdapter.notifyItemRangeRemoved(positionStart, itemCount);
            onAdapterDataChanged();
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            wrapperAdapter.notifyItemMoved(fromPosition, toPosition);
            onAdapterDataChanged();
        }
    };

    public static class Builder {

        private final RecyclerView recyclerView;
        private final Paginate.Callbacks callbacks;

        private int loadingTriggerThreshold = 5;
        private boolean addLoadingListItem = true;
        private boolean addErrorListItem = false;
        private LoadingListItemCreator loadingListItemCreator;
        private LoadingListItemSpanLookup loadingListItemSpanLookup;
        private ErrorListItemCreator errorListItemCreator;

        public Builder(RecyclerView recyclerView, Paginate.Callbacks callbacks) {
            this.recyclerView = recyclerView;
            this.callbacks = callbacks;
        }

        /**
         * Set the offset from the end of the list at which the load more event needs to be triggered. Default offset
         * if 5.
         *
         * @param threshold number of items from the end of the list.
         * @return {@link com.paginate.recycler.RecyclerPaginate.Builder}
         */
        public Builder setLoadingTriggerThreshold(int threshold) {
            this.loadingTriggerThreshold = threshold;
            return this;
        }

        /**
         * Setup loading row. If loading row is used original adapter set on RecyclerView will be wrapped with
         * internal adapter that will add loading row as the last item in the list. Paginate will observer the
         * changes upon original adapter and remove loading row if there is no more data to load. By default loading
         * row will be added.
         *
         * @param addLoadingListItem true if loading row needs to be added, false otherwise.
         * @return {@link com.paginate.recycler.RecyclerPaginate.Builder}
         * @see {@link com.paginate.Paginate.Callbacks#hasLoadedAllItems()}
         * @see {@link com.paginate.recycler.RecyclerPaginate.Builder#setLoadingListItemCreator(LoadingListItemCreator)}
         */
        public Builder addLoadingListItem(boolean addLoadingListItem) {
            this.addLoadingListItem = addLoadingListItem;
            return this;
        }

        /**
         * Setup loading row. If loading row is used original adapter set on RecyclerView will be wrapped with
         * internal adapter that will add loading row as the last item in the list. Paginate will observer the
         * changes upon original adapter and remove loading row if there is no more data to load. By default loading
         * row will be added...
         *
         * @param addErrorListItem true if error row needs to be added, false otherwise.
         * @return {@link com.paginate.recycler.RecyclerPaginate.Builder}
         * @see {@link com.paginate.Paginate.Callbacks#hasLoadedAllItems()}
         * @see {@link com.paginate.recycler.RecyclerPaginate.Builder#setLoadingListItemCreator(LoadingListItemCreator)}
         */
        public Builder addErrorListItem(boolean addErrorListItem) {
            this.addErrorListItem = addErrorListItem;
            return this;
        }

        /**
         * Set custom loading list item creator. If no creator is set default one will be used.
         *
         * @param creator Creator that will ne called for inflating and binding loading list item.
         * @return {@link com.paginate.recycler.RecyclerPaginate.Builder}
         */
        public Builder setLoadingListItemCreator(LoadingListItemCreator creator) {
            this.loadingListItemCreator = creator;
            return this;
        }

        /**
         * Set custom error list item creator. If no creator is set default one will be used.
         *
         * @param creator Creator that will be called for inflating and binding error list item.
         * @return {@link com.paginate.recycler.RecyclerPaginate.Builder}
         */
        public Builder setCustomErrorItemCreator(ErrorListItemCreator creator) {
            this.errorListItemCreator = creator;
            return this;
        }

        /**
         * Set custom SpanSizeLookup for loading list item. Use this when {@link GridLayoutManager} is used and
         * loading list item needs to have custom span. Full span of {@link GridLayoutManager} is used by default.
         *
         * @param loadingListItemSpanLookup LoadingListItemSpanLookup that will be called for loading list item span.
         * @return {@link com.paginate.recycler.RecyclerPaginate.Builder}
         */
        public Builder setLoadingListItemSpanSizeLookup(LoadingListItemSpanLookup loadingListItemSpanLookup) {
            this.loadingListItemSpanLookup = loadingListItemSpanLookup;
            return this;
        }

        /**
         * Create pagination functionality upon RecyclerView.
         *
         * @return {@link Paginate} instance.
         */
        public Paginate build() {
            if (recyclerView.getAdapter() == null) {
                throw new IllegalStateException("Adapter needs to be set!");
            }
            if (recyclerView.getLayoutManager() == null) {
                throw new IllegalStateException("LayoutManager needs to be set on the RecyclerView");
            }

            if (loadingListItemCreator == null) {
                loadingListItemCreator = LoadingListItemCreator.DEFAULT;
            }

            if (loadingListItemSpanLookup == null) {
                loadingListItemSpanLookup = new DefaultLoadingListItemSpanLookup(recyclerView.getLayoutManager());
            }

            return new RecyclerPaginate(recyclerView, callbacks, loadingTriggerThreshold, addLoadingListItem,
                    true,
                    errorListItemCreator,
                    loadingListItemCreator,
                    loadingListItemSpanLookup);
        }
    }

}