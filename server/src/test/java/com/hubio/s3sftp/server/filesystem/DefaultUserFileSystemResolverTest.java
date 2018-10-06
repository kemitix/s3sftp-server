package com.hubio.s3sftp.server.filesystem;

import com.upplication.s3fs.S3FileSystem;
import lombok.val;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.mock;

class DefaultUserFileSystemResolverTest implements WithAssertions {

    private final S3FileSystem fileSystem = mock(S3FileSystem.class);
    private final DefaultUserFileSystemResolver subject = new DefaultUserFileSystemResolver();

    @Test
    void resolveKnownUser() {
        //given
        val username = "username";
        subject.put(username, fileSystem);
        //when
        final Optional<S3FileSystem> result = subject.resolve(username);
        //then
        assertThat(result).contains(fileSystem);
    }

    @Test
    void resolveUnknownUser() {
        //given
        val username = "username";
        //when
        final Optional<S3FileSystem> result = subject.resolve(username);
        //then
        assertThat(result).isEmpty();
    }
}
