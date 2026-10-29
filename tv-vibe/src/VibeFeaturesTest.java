package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class VibeFeaturesTest {
    @Test public void titleContainingGenreIsNotSilentlyReclassified(){assertFalse(VibeTaste.genreRequest("Life of Crime"));}
    @Test public void directGenrePhraseAccepted(){assertTrue(VibeTaste.genreRequest("Hindi thrillers"));assertTrue(VibeTaste.genreRequest("show English comedy"));}
    @Test public void broadMixIsDefault(){assertEquals("/trending/all/week?",VibeTaste.query(0,0,""));}
    @Test public void genreAndOriginalLanguageActuallyAffectRequest(){String s=VibeTaste.query(1,1,"");assertTrue(s.contains("with_genres=53"));assertTrue(s.contains("with_original_language=hi"));assertTrue(s.contains("watch_region=IN"));}
    @Test public void sportsDoesNotInventTmdbGenre(){assertTrue(VibeTaste.query(10,0,"").contains("query=cricket"));}
    @Test public void searchIsEncoded(){assertTrue(VibeTaste.query(0,0,"A&B").contains("A%26B"));}
    @Test public void voiceDetectsGenreAndLanguage(){assertEquals(1,VibeTaste.detectGenre("show Hindi thrillers"));assertEquals(1,VibeTaste.detectLanguage("show Hindi thrillers"));}
    @Test public void tvAndMovieGenreIdsDiffer(){assertEquals("28",VibeTaste.genreId(5,false));assertEquals("10759",VibeTaste.genreId(5,true));}
    @Test public void shelvesHaveStableNames(){assertEquals("One More Twist",VibeTaste.shelf(1));assertEquals("Easy Evenings",VibeTaste.shelf(4));}
    @Test public void carouselWrapsBothWays(){assertEquals(3,VibeGameRules.wrap(0,-1,4));assertEquals(0,VibeGameRules.wrap(3,1,4));assertEquals(0,VibeGameRules.wrap(0,1,0));}
    @Test public void perfectCricketTimingIsSix(){assertEquals(6,VibeGameRules.cricketRuns(.72f));}
    @Test public void missedShotIsWicket(){assertEquals(0,VibeGameRules.cricketRuns(.1f));}
    @Test public void cricketBoundaryScoring(){assertEquals(4,VibeGameRules.cricketRuns(.8f));assertEquals(2,VibeGameRules.cricketRuns(.9f));assertEquals(1,VibeGameRules.cricketRuns(.99f));}
    @Test public void inningsEndsAtBallsOrWickets(){assertFalse(VibeGameRules.inningsOver(11,2));assertTrue(VibeGameRules.inningsOver(12,2));assertTrue(VibeGameRules.inningsOver(2,3));}
}
