package com.gathin.tvhome;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.Random;

/** Decorative static rain glass: no weather claim, video, blur or animation loop. */
final class VibeRain extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    VibeRain(Context context){super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float w=getWidth(),h=getHeight();
        paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xFF72BDD7,0xFF245B83,0xFF102C49},new float[]{0,.48f,1},Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,h,paint);paint.setShader(null);
        Random random=new Random(40706);
        for(int i=0;i<150;i++){
            float x=random.nextFloat()*w,y=random.nextFloat()*h,r=w*(.0015f+random.nextFloat()*.0028f);
            paint.setStyle(Paint.Style.FILL);paint.setColor(0x34204361);canvas.drawOval(x-r,y-r*1.4f,x+r,y+r*1.8f,paint);
            paint.setColor(0x45D8F5FF);paint.setStrokeWidth(Math.max(1,w/1600));canvas.drawLine(x-r*.4f,y-r,x-r*.3f,y-r*.35f,paint);
            if(i%3==0){paint.setColor(0x12315570);paint.setStrokeWidth(r*.7f);canvas.drawLine(x+r*.3f,y+r,x+r*.8f,y+r*7,paint);}
        }
    }
}
