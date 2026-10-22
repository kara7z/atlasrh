package com.example.atlasrh.service;

import org.springframework.stereotype.Service;

/**
 * Auth eService
 */
@Service
public class AuthService {
  public String pageName(String name) {
    return name.toUpperCase();
  }
}
