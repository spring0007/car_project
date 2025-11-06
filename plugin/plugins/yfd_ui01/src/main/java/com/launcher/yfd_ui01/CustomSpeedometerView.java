package com.launcher.yfd_ui01;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;


public class CustomSpeedometerView extends View {
    //private Bitmap mBackGround;
    private Bitmap mNeedle;
    private float mCurrentValue = 0;

    public CustomSpeedometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initResources(context);

    }

    private void initResources(Context context) {
        //float scale = getResources().getFloat(R.dimen.speed_image_scale);
        //mBackGround = BitmapFactory.decodeResource(context.getResources(), R.drawable.ui7_car_peed_bg);
        mNeedle = BitmapFactory.decodeResource(context.getResources(), R.drawable.ui7_car_speed_point);
        //mBackGround = scaleBitmap(mBackGround, scale);
        //mNeedle = scaleBitmap(mNeedle, scale);

    }

    //TODO 实现控制圆形放大缩小的功能
//    private Bitmap scaleBitmap(Bitmap bitmap, float scale) {
//        int width = bitmap.getWidth();
//        int height = bitmap.getHeight();
//        int newWidth = (int) (width * scale);
//        int newHeight = (int) (height * scale);
//        return Bitmap.createScaledBitmap(bitmap,newWidth, newHeight, true);
//    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        //canvas.drawBitmap(mBackGround, centerX - ((float) mBackGround.getWidth() / 2), centerY - ((float) mBackGround.getHeight() / 2), null);
        //canvas.save();
        canvas.rotate(mCurrentValue, centerX, centerY);
        canvas.drawBitmap(mNeedle, centerX - ((float) mNeedle.getWidth() / 2), centerY - ((float) mNeedle.getHeight() / 2), null);
        canvas.restore();
    }


    public void setCurrentValue(float value) {
        // mCurrentValue =  -135 + (value / 7000f) * 270;
        mCurrentValue = value;
        this.invalidate();
    }

}