package com.hubio.s3sftp.server;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

class AbstractAuthenticationProviderTest implements WithAssertions {

    private final AbstractAuthenticationProvider subject = new AbstractAuthenticationProvider() {
    };

    @Test
    void shouldReturnHomeDirExistsChecker() throws Exception {
        //given
        final HomeDirExistsChecker checker = (username, session) -> false;
        subject.setHomeDirExistsChecker(checker);
        //when
        final HomeDirExistsChecker result = subject.getHomeDirExistsChecker();
        //then
        assertThat(result).isSameAs(checker);
    }
}
