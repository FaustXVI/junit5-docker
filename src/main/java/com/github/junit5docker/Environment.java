package com.github.junit5docker;

public @interface Environment {
    String key();

    String value();
}
