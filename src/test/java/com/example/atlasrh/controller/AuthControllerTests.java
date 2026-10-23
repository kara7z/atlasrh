package com.example.atlasrh.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.atlasrh.service.AuthService;

@WebMvcTest({AuthController.class, HomeController.class})
class AuthControllerTests {
  @Autowired
  private MockMvc mvc;

  @MockitoBean
  private AuthService authService;

  @Test
  void loginUsesTheServiceInterface() throws Exception {
    when(authService.pageName("Hello in login page")).thenReturn("HELLO IN LOGIN PAGE");

    mvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(content().string("HELLO IN LOGIN PAGE"));

    verify(authService).pageName("Hello in login page");
  }

  @Test
  void homeResponseIsUnchanged() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string("Hello World"));
  }
}
