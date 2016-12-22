package com.github.junit5docker;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class PortBindingTest {

    @Test
    void shouldValidateEqualsContract() {
        EqualsVerifier.forClass(PortBinding.class).verify();
    }
}
