package com.josjis.tv;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

public final class Design {
    public static final int BG = Color.rgb(9, 10, 14);
    public static final int PANEL = Color.rgb(27, 30, 39);
    public static final int ACCENT = Color.rgb(243, 77, 91);
    public static final int WHITE = Color.WHITE;
    public static final int MUTED = Color.rgb(169, 173, 184);
    public static int dp(Context context, int value) { return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f); }
    public static GradientDrawable rounded(int background, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(background); shape.setCornerRadius(radius);
        return shape;
    }
    public static GradientDrawable gradient(int one, int two, int radius) {
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{one, two});
        shape.setCornerRadius(radius); return shape;
    }
    public static TextView text(Context context, String value, int size, int color, boolean bold) {
        TextView tv = new TextView(context);
        tv.setText(value); tv.setTextSize(size); tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }
    public static TextView button(Context context, String label, boolean primary, Runnable action) {
        TextView tv = text(context, label, 14, WHITE, true);
        tv.setGravity(Gravity.CENTER); tv.setPadding(dp(context,20),dp(context,12),dp(context,20),dp(context,12));
        tv.setBackground(rounded(primary ? ACCENT : PANEL, dp(context,10)));
        tv.setFocusable(true); tv.setClickable(true);
        tv.setOnClickListener(v -> action.run());
        tv.setOnFocusChangeListener((v, focused) -> {
            GradientDrawable bg = rounded(focused ? WHITE : (primary ? ACCENT : PANEL), dp(context,10));
            tv.setBackground(bg); tv.setTextColor(focused ? BG : WHITE);
            v.animate().scaleX(focused ? 1.045f : 1f).scaleY(focused ? 1.045f : 1f).setDuration(120).start();
        });
        return tv;
    }
    private Design() { }
}
