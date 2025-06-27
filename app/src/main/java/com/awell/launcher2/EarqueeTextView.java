/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.awell.launcher2;

import android.content.Context;
import android.util.AttributeSet;


/**
 * We use a custom tab view to process our own focus traversals.
 */
public class EarqueeTextView extends androidx.appcompat.widget.AppCompatTextView {
    public EarqueeTextView(Context context) {
        super(context);
    }
    public EarqueeTextView(Context context, AttributeSet attrs,
                           int defStyle) {
        super(context, attrs, defStyle);
    }
    public EarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    @Override
    public boolean isFocused() {
        return true;
    }
}

