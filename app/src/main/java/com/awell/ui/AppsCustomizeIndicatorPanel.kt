package com.awell.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.withSave
import com.awell.launcher.library.R

/**
 * 全部app列表下方的页码指示器
 */
@SuppressLint("UseCompatLoadingForDrawables")
class AppsCustomizeIndicatorPanel(context: Context) : View(context) {

    private val TAG = AppsCustomizeIndicatorPanel::class.simpleName

    private var mPaint = Paint()

    private var mBmpSelect: Bitmap
    private var mBmpBackground: Bitmap

    private var movX = 0

    private val dotGap: Int = 16

    var mTotalPages: Int = 0
    var mCurrentPage: Int = 0


    init {
        mPaint.isAntiAlias = true
        val bmp = resources.getDrawable(R.drawable.dot_nor3) as BitmapDrawable
        mBmpBackground = bmp.bitmap
        val bmpDraw = resources.getDrawable(R.drawable.dot_sel3) as BitmapDrawable
        mBmpSelect = bmpDraw.bitmap


    }
    fun setIndicatorStyle( style : Int){
        when (style) {
            1 -> {
                val bmp = resources.getDrawable(R.drawable. mui101_ic_pageindicator_current) as BitmapDrawable
                mBmpBackground = bmp.bitmap
                val bmpDraw = resources.getDrawable(R.drawable.mui101_ic_pageindicator_default) as BitmapDrawable
                mBmpSelect = bmpDraw.bitmap
            }
//            2 -> { //逸卡思 --长条
//                val bmp = resources.getDrawable(R.drawable.yks_dot_sel) as BitmapDrawable
//                mBmpBackground = bmp.bitmap
//                val bmpDraw = resources.getDrawable(R.drawable.yks_dot_nor) as BitmapDrawable
//                mBmpSelect = bmpDraw.bitmap
//
//            }
            else -> {
                val bmp = resources.getDrawable(R.drawable.dot_nor3) as BitmapDrawable
                mBmpBackground = bmp.bitmap
                val bmpDraw = resources.getDrawable(R.drawable.dot_sel3) as BitmapDrawable
                mBmpSelect = bmpDraw.bitmap
            }
        }
    }


    override fun onDraw(canvas: Canvas?) {

        val parentWidth: Int? = (parent as? ViewGroup)?.width
        val parentHeight: Int? = (parent as? ViewGroup)?.height

        parentWidth?.let {
            parentHeight?.let { it1 ->
                canvas?.clipRect(
                    0, 0, it, it1
                )
            }
        }

        canvas?.withSave {

            canvas.rotate(0.0f)
            val bmpDotW: Int = mBmpSelect.width
            val startX: Int =
                ((parentWidth?.minus((bmpDotW + dotGap) * mTotalPages) ?: 0) + dotGap) / 2 + 20
            var startXTmp = startX

            for (i in 0..<(mTotalPages)) {
                mBmpSelect.let {
                    canvas.drawBitmap(
                        it, (startXTmp - 20).toFloat(), 0f, mPaint
                    )
                }
                startXTmp += bmpDotW + dotGap
            }

            mBmpBackground.let {
                canvas.drawBitmap(
                    it, (startX + (bmpDotW + dotGap) * mCurrentPage + movX - 20).toFloat(),
                    0f,
                    mPaint
                )
            }
        }
    }
}