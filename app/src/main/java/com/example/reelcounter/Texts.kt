package com.example.reelcounter

object Texts {
    const val ABOUT = "Reel Counter counts how many Instagram Reels you swipe through each day and shows the number " +
        "in a small pill at the top of your screen and on a home-screen widget.\n\n" +
        "It DOES: notice when you swipe to a new Reel, keep one number per day on this phone, and nudge you once when you pass your limit.\n\n" +
        "It does NOT: read your messages, comments, usernames or any text, take screenshots, or connect to the internet " +
        "(the app doesn't even have the internet permission).\n\n" +
        "To count, it needs the Accessibility permission. Android will show a scary warning for every accessibility app; " +
        "that is normal."
    const val PRIVACY = "Privacy, honestly:\n\n" +
        "• No internet permission: this app physically cannot send data anywhere.\n" +
        "• It stores only a date and a number per day, on this phone. Backups are disabled.\n" +
        "• The accessibility service only looks at event types and the technical IDs of on-screen elements " +
        "(to tell the Reels screen from the feed). It never reads or saves text, names, messages or comments.\n" +
        "• Android sends the service window-change events from all apps so it can hide the pill when you leave Instagram. " +
        "For any app other than Instagram (or YouTube if enabled), only the app's package name is looked at, then ignored.\n" +
        "• Export/Import writes a small JSON file only where you choose."
    const val BATTERY = "Keep counting alive:\n\n" +
        "1. Open Android Settings > Apps > Reel Counter > Battery and choose 'Unrestricted' (or 'Don't optimise').\n" +
        "2. On Xiaomi/Oppo/Vivo/Samsung etc., also allow 'Autostart' / 'Run in background' and lock the app in Recents.\n" +
        "3. If counting stops, open Accessibility settings and check Reel Counter is still ON.\n\n" +
        "The app uses no wake locks and does almost nothing between swipes."
}
