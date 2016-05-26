package io.singleton.wearface;

public class Constants {

    static final String BASE_URL = "http://www.dial.fyi/";
    static final String USER_FRIENDLY_BASE_URL = "dial.fyi/";

    static final String REGISTER_PATH = "register";
    static final String IMG_LIST_PATH = "imgs/%s";
    static final String USER_CONFIG_PATH = "ac/%s";

    static String getRegisterUrl() {
        return BASE_URL + REGISTER_PATH;
    }

    static String getImageListUrl(String token) {
        return BASE_URL + String.format(IMG_LIST_PATH, token);
    }

}
