package ca.bc.gov.hlth.hnclientv2;

import io.netty.util.internal.StringUtil;

public  class Util {

	public static void requireNonBlank(String str, String msg) {
        if (StringUtil.isNullOrEmpty(str)) {
            throw new NullPointerException(msg);
        }
    }

}
