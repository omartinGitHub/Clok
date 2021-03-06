package ch.omartin.clok;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
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
	private Paint backgroundPaint;
	private Paint clockPaint;
	private Paint tickPaint;
	private Paint timePaint;
	private Paint secondsPaint;
	private Paint lightStatusPaint;

	private final int padding = 30;
	private final int hoursTickSize = 40;
	private final int minutesTickSize = 20;
	private final int centerRadius = 30;
	private final int secondsRadius = 20;
	private final int secondDelay = 1000;
	private final int minuteDelay = 60_000;
	private final float strokeWidth = 5.0f;

	private int color = Color.BLACK;
	private int backgroundColor = Color.WHITE;

	private final String datePattern = "dd.MM.yyyy";
	private final String timePattern = "H:mm:ss";
	private final String timeWithoutSecondsPattern = "H:mm";
	private final Locale locale = Locale.getDefault();
	private final SimpleDateFormat dateFormatter = new SimpleDateFormat(this.datePattern, this.locale);
	private final SimpleDateFormat timeFormatter = new SimpleDateFormat(this.timePattern, this.locale);
	private final SimpleDateFormat timeWithoutSecondsFormatter = new SimpleDateFormat(this.timeWithoutSecondsPattern, this.locale);
	private final SimpleDateFormat formatter = new SimpleDateFormat(this.datePattern + " " + this.timePattern, this.locale);

	private TickMode tickMode = TickMode.MODE_12;
	private boolean isInvertColors = false;
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
		Log.d("version code", String.valueOf(BuildConfig.VERSION_CODE));
		Log.d("version name", BuildConfig.VERSION_NAME);

		int textSize = getResources().getDimensionPixelSize(R.dimen.fontSize);

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
		Boolean modePrefs = sharedPref.getBoolean(SettingsActivity.KEY_PREF_HOUR_MODE, false);
		this.isInvertColors = sharedPref.getBoolean(SettingsActivity.KEY_PREF_INVERT_COLORS, this.isInvertColors);
		this.isSecondHandDrawn = sharedPref.getBoolean(SettingsActivity.KEY_PREF_DISPLAY_SECONDS, this.isSecondHandDrawn);
		this.isMinuteHandDrawn = sharedPref.getBoolean(SettingsActivity.KEY_PREF_DISPLAY_MINUTES, this.isMinuteHandDrawn);
		this.isHourHandDrawn = sharedPref.getBoolean(SettingsActivity.KEY_PREF_DISPLAY_HOURS, this.isHourHandDrawn);
		int syncDelay = this.minuteDelay;
		int drawDelay = this.secondDelay;

		if(modePrefs)
		{
			this.tickMode = TickMode.MODE_24;
		}
		else
		{
			this.tickMode = TickMode.MODE_12;
		}

		if(this.isInvertColors)
		{
			this.color = Color.WHITE;
			this.backgroundColor = Color.BLACK;
		}

		if(!this.isSecondHandDrawn)
		{
			drawDelay = this.minuteDelay;
		}

		this.backgroundPaint = new Paint();
		this.backgroundPaint.setColor(this.backgroundColor);
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
		this.lightStatusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.lightStatusPaint.setStyle(Paint.Style.FILL);

		final Handler syncHandler = new Handler();
		final AbstractRunnable syncRunnableCode = new SyncRunnable(syncHandler, syncDelay);

		// post
		syncRunnableCode.postRunnable();

		final Handler drawHandler = new Handler();
		final AbstractRunnable drawRunnable = new DrawRunnable(drawHandler, drawDelay);

		// Start the initial runnable task by posting through the handler
		drawRunnable.postRunnable();
	}

	/**
	 * sync with internal clock
	 */
	private synchronized void synchronize()
	{
		// use this date for the next cycle until next sync.
		this.calendar = Calendar.getInstance();
		this.minutes = calendar.get(Calendar.MINUTE);
		this.seconds = calendar.get(Calendar.SECOND);
		final Date date = calendar.getTime();

		if(tickMode == TickMode.MODE_24)
		{
			this.hours = calendar.get(Calendar.HOUR_OF_DAY);
		}
		else
        {
            this.hours = calendar.get(Calendar.HOUR);
        }

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

		drawBackground(canvas);
		drawLightStatus(canvas, this.tickMode);
		drawPerimeter(canvas);
		drawCenter(canvas);
		drawTicks(canvas, this.tickMode);
		drawHands(canvas, this.tickMode);
		drawNumbers(canvas, this.tickMode);
		drawTime(canvas);
	}

	/**
	 * draw clock background
	 * @param canvas where to draw
	 */
	private void drawBackground(final Canvas canvas)
	{
		canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), this.backgroundPaint);
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

		this.clockPaint.setStyle(Paint.Style.FILL);
		canvas.drawCircle(center[0], center[1], this.centerRadius, this.clockPaint);
		this.clockPaint.setStyle(Paint.Style.STROKE);
	}

	/**
	 *
	 * @param canvas where to draw
	 * @param tickMode 12 or 24 hour mode
	 */
	private void drawLightStatus(final Canvas canvas, final TickMode tickMode)
	{
		if(tickMode == TickMode.MODE_24)
		{
			// night
			this.lightStatusPaint.setColor(Color.BLACK);
			this.lightStatusPaint.setAlpha(128);
			fillHours(canvas, this.lightStatusPaint, tickMode, 0, 7);
			fillHours(canvas, this.lightStatusPaint, tickMode, 21, 24);

			// day
			this.lightStatusPaint.setColor(Color.WHITE);
			this.lightStatusPaint.setAlpha(128);
			fillHours(canvas, this.lightStatusPaint, tickMode, 7, 21);
		}
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

		canvas.save();
		canvas.translate(center[0], center[1]);

		// hours
		for(int i=0; i<nbrHoursTicks; i++)
		{
			canvas.rotate(degreesPerTick);
			canvas.drawLine(startX, startY, endX, endY, this.tickPaint);
		}

		canvas.restore();

		int nbrMinutesTicks = tickMode.getNbrMinutesTicks();
		degreesPerTick = 360.0f / nbrMinutesTicks;
		endY = startY + this.minutesTickSize;

		canvas.save();
		canvas.translate(center[0], center[1]);

		// minutes
		for(int i=0; i<nbrMinutesTicks; i++)
		{
			canvas.rotate(degreesPerTick);
			canvas.drawLine(startX, startY, endX, endY, this.tickPaint);
		}

		canvas.restore();
	}

	/**
	 *
	 * @param canvas where to draw
	 * @param tickMode 12 or 24 hour mode
	 */
	private void drawNumbers(final Canvas canvas, final TickMode tickMode)
	{
		int[] center = getCenter();
		int radius = getRadius();

		// TODO
		int nbrHoursTicks = tickMode.getNbrHoursTicks();
		float degreesPerTick = 360.0f / nbrHoursTicks;
		float x = 0;
		float y = 0 - (radius * 0.75f);
		float textSize = this.tickPaint.getTextSize();

		canvas.save();
		canvas.translate(center[0], center[1]);
		this.tickPaint.setTextSize(40);

		// hours
		for(int i=0; i<nbrHoursTicks; i++)
		{
			Rect bounds = new Rect();
			String text = String.valueOf((i + 1) % 25);
			this.tickPaint.getTextBounds(text, 0, text.length(), bounds);
			canvas.rotate(degreesPerTick);
			canvas.drawText(text, x - (bounds.width() / 2), y, this.tickPaint);
		}

		canvas.restore();
		this.tickPaint.setTextSize(textSize);
	}

	/**
	 * draw hours/minutes/seconds somewhere
	 * @param canvas where to draw
	 * @param tickMode 12 or 24 hour mode
	 */
	private void drawHands(final Canvas canvas, final TickMode tickMode)
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
		int margin = 10;

		if(this.calendar != null)
		{
			Rect dateBounds = new Rect();
			Rect timeBounds = new Rect();
			Calendar calendar = Calendar.getInstance(Locale.getDefault());
			Date date = calendar.getTime();
			String dateText = this.dateFormatter.format(date);
			String timeText = this.timeFormatter.format(date);

			if(!this.isSecondHandDrawn)
			{
				timeText = this.timeWithoutSecondsFormatter.format(date);
			}

			this.timePaint.getTextBounds(dateText, 0, dateText.length(), dateBounds);
			this.timePaint.getTextBounds(timeText, 0, timeText.length(), timeBounds);
			// height of
			float height = dateBounds.height() + timeBounds.height() + (2 * margin);
			float width = Math.max(dateBounds.width(), timeBounds.width()) + (2 * margin);
			float x = center[0] - (width / 2);
			float y = center[1] + (radius / 2.0f);
			float left = x - margin;
			float top = y - height - margin;
			float right = x + width;
			float bottom = y;

			this.timePaint.setColor(getBackColor());
			this.timePaint.setAlpha(128);
			canvas.drawRect(left, top, right, bottom, this.timePaint);
			Paint.Style style = this.timePaint.getStyle();
			this.timePaint.setColor(getFrontColor());
			this.timePaint.setAlpha(255);
			this.timePaint.setStyle(Paint.Style.STROKE);
			canvas.drawRect(left, top, right, bottom, this.timePaint);
			this.timePaint.setStyle(style);

			canvas.save();
			canvas.translate(left + margin, top + margin);
			drawMultipleLines(new String[] {timeText, dateText}, canvas, this.timePaint, margin);
			canvas.restore();
		}
	}

	private int[] drawMultipleLines(String[] lines, Canvas canvas, Paint paint, int margin)
	{
		int[] widths = new int[lines.length];
		int[] heights = new int[lines.length];
		Rect bounds = new Rect();
		int maxWidth = 0;
		int totalHeight = 0;
		int offset = 0;

		// get required metrics
		for(int i=0; i<lines.length; i++)
		{
			String line = lines[i];
			paint.getTextBounds(line, 0, line.length(), bounds);
			widths[i] = bounds.width();
			heights[i] = bounds.height();
			maxWidth = Math.max(maxWidth, widths[i]);
			totalHeight += heights[i];
		}

		int center = maxWidth / 2;

		// draw each line centered
		for(int i=0; i<lines.length; i++)
		{
			offset += heights[i];
			String line = lines[i];
			int x = center - (widths[i] / 2);
			int y = offset;
			canvas.drawText(line, x, y, paint);
			offset += margin;		}

		int[] result = new int[2];
		result[0] = maxWidth;
		result[1] = totalHeight;

		return result;
	}

	/**
	 * displays hours on dial background
	 * @param canvas where to draw
	 * @param paint how to draw
	 * @param tickMode 12 or 24 hour mode
	 * @param from hour to start from
	 * @param to hour to end to
	 */
	private void fillHours(final Canvas canvas, final Paint paint, TickMode tickMode, final int from, final int to)
	{
		int maxHours = tickMode.getNbrHoursTicks();

		if(from > to)
		{
			throw new IllegalArgumentException("from is bigger than to : " + from + " " + to);
		}
		if(from > maxHours)
		{
			throw new IllegalArgumentException("from is bigger than maxHours : " + from + " " + maxHours);
		}

		int[] center = getCenter();
		int centerX = center[0];
		int centerY = center[1];
		int radius = getRadius();

		RectF rect = new RectF(-radius, -radius, radius, radius);
		float startAngle = (from / (float) maxHours) * 360.0f;
		float sweepAngle = ((to - from) / (float) maxHours) * 360.0f;

		// move to center of clock, change reference angle
		canvas.save();
		canvas.translate(centerX, centerY);
		canvas.rotate(-90);

		Path path = new Path();
		path.moveTo(0, 0);
		path.arcTo(rect, startAngle, sweepAngle, true);
		path.lineTo(0, 0);
		path.close();

		canvas.drawPath(path, paint);
		canvas.restore();
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

	private int getFrontColor()
	{
		return this.color;
	}

	private int getBackColor()
	{
		return this.backgroundColor;
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
