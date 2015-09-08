package com.example.chenhaoych.tagactivity.tagmanager;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;

/**
 * Created by chenhao.ych on 2015/7/8.
 */
public class TagEditChip extends ImageSpan {

    private String mEntry;
    private boolean mSelected = false;

    private CharSequence mOriginalText;

    public TagEditChip(Drawable drawable, String entry, int offset) {
        super(drawable, DynamicDrawableSpan.ALIGN_BOTTOM);
        mEntry = entry;
    }

    /**
     * Set the selected state of the chip.
     * @param selected
     */
    public void setSelected(boolean selected) {
        mSelected = selected;
    }

    /**
     * Return true if the chip is selected.
     */
    public boolean isSelected() {
        return mSelected;
    }
    /**
     * Get associated String.
     */
    public String getEntry() {
        return mEntry;
    }

    public void setOriginalText(String text) {
        if (!TextUtils.isEmpty(text)) {
            text = text.trim();
        }
        mOriginalText = text;
    }

    public CharSequence getOriginalText() {
        return !TextUtils.isEmpty(mOriginalText) ? mOriginalText : mEntry;
    }
}
