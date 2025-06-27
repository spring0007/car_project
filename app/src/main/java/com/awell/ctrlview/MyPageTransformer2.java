package com.awell.ctrlview;

import android.view.View;

import androidx.viewpager.widget.ViewPager;

public class MyPageTransformer2 implements ViewPager.PageTransformer {
       private static final float MIN_SCALE = 0.75f;

         public void transformPage(View page, float position) {
             page.setCameraDistance(page.getWidth() * 20);

             if (position < -1) { // [-Infinity,-1)
                 page.setPivotX(0f);
                 page.setPivotY(0f);
                 page.setRotationY(0);
             } else if (position <= 0) { // [-1,0]
                 page.setPivotX(page.getWidth());
                 page.setPivotY(0f);
                 page.setRotationY(-90f * position);
             } else if (position <= 1) { // (0,1]
                 page.setPivotX(0f);
                 page.setPivotY(0f);
                 page.setRotationY(-90f * position);
             } else { // (1,+Infinity]
                 page.setPivotX(0f);
                 page.setPivotY(0f);
                 page.setRotationY(0);
             }
              }
}
