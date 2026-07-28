package com.yekdb.storage.page;

/**
 * YEKDB fiziksel sayfa türlerini temsil eder.
 *
 * Her türün disk üzerinde saklanan sabit bir kodu vardır.
 */
public enum PageType {

    FILE_HEADER(0),
    DATA(1),
    INDEX(2),
    FREE(3);

    private final int code;

    PageType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static PageType fromCode(int code) {

        for (PageType type : values()) {

            if (type.code == code) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Unknown page type code: " + code
        );
    }
}