package net.hollowcube.apiserver.text;

import java.util.List;

/// The term list, each with the longer words it appears inside that are fine to say.
///
/// Terms are stems where the endings vary — `asshol` for asshole and assholes — and the filter
/// already sees through case, accents, leetspeak and spacing, so a term is written once, plainly.
/// A negative is a single word; two words that only spell a term across their seam (`finish it`,
/// `who responded`) are handled by the filter and do not belong here.
final class Profanities {

    static final Trie TERM_TRIE = new Trie();

    private static void term(String term, String... negatives) {
        TERM_TRIE.put(term, List.of(negatives));
    }

    static {
        term("anuslick");
        term("arsehol");
        term("arselick");
        term("asslick");
        term("arsch", "barsch", "marsch");
        term("asshol");
        term("auschwitz");
        term("beaner", "beanery");
        term("bestiality");
        term("baise", "unbaised");
        term("bakachon");
        term("bakatyon");
        term("bastard", "bastardiz", "bastardis");
        term("bitch");
        term("btch");
        term("biatch");
        term("bussy");
        term("blowjob");
        term("blowme");
        term("bukakke");
        term("buttplug");
        term("buttchug");
        term("butagorosi"); // https://www.urbandictionary.com/define.php?term=butagorosi
        term("cagada");
        term("caralho");
        term("cameljockey");
        term("castrate");
        term("cazzo");
        term("ceemen");
        term("chankoro");
        term("chink", "pachinko", "chinkapin", "chinkara", "chinking");
        term("chingchong");
        term("choad");
        term("chode", "psychodelic");
        term("chlamydia");
        term("clit", "clitheroe", "clitch", "clitic", "clitellum");
        term("clitoris");
        term("cock",
            "cockade", "cockatiel", "cockatiels", "cockatoo", "cockatoos", "cockatrice", "cockayne", "cockburn",
            "cockcroft", "cocked", "cocker", "cockerel", "cockers", "cockeyed", "cockiness", "cocking", "cocklebur",
            "cockney", "cockpit", "cockpits", "cockroach", "cockroaches", "cockscomb", "cockspur", "cocktail", "cockle",
            "cockamamie", "cockamamy", "babcock", "gamecock", "hancock", "haycock", "hitchcock", "leacock", "peacock",
            "poppycock", "shuttlecock", "stopcock", "woodcock",
            "cockchafer", "cockcrow", "cocky", "cockapoo", "cocksure", "cockswain");
        term("coon", "cocoon", "laocoon", "raccoon", "racoon", "tycoon", "coccoon", "recoonect", "coonhound");
        term("cocain");
        term("coitus");
        term("cottonpic");
        term("cottonpik");
        term("cum",
            "acumen", "acuminate", "altocumulus", "cumber", "cumbing", "cumbria", "cumbrian", "cumbrous", "cummerbund",
            "cumming", "cumulat", "cumuli", "cumulonimbus", "cumulus", "encumber", "encumbrance", "scumbag", "locum",
            "modicum", "magnacumlaude", "macumba", "practicum", "recumbent", "scum", "slocum", "stratocumulus", "succumb",
            "talcum", "taraxacum", "tecumseh", "tucuman", "capsicum", "cecum", "circum", "colchicum", "document",
            "ecumeni", // ecumenical, ecumenism
            "illyricum",
            "incumben", // incumbent, incumbency
            "cumminity", "cumpl", "cumin", "cumquat", "cumbia", "cumbre", "cummins", "documet");
        term("cunt", "scunthorpe", "acocunt", "ocunting");
        term("cvnt");
        term("cunny");
        term("cunnie");
        term("csam");
        term("cyka", "bouncykath");
        term("darkie");
        term("dick",
            "chappaquiddick", "dickens", "dickensian", "dickerson", "dickey", "dickies", "dickinson", "dickson",
            "dickvandyke", "dicky", "riddick",
            "dicker", "dickcissel");
        term("dildo");
        term("douchebag");
        term("dyke", "vandyke");
        term("downie");
        term("dumbass");
        term("ejaculate");
        term("fag", "antofagasta", "serfage", "wharfage", "fagin", "leafage", "cofagrigu", "naturfag");
        term("feck", "feckless");
        term("fellate");
        term("fellatio");
        term("felch", "apfelchips", "felchen");
        term("fuck");
        term("fvck");
        term("fxck");
        term("fack", "fackel");
        term("fzck");
        term("fck");
        term("fudgepacker");
        term("flange", "flanged", "flanges", "flanger");
        term("gestapo");
        term("gook");
        term("horny", "thorny");
        term("hooker");
        term("hitler");
        term("incest");
        term("jap", "japan", "japko", "japonic", "japchae", "jape", "perjaps");
        term("jizz");
        term("jigabo");
        term("junglebunny");
        term("kkk");
        term("kike", "lkike");
        term("klux");
        term("kluklux");
        term("klukluxklan");
        term("koon", "alkakoon", "pakoon", "sekoon", "olkoon", "tulkoon");
        term("lickmy");
        term("masturbat");
        term("molest", "molesta", "moleste", "molesti", "molesto");
        term("muff",
            "muffed", "muffin", "muffins", "muffle", "muffled", "muffler", "mufflers", "muffles", "muffling", "muffs",
            "ragamuffin", "earmuff", "earmuffs");
        term("nazi", "ashkenazi", "ashkenazic", "ashkenazim", "monazite",
            "amnazing", "naziva", "nazim", "nazir", "nazional");
        term("nigg");
        term("niqa", "niqab");
        term("nigga");
        term("niqqa");
        term("niggu");
        term("niqqu");
        term("niggr");
        term("niger", // yes, this will false flag, but according to Seth it's worth it
            "nigeria", "nigerien", "weniger", "einiger", "sonniger");
        term("nigger");
        term("niglet");
        term("nignog");
        term("paki", "pakistan", "chlopaki", "paking");
        term("penis", "penistone", "openish", "happenis", "penisola", "peniscola");
        term("porn");
        term("prostitut");
        term("pube", "puber", "pubesc");
        term("pussie");
        term("pussy", "pussycat", "pussyfoot", "pussywillow");
        term("raghead");
        term("rape",
            "grape", "trapeze", "trapezium", "trapezius", "trapezoid", "therapeutic", "drape", "parapet", "rapeseed",
            "scrape", "serape",
            "rapelle", "rattrape", "trapes", "wraped");
        term("rapist", "therapist", "tharapist");
        term("retard", "retardant", "retarder", "retarding");
        term("rimjob");
        term("shit", "cushitic", "shitake", "peshitta", "libshitz", "shitzu", "yamashita",
            "screnshit", "zhanshishiti");
        term("slut", "aboslutely");
        term("spunk", "spunky", "tidspunkt");
        term("suckmy");
        term("sodom");
        term("semen", "sement", "horsemen", "norsemen", "wisemen", "advertisemen"); // basement, casement
        term("teensex");
        term("tittie");
        term("titty", "entitty");
        term("trannie");
        term("tranny");
        term("vagina");
        term("wank", "swank", "wankel", "obiwankenobi", "thwanky");
        term("wetback");
        term("whore");
        term("whitepower");
        term("fondle");
        term("minestorm");
        term("kissmy");
        term("blowmy");
        term("jelqing");
        term("dafuq");
    }

    private Profanities() {}
}
