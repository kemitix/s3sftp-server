package com.hubio.s3sftp.server;

import com.hubio.s3sftp.server.filesystem.UserFileSystemResolver;
import com.upplication.s3fs.S3FileSystem;
import com.upplication.s3fs.S3Path;
import de.bechte.junit.runners.context.HierarchicalContextRunner;
import lombok.val;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.random.Random;
import org.apache.sshd.server.ServerFactoryManager;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.*;
import org.assertj.core.api.WithAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JailedSftpSubsystem}.
 *
 * @author Paul Campbell (paul.campbell@hubio.com)
 */
@RunWith(HierarchicalContextRunner.class)
public class JailedSftpSubsystemTest implements WithAssertions {

    private final ExecutorService executorService = mock(ExecutorService.class);
    private final SessionBucket sessionBucket = mock(SessionBucket.class);
    private final SessionHome sessionHome = mock(SessionHome.class);
    private final SessionJail sessionJail = mock(SessionJail.class);
    private final UserFileSystemResolver userFileSystemResolver = mock(UserFileSystemResolver.class);
    private final ServerSession serverSession = mock(ServerSession.class);
    private final ServerFactoryManager serverFactoryManager = mock(ServerFactoryManager.class);
    private final Random randomizer = mock(Random.class);
    private final S3FileSystem s3FileSystem = mock(S3FileSystem.class);
    private final Factory<Random> randomFactory = mock(Factory.class);
    private final SftpFileSystemAccessor accessor = mock(SftpFileSystemAccessor.class);
    private final SftpErrorStatusDataHandler errorStatusDataHandler = mock(SftpErrorStatusDataHandler.class);

    private JailedSftpSubsystem sftpSubsystem =
            new JailedSftpSubsystem(
                    executorService, false, UnsupportedAttributePolicy.Warn, sessionBucket,
                    sessionHome, sessionJail, userFileSystemResolver, accessor, errorStatusDataHandler);

    private SftpSession sftpSession;

    @Test
    public void shouldRemoveOnlyPermissionsFromAttributes() throws Exception {
        //given
        val path = Paths.get(".");
        val attributes = new HashMap<String, String>();
        attributes.put("basic", "keep me");
        attributes.put("permissions", "drop me");
        val modifiedAttributes = new HashMap<String, Object>();
        sftpSubsystem.addSftpEventListener(new AbstractSftpEventListenerAdapter() {
            @Override
            public void modifyingAttributes(final ServerSession session, final Path path, final Map<String, ?> attrs) {
                modifiedAttributes.putAll(attrs);
            }
        });
        //when
        sftpSubsystem.doSetAttributes(path, attributes);
        //then
        assertThat(modifiedAttributes).containsOnlyKeys("basic")
                                      .containsValues("keep me")
                                      .doesNotContainValue("drop me");
    }

    public class ResolveFile {

        private String username;

        private String bucket;

        @Before
        public void setUp() {
            username = "bob";
            bucket = "bucket";
            given(serverSession.getFactoryManager()).willReturn(serverFactoryManager);
            given(serverFactoryManager.getRandomFactory()).willReturn(randomFactory);
            given(randomFactory.create()).willReturn(randomizer);
            given(serverSession.getUsername()).willReturn(username);
            given(serverSession.getIntProperty(SftpSubsystem.FILE_HANDLE_SIZE, SftpSubsystem.DEFAULT_FILE_HANDLE_SIZE))
                    .willReturn(SftpSubsystem.DEFAULT_FILE_HANDLE_SIZE);
            given(serverSession.getIntProperty(SftpSubsystem.MAX_FILE_HANDLE_RAND_ROUNDS, SftpSubsystem.DEFAULT_FILE_HANDLE_ROUNDS))
                    .willReturn(SftpSubsystem.DEFAULT_FILE_HANDLE_ROUNDS);
            sftpSubsystem.setSession(serverSession);
            sftpSession = SftpSession.of(serverSession);
            given(sessionBucket.getBucket(anyObject())).willReturn(bucket);
            given(sessionJail.getJail(anyObject())).willReturn("");
            given(sessionHome.getHomePath(anyObject())).willReturn("");
            given(userFileSystemResolver.resolve(username)).willReturn(Optional.of(s3FileSystem));
            given(s3FileSystem.getSeparator()).willReturn("/");
        }

        public class Root {

            @Before
            public void setUp() {
                given(sessionHome.getHomePath(sftpSession)).willReturn("");
            }

            @Test
            public void resolveFileRoot() {
                //when
                val path = sftpSubsystem.resolveFile(".");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/");
            }

            @Test
            public void resolveFileDirectory() {
                //when
                val path = sftpSubsystem.resolveFile("/subdir");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/subdir");
            }

            @Test
            public void resolveFileFile() {
                //when
                val path = sftpSubsystem.resolveFile("/file.txt");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/file.txt");
            }

            @Test
            public void resolverFilePreresolved() {
                //when
                val path = sftpSubsystem.resolveFile("/bucket/file.txt");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/file.txt");
            }

            @Test
            public void resolveFileShouldRemoveTrailingPeriod() {
                //when
                val path = sftpSubsystem.resolveFile("dir/.");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/dir");
            }

            @Test
            public void resolveFileShouldResolveParentDir() {
                //when
                val path = sftpSubsystem.resolveFile("dir/subdir/..");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/dir/subdir/..");
            }
        }

        public class Home {

            @Before
            public void setUp() {
                given(sessionHome.getHomePath(anyObject())).willReturn(username);
            }

            @Test
            public void resolveUserDir() {
                //when
                val path = sftpSubsystem.resolveFile("");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/bob");
            }

            @Test
            public void resolveWithinUserDir() {
                //when
                val path = sftpSubsystem.resolveFile("file.txt");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/bob/file.txt");
            }

            @Test
            public void resolveFileRoot() {
                //when
                val path = sftpSubsystem.resolveFile(".");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/bob");
            }

            @Test
            public void resolveFileDirectory() {
                //when
                val path = sftpSubsystem.resolveFile("/subdir");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/bob/subdir");
            }

            @Test
            public void resolveFileFile() {
                //when
                val path = sftpSubsystem.resolveFile("/file.txt");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/bob/file.txt");
            }

            @Test
            public void resolverFilePreresolved() {
                //when
                val path = sftpSubsystem.resolveFile("/bucket/bob/file.txt");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bucket/bob/file.txt");
            }

            @Test
            public void resolveFileShouldRemoveTrailingPeriod() {
                //given
                val path = "dir/.";
                //when
                val result = sftpSubsystem.resolveFile(path);
                //then
                assertThat(S3PathUtil.dirPath(((S3Path) result))).isEqualTo("/bucket/bob/dir");
            }

            @Test
            public void resolveFileShouldResolveParentDir() {
                //given
                val path = "dir/subdir/..";
                //when
                val result = sftpSubsystem.resolveFile(path);
                //then
                assertThat(S3PathUtil.dirPath(((S3Path) result))).isEqualTo("/bucket/bob/dir/subdir/..");
            }
        }

        public class Jailed {

            @Before
            public void setUp() {
                given(sessionHome.getHomePath(anyObject())).willReturn(String.format("users/%s", username));
                given(sessionJail.getJail(anyObject())).willReturn("users");
                // i.e. user should only see their own username as the visible path
            }

            @Test
            public void dot() {
                //when
                val path = sftpSubsystem.resolveFile(".");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bob/");
            }

            @Test
            public void home() {
                //when
                val path = sftpSubsystem.resolveFile("/bob");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bob/");
            }

            @Test
            public void file() {
                //when
                val path = sftpSubsystem.resolveFile("/bob/file.txt");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bob/file.txt");
            }

            @Test
            public void fullJailedPath() {
                //when
                val path = sftpSubsystem.resolveFile("/bucket/users/bob/file.txt");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bob/file.txt");
            }

            @Test
            public void removeTrailingPeriod() {
                //when
                val path = sftpSubsystem.resolveFile("/bob/dir/.");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bob/dir");
            }

            @Test
            public void acceptNavToParentDir() {
                //when
                val path = sftpSubsystem.resolveFile("/bob/dir/subdir/..");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bob/dir/subdir/..");
            }

            @Test
            public void homeIsOutsideJail() {
                //given
                given(sessionJail.getJail(anyObject())).willReturn("jail");
                given(sessionHome.getHomePath(anyObject())).willReturn("home");
                //then
                assertThatExceptionOfType(SftpServerJailMappingException.class)
                        .isThrownBy(() -> sftpSubsystem.resolveFile("."))
                        .withMessage("User directory is outside jailed path: jail: home");
            }

            @Test
            public void tryToExitJail() {
                //given
                val path = sftpSubsystem.resolveFile("/");
                //then
                assertThat(S3PathUtil.dirPath((S3Path)path)).isEqualTo("/bob/");
            }
        }

        @Test
        public void filesystemForUserIsMissing() {
            //given
            given(userFileSystemResolver.resolve(username)).willReturn(Optional.empty());
            //then
            assertThatExceptionOfType(RuntimeException.class)
                    .isThrownBy(() -> sftpSubsystem.resolveFile("path"))
                    .withMessage("Error finding filesystem.");
        }
    }
}
