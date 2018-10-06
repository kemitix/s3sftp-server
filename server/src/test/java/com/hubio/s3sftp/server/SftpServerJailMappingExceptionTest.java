package com.hubio.s3sftp.server;

import lombok.val;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

class SftpServerJailMappingExceptionTest implements WithAssertions {

    @Test
    void shouldCreateException() {
        //given
        val jail = "jail";
        val home = "home";
        //when
        final SftpServerJailMappingException result = new SftpServerJailMappingException(jail, home);
        //then
        assertThat(result.getMessage()).isEqualTo("User directory is outside jailed path: jail: home");
        assertThat(result.getInput()).isEqualTo(home);
    }
}
