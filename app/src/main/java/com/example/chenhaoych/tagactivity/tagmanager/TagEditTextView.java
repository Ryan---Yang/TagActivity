package com.example.chenhaoych.tagactivity.tagmanager;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.QwertyKeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chenhaoych.tagactivity.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by chenhao.ych on 2015/7/8.
 */
public class TagEditTextView extends MultiAutoCompleteTextView implements
        TextView.OnEditorActionListener {
    private static final int MAX_CHIP_TEXT_LIMIT = 8;
    private static final int MAX_CHIPS = 10;

    private static final char COMMIT_CHAR_COMMA = ',';
    private static final char COMMIT_CHAR_SEMICOLON = '，';
    private static final char COMMIT_CHAR_SPACE = ' ';
    private static final String TAG = "RecipientEditTextView";
    private static int sSelectedTextColor = -1;

    private Drawable mChipBackground = null;
    private Drawable mChipBackgroundPressed;
    private float mChipHeight;
    private float mChipFontSize;
    private int mChipPadding;

    private Tokenizer mTokenizer;
    private TagEditChip mSelectedChip;
    private TextWatcher mTextWatcher;
    private Context mContext;

    public TagEditTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setChipDimensions(context, attrs);
        if (sSelectedTextColor == -1) {
            sSelectedTextColor = context.getResources().getColor(android.R.color.white);
        }
        setInputType(getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mTextWatcher = new RecipientTextWatcher();
        addTextChangedListener(mTextWatcher);
        setOnEditorActionListener(this);
        mContext = context;
    }

    @Override
    public boolean onEditorAction(TextView view, int action, KeyEvent keyEvent) {
        if (action == EditorInfo.IME_ACTION_DONE) {
            if (commitDefault()) {
                return true;
            }
            if (mSelectedChip != null) {
                clearSelectedChip();
                return true;
            } else if (focusNext()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection connection = super.onCreateInputConnection(outAttrs);
        int imeActions = outAttrs.imeOptions & EditorInfo.IME_MASK_ACTION;
        if ((imeActions & EditorInfo.IME_ACTION_DONE) != 0) {
            // clear the existing action
            outAttrs.imeOptions ^= imeActions;
            // set the DONE action
            outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;
        }
        if ((outAttrs.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        }
        outAttrs.actionLabel = getContext().getString(R.string.done);
        return connection;
    }

    private TagEditChip getLastChip() {
        TagEditChip last = null;
        TagEditChip[] chips = getSortedRecipients();
        if (chips != null && chips.length > 0) {
            last = chips[chips.length - 1];
        }
        return last;
    }

    private TagEditChip[] getSortedRecipients() {
        TagEditChip[] recips = getSpannable()
                .getSpans(0, getText().length(), TagEditChip.class);
        ArrayList<TagEditChip> recipientsList = new ArrayList<TagEditChip>(Arrays
                .asList(recips));
        final Spannable spannable = getSpannable();
        Collections.sort(recipientsList, new Comparator<TagEditChip>() {
            @Override
            public int compare(TagEditChip first, TagEditChip second) {
                int firstStart = spannable.getSpanStart(first);
                int secondStart = spannable.getSpanStart(second);
                if (firstStart < secondStart) {
                    return -1;
                } else if (firstStart > secondStart) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        return recipientsList.toArray(new TagEditChip[recipientsList.size()]);
    }

    @Override
    public void onSelectionChanged(int start, int end) {
        // When selection changes, see if it is inside the chips area.
        // If so, move the cursor back after the chips again.
        TagEditChip last = getLastChip();
        if (last != null && start < getSpannable().getSpanEnd(last)) {
            // Grab the last chip and set the cursor to after it.
            setSelection(Math.min(getSpannable().getSpanEnd(last) + 1, getText().length()));
        }
        super.onSelectionChanged(start, end);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!TextUtils.isEmpty(getText())) {
            super.onRestoreInstanceState(null);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        // If the user changes orientation while they are editing, just roll back the selection.
        clearSelectedChip();
        return super.onSaveInstanceState();
    }

    @Override
    public void append(CharSequence text, int start, int end) {
        // We need care about watching text changes while appending ',' or ';'.
        if (!TextUtils.isEmpty(text)) {
            String textString = text.toString().trim();
            if (textString.equals(String.valueOf(COMMIT_CHAR_COMMA))
                    || textString.equals(String.valueOf(COMMIT_CHAR_SEMICOLON))) {
                super.append(text, start, end);
                return;
            }
        }
        // We don't care about watching text changes while appending.
        if (mTextWatcher != null) {
            removeTextChangedListener(mTextWatcher);
        }
        super.append(text, start, end);
    }

    @Override
    public void onFocusChanged(boolean hasFocus, int direction, Rect previous) {
        super.onFocusChanged(hasFocus, direction, previous);
        if (!hasFocus) {
        } else {
            expand();
        }
    }

    @Override
    public void performValidation() {
        // Do nothing. Chips handles its own validation.
    }

    private void expand() {
        setCursorVisible(true);
        Editable text = getText();
        setSelection(text != null && text.length() > 0 ? text.length() : 0);
    }

    private CharSequence ellipsizeText(CharSequence text, TextPaint paint, float maxWidth) {
        paint.setTextSize(mChipFontSize);
        if (maxWidth <= 0 && Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Max width is negative: " + maxWidth);
        }
        return TextUtils.ellipsize(text, paint, maxWidth,
                TextUtils.TruncateAt.END);
    }

    private Bitmap createSelectedChip(String contact, TextPaint paint, Layout layout) {
        // Ellipsize the text so that it takes AT MOST the entire width of the
        // autocomplete text entry area. Make sure to leave space for padding
        // on the sides.
        int height = (int) mChipHeight;
        float[] widths = new float[1];
        paint.getTextWidths(" ", widths);
        CharSequence ellipsizedText = ellipsizeText(contact, paint,
                calculateAvailableWidth(true) - widths[0]);

        // Make sure there is a minimum chip width so the user can ALWAYS
        // tap a chip without difficulty.
        int width = (int) Math.floor(paint.measureText(ellipsizedText, 0,
                ellipsizedText.length()))
                + (mChipPadding * 2);

        // Create the background of the chip.
        Bitmap tmpBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tmpBitmap);
        if (mChipBackgroundPressed != null) {
            mChipBackgroundPressed.setBounds(0, 0, width, height);
            mChipBackgroundPressed.draw(canvas);
            paint.setColor(sSelectedTextColor);
            // Vertically center the text in the chip.
            canvas.drawText(ellipsizedText, 0, ellipsizedText.length(), mChipPadding,
                    getTextYOffset((String) ellipsizedText, paint, height), paint);
        } else {
            Log.w(TAG, "Unable to draw a background for the chips as it was never set");
        }
        return tmpBitmap;
    }

    private Bitmap createUnselectedChip(String contact, TextPaint paint, Layout layout) {
        // Ellipsize the text so that it takes AT MOST the entire width of the
        // autocomplete text entry area. Make sure to leave space for padding
        // on the sides.
        int height = (int) mChipHeight;
        float[] widths = new float[1];
        paint.getTextWidths(" ", widths);
        CharSequence ellipsizedText = ellipsizeText(contact, paint,
                calculateAvailableWidth(false) - widths[0]);
        // Make sure there is a minimum chip width so the user can ALWAYS
        // tap a chip without difficulty.
        int width = (int) Math.floor(paint.measureText(ellipsizedText, 0,
                ellipsizedText.length()))
                + (mChipPadding * 2);

        // Create the background of the chip.
        Bitmap tmpBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tmpBitmap);
        Drawable background = mChipBackground;
        if (background != null) {
            background.setBounds(0, 0, width, height);
            background.draw(canvas);
            paint.setColor(getContext().getResources().getColor(android.R.color.black));
            // Vertically center the text in the chip.
            canvas.drawText(ellipsizedText, 0, ellipsizedText.length(), mChipPadding,
                    getTextYOffset((String) ellipsizedText, paint, height), paint);
        } else {
            Log.w(TAG, "Unable to draw a background for the chips as it was never set");
        }
        return tmpBitmap;
    }

    private float getTextYOffset(String text, TextPaint paint, int height) {
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        int textHeight = bounds.bottom - bounds.top;
        return height - ((height - textHeight) / 2) - (int) paint.descent() / 2;
    }

    private TagEditChip constructChipSpan(String contact, int offset, boolean pressed)
            throws NullPointerException {
        if (mChipBackground == null) {
            throw new NullPointerException(
                    "Unable to render any chips as setChipDimensions was not called.");
        }
        Layout layout = getLayout();

        TextPaint paint = getPaint();
        float defaultSize = paint.getTextSize();
        int defaultColor = paint.getColor();

        Bitmap tmpBitmap;
        if (pressed) {
            tmpBitmap = createSelectedChip(contact, paint, layout);
        } else {
            tmpBitmap = createUnselectedChip(contact, paint, layout);
        }
        // Pass the full text, un-ellipsized, to the chip.
        Drawable result = new BitmapDrawable(getResources(), tmpBitmap);
        result.setBounds(0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight());
        TagEditChip tagEditChip = new TagEditChip(result, contact, offset);
        // Return text to the original size.
        paint.setTextSize(defaultSize);
        paint.setColor(defaultColor);
        return tagEditChip;
    }

    private float calculateAvailableWidth(boolean pressed) {
        return getWidth() - getPaddingLeft() - getPaddingRight() - (mChipPadding * 2);
    }


    private void setChipDimensions(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TagEditTextView, 0,
                0);
        Resources r = getContext().getResources();
        mChipBackground = a.getDrawable(R.styleable.TagEditTextView_chipBackground);
        if (mChipBackground == null) {
            mChipBackground = r.getDrawable(R.drawable.tag_btn);
        }
        mChipBackgroundPressed = a
                .getDrawable(R.styleable.TagEditTextView_chipBackgroundPressed);
        if (mChipBackgroundPressed == null) {
            mChipBackgroundPressed = r.getDrawable(R.drawable.tag_btn_pressed);
        }
        mChipPadding = a.getDimensionPixelSize(R.styleable.TagEditTextView_chipPadding, -1);
        if (mChipPadding == -1) {
            mChipPadding = (int) r.getDimension(R.dimen.chip_padding);
        }
        mChipHeight = a.getDimensionPixelSize(R.styleable.TagEditTextView_chipHeight, -1);
        if (mChipHeight == -1) {
            mChipHeight = r.getDimension(R.dimen.chip_height);
        }
        mChipFontSize = a.getDimensionPixelSize(R.styleable.TagEditTextView_chipFontSize, -1);
        if (mChipFontSize == -1) {
            mChipFontSize = r.getDimension(R.dimen.chip_text_size);
        }
        a.recycle();
    }

    @Override
    public void onSizeChanged(int width, int height, int oldw, int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);
    }

    private String createTokenizedEntry(String token) {
        if (TextUtils.isEmpty(token)) {
            return null;
        }
        token = token.trim();
        char charAt = token.charAt(token.length() - 1);
        if (charAt == COMMIT_CHAR_COMMA || charAt == COMMIT_CHAR_SEMICOLON) {
            token = token.substring(0, token.length() - 1);
        }
        return token;
    }

    @Override
    public void setTokenizer(Tokenizer tokenizer) {
        mTokenizer = tokenizer;
        super.setTokenizer(mTokenizer);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mSelectedChip != null) {
            clearSelectedChip();
            return true;
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (event.hasNoModifiers()) {
                    if (commitDefault()) {
                        return true;
                    }
                    if (mSelectedChip != null) {
                        clearSelectedChip();
                        return true;
                    } else if (focusNext()) {
                        return true;
                    }
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private boolean focusNext() {
        View next = focusSearch(View.FOCUS_DOWN);
        if (next != null) {
            next.requestFocus();
            return true;
        }
        return false;
    }

    private boolean commitDefault() {
        // If there is no tokenizer, don't try to commit.
        if (mTokenizer == null) {
            return false;
        }
        Editable editable = getText();
        setSelection(length());
        int end = getSelectionEnd();
        int start = mTokenizer.findTokenStart(editable, end);

        if (shouldCreateChip(start, end)) {
            return commitChip(start, end, editable);
        }
        return false;
    }

    private void commitByCharacter() {
        // We can't possibly commit by character if we can't tokenize.
        if (mTokenizer == null) {
            return;
        }
        Editable editable = getText();
        int end = getSelectionEnd();
        int start = mTokenizer.findTokenStart(editable, end);
        if (shouldCreateChip(start, end)) {
            commitChip(start, end, editable);
        }
        setSelection(getText().length());
    }

    private boolean commitChip(int start, int end, Editable editable) {
        int tokenEnd = mTokenizer.findTokenEnd(editable, start);
        if (editable.length() > tokenEnd + 1) {
            char charAt = editable.charAt(tokenEnd + 1);
            if (charAt == COMMIT_CHAR_COMMA || charAt == COMMIT_CHAR_SEMICOLON) {
                tokenEnd++;
            }
        }
        char originalFirst = editable.toString().substring(start, end).trim().charAt(0);
        if (originalFirst == COMMIT_CHAR_COMMA || originalFirst == COMMIT_CHAR_SEMICOLON) {
            editable.replace(start, end, "");
            return false;
        }
        String text = editable.toString().substring(start, tokenEnd).trim();
        clearComposingText();
        if (text != null && text.length() > 0 && !text.equals(" ")) {
            if (getRecipientsNumber() >= MAX_CHIPS || findSameChip(text)) {
                editable.replace(start, end, "");
                Toast.makeText(mContext, findSameChip(text) ? R.string.tag_same : R.string.tag_max, Toast.LENGTH_SHORT).show();
                return true;
            }
            String entry = createTokenizedEntry(text);
            if (entry != null) {
                QwertyKeyListener.markAsReplaced(editable, start, end, "");
                CharSequence chipText = createChip(entry, false);
                if (chipText != null && start > -1 && end > -1) {
                    editable.replace(start, end, chipText);
                }
            }
            return true;
        }
        return false;
    }

    private boolean shouldCreateChip(int start, int end) {
        return hasFocus() && enoughToFilter() && !alreadyHasChip(start, end);
    }

    private boolean alreadyHasChip(int start, int end) {
        TagEditChip[] chips = getSpannable().getSpans(start, end, TagEditChip.class);
        return (!(chips == null || chips.length == 0));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (mSelectedChip != null) {
                removeChip(mSelectedChip);
                return false;
            } else if (getLastChip() != null && length() > 0) {
                char last, beforelast;
                int end = getSelectionEnd() == 0 ? 0 : getSelectionEnd() - 1;
                int len = length() - 1;
                if (end != len) {
                    last = getText().charAt(end);
                    beforelast = getText().charAt(end - 1);
                } else {
                    last = getText().charAt(len);
                    beforelast = getText().charAt(len - 1);
                }
                if ((last == COMMIT_CHAR_SPACE && beforelast == COMMIT_CHAR_COMMA) || (last == COMMIT_CHAR_SPACE && beforelast == COMMIT_CHAR_SEMICOLON)
                        || last == COMMIT_CHAR_COMMA || last == COMMIT_CHAR_SEMICOLON) {
                    if (end == len)
                        mSelectedChip = selectChip(getLastChip());
                    return false;
                }
            }
        }

        return keyCode == KeyEvent.KEYCODE_ENTER && event.hasNoModifiers() || super.onKeyDown(keyCode, event);

    }

    private Spannable getSpannable() {
        return getText();
    }

    private int getChipStart(TagEditChip chip) {
        return getSpannable().getSpanStart(chip);
    }

    private int getChipEnd(TagEditChip chip) {
        return getSpannable().getSpanEnd(chip);
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        if (enoughToFilter() && !isCompletedToken(text)) {
            int end = getSelectionEnd();
            int start = mTokenizer.findTokenStart(text, end);
            // If this is a TagEditChip, don't filter
            // on its contents.
            Spannable span = getSpannable();
            TagEditChip[] chips = span.getSpans(start, end, TagEditChip.class);
            if (chips != null && chips.length > 0) {
                return;
            }
        }
        super.performFiltering(text, keyCode);
    }

    private boolean isCompletedToken(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return false;
        }
        // Check to see if this is a completed token before filtering.
        int end = text.length();
        int start = mTokenizer.findTokenStart(text, end);
        String token = text.toString().substring(start, end).trim();
        if (!TextUtils.isEmpty(token)) {
            char atEnd = token.charAt(token.length() - 1);
            return atEnd == COMMIT_CHAR_COMMA || atEnd == COMMIT_CHAR_SEMICOLON;
        }
        return false;
    }

    private void clearSelectedChip() {
        if (mSelectedChip != null) {
            unselectChip(mSelectedChip);
            mSelectedChip = null;
        }
        setCursorVisible(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isFocused()) {
            // Ignore any chip taps until this view is focused.
            return super.onTouchEvent(event);
        }
        boolean handled = super.onTouchEvent(event);
        int action = event.getAction();
        boolean chipWasSelected = false;
        if (action == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            int offset = putOffsetInRange(getOffsetForPosition(x, y));
            TagEditChip currentChip = findChip(offset);
            if (currentChip != null) {
                if (action == MotionEvent.ACTION_UP) {
                    if (mSelectedChip != null && mSelectedChip != currentChip) {
                        clearSelectedChip();
                        mSelectedChip = selectChip(currentChip);
                    } else if (mSelectedChip == null) {
                        setSelection(getText().length());
                        commitDefault();
                        mSelectedChip = selectChip(currentChip);
                    } else {
                        onClick(mSelectedChip, offset, x, y);
                    }
                }
                chipWasSelected = true;
                handled = true;

            } else if (mSelectedChip != null
                    /*&& mSelectedChip.getContactId() == TagEditEntry.INVALID_CONTACT*/) {
                chipWasSelected = true;
            }
        }
        if (action == MotionEvent.ACTION_UP && !chipWasSelected) {
            clearSelectedChip();
        }
        return handled;
    }

    private int putOffsetInRange(int o) {
        int offset = o;
        Editable text = getText();
        int length = text.length();
        // Remove whitespace from end to find "real end"
        int realLength = length;
        for (int i = length - 1; i >= 0; i--) {
            if (text.charAt(i) == ' ') {
                realLength--;
            } else {
                break;
            }
        }
        // If the offset is beyond or at the end of the text,
        // leave it alone.
        if (offset >= realLength) {
            return offset;
        }
        Editable editable = getText();
        while (offset >= 0 && findText(editable, offset) == -1 && findChip(offset) == null) {
            // Keep walking backward!
            offset--;
        }
        return offset;
    }

    private int findText(Editable text, int offset) {
        if (text.charAt(offset) != ' ') {
            return offset;
        }
        return -1;
    }

    private TagEditChip findChip(int offset) {
        TagEditChip[] chips = getSpannable().getSpans(0, getText().length(), TagEditChip.class);
        // Find the chip that contains this offset.
        for (TagEditChip chip : chips) {
            int start = getChipStart(chip);
            int end = getChipEnd(chip);
            if (offset >= start && offset <= end) {
                return chip;
            }
        }
        return null;
    }

    // Use this method to generate text to add to the list of addresses.
    private String createAddressText(String entry) {
        String trimmedDisplayText = entry.trim();
        int index = trimmedDisplayText.indexOf(",");
        return mTokenizer != null && !TextUtils.isEmpty(trimmedDisplayText)
                && index < trimmedDisplayText.length() - 1 ? (String) mTokenizer
                .terminateToken(trimmedDisplayText) : trimmedDisplayText;

    }

    private CharSequence createChip(String entry, boolean pressed) {
        String displayText = createAddressText(entry);
        if (TextUtils.isEmpty(displayText)) {
            return null;
        }
        SpannableString chipText = null;
        // Always leave a blank space at the end of a chip.
        int end = getSelectionEnd();
        int start = mTokenizer.findTokenStart(getText(), end);
        int textLength = displayText.length() - 1;
        chipText = new SpannableString(displayText);
        try {
            TagEditChip chip = constructChipSpan(entry, start, pressed);
            chipText.setSpan(chip, 0, textLength,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            chip.setOriginalText(chipText.toString());
        } catch (NullPointerException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
        return chipText;
    }

    private int getRecipientsNumber() {
        TagEditChip[] recips = getSpannable()
                .getSpans(0, getText().length(), TagEditChip.class);
        return recips.length;
    }

    public String getChipsString() {
        TagEditChip[] recips = getSortedRecipients();
        ;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recips.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(recips[i].getEntry());
        }
        return sb.toString();
    }

    private boolean findSameChip(String entry) {
        TagEditChip[] recips = getSpannable()
                .getSpans(0, getText().length(), TagEditChip.class);
        for (TagEditChip chip : recips) {
            if (entry.equals(chip.getEntry()))
                return true;
        }
        return false;
    }

    private TagEditChip selectChip(TagEditChip currentChip) {
        int start = getChipStart(currentChip);
        int end = getChipEnd(currentChip);
        getSpannable().removeSpan(currentChip);
        TagEditChip newChip;
        try {
            newChip = constructChipSpan(currentChip.getEntry(), start, true);
        } catch (NullPointerException e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
        Editable editable = getText();

        QwertyKeyListener.markAsReplaced(editable, start, end, "");
        if (start == -1 || end == -1) {
            Log.d(TAG, "The chip being selected no longer exists but should.");
        } else {
            editable.setSpan(newChip, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        }
        newChip.setSelected(true);
        setCursorVisible(false);
        return newChip;
    }

    private void unselectChip(TagEditChip chip) {
        int start = getChipStart(chip);
        int end = getChipEnd(chip);
        Editable editable = getText();
        mSelectedChip = null;
        if (start == -1 || end == -1) {
            Log.w(TAG, "The chip doesn't exist or may be a chip a user was editing");
            setSelection(editable.length());
            commitDefault();
        } else {
            getSpannable().removeSpan(chip);
            QwertyKeyListener.markAsReplaced(editable, start, end, "");
            editable.removeSpan(chip);
            try {
                editable.setSpan(constructChipSpan(chip.getEntry(), start, false), start, end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } catch (NullPointerException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        setCursorVisible(true);
        setSelection(editable.length());
    }

    private void removeChip(TagEditChip chip) {
        Spannable spannable = getSpannable();
        int spanStart = spannable.getSpanStart(chip);
        int spanEnd = spannable.getSpanEnd(chip);
        Editable text = getText();
        int toDelete = spanEnd;
        boolean wasSelected = chip == mSelectedChip;
        // Clear that there is a selected chip before updating any text.
        if (wasSelected) {
            mSelectedChip = null;
        }
        // Always remove trailing spaces when removing a chip.
        while (toDelete >= 0 && toDelete < text.length() && text.charAt(toDelete) == ' ') {
            toDelete++;
        }
        spannable.removeSpan(chip);
        if (spanStart >= 0 && toDelete > 0) {
            text.delete(spanStart, toDelete);
        }
        if (wasSelected) {
            clearSelectedChip();
        }
    }

    public void onClick(TagEditChip chip, int offset, float x, float y) {
        if (chip.isSelected()) {
            clearSelectedChip();
        }
    }

    @Override
    public void removeTextChangedListener(TextWatcher watcher) {
        mTextWatcher = null;
        super.removeTextChangedListener(watcher);
    }

    private class RecipientTextWatcher implements TextWatcher {

        @Override
        public void afterTextChanged(Editable s) {
            // If the text has been set to null or empty, make sure we remove
            // all the spans we applied.
            if (TextUtils.isEmpty(s)) {
                // Remove all the chips spans.
                Spannable spannable = getSpannable();
                TagEditChip[] chips = spannable.getSpans(0, getText().length(),
                        TagEditChip.class);
                for (TagEditChip chip : chips) {
                    spannable.removeSpan(chip);
                }
                return;
            }
            // If the user is editing a chip, don't clear it.

            if (mSelectedChip != null
                    /*&& mSelectedChip.getContactId() != TagEditEntry.INVALID_CONTACT*/) {
                setCursorVisible(true);
                setSelection(getText().length());
                clearSelectedChip();
            }
            int length = s.length();
            // Make sure there is content there to parse and that it is
            // not just the commit character.
            if (length > 0) {
                char last;
                int end = getSelectionEnd() == 0 ? 0 : getSelectionEnd() - 1;
                int len = length() - 1;
                if (end != len) {
                    last = s.charAt(end);
                    if (last == COMMIT_CHAR_SEMICOLON || last == COMMIT_CHAR_COMMA) {
                        s.delete(end, end + 1);
                        commitDefault();
                    }
                } else {
                    last = s.charAt(len);
                }
                if (last == COMMIT_CHAR_SEMICOLON || last == COMMIT_CHAR_COMMA) {
                    commitByCharacter();
                } else if (last == COMMIT_CHAR_SPACE) {
                }
                int tokenStart = mTokenizer.findTokenStart(s, getSelectionStart());
                int tokenEnd = mTokenizer.findTokenEnd(s, tokenStart);
                if (s.toString().substring(tokenStart, tokenEnd).length() > MAX_CHIP_TEXT_LIMIT) {
                    s.delete(getSelectionStart() - 1, getSelectionEnd());
                }
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // This is a delete; check to see if the insertion point is on a space
            // following a chip.
            // If the item deleted is a space, and the thing before the
            // space is a chip, delete the entire span.
            int selStart = getSelectionStart();
            TagEditChip[] repl = getSpannable().getSpans(selStart, selStart,
                    TagEditChip.class);
            if (repl.length > 0) {
                // There is a chip there! Just remove it.
                Editable editable = getText();
                // Add the separator token.
                int tokenStart = mTokenizer.findTokenStart(editable, selStart);
                int tokenEnd = mTokenizer.findTokenEnd(editable, tokenStart);
                tokenEnd = tokenEnd + 1;
                if (tokenEnd > editable.length()) {
                    tokenEnd = editable.length();
                }
                editable.delete(tokenStart, tokenEnd);
                getSpannable().removeSpan(repl[0]);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Do nothing.
        }
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        return false;
    }

    public void commitUsualTag(String text) {
        int index = getSelectionStart();
        commitDefault();
        if (!text.isEmpty()) {
            getEditableText().insert(index, text);
            commitDefault();
        }
    }
}
