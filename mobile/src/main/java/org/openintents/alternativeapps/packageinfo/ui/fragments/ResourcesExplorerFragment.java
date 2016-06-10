package org.openintents.alternativeapps.packageinfo.ui.fragments;

import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import java.io.File;

import fr.xgouchet.androidlib.data.FileUtils;
import org.openintents.alternativeapps.R;
import org.openintents.alternativeapps.common.Constants;
import org.openintents.alternativeapps.packageinfo.AsyncResourcesExtractor;
import org.openintents.alternativeapps.packageinfo.AsyncResourcesExtractor.ResourcesExtractorListener;
import org.openintents.alternativeapps.packageinfo.ui.adapter.ResourcesAdapter;

public class ResourcesExplorerFragment extends Fragment implements
        ResourcesExtractorListener {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container, final Bundle savedInstanceState) {
        // build UI
        View root = inflater.inflate(R.layout.layout_resources, container,
                false);
        mResourcesListView = (ExpandableListView) root
                .findViewById(android.R.id.list);
        mResourcesListView.setEmptyView(root.findViewById(android.R.id.empty));

        setHasOptionsMenu(true);

        // Start loading resources
        PackageInfo info = getArguments().getParcelable(
                Constants.EXTRA_PACKAGE_INFO);
        AsyncResourcesExtractor extractor;
        extractor = new AsyncResourcesExtractor(getActivity(), this);
        extractor.execute(info);

        String appLabel = getActivity().getPackageManager()
                .getApplicationLabel(info.applicationInfo).toString();
        getActivity().setTitle(
                getActivity().getString(R.string.title_package, appLabel));
        return root;
    }

    /**
     * @see android.support.v4.app.Fragment#onCreateOptionsMenu(android.view.Menu,
     * android.view.MenuInflater)
     */
    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.resources, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        boolean res = true;
        switch (item.getItemId()) {
            case R.id.action_switch_background:
                mAdapter.switchBackground();
                break;
            case R.id.action_zoom_in:
                mAdapter.zoomIn();
                break;
            case R.id.action_zoom_out:
                mAdapter.zoomOut();
                break;
            case R.id.action_zoom_reset:
                mAdapter.zoomReset();
                break;
            default:
                res = super.onOptionsItemSelected(item);
                break;
        }

        return res;
    }

    /**
     * @see AsyncResourcesExtractor.ResourcesExtractorListener#onExtractionError(java.lang.Exception)
     */
    @Override
    public void onExtractionError(final Exception exception) {
        Snackbar.make(getActivity().findViewById(android.R.id.content),
                "Unable to retrieve package resources", Snackbar.LENGTH_LONG)
                .show();

        getActivity().getSupportFragmentManager().popBackStack();
    }

    /**
     * @see AsyncResourcesExtractor.ResourcesExtractorListener#onResourcesExctracted(java.io.File)
     */
    @Override
    public void onResourcesExctracted(final File file) {
        mFolder = new File(file, "res");

        mAdapter = new ResourcesAdapter(getActivity(), mFolder);
        mResourcesListView.setAdapter(mAdapter);
    }

    /**
     * @see android.support.v4.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        FileUtils.deleteRecursiveFolder(mFolder);
    }

    private ExpandableListView mResourcesListView;
    private ResourcesAdapter mAdapter;
    private File mFolder;
}
