package com.assignment.atm;

import com.assignment.atm.service.AtmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AtmApplicationTests {

    @Autowired
    private AtmService atmService;

    @Test
    void contextLoads() {
        assertThat(atmService).isNotNull();
        assertThat(atmService.isLoggedIn()).isFalse();
    }
}
