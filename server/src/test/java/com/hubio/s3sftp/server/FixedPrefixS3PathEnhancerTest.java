package com.hubio.s3sftp.server;

import com.upplication.s3fs.S3FileSystem;
import com.upplication.s3fs.S3Path;
import lombok.val;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FixedPrefixS3PathEnhancer}.
 *
 * @author Paul Campbell (paul.campbell@hubio.com)
 */
public class FixedPrefixS3PathEnhancerTest {

    private String prefix = "/home";
    private S3FileSystem fileSystem = mock(S3FileSystem.class);
    private S3PathEnhancer subject = S3PathEnhancer.fixedPrefix(prefix);

    @Test
    public void shouldAlreadyStartWithPrefix() throws Exception {
        //given
        final S3Path path = new S3Path(fileSystem, prefix + "/user");
        //when
        final S3Path result = subject.apply(path);
        //then
        assertThat(S3PathUtil.dirPath(result)).isEqualTo("/home/user");
    }

    @Test
    public void shouldNeedPrefixAdded() throws Exception {
        //given
        val path = new S3Path(fileSystem, "/user");
        //when
        val result = subject.apply(path);
        //then
        assertThat(S3PathUtil.dirPath(result)).isEqualTo("/home/user/");
    }
}
