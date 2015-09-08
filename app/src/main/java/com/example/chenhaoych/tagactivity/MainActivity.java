package com.example.chenhaoych.tagactivity;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;

import com.example.chenhaoych.tagactivity.tagmanager.TagsEditor;
import com.example.chenhaoych.tagactivity.view.AutoWrapLineLayout;
import com.example.chenhaoych.tagactivity.view.ViewUtils;


public class MainActivity extends FragmentActivity {

    static final String[] USUAL_TAGS = {"aaaa", "bbbbbbbb", "ccccccc", "aaa", "bbbbb", "aaaaaaaa", "dd", "eeeeee"};
    private TagsEditor mTagEditText;

    private AutoWrapLineLayout mContainer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTagEditText = (TagsEditor) findViewById(R.id.tag_content);
//        mTagWords = (TextView) findViewById(R.id.talent_buyer_publish_tag_content_words);
        mContainer = (AutoWrapLineLayout) findViewById(R.id.usual_tags_container);
        mContainer.removeAllViews();
        for(final String text : USUAL_TAGS){
            mContainer.addView(ViewUtils.getTagLayout(this, text, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTagEditText.commitUsualTag(text);
                }
            }));
        }

    }

}
