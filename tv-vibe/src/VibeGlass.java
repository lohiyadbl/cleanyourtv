package com.gathin.tvhome;
import android.animation.ValueAnimator;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.PathInterpolator;

/** Static translucent gradients with brief focus motion; no backdrop blur or continuous GPU pass. */
final class VibeGlass extends Drawable {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean focused;
    VibeGlass(boolean focus) { focused=focus; }
    static boolean motion(android.content.Context context) {
        return ValueAnimator.areAnimatorsEnabled() && context.getSharedPreferences("vibe-dashboard",0).getBoolean("motion",true);
    }
    static void focus(View v, boolean focused) {
        v.setBackground(new VibeGlass(focused));
        v.animate().cancel();
        v.animate().scaleX(focused?1.025f:1f).scaleY(focused?1.025f:1f)
            .translationY(focused?-2*v.getResources().getDisplayMetrics().density:0)
            .setDuration(motion(v.getContext())?200:0).setInterpolator(new PathInterpolator(.2f,.8f,.2f,1)).start();
    }
    @Override public void draw(Canvas c) {
        Rect r=getBounds(); float radius=Math.min(r.width(),r.height())*.16f;
        RectF f=new RectF(r.left+2,r.top+2,r.right-2,r.bottom-2);
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(f.left,f.top,f.right,f.bottom,
            focused?new int[]{0xE53C7597,0xE52C5E86,0xED173A60}:new int[]{0xC53A789F,0xD1275A83,0xE31A3F65},null,Shader.TileMode.CLAMP));
        c.drawRoundRect(f,radius,radius,p); p.setShader(null);
        p.setColor(focused?0x99FFFFFF:0x45FFFFFF); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(focused?2.5f:1.4f);
        c.drawRoundRect(f,radius,radius,p);
        int save=c.save(); Path clip=new Path(); clip.addRoundRect(f,radius,radius,Path.Direction.CW); c.clipPath(clip);
        p.setStyle(Paint.Style.FILL); p.setShader(new LinearGradient(0,f.top,0,f.top+f.height()*.48f,0x24FFFFFF,0x00FFFFFF,Shader.TileMode.CLAMP));
        c.drawRect(f,p); p.setShader(null); c.restoreToCount(save);
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
