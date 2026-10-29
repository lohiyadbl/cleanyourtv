package com.gathin.tvhome;
import java.util.Locale;
final class VibeTaste {
    static final String[] GENRES = {"Any genre", "Thriller", "Crime", "Sci-fi", "Comedy", "Action", "Family", "Drama", "Animation", "Documentary", "Sports"};
    static final String[] SHELVES = {"Made for Your Evening", "One More Twist", "Under the Surface", "Beyond the Ordinary", "Easy Evenings", "Big-Screen Adrenaline", "Watch Together", "Stories That Stay", "Drawn Into Another World", "Real Stories", "Match Night"};
    static final String[] LANGUAGES = {"Any language", "Hindi", "English", "Marathi", "Tamil", "Telugu", "Malayalam", "Kannada", "Bengali"};
    static final String[] CODES = {"", "hi", "en", "mr", "ta", "te", "ml", "kn", "bn"};
    static String genreId(int genre, boolean tv) {
        int[] movieIds={0,53,80,878,35,28,10751,18,16,99,0};
        int[] tvIds={0,0,80,10765,35,10759,10751,18,16,99,0};
        return genre > 0 && genre < movieIds.length ? String.valueOf((tv ? tvIds : movieIds)[genre]) : "0";
    }
    static int detectGenre(String input) {
        String s=input.toLowerCase(Locale.ROOT);
        String[][] words={{"thrill","twist"},{"crime","detective"},{"sci-fi","science fiction","space"},{"comedy","funny","laugh"},{"action"},{"family","kids"},{"drama"},{"animation","cartoon"},{"documentary"},{"sport","cricket"}};
        for(int i=0;i<words.length;i++) for(String word:words[i]) if(s.contains(word)) return i+1;
        return 0;
    }
    static boolean genreRequest(String query) {
        String s=query.toLowerCase(Locale.ROOT).trim();
        if (detectGenre(s)==0) return false;
        if(s.startsWith("show ") || s.startsWith("suggest ") || s.startsWith("recommend ") || s.startsWith("find ")) return true;
        return s.matches("(?:(?:hindi|english|marathi|tamil|telugu|malayalam|kannada|bengali) )?(?:thrillers?|crime|sci-fi|science fiction|comedy|action|family|drama|animation|documentary|sports?)(?: (?:movies|shows))?");
    }
    static int detectLanguage(String input) {
        String s=input.toLowerCase(Locale.ROOT);
        for(int i=1;i<LANGUAGES.length;i++) if(s.contains(LANGUAGES[i].toLowerCase(Locale.ROOT))) return i;
        return 0;
    }
    static String shelf(int genre) { return SHELVES[Math.max(0,Math.min(genre,SHELVES.length-1))]; }
    static String query(int genre, int language, String search) {
        if (!search.isEmpty()) return "/search/multi?include_adult=false&query=" + encode(search);
        // Movie discover IDs differ from TV IDs. Sports is searched explicitly, never a fabricated genre ID.
        if(genre==10) return "/search/multi?include_adult=false&query=cricket";
        if(genre==0 && language==0) return "/trending/all/week?";
        String id=genreId(genre,false);
        String code=CODES[Math.max(0,Math.min(language,CODES.length-1))];
        return "/discover/movie?include_adult=false&region=IN&watch_region=IN&with_watch_monetization_types=flatrate&sort_by=popularity.desc"
            + (id.equals("0") ? "" : "&with_genres="+id) + (code.isEmpty()?"":"&with_original_language="+code);
    }
    private static String encode(String value) {
        try { return java.net.URLEncoder.encode(value,"UTF-8"); } catch(java.io.UnsupportedEncodingException e) { throw new AssertionError(e); }
    }
}
