package org.openintents.alternativeapps.common;

import fr.xgouchet.androidlib.common.AbstractChangeLog;
import org.openintents.alternativeapps.R;

public class ChangeLog extends AbstractChangeLog {

	@Override
	public int getChangeLogResourceForVersion(final int version) {
		return R.string.release1_log;
	}

	@Override
	public int getTitleResourceForVersion(final int version) {
		return R.string.release1;
	}

}
