package com.hubio.s3sftp.server;

import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import java.security.PublicKey;

import static org.mockito.Mockito.mock;

class PublicKeyAuthenticationProviderTest implements WithAssertions {

    private final PublicKey key = mock(PublicKey.class);
    private final ServerSession session = mock(ServerSession.class);

    private boolean expectedResponse;

    private final PublickeyAuthenticator publickeyAuthenticator =
            new PublickeyAuthenticator() {
                @Override
                public boolean authenticate(final String username, final PublicKey key, final ServerSession session) {
                    return expectedResponse;
                }
            };
    private final PublicKeyAuthenticationProvider subject= new PublicKeyAuthenticationProvider(publickeyAuthenticator);

    @Test
    void shouldDelegateToAuthenticator() {
        //given
        expectedResponse = false;
        //then
        assertThat(subject.authenticate("username", key, session)).isFalse();
        //given
        expectedResponse = true;
        //then
        assertThat(subject.authenticate("username", key, session)).isTrue();
    }
}
