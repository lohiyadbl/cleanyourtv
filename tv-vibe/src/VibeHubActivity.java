package com.gathin.tvhome;
import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.net.URL;

/** Remote-first carousel, preferences, Google Assistant entry point, and offline game menu. */
public final class VibeHubActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout page;
    private TextView title,details,position,status;
    private ImageView poster;
    private Orb orb;
    private JSONArray items=new JSONArray();
    private int current, imageToken;
    private volatile boolean active;
    private float touchX;
    private final ExecutorService images=new ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1),new ThreadPoolExecutor.DiscardOldestPolicy());
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);prefs=getSharedPreferences("vibe-dashboard",0);
        current=state==null?getIntent().getIntExtra("index",0):state.getInt("index");
        try{items=new JSONArray(prefs.getString("movies","[]"));}catch(JSONException ignored){}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF397FA5,0xFF1A466D,0xFF102D49}));
        page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(40),dp(24),dp(40),dp(24));scroll.addView(page);setContentView(scroll);
        TextView header=label("TV VIBE  /  YOUR EVENING",15,0xFF91BFCB);page.addView(header);
        LinearLayout tools=new LinearLayout(this);page.addView(tools);
        button(tools,"‹ Home",()->finish());button(tools,"Your tastes",this::tastes);button(tools,"Games",this::games);
        button(tools,"Jarvis",this::voice);button(tools,"Assistant look",this::assistantLook);
        LinearLayout content=new LinearLayout(this);content.setPadding(0,dp(14),0,dp(10));page.addView(content);
        poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.FIT_CENTER);poster.setContentDescription("Selected title poster");
        content.addView(poster,new LinearLayout.LayoutParams(dp(150),dp(215)));
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);copy.setPadding(dp(24),0,0,0);content.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        position=label("",15,0xFF97E6DA);copy.addView(position);
        title=label("",30,android.graphics.Color.WHITE);copy.addView(title);
        details=label("",16,0xFFD6DFEB);copy.addView(details);
        LinearLayout controls=new LinearLayout(this);copy.addView(controls);
        button(controls,"‹ Previous",()->step(-1));button(controls,"Availability",this::availability);button(controls,"Next ›",()->step(1));
        orb=new Orb();copy.addView(orb,new LinearLayout.LayoutParams(dp(110),dp(65)));
        status=label("Jarvis opens Google Assistant. Its listening and results use Google’s own interface. Search titles separately with the TV keyboard.",13,0xFFABBBD2);page.addView(status);
        LinearLayout lower=new LinearLayout(this);page.addView(lower);button(lower,"Search titles",this::typeQuery);button(lower,"Show all tastes",()->setTastes(0,0,""));
        TextView credit=label("India availability: TMDB / JustWatch · Verify with the platform. IMDb ratings: OMDb.\nThis product uses the TMDB API but is not endorsed or certified by TMDB.",11,0xFF93A3B7);page.addView(credit);
        render();
        if("games".equals(getIntent().getStringExtra("mode")))games();
        if("voice".equals(getIntent().getStringExtra("mode")))voice();
        if("assistant-look".equals(getIntent().getStringExtra("mode")))assistantLook();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(5),0,dp(5));return t;}
    private void button(LinearLayout parent,String text,Runnable action){
        Button b=new Button(this);b.setText(text);b.setTextSize(13);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setBackground(new VibeGlass(false));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(42));lp.setMargins(dp(4),dp(4),dp(4),dp(4));parent.addView(b,lp);
        b.setPadding(dp(14),0,dp(14),0);b.setOnClickListener(v->action.run());b.setOnFocusChangeListener((v,f)->VibeGlass.focus(v,f));
    }
    private JSONObject item(){return items.optJSONObject(current);}
    private void render(){
        if(items.length()==0){current=0;title.setText("Make this evening yours");position.setText("FOR YOU");details.setText("Add your TMDB API key from the Home recommendation card.\nChoose your tastes or search for a title.\nGames work offline without any API key.");poster.setImageDrawable(null);return;}
        current=Math.max(0,Math.min(current,items.length()-1));JSONObject d=item();if(d==null)return;
        position.setText(d.optString("shelf","Your Mix")+"  ·  "+(current+1)+" / "+items.length());
        title.setText(d.optString("title"));details.setText(d.optString("year")+"  ·  "+d.optString("genres")+"\nIMDb "+d.optString("rating")+"\nStream in India: "+d.optString("platforms")+"\nLeft/Right or swipe to browse. OK on Availability for options.");
        title.animate().cancel();title.setAlpha(VibeGlass.motion(this)?0f:1f);title.animate().alpha(1f).setDuration(VibeGlass.motion(this)?180:0).start();
        poster.setImageDrawable(null);loadPoster(d.optString("poster"));
    }
    private void step(int direction){if(items.length()<2)return;current=VibeGameRules.wrap(current,direction,items.length());render();}
    @Override public boolean dispatchKeyEvent(KeyEvent e){
        if(e.getAction()==KeyEvent.ACTION_DOWN && (e.getKeyCode()==KeyEvent.KEYCODE_DPAD_LEFT || e.getKeyCode()==KeyEvent.KEYCODE_DPAD_RIGHT)){
            // On title artwork, horizontal keys browse; buttons retain normal left/right focus navigation.
            View focus=getCurrentFocus();if(focus==poster){step(e.getKeyCode()==KeyEvent.KEYCODE_DPAD_LEFT?-1:1);return true;}
        }
        return super.dispatchKeyEvent(e);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN)touchX=e.getX();
        if(e.getAction()==MotionEvent.ACTION_UP && Math.abs(e.getX()-touchX)>dp(70)) {step(e.getX()<touchX?1:-1);MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();return true;}
        return super.dispatchTouchEvent(e);
    }
    private void availability(){
        JSONObject d=item();if(d==null)return;
        String link=d.optString("link");if(!link.startsWith("https://www.themoviedb.org/"))return;
        try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(link)));}catch(ActivityNotFoundException ex){new AlertDialog.Builder(this).setTitle(d.optString("title")).setMessage("Open one of these apps and search for the title:\n"+d.optString("platforms")).setPositiveButton("Close",null).show();}
    }
    private void tastes(){
        new AlertDialog.Builder(this).setTitle("Choose a genre · All is the default").setSingleChoiceItems(VibeTaste.GENRES,prefs.getInt("genre",0),(d,which)->{
            d.dismiss();new AlertDialog.Builder(this).setTitle("Original language").setSingleChoiceItems(VibeTaste.LANGUAGES,prefs.getInt("language",0),(dialog,lang)->{dialog.dismiss();setTastes(which,lang,"");}).setNegativeButton("Cancel",null).show();
        }).setNegativeButton("Cancel",null).show();
    }
    private void setTastes(int genre,int language,String query){
        prefs.edit().putInt("genre",genre).putInt("language",language).putString("query",query).putLong("tasteRevision",System.currentTimeMillis()).remove("movies").apply();
        Toast.makeText(this,"Saved. Home will refresh recommendations using your API key.",Toast.LENGTH_LONG).show();finish();
    }
    private void games(){new AlertDialog.Builder(this).setTitle("Play · TV remote only").setItems(new String[]{"Neon Rally · Left/Right + OK","Boundary Club · timing cricket · OK"},(d,which)->startActivity(new Intent(this,VibeGameActivity.class).putExtra("cricket",which==1))).setNegativeButton("Close",null).show();}
    private void assistantLook(){
        String[] looks={"Minimal orb","HUD rings","Off"};
        new AlertDialog.Builder(this).setTitle("Jarvis launcher appearance").setSingleChoiceItems(looks,prefs.getInt("orb",0),(d,which)->{prefs.edit().putInt("orb",which).apply();orb.invalidate();d.dismiss();})
          .setNeutralButton("Toggle motion",(d,w)->{prefs.edit().putBoolean("motion",!prefs.getBoolean("motion",true)).apply();orb.invalidate();}).setNegativeButton("Close",null).show();
    }
    private void voice(){
        status.setText("Opening Google Assistant. Jarvis artwork belongs to the launcher; Google controls listening and results.");
        VibeAssistant.open(this);
    }
    private void typeQuery(){
        EditText edit=new EditText(this);edit.setSingleLine(true);edit.setTextColor(Color.WHITE);edit.setHint("Movie title or genre");
        new AlertDialog.Builder(this).setTitle("Search titles or genres").setView(edit).setPositiveButton("Search",(d,w)->acceptQuery(edit.getText().toString())).setNegativeButton("Cancel",null).show();
    }
    private void acceptQuery(String raw){
        String q=raw.trim();if(q.isEmpty())return;String lower=q.toLowerCase(Locale.ROOT);
        int genre=VibeTaste.detectGenre(q),lang=VibeTaste.detectLanguage(q);
        // A whole title is searched unless it is plainly phrased as a genre request.
        boolean category=VibeTaste.genreRequest(q);
        setTastes(category?genre:0,category?lang:0,category?"":q);
    }
    private void loadPoster(String path){
        if(!path.matches("/[a-zA-Z0-9._-]+"))return;
        final int token=++imageToken;
        images.execute(()->{
            if(!active || token!=imageToken)return;
            javax.net.ssl.HttpsURLConnection c=null;Bitmap image=null;
            try{
                c=(javax.net.ssl.HttpsURLConnection)new URL("https://image.tmdb.org/t/p/w185"+path).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(5000);c.setInstanceFollowRedirects(false);
                if(c.getResponseCode()!=200)return;
                try(InputStream in=c.getInputStream();ByteArrayOutputStream b=new ByteArrayOutputStream()){
                    byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(b.size()+n>350000 || Thread.currentThread().isInterrupted())return;b.write(buf,0,n);}
                    byte[] bytes=b.toByteArray();image=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                }
            }catch(Exception ignored){}finally{if(c!=null)c.disconnect();}
            final Bitmap result=image;runOnUiThread(()->{if(!isDestroyed() && token==imageToken){poster.setImageBitmap(result);poster.setAlpha(VibeGlass.motion(this)?0f:1f);poster.animate().alpha(1f).setDuration(VibeGlass.motion(this)?180:0).start();}});
        });
    }
    @Override protected void onResume(){super.onResume();active=true;poster.setFocusable(true);poster.requestFocus();render();orb.invalidate();}
    @Override protected void onPause(){active=false;super.onPause();}
    @Override protected void onDestroy(){imageToken++;images.shutdownNow();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle b){b.putInt("index",current);super.onSaveInstanceState(b);}
    private final class Orb extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Orb(){super(VibeHubActivity.this);setContentDescription("Decorative Jarvis orb; select Jarvis to open Google Assistant");}
        @Override protected void onDraw(Canvas c){super.onDraw(c);int style=prefs.getInt("orb",0);if(style==2)return;
            float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)*.5f;
            float phase=VibeGlass.motion(VibeHubActivity.this)?(float)(System.currentTimeMillis()%6000)/6000f:0f;
            p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(x,y,r*1.7f,new int[]{0xFFCEFFFF,0xFF40BDD7,0x00194E98},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r*1.7f,p);p.setShader(null);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(1));p.setColor(0xBB77E6F1);c.drawCircle(x,y,r*(1.25f+.08f*(float)Math.sin(phase*6.28)),p);
            if(style==1){p.setColor(0xFF8EBBFF);c.save();c.rotate(phase*360,x,y);c.drawArc(x-r*1.8f,y-r*1.8f,x+r*1.8f,y+r*1.8f,20,230,false,p);c.restore();}
            if(active && VibeGlass.motion(VibeHubActivity.this))postInvalidateDelayed(66);
        }
    }
}
