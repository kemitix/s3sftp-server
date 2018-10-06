package com.hubio.s3sftp.server;

import lombok.val;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.session.ServerSession;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class PasswordAuthenticationProviderTest implements WithAssertions {

    private final MyPasswordAuthenticationProvider subject = new MyPasswordAuthenticationProvider();

    @Test
    void authenticatePassword() {
        //given
        val username = "username";
        val password = "password";
        val session = mock(ServerSession.class);
        //when - false
        subject.authenticationResponse = false;
        final boolean resultFalse = subject.authenticate(username, password, session);
        //when - true
        subject.authenticationResponse = true;
        final boolean resultTrue = subject.authenticate(username, password, session);
        //then
        assertThat(resultFalse).isFalse();
        assertThat(resultTrue).isTrue();
    }

    private static class MyPasswordAuthenticationProvider implements PasswordAuthenticationProvider {

        private final HomeDirExistsChecker homeDirExistsChecker = mock(HomeDirExistsChecker.class);

        private boolean authenticationResponse;

        @Override
        public boolean authenticatePassword(
                final String username, final String password, final SftpSession session
        ) throws PasswordChangeRequiredException {
            return authenticationResponse;
        }

        @Override
        public HomeDirExistsChecker getHomeDirExistsChecker() {
            given(homeDirExistsChecker.check(any(), any())).willReturn(authenticationResponse);
            return homeDirExistsChecker;
        }

        @Override
        public void setHomeDirExistsChecker(final HomeDirExistsChecker homeDirExistsChecker) {

        }
    }
}
