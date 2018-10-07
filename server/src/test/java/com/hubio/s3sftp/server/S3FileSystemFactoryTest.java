package com.hubio.s3sftp.server;

import com.hubio.s3sftp.server.filesystem.S3SftpFileSystemProvider;
import com.hubio.s3sftp.server.filesystem.S3SftpFileSystemProviderFactory;
import com.hubio.s3sftp.server.filesystem.UserFileSystemResolver;
import com.upplication.s3fs.S3FileSystem;
import lombok.val;
import net.kemitix.mon.maybe.Maybe;
import org.apache.sshd.server.session.ServerSession;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.FileSystem;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class S3FileSystemFactoryTest implements WithAssertions {

    private final ServerSession serverSession = mock(ServerSession.class);
    private final S3FileSystem s3FileSystem = mock(S3FileSystem.class);
    private final S3SftpFileSystemProviderFactory fileSystemProviderFactory =
            mock(S3SftpFileSystemProviderFactory.class);
    private final S3SftpFileSystemProvider s3SftpFileSystemProvider = mock(S3SftpFileSystemProvider.class);
    private final UserFileSystemResolver userFileSystemResolver = mock(UserFileSystemResolver.class);

    private S3FileSystemFactory subject =
            new S3FileSystemFactory(session -> "bucket", session -> "home", session -> "", URI.create("uri"),
                    fileSystemProviderFactory, userFileSystemResolver
    );

    @Test
    void shouldCreateFileSystem() throws Exception {
        //given
        val username = "newUser";
        given(serverSession.getUsername()).willReturn(username);
        given(userFileSystemResolver.resolve(username)).willReturn(Maybe.nothing());
        given(fileSystemProviderFactory.createWith(any(), eq(serverSession))).willReturn(s3SftpFileSystemProvider);
        given(s3SftpFileSystemProvider.getFileSystem(any(), any())).willReturn(s3FileSystem);
        given(s3SftpFileSystemProvider.getSession()).willReturn(serverSession);
        //when
        final FileSystem result = subject.createFileSystem(serverSession);
        //then
        assertThat(result).isSameAs(s3FileSystem);
    }
}
