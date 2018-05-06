package ch.omartin.clok;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * main activity
 */
public class MainActivity extends AppCompatActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(myToolbar);

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		Log.d("menu selected ", item.toString());

		switch (item.getItemId())
		{
			case R.id.action_settings:
				// User chose the "Settings" item, show the app settings UI...
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				return true;
			case R.id.action_about:
				String versionName = BuildConfig.VERSION_NAME;
				int versionCode = BuildConfig.VERSION_CODE;
				String message = getString(R.string.version_text) + " : " + versionName + " " + versionCode;
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(message).setTitle(R.string.about_settings);
				AlertDialog dialog = builder.create();
				dialog.show();
				return true;
			default:
				// If we got here, the user's action was not recognized.
				// Invoke the superclass to handle it.
				return super.onOptionsItemSelected(item);
		}
	}
}
