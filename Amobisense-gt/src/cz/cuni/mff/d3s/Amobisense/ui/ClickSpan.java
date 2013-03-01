package cz.cuni.mff.d3s.Amobisense.ui;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

class ClickSpan extends ClickableSpan {

    private OnClickListener mListener;

    public ClickSpan(OnClickListener listener) {
        mListener = listener;
    }

    @Override
    public void onClick(View widget) {
       if (mListener != null) mListener.onClick();
    }

    public interface OnClickListener {
        void onClick();
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setColor(0xff0000ff); // remove this if you don't want to want to override the textView's color if you specified it in main.xml
    }
}