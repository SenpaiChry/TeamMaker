package com.example.myapplication;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * ScrollView con altezza massima: si adatta al contenuto (wrap) finché non
 * supera maxHeight, poi si ferma e scrolla. Usato nella modale modifica partita
 * per non lasciare vuoto con pochi set e non sforare con tanti.
 */
public class MaxHeightScrollView extends ScrollView {

    private int maxHeight = 0;

    public MaxHeightScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (maxHeight > 0) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
