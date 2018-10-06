package com.hubio.s3sftp.server;

import com.upplication.s3fs.S3FileSystem;
import com.upplication.s3fs.S3Path;
import lombok.val;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class FixedPrefixS3PathEnhancerTest implements WithAssertions {

    private final String prefix = "/home";
    private final S3FileSystem fileSystem = mock(S3FileSystem.class);
    private final S3PathEnhancer subject = S3PathEnhancer.fixedPrefix(prefix);

    @Test
    void shouldAlreadyStartWithPrefix() {
        //given
        val path = new S3Path(fileSystem, prefix + "/user");
        //when
        final S3Path result = subject.apply(path);
        //then
        assertThat(S3PathUtil.dirPath(result)).isEqualTo("/home/user");
    }

    @Test
    void shouldNeedPrefixAdded() {
        //given
        val path = new S3Path(fileSystem, "/user");
        //when
        final S3Path result = subject.apply(path);
        //then
        assertThat(S3PathUtil.dirPath(result)).isEqualTo("/home/user/");
    }
}
