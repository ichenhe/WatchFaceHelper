package cc.chenhe.lib.watchfacehelper.demo;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Calendar;

import cc.chenhe.lib.watchfacehelper.BaseWatchFaceService;

/**
 * Created by 晨鹤 on 2017/10/13.
 * Demo.
 */

public class DemoWatchFace extends BaseWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new MyEngine();
    }

    private class MyEngine extends BaseWatchFaceService.BaseEngine {

        private Paint mPaint;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setInteractiveUpdateRateMS(1000);

            enableBatteryReceiver();

            mPaint = new Paint();
            mPaint.setColor(Color.WHITE);
            mPaint.setTextAlign(Paint.Align.CENTER);
            mPaint.setTextSize(50);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);

            canvas.drawColor(Color.BLACK);

            String s = getCalendar().get(is12h() ? Calendar.HOUR : Calendar.HOUR_OF_DAY) + ":"
                    + getCalendar().get(Calendar.MINUTE);
            if (!isInAmbientMode())
                s += ":" + getCalendar().get(Calendar.SECOND);
            canvas.drawText(s, bounds.width() / 2f, bounds.height() / 2f, mPaint);
        }

        @Override
        public void onBatteryChanged(int level, int scale, int status) {
            super.onBatteryChanged(level, scale, status);
            Log.i("Battery", "Level: " + level + " Scale: " + scale + " status: " + status);
        }
    }
}
