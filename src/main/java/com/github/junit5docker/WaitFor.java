package com.github.junit5docker;

public @interface WaitFor {

    public static String NOTHING = "";

    String value();
}
