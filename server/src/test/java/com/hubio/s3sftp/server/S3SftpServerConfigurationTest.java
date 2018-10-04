package com.hubio.s3sftp.server;

import org.assertj.core.api.WithAssertions;
import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * @author Paul Campbell (pcampbell@kemitix.net)
 */

public class S3SftpServerConfigurationTest implements WithAssertions {

    @Test
    public void defaultSessionHomeIsBlank() {
        //given
        final S3SftpServerConfiguration configuration = defaultConfiguration();
        final SftpSession session = mock(SftpSession.class);
        //when
        final String homePath = configuration.getSessionHome().getHomePath(session);
        //then
        assertThat(homePath).isBlank();
    }

    @Test
    public void defaultSessionJailIsBlank() {
        //given
        final S3SftpServerConfiguration configuration = defaultConfiguration();
        final SftpSession session = mock(SftpSession.class);
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