package net.christianbeier.droidvnc_ng;

/*
 * DroidVNC-NG InputService that binds to the Android a11y API and posts input events sent by the native backend to Android.
 *
 * Its original version was copied from https://github.com/anyvnc/anyvnc/blob/master/apps/ui/android/src/com/anyvnc/AnyVncAccessibilityService.java at
 * f32015d9d29d2d022217f52a99f676ace90cc29e.
 *
 * Original author is Tobias Junghans <tobydox@veyon.io>
 *
 * Licensed under GPL-2.0 as per https://github.com/anyvnc/anyvnc/blob/master/COPYING.
 */

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.ViewConfiguration;
import android.graphics.Path;

public class InputService extends AccessibilityService {

	/**
	 * This globally tracks gesture completion status and is _not_ per gesture.
	 */
	private static class GestureCallback extends AccessibilityService.GestureResultCallback {
		private boolean mCompleted = true; // initially true so we can actually dispatch something

		@Override
		public synchronized void onCompleted(GestureDescription gestureDescription) {
			mCompleted = true;
		}

		@Override
		public synchronized void onCancelled(GestureDescription gestureDescription) {
			mCompleted = true;
		}
	}

	private static final String TAG = "InputService";

	private static InputService instance;

	private GestureCallback mGestureCallback = new GestureCallback();


	@Override
	public void onAccessibilityEvent( AccessibilityEvent event ) { }

	@Override
	public void onInterrupt() { }

	@Override
	public void onServiceConnected()
	{
		super.onServiceConnected();
		instance = this;
		Log.i(TAG, "onServiceConnected");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		instance = null;
		Log.i(TAG, "onDestroy");
	}

	public static boolean isEnabled()
	{
		return instance != null;
	}


	public static void onPointerEvent(int buttonMask,int x,int y, long client) {

		try {
			// left mouse button
			if ((buttonMask & (1 << 0)) != 0) {
				instance.tap(x, y);
			}

			// right mouse button
			if ((buttonMask & (1 << 2)) != 0) {
				instance.longPress(x, y);
			}

			// scroll up
			if ((buttonMask & (1 << 3)) != 0) {

				DisplayMetrics displayMetrics = new DisplayMetrics();
				WindowManager wm = (WindowManager) instance.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
				wm.getDefaultDisplay().getRealMetrics(displayMetrics);

				instance.scroll(x, y, -displayMetrics.heightPixels / 2);
			}

			// scroll down
			if ((buttonMask & (1 << 4)) != 0) {

				DisplayMetrics displayMetrics = new DisplayMetrics();
				WindowManager wm = (WindowManager) instance.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
				wm.getDefaultDisplay().getRealMetrics(displayMetrics);

				instance.scroll(x, y, displayMetrics.heightPixels / 2);
			}
		} catch (Exception e) {
			// nop, instance probably null
		}
	}


	private void tap( int x, int y )
	{
			dispatchGesture( createClick( x, y, ViewConfiguration.getTapTimeout()), null, null );
	}

	private  void longPress( int x, int y )
	{
			dispatchGesture( createClick( x, y, ViewConfiguration.getTapTimeout() + ViewConfiguration.getLongPressTimeout()), null, null );
	}

	private void scroll( int x, int y, int scrollAmount )
	{
			/*
			   Ignore if another gesture is still ongoing. Especially true for scroll events:
			   These mouse button 4,5 events come per each virtual scroll wheel click, an incoming
			   event would cancel the preceding one, only actually scrolling when the user stopped
			   scrolling.
			 */
			if(!mGestureCallback.mCompleted)
				return;

			mGestureCallback.mCompleted = false;
			dispatchGesture(createSwipe(x, y, x, y - scrollAmount, ViewConfiguration.getScrollDefaultDelay()), mGestureCallback, null);
	}

	private static GestureDescription createClick( int x, int y, int duration )
	{
		Path clickPath = new Path();
		clickPath.moveTo( x, y );
		GestureDescription.StrokeDescription clickStroke = new GestureDescription.StrokeDescription( clickPath, 0, duration );
		GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
		clickBuilder.addStroke( clickStroke );
		return clickBuilder.build();
	}

	private static GestureDescription createSwipe( int x1, int y1, int x2, int y2, int duration )
	{
		Path swipePath = new Path();

		x1 = Math.max(x1, 0);
		y1 = Math.max(y1, 0);
		x2 = Math.max(x2, 0);
		y2 = Math.max(y2, 0);

		swipePath.moveTo( x1, y1 );
		swipePath.lineTo( x2, y2 );
		GestureDescription.StrokeDescription swipeStroke = new GestureDescription.StrokeDescription( swipePath, 0, duration );
		GestureDescription.Builder swipeBuilder = new GestureDescription.Builder();
		swipeBuilder.addStroke( swipeStroke );
		return swipeBuilder.build();
	}
}
