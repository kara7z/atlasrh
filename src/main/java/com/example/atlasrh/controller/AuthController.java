package com.example.atlasrh.controller;

import com.example.atlasrh.service.AuthService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * AuthController
 */

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;

  AuthController(AuthService authService) {
    this.authService = authService;
  }

  @GetMapping("/login")
  public String login() {
    return authService.pageName("Login Page");
  }

}
