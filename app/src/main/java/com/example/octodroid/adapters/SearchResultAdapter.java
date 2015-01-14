package com.example.octodroid.adapters;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.example.octodroid.views.MoreLoadScrollListener;
import com.example.octodroid.views.holders.ProgressViewHolder;
import com.example.octodroid.views.holders.RepositoryItemViewHolder;
import com.rejasupotaro.octodroid.GitHub;
import com.rejasupotaro.octodroid.http.Order;
import com.rejasupotaro.octodroid.http.Response;
import com.rejasupotaro.octodroid.http.Sort;
import com.rejasupotaro.octodroid.models.Repository;
import com.rejasupotaro.octodroid.models.SearchResult;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.android.view.ViewObservable;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;

public class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static class ViewType {
        private static final int ITEM = 1;
        private static final int FOOTER = 2;
    }

    private RecyclerView recyclerView;
    private List<Repository> repositories = new ArrayList<>();
    private CompositeSubscription subscription = new CompositeSubscription();
    private BehaviorSubject<Observable<Response<SearchResult>>> responseSubject;
    private Observable<Response<SearchResult>> pagedResponse;

    public SearchResultAdapter(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        LinearLayoutManager layoutManager = new LinearLayoutManager(recyclerView.getContext());
        recyclerView.setOnScrollListener(new MoreLoadScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int current_page) {
                if (pagedResponse != null) {
                    responseSubject.onNext(pagedResponse);
                }
            }
        });
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ViewType.FOOTER) {
            return ProgressViewHolder.create(parent);
        } else {
            return RepositoryItemViewHolder.create(parent);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        int viewType = getItemViewType(position);
        if (viewType == ViewType.FOOTER) {
            // do nothing
        } else {
            Repository repository = repositories.get(position);
            ((RepositoryItemViewHolder) viewHolder).bind(repository);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (repositories.size() == 0 || position == repositories.size()) {
            return ViewType.FOOTER;
        } else {
            return ViewType.ITEM;
        }
    }

    @Override
    public int getItemCount() {
        return repositories.size() + 1;
    }

    public void clear() {
        repositories.clear();
        notifyDataSetChanged();
    }

    private void addRepositories(List<Repository> repositories) {
        this.repositories.addAll(repositories);
        notifyDataSetChanged();
    }

    public void submit(String query) {
        clear();

        responseSubject = BehaviorSubject.create(GitHub.client().searchRepositories(query, Sort.STARS, Order.DESC));
        subscription.add(ViewObservable.bindView(recyclerView, responseSubject)
                .flatMap(r -> r)
                .subscribe(r -> {
                    if (r.entity().getItems() == null || r.entity().getItems().isEmpty()) {
                        return;
                    }

                    List<Repository> repositories = r.entity().getItems();
                    addRepositories(repositories);

                    pagedResponse = r.next();
                }));
    }

    public void destroy() {
        subscription.unsubscribe();
    }
}