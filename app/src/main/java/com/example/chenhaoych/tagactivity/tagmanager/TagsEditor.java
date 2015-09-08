package com.example.chenhaoych.tagactivity.tagmanager;

import android.content.Context;
import android.text.Annotation;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.MultiAutoCompleteTextView;


public class TagsEditor extends TagEditTextView {
    private char mLastSeparator = ',';

    public TagsEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        RecipientsEditorTokenizer mTokenizer = new RecipientsEditorTokenizer();
        setTokenizer(mTokenizer);
        setThreshold(1);
        addTextChangedListener(new TextWatcher() {
            private Annotation[] mAffected;

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                    int count, int after) {
                mAffected = ((Spanned) s).getSpans(start, start + count,
                        Annotation.class);
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                    int before, int after) {
                if (before == 0 && after == 1) {    // inserting a character
                    char c = s.charAt(start);
                    if (c == ',' || c == '，') {
                        // Remember the delimiter the user typed to end this recipient. We'll
                        // need it shortly in terminateToken().
                        mLastSeparator = c;
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mAffected != null) {
                    for (Annotation a : mAffected) {
                        s.removeSpan(a);
                    }
                }
                mAffected = null;
            }
        });
    }

    @Override
    public boolean enoughToFilter() {
        if (!super.enoughToFilter()) {
            return false;
        }
        int end = getSelectionEnd();
        int len = getText().length();

        return end == len;

    }

    private class RecipientsEditorTokenizer
            implements MultiAutoCompleteTextView.Tokenizer {

        @Override
        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            char c;

            // If we're sitting at a delimiter, back up so we find the previous token
            if (i > 0 && ((c = text.charAt(i - 1)) == ',' || c == '，')) {
                --i;
            }
            // Now back up until the start or until we find the separator of the previous token
            while (i > 0 && (c = text.charAt(i - 1)) != ',' && c != '，') {
                i--;
            }
            while (i < cursor && text.charAt(i) == ' ') {
                i++;
            }

            return i;
        }

        @Override
        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();
            char c;

            while (i < len) {
                if ((c = text.charAt(i)) == ',' || c == '，') {
                    return i;
                } else {
                    i++;
                }
            }

            return len;
        }

        @Override
        public CharSequence terminateToken(CharSequence text) {
            int i = text.length();

            while (i > 0 && text.charAt(i - 1) == ' ') {
                i--;
            }

            char c;
            if (i > 0 && ((c = text.charAt(i - 1)) == ',' || c == '，')) {
                return text;
            } else {
                // Use the same delimiter the user just typed.
                // This lets them have a mixture of commas and semicolons in their list.
                String separator = mLastSeparator + " ";
                if (text instanceof Spanned) {
                    SpannableString sp = new SpannableString(text + separator);
                    TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
                            Object.class, sp, 0);
                    return sp;
                } else {
                    return text + separator;
                }
            }
        }
    }

}
