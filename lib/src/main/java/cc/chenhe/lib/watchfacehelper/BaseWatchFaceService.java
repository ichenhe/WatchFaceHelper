package cc.chenhe.lib.watchfacehelper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.view.SurfaceHolder;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.TimeZone;


/**
 * Created by 晨鹤 on 2017/7/18.
 * 基础表盘服务。
 */

public abstract class BaseWatchFaceService extends CanvasWatchFaceService {

    public class BaseEngine extends BaseAbstractEngine {

        @Override
        public void onTimeZoneChanged() {

        }

        @Override
        public void onBatteryChanged(int level, int scale, int status) {

        }
    }

    private abstract class BaseAbstractEngine extends Engine {

        private static final int MSG_UPDATE_TIME = 0;
        /**
         * 刷新时间间隔(ms)
         */
        private long Interactive_Update_Rate_MS = 0;

        //标识广播接收器是否注册
        private boolean mRegedTimeZoneReceiver = false;
        private boolean mRegedCommonReceiver = false;

        //标识广播是否需要接受
        private boolean mNeedRecBatteryChanged = false;

        /*是否为12小时制*/
        private boolean m12h;
        /*是否为ticwear系统*/
        private boolean mTicwear;
        /*是否支持低功耗常亮*/
        private boolean mLowBitAmbient;
        /*是否需要烧屏保护*/
        private boolean mBurnInProtection;

        private Calendar mCalendar;

        /*定时触发*/
        final private Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what != MSG_UPDATE_TIME) return;
                invalidate();
                if (shouldTimerBeRunning()) {
                    long timeMs = System.currentTimeMillis();
                    long delayMs = Interactive_Update_Rate_MS
                            - (timeMs % Interactive_Update_Rate_MS);
                    mUpdateTimeHandler
                            .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                }
            }
        };

        /*监听时区改变，不可见时会取消注册*/
        final private BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                onTimeZoneChanged();
                invalidate();
            }
        };

        final private BroadcastReceiver mCommonReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case Intent.ACTION_BATTERY_CHANGED:
                        onBatteryChanged(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0),
                                intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100),
                                intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0));
                        break;
                }
            }
        };

        @Override
        public void onDestroy() {
            unregCommonReceiver();
            unregTimeZoneReceiver();
            super.onDestroy();
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            mCalendar = Calendar.getInstance();

            mTicwear = !getProperty("mTicwear.version.name", "unknown").equals("unknown");
        }

        /**
         * 可见状态改变
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                regReceiver();

                //刷新时区，防止在不可见时被改变。
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregTimeZoneReceiver();
            }
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            //启用全局抗锯齿
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        }

        /**
         * 每分钟触发
         */
        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        /**
         * 检测到系统屏幕特性
         */
        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        /**
         * 常亮/激活状态切换
         */
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            invalidate();
            updateTimer();
        }

        /**
         * 更新时钟状态
         */
        private void updateTimer() {
            String time = android.provider.Settings.System.getString(getContentResolver(),
                    android.provider.Settings.System.TIME_12_24);
            m12h = time != null && time.equals("12");

            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning())
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);

        }

        /**
         * 确认时钟是否应该继续运行
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode() && Interactive_Update_Rate_MS > 0;
        }

        /**
         * 注册监听器
         */
        private void regReceiver() {
            if (!isVisible()) return;
            //时区
            if (!mRegedTimeZoneReceiver) {
                IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
                BaseWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
                mRegedTimeZoneReceiver = true;
            }

            //其他
            if (mRegedCommonReceiver) unregCommonReceiver();
            IntentFilter filter = new IntentFilter();

            if (mNeedRecBatteryChanged) filter.addAction(Intent.ACTION_BATTERY_CHANGED);

            BaseWatchFaceService.this.registerReceiver(mCommonReceiver, filter);
            mRegedCommonReceiver = true;
        }

        /**
         * 注销时区监听器
         */
        private void unregTimeZoneReceiver() {
            if (mRegedTimeZoneReceiver) {
                BaseWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
                mRegedTimeZoneReceiver = false;
            }
            unregCommonReceiver();
        }

        /**
         * 注销其他监听器
         */
        private void unregCommonReceiver() {
            if (!mRegedCommonReceiver) return;
            BaseWatchFaceService.this.unregisterReceiver(mCommonReceiver);
            mRegedCommonReceiver = false;
        }

        private String getProperty(String key, String defaultValue) {
            String value = defaultValue;
            try {
                Class<?> c = Class.forName("android.os.SystemProperties");
                Method get = c.getMethod("get", String.class, String.class);
                value = (String) (get.invoke(c, key, "unknown"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return value;
        }

        /**
         * 启用电池广播接收器
         */
        public void enableBatteryReceiver() {
            mNeedRecBatteryChanged = true;
            regReceiver();
        }

        /**
         * 设置激活时刷新间隔。≤0为不刷新。
         *
         * @param rate 间隔（ms）
         */
        public void setInteractiveUpdateRateMS(int rate) {
            Interactive_Update_Rate_MS = rate;
            updateTimer();
        }

        public abstract void onTimeZoneChanged();

        public abstract void onBatteryChanged(int level, int scale, int status);

        public boolean is12h() {
            return m12h;
        }

        public boolean isTicwear() {
            return mTicwear;
        }

        public boolean isSupportLowBitAmbient() {
            return mLowBitAmbient;
        }

        public boolean isBurnInProtection() {
            return mBurnInProtection;
        }

        public Calendar getCalendar() {
            return mCalendar;
        }
    }
}
