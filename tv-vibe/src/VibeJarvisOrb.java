package com.gathin.tvhome;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;

/** Decorative launcher skin only. It has no microphone or Assistant response connection. */
final class VibeJarvisOrb extends Drawable implements Runnable {
    private final Context context;
    private final SharedPreferences prefs;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean active;
    VibeJarvisOrb(Context ctx){context=ctx;prefs=ctx.getSharedPreferences("vibe-dashboard",0);}
    void active(boolean enabled){active=enabled;unscheduleSelf(this);invalidateSelf();}
    @Override public void draw(Canvas c){
        int style=prefs.getInt("orb",0);if(style==2)return;
        Rect b=getBounds();float x=b.exactCenterX(),y=b.exactCenterY(),r=Math.min(b.width(),b.height())*.31f;
        boolean motion=active && VibeGlass.motion(context);
        float phase=motion?(SystemClock.uptimeMillis()%6000)/6000f:0;
        paint.setStyle(Paint.Style.FILL);paint.setShader(new RadialGradient(x-r*.4f,y-r*.45f,r*2,new int[]{0xFFFCF8FF,0xFFBCC9E5,0xFF566C98,0x003F719C},null,Shader.TileMode.CLAMP));
        c.drawCircle(x,y,r*(1+.025f*(float)Math.sin(phase*6.28f)),paint);paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(1,b.width()*.012f));paint.setColor(0xBBD5F6FF);
        if(style==1){c.drawCircle(x,y,r*1.25f,paint);int save=c.save();c.rotate(phase*360,x,y);c.drawArc(x-r*1.5f,y-r*1.5f,x+r*1.5f,y+r*1.5f,20,235,false,paint);c.restoreToCount(save);}
        if(motion)scheduleSelf(this,SystemClock.uptimeMillis()+80);
    }
    @Override public void run(){if(active)invalidateSelf();}
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter filter){}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
