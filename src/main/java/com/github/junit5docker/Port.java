package com.github.junit5docker;

public @interface Port {
    int exposed();

    int inner();
}
