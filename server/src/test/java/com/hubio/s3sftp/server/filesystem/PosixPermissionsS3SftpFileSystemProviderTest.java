package com.hubio.s3sftp.server.filesystem;

import lombok.val;
import me.andrz.builder.map.MapBuilder;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class PosixPermissionsS3SftpFileSystemProviderTest implements WithAssertions {

    private final S3SftpFileSystemProvider delegate = mock(S3SftpFileSystemProvider.class);
    private final PosixPermissionsS3SftpFileSystemProvider subject =
            new PosixPermissionsS3SftpFileSystemProvider(delegate);

    @Test
    void readAttributes() throws Exception {
        //given
        val path = mock(Path.class);
        val attributes = "attributes";
        val options = LinkOption.NOFOLLOW_LINKS;
        final Map<String, Object> expected = emptyMap();
        given(delegate.readAttributes(path, attributes, options)).willReturn(expected);
        //when
        final Map<String, Object> result = subject.readAttributes(path, attributes, options);
        //then
        assertThat(result).as("delegated result")
                          .isSameAs(expected)
                          .as("permissions added")
                          .containsKey("permissions");
    }

    private Map<String, Object> emptyMap() {
        return new MapBuilder<String, Object>().build();
    }
}
