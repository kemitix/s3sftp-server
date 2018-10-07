package com.hubio.s3sftp.server.filesystem;

import com.upplication.s3fs.S3FileSystem;
import lombok.val;
import net.kemitix.mon.maybe.Maybe;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class DefaultUserFileSystemResolverTest implements WithAssertions {

    private final DefaultUserFileSystemResolver subject = new DefaultUserFileSystemResolver();

    @Test
    void resolveKnownUser() {
        //given
        val username = "username";
        val fileSystem = mock(S3FileSystem.class);
        subject.put(username, fileSystem);
        //when
        final Maybe<S3FileSystem> result = subject.resolve(username);
        //then
        result.match(
                s3FileSystem -> assertThat(s3FileSystem).isSameAs(fileSystem),
                () -> fail("filesystem not resolved")
        );
    }

    @Test
    void resolveUnknownUser() {
        //given
        val username = "username";
        //when
        final Maybe<S3FileSystem> result = subject.resolve(username);
        //then
        assertThat(result.isNothing()).isTrue();
    }
}
