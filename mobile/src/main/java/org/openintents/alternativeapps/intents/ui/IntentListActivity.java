package org.openintents.alternativeapps.intents.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.openintents.alternativeapps.AboutActivity;
import org.openintents.alternativeapps.PreferencesActivity;
import org.openintents.alternativeapps.R;
import org.openintents.alternativeapps.common.FirebaseRecyclerAdapter;
import org.openintents.alternativeapps.intents.model.IntentSpecification;
import org.openintents.alternativeapps.packageinfo.ui.PackagesActivity;

/**
 * An activity representing a list of Intents. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link IntentDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class IntentListActivity extends AppCompatActivity {

    private static final String TAG = "IntentListActivity";
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intent_list);

        View recyclerView = findViewById(R.id.intent_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);

        if (findViewById(R.id.intent_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.intents, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                startActivity(new Intent(this, PackagesActivity.class));
                return true;
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReferenceFromUrl("https://oi-apps-fm.firebaseio.com/").child("specification");
        FirebaseRecyclerAdapter<IntentSpecification, IntentSpecViewHolder> adapter = new FirebaseRecyclerAdapter<IntentSpecification, IntentSpecViewHolder>(IntentSpecification.class, R.layout.intent_list_content, IntentSpecViewHolder.class, ref) {
            public void populateViewHolder(final IntentSpecViewHolder holder, IntentSpecification spec, int position) {
                holder.mItem = spec;
                holder.actionView.setText(holder.mItem.action);
                holder.titleView.setText(holder.mItem.title);

                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mTwoPane) {
                            Bundle arguments = new Bundle();
                            arguments.putString(IntentDetailFragment.ARG_ACTION, holder.mItem.action);
                            IntentDetailFragment fragment = new IntentDetailFragment();
                            fragment.setArguments(arguments);
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.intent_detail_container, fragment)
                                    .commit();
                        } else {
                            Context context = v.getContext();
                            Intent intent = new Intent(context, IntentDetailActivity.class);
                            intent.putExtra(IntentDetailFragment.ARG_ACTION, holder.mItem.action);

                            context.startActivity(intent);
                        }
                    }
                });
            }
        };
        //adapter = new SimpleItemRecyclerViewAdapter(IntentSpecification.POPULAR);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                       RecyclerView.State state) {
                outRect.bottom = 48;
            }
        });
    }

    private static class IntentSpecViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView actionView;
        public final TextView titleView;
        public IntentSpecification mItem;

        public IntentSpecViewHolder(View view) {
            super(view);
            mView = view;
            actionView = (TextView) view.findViewById(R.id.action);
            titleView = (TextView) view.findViewById(R.id.title);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + titleView.getText() + "'";
        }
    }
}
