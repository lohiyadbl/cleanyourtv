package com.gathin.tvhome;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import java.util.Arrays;

/** Two small offline remote games. No billing, downloads, account or network access. */
public final class VibeGameActivity extends Activity {
    private Game view;
    @Override public void onCreate(Bundle state) { super.onCreate(state); view=new Game(getIntent().getBooleanExtra("cricket",false)); setContentView(view); }
    @Override protected void onResume() { super.onResume(); view.active=true; view.last=0; view.postInvalidateOnAnimation(); }
    @Override protected void onPause() { view.active=false; view.held=0; view.playing=false; super.onPause(); }
    private final class Game extends View {
        final boolean cricket;
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final boolean[] bricks=new boolean[24];
        boolean active,playing,over;
        int held,score,lives=3,balls,wickets;
        float paddle=480,x=480,y=330,vx=155,vy=-185,phase;
        long last;
        String message="Press OK to start";
        Game(boolean mode) { super(VibeGameActivity.this); cricket=mode; Arrays.fill(bricks,true); setFocusable(true); setFocusableInTouchMode(true); requestFocus(); }
        void text(Canvas c,String value,float xx,float yy,float size,int color) { paint.setShader(null); paint.setColor(color); paint.setTextSize(size); paint.setStyle(Paint.Style.FILL); c.drawText(value,xx,yy,paint); }
        void rectangle(Canvas c,float l,float t,float r,float b,int color) { paint.setColor(color);paint.setShader(null);paint.setStyle(Paint.Style.FILL);c.drawRoundRect(l,t,r,b,8,8,paint); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas); canvas.drawColor(0xFF080F1B);
            long now=System.nanoTime(); float dt=last==0?0:Math.min(.04f,(now-last)/1_000_000_000f); last=now;
            if(active && playing && !over) update(dt);
            int save=canvas.save(); float scale=Math.min(getWidth()/960f,getHeight()/540f);
            canvas.translate((getWidth()-960*scale)/2,(getHeight()-540*scale)/2);canvas.scale(scale,scale);
            text(canvas,cricket?"BOUNDARY CLUB":"NEON RALLY",48,57,28,Color.WHITE);
            text(canvas,cricket?"12 balls · 3 wickets · timing cricket":"Clear the neon wall · 3 lives",48,86,15,0xFFABBBD2);
            text(canvas,cricket?"Score "+score+"  •  "+balls+"/12 balls  •  "+wickets+" wickets":"Score "+score+"  •  Lives "+lives,48,124,20,0xFF72E6DC);
            if(cricket) {
                rectangle(canvas,60,190,900,365,0xFF102B2E);
                rectangle(canvas,640,205,678,350,0xFF2D8069);
                rectangle(canvas,650,205,666,350,0xFF65D9A9);
                paint.setColor(0xFFFFD17E);canvas.drawCircle(80+phase*800,280,13,paint);
                text(canvas,"Hit OK as the ball enters the green strike zone",60,405,22,Color.WHITE);
                text(canvas,"OK: bowl / swing · Back: leave game",60,492,17,0xFFABBBD2);
            } else {
                for(int i=0;i<bricks.length;i++) if(bricks[i]) rectangle(canvas,64+(i%8)*105,155+(i/8)*30,160+(i%8)*105,177+(i/8)*30, i/8==0?0xFFB39CFF:i/8==1?0xFF65D9E1:0xFFFFBA7B);
                rectangle(canvas,paddle-60,464,paddle+60,477,0xFF88DDFF);
                paint.setColor(Color.WHITE);canvas.drawCircle(x,y,8,paint);
                text(canvas,"Left/Right: move · OK: start/pause · Back: leave",60,520,17,0xFFABBBD2);
            }
            text(canvas,message,60,443,19,0xFFFFD17E); canvas.restoreToCount(save);
            if(active && playing && !over) postInvalidateDelayed(33);
        }
        void update(float dt) {
            if(cricket) { phase+=dt*.64f;if(phase>1.03f) finishBall(0);return; }
            paddle=Math.max(64,Math.min(896,paddle+held*430*dt)); x+=vx*dt;y+=vy*dt;
            if(x<44 || x>916) {x=Math.max(44,Math.min(916,x));vx=-vx;}
            if(y<140) {y=140;vy=Math.abs(vy);}
            if(vy>0 && y>=454 && y<480 && Math.abs(x-paddle)<67) {y=453;vy=-Math.abs(vy);vx=(x-paddle)*3.5f;}
            for(int i=0;i<bricks.length;i++) if(bricks[i]) {
                float l=64+(i%8)*105,t=155+(i/8)*30;
                if(x>l-6 && x<l+102 && y>t-7 && y<t+29) {bricks[i]=false;score+=10;vy=-vy;break;}
            }
            boolean any=false;for(boolean b:bricks) any|=b;
            if(!any) {over=true;playing=false;message="Wall cleared! OK to play again";}
            if(y>505) {lives--;playing=false;x=paddle;y=330;vy=-185; if(lives<=0) {over=true;message="Game over · OK to play again";}else message="Press OK for your next ball";}
        }
        void finishBall(int runs) {
            score+=runs;balls++;if(runs==0)wickets++;
            playing=false;over=VibeGameRules.inningsOver(balls,wickets);
            message=(runs==0?"Bowled!":runs+" runs!")+(over?" Innings complete · OK to restart":" OK for next ball");
        }
        void reset() {score=0;lives=3;balls=0;wickets=0;over=false;Arrays.fill(bricks,true);x=480;y=330;vx=155;vy=-185;paddle=480;phase=0;}
        @Override public boolean onKeyDown(int code,KeyEvent event) {
            if(code==KeyEvent.KEYCODE_DPAD_LEFT){held=-1;return true;}
            if(code==KeyEvent.KEYCODE_DPAD_RIGHT){held=1;return true;}
            if(code==KeyEvent.KEYCODE_DPAD_CENTER || code==KeyEvent.KEYCODE_ENTER || code==KeyEvent.KEYCODE_BUTTON_A) {
                if(event.getRepeatCount()>0) return true;
                if(over)reset();
                if(cricket) {if(playing)finishBall(VibeGameRules.cricketRuns(phase));else {phase=0;playing=true;message="Time your shot…";}}
                else {playing=!playing;message=playing?"":"Paused · OK to resume";}
                last=0;invalidate();return true;
            }
            return super.onKeyDown(code,event);
        }
        @Override public boolean onKeyUp(int code,KeyEvent event) { if(code==KeyEvent.KEYCODE_DPAD_LEFT || code==KeyEvent.KEYCODE_DPAD_RIGHT){held=0;return true;}return super.onKeyUp(code,event); }
    }
}
