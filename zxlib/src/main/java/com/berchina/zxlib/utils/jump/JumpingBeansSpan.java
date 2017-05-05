/*
 * Copyright 2015 Frakbot (Sebastiano Poggi and Francesco Pontillo)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.berchina.zxlib.utils.jump;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.text.TextPaint;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;

/**
 * Created by zx on 2017/3/24 13:47
 * 项目名称： ZxLib
 * 类描述： 文字跳动
 * 备注
 */
final class JumpingBeansSpan extends SuperscriptSpan implements ValueAnimator.AnimatorUpdateListener {

    private final WeakReference<TextView> textView;
    private final int delay;
    private final int loopDuration;
    private final float animatedRange;
    private int shift;
    private ValueAnimator jumpAnimator;

    public JumpingBeansSpan(TextView textView, int loopDuration, int position, int waveCharOffset,
                            float animatedRange) {
        this.textView = new WeakReference<TextView>(textView);
        this.delay = waveCharOffset * position;
        this.loopDuration = loopDuration;
        this.animatedRange = animatedRange;
    }

    @Override
    public void updateMeasureState(TextPaint tp) {
        initIfNecessary(tp.ascent());
        tp.baselineShift = shift;
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        initIfNecessary(tp.ascent());
        tp.baselineShift = shift;
    }

    private void initIfNecessary(float ascent) {
        if (jumpAnimator != null) {
            return;
        }

        this.shift = 0;
        int maxShift = (int) ascent / 2;
        jumpAnimator = ValueAnimator.ofInt(0, maxShift);
        jumpAnimator
                .setDuration(loopDuration)
                .setStartDelay(delay);
        jumpAnimator.setInterpolator(new JumpInterpolator(animatedRange));
        jumpAnimator.setRepeatCount(ValueAnimator.INFINITE);
        jumpAnimator.setRepeatMode(ValueAnimator.RESTART);
        jumpAnimator.addUpdateListener(this);
        jumpAnimator.start();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        // No need for synchronization as this always run on main thread anyway
        TextView v = textView.get();
        if (v != null) {
            updateAnimationFor(animation, v);
        } else {
            cleanupAndComplainAboutUserBeingAFool();
        }
    }

    private void updateAnimationFor(ValueAnimator animation, TextView v) {
        if (isAttachedToHierarchy(v)) {
            shift = Integer.parseInt(animation.getAnimatedValue().toString());
            v.invalidate();
        }
    }

    private static boolean isAttachedToHierarchy(View v) {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return v.isAttachedToWindow();
        }*/
        return v.getParent() != null;   // Best-effort fallback
    }

    private void cleanupAndComplainAboutUserBeingAFool() {
        // The textview has been destroyed and teardown() hasn't been called
        teardown();
        Log.w("JumpingBeans", "!!! Remember to call JumpingBeans.stopJumping() when appropriate !!!");
    }

    /*package*/ void teardown() {
        if (jumpAnimator != null) {
            jumpAnimator.cancel();
            jumpAnimator.removeAllListeners();
        }
        if (textView.get() != null) {
            textView.clear();
        }
    }

    /**
     * A tweaked {@link android.view.animation.AccelerateDecelerateInterpolator}
     * that covers the full range in a fraction of its input range, and holds on
     * the final value on the rest of the input range. By default, this fraction
     * is 65% of the full range.
     */
    private static class JumpInterpolator implements TimeInterpolator {

        private final float animRange;

        public JumpInterpolator(float animatedRange) {
            animRange = Math.abs(animatedRange);
        }

        @Override
        public float getInterpolation(float input) {
            // We want to map the [0, PI] sine range onto [0, animRange]
            if (input > animRange) return 0f;
            double radians = (input / animRange) * Math.PI;
            return (float) Math.sin(radians);
        }

    }

}
