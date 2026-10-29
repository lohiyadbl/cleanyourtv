package com.gathin.tvhome;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.widget.Toast;

/** Jarvis is launcher artwork and an entry point to Google's installed Assistant. */
final class VibeAssistant {
    static boolean open(Activity activity) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_ASSIST).setPackage("com.google.android.katniss"));
            return true;
        } catch (ActivityNotFoundException | SecurityException first) {
            // Component resolved by the user's Android 10 TV inventory; no search text or voice capture.
            try {
                activity.startActivity(new Intent(Intent.ACTION_ASSIST).setComponent(new ComponentName(
                    "com.google.android.katniss", "com.google.android.apps.tvsearch.app.launch.trampoline.SearchActivityTrampoline")));
                return true;
            } catch (ActivityNotFoundException | SecurityException unavailable) {
                Toast.makeText(activity,"Google Assistant could not open. Check it in TV Settings and try the remote mic button.",Toast.LENGTH_LONG).show();
                return false;
            }
        }
    }
}
