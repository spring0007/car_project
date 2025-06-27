package com.awell.ctrlview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.awell.launcher.R;

/**
 * 绘制频谱
 * @author Administrator
 *
 */
public class SpetrumView extends View {
	
	private Bitmap mSpectrumDarkBitmap = null; // 暗色的图片
	private Bitmap mSpectrumLightBitmap = null; // 亮色的频谱图片
	
	private int[] mSpetrumDatas = new int[16]; // 频谱数据
	
	private static final int MUSIC_SPETRUM_MAX = 8; // 音乐最大值
	
	private Paint painter = null;

	public SpetrumView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mSpectrumDarkBitmap  = BitmapFactory.decodeResource(getResources(), R.drawable.spectrum_n);
		mSpectrumLightBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.spectrum_d);
		
		painter = new Paint();
	}
	
	/**
	 * 刷新频谱
	 * @param datas
	 */
	public void updateSpetrum(String[] datas) {
		double maxGreenBlockValue = 0;
        int nPieceNum = 16; // 最多只取16位
        int nGreenBlockNum = MUSIC_SPETRUM_MAX; // 8块
        int PEAK_TOLERANT = 0;

        if (datas.length < nPieceNum) {
            nPieceNum = datas.length;
        }

        for (int i = 0; i < nPieceNum; i++) {
            if (Double.valueOf(datas[i]) > maxGreenBlockValue)
                maxGreenBlockValue = Double.valueOf(datas[i]);
        }
        if (0 == maxGreenBlockValue) {
            maxGreenBlockValue = 1;
        }
        int blockNum;
        double dTemp;
        int nPreFreValue = 0;
        for (int i = 0; i < nPieceNum; ++i) {
            dTemp = (double) (Double.valueOf(datas[i]) / maxGreenBlockValue);
            blockNum = (int) ((nGreenBlockNum - PEAK_TOLERANT) * dTemp);
            if (blockNum < 1) {
            	blockNum = 1;
            }
            nPreFreValue = i * mSpetrumDatas.length / nPieceNum; // 取显示的对应的位数
            if (nPreFreValue < mSpetrumDatas.length) {
            	mSpetrumDatas[nPreFreValue] = blockNum;
            }
        }
        
        invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		int nWidget = getWidth() / 32;
		int nHeight = getHeight() / 8;
		if (null == mSpetrumDatas || mSpetrumDatas.length == 0) {
			for (int i = 0; i < 32; i++) {
				// 绘制两个16，重复一遍
				for (int j = 0; j < 8; ++j) {
					canvas.drawBitmap(mSpectrumDarkBitmap, nWidget * i, nHeight * j, painter);
				}
			}
		} else {
			for (int i = 0; i < 32; i++) {
				int h = mSpetrumDatas[i % mSpetrumDatas.length];
				for (int j = 0; j < MUSIC_SPETRUM_MAX - h; ++j) {
					canvas.drawBitmap(mSpectrumDarkBitmap, nWidget * i, nHeight * j, painter);
				}
				for (int j = MUSIC_SPETRUM_MAX - h; j < MUSIC_SPETRUM_MAX; ++j) {
					canvas.drawBitmap(mSpectrumLightBitmap, nWidget * i, nHeight * j, painter);
				}
			}
		}
		
		super.onDraw(canvas);
	}
}

