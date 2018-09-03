package com.hubio.s3sftp.server;

import com.hubio.s3sftp.server.filesystem.UserFileSystemResolver;
import lombok.val;
import org.apache.sshd.server.subsystem.sftp.SftpErrorStatusDataHandler;
import org.apache.sshd.server.subsystem.sftp.SftpEventListener;
import org.apache.sshd.server.subsystem.sftp.SftpEventListenerManager;
import org.apache.sshd.server.subsystem.sftp.SftpFileSystemAccessor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JailedSftpSubsystemFactory}.
 *
 * @author Paul Campbell (paul.campbell@hubio.com)
 */
public class JailedSftpSubsystemFactoryTest {

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
    public void create() {
        //given
        sftpSubsystemFactory.addSftpEventListener(listener);
        //when
        val sftpSubsystem = sftpSubsystemFactory.create();
        //then
        assertThat(sftpSubsystem).isInstanceOf(JailedSftpSubsystem.class);
        // that the listener was added to the command - true if matching listener was removed
        assertThat(((SftpEventListenerManager) sftpSubsystem).removeSftpEventListener(listener)).isTrue();
    }
}
