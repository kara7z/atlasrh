package com.example.atlasrh.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.service.AuthService;

/**
 * AuthController
 */

@RestController
@RequestMapping
public class AuthController {

  private final AuthService authService;

  AuthController(AuthService authService) {
    this.authService = authService;
  }

  @GetMapping("/login")
  public ResponseEntity login() {
    return ResponseEntity.ok(authService.pageName("Login Page"));
  }

}
