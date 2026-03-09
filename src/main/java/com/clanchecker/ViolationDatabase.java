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

    private static final String[] CHEATS_KEYWORDS = {
            "wurst", "meteor", "aristois", "impact", "inertia", "lambda",
            "liquidbounce", "sigma", "novoline", "exhibition", "rise",
            "tenacity", "enthium", "drip", "azura", "pandaware",
            "astolfo", "zeroday", "antic", "hanabi", "sixsense",
            "wolfram", "kami", "salhack", "future", "rusherhack",
            "konas", "phobos", "gamesense", "pyro",
            "killaura", "aimbot", "autoclicker", "autoclick",
            "xray", "x-ray", "nuker", "scaffold",
            "flyhack", "noclip", "antiknockback",
            "antikb", "velocity", "bunnyhop", "bhop",
            "triggerbot", "autototem", "autocrystal", "automine",
            "fastplace", "fastbreak", "freecam",
            "wallhack", "tracers", "chams", "baritone",
            "huzuni", "weepcraft", "vape", "cheatbreaker",
            "макрос", "автокликер", "автоклик",
            "чит", "читы", "хак", "хаки", "hack", "hacks",
            "cheat", "cheats", "exploit", "эксплойт", "дюп", "dupe",
            "inject", "инжект"
    };

    private static final String[] PROFANITY_KEYWORDS = {
            "хуй", "хуя", "хуе", "хуи", "хуё",
            "пизд", "пизда", "пиздец", "пиздос",
            "блять", "блядь", "бляд", "блят",
            "ебат", "ебан", "ебал", "ёбан", "ебаш", "ебуч",
            "сука", "сучк", "сучар",
            "мудак", "мудил", "мудо",
            "залупа", "залуп",
            "долбоёб", "долбоеб",
            "шлюх", "шалав",
            "пидор", "пидар", "пидр", "педик",
            "гандон", "гондон",
            "дрочи", "дроч",
            "жопа", "жоп",
            "срать", "срал", "сран",
            "говно", "говня", "говен",
            "дерьм",
            "cyka", "blyat", "blyad", "pidar", "pidor", "nahui", "nahuy",
            "suka", "ebal", "ebat", "huy", "hui",
            "fuck", "fck", "phuck",
            "shit", "sh1t",
            "bitch", "b1tch",
            "damn", "dick", "d1ck",
            "pussy", "penis", "vagina",
            "cock", "cunt", "whore", "slut",
            "bastard", "nigger", "nigga", "n1gga", "n1gger",
            "faggot", "fag",
            "retard", "retarded"
    };

    private static final String[] INSULTS_KEYWORDS = {
            "дебил", "идиот", "кретин", "имбецил", "тупица",
            "лох", "лошар", "нуб", "ноуб", "noob", "нубяр",
            "чмо", "чмошн", "уёбок", "уебок", "уёбищ",
            "ублюдок", "выродок", "отброс", "мразь", "мраз",
            "тварь", "подонок", "урод", "уродин",
            "быдло", "быдл",
            "даун", "аутист",
            "trash", "loser", "idiot", "moron",
            "дурак", "дура", "дурень",
            "козёл", "козел", "козлин",
            "скотина", "скот",
            "гнида", "падла", "паскуд",
            "чурка", "хач", "чурбан"
    };

    private static final String[] POLITICS_KEYWORDS = {
            "путин", "putin", "зеленский", "zelensky", "zelenskiy",
            "байден", "biden", "трамп", "trump",
            "навальный", "navalny", "навальн",
            "лукашенко", "lukashenko",
            "сталин", "stalin", "ленин", "lenin",
            "гитлер", "hitler", "муссолини", "mussolini",
            "украина", "ukraine", "ukrain",
            "россия", "russia", "русня",
            "нато", "nato",
            "крым", "crimea", "донбасс", "donbass",
            "днр", "лнр",
            "мариуполь", "mariupol", "буча", "bucha",
            "фашизм", "фашист", "fascis", "нацизм", "нацист", "nazi",
            "свастика", "swastika",
            "террорис", "terrorist",
            "геноцид", "genocide",
            "холокост", "holocaust"
    };

    private static final String[] NSFW_KEYWORDS = {
            "порно", "porn", "porno", "pr0n",
            "хентай", "hentai", "хентаи",
            "секс", "sex", "s3x",
            "эротик", "erotic", "эротика",
            "onlyfans", "онлифанс",
            "pornhub", "xvideos", "xhamster", "brazzers",
            "rule34", "r34",
            "nsfw", "xxx",
            "стриптиз", "striptease",
            "минет", "blowjob",
            "анал", "anal",
            "оргия", "orgy",
            "фетиш", "fetish",
            "бдсм", "bdsm",
            "furry", "фурри",
            "яой", "yaoi",
            "юри", "yuri"
    };

    public static List<ViolationResult> checkClanName(String clanName, int slot) {
        List<ViolationResult> results = new ArrayList<>();
        if (clanName == null || clanName.isEmpty()) return results;

        String lowerName = clanName.toLowerCase()
                .replace("_", "")
                .replace("-", "")
                .replace(".", "")
                .replace(" ", "");

        String lowerOriginal = clanName.toLowerCase();

        checkCategory(clanName, lowerName, lowerOriginal, CHEATS_KEYWORDS, "Читы/Макросы/ПО", slot, results);
        checkCategory(clanName, lowerName, lowerOriginal, PROFANITY_KEYWORDS, "Нецензурная лексика", slot, results);
        checkCategory(clanName, lowerName, lowerOriginal, INSULTS_KEYWORDS, "Оскорбления", slot, results);
        checkCategory(clanName, lowerName, lowerOriginal, POLITICS_KEYWORDS, "Политика", slot, results);
        checkCategory(clanName, lowerName, lowerOriginal, NSFW_KEYWORDS, "18+ контент", slot, results);

        return results;
    }

    private static void checkCategory(String original, String normalized, String lowerOriginal,
                                       String[] keywords, String category, int slot,
                                       List<ViolationResult> results) {
        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase();
            if (normalized.contains(lowerKeyword) || lowerOriginal.contains(lowerKeyword)) {
                results.add(new ViolationResult(original, category, keyword, slot));
                return;
            }
        }
    }
}
