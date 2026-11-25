package dev.lumen.util;

/**
 * Two-set (두벌식) Hangul composer. Feed it the letters typed on a QWERTY layout; it reports what to
 * commit to the text and what the in-progress syllable currently looks like.
 */
public final class Hangul {
    private static final String CHO = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
    private static final String JUNG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ";
    private static final String JONG = " ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ";
    private static final String KEYS = "rRseEfaqQtTdwWczxvgkoiOjpuPhynbml";
    private static final String JAMO = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎㅏㅐㅑㅒㅓㅔㅕㅖㅗㅛㅜㅠㅡㅣ";
    private static final String[][] VOWEL_PAIRS = {{"ㅗㅏ", "ㅘ"}, {"ㅗㅐ", "ㅙ"}, {"ㅗㅣ", "ㅚ"}, {"ㅜㅓ", "ㅝ"}, {"ㅜㅔ", "ㅞ"}, {"ㅜㅣ", "ㅟ"}, {"ㅡㅣ", "ㅢ"}};
    private static final String[][] FINAL_PAIRS = {{"ㄱㅅ", "ㄳ"}, {"ㄴㅈ", "ㄵ"}, {"ㄴㅎ", "ㄶ"}, {"ㄹㄱ", "ㄺ"}, {"ㄹㅁ", "ㄻ"}, {"ㄹㅂ", "ㄼ"}, {"ㄹㅅ", "ㄽ"}, {"ㄹㅌ", "ㄾ"}, {"ㄹㅍ", "ㄿ"}, {"ㄹㅎ", "ㅀ"}, {"ㅂㅅ", "ㅄ"}};

    private char cho, jung, jong; // 0 = empty; compatibility jamo

    /** Result of one key: text to commit before the syllable in progress. */
    public String commit = "";

    /** Maps a typed character to a jamo, or 0 if it is not a Hangul key. Upper-case letters without a shifted jamo fall back to lower case. */
    public static char jamo(char typed) {
        int i = KEYS.indexOf(typed);
        if (i < 0 && Character.isUpperCase(typed)) i = KEYS.indexOf(Character.toLowerCase(typed));
        return i < 0 ? 0 : JAMO.charAt(i);
    }

    private static boolean vowel(char j) { return JUNG.indexOf(j) >= 0; }
    private static char pair(String[][] table, char a, char b) {
        for (String[] p : table) if (p[0].charAt(0) == a && p[0].charAt(1) == b) return p[1].charAt(0);
        return 0;
    }
    private static String split(String[][] table, char c) {
        for (String[] p : table) if (p[1].charAt(0) == c) return p[0];
        return null;
    }

    public boolean composing() { return cho != 0 || jung != 0; }

    /** The syllable in progress as text ("" when nothing is being composed). */
    public String current() {
        if (cho != 0 && jung != 0) {
            int c = CHO.indexOf(cho), v = JUNG.indexOf(jung), f = jong == 0 ? 0 : JONG.indexOf(jong);
            return String.valueOf((char) (0xAC00 + (c * 21 + v) * 28 + f));
        }
        if (cho != 0) return String.valueOf(cho);
        if (jung != 0) return String.valueOf(jung);
        return "";
    }

    /** Ends the syllable in progress (it stays in the text as it is). */
    public void reset() { cho = jung = jong = 0; commit = ""; }

    private void flush() { commit += current(); cho = jung = jong = 0; }

    /** Adds one jamo. Afterwards {@link #commit} holds finished text and {@link #current()} the new in-progress syllable. */
    public void add(char j) {
        commit = "";
        if (vowel(j)) {
            if (jong != 0) {
                // The final consonant moves to the next syllable: 간 + ㅏ = 가나, 닭 + ㅏ = 달가
                String two = split(FINAL_PAIRS, jong);
                char carry = two == null ? jong : two.charAt(1);
                jong = two == null ? 0 : two.charAt(0);
                flush();
                cho = carry; jung = j;
            } else if (jung != 0) {
                char both = pair(VOWEL_PAIRS, jung, j);
                if (both != 0) jung = both; else { flush(); jung = j; }
            } else jung = j;
        } else {
            if (cho != 0 && jung != 0) {
                if (jong == 0) {
                    if (JONG.indexOf(j) > 0) jong = j; else { flush(); cho = j; }
                } else {
                    char both = pair(FINAL_PAIRS, jong, j);
                    if (both != 0) jong = both; else { flush(); cho = j; }
                }
            } else if (cho != 0 || jung != 0) { flush(); cho = j; }
            else cho = j;
        }
    }

    /** Removes the last jamo of the syllable in progress. @return false if nothing was being composed. */
    public boolean backspace() {
        commit = "";
        if (!composing()) return false;
        if (jong != 0) { String two = split(FINAL_PAIRS, jong); jong = two == null ? 0 : two.charAt(0); }
        else if (jung != 0) { String two = split(VOWEL_PAIRS, jung); jung = two == null ? 0 : two.charAt(0); }
        else cho = 0;
        return true;
    }
}
