package org.openintents.alternativeapps.intents.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.openintents.alternativeapps.R;
import org.openintents.alternativeapps.common.ManifestUtils;
import org.openintents.alternativeapps.common.RepositoryUtils;
import org.openintents.alternativeapps.intents.model.IntentSpecification;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A fragment representing a single Intent detail screen.
 * This fragment is either contained in a {@link IntentListActivity}
 * in two-pane mode (on tablets) or a {@link IntentDetailActivity}
 * on handsets.
 */
public class IntentDetailFragment extends android.support.v4.app.Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";
    private static final String TAG = "IntentDetailFragment";

    private RecyclerView packageListView;
    private SimpleItemRecyclerViewAdapter adapter;
    private TextView intentTitleView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public IntentDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.intent_detail, container, false);
        intentTitleView = (TextView) rootView.findViewById(R.id.intent_title);
        packageListView = (RecyclerView) rootView.findViewById(R.id.package_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(rootView.getContext(), LinearLayoutManager.VERTICAL, false);
        packageListView.setLayoutManager(layoutManager);
        adapter = new SimpleItemRecyclerViewAdapter();
        packageListView.setAdapter(adapter);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            String action = getArguments().getString(ARG_ITEM_ID);
            IntentSpecification spec = getSpecForAction(action);
            if (spec != null) {
                updateUI(spec);
            }
        }
        return rootView;
    }

    private void updateUI(IntentSpecification spec) {
        intentTitleView.setText(spec.title);
        loadAlternativeAppsFor(spec.asIntent());
    }

    private IntentSpecification getSpecForAction(String action) {
        for (IntentSpecification intentSpecification : IntentSpecification.POPULAR) {
            if (intentSpecification.action.equals(action)) {
                return intentSpecification;
            }
        }
        DatabaseReference ref = FirebaseDatabase.getInstance().getReferenceFromUrl("https://oi-apps-fm.firebaseio.com/");
        ref.child("specification").child(RepositoryUtils.sanitizeForFirebasePath(action)).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        IntentSpecification specification = dataSnapshot.getValue(IntentSpecification.class);
                        updateUI(specification);
                    }

                    @Override
                    public void onCancelled(DatabaseError firebaseError) {

                    }
                });

        return null;
    }

    public void loadAlternativeAppsFor(Intent intent) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReferenceFromUrl("https://oi-apps-fm.firebaseio.com/");
        ref.child("action").child(RepositoryUtils.sanitizeForFirebasePath(intent.getAction())).child("packages").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Object intents = dataSnapshot.getValue();
                        Log.d(TAG, "onDataChange: " + intents);
                        if (intents != null) {
                            adapter.setValues(convertToPackageInfo((Map<String, Object>) intents));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG, "onCancelled: " + databaseError);
                    }
                });
    }

    private List<ManifestUtils.SparsePackageInfo> convertToPackageInfo(Map<String, Object> intents) {
        List<ManifestUtils.SparsePackageInfo> packageList = new ArrayList<>();
        Iterator<String> keys = intents.keySet().iterator();
        while (keys.hasNext()){
            String pathForPackage = keys.next();
            Map<String, Object> packageInfo = (Map<String, Object>) intents.get(pathForPackage);
            ManifestUtils.SparsePackageInfo sparsePackageInfo = new ManifestUtils.SparsePackageInfo();
            String packageName = (String) packageInfo.get("packageName");
            sparsePackageInfo.setPackage(packageName);
            packageList.add(sparsePackageInfo);
        }
        return packageList;
    }

    private static class PackageViewHolder extends RecyclerView.ViewHolder {
        public ManifestUtils.SparsePackageInfo packageInfo;
        public TextView packageNameView;

        public PackageViewHolder(View itemView) {
            super(itemView);
            packageNameView = (TextView) itemView.findViewById(R.id.id);
        }
    }

    private static class SimpleItemRecyclerViewAdapter extends RecyclerView.Adapter<PackageViewHolder> {

        List<ManifestUtils.SparsePackageInfo> values = new ArrayList<>();

        @Override
        public PackageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.intent_detail_content, parent, false);
            return new PackageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PackageViewHolder holder, int position) {
            holder.packageInfo = values.get(position);
            holder.packageNameView.setText(values.get(position).getPackageName());
        }

        @Override
        public int getItemCount() {
            return values.size();
        }

        public void setValues(List<ManifestUtils.SparsePackageInfo> sparsePackageInfos) {
            int oldSize = values.size();
            values.clear();
            values.addAll(sparsePackageInfos);
            notifyItemRangeRemoved(0, oldSize);
            notifyItemRangeInserted(0, values.size());
        }
    }
}
