package com.gathin.tvhome;
final class VibeGameRules {
    static int cricketRuns(float phase) {
        float delta=Math.abs(phase-.72f);
        if(delta<=.045f) return 6;
        if(delta<=.11f) return 4;
        if(delta<=.20f) return 2;
        if(delta<=.29f) return 1;
        return 0;
    }
    static boolean inningsOver(int balls,int wickets) { return balls>=12 || wickets>=3; }
    static int wrap(int current,int direction,int count) { return count<=0?0:Math.floorMod(current+direction,count); }
}
