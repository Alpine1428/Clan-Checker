package com.clanchecker;

import java.util.ArrayList;
import java.util.List;

public class ViolationDatabase {

    public static class ViolationResult {
        public final String clanName;
        public final String category;
        public final String matchedWord;
        public final int slot;

        public ViolationResult(String clanName, String category, String matchedWord, int slot) {
            this.clanName = clanName;
            this.category = category;
            this.matchedWord = matchedWord;
            this.slot = slot;
        }
    }

    private static final String[] CHEATS = {
        "wurst", "meteor", "aristois", "impact", "inertia", "lambda",
        "liquidbounce", "sigma", "novoline", "exhibition", "rise",
        "tenacity", "enthium", "azura", "pandaware",
        "astolfo", "zeroday", "hanabi", "sixsense",
        "wolfram", "kami", "salhack", "future", "rusherhack",
        "konas", "phobos", "gamesense", "pyro",
        "killaura", "aimbot", "autoclicker", "autoclick",
        "xray", "nuker", "scaffold",
        "flyhack", "noclip", "antiknockback",
        "antikb", "velocity", "bunnyhop", "bhop",
        "triggerbot", "autototem", "autocrystal", "automine",
        "fastplace", "fastbreak", "freecam",
        "wallhack", "tracers", "chams", "baritone",
        "huzuni", "weepcraft", "vape", "cheatbreaker",
        "hack", "hacks", "cheat", "cheats", "exploit", "dupe",
        "inject"
    };

    private static final String[] PROFANITY = {
        "fuck", "fck", "phuck",
        "shit", "sh1t",
        "bitch", "b1tch",
        "dick", "d1ck",
        "pussy", "penis", "vagina",
        "cock", "cunt", "whore", "slut",
        "bastard", "nigger", "nigga", "n1gga", "n1gger",
        "faggot", "fag",
        "retard", "retarded",
        "cyka", "blyat", "blyad", "pidar", "pidor", "nahui", "nahuy",
        "suka", "ebal", "ebat", "huy", "hui"
    };

    private static final String[] INSULTS = {
        "noob", "trash", "loser", "idiot", "moron", "stupid",
        "dumb", "lame", "pathetic"
    };

    private static final String[] POLITICS = {
        "putin", "zelensky", "zelenskiy",
        "biden", "trump",
        "navalny", "lukashenko",
        "stalin", "lenin",
        "hitler", "mussolini",
        "nazi", "fascis", "swastika",
        "terrorist", "genocide", "holocaust"
    };

    private static final String[] NSFW = {
        "porn", "porno", "pr0n",
        "hentai", "sex", "s3x",
        "erotic", "onlyfans",
        "pornhub", "xvideos", "xhamster", "brazzers",
        "rule34", "r34",
        "nsfw", "xxx",
        "blowjob", "anal", "orgy",
        "fetish", "bdsm",
        "furry", "yaoi", "yuri"
    };

    public static List<ViolationResult> checkClanName(String clanName, int slot) {
        List<ViolationResult> results = new ArrayList<>();
        if (clanName == null || clanName.isEmpty()) return results;

        String normalized = clanName.toLowerCase()
                .replace("_", "").replace("-", "")
                .replace(".", "").replace(" ", "");
        String lower = clanName.toLowerCase();

        check(clanName, normalized, lower, CHEATS, "Cheats", slot, results);
        check(clanName, normalized, lower, PROFANITY, "Profanity", slot, results);
        check(clanName, normalized, lower, INSULTS, "Insults", slot, results);
        check(clanName, normalized, lower, POLITICS, "Politics", slot, results);
        check(clanName, normalized, lower, NSFW, "NSFW", slot, results);

        return results;
    }

    private static void check(String original, String normalized, String lower,
                               String[] keywords, String category, int slot,
                               List<ViolationResult> results) {
        for (String kw : keywords) {
            String lk = kw.toLowerCase();
            if (normalized.contains(lk) || lower.contains(lk)) {
                results.add(new ViolationResult(original, category, kw, slot));
                return;
            }
        }
    }
}
