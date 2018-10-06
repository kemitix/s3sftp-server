package com.hubio.s3sftp.server.filesystem;

import com.amazonaws.services.s3.AmazonS3;
import com.hubio.s3sftp.server.S3PathEnhancer;
import com.upplication.s3fs.S3FileSystem;
import com.upplication.s3fs.S3Path;
import lombok.val;
import org.apache.sshd.client.subsystem.sftp.SftpDirectoryStream;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class PathEnhancingS3SftpFileSystemProviderTest implements WithAssertions {

    private final S3Path rawPath = mock(S3Path.class);
    private final S3Path enhancedPath = mock(S3Path.class);
    private final DelegatableS3FileSystemProvider delegatedProvider = mock(DelegatableS3FileSystemProvider.class);
    private final S3PathEnhancer s3PathEnhancer = mock(S3PathEnhancer.class);
    private final FileSystem fileSystem = mock(FileSystem.class);

    private final PathEnhancingS3SftpFileSystemProvider subject =
            new PathEnhancingS3SftpFileSystemProvider(delegatedProvider, s3PathEnhancer);

    @BeforeEach
    void setUp() {
        given(s3PathEnhancer.apply(rawPath)).willReturn(enhancedPath);
    }

    @Test
    void newFileChannel() throws Exception {
        //given
        final Set<OpenOption> options = new HashSet<>();
        val attrs = new FileAttribute[]{};
        val expected = mock(FileChannel.class);
        given(delegatedProvider.newFileChannel(enhancedPath, options, attrs)).willReturn(expected);
        //when
        final FileChannel result = subject.newFileChannel(rawPath, options, attrs);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void readAttributesByType() throws Exception {
        //given
        val type = BasicFileAttributes.class;
        val options = LinkOption.NOFOLLOW_LINKS;
        val expected = mock(BasicFileAttributes.class);
        given(delegatedProvider.readAttributes(enhancedPath, type, options)).willReturn(expected);
        //when
        final BasicFileAttributes result = subject.readAttributes(rawPath, type, options);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void readAttributesByName() throws Exception {
        //given
        val attributes = "attributes";
        val options = LinkOption.NOFOLLOW_LINKS;
        final HashMap<String, Object> expected = new HashMap<>();
        given(delegatedProvider.readAttributes(enhancedPath, attributes, options)).willReturn(expected);
        //when
        final Map<String, Object> result = subject.readAttributes(rawPath, attributes, options);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void newFileSystemByPath() throws Exception {
        //given
        final Map<String, Object> env = new HashMap<>();
        val expected = mock(FileSystem.class);
        given(delegatedProvider.newFileSystem(enhancedPath, env)).willReturn(expected);
        //when
        final FileSystem result = subject.newFileSystem(rawPath, env);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void newDirectoryStream() throws Exception {
        //given
        final DirectoryStream.Filter<Path> filter = entry -> false;
        val expected = mock(SftpDirectoryStream.class);
        given(delegatedProvider.newDirectoryStream(enhancedPath, filter)).willReturn(expected);
        //when
        final DirectoryStream<Path> result = subject.newDirectoryStream(rawPath, filter);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void newInputStream() throws Exception {
        //given
        val options = mock(OpenOption.class);
        val expected = mock(InputStream.class);
        given(delegatedProvider.newInputStream(enhancedPath, options)).willReturn(expected);
        //when
        final InputStream result = subject.newInputStream(rawPath, options);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void newByteChannel() throws Exception {
        //given
        final Set<OpenOption> options = new HashSet<>();
        val attrs = new FileAttribute[]{};
        val expected = mock(SeekableByteChannel.class);
        given(delegatedProvider.newByteChannel(enhancedPath, options, attrs)).willReturn(expected);
        //when
        final SeekableByteChannel result = subject.newByteChannel(rawPath, options, attrs);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void createDirectory() throws Exception {
        //given
        val attrs = new FileAttribute[]{};
        //when
        subject.createDirectory(rawPath, attrs);
        //then
        then(delegatedProvider).should()
                               .createDirectory(enhancedPath, attrs);
    }

    @Test
    void delete() throws Exception {
        //given
        //when
        subject.delete(rawPath);
        //then
        then(delegatedProvider).should()
                               .delete(enhancedPath);
    }

    @Test
    void copy() throws Exception {
        //given
        val options = mock(CopyOption.class);
        val target = mock(S3Path.class);
        val enhancedTarget = mock(S3Path.class);
        given(s3PathEnhancer.apply(target)).willReturn(enhancedTarget);
        //when
        subject.copy(rawPath, target, options);
        //then
        then(delegatedProvider).should()
                               .copy(enhancedPath, enhancedTarget, options);
    }

    @Test
    void move() throws Exception {
        //given
        val target = mock(S3Path.class);
        val enhancedTarget = mock(S3Path.class);
        given(s3PathEnhancer.apply(target)).willReturn(enhancedTarget);
        //when
        subject.move(rawPath, target);
        //then
        then(delegatedProvider).should()
                               .move(enhancedPath, enhancedTarget);
    }

    @Test
    void isSameFile() throws Exception {
        //given
        val path2 = mock(S3Path.class);
        val enhancedPath2 = mock(S3Path.class);
        given(s3PathEnhancer.apply(path2)).willReturn(enhancedPath2);
        given(delegatedProvider.isSameFile(enhancedPath, enhancedPath2)).willReturn(true)
                                                                        .willReturn(false);
        //when
        final boolean result1 = subject.isSameFile(rawPath, path2);
        final boolean result2 = subject.isSameFile(rawPath, path2);
        //then
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
    }

    @Test
    void isHidden() throws Exception {
        //given
        given(delegatedProvider.isHidden(enhancedPath)).willReturn(true)
                                                       .willReturn(false);
        //when
        final boolean result1 = subject.isHidden(rawPath);
        final boolean result2 = subject.isHidden(rawPath);
        //then
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
    }

    @Test
    void checkAccess() throws Exception {
        //given
        val modes = AccessMode.READ;
        //when
        subject.checkAccess(rawPath, modes);
        //then
        then(delegatedProvider).should()
                               .checkAccess(enhancedPath, modes);
    }

    @Test
    void newOutputStream() throws Exception {
        //given
        val options = mock(OpenOption.class);
        val expected = mock(OutputStream.class);
        given(delegatedProvider.newOutputStream(enhancedPath, options)).willReturn(expected);
        //when
        final OutputStream result = subject.newOutputStream(rawPath, options);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void newAsynchronousFileChannel() throws Exception {
        //given
        final Set<OpenOption> options = new HashSet<>();
        val executor = mock(ExecutorService.class);
        val attrs = new FileAttribute[]{};
        val expected = mock(AsynchronousFileChannel.class);
        given(delegatedProvider.newAsynchronousFileChannel(enhancedPath, options, executor, attrs)).willReturn(
                expected);
        //when
        final AsynchronousFileChannel result = subject.newAsynchronousFileChannel(rawPath, options, executor, attrs);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void createSymbolicLink() throws Exception {
        //given
        val target = mock(S3Path.class);
        val enhancedTarget = mock(S3Path.class);
        given(s3PathEnhancer.apply(target)).willReturn(enhancedTarget);
        val attrs = new FileAttribute[]{};
        //when
        subject.createSymbolicLink(rawPath, target, attrs);
        //then
        then(delegatedProvider).should()
                               .createSymbolicLink(enhancedPath, enhancedTarget, attrs);
    }

    @Test
    void createLink() throws Exception {
        //given
        val existing = mock(S3Path.class);
        val enhancedExisting = mock(S3Path.class);
        given(s3PathEnhancer.apply(existing)).willReturn(enhancedExisting);
        //when
        subject.createLink(rawPath, existing);
        //then
        then(delegatedProvider).should()
                               .createLink(enhancedPath, enhancedExisting);
    }

    @Test
    void deleteIfExists() throws Exception {
        //given
        given(delegatedProvider.deleteIfExists(enhancedPath)).willReturn(true)
                                                             .willReturn(false);
        //when
        final boolean result1 = subject.deleteIfExists(rawPath);
        final boolean result2 = subject.deleteIfExists(rawPath);
        //then
        assertThat(result1).isTrue();
        assertThat(result2).isFalse();
    }

    @Test
    void readSymbolicLink() throws Exception {
        //given
        val expected = mock(Path.class);
        given(delegatedProvider.readSymbolicLink(enhancedPath)).willReturn(expected);
        //when
        final Path result = subject.readSymbolicLink(rawPath);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void setAttribute() throws Exception {
        //given
        val attribute = "attribute";
        val value = new Object();
        val options = LinkOption.NOFOLLOW_LINKS;
        //when
        subject.setAttribute(rawPath, attribute, value, options);
        //then
        then(delegatedProvider).should()
                               .setAttribute(enhancedPath, attribute, value, options);
    }

    @Test
    void newFileSystem() throws Exception {
        //given
        val path = mock(S3Path.class);
        final Map<String, String> env = new HashMap<>();
        val expected = mock(FileSystem.class);
        given(delegatedProvider.newFileSystem(path, env)).willReturn(expected);
        given(s3PathEnhancer.apply(path)).willReturn(path);
        //when
        final FileSystem result = subject.newFileSystem(path, env);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void getFileSystemWithURIAndMap() {
        //given
        val uri = URI.create("http://localhost");
        final Map<String, String> env = new HashMap<>();
        val expected = mock(S3FileSystem.class);
        given(delegatedProvider.getFileSystem(uri, env)).willReturn(expected);
        //when
        final FileSystem result = subject.getFileSystem(uri, env);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void getPath() {
        //given
        val uri = URI.create("http://localhost");
        val expected = mock(Path.class);
        given(delegatedProvider.getPath(uri)).willReturn(expected);
        //when
        final Path result = subject.getPath(uri);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void getFileStore() throws Exception {
        //given
        val expected = mock(FileStore.class);
        given(delegatedProvider.getFileStore(enhancedPath)).willReturn(expected);
        //when
        final FileStore result = subject.getFileStore(rawPath);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void getFileAttributeView() {
        //given
        val type = FileAttributeView.class;
        val options = LinkOption.NOFOLLOW_LINKS;
        val expected = mock(FileAttributeView.class);
        given(delegatedProvider.getFileAttributeView(enhancedPath, type, options)).willReturn(expected);
        //when
        final FileAttributeView result = subject.getFileAttributeView(rawPath, type, options);
        //then
        assertThat(result).isSameAs(expected);
    }

    @Test
    void getFileSystemMap() {
        //given
        val uri = URI.create("http://localhost");
        final Map<String, String> props = new HashMap<>();
        given(delegatedProvider.getFileSystem(uri, props)).willReturn(fileSystem);
        //when
        final FileSystem result = subject.getFileSystem(uri, props);
        //then
        assertThat(result).isSameAs(fileSystem);
    }

    @Test
    void getFileSystemKeyProps() {
        //given
        val uri = URI.create("http://localhost");
        val props = new Properties();
        val key = "key";
        given(delegatedProvider.getFileSystemKey(uri, props)).willReturn(key);
        //when
        final String result = subject.getFileSystemKey(uri, props);
        //then
        assertThat(result).isSameAs(key);
    }

    @Test
    void overloadProperties() {
        //given
        val props = new Properties();
        final Map<String, String> env = new HashMap<>();
        //when
        subject.overloadProperties(props, env);
        //then
        then(delegatedProvider).should()
                               .overloadProperties(props, env);
    }

    @Test
    void overloadPropertiesWithEnv() {
        val props = new Properties();
        final Map<String, String> env = new HashMap<>();
        val key = "key";
        //given - false
        given(delegatedProvider.overloadPropertiesWithEnv(props, env, key)).willReturn(false);
        //when
        final boolean resultFalse = subject.overloadPropertiesWithEnv(props, env, key);
        //then
        assertThat(resultFalse).isFalse();
        //given - true
        given(delegatedProvider.overloadPropertiesWithEnv(props, env, key)).willReturn(true);
        //when
        final boolean resultTrue = subject.overloadPropertiesWithEnv(props, env, key);
        //then
        assertThat(resultTrue).isTrue();
    }

    @Test
    void getAmazonS3() {
        //given
        val uri = URI.create("http://localhost");
        val props = new Properties();
        val amazonS3 = mock(AmazonS3.class);
        given(delegatedProvider.getAmazonS3(uri, props)).willReturn(amazonS3);
        //when
        final AmazonS3 result = subject.getAmazonS3(uri, props);
        //then
        assertThat(result).isSameAs(amazonS3);
    }

}
