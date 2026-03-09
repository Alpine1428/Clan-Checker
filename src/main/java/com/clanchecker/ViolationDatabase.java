package com.clanchecker;

import java.util.*;

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

    private static final Map<Character, String> CHAR_MAP = new HashMap<>();

    static {
        // Cyrillic lowercase -> Latin
        CHAR_MAP.put('\u0430', "a");  CHAR_MAP.put('\u0431', "b");
        CHAR_MAP.put('\u0432', "v");  CHAR_MAP.put('\u0433', "g");
        CHAR_MAP.put('\u0434', "d");  CHAR_MAP.put('\u0435', "e");
        CHAR_MAP.put('\u0451', "e");  CHAR_MAP.put('\u0436', "zh");
        CHAR_MAP.put('\u0437', "z");  CHAR_MAP.put('\u0438', "i");
        CHAR_MAP.put('\u0439', "i");  CHAR_MAP.put('\u043a', "k");
        CHAR_MAP.put('\u043b', "l");  CHAR_MAP.put('\u043c', "m");
        CHAR_MAP.put('\u043d', "n");  CHAR_MAP.put('\u043e', "o");
        CHAR_MAP.put('\u043f', "p");  CHAR_MAP.put('\u0440', "r");
        CHAR_MAP.put('\u0441', "s");  CHAR_MAP.put('\u0442', "t");
        CHAR_MAP.put('\u0443', "u");  CHAR_MAP.put('\u0444', "f");
        CHAR_MAP.put('\u0445', "h");  CHAR_MAP.put('\u0446', "ts");
        CHAR_MAP.put('\u0447', "ch"); CHAR_MAP.put('\u0448', "sh");
        CHAR_MAP.put('\u0449', "sch"); CHAR_MAP.put('\u044a', "");
        CHAR_MAP.put('\u044b', "y");  CHAR_MAP.put('\u044c', "");
        CHAR_MAP.put('\u044d', "e");  CHAR_MAP.put('\u044e', "yu");
        CHAR_MAP.put('\u044f', "ya");

        // Cyrillic uppercase -> Latin
        CHAR_MAP.put('\u0410', "a");  CHAR_MAP.put('\u0411', "b");
        CHAR_MAP.put('\u0412', "v");  CHAR_MAP.put('\u0413', "g");
        CHAR_MAP.put('\u0414', "d");  CHAR_MAP.put('\u0415', "e");
        CHAR_MAP.put('\u0401', "e");  CHAR_MAP.put('\u0416', "zh");
        CHAR_MAP.put('\u0417', "z");  CHAR_MAP.put('\u0418', "i");
        CHAR_MAP.put('\u0419', "i");  CHAR_MAP.put('\u041a', "k");
        CHAR_MAP.put('\u041b', "l");  CHAR_MAP.put('\u041c', "m");
        CHAR_MAP.put('\u041d', "n");  CHAR_MAP.put('\u041e', "o");
        CHAR_MAP.put('\u041f', "p");  CHAR_MAP.put('\u0420', "r");
        CHAR_MAP.put('\u0421', "s");  CHAR_MAP.put('\u0422', "t");
        CHAR_MAP.put('\u0423', "u");  CHAR_MAP.put('\u0424', "f");
        CHAR_MAP.put('\u0425', "h");  CHAR_MAP.put('\u0426', "ts");
        CHAR_MAP.put('\u0427', "ch"); CHAR_MAP.put('\u0428', "sh");
        CHAR_MAP.put('\u0429', "sch"); CHAR_MAP.put('\u042a', "");
        CHAR_MAP.put('\u042b', "y");  CHAR_MAP.put('\u042c', "");
        CHAR_MAP.put('\u042d', "e");  CHAR_MAP.put('\u042e', "yu");
        CHAR_MAP.put('\u042f', "ya");

        // Leet speak
        CHAR_MAP.put('0', "o"); CHAR_MAP.put('1', "i");
        CHAR_MAP.put('2', "z"); CHAR_MAP.put('3', "e");
        CHAR_MAP.put('4', "a"); CHAR_MAP.put('5', "s");
        CHAR_MAP.put('6', "g"); CHAR_MAP.put('7', "t");
        CHAR_MAP.put('8', "b"); CHAR_MAP.put('9', "g");

        // Accented Latin
        CHAR_MAP.put('\u00e0', "a"); CHAR_MAP.put('\u00e1', "a");
        CHAR_MAP.put('\u00e2', "a"); CHAR_MAP.put('\u00e3', "a");
        CHAR_MAP.put('\u00e4', "a"); CHAR_MAP.put('\u00e5', "a");
        CHAR_MAP.put('\u00e6', "ae"); CHAR_MAP.put('\u00e7', "c");
        CHAR_MAP.put('\u00e8', "e"); CHAR_MAP.put('\u00e9', "e");
        CHAR_MAP.put('\u00ea', "e"); CHAR_MAP.put('\u00eb', "e");
        CHAR_MAP.put('\u00ec', "i"); CHAR_MAP.put('\u00ed', "i");
        CHAR_MAP.put('\u00ee', "i"); CHAR_MAP.put('\u00ef', "i");
        CHAR_MAP.put('\u00f0', "d"); CHAR_MAP.put('\u00f1', "n");
        CHAR_MAP.put('\u00f2', "o"); CHAR_MAP.put('\u00f3', "o");
        CHAR_MAP.put('\u00f4', "o"); CHAR_MAP.put('\u00f5', "o");
        CHAR_MAP.put('\u00f6', "o"); CHAR_MAP.put('\u00f8', "o");
        CHAR_MAP.put('\u00f9', "u"); CHAR_MAP.put('\u00fa', "u");
        CHAR_MAP.put('\u00fb', "u"); CHAR_MAP.put('\u00fc', "u");
        CHAR_MAP.put('\u00fd', "y"); CHAR_MAP.put('\u00ff', "y");
        CHAR_MAP.put('\u00df', "ss");

        // Symbols
        CHAR_MAP.put('@', "a"); CHAR_MAP.put('$', "s");
        CHAR_MAP.put('!', "i"); CHAR_MAP.put('|', "l");
        CHAR_MAP.put('+', "t"); CHAR_MAP.put('(', "c");
        CHAR_MAP.put('{', "c"); CHAR_MAP.put('[', "c");
        CHAR_MAP.put(')', "d"); CHAR_MAP.put('}', "d");
        CHAR_MAP.put(']', "d");

        // Fullwidth Latin
        for (char c = '\uff21'; c <= '\uff3a'; c++) {
            CHAR_MAP.put(c, String.valueOf((char) ('a' + (c - '\uff21'))));
        }
        for (char c = '\uff41'; c <= '\uff5a'; c++) {
            CHAR_MAP.put(c, String.valueOf((char) ('a' + (c - '\uff41'))));
        }
    }

    public static String normalize(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        String lower = input.toLowerCase();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (isSeparator(c)) continue;
            String mapped = CHAR_MAP.get(c);
            if (mapped != null) {
                sb.append(mapped);
            } else if (c >= 'a' && c <= 'z') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static List<String> generateAlternatives(String input) {
        List<String> alts = new ArrayList<>();
        if (input == null) return alts;
        String lower = input.toLowerCase();
        String norm = normalize(input);

        // 4 = ch (Russian leet: ч=4)
        if (lower.contains("4")) {
            alts.add(normalize(lower.replace("4", "ch")));
            // Also 4 at different positions
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lower.length(); i++) {
                if (lower.charAt(i) == '4') sb.append("ch");
                else {
                    String m = CHAR_MAP.get(lower.charAt(i));
                    if (m != null) sb.append(m);
                    else if (lower.charAt(i) >= 'a' && lower.charAt(i) <= 'z') sb.append(lower.charAt(i));
                }
            }
            alts.add(sb.toString());
        }
        // 6 = b or sh
        if (lower.contains("6")) {
            alts.add(normalize(lower.replace("6", "b")));
            alts.add(normalize(lower.replace("6", "sh")));
        }
        // 3 = z (alternative to e)
        if (lower.contains("3")) {
            alts.add(normalize(lower.replace("3", "z")));
        }
        // ck -> k
        if (norm.contains("ck")) alts.add(norm.replace("ck", "k"));
        // ph -> f
        if (norm.contains("ph")) alts.add(norm.replace("ph", "f"));
        // doubled letters removed
        alts.add(removeDoubles(norm));
        // x -> ks
        if (norm.contains("x")) alts.add(norm.replace("x", "ks"));
        // x -> h (Russian x=х)
        if (lower.contains("x")) alts.add(normalize(lower.replace("x", "h")));

        return alts;
    }

    private static boolean isSeparator(char c) {
        return c == '_' || c == '-' || c == '.' || c == ' ' || c == '*'
            || c == '#' || c == '~' || c == '`' || c == '\''
            || c == '"' || c == ',' || c == ';' || c == ':'
            || c == '/' || c == '\\' || c == '<' || c == '>'
            || c == '^' || c == '=' || c == '%' || c == '&'
            || c == '\u00a7';
    }

    private static String removeDoubles(String s) {
        if (s.length() <= 1) return s;
        StringBuilder sb = new StringBuilder();
        sb.append(s.charAt(0));
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != s.charAt(i - 1)) sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    private static String removeVowels(String s) {
        return s.replaceAll("[aeiou]", "");
    }

    // =====================================================================
    // KEYWORD DATABASES
    // =====================================================================

    private static final String[] CHEATS = {
        "wurst", "meteor", "aristois", "impact", "inertia", "lambda",
        "liquidbounce", "sigma", "novoline", "exhibition", "rise",
        "tenacity", "enthium", "azura", "pandaware", "astolfo",
        "zeroday", "hanabi", "sixsense", "wolfram", "kami",
        "salhack", "future", "rusherhack", "konas", "phobos",
        "gamesense", "pyro", "huzuni", "weepcraft", "vape",
        "cheatbreaker",
        "fdpclient", "fdp", "novahack", "skilled",
        "medusa", "flavor", "drip",
        "antic", "atani", "autumn",
        "crypt", "eclipse",
        "flux", "hydra",
        "neptune", "phantom", "prestige", "remix",
        "raven", "ravenclient", "ravenb",
        "skid", "sleek",
        "vapor", "xatz", "zephyr",
        "neverlose", "onetap", "skeet", "fatality",
        "interium", "nixware", "evolve",
        "aimware", "legendware", "spirthack",
        "killaura", "kilaura", "killaure", "kilaure",
        "aimbot", "aimassist",
        "autoclicker", "autoclick", "autoklicker", "autoklick",
        "avtoklik", "avtoclicker", "avtokliker",
        "xray", "xrei",
        "nuker", "scaffold", "scaff",
        "flyhack",
        "noclip",
        "antikb", "nokb", "antiknockback",
        "velocity", "bhop", "bunnyhop",
        "triggerbot",
        "autototem", "autocrystal", "automine",
        "fastplace", "fastbreak",
        "freecam",
        "wallhack",
        "tracers", "tracer",
        "chams", "esp",
        "baritone", "bariton",
        "speedhack",
        "highjump",
        "noslowdown", "noslow",
        "nofall", "nofalldamage",
        "waterwalk",
        "phase", "phaze",
        "reach", "longreach",
        "hitbox", "hitboxes",
        "criticals", "crits", "autocrit",
        "autosoup", "autopotion", "autopot",
        "autoarmor",
        "cheststealer",
        "inventorywalk",
        "safewalk",
        "antibot",
        "backtrack",
        "hack", "hacks", "hak", "haks",
        "cheat", "cheats", "chiter", "cheater",
        "hacker", "haker", "hackerman",
        "exploit", "eksploit",
        "dupe", "duper", "duping",
        "inject", "injector", "inzhekt",
        "macro", "makro", "makros",
        "chity", "chiti",
        "haki",
        "vzlom",
        "obhod",
        "clicker", "kliker",
        "autofarm", "autofish", "autoeat",
        "fullbright",
        "blink", "disabler",
        "antiafk",
        "ghostclient",
        "closetcheater"
    };

    private static final String[] PROFANITY = {
        // HUI family
        "hui", "huy", "huj", "hue", "huya", "huyu", "huei",
        "huilo", "huylo", "huesos", "hueta", "hueviy",
        "huynya", "huinya", "huev",
        "nahui", "nahuy", "nahuj",
        "poshyolnahui", "idnahui", "idinahui",
        "ohuel", "ohuet", "ohuyel",
        // PIZD family
        "pizd", "pizda", "pizdec", "pizdos", "pizdez",
        "pizdato", "pizduk", "pizdet", "pizdish",
        "pizdabol",
        "raspizday", "raspizdyai",
        "pripizd", "zapizd", "otpizd",
        "spizd", "spizdil",
        // BLYAT family
        "blyat", "bliat", "blad", "blyad", "blya",
        "bljat", "blyt", "blet", "blyadi",
        "blyaha", "blyadina", "blyadstvo",
        "blyadskiy",
        // EBAT family
        "ebat", "eban", "ebal", "ebash", "ebuch",
        "ebanat", "ebanko", "ebanyi", "ebaniy",
        "ebanashka", "ebanutiy",
        "ebalo", "ebanul", "ebashit",
        "zaebat", "zaeb", "zaebis", "zaebali", "zaebal",
        "proeb", "proebal",
        "ueb", "ueban", "uebok", "uebische",
        "vyeb", "vyebal",
        "doeb", "doebal",
        "oteb", "otebal",
        "poeb", "poebal",
        "razeb", "razebal",
        "eblan", "eblanysh",
        // YOBANIY family
        "yoban", "yobany", "yobaniy", "yob",
        // SUKA family
        "suka", "suchka", "suchar", "suchara",
        "syki", "suca", "sukablyat",
        "sukiny", "sukin",
        // MUDAK family
        "mudak", "mudila", "mudo", "mudozv",
        "mudozvon",
        // ZALUP family
        "zalupa", "zalup",
        // DOLBOEB family
        "dolboeb", "dolboyob", "dolboed",
        // SHLUHA family
        "shluh", "shlukha", "shluha",
        "shalav", "shalava",
        // PIDOR family
        "pidor", "pidar", "pidr", "pederast",
        "pedik", "peder",
        "pidoras", "pidaras", "pidrila",
        // GONDON family
        "gandon", "gondon",
        // DROCHIT family
        "droch", "drochi", "drochit", "drochila",
        "nadroch", "zadroch",
        // ZHOPA family
        "zhopa", "zhop", "zhopu", "zhope",
        "zhopoliz",
        // SRAT family
        "srat", "sral", "sran", "sraka",
        "nasrat", "obsrat", "usrat",
        "zasranec", "zasranka",
        // GOVNO family
        "govno", "govnya", "goven", "gavno",
        "govnoed",
        // DERMO family
        "dermo", "derm",
        // CHLEN family
        "chlen", "chlenov", "chleny",
        "chlenososkа",
        // HREN family
        "hren", "hrenov",
        "nahren", "ohrenet",
        // MANDA family
        "manda", "mande", "mandi",
        // PERD family
        "perdun", "perdet",
        // TRAH family
        "potrah", "trah", "trahat", "trahnut",
        // SPERMA
        "sperma",
        // English profanity
        "fuck", "fck", "fuk", "fuc", "phuck", "phuk",
        "fucker", "fucked", "fucking", "fuckoff",
        "motherfucker", "mofo",
        "fuckboy", "fucktard",
        "shit", "sht", "shiit", "shiet", "shyt",
        "shitty", "bullshit", "dipshit", "shithead",
        "bitch", "bich", "biatch", "bytch",
        "asshole",
        "damn", "dammit", "goddamn",
        "dick", "dik", "dixk",
        "dickhead",
        "pussy", "pusi", "pusy",
        "penis",
        "vagina", "vagin",
        "cock", "cok",
        "cocksucker",
        "cunt", "kunt",
        "whore", "hore",
        "slut",
        "bastard",
        "nigger", "niger",
        "nigga", "niga",
        "faggot", "fagot",
        "retard", "retarded",
        "wanker", "wank", "tosser",
        "twat",
        "prick",
        "douche",
        "jerkoff", "jackoff",
        "cum", "cumshot",
        "blowjob",
        "handjob",
        "cyka",
        "pizdabolshy", "ebanina",
        "konchita", "konch", "konchit", "konchay",
        "govnyany", "sranyy"
    };

    private static final String[] INSULTS = {
        "debil", "debik", "debiloid",
        "idiot", "idioty",
        "kretin", "kretinka",
        "imbecil",
        "tupoi", "tupoy", "tupica", "tupitsa",
        "loh", "lohar", "loshar", "loshara", "loshok",
        "loshped",
        "nub", "noub", "nubik", "nubyara",
        "nubas",
        "chmo", "chmoshnik",
        "ubludok", "ublyudok",
        "vyrodok", "virodok",
        "otbros", "otbrosy",
        "mraz", "mrazota", "mrazish", "mrazi",
        "tvar", "tvari",
        "podonok", "podonki",
        "urod", "urodina", "urody",
        "bydlo", "bydlyak",
        "daun", "dauny", "daunism",
        "autist", "autizm",
        "durak", "dura", "duren", "durdom",
        "durochka",
        "kozel", "kozlina", "kozly",
        "skotina", "skot", "skoty",
        "gnida", "gnidy",
        "padla", "padlo",
        "paskuda", "paskudina",
        "churka", "churki", "churban",
        "hach", "khach", "hachik",
        "pedofil",
        "zoofil",
        "necrofil",
        "vyblyadok",
        "pridurok", "pridurki",
        "zhirn", "zhirnyi", "zhirtrest",
        "zhirnyara",
        "bomzh", "bomzhara",
        "bichara",
        "svoloch", "svolochi",
        "negodyai",
        "shavka", "shavki",
        "psih", "psikh", "psikhopat",
        "shizofrenik", "shiz", "shiza",
        "oligofren",
        "obezyana",
        "baran",
        "osel", "ishak",
        "svin", "svinya",
        "gadina", "gad", "gady",
        "parasit", "parazit",
        "ushlepok",
        "shmara",
        "zhulik",
        "podlets", "podlyi",
        "noob", "nub", "newbie",
        "trash", "garbage",
        "loser", "looser",
        "moron",
        "stupid", "stoopid",
        "dumb", "dumbass",
        "lame", "lameo", "lamer",
        "pathetic",
        "cringe",
        "toxic",
        "clown",
        "simp",
        "virgin",
        "incel",
        "nerd",
        "braindead",
        "degenerate", "degen",
        "scumbag", "scum",
        "filth", "filthy",
        "worthless",
        "disgrace", "disgusting",
        "peasant",
        "coward",
        "creep",
        "freak",
        "psycho",
        "lunatic",
        "imbecile",
        "dimwit", "halfwit", "nitwit",
        "bonehead",
        "knobhead",
        "bellend",
        "muppet",
        "plonker",
        "numpty",
        "donkey", "jackass"
    };

    private static final String[] POLITICS = {
        "putin", "putler",
        "zelensky", "zelenskiy", "zelenskii", "zelensk",
        "biden", "bayden",
        "trump", "tramp",
        "navalny", "navalnyi",
        "lukashenko", "lukash",
        "stalin",
        "lenin",
        "hitler", "gitler", "adolph",
        "mussolini",
        "poroshenko", "porosh",
        "merkel",
        "macron", "makron",
        "obama",
        "xijinping", "jinping",
        "kimjongun",
        "pinochet",
        "castro",
        "cheguevara",
        "gorbachev",
        "khrushchev", "hruschev",
        "brezhnev",
        "yeltsin", "eltsyn",
        "medvedev",
        "lavrov",
        "shoigu",
        "ukraine", "ukrain", "ukraina", "ukry", "ukrop",
        "rusnya", "rashka", "raska",
        "crimea", "krim", "krym",
        "donbass", "donbas",
        "donetsk", "lugansk", "luhansk",
        "mariupol", "bucha",
        "chechnya", "ichkeria",
        "dagestan",
        "abkhaziya", "osetiya",
        "palestina", "palestine", "izrail",
        "azov",
        "wagner", "vagner",
        "kadyrov",
        "prigozhin",
        "girkin", "strelkov",
        "fascism", "fashizm", "fashist", "fascist",
        "nazism", "nazizm", "nazi",
        "neonazi",
        "swastika", "svastika",
        "kommunizm", "kommunist",
        "propaganda",
        "terrorist", "terrorizm",
        "genocide", "genocid", "genozid",
        "holocaust", "holokost",
        "voina", "voyna",
        "zieg", "sieg",
        "heil", "hayl",
        "reich", "reih",
        "fuhrer", "fyurer",
        "bandera", "bander", "banderovets",
        "vlasov", "vlasovets",
        "denazifikatsiya",
        "spetsoperatsiya",
        "krimnash",
        "zhirinovsky", "zhirinovsk"
    };

    private static final String[] NSFW = {
        "porn", "prn", "pron",
        "porno",
        "hentai",
        "ecchi",
        "sex", "seks",
        "erotic", "erotik", "erotika",
        "onlyfans",
        "fansly",
        "pornhub", "pornhab",
        "xvideos", "xvideo",
        "xhamster",
        "brazzers", "brazers",
        "chaturbate",
        "stripchat",
        "rule34",
        "nhentai",
        "gelbooru", "danbooru",
        "e621",
        "redtube",
        "youporn",
        "xnxx",
        "motherless",
        "spankbang",
        "nsfw",
        "xxx", "xxxx",
        "striptease", "striptiz",
        "blowjob",
        "anal", "analsex",
        "orgy", "orgiya",
        "fetish", "fetich",
        "bdsm",
        "lesbian", "lesbi", "lezbi",
        "gay", "gey",
        "tranny",
        "furry", "furri",
        "yaoi",
        "yuri",
        "milf",
        "dildo",
        "vibrator",
        "orgasm", "orgazm",
        "masturbat", "masturb",
        "bukkake", "bukake",
        "creampie",
        "gangbang",
        "deepthroat",
        "bondage",
        "dominat",
        "submissiv",
        "sadism", "sadist",
        "masochis", "mazohis",
        "voyeur",
        "exhibitionist",
        "incest",
        "lolicon", "loli",
        "shotacon", "shota",
        "ahegao",
        "waifu",
        "harem",
        "tentacle",
        "futanari", "futa",
        "prostitutka", "prostit",
        "razvrat", "razvrash",
        "intim",
        "sosat",
        "trahnut", "trahat",
        "shalava",
        "putana", "kurva", "kurwa",
        "shlyuha", "shluha",
        "minyet", "minet",
        "gruppovuha",
        "svinger"
    };

    private static final String[] SERVER_RULES = {
        "grief", "griefer", "griefing",
        "troll", "trolling",
        "spam", "spammer", "spamming",
        "flood", "flooding",
        "abuse", "abuz", "abusing",
        "glitch", "glitching",
        "scam", "skam", "scammer", "scamming",
        "steal", "stealing",
        "murder",
        "suicide", "suicid",
        "rape",
        "bomb", "bombing",
        "threat",
        "weed",
        "marijuana",
        "cocaine", "kokain",
        "heroin", "geroin",
        "narkotik", "narkota",
        "meth", "metamfetamin",
        "amfetamin",
        "ekstazi", "ecstasy",
        "gashish", "hashish",
        "opium",
        "casino", "kazino",
        "doxx",
        "swat", "swatting",
        "ddos",
        "discord",
        "telegram",
        "cancer", "rak",
        "aids", "spid",
        "covid", "korona",
        "ebola",
        "selfharm",
        "anorexia",
        "obman", "moshennik",
        "multiakk", "multiaccount",
        "tvink"
    };

    // Short words: exact match only to avoid false positives
    private static final Set<String> SHORT_EXACT_ONLY = new HashSet<>(Arrays.asList(
        "rat", "pig", "dog", "gay", "fag", "ass", "cum",
        "bug", "wh", "esp", "bot", "gad", "lsd",
        "dnr", "lnr", "rak", "yob", "hue"
    ));

    // =====================================================================
    // MAIN CHECK
    // =====================================================================

    public static List<ViolationResult> checkClanName(String clanName, int slot) {
        List<ViolationResult> results = new ArrayList<>();
        if (clanName == null || clanName.isEmpty()) return results;

        String lower = clanName.toLowerCase().replaceAll("[\\s_\\-.]", "");
        String normalized = normalize(clanName);
        String noVowels = removeVowels(normalized);
        String noDoubles = removeDoubles(normalized);
        List<String> alternatives = generateAlternatives(clanName);

        checkCategory(clanName, lower, normalized, noVowels, noDoubles, alternatives,
                CHEATS, "Cheats", slot, results);
        checkCategory(clanName, lower, normalized, noVowels, noDoubles, alternatives,
                PROFANITY, "Profanity", slot, results);
        checkCategory(clanName, lower, normalized, noVowels, noDoubles, alternatives,
                INSULTS, "Insults", slot, results);
        checkCategory(clanName, lower, normalized, noVowels, noDoubles, alternatives,
                POLITICS, "Politics", slot, results);
        checkCategory(clanName, lower, normalized, noVowels, noDoubles, alternatives,
                NSFW, "NSFW", slot, results);
        checkCategory(clanName, lower, normalized, noVowels, noDoubles, alternatives,
                SERVER_RULES, "Server Rules", slot, results);

        return results;
    }

    private static void checkCategory(String original, String lower, String normalized,
                                       String noVowels, String noDoubles,
                                       List<String> alternatives,
                                       String[] keywords, String category,
                                       int slot, List<ViolationResult> results) {
        for (String keyword : keywords) {
            String normKw = normalize(keyword);
            if (normKw.isEmpty()) {
                normKw = keyword.toLowerCase().replaceAll("[\\s_\\-.]", "");
            }
            if (normKw.length() < 2) continue;

            boolean shortExact = SHORT_EXACT_ONLY.contains(keyword.toLowerCase())
                || normKw.length() <= 3;

            if (shortExact) {
                if (normalized.equals(normKw) || lower.equals(normKw)
                    || noDoubles.equals(normKw)) {
                    results.add(new ViolationResult(original, category, keyword, slot));
                    return;
                }
                for (String alt : alternatives) {
                    if (alt.equals(normKw)) {
                        results.add(new ViolationResult(original, category, keyword, slot));
                        return;
                    }
                }
            } else {
                if (normalized.contains(normKw)) {
                    results.add(new ViolationResult(original, category, keyword, slot));
                    return;
                }
                String lowerKw = keyword.toLowerCase().replaceAll("[\\s_\\-.]", "");
                if (lower.contains(lowerKw)) {
                    results.add(new ViolationResult(original, category, keyword, slot));
                    return;
                }
                String noDblKw = removeDoubles(normKw);
                if (noDblKw.length() >= 3 && noDoubles.contains(noDblKw)) {
                    results.add(new ViolationResult(original, category, keyword, slot));
                    return;
                }
                if (normKw.length() >= 4) {
                    String nvKw = removeVowels(normKw);
                    if (nvKw.length() >= 3 && noVowels.contains(nvKw)) {
                        results.add(new ViolationResult(original, category, keyword, slot));
                        return;
                    }
                }
                for (String alt : alternatives) {
                    if (alt.contains(normKw)) {
                        results.add(new ViolationResult(original, category, keyword, slot));
                        return;
                    }
                }
            }
        }
    }
}
