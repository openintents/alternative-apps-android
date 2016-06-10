package org.openintents.alternativeapps;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;
import org.openintents.alternativeapps.common.Constants;
import org.openintents.alternativeapps.common.Settings;

public class SecretPreferences extends Activity implements
		OnCheckedChangeListener {

	private ToggleButton mToggleButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_secret);

		mToggleButton = (ToggleButton) findViewById(android.R.id.toggle);
		mToggleButton.setOnCheckedChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences prefs;

		prefs = getSharedPreferences(Constants.PREFERENCES,
				Context.MODE_PRIVATE);
		Settings.updateFromPreferences(prefs);
		mToggleButton.setChecked(Settings.sEnableSecret);

	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		SharedPreferences prefs;

		prefs = getSharedPreferences(Constants.PREFERENCES,
				Context.MODE_PRIVATE);
		Settings.setEnableSecrets(prefs, isChecked);
	}

}
