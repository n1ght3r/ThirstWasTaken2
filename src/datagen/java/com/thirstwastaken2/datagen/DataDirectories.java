package com.thirstwastaken2.datagen;

/** Where each kind of data file goes. 1.21 renamed the data pack directories from plural to singular. */
final class DataDirectories {
    private DataDirectories() { }

    /** The directory for {@code singular}, such as {@code recipe}, spelled the way this version reads it. */
    static String of(String singular) {
        //? if >=1.21 {
        return singular;
        //?} else {
        /*return singular + "s";
        *///?}
    }
}
