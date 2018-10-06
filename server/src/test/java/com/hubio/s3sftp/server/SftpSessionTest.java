package com.hubio.s3sftp.server;

import lombok.val;
import org.apache.sshd.server.session.ServerSession;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import java.net.SocketAddress;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class SftpSessionTest implements WithAssertions {

    @Test
    void shouldGetWrapperSession() {
        //given
        val serverSession = mock(ServerSession.class);
        val subject = SftpSession.of(serverSession);
        //when
        final ServerSession result = subject.getServerSession();
        //then
        assertThat(result).isSameAs(serverSession);
    }

    @Test
    void shouldGetClientAddress() {
        //given
        val clientAddress = mock(SocketAddress.class);
        val serverSession = mock(ServerSession.class);
        given(serverSession.getClientAddress()).willReturn(clientAddress);
        final SftpSession subject = SftpSession.of(serverSession);
        //when
        final SocketAddress result = subject.getClientAddress();
        //then
        assertThat(result).isSameAs(clientAddress);
    }

    @Test
    void shouldGetUsername() {
        //given
        val username = "Username";
        val serverSession = mock(ServerSession.class);
        given(serverSession.getUsername()).willReturn(username);
        final SftpSession subject = SftpSession.of(serverSession);
        //when
        final String result = subject.getUsername();
        //then
        assertThat(result).isEqualTo(username);
    }
}
