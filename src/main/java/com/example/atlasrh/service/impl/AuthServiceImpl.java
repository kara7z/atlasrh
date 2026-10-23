package com.example.atlasrh.service.impl;

import org.springframework.stereotype.Service;

import com.example.atlasrh.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService {
  @Override
  public String pageName(String name) {
    return name.toUpperCase();
  }
}
