package com.josjis.tv;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.net.URI;

/**
 * The service only documents an embedded iframe + postMessage controller.
 * This activity does not extract, intercept, decrypt, or rehost video URLs.
 */
public final class PlayerActivity extends Activity {
    private WebView web;
    private FrameLayout root;
    private TextView status;
    private TextView time;
    private TextView toggle;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean playing = true;
    private boolean embeddedControls = false;
    private int id, season, episode;
    private String type, title;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(5894 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        id = getIntent().getIntExtra("id",0);
        type = getIntent().getStringExtra("type");
        title = getIntent().getStringExtra("title");
        season = Math.max(1,getIntent().getIntExtra("season",1));
        episode = Math.max(1,getIntent().getIntExtra("episode",1));
        if (id < 1 || (!"movie".equals(type) && !"tv".equals(type))) { finish(); return; }
        if (title == null) title = "JosJisTV";
        root = new FrameLayout(this); root.setBackgroundColor(Color.BLACK);
        web = new WebView(this); web.setBackgroundColor(Color.BLACK);
        web.setFocusable(false); web.setFocusableInTouchMode(false);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                String host = request.getUrl().getHost();
                // Prevent an embedded player from redirecting the TV app to external web sites.
                return !("framextv.xyz".equalsIgnoreCase(host) || "framextv.cc".equalsIgnoreCase(host));
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame() && status != null) status.setText("Gagal membuka player. Periksa internet.");
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreenView != null) { callback.onCustomViewHidden(); return; }
                fullscreenView = view; fullscreenCallback = callback;
                root.addView(view,new FrameLayout.LayoutParams(-1,-1));
                web.setVisibility(View.INVISIBLE);
            }
            @Override public void onHideCustomView() { exitFullscreen(); }
        });
        root.addView(web,new FrameLayout.LayoutParams(-1,-1));
        buildControls(); setContentView(root);
        loadPlayer(); handler.postDelayed(pollProgress,1100);
    }
    private int d(int v) { return Design.dp(this,v); }
    private LinearLayout horiz() { LinearLayout v=new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private void space(LinearLayout v,int width){ v.addView(new View(this),new LinearLayout.LayoutParams(d(width),1)); }
    private void buildControls() {
        LinearLayout top=horiz(); top.setPadding(d(28),d(15),d(22),d(15));
        top.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xE0000000,0x00000000}));
        TextView back=Design.button(this,"←  Kembali",false,this::finish);
        top.addView(back,new LinearLayout.LayoutParams(-2,d(50))); space(top,25);
        String label=title+("tv".equals(type)?"   •   S"+season+"E"+episode:"");
        TextView heading=Design.text(this,label,22,Design.WHITE,true);
        heading.setSingleLine(true); heading.setEllipsize(android.text.TextUtils.TruncateAt.END);
        top.addView(heading,new LinearLayout.LayoutParams(0,d(50),1f));
        TextView chip=Design.text(this,"FRAME XTV · EMBED",11,Design.MUTED,true);
        top.addView(chip,new LinearLayout.LayoutParams(-2,d(50)));
        FrameLayout.LayoutParams topLp=new FrameLayout.LayoutParams(-1,d(88),Gravity.TOP);
        root.addView(top,topLp);

        LinearLayout bottom=new LinearLayout(this); bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setPadding(d(28),d(18),d(28),d(17));
        bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xF0000000,0xC0000000,0x00000000}));
        status=Design.text(this,"Menghubungkan ke FrameXTV…",12,Design.MUTED,false);
        bottom.addView(status,new LinearLayout.LayoutParams(-1,d(27)));
        LinearLayout bar=horiz();
        toggle=Design.button(this,"Ⅱ  Pause",true,this::togglePlayback);
        bar.addView(toggle,new LinearLayout.LayoutParams(-2,d(49))); space(bar,10);
        bar.addView(Design.button(this,"↶  −10s",false,()->send("seekBy","seconds",-10)),new LinearLayout.LayoutParams(-2,d(49)));
        space(bar,10);
        bar.addView(Design.button(this,"+10s  ↷",false,()->send("seekBy","seconds",10)),new LinearLayout.LayoutParams(-2,d(49)));
        space(bar,10);
        bar.addView(Design.button(this,"Subtitle",false,()->send("setSubtitle","index",0)),new LinearLayout.LayoutParams(-2,d(49)));
        space(bar,10);
        bar.addView(Design.button(this,"Server Auto",false,()->{
            loadPlayer(); status.setText("Mencoba ulang dengan server otomatis…");
        }),new LinearLayout.LayoutParams(-2,d(49)));
        space(bar,10);
        bar.addView(Design.button(this,"Kontrol bawaan",false,()->{
            embeddedControls=!embeddedControls;
            loadPlayer(); status.setText(embeddedControls?"Kontrol FrameXTV aktif":"Kontrol JosJisTV aktif");
        }),new LinearLayout.LayoutParams(-2,d(49)));
        space(bar,20);
        time=Design.text(this,"--:-- / --:--",13,Design.WHITE,true);
        bar.addView(time,new LinearLayout.LayoutParams(-2,d(49)));
        bottom.addView(bar,new LinearLayout.LayoutParams(-1,d(52)));
        FrameLayout.LayoutParams bottomLp=new FrameLayout.LayoutParams(-1,d(135),Gravity.BOTTOM);
        root.addView(bottom,bottomLp);
        back.requestFocus();
    }
    private String embedUrl() {
        return FramexLinks.embed(id, type, season, episode, embeddedControls);
    }
    private void loadPlayer() {
        String page = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'/>"+
                "<style>*{box-sizing:border-box}html,body{height:100%;margin:0;background:#000;overflow:hidden}iframe{border:0;display:block;width:100%;height:100%}</style></head>"+
                "<body><iframe id='player' src='"+embedUrl()+"' allow='autoplay; fullscreen; encrypted-media; picture-in-picture' allowfullscreen></iframe>"+
                "<script>window.josjisProgress={currentTime:0,duration:0};"+
                "window.addEventListener('message',function(e){if(e.origin!=='https://framextv.xyz')return;"+
                "if(e.data&&e.data.event==='frameXTV:timeupdate'){"+
                "window.josjisProgress.currentTime=Number(e.data.currentTime)||0;"+
                "window.josjisProgress.duration=Number(e.data.duration)||0;}});"+
                "window.josjisSend=function(action,args){var f=document.getElementById('player');"+
                "if(f&&f.contentWindow){var msg=Object.assign({action:action},args||{});"+
                "f.contentWindow.postMessage(msg,'https://framextv.xyz');}};"+
                "</script></body></html>";
        web.loadDataWithBaseURL("https://josjistv.app/",page,"text/html","UTF-8",null);
        if(status!=null) status.setText("Player disematkan. Ketersediaan bergantung pada penyedia.");
        playing=true;
        if(toggle!=null) toggle.setText("Ⅱ  Pause");
    }
    private void send(String action,String key,int value) {
        if(web==null) return;
        String args=key==null?"{}":"{\""+key+"\":"+value+"}";
        web.evaluateJavascript("window.josjisSend && window.josjisSend('"+action+"',"+args+");",null);
    }
    private void togglePlayback() {
        playing=!playing; send(playing?"play":"pause",null,0);
        toggle.setText(playing?"Ⅱ  Pause":"▶  Play");
    }
    private final Runnable pollProgress = new Runnable() {
        @Override public void run() {
            if(web==null || isFinishing()) return;
            web.evaluateJavascript("(function(){var p=window.josjisProgress||{};return Math.floor(p.currentTime||0)+'|'+Math.floor(p.duration||0)})()",value->{
                if(time==null || value==null) return;
                try {
                    String[] pieces=value.replace("\"","").split("\\|");
                    int current=Integer.parseInt(pieces[0]); int duration=Integer.parseInt(pieces[1]);
                    if(duration>0) time.setText(clock(current)+" / "+clock(duration));
                } catch(Exception ignored) { }
            });
            handler.postDelayed(this,1600);
        }
    };
    private String clock(int seconds) { return String.format(java.util.Locale.US,"%02d:%02d",seconds/60,seconds%60); }
    private void exitFullscreen() {
        if(fullscreenView==null) return;
        root.removeView(fullscreenView); fullscreenView=null;
        web.setVisibility(View.VISIBLE);
        if(fullscreenCallback!=null) { fullscreenCallback.onCustomViewHidden(); fullscreenCallback=null; }
    }
    @Override public boolean onKeyDown(int keyCode,KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode==KeyEvent.KEYCODE_MEDIA_PLAY || keyCode==KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if(keyCode==KeyEvent.KEYCODE_MEDIA_PLAY && !playing) togglePlayback();
            else if(keyCode==KeyEvent.KEYCODE_MEDIA_PAUSE && playing) togglePlayback();
            else if(keyCode==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) togglePlayback();
            return true;
        }
        if(keyCode==KeyEvent.KEYCODE_MEDIA_FAST_FORWARD){send("seekBy","seconds",10);return true;}
        if(keyCode==KeyEvent.KEYCODE_MEDIA_REWIND){send("seekBy","seconds",-10);return true;}
        return super.onKeyDown(keyCode,event);
    }
    @Override public void onBackPressed() { if(fullscreenView!=null) exitFullscreen(); else super.onBackPressed(); }
    @Override protected void onPause() { super.onPause(); if(web!=null) web.onPause(); }
    @Override protected void onResume() { super.onResume(); if(web!=null) web.onResume(); }
    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        exitFullscreen();
        if(web!=null) { web.loadUrl("about:blank"); web.stopLoading(); root.removeView(web); web.destroy(); web=null; }
        super.onDestroy();
    }
}
