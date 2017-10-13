# WatchFaceHelper
帮助你简化AndroidWear表盘开发。（支持Ticwear）

# 特性
- 实现`CanvasWatchFaceService`和`Engine`的基本方法。
- 内置时区监听。
- 可选电量监听。

# 基本用法
1. 创建你的WatchFaceService并继承自`BaseWatchFaceService`.
2. 创建你的Engine并继承自`BaseWatchFaceService.BaseEngine`.
3. 专注于表盘的设计与绘制。

# 扩展用法
**注意**：所有内置的监听均会在表盘不可见时销毁并在可见时重新注册。因此当表盘不可见时，监听无法被正常调用，即使有关属性发生了改变。

**除时区外，其他监听默认没有启用，你需要调用`enableXXXX()`来启用方可正常收到通知。** 

## 时区监听
WatchFaceHelper内部已经实现了时区监听，你不必关心因时区变化带来的问题。
如果需要，你也可以在`Engine`里重写`onTimeZoneChanged`方法，此方法将在时区变化时被调用。
```java
    private class MyEngine extends BaseWatchFaceService.BaseEngine {
    
        @Override
        public void onTimeZoneChanged() {
            super.onTimeZoneChanged();
            //Do something you want here...
        }
    }
```

## 电量监听
在`Engine`里重写`onBatteryChanged`方法，此方法将在电池状态或电量变化时被调用。
```java
    private class MyEngine extends BaseWatchFaceService.BaseEngine {
    
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            //启用电量监听
            enableBatteryReceiver();
        }
    
        @Override
        public void onBatteryChanged(int level, int scale, int status) {
            super.onBatteryChanged(level, scale, status);
            Log.i("Battery", "Level: " + level + " Scale: " + scale + " status: " + status);
            //Do something you want here...
        }
    }
```

# DEMO
下面是一个简单的表盘，可以看出，使用了WatchFaceHelper，你只需要进行表盘的绘制就可以了。
此demo位于/app Module，你可以下载并运行。
```java
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
        public void onTimeZoneChanged() {
            super.onTimeZoneChanged();
        }

        @Override
        public void onBatteryChanged(int level, int scale, int status) {
            super.onBatteryChanged(level, scale, status);
            Log.i("Battery", "Level: " + level + " Scale: " + scale + " status: " + status);
        }
    }
}
```
