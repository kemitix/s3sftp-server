package com.hubio.s3sftp.server;

import org.apache.sshd.server.session.ServerSession;
import org.assertj.core.api.WithAssertions;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@EnableRuleMigrationSupport
class DefaultHomeDirExistsCheckerTest implements WithAssertions {

    private final JailedSftpSubsystemFactory sftpSubsystemFactory = mock(JailedSftpSubsystemFactory.class);
    private final S3FileSystemFactory s3FilesystemFactory = mock(S3FileSystemFactory.class);
    private final ServerSession session = mock(ServerSession.class);
    private final JailedSftpSubsystem sftpSubsystem = mock(JailedSftpSubsystem.class);
    private final String username = "username";

    private final DefaultHomeDirExistsChecker subject =
            new DefaultHomeDirExistsChecker(sftpSubsystemFactory, s3FilesystemFactory);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @BeforeEach
    void setUp() {
        given(sftpSubsystemFactory.create()).willReturn(sftpSubsystem);
    }

    @Test
    void shouldFindHomeDir() throws Exception {
        //given
        final Path mockHome = folder.newFolder().toPath();
        given(sftpSubsystem.resolveFile(any())).willReturn(mockHome);
        //when
        final boolean result = subject.check(username, session);
        //then
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotFindHomeDir() {
        //given
        given(sftpSubsystem.resolveFile(any())).willReturn(Paths.get("/garbage"));
        //when
        final boolean result = subject.check(username, session);
        //then
        assertThat(result).isFalse();
    }

    @Test
    void shouldHandleIOException() throws Exception {
        //given
        doThrow(IOException.class).when(s3FilesystemFactory)
                                  .createFileSystem(session);
        //then
        assertThatCode(() ->  subject.check(username, session))
                .doesNotThrowAnyException();
    }
}
