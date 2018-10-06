package com.hubio.s3sftp.server;

import com.hubio.s3sftp.server.filesystem.UserFileSystemResolver;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.subsystem.sftp.SftpErrorStatusDataHandler;
import org.apache.sshd.server.subsystem.sftp.SftpEventListener;
import org.apache.sshd.server.subsystem.sftp.SftpEventListenerManager;
import org.apache.sshd.server.subsystem.sftp.SftpFileSystemAccessor;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class JailedSftpSubsystemFactoryTest implements WithAssertions {

    private final SftpEventListener listener = mock(SftpEventListener.class);
    private final SessionBucket sessionBucket = mock(SessionBucket.class);
    private final SessionHome sessionHome = mock(SessionHome.class);
    private final SessionJail sessionJail = mock(SessionJail.class);
    private final UserFileSystemResolver userFileSystemResolver = mock(UserFileSystemResolver.class);
    private final SftpFileSystemAccessor accessor = mock(SftpFileSystemAccessor.class);
    private final SftpErrorStatusDataHandler errorStatusDataHandler = mock(SftpErrorStatusDataHandler.class);

    private final JailedSftpSubsystemFactory sftpSubsystemFactory = new JailedSftpSubsystemFactory(
            sessionBucket, sessionHome, sessionJail, userFileSystemResolver, accessor, errorStatusDataHandler);

    @Test
    void create() {
        //given
        sftpSubsystemFactory.addSftpEventListener(listener);
        //when
        final Command sftpSubsystem = sftpSubsystemFactory.create();
        //then
        assertThat(sftpSubsystem).isInstanceOf(JailedSftpSubsystem.class);
        // that the listener was added to the command - true if matching listener was removed
        assertThat(((SftpEventListenerManager) sftpSubsystem).removeSftpEventListener(listener)).isTrue();
    }
}
