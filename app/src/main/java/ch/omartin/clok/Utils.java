package ch.omartin.clok;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public class Utils
{
	public static final String getVersion(Activity activity)
	{
		String versionName = "";
		int versionCode = -1;

		try
		{
			PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
			versionName = info.versionName;
			versionCode = info.versionCode;
		}
		catch(PackageManager.NameNotFoundException nex)
		{
			Log.e("get package info", nex.getMessage(), nex);
		}

		return versionName + " " + versionCode;
	}
}
