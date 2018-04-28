package ch.omartin.clok;

import android.os.Handler;
import android.util.Log;

/**
 * parent class of runnables
 */
abstract class AbstractRunnable implements Runnable
{
	private final Handler handler;
	private final int delay;

	AbstractRunnable(Handler handler, int delay)
	{
		this.handler = handler;
		this.delay = delay;
	}

	void postRunnable()
	{
		postRunnable(false);
	}

	void postRunnable(boolean delay)
	{
		boolean result;

		if(!delay)
		{
			result = handler.post(this);
		}
		else
		{
			result = handler.postDelayed(this, this.delay);
		}

		if(!result)
		{
			Log.e("runnable result", "could not be posted");
		}
	}
}
