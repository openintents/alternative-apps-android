package org.openintents.alternativeapps.packageinfo.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.openintents.alternativeapps.R;
import org.openintents.alternativeapps.common.ChangeLog;
import org.openintents.alternativeapps.common.Constants;
import org.openintents.alternativeapps.common.PackageUtils;
import org.openintents.alternativeapps.common.Settings;
import org.openintents.alternativeapps.packageinfo.ui.fragments.PackageInfoFragment;
import org.openintents.alternativeapps.packageinfo.ui.fragments.PackageListFragment;
import org.openintents.alternativeapps.packageinfo.ui.fragments.ResourcesExplorerFragment;

public class PackagesActivity extends AppCompatActivity implements
		OnBackStackChangedListener {

	// UI
	private PackageListFragment mListFragment;

	private boolean mIsTwoPaned;

	/**
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup UI
		setContentView(R.layout.layout_main);

		// Fragmented UI : phone vs tablets
		if (findViewById(R.id.tabletFragmentContainer) == null) {
			mIsTwoPaned = false;
			mListFragment = new PackageListFragment();
			final FragmentTransaction transaction = getSupportFragmentManager()
					.beginTransaction();
			transaction.add(R.id.phoneFragmentContainer, mListFragment,
					Constants.TAG_FRAGMENT_LIST);
			transaction.commit();
		} else {
			mIsTwoPaned = true;
			mListFragment = (PackageListFragment) getSupportFragmentManager()
					.findFragmentById(R.id.packageListFragment);
		}

		getSupportFragmentManager().addOnBackStackChangedListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();

		ChangeLog changeLog;
		SharedPreferences prefs;

		changeLog = new ChangeLog();
		prefs = getSharedPreferences(Constants.PREFERENCES,
				Context.MODE_PRIVATE);
		Settings.updateFromPreferences(prefs);

		changeLog.displayChangeLog(this, prefs);
	}

	@Override
	protected void onResume() {
		super.onResume();

		FragmentManager fragMgr = getSupportFragmentManager();

		PackageInfoFragment infoFragment = (PackageInfoFragment) fragMgr
				.findFragmentByTag(Constants.TAG_FRAGMENT_DETAILS);

		if (infoFragment != null) {

			if (!PackageUtils.isPackageInstalled(this,
					infoFragment.getPackageInfo())) {

				BackStackEntry top = fragMgr.getBackStackEntryAt(0);
				if (Constants.TAG_FRAGMENT_DETAILS.equals(top.getName())) {
					fragMgr.popBackStack();
				} else {
					final FragmentTransaction transaction = fragMgr
							.beginTransaction();
					transaction.remove(infoFragment);
					transaction.commit();
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
	}

	@Override
	public void onBackStackChanged() {

		boolean hasBackStack = (getSupportFragmentManager()
				.getBackStackEntryCount() > 0);
		getSupportActionBar().setDisplayHomeAsUpEnabled(hasBackStack && !mIsTwoPaned);

	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		boolean res = true;

		switch (item.getItemId()) {
		case android.R.id.home:
			getSupportFragmentManager().popBackStack();
			break;
		default:
			res = super.onOptionsItemSelected(item);
			break;
		}

		return res;
	}

	public void browsePackageResources(final PackageInfo info) {

		int containerId;
		Bundle args = new Bundle();
		args.putParcelable(Constants.EXTRA_PACKAGE_INFO, info);

		final Fragment resources = new ResourcesExplorerFragment();
		resources.setArguments(args);

		final FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();

		if (mIsTwoPaned) {
			containerId = R.id.tabletFragmentContainer;
		} else {
			containerId = R.id.phoneFragmentContainer;
			transaction.addToBackStack(Constants.TAG_FRAGMENT_RESOURCES);
		}

		transaction.replace(containerId, resources,
				Constants.TAG_FRAGMENT_RESOURCES);
		transaction.commit();

	}

	/**
	 * 
	 * @param arguments
	 */
	public void showPackageInfo(final Bundle arguments) {
		int containerId;
		final Fragment details = new PackageInfoFragment();
		details.setArguments(arguments);

		final FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();

		if (mIsTwoPaned) {
			containerId = R.id.tabletFragmentContainer;
		} else {
			containerId = R.id.phoneFragmentContainer;
			transaction.addToBackStack(Constants.TAG_FRAGMENT_DETAILS);
		}

		transaction.replace(containerId, details,
				Constants.TAG_FRAGMENT_DETAILS);
		transaction.commit();

	}

	public void showPackageInfo(final PackageInfo info) {
		PackageInfo fullInfo = PackageUtils.getFullPackageInfo(this, info);

		Bundle args = new Bundle();
		args.putParcelable(Constants.EXTRA_PACKAGE_INFO, fullInfo);
		showPackageInfo(args);
	}

}
