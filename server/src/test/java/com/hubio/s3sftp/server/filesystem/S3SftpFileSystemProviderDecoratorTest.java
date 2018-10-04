package com.hubio.s3sftp.server.filesystem;

import com.amazonaws.services.s3.AmazonS3;
import com.upplication.s3fs.AmazonS3Factory;
import com.upplication.s3fs.S3FileSystem;
import com.upplication.s3fs.S3FileSystemProvider;
import net.kemitix.mon.result.Result;
import org.apache.sshd.common.session.Session;
import org.assertj.core.api.WithAssertions;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

public class S3SftpFileSystemProviderDecoratorTest implements WithAssertions {

    private final S3SftpFileSystemProvider provider = mock(S3SftpFileSystemProvider.class);
    private final S3SftpFileSystemProviderDecorator decorator = new S3SftpFileSystemProviderDecorator(provider);

    @Test
    public void whenSetAmazonS3ThenDelegate() {
        //given
        final AmazonS3 amazonS3 = mock(AmazonS3.class);
        //when
        decorator.setAmazonS3(amazonS3);
        //then
        then(provider).should().setAmazonS3(amazonS3);
    }

    @Test
    public void whenGetS3FileSystemProviderThenDelegate() {
        //given
        final S3FileSystemProvider expected = mock(S3FileSystemProvider.class);
        given(provider.getS3FileSystemProvider()).willReturn(expected);
        //when
        final S3FileSystemProvider result = decorator.getS3FileSystemProvider();
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    public void whenGetAmazonS3FactoryThenDelegate() {
        //given
        final AmazonS3Factory expected = mock(AmazonS3Factory.class);
        final Properties properties = new Properties();
        given(provider.getAmazonS3Factory(properties)).willReturn(expected);
        //when
        final AmazonS3Factory result = decorator.getAmazonS3Factory(properties);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    public void whenGetAllFileSystemThenDelegate() {
        //given
        final List<S3FileSystem> expected = Collections.emptyList();
        given(provider.getAllFileSystems()).willReturn(expected);
        //when
        final List<S3FileSystem> result = decorator.getAllFileSystems();
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    public void whenGetSessionThenDelegate() {
        //given
        final Session expected = mock(Session.class);
        given(provider.getSession()).willReturn(expected);
        //when
        final Session result = decorator.getSession();
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    public void whenNewFileSystemThenDelegate() throws Throwable {
        //given
        final S3FileSystem expected = mock(S3FileSystem.class);
        final URI uri = URI.create("http://localhost");
        final Properties properties = new Properties();
        given(provider.newFileSystem(uri, properties)).willReturn(Result.ok(expected));
        //when
        final Result<S3FileSystem> result = decorator.newFileSystem(uri, properties);
        //then
        assertThat(result.orElseThrow()).isSameAs(expected);
    }
}