package org.group1.coffeeshopapi.bakong;

/**
 * Holds the Bakong access token actually in use. NBC issues one against a registered email and
 * it expires after roughly 90 days, so the configured {@code BAKONG_TOKEN} is only ever the
 * starting value — {@link #renew()} replaces it in memory when the API rejects it.
 */
public interface BakongTokenService {

    /** The token to send on the next call: the renewed one if there is one, else the configured one. */
    String currentToken();

    /**
     * Asks NBC for a fresh token for the configured email and keeps it for subsequent calls.
     *
     * @return the new token, or {@code null} when no email is configured or NBC refused —
     *         in which case the caller must report a configuration problem rather than retry.
     */
    String renew();
}
