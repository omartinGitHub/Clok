package ch.omartin.clok;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Olivier Martin on 14.03.2018.
 */
public class ClokView extends View
{
	private Paint clockPaint;
	private Paint tickPaint;
	private Paint timePaint;
	private Paint secondsPaint;

	private final int padding = 30;
	private final int hoursTickSize = 40;
	private final int minutesTickSize = 20;
	private final int centerRadius = 30;
	private final int secondsRadius = 20;
	private final int syncDelay = 60_000;
	private final int drawDelay = 1000;
	private final float strokeWidth = 5.0f;

	private final SimpleDateFormat formatter = new SimpleDateFormat("H:mm:ss dd.MM.yyyy", Locale.getDefault());

	private TickMode tickMode = TickMode.MODE_12;
	private boolean isHourHandDrawn = true;
	private boolean isMinuteHandDrawn = true;
	private boolean isSecondHandDrawn = true;

	private volatile Calendar calendar;
	private volatile int hours;
	private volatile int minutes;
	private volatile int seconds;

	public ClokView(Context context, AttributeSet attributeSet)
	{
		super(context, attributeSet);

		init();
	}

	private void init()
	{
		int color = Color.BLACK;
		int textSize = getResources().getDimensionPixelSize(R.dimen.fontSize);

		this.clockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.clockPaint.setColor(color);
		this.clockPaint.setStyle(Paint.Style.STROKE);
		this.clockPaint.setStrokeWidth(this.strokeWidth);
		this.tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.tickPaint.setColor(color);
		this.tickPaint.setStrokeWidth(this.strokeWidth);
		this.secondsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.secondsPaint.setColor(color);
		this.secondsPaint.setAlpha(128);
		this.timePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.timePaint.setColor(color);
		this.timePaint.setTextSize(textSize);

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
		Boolean modePrefs = sharedPref.getBoolean(SettingsActivity.KEY_PREF_HOUR_MODE, false);
		this.isSecondHandDrawn = sharedPref.getBoolean(SettingsActivity.KEY_PREF_DISPLAY_SECONDS, this.isSecondHandDrawn);
		this.isMinuteHandDrawn = sharedPref.getBoolean(SettingsActivity.KEY_PREF_DISPLAY_MINUTES, this.isMinuteHandDrawn);
		this.isHourHandDrawn = sharedPref.getBoolean(SettingsActivity.KEY_PREF_DISPLAY_HOURS, this.isHourHandDrawn);

		if(modePrefs)
		{
			this.tickMode = TickMode.MODE_24;
		}
		else
		{
			this.tickMode = TickMode.MODE_12;
		}

		final Handler syncHandler = new Handler();
		final AbstractRunnable syncRunnableCode = new SyncRunnable(syncHandler, ClokView.this.syncDelay);

		// post
		syncRunnableCode.postRunnable();

		final Handler drawHandler = new Handler();
		final AbstractRunnable drawRunnable = new DrawRunnable(drawHandler, ClokView.this.drawDelay);

		// Start the initial runnable task by posting through the handler
		drawRunnable.postRunnable();
	}

	/**
	 * sync with internal clock
	 */
	private synchronized void synchronize()
	{
		// use this date for the next cycle until next sync.
		this.calendar = Calendar.getInstance(Locale.getDefault());
		this.hours = calendar.get(Calendar.HOUR);
		this.minutes = calendar.get(Calendar.MINUTE);
		this.seconds = calendar.get(Calendar.SECOND);
		final Date date = calendar.getTime();
		Log.d("sync ", this.formatter.format(date));
	}

	/**
	 * increment seconds and maybe the rest
	 */
	private synchronized void increment(final int nbrHoursTicks)
	{
		this.seconds++;

		if(this.seconds >= 60)
		{
			this.seconds = 0;
			this.minutes++;

			if(this.minutes >= 60)
			{
				this.minutes = 0;
				this.hours++;

				if(this.hours >= nbrHoursTicks)
				{
					this.hours = 0;
				}
			}
		}
	}

	@Override
	protected void onDraw(final Canvas canvas)
	{
		super.onDraw(canvas);

		drawPerimeter(canvas);
		drawCenter(canvas);
		drawBackground(canvas);
		drawTicks(canvas, this.tickMode);
		drawArms(canvas, this.tickMode);
		drawTime(canvas);
	}

	/**
	 * draw clock perimeter
	 * @param canvas where to draw
	 */
	private void drawPerimeter(final Canvas canvas)
	{
		int[] center = getCenter();
		int radius = getRadius();

		canvas.drawCircle(center[0], center[1], radius, this.clockPaint);
	}

	/**
	 * draw center at the center
	 * @param canvas where to draw
	 */
	private void drawCenter(final Canvas canvas)
	{
		int[] center = getCenter();

		canvas.drawCircle(center[0], center[1], this.centerRadius, this.clockPaint);
	}

	/**
	 * draw clock background
	 * @param canvas where to draw
	 */
	private void drawBackground(final Canvas canvas)
	{
		// TODO
	}

	/**
	 * draw ticks somewhere
	 * @param canvas where to draw
	 * @param tickMode 12 or 24 hour mode
	 */
	private void drawTicks(final Canvas canvas, final TickMode tickMode)
	{
		int radius = getRadius();
		int nbrHoursTicks = tickMode.getNbrHoursTicks();
		float degreesPerTick = 360.0f / nbrHoursTicks;
		int[] center = getCenter();
		float startX = 0;
		float startY = 0 - radius;
		float endX = startX;
		float endY = startY + this.hoursTickSize;

		// hours
		for(int i=0; i<nbrHoursTicks; i++)
		{
			float degrees = i * degreesPerTick;
			canvas.save();
			canvas.translate(center[0], center[1]);
			canvas.rotate(degrees);
			canvas.drawLine(startX, startY, endX, endY, this.tickPaint);
			canvas.restore();
		}

		int nbrMinutesTicks = tickMode.getNbrMinutesTicks();
		degreesPerTick = 360.0f / nbrMinutesTicks;
		endY = startY + this.minutesTickSize;

		// minutes
		for(int i=0; i<nbrMinutesTicks; i++)
		{
			float degrees = i * degreesPerTick;
			canvas.save();
			canvas.translate(center[0], center[1]);
			canvas.rotate(degrees);
			canvas.drawLine(startX, startY, endX, endY, this.tickPaint);
			canvas.restore();
		}
	}

	/**
	 * draw hours/minutes/seconds somewhere
	 * @param canvas where to draw
	 * @param tickMode 12 or 24 hour mode
	 */
	private void drawArms(final Canvas canvas, final TickMode tickMode)
	{
		int nbrHoursTicks = tickMode.getNbrHoursTicks();
		int radius = getRadius();
		int[] center = getCenter();
		int centerX = center[0];
		int centerY = center[1];
		int endX;
		int endY;
		float degrees;

//		Log.d("draw time", hours + " : " + minutes + " : " + seconds);

		canvas.save();
		canvas.translate(centerX, centerY);

		// hours
		if(this.isHourHandDrawn)
		{
			degrees = ((float) hours / nbrHoursTicks) * 360;
			endX = 0;
			endY = radius / 2;
			canvas.rotate(degrees);
			canvas.drawLine(0, -this.centerRadius, endX, -endY, this.tickPaint);
			canvas.rotate(-degrees);
		}

		// minutes
		if(this.isMinuteHandDrawn)
		{
			degrees = (minutes / 60.0f) * 360;
			endX = 0;
			endY = (int) (radius * 0.75);
			canvas.rotate(degrees);
			canvas.drawLine(0, -this.centerRadius, endX, -endY, this.tickPaint);
			canvas.rotate(-degrees);
		}

		//seconds
		if(this.isSecondHandDrawn)
		{
			degrees = (seconds / 60.0f) * 360;
			endX = 0;
			endY = radius;
			canvas.rotate(degrees);
			canvas.drawCircle(0, -endY, this.secondsRadius, this.secondsPaint);
			//		canvas.drawLine(0, 0, endX, -endY, this.tickPaint);
		}

		canvas.restore();
		increment(nbrHoursTicks);
	}

	/**
	 * draw time somewhere
	 * @param canvas where to draw
	 */
	private void drawTime(final Canvas canvas)
	{
		int radius = getRadius();
		int[] center = getCenter();

		if(this.calendar != null)
		{
			Rect bounds = new Rect();
			Calendar calendar = Calendar.getInstance(Locale.getDefault());
			Date date = calendar.getTime();
			String text = this.formatter.format(date);
			this.timePaint.getTextBounds(text, 0, text.length(), bounds);
			float x = center[0] - (bounds.width() / 2);
			float y = center[1] + (radius / 2.0f);

			canvas.drawText(text, x, y, this.timePaint);
		}
	}

	@NonNull
	private int[] getCenter()
	{
		int width = getWidth();
		int height = getHeight();
		int x = width / 2;
		int y = height / 2;

		return new int[] {x,y};
	}

	private int getRadius()
	{
		int width = getWidth();
		int height = getHeight();
		int radius = (Math.min(width, height) / 2) - this.padding;

		return radius;
	}

	/**
	 * abstract runnable
	 */
	private abstract class AbstractRunnable implements Runnable
	{
		final Handler handler;
		final int delay;

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
			boolean result = false;

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

	/**
	 * just calls invalidate()
	 */
	private class DrawRunnable extends AbstractRunnable
	{
		DrawRunnable(Handler handler, int delay)
		{
			super(handler, delay);
		}

		@Override
		public void run()
		{
			// redraw
			invalidate();
			// Repeat this the same runnable code block again another delay
			postRunnable(true);
		}
	}

	/**
	 * sync with internal clock
	 */
	private class SyncRunnable extends AbstractRunnable
	{
		SyncRunnable(Handler handler, int delay)
		{
			super(handler, delay);
		}

		@Override
		public void run()
		{
			synchronize();
			// post with delay
			postRunnable(true);
		}
	}
}
