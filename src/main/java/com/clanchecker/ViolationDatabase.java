package com.clanchecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // =====================================================================
    // NORMALIZATION MAP: all tricky characters -> normal latin letters
    // =====================================================================
    private static final Map<Character, Character> CHAR_MAP = new HashMap<>();

    static {
        // Cyrillic -> Latin
        mapChars("\u0430a"); // a
        mapChars("\u0431b"); // б
        mapChars("\u0432v"); // в
        mapChars("\u0433g"); // г
        mapChars("\u0434d"); // д
        mapChars("\u0435e"); // е
        mapChars("\u0451e"); // ё -> e
        mapChars("\u0436zh"); // ж (special, handled separately)
        mapChars("\u0437z"); // з
        mapChars("\u0438i"); // и
        mapChars("\u0439i"); // й -> i
        mapChars("\u043ai"); // к -> k (handled below)
        mapChars("\u043bl"); // л
        mapChars("\u043cm"); // м
        mapChars("\u043dn"); // н
        mapChars("\u043eo"); // о
        mapChars("\u043fp"); // п -> p
        mapChars("\u0440r"); // р
        mapChars("\u0441s"); // с -> s (looks like c)
        mapChars("\u0442t"); // т
        mapChars("\u0443u"); // у
        mapChars("\u0444f"); // ф
        mapChars("\u0445h"); // х -> h
        mapChars("\u0446c"); // ц -> c
        mapChars("\u0447ch"); // ч (special)
        mapChars("\u0448sh"); // ш (special)
        mapChars("\u0449sh"); // щ (special)
        // ъ, ь -> skip
        mapChars("\u044by"); // ы -> y
        // ь -> skip
        mapChars("\u044de"); // э -> e
        mapChars("\u044eu"); // ю -> u
        mapChars("\u044fa"); // я -> a

        // Leet speak: numbers -> letters
        CHAR_MAP.put('0', 'o');
        CHAR_MAP.put('1', 'i');
        CHAR_MAP.put('2', 'z');
        CHAR_MAP.put('3', 'e');
        CHAR_MAP.put('4', 'a');
        CHAR_MAP.put('5', 's');
        CHAR_MAP.put('6', 'g');
        CHAR_MAP.put('7', 't');
        CHAR_MAP.put('8', 'b');
        CHAR_MAP.put('9', 'g');

        // Special unicode lookalikes
        CHAR_MAP.put('\u00e0', 'a'); // à
        CHAR_MAP.put('\u00e1', 'a'); // á
        CHAR_MAP.put('\u00e2', 'a'); // â
        CHAR_MAP.put('\u00e3', 'a'); // ã
        CHAR_MAP.put('\u00e4', 'a'); // ä
        CHAR_MAP.put('\u00e5', 'a'); // å
        CHAR_MAP.put('\u00e8', 'e'); // è
        CHAR_MAP.put('\u00e9', 'e'); // é
        CHAR_MAP.put('\u00ea', 'e'); // ê
        CHAR_MAP.put('\u00eb', 'e'); // ë
        CHAR_MAP.put('\u00ec', 'i'); // ì
        CHAR_MAP.put('\u00ed', 'i'); // í
        CHAR_MAP.put('\u00ee', 'i'); // î
        CHAR_MAP.put('\u00ef', 'i'); // ï
        CHAR_MAP.put('\u00f2', 'o'); // ò
        CHAR_MAP.put('\u00f3', 'o'); // ó
        CHAR_MAP.put('\u00f4', 'o'); // ô
        CHAR_MAP.put('\u00f5', 'o'); // õ
        CHAR_MAP.put('\u00f6', 'o'); // ö
        CHAR_MAP.put('\u00f9', 'u'); // ù
        CHAR_MAP.put('\u00fa', 'u'); // ú
        CHAR_MAP.put('\u00fb', 'u'); // û
        CHAR_MAP.put('\u00fc', 'u'); // ü
        CHAR_MAP.put('\u00fd', 'y'); // ý
        CHAR_MAP.put('\u00ff', 'y'); // ÿ
        CHAR_MAP.put('\u00f1', 'n'); // ñ
        CHAR_MAP.put('\u00e7', 'c'); // ç
        CHAR_MAP.put('\u00df', 's'); // ß

        // Cyrillic lookalikes used to bypass filters
        CHAR_MAP.put('\u0410', 'a'); // А
        CHAR_MAP.put('\u0412', 'b'); // В -> b
        CHAR_MAP.put('\u0415', 'e'); // Е
        CHAR_MAP.put('\u041a', 'k'); // К
        CHAR_MAP.put('\u041c', 'm'); // М
        CHAR_MAP.put('\u041d', 'h'); // Н -> h
        CHAR_MAP.put('\u041e', 'o'); // О
        CHAR_MAP.put('\u0420', 'p'); // Р -> p
        CHAR_MAP.put('\u0421', 'c'); // С -> c
        CHAR_MAP.put('\u0422', 't'); // Т
        CHAR_MAP.put('\u0423', 'y'); // У -> y
        CHAR_MAP.put('\u0425', 'x'); // Х -> x
        CHAR_MAP.put('\u042a', ' '); // Ъ
        CHAR_MAP.put('\u042c', ' '); // Ь

        // Common symbol substitutions
        CHAR_MAP.put('@', 'a');
        CHAR_MAP.put('$', 's');
        CHAR_MAP.put('!', 'i');
        CHAR_MAP.put('|', 'l');
        CHAR_MAP.put('+', 't');
        CHAR_MAP.put('(', 'c');
        CHAR_MAP.put('{', 'c');
        CHAR_MAP.put('[', 'c');
    }

    private static void mapChars(String mapping) {
        if (mapping.length() >= 2) {
            CHAR_MAP.put(mapping.charAt(0), mapping.charAt(1));
        }
    }

    /**
     * Normalize string: remove junk, map all tricky chars to latin.
     */
    public static String normalize(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : input.toLowerCase().toCharArray()) {
            // Skip separators and decorations
            if (c == '_' || c == '-' || c == '.' || c == ' ' || c == '*'
                || c == '#' || c == '~' || c == '`' || c == '\''
                || c == '"' || c == ',' || c == ';' || c == ':'
                || c == '/' || c == '\\' || c == '<' || c == '>'
                || c == '^' || c == '=' || c == '%' || c == '&') {
                continue;
            }
            Character mapped = CHAR_MAP.get(c);
            if (mapped != null) {
                if (mapped != ' ') {
                    sb.append(mapped);
                }
            } else if (c >= 'a' && c <= 'z') {
                sb.append(c);
            }
            // else skip unknown chars
        }
        return sb.toString();
    }

    // =====================================================================
    // KEYWORD DATABASES
    // All keywords must be in normalized latin form (lowercase, no spaces)
    // =====================================================================

    // --- CHEATS, HACKS, MACROS, SOFTWARE ---
    private static final String[] CHEATS = {
        // Cheat clients
        "wurst", "meteor", "aristois", "impact", "inertia", "lambda",
        "liquidbounce", "sigma", "novoline", "exhibition", "rise",
        "tenacity", "enthium", "azura", "pandaware", "astolfo",
        "zeroday", "hanabi", "sixsense", "wolfram", "kami",
        "salhack", "future", "rusherhack", "konas", "phobos",
        "gamesense", "pyro", "huzuni", "weepcraft", "vape",
        "cheatbreaker", "badlion", "lunar", "feather",
        "fdpclient", "fdp", "novahack", "skilled",
        "medusa", "flavor", "flavor", "drip",
        "antic", "atani", "autumn", "blaze",
        "crypt", "death", "dream", "eclipse",
        "flux", "hydra", "lime", "moon",
        "neptune", "phantom", "prestige", "remix",
        "raven", "ravenclient", "ravenb", "ravenb+",
        "skid", "sleek", "snow", "tap",
        "vapor", "xatz", "zephyr",
        // Cheat functions
        "killaura", "killaure", "kilaura", "kilaure",
        "aimbot", "aim bot", "aimassist",
        "autoclicker", "autoclick", "autoklicker", "autoklick",
        "xray", "x ray", "xrei", "xray mod",
        "nuker", "scaffold", "scaff",
        "flyhack", "fly hack", "flyh4ck",
        "noclip", "no clip",
        "antiknockback", "antikb", "anti kb", "nokb",
        "velocity", "bhop", "bunnyhop", "bunny hop",
        "triggerbot", "trigger bot",
        "autototem", "auto totem",
        "autocrystal", "auto crystal",
        "automine", "auto mine",
        "fastplace", "fast place",
        "fastbreak", "fast break",
        "freecam", "free cam",
        "wallhack", "wall hack", "wh",
        "tracers", "tracer",
        "chams", "esp",
        "baritone", "bariton",
        "speedhack", "speed hack",
        "highjump", "high jump",
        "noslowdown", "no slowdown",
        "nofall", "no fall", "nofalldamage",
        "jesus", "waterwalk", "water walk",
        "phase", "phaze",
        "timer", "timerh4ck",
        "reach", "longreach", "long reach",
        "hitbox", "hitboxes",
        "criticals", "crits", "autocrit",
        "autosoup", "autopotion", "autopot",
        "autoarmor", "auto armor",
        "cheststealer", "chest stealer",
        "inventorywalk", "inv walk",
        "step", "stepper",
        "strafe", "sprint",
        "safewalk", "safe walk",
        "antibot", "anti bot",
        "backtrack", "back track",
        // General terms
        "hack", "hacks", "h4ck", "h4cks", "hak", "haks",
        "cheat", "cheats", "ch3at", "ch34t", "chiter",
        "chiter", "cheater", "hacker", "haker",
        "exploit", "eksploit", "expl0it",
        "dupe", "duper", "duping", "dup",
        "inject", "injector", "inzhekt",
        "macro", "makro", "makros", "macros",
        "autokliker", "avtokliker",
        "chity", "chiti",
        "haki", "khaki",
        "vzlom", "vzl0m",
        "obd", "obhod",
        "soft", "s0ft",
        // RU transliterated
        "aimbot", "killaura",
        "avtoklik", "avtoclicker",
        "makros", "makro",
        "eksploit", "dyp", "dyup",
        "inzhekt", "inzekt"
    };

    // --- PROFANITY (RU transliterated + EN) ---
    private static final String[] PROFANITY = {
        // Russian mat (transliterated to latin)
        "hui", "huy", "huj", "huй",
        "huya", "huyu", "huei", "hue",
        "huilo", "huylo", "huesos",
        "pizd", "pizda", "pizdec", "pizdos", "pizdez",
        "pizdato", "pizduk", "pizdet",
        "blyat", "bliat", "blad", "blyad", "blya",
        "blyad", "bljat", "blyt", "blet",
        "ebat", "eban", "ebal", "ebash", "ebuch",
        "ebanat", "ebanko", "ebanyi",
        "yobany", "yoban", "yob",
        "suka", "suchka", "suchar", "suchara",
        "syki", "s00ka", "suca",
        "mudak", "mudila", "mudo", "mudak",
        "mudozv", "mudila",
        "zalupa", "zalup", "zalepa",
        "dolboeb", "dolboyob", "dolboed",
        "shluh", "shlukha", "shluha",
        "shalav", "shalava",
        "pidor", "pidar", "pidr", "pederast",
        "pedik", "peder", "pid0r", "pid4r", "pid",
        "gandon", "gondon", "gand0n", "g0nd0n", "prezervativ",
        "droch", "drochi", "drochit", "drochila",
        "zhopa", "zhop", "zhopu", "zhope",
        "srat", "sral", "sran", "sraka",
        "govno", "govnya", "goven", "g0vn0", "gavno",
        "dermo", "derm",
        "potrah", "trah", "trahat",
        "sperma", "hren", "chlen",
        "manda", "mande", "mandi",
        "uebok", "uebische", "uebki",
        "perdun", "perdet",
        "zasranec", "zasranka",
        // English profanity
        "fuck", "fck", "fuk", "fuc", "phuck", "phuk",
        "fucker", "fucked", "fucking", "fuckoff",
        "motherfucker", "mofo", "mthrfckr",
        "shit", "sht", "shiit", "shiet", "shyt",
        "shitty", "bullshit",
        "bitch", "bich", "biatch", "bytch",
        "ass", "arse", "asshol", "asshole",
        "damn", "dammit",
        "dick", "dik", "dixk", "d1ck", "dikk",
        "pussy", "pusi", "pusy", "pussi",
        "penis", "penls", "pen1s",
        "vagina", "vagin", "vag",
        "cock", "cok", "c0ck", "c0k",
        "cunt", "c0nt", "kunt",
        "whore", "hore", "w h o r e",
        "slut", "sl00t", "sl0t",
        "bastard", "bastrd",
        "nigger", "niger", "n1gger", "nigg3r", "n1gg3r",
        "nigga", "niga", "n1gga", "nigg4",
        "faggot", "fagot", "fagt", "phag",
        "fag", "f4g",
        "retard", "retarded", "r3tard", "ret4rd",
        "stfu", "gtfo", "lmfao",
        "wanker", "wank", "tosser",
        "twat", "tw4t",
        "prick", "pr1ck",
        "douche", "d0uche",
        "dildo", "dild0",
        "jerkoff", "jackoff",
        "cum", "cumshot",
        "blowjob", "bl0wjob",
        "handjob",
        // Mixed RU-EN bypasses
        "cyka", "сука", "cyка",
        "bl9t", "bl9d",
        "p1zda", "p1zd",
        "3bat", "3bal",
        "x y i", "x u i", "x u y",
        "p i z d",
        "b l y a",
        "e b a l",
        "s u k a"
    };

    // --- INSULTS ---
    private static final String[] INSULTS = {
        // RU transliterated
        "debil", "deb1l", "debik",
        "idiot", "idi0t", "1diot",
        "kretin", "kretinka",
        "imbecil", "imbecill",
        "tupoi", "tupoy", "tupica", "tupitsa",
        "loh", "l0h", "lohar", "loshar", "loshara",
        "nub", "noub", "n00b", "newb", "nubik", "nubyara",
        "chmo", "chmoshnik", "chm0",
        "ubludok", "ublyudok",
        "vyrodok", "virodok",
        "otbros", "otbrosy",
        "mraz", "mrazota", "mrazish",
        "tvar", "tvari",
        "podonok", "pad0nok",
        "urod", "ur0d", "urodina",
        "bydlo", "bydl", "bydl0",
        "daun", "d4un", "down",
        "autist", "aut1st",
        "durak", "dura", "duren", "dur4k",
        "kozel", "k0zel", "kozlina",
        "skotina", "skot", "sk0t",
        "gnida", "gn1da",
        "padla", "padlo",
        "paskuda", "paskud",
        "churka", "ch00rka",
        "hach", "khach",
        "churban", "ch00rban",
        "ped0fil", "pedofil",
        "zoofil", "z00fil",
        "necrofil",
        "vyblyadok",
        "pridurok", "prid00rok",
        "eblан", "eblan",
        "pizduk",
        "zhirn", "zhirnyi", "zhirtryes",
        "bomzh", "b0mzh",
        "bich", "bichara",
        "svoloch", "sv0loch",
        "negodyai", "neg0dyai",
        "shavka",
        "psih", "psikh", "psikhopat",
        "shizofrenik", "shiz",
        // English insults
        "noob", "n00b", "nub", "newbie",
        "trash", "tr4sh", "garbage",
        "loser", "l0ser", "looser",
        "idiot", "idi0t", "1diot",
        "moron", "m0ron",
        "stupid", "stup1d", "stoopid",
        "dumb", "dumba", "dumbass",
        "lame", "lameo", "lamer",
        "pathetic", "pathetik",
        "cringe", "cringer",
        "toxic", "tox1c",
        "rat", "r4t",
        "dog", "dawg",
        "pig", "p1g",
        "monkey", "m0nkey",
        "clown", "cl0wn",
        "simp", "s1mp",
        "virgin", "v1rgin",
        "incel", "1ncel",
        "nerd", "n3rd",
        "geek", "g33k",
        "bot", "b0t",
        "braindead", "brainded"
    };

    // --- POLITICS ---
    private static final String[] POLITICS = {
        // Politicians
        "putin", "put1n", "puten", "pytin",
        "zelensky", "zelenskiy", "zelenskii", "zelensk",
        "biden", "b1den", "bayden",
        "trump", "tramp", "tr4mp",
        "navalny", "navalnyi", "naval",
        "lukashenko", "lukashenk", "lukash",
        "stalin", "stal1n", "stаlin",
        "lenin", "len1n",
        "hitler", "h1tler", "gitler", "g1tler", "adolph",
        "mussolini", "mussolin",
        "poroshenko", "porosh",
        "merkel",
        "macron", "makron",
        "obama", "0bama",
        "xi jinping", "xijinping",
        // Countries/regions (political context)
        "ukraine", "ukrain", "ukraina",
        "russia", "rusia", "rusnya", "rashka", "raska",
        "rossiya", "rossia",
        "nato", "n4to", "nato",
        "crimea", "krim", "krym",
        "donbass", "donbas", "d0nbass",
        "donetsk", "d0netsk",
        "lugansk", "luhansk",
        "dnr", "lnr",
        "mariupol", "mariup0l",
        "bucha",
        "moscow", "moskva", "moskow",
        "kiev", "kyiv",
        "usa",
        // Ideologies and terms
        "fascism", "fashizm", "fashist", "fascist",
        "nazism", "nazizm", "nazi", "naz1", "n4zi",
        "neonazi", "neonazist",
        "swastika", "svastika", "svastica",
        "communist", "kommunizm", "kommunist",
        "propaganda", "prop4ganda",
        "terrorist", "terror1st", "terrorizm",
        "genocide", "genocid", "genozid",
        "holocaust", "holokost", "holokaust",
        "war", "voina", "voyna",
        "zieg", "sieg", "z1eg",
        "heil", "he1l", "hayl",
        "reich", "reih", "r3ich",
        "fuhrer", "fyurer",
        "bandera", "bander",
        "azov", "az0v",
        "wagner", "vagner",
        "kadyrov", "kadyr0v",
        "zhirinovsky", "zhirinovsk",
        "shoigu", "sh01gu"
    };

    // --- NSFW / 18+ ---
    private static final String[] NSFW = {
        // Porn
        "porn", "p0rn", "prn", "pron", "pr0n",
        "porno", "p0rno", "porn0",
        "hentai", "henta1", "hent4i", "h3ntai",
        "ecchi", "3cchi",
        "sex", "s3x", "sexx", "secks", "seks",
        "erotic", "erotik", "erotika", "er0tic",
        "onlyfans", "0nlyfans", "onlyf4ns",
        "fansly",
        // Porn sites
        "pornhub", "p0rnhub", "pornhab",
        "xvideos", "xvideo", "xvideo",
        "xhamster", "xh4mster",
        "brazzers", "brazers", "br4zzers",
        "chaturbate", "ch4turbate",
        "stripchat", "str1pchat",
        "rule34", "r34", "rule 34",
        "nhentai", "nhenta1",
        "gelbooru", "danbooru",
        "e621",
        // Terms
        "nsfw", "n5fw",
        "xxx", "xxxx",
        "striptease", "str1ptease", "striptiz",
        "blowjob", "bl0wjob", "bj",
        "anal", "an4l", "analsex",
        "orgy", "0rgy", "orgiya",
        "fetish", "fet1sh", "fetich",
        "bdsm", "bd5m",
        "lesbian", "lesbi", "lezbi",
        "gay", "g4y", "gey",
        "trans", "tr4ns", "tranny",
        "furry", "furri", "f00rri",
        "yaoi", "ya0i", "yaooi",
        "yuri", "yur1",
        "milf", "m1lf",
        "dildo", "d1ldo", "dild0",
        "vibrator", "vibr4tor",
        "orgasm", "0rgasm", "orgazm",
        "masturbat", "masturb", "fap",
        "bukkake", "bukake",
        "creampie", "cream pie",
        "gangbang", "g4ngbang",
        "deepthroat", "deep throat",
        "bondage", "b0ndage",
        "dominat", "d0minat",
        "submissiv",
        "sadism", "sadist",
        "masochis", "mazohis",
        "voyeur", "v0yeur",
        "exhibitionist",
        "incest", "1ncest",
        "lolicon", "loli", "l0li",
        "shotacon", "shota", "sh0ta",
        "ahegao", "aheg40",
        "waifu", "wa1fu",
        "harem", "har3m",
        "tentacle", "tent4cle",
        "futanari", "futa",
        "trap",
        // RU transliterated
        "porno", "seks", "prostitutka", "blyad",
        "striptiz", "razvrat", "razvrash",
        "intim", "1ntim",
        "sosat", "s0sat",
        "drochit", "droch",
        "konchit", "konch",
        "trahnut", "trahat",
        "ebat", "ebal",
        "shalava", "prostitut",
        "putana", "kurva",
        "kurwa"
    };

    // --- SERVER RULE VIOLATIONS ---
    private static final String[] SERVER_RULES = {
        // Griefing/trolling
        "grief", "gr1ef", "griefer",
        "troll", "tr0ll",
        "spam", "sp4m", "spammer",
        "flood", "fl00d",
        "abuse", "ab00se", "abuz",
        "bug", "b00g", "bugged",
        "glitch", "gl1tch",
        "scam", "sc4m", "skam", "scamer", "scammer",
        "steal", "st34l",
        "tos", "t0s",
        // Threats/violence
        "kill", "keel",
        "die", "d1e",
        "death", "d34th",
        "murder", "murd3r",
        "suicide", "suicid",
        "rape", "r4pe", "r4p3",
        "bomb", "b0mb",
        "shoot", "sh00t",
        // Drugs
        "weed", "w33d",
        "marijuana", "marihuana",
        "cocaine", "c0caine", "kokain",
        "heroin", "her01n", "geroin",
        "drug", "dr00g", "narkotik",
        "meth", "m3th", "metamfetamin",
        "lsd",
        "mushroom", "grib", "griby",
        "crack",
        // Gambling (if server forbids)
        "casino", "cas1no", "kazino",
        "bet", "b3t",
        // Doxxing
        "dox", "doxx", "d0x",
        "swat", "sw4t", "swatting",
        "ddos", "dd0s",
        "ip", "1p",
        "leak", "l34k",
        // Advertising
        "discord", "disc0rd",
        "telegram", "teleg", "tg",
        "vk.com", "vkcom",
        // Misc
        "report", "rep0rt",
        "toxic", "t0xic",
        "cancer", "c4ncer", "rak",
        "aids", "a1ds", "spid",
        "covid", "c0vid", "korona",
        "ebola", "eb0la"
    };

    // =====================================================================
    // MAIN CHECK METHOD
    // =====================================================================

    public static List<ViolationResult> checkClanName(String clanName, int slot) {
        List<ViolationResult> results = new ArrayList<>();
        if (clanName == null || clanName.isEmpty()) return results;

        // Create multiple representations for checking
        String lower = clanName.toLowerCase();
        String normalized = normalize(clanName);

        // Also try without vowels (common bypass)
        String noVowels = removeVowels(normalized);

        // Check all categories
        checkCategory(clanName, lower, normalized, noVowels, CHEATS, "Cheats", slot, results);
        checkCategory(clanName, lower, normalized, noVowels, PROFANITY, "Profanity", slot, results);
        checkCategory(clanName, lower, normalized, noVowels, INSULTS, "Insults", slot, results);
        checkCategory(clanName, lower, normalized, noVowels, POLITICS, "Politics", slot, results);
        checkCategory(clanName, lower, normalized, noVowels, NSFW, "NSFW", slot, results);
        checkCategory(clanName, lower, normalized, noVowels, SERVER_RULES, "Server Rules", slot, results);

        return results;
    }

    private static void checkCategory(String original, String lower, String normalized,
                                       String noVowels, String[] keywords, String category,
                                       int slot, List<ViolationResult> results) {
        for (String keyword : keywords) {
            String normalizedKeyword = normalize(keyword);
            if (normalizedKeyword.isEmpty()) {
                normalizedKeyword = keyword.toLowerCase()
                        .replace(" ", "").replace("_", "")
                        .replace("-", "").replace(".", "");
            }

            // Check in normalized form (main check)
            if (normalized.contains(normalizedKeyword)) {
                results.add(new ViolationResult(original, category, keyword, slot));
                return;
            }

            // Check in original lowercase (for exact matches)
            String lowerKeyword = keyword.toLowerCase().replace(" ", "");
            if (lower.replace(" ", "").replace("_", "").replace("-", "").contains(lowerKeyword)) {
                results.add(new ViolationResult(original, category, keyword, slot));
                return;
            }

            // Check without vowels (catches "fck", "btch", etc.)
            if (normalizedKeyword.length() >= 3) {
                String kwNoVowels = removeVowels(normalizedKeyword);
                if (kwNoVowels.length() >= 3 && noVowels.contains(kwNoVowels)) {
                    results.add(new ViolationResult(original, category, keyword, slot));
                    return;
                }
            }
        }
    }

    private static String removeVowels(String s) {
        return s.replaceAll("[aeiou]", "");
    }
}
