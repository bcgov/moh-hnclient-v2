package ca.bc.gov.hlth.hnclientv2;

public final class Util {

    private Util() {
    }

    public static void requireNonBlank(String str, String msg) {
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException(msg);
        }
    }

}
