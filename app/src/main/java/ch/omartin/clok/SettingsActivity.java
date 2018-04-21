package ch.omartin.clok;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 * settings activity
 */
public class SettingsActivity extends AppCompatActivity
{
	public static final String KEY_PREF_HOUR_MODE = "hour_mode";
	public static final String KEY_PREF_INVERT_COLORS = "invert_colors";
	public static final String KEY_PREF_DISPLAY_SECONDS = "display_seconds";
	public static final String KEY_PREF_DISPLAY_MINUTES = "display_minutes";
	public static final String KEY_PREF_DISPLAY_HOURS = "display_hours";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		getSupportFragmentManager().beginTransaction()
				.replace(android.R.id.content, new SettingsFragment())
				.commit();
	}
}
