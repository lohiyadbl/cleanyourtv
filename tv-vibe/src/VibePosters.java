package com.gathin.tvhome;
import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import org.json.*;
import java.io.*;
import java.net.URL;
import java.util.concurrent.*;

/** Four tiny TMDB posters, fetched on demand with one worker and a bounded queue. */
final class VibePosters extends Drawable {
    private final Activity owner;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final LruCache<String,Bitmap> cache=new LruCache<>(8);
    private final ExecutorService worker=new ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(4),new ThreadPoolExecutor.DiscardOldestPolicy());
    private final String[] paths={"","","",""};
    private volatile boolean active,closed;
    private int selected;
    void select(int index){selected=index;invalidateSelf();}
    VibePosters(Activity activity){owner=activity;}
    void active(boolean enabled){active=enabled;}
    void close(){closed=true;active=false;worker.shutdownNow();cache.evictAll();}
    void show(JSONArray rows){
        for(int i=0;i<4;i++){
            JSONObject item=rows.optJSONObject(i);String path=item==null?"":item.optString("poster");paths[i]=path;
            if(path.matches("/[a-zA-Z0-9._-]+") && cache.get(path)==null && active && !closed)load(path);
        }
        invalidateSelf();
    }
    private void load(String path){
        worker.execute(()->{
            if(!active || closed || cache.get(path)!=null)return;
            javax.net.ssl.HttpsURLConnection connection=null;Bitmap bitmap=null;
            try{
                connection=(javax.net.ssl.HttpsURLConnection)new URL("https://image.tmdb.org/t/p/w185"+path).openConnection();
                connection.setConnectTimeout(5000);connection.setReadTimeout(5000);connection.setInstanceFollowRedirects(false);
                if(connection.getResponseCode()!=200)return;
                try(InputStream input=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] bytes=new byte[4096];int n;
                    while((n=input.read(bytes))!=-1){if(out.size()+n>350000 || Thread.currentThread().isInterrupted())return;out.write(bytes,0,n);}
                    byte[] bytesAll=out.toByteArray();BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
                    BitmapFactory.decodeByteArray(bytesAll,0,bytesAll.length,bounds);
                    if(bounds.outWidth<1 || bounds.outHeight<1 || bounds.outWidth>500 || bounds.outHeight>1000)return;
                    bitmap=BitmapFactory.decodeByteArray(bytesAll,0,bytesAll.length);
                }
            }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}
            final Bitmap ready=bitmap;
            owner.runOnUiThread(()->{if(!closed && ready!=null){cache.put(path,ready);invalidateSelf();}});
        });
    }
    @Override public void draw(Canvas canvas){
        Rect bounds=getBounds();float gap=bounds.width()*.025f,width=(bounds.width()-3*gap)/4f;
        for(int i=0;i<4;i++){
            RectF cell=new RectF(bounds.left+i*(width+gap),bounds.top,bounds.left+i*(width+gap)+width,bounds.bottom);
            paint.setShader(new LinearGradient(cell.left,cell.top,cell.right,cell.bottom,0xBB4A91B7,0xDD193B5D,Shader.TileMode.CLAMP));
            canvas.drawRoundRect(cell,8,8,paint);paint.setShader(null);
            Bitmap bitmap=cache.get(paths[i]);
            if(bitmap!=null){int save=canvas.save();Path clip=new Path();clip.addRoundRect(cell,8,8,Path.Direction.CW);canvas.clipPath(clip);canvas.drawBitmap(bitmap,null,cell,paint);canvas.restoreToCount(save);}
            else{paint.setColor(0xBBDFF6FF);paint.setTextSize(Math.max(10,cell.width()*.15f));canvas.drawText("FOR YOU",cell.left+cell.width()*.1f,cell.centerY(),paint);}
            if(i==selected){paint.setShader(null);paint.setColor(0xFFF2FAFF);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);canvas.drawRoundRect(cell,8,8,paint);paint.setStyle(Paint.Style.FILL);}
        }
    }
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter filter){}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
