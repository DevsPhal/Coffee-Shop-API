package org.group1.coffeeshopapi.bakong;

// Holds the Bakong access token in use. It expires after about 90 days, so renew() replaces it
// in memory once the API starts rejecting it.
public interface BakongTokenService {

    // The token to send on the next call.
    String currentToken();

    // Asks NBC for a fresh token and keeps it for subsequent calls. Returns null if renewal failed.
    String renew();
}
