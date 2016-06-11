package org.openintents.alternativeapps.packageinfo.ui.fragments;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

import org.openintents.alternativeapps.AboutActivity;
import org.openintents.alternativeapps.PreferencesActivity;
import org.openintents.alternativeapps.R;
import org.openintents.alternativeapps.common.Constants;
import org.openintents.alternativeapps.common.ManifestUtils;
import org.openintents.alternativeapps.common.PackageUtils;
import org.openintents.alternativeapps.common.Settings;
import org.openintents.alternativeapps.packageinfo.ui.PackagesActivity;
import org.openintents.alternativeapps.packageinfo.ui.adapter.PackageListAdapter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

@SuppressLint("DefaultLocale")
public class PackageListFragment extends Fragment implements
        OnItemClickListener, OnQueryTextListener {

    private class ExportTask extends AsyncTask<List<PackageInfo>, Integer, Void> {

        @Override
        protected Void doInBackground(List<PackageInfo>... arrayOfPackageLists) {
            List<PackageInfo> packages = arrayOfPackageLists[0];
            PackageManager pm = mActivity.getPackageManager();
            for (int i = 0, size = packages.size(); i < size; i++) {
                PackageInfo packageInfo = packages.get(i);
                if (isExportAllowed(packageInfo, pm)) {
                    try {
                        ManifestUtils.getManifest(packageInfo);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ParserConfigurationException e) {
                        e.printStackTrace();
                    }
                }
                publishProgress(i, size);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (getActivity() != null) {
                if (values[0] % 10 == 0) {
                    Toast.makeText(getActivity(),
                            getString(R.string.export_progress, values[0], values[1]), Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (getActivity() != null) {
                Toast.makeText(getActivity(), R.string.export_finished, Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * @see android.support.v4.app.Fragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        mActivity = (PackagesActivity) activity;
        mPackageManager = mActivity.getPackageManager();
    }

    /**
     * @see android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
     * android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container, final Bundle savedInstanceState) {

        View root = inflater
                .inflate(R.layout.layout_app_list, container, false);

        mListView = (ListView) root.findViewById(android.R.id.list);
        mListView.setEmptyView(root.findViewById(android.R.id.empty));
        mListView.setFastScrollEnabled(true);

        mPackages = new LinkedList<>();
        mAdapter = new PackageListAdapter(mActivity, mPackages);
        mListView.setAdapter(mAdapter);

        mListView.setOnCreateContextMenuListener(this);
        mListView.setOnItemClickListener(this);

        mListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);

        mSortMethod = Settings.sDefaultSortMethod;

        setHasOptionsMenu(true);

        return root;
    }

    /**
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = mActivity.getSharedPreferences(
                Constants.PREFERENCES, Context.MODE_PRIVATE);
        Settings.updateFromPreferences(prefs);

        new Thread(mRefreshRunnable).start();
    }

    /**
     * @see android.support.v4.app.Fragment#onCreateOptionsMenu(android.view.Menu,
     * android.view.MenuInflater)
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main, menu);

        // generate search view for the search menu
        SearchView search = new SearchView(getActivity());
        search.setQueryHint(getString(android.R.string.search_go));
        search.setOnQueryTextListener(this);

        menu.findItem(R.id.action_search).setActionView(search);
    }

    /**
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        boolean result = true;
        boolean refresh = false;

        switch (item.getItemId()) {
            case R.id.action_share:
                new android.support.v7.app.AlertDialog.Builder(getActivity())
                        .setTitle(R.string.title_start_export)
                        .setMessage(R.string.export_hint)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                executeExportTask();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create().show();
                break;
            case R.id.action_about:
                startActivity(new Intent(mActivity, AboutActivity.class));
                break;
            case R.id.action_sort_by_name:
                mSortMethod = Constants.SORT_BY_NAME;
                refresh = true;
                break;
            case R.id.action_sort_by_package:
                mSortMethod = Constants.SORT_BY_PACKAGE;
                refresh = true;
                break;
            case R.id.action_sort_by_install:
                mSortMethod = Constants.SORT_BY_INSTALL;
                refresh = true;
                break;
            case R.id.action_sort_by_update:
                mSortMethod = Constants.SORT_BY_UPDATE;
                refresh = true;
                break;
            case R.id.action_settings:
                startActivity(new Intent(mActivity,
                        PreferencesActivity.class));
                break;
            default:
                result = super.onOptionsItemSelected(item);
                break;
        }

        if (refresh) {
            mAdapter.clear();
            new Thread(mRefreshRunnable).start();
        }
        return result;
    }

    private void executeExportTask() {
        new ExportTask().execute(mPackages);
    }

    /**
     * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
     * android.view.View, android.view.ContextMenu.ContextMenuInfo)
     */
    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View view,
                                    final ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;

        mSelectedPackage = mPackages.get(info.position);

        mActivity.getMenuInflater().inflate(R.menu.package_context, menu);

        ApplicationInfo appInfo = mSelectedPackage.applicationInfo;

        if (appInfo != null) {
            int flags = appInfo.flags;
            if ((flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) {
                menu.setGroupEnabled(R.id.group_installed_apps, false);
            }
        }

        if (Settings.sEnableSecret) {
            mActivity.getMenuInflater().inflate(R.menu.secret, menu);
        }
    }

    /**
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        boolean res = true;

        switch (item.getItemId()) {
            case R.id.action_remove_from_list:
                mPackages.remove(mSelectedPackage);
                mAdapter.notifyDataSetChanged();
                break;
            case R.id.action_uninstall:
                startActivity(PackageUtils.uninstallPackageIntent(mSelectedPackage));
                break;
            case R.id.action_explore_resources:
                mActivity.browsePackageResources(mSelectedPackage);
                break;
            case R.id.action_export_manifest:
                PackageUtils.exportManifest(mActivity, mSelectedPackage);
                break;
            case R.id.action_display_info:
                startActivity(PackageUtils.applicationInfoIntent(mSelectedPackage));
                break;
            case R.id.action_display_store:
                startActivity(PackageUtils
                        .applicationPlayStoreIntent(mSelectedPackage));
                break;
            case R.id.action_export_apk:
                PackageUtils.exportAPK(mActivity, mSelectedPackage);
                break;
            default:
                res = super.onContextItemSelected(item);
                break;
        }
        return res;
    }

    /**
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView,
     * android.view.View, int, long)
     */
    @Override
    public void onItemClick(final AdapterView<?> parent, final View view,
                            final int position, final long id) {
        final PackageInfo info = mAdapter.getItem(position);
        mActivity.showPackageInfo(info);

    }

    /**
     * @see android.widget.SearchView.OnQueryTextListener#onQueryTextChange(java.lang.String)
     */
    @Override
    public boolean onQueryTextChange(final String newText) {
        mAdapter.setQuery(newText.toLowerCase());
        return true;
    }

    /**
     * @see android.widget.SearchView.OnQueryTextListener#onQueryTextSubmit(java.lang.String)
     */
    @Override
    public boolean onQueryTextSubmit(final String query) {
        int index = mListView.getFirstVisiblePosition();
        index = findNextPackageWithQuery(query.toLowerCase(), index);

        if (index >= 0) {
            mListView.setSelection(index);
        } else {
            Snackbar.make(mActivity.findViewById(android.R.id.content),
                    "No package contains \"" + query + "\"", Snackbar.LENGTH_LONG)
                    .show();
        }

        return true;
    }

    /**
     * Refresh the list of packages, filter, sort and display it
     */
    private void refreshData() {
        final PackageManager pm = mActivity.getPackageManager();
        final List<PackageInfo> packages = pm.getInstalledPackages(0);

        filterPackages(packages);

        Collections.sort(packages, Constants.getComparator(pm, mSortMethod));

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPackages.clear();
                mPackages.addAll(packages);
                mAdapter.setSortMethod(mSortMethod);
                mAdapter.notifyDataSetChanged();
            }
        });
    }


    private boolean isExportAllowed(PackageInfo packageInfo, PackageManager pm) {
        return pm.getInstallerPackageName(packageInfo.packageName) != null;
    }

    /**
     * Filter the Packages based on the settings
     */
    private void filterPackages(final List<PackageInfo> packages) {
        int flags;
        ApplicationInfo appInfo;
        List<PackageInfo> remove = new LinkedList<>();

        // filter
        for (PackageInfo pkg : packages) {
            appInfo = pkg.applicationInfo;

            if (appInfo != null) {
                flags = appInfo.flags;
                if (!isExportAllowed(pkg, mPackageManager) || (Settings.sIgnoreSystemPackages &&
                        (flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)) {
                    remove.add(pkg);
                }
            }
        }

        packages.removeAll(remove);
    }

    private int findNextPackageWithQuery(final String query,
                                         final int searchIndex) {
        int index = -1, pos, count;
        String name, pkg;
        PackageInfo info;

        count = mPackages.size();
        for (int i = 0; i < count; i++) {
            pos = (i + searchIndex + 1) % count;
            info = mPackages.get(pos);

            pkg = info.packageName.toLowerCase();
            if (pkg.contains(query)) {
                index = pos;
                break;
            }

            name = mPackageManager.getApplicationLabel(info.applicationInfo)
                    .toString().toLowerCase();
            if (name.contains(query)) {
                index = pos;
                break;
            }
        }

        return index;
    }

    /**
     * Runnable used to list all packages in background
     */
    private Runnable mRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshData();
        }
    };

    protected boolean mActivateOnClick;
    protected int mSortMethod;
    protected PackagesActivity mActivity;
    protected PackageInfo mSelectedPackage;
    protected List<PackageInfo> mPackages;
    protected ListView mListView;
    protected PackageListAdapter mAdapter;

    protected PackageManager mPackageManager;

}
