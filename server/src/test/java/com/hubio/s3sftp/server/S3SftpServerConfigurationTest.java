package com.hubio.s3sftp.server;

import lombok.val;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class S3SftpServerConfigurationTest implements WithAssertions {

    @Test
    void defaultSessionHomeIsBlank() {
        //given
        final S3SftpServerConfiguration configuration = defaultConfiguration();
        val session = mock(SftpSession.class);
        //when
        final String homePath = configuration.getSessionHome().getHomePath(session);
        //then
        assertThat(homePath).isBlank();
    }

    @Test
    void defaultSessionJailIsBlank() {
        //given
        final S3SftpServerConfiguration configuration = defaultConfiguration();
        val session = mock(SftpSession.class);
        //when
        final String jail = configuration.getSessionJail().getJail(session);
        //then
        assertThat(jail).isBlank();
    }

    private S3SftpServerConfiguration defaultConfiguration() {
        return S3SftpServerConfiguration.builder()
                .authenticationProvider(mock(AuthenticationProvider.class))
                .sessionBucket(mock(SessionBucket.class))
                .uri("")
                .build();
    }

}