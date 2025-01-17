

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crypto;

import java.math.BigInteger;

/**
 *
 * @author lcv
 */
public class Keygen {

    public static final String HEXATOMS = "0123456789ABCDEF", 
            VOWELS = "aaaeeioou",
            CLINK="rsnlm",
            CONSONANTS = CLINK+"bcdfghjklmnprstvwz";
    public static final String ALPHANUMATOMS = " abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-";
    public static final int KOFF = 10;

    protected String inputCode, scanonical;
    protected int inputcodelength;
    protected BigInteger canonical;
    protected boolean offset;

    public static String getAlphaNumKey() {
        return getAlphaNumKey(8);
    }

    public static String getHexaKey() {
        return getHexaKey(8);
    }

    public static String getAlphaNumKey(int length) {
        String newkey = "";
        final int len = length;
        for (int i = 0; i < len; i++) {
            newkey = newkey + ALPHANUMATOMS.charAt(1 + (int) (Math.random() * (ALPHANUMATOMS.length() - 1)));
        }
        return newkey;
    }

    public static String getWordo(int length) {
        String newkey = "";
        final int len = length / 2 + (int) (length * Math.random() / 2);
        for (int i = 0; i < len;) {
            if (i == 0) {
                newkey = newkey + ("" + CONSONANTS.charAt(1 + (int) (Math.random() * (CONSONANTS.length() - 1)))).toUpperCase();
                i += 2;
            } else {
                newkey = newkey + VOWELS.charAt(1 + (int) (Math.random() * (VOWELS.length() - 1)));
                i++;
                double d = Math.random();
                if (d < 0.5) {
                    newkey = newkey + CONSONANTS.charAt(1 + (int) (Math.random() * (CONSONANTS.length() - 1)));
                    i++;
                } else if (d < 0.75) {
                    newkey = newkey + VOWELS.charAt(1 + (int) (Math.random() * (VOWELS.length() - 1)));
                    i++;
                    newkey = newkey + CLINK.charAt(1 + (int) (Math.random() * (CLINK.length() - 1)));
                    i++;

                } else {
                    newkey = newkey + CONSONANTS.charAt(1 + (int) (Math.random() * (CONSONANTS.length() - 1)));
                    i++;
                    newkey = newkey + CONSONANTS.charAt(1 + (int) (Math.random() * (CONSONANTS.length() - 1)));
                    i++;
                }
            }
        }
        return newkey;
    }

    public static String getHexaKey(int length) {
        String newkey = "";
        final int len = length;
        for (int i = 0; i < len; i++) {
            newkey = newkey + HEXATOMS.charAt((int) (Math.random() * HEXATOMS.length()));
        }
        return newkey;
    }

    private void initCode(String input) {
        inputCode = input;
        inputcodelength = inputCode.length();
        offset = false;
        scanonical = "";
    }

    public Keygen() {
        initCode(ALPHANUMATOMS);
    }

    public Keygen(String input) {
        initCode(input);
    }

    public Keygen rotateLeft(int n) {
        String c;

        for (int i = 0; i < n; i++) {
            c = "" + scanonical.charAt(0);
            scanonical = scanonical.substring(1) + c;
        }
        return this;
    }

    public Keygen rotateRight(int n) {
        String c;

        for (int i = 0; i < n; i++) {
            c = "" + scanonical.charAt(scanonical.length() - 1);
            scanonical = "" + c + scanonical.substring(0, scanonical.length() - 1);
        }
        unlock();
        return this;
    }

    public Keygen lock() {
        if (scanonical.equals("")) {
            scanonical = canonical.toString();
        }
        return this;
    }

    public Keygen unlock() {
        if (!scanonical.equals("")) {
            canonical = new BigInteger(scanonical);
            scanonical = "";
        }
        return this;
    }

    public Keygen setOffset(boolean o) {
        offset = o;
        return this;
    }

    /**
     * Checks if a given word could have been generated by the current code
     *
     * @param word
     * @return
     */
    public boolean belongs(String word) {
        for (char cs : word.toCharArray()) {
            if (inputCode.indexOf(cs) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Encodes the given word
     *
     * @param w
     * @return
     */
    public Keygen encode(String w) throws Exception {
        int koffset = 0;
        if (offset) {
            koffset = (int) (Math.random() * (KOFF - 1)) + 1;
        }
        canonical = BigInteger.valueOf(0);
        if (!belongs(w)) {
            throw (new Exception("Word " + w + " does not fix within the input code "));
        }
        for (int i = 0; i < w.length(); i++) {
            char ichar = w.charAt(i);
            int kdigit = inputCode.indexOf(ichar);
            canonical = canonical.multiply(BigInteger.valueOf(inputcodelength));
            canonical = canonical.add(BigInteger.valueOf(kdigit));
        }
        if (offset) {
            lock();
            rotateLeft(koffset);
            scanonical = koffset + scanonical;
            unlock();
        }
        return this;
    }
    // 20730738123215545446067099070324852
    //3073812321554544606709907032485227

    public String decode() {
        int koffset;
        boolean exit = false;
        String res = "";
        if (offset) {
            lock();
            koffset = Integer.valueOf("" + scanonical.charAt(0));
            scanonical = scanonical.substring(1);
            rotateRight(koffset);
            unlock();
        }
        BigInteger aux = canonical;
        while (!exit) {
            int kdigit = aux.mod(BigInteger.valueOf(this.inputcodelength)).intValue();
            char ichar = inputCode.charAt(kdigit);
            res = "" + ichar + res;
            aux = aux.divide(BigInteger.valueOf(this.inputcodelength));
            exit = aux.compareTo(BigInteger.valueOf(0)) <= 0;
        }
        return res;
    }

    public Keygen recode(Keygen other) {
        canonical = other.getCanonical();
        return this;
    }

    public BigInteger getCanonical() {
        return canonical;
    }

}
