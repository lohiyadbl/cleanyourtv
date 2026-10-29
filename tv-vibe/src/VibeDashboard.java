package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

/** Private TV Vibe extension: visible-only requests, no login access, no tracking SDK. */
final class VibeDashboard {
    private final Activity a;
    private final SharedPreferences p;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService net = Executors.newFixedThreadPool(2);
    private final TextView weather, movies, subs, play, assistant, featured;
    private final VibePosters posters;
    private final VibeJarvisOrb jarvisOrb;
    private int movieIndex;
    private long tasteRevision;
    private float touchStart;
    private final Runnable down, up;
    private volatile boolean visible, closed;
    private boolean weatherBusy, moviesBusy;
    private long weatherAttempt, moviesAttempt;
    private String weatherDetail = "Waiting for a weather update.";
    private JSONArray recommendations = new JSONArray();
    private final Runnable tick = new Runnable() {
        @Override public void run() { if (visible && !closed) { refresh(); ui.postDelayed(this, 60000); } }
    };
    VibeDashboard(Activity activity, Runnable downAction, Runnable upAction) {
        a = activity; down = downAction; up = upAction;
        p = a.getSharedPreferences("vibe-dashboard", 0);
        FrameLayout root=a.findViewById(R.id.root);
        android.view.ViewGroup content=(android.view.ViewGroup)a.findViewById(R.id.leftBlock).getParent().getParent();
        int contentIndex=root.indexOfChild(content);
        root.addView(new VibeRain(a),contentIndex,new FrameLayout.LayoutParams(-1,-1));
        View leftBackground=new View(a);leftBackground.setBackground(new VibeGlass(false));
        FrameLayout.LayoutParams leftLp=new FrameLayout.LayoutParams(dp(305),dp(306));leftLp.leftMargin=dp(40);leftLp.topMargin=dp(20);
        root.addView(leftBackground,root.indexOfChild(content),leftLp);
        View clock=a.findViewById(R.id.leftBlock);
        android.widget.RelativeLayout.LayoutParams clockLp=(android.widget.RelativeLayout.LayoutParams)clock.getLayoutParams();
        clockLp.width=dp(270);clockLp.removeRule(android.widget.RelativeLayout.START_OF);clock.setLayoutParams(clockLp);
        ((TextView)a.findViewById(R.id.clock)).setTextSize(60);
        ((TextView)a.findViewById(R.id.date)).setTextSize(12);
        View controls=a.findViewById(R.id.topButtons);
        ((android.view.ViewGroup)controls.getParent()).removeView(controls);
        FrameLayout.LayoutParams controlsLp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);controlsLp.topMargin=dp(10);controlsLp.rightMargin=dp(24);
        controls.setScaleX(.78f);controls.setScaleY(.78f);controls.setPivotX(0);controls.setPivotY(0);
        root.addView(controls,controlsLp);
        FrameLayout overlay=new FrameLayout(a);root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        weather=panel("NERUL\nWeather loading…",overlay,56,150,270,96,false);weather.setTextSize(17);
        subs=panel("SUBSCRIPTIONS\nSelect to add dates",overlay,56,255,270,54,false);subs.setTextSize(11);subs.setPadding(dp(10),dp(3),dp(10),dp(3));
        movies=panel("FOR YOU\nSelect to set up movie data",overlay,364,70,382,160,true);movies.setTextSize(11);movies.setMaxLines(3);
        posters=new VibePosters(a);posters.setBounds(0,0,dp(350),dp(88));movies.setCompoundDrawables(null,posters,null,null);movies.setCompoundDrawablePadding(dp(5));
        featured=panel("SWIPE\nLeft/Right to browse\nOK for details",overlay,758,70,158,160,true);featured.setTextSize(12);
        play=panel("ARCADE\nPlay\n2 offline games",overlay,364,244,270,82,true);
        assistant=panel("JARVIS\nJust ask.\nGoogle Assistant",overlay,646,244,270,82,true);
        jarvisOrb=new VibeJarvisOrb(a);jarvisOrb.setBounds(0,0,dp(54),dp(54));
        assistant.setCompoundDrawables(null,null,jarvisOrb,null);
        assistant.setContentDescription("Jarvis: open Google Assistant");
        assistant.setOnClickListener(v->VibeAssistant.open(a));
        assistant.setOnLongClickListener(v->{a.startActivity(new Intent(a,VibeHubActivity.class).putExtra("mode","assistant-look"));return true;});
        featured.setOnClickListener(v->movies.performClick());
        TextView[] tiles={weather,movies,featured,play,assistant,subs};
        for(int i=0;i<tiles.length;i++){
            final int pos=i;
            tiles[i].setOnKeyListener((v,code,event)->{
                if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
                if(code==KeyEvent.KEYCODE_DPAD_UP){if(pos==3){movies.requestFocus();return true;}if(pos==4){featured.requestFocus();return true;}if(pos==5){weather.requestFocus();return true;}up.run();return true;}
                if(code==KeyEvent.KEYCODE_DPAD_DOWN){if(pos==1){play.requestFocus();return true;}if(pos==2){assistant.requestFocus();return true;}if(pos==0){subs.requestFocus();return true;}down.run();return true;}
                if(v==movies && recommendations.length()>1){
                    if(code==KeyEvent.KEYCODE_DPAD_LEFT && movieIndex>0){movieIndex--;renderMovies();return true;}
                    if(code==KeyEvent.KEYCODE_DPAD_RIGHT && movieIndex<recommendations.length()-1){movieIndex++;renderMovies();return true;}
                }
                if(code==KeyEvent.KEYCODE_DPAD_LEFT){tiles[pos==4?3:pos==3||pos==5?0:Math.max(0,pos-1)].requestFocus();return true;}
                if(code==KeyEvent.KEYCODE_DPAD_RIGHT){tiles[pos==3?4:pos==5?3:pos==2?2:Math.min(2,pos+1)].requestFocus();return true;}
                return false;
            });
        }
        a.findViewById(R.id.hero).setVisibility(View.GONE);
        weather.setOnClickListener(v -> new AlertDialog.Builder(a).setTitle("Nerul · 400706")
            .setMessage(weatherDetail + "\n\nOpen-Meteo / CAMS model estimate near Nerul (19.033, 73.020). US AQI scale; not an Indian CPCB station reading.")
            .setPositiveButton("Close", null).setNeutralButton("Refresh", (d,w) -> { weatherAttempt = 0; refresh(); }).show());
        movies.setOnClickListener(v -> {
            if (p.getString("tmdb", "").isEmpty()) keys();
            else a.startActivity(new Intent(a, VibeHubActivity.class).putExtra("index", movieIndex));
        });
        movies.setOnTouchListener((v,event) -> {
            if(event.getAction()==android.view.MotionEvent.ACTION_DOWN) { touchStart=event.getX(); return true; }
            if(event.getAction()==android.view.MotionEvent.ACTION_UP) {
                float delta=event.getX()-touchStart;
                if(Math.abs(delta)>dp(45) && recommendations.length()>1) {
                    movieIndex=VibeGameRules.wrap(movieIndex,delta<0?1:-1,recommendations.length());renderMovies();
                } else v.performClick();
                return true;
            }
            return true;
        });
        play.setOnClickListener(v -> a.startActivity(new Intent(a,VibeHubActivity.class).putExtra("mode","games")));
        play.setOnLongClickListener(v -> { a.startActivity(new Intent(a,VibeHubActivity.class).putExtra("mode","games")); return true; });
        movies.setOnLongClickListener(v -> { keys(); return true; });
        subs.setOnClickListener(v -> editSubscriptions());
        loadCache(); renderSubscriptions();
    }
    private int dp(int n) { return Math.round(n * a.getResources().getDisplayMetrics().density); }
    private TextView panel(String text,FrameLayout parent,int x,int y,int width,int height,boolean glass){
        TextView t=new TextView(a);t.setText(text);t.setTextSize(14);t.setTextColor(Color.WHITE);t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(dp(10),dp(8),dp(10),dp(8));t.setMaxLines(5);t.setEllipsize(android.text.TextUtils.TruncateAt.END);t.setFocusable(true);t.setClickable(true);
        if(glass)t.setBackground(new VibeGlass(false));
        t.setOnFocusChangeListener((v,f)->{if(glass)VibeGlass.focus(v,f);else {v.setBackground(f?new VibeGlass(true):null);v.setAlpha(f?1f:.9f);}});
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(width),dp(height));lp.leftMargin=dp(x);lp.topMargin=dp(y);parent.addView(t,lp);return t;
    }
    boolean focus() { return weather.requestFocus(); }
    void resume() { if (closed) return;
        long revision=p.getLong("tasteRevision",0);
        if(revision!=tasteRevision) {tasteRevision=revision;moviesAttempt=0;movieIndex=0;recommendations=new JSONArray();renderMovies();}
        visible = true; jarvisOrb.active(true); posters.active(true); posters.show(recommendations); ui.removeCallbacks(tick); tick.run(); }
    void pause() { visible = false; jarvisOrb.active(false); posters.active(false); ui.removeCallbacks(tick); }
    void close() { closed = true; pause(); posters.close(); net.shutdownNow(); ui.removeCallbacksAndMessages(null); }
    private String stamp(long millis) {
        return java.time.format.DateTimeFormatter.ofPattern("d MMM HH:mm").withZone(ZoneId.of("Asia/Kolkata")).format(Instant.ofEpochMilli(millis));
    }
    private void loadCache() {
        weatherDetail = p.getString("weatherDetail", weatherDetail);
        weather.setText(p.getString("weatherTile", "NERUL\nWeather loading…"));
        try { recommendations = new JSONArray(p.getString("movies", "[]")); } catch (JSONException ignored) { }
        renderMovies();
    }
    private void refresh() {
        renderSubscriptions();
        long now = System.currentTimeMillis();
        if (!weatherBusy && now - weatherAttempt >= 30L * 60000) {
            weatherBusy = true; weatherAttempt = now;
            net.execute(() -> {
                try {
                    JSONObject w = json("https://api.open-meteo.com/v1/forecast?latitude=19.033&longitude=73.020&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=Asia%2FKolkata").getJSONObject("current");
                    String temp = String.format(Locale.US, "%.0f°C", w.getDouble("temperature_2m"));
                    String detail = "Temperature: " + temp + "\nHumidity: " + w.getInt("relative_humidity_2m") + "%\nWind: " + w.getDouble("wind_speed_10m") + " km/h\nWeather: " + condition(w.getInt("weather_code"));
                    String aq = "AQI unavailable";
                    try {
                        JSONObject q = json("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=19.033&longitude=73.020&current=us_aqi,pm2_5&timezone=Asia%2FKolkata").getJSONObject("current");
                        if (!q.isNull("us_aqi")) {
                            int n = q.getInt("us_aqi"); aq = "US AQI " + n + " · " + VibeRules.aqi(n);
                        }
                        detail += "\n" + aq + "\nPM2.5: " + (q.isNull("pm2_5") ? "Unavailable" : q.getDouble("pm2_5") + " µg/m³") + "\nModel time: " + q.optString("time", "Unknown");
                    } catch (Exception ignored) { detail += "\nAQI unavailable; weather remains available."; }
                    String tile = "NERUL · " + temp + "\n" + condition(w.getInt("weather_code")) + "\n" + aq + "\nUpdated " + stamp(now);
                    String result = detail + "\nUpdated " + stamp(now);
                    ui.post(() -> { if (closed) return; weatherBusy = false; weatherDetail = result; weather.setText(tile); p.edit().putString("weatherTile", tile).putString("weatherDetail", result).apply(); });
                } catch (Exception e) {
                    ui.post(() -> { if (closed) return; weatherBusy = false; weather.setText("NERUL · Update unavailable\nCached data: select for details"); });
                }
            });
        }
        String key = p.getString("tmdb", "");
        if (!key.isEmpty() && !moviesBusy && now - moviesAttempt >= 6L * 60 * 60000) {
            moviesBusy = true; moviesAttempt = now;
            String omdb = p.getString("omdb", "");
            final int selectedGenre=p.getInt("genre",0), selectedLanguage=p.getInt("language",0);
            final String query=p.getString("query", "");
            final long requestRevision=tasteRevision;
            net.execute(() -> {
                try {
                    String request=VibeTaste.query(selectedGenre,selectedLanguage,query);
                    JSONArray candidates = json("https://api.themoviedb.org/3"+request+"&api_key=" + esc(key)).getJSONArray("results");
                    JSONArray result = new JSONArray();
                    for (int i = 0; i < Math.min(8, candidates.length()) && result.length() < 4; i++) {
                        JSONObject c = candidates.getJSONObject(i);
                        String type = c.optString("media_type", "movie");
                        if ((!type.equals("movie") && !type.equals("tv")) || c.optBoolean("adult")) continue;
                        JSONObject d = json("https://api.themoviedb.org/3/" + type + "/" + c.getInt("id") + "?api_key=" + esc(key) + "&append_to_response=watch%2Fproviders,external_ids");
                        JSONObject providers = d.optJSONObject("watch/providers");
                        JSONObject regions = providers == null ? null : providers.optJSONObject("results");
                        JSONObject india = regions == null ? null : regions.optJSONObject("IN");
                        JSONArray streaming = india == null ? null : india.optJSONArray("flatrate");
                        if (streaming == null || streaming.length() == 0) continue;
                        StringJoiner platforms = new StringJoiner(", ");
                        for (int j = 0; j < streaming.length(); j++) platforms.add(streaming.getJSONObject(j).optString("provider_name"));
                        StringJoiner genres = new StringJoiner(" / ");
                        JSONArray g = d.optJSONArray("genres");
                        if (g != null) for (int j = 0; j < g.length(); j++) genres.add(g.getJSONObject(j).optString("name"));
                        JSONObject ids = d.optJSONObject("external_ids");
                        String imdb = d.optString("imdb_id", ids == null ? "" : ids.optString("imdb_id"));
                        String rating = "Unavailable";
                        if (imdb.matches("tt[0-9]+") && !omdb.isEmpty()) {
                            try { String r = json("https://www.omdbapi.com/?apikey=" + esc(omdb) + "&i=" + esc(imdb)).optString("imdbRating", "");
                                if (r.matches("[0-9]+(\\.[0-9]+)?")) rating = r + "/10";
                            } catch (Exception ignored) { }
                        }
                        String date = d.optString(type.equals("tv") ? "first_air_date" : "release_date");
                        result.put(new JSONObject().put("title", d.optString(type.equals("tv") ? "name" : "title"))
                            .put("year", date.length() >= 4 ? date.substring(0,4) : "Year unavailable")
                            .put("genres", genres.toString()).put("rating", rating).put("platforms", platforms.toString())
                            .put("link", india.optString("link")).put("type", type)
                            .put("poster",d.optString("poster_path"))
                            .put("shelf",VibeTaste.shelf(selectedGenre>0?selectedGenre:VibeTaste.detectGenre(genres.toString()))));
                        // Bound the preview's network work. No background or continuous discovery.
                        if (i >= 7) break;
                    }
                    ui.post(() -> { if (closed) return; moviesBusy = false; if(requestRevision!=tasteRevision) { moviesAttempt=0; refresh(); return; } recommendations = result; movieIndex=0;
                        p.edit().putString("movies", result.toString()).putLong("moviesTime", now).apply(); renderMovies(); });
                } catch (Exception e) {
                    ui.post(() -> { if (closed) return; moviesBusy = false; renderMovies(); featured.setText("Update failed\nCached recommendations if shown\nCheck API key / network"); });
                }
            });
        }
    }
    private String condition(int code) {
        if (code == 0) return "Clear"; if (code <= 3) return "Cloudy"; if (code <= 48) return "Fog";
        if (code <= 67) return "Rain / drizzle"; if (code <= 77) return "Snow";
        if (code <= 82) return "Rain showers"; if (code <= 86) return "Snow showers"; return "Thunderstorms";
    }
    private void renderMovies() {
        JSONObject first = recommendations.optJSONObject(movieIndex);
        posters.show(recommendations);posters.select(movieIndex);
        movies.setText(first == null ? (p.getString("tmdb", "").isEmpty() ? "FOR YOU\nSelect to add API keys\nGeneral discovery · India" : "FOR YOU\nNo recommendations available\nSelect for options")
            : first.optString("shelf", "YOUR MIX") + " · " + (movieIndex+1) + "/" + recommendations.length() + "\n" + first.optString("title") + " (" + first.optString("year") + ")\nIMDb " + first.optString("rating"));
        featured.setText(first==null?"SWIPE\nLeft/Right to browse\nOK for details":first.optString("title")+"\n"+first.optString("year")+" · "+first.optString("genres")+"\nIMDb "+first.optString("rating")+"\n"+first.optString("platforms"));
    }
    private void showMovies() {
        if (p.getString("tmdb", "").isEmpty()) { keys(); return; }
        LinearLayout content = column();
        for (int i = 0; i < recommendations.length(); i++) {
            JSONObject item = recommendations.optJSONObject(i); if (item == null) continue;
            Button b = new Button(a); b.setAllCaps(false);
            b.setText(item.optString("title") + " (" + item.optString("year") + ") · " + item.optString("type")
                + "\n" + item.optString("genres") + "\nIMDb " + item.optString("rating") + "\nStream in India: " + item.optString("platforms"));
            b.setOnClickListener(v -> openAvailability(item)); content.addView(b);
        }
        TextView source = new TextView(a); source.setTextColor(Color.WHITE);
        source.setText("Popular discovery, not viewing-history personalisation.\nAvailability: TMDB / JustWatch, India; verify with platform.\nIMDb ratings supplied by OMDb.\nUpdated: " + (p.getLong("moviesTime",0) == 0 ? "Not yet" : stamp(p.getLong("moviesTime",0)))
            + "\nThis product uses the TMDB API but is not endorsed or certified by TMDB."); content.addView(source);
        ScrollView scroll = new ScrollView(a); scroll.addView(content);
        new AlertDialog.Builder(a).setTitle("For You").setView(scroll).setPositiveButton("Close", null)
            .setNeutralButton("API keys", (d,w) -> keys()).setNegativeButton("Refresh", (d,w) -> { moviesAttempt = 0; refresh(); }).show();
    }
    private void openAvailability(JSONObject item) {
        String url = item.optString("link");
        if (!url.startsWith("https://www.themoviedb.org/")) return;
        try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (android.content.ActivityNotFoundException e) { Toast.makeText(a, "No browser installed. Open the listed streaming app to search for this title.", Toast.LENGTH_LONG).show(); }
    }
    private LinearLayout column() { LinearLayout v = new LinearLayout(a); v.setOrientation(LinearLayout.VERTICAL); v.setPadding(dp(16), dp(10), dp(16), dp(10)); return v; }
    private EditText entry(LinearLayout parent, String hint, String value, boolean secret) {
        EditText e = new EditText(a); e.setSingleLine(true); e.setHint(hint); e.setText(value); e.setTextColor(Color.WHITE); e.setHintTextColor(0xFFAAAAAA);
        e.setInputType(InputType.TYPE_CLASS_TEXT | (secret ? InputType.TYPE_TEXT_VARIATION_PASSWORD : 0)); parent.addView(e); return e;
    }
    private void keys() {
        LinearLayout c = column();
        TextView info = new TextView(a); info.setTextColor(Color.WHITE);
        info.setText("Optional personal API keys; not your streaming passwords.\nTMDB v3 API key: themoviedb.org/settings/api\nOMDb key: omdbapi.com/apikey.aspx\nStored only in this app's private settings; sent only to the respective HTTPS API. Clear fields and Save to remove keys and cached recommendations."); c.addView(info);
        EditText tm = entry(c, "TMDB v3 API key", p.getString("tmdb", ""), true);
        EditText om = entry(c, "OMDb API key (optional)", p.getString("omdb", ""), true);
        AlertDialog dialog = new AlertDialog.Builder(a).setTitle("Movie data setup").setView(c).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(-1).setOnClickListener(button -> {
            String t = tm.getText().toString().trim(), o = om.getText().toString().trim();
            if ((!t.isEmpty() && !t.matches("[a-fA-F0-9]{32}")) || (!o.isEmpty() && !o.matches("[a-zA-Z0-9]+"))) {
                tm.setError("Use a TMDB v3 API key (32 hex characters), not a bearer token"); return;
            }
            if (moviesBusy) { Toast.makeText(a, "Please wait for the current refresh, then save.", Toast.LENGTH_SHORT).show(); return; }
            p.edit().putString("tmdb",t).putString("omdb",o).remove("movies").remove("moviesTime").apply();
            recommendations = new JSONArray(); moviesAttempt = 0; renderMovies(); dialog.dismiss(); refresh();
        })); dialog.show();
    }
    private static final String[] SERVICES = {"Netflix", "Prime Video", "JioHotstar", "SonyLIV", "ZEE5", "YouTube Premium"};
    private void renderSubscriptions() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        String best = "Select to add dates"; long soonest = Long.MAX_VALUE;
        for (String name : SERVICES) {
            try {
                String date = p.getString("date-" + name, ""); if (date.isEmpty()) continue;
                long days = ChronoUnit.DAYS.between(today, VibeRules.date(date));
                if (days < soonest) { soonest = days; best = name + " · " + date + "\n" + VibeRules.due(days, p.getBoolean("auto-" + name, false)); }
            } catch (Exception ignored) { }
        }
        subs.setText("SUBSCRIPTIONS\n" + best + "\nManual dates · select to edit");
    }
    private void editSubscriptions() {
        String[] rows = new String[SERVICES.length];
        for (int i=0;i<rows.length;i++) rows[i] = SERVICES[i] + " · " + p.getString("date-" + SERVICES[i], "Not set");
        new AlertDialog.Builder(a).setTitle("Subscription dates · manual")
            .setItems(rows, (dialog, index) -> editDate(SERVICES[index])).setPositiveButton("Close",null).show();
    }
    private void editDate(String name) {
        LinearLayout c = column(); EditText date = entry(c, "YYYY-MM-DD (blank removes)", p.getString("date-" + name,""), false);
        CheckBox auto = new CheckBox(a); auto.setText("Auto-renews on this date"); auto.setTextColor(Color.WHITE); auto.setChecked(p.getBoolean("auto-" + name,false)); c.addView(auto);
        AlertDialog d = new AlertDialog.Builder(a).setTitle(name).setView(c).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        d.setOnShowListener(v -> d.getButton(-1).setOnClickListener(button -> {
            String value = date.getText().toString().trim();
            try { if (!value.isEmpty()) VibeRules.date(value); }
            catch (Exception e) { date.setError("Enter a valid date: YYYY-MM-DD"); return; }
            p.edit().putString("date-" + name,value).putBoolean("auto-" + name,auto.isChecked()).apply(); renderSubscriptions(); d.dismiss();
        })); d.show();
    }
    private static String esc(String s) { return Uri.encode(s); }
    private JSONObject json(String url) throws IOException, JSONException {
        if (!visible || closed || Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
        HttpsURLConnectionGuard guard = new HttpsURLConnectionGuard(url);
        try { return new JSONObject(guard.read()); } finally { guard.close(); }
    }
    private static final class HttpsURLConnectionGuard {
        final javax.net.ssl.HttpsURLConnection conn;
        HttpsURLConnectionGuard(String url) throws IOException {
            conn = (javax.net.ssl.HttpsURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(7000); conn.setReadTimeout(7000); conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Accept", "application/json");
        }
        String read() throws IOException {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
            if (conn.getResponseCode() != 200) throw new IOException("API request unavailable");
            try (InputStream in = conn.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096]; int n;
                while ((n = in.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted() || bytes.size() + n > 1048576) throw new IOException("Response limit");
                    bytes.write(buffer,0,n);
                }
                return bytes.toString(StandardCharsets.UTF_8.name());
            }
        }
        void close() { conn.disconnect(); }
    }
}
