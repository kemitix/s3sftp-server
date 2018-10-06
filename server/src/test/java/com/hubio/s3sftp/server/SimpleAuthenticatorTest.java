package com.hubio.s3sftp.server;

import org.apache.sshd.server.session.ServerSession;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class SimpleAuthenticatorTest implements WithAssertions {

    private final SimpleAuthenticator subject = new SimpleAuthenticator();
    private final HomeDirExistsChecker homeDirExistsChecker = mock(HomeDirExistsChecker.class);
    private final ServerSession session = mock(ServerSession.class);

    private final String username = "username";
    private final String password = "password";

    private boolean authenticationResult;

    @BeforeEach
    void setUp() {
        subject.setHomeDirExistsChecker(homeDirExistsChecker);
        givenUser(username, password);
        givenHomeDirExists(username, session);
    }

    private void givenHomeDirExists(final String username, final ServerSession session) {
        given(homeDirExistsChecker.check(username, session)).willReturn(true);
    }

    private void givenHomeDirDoesNotExist(final String username, final ServerSession session) {
        given(homeDirExistsChecker.check(username, session)).willReturn(false);
    }

    private void givenUser(final String username, final String password) {
        subject.addUser(username, password);
    }

    private void authenticate(final String username, final String password, final ServerSession session) {
        authenticationResult = subject.authenticate(username, password, session);
    }

    @Nested
    class InvalidAuthentication {

        @AfterEach
        void thenAuthenticationShouldFail() {
            assertThat(authenticationResult).isFalse();
        }

        @Test
        void givenInvalidUser() {
            authenticate("invalid", password, session);
        }

        @Test
        void givenInvalidPassword() {
            authenticate(username, "wrong", session);
        }

        @Test
        void givenMissingHome() {
            //given
            givenHomeDirDoesNotExist(username, session);
            //when
            authenticate(username, password, session);
        }

    }

    @Nested
    class ValidAuthentication{

        @AfterEach
        void thenAuthenticationShouldSucceed() {
            assertThat(authenticationResult).isTrue();
        }

        @Test
        void givenValidUsernameAndPasswordAndHomeExists() {
            authenticate(username, password, session);
        }

    }

    @Test
    void givenMissingHomeCheckerThenThrowException() {
        //given
        subject.setHomeDirExistsChecker(null);
        //then
        assertThatNullPointerException()
                .isThrownBy(() -> authenticate(username, password, session))
                .withMessage("No HomeDirExistsChecker set");
    }

}
