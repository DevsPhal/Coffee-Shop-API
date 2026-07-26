package org.group1.coffeeshopapi.service;

import org.group1.coffeeshopapi.dto.request.LoginRequest;
import org.group1.coffeeshopapi.dto.request.RegisterRequest;
import org.group1.coffeeshopapi.dto.response.AuthenticationResponse;

public interface AuthenticationService {

    AuthenticationResponse register(RegisterRequest registerRequest);

    AuthenticationResponse login(LoginRequest loginRequest);
}