/**
 * The MIT License (MIT)
 * Copyright (c) 2017 Hubio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.hubio.s3sftp.server.filesystem;

import com.upplication.s3fs.S3FileSystem;
import com.upplication.s3fs.S3FileSystemProvider;
import com.upplication.s3fs.util.Cache;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.net.URI;
import java.nio.file.FileSystem;
import java.util.Map;
import java.util.Properties;

/**
 * Mapper to allow an {@link S3SftpFileSystemProvider} as an {@link S3FileSystemProvider}.
 *
 * @author Paul Campbell (paul.campbell@hubio.com)
 */
@Slf4j
@RequiredArgsConstructor
class InvertedS3FileSystemProvider extends S3FileSystemProvider {

    @Delegate
    private final S3SftpFileSystemProvider provider;

    @Override
    public String getScheme() {
        log.error("getScheme - unsupported {}", getSessionId());
        throw new UnsupportedOperationException("Inverted - getScheme");
    }

    @Override
    public FileSystem newFileSystem(final URI uri, final Map<String, ?> env) {
        log.debug("newFileSystem {}", getSessionId());
        val props = new Properties();
        env.forEach(props::put);
        return provider.newFileSystem(uri, props)
                .orElseThrowUnchecked();
    }

    @Override
    protected void validateUri(final URI uri) {
        log.error("validateUri - unsupported {}", getSessionId());
        throw new UnsupportedOperationException("Inverted - validateUri");
    }

    @Override
    public boolean overloadPropertiesWithSystemProps(final Properties props, final String key) {
        log.error("overloadPropertiesWithSystemProps - unsupported {}", getSessionId());
        throw new UnsupportedOperationException("Inverted - overloadPropertiesWithSystemProps");
    }

    @Override
    public boolean overloadPropertiesWithSystemEnv(final Properties props, final String key) {
        log.error("overloadPropertiesWithSystemEnv - unsupported {}", getSessionId());
        throw new UnsupportedOperationException("Inverted - overloadPropertiesWithSystemEnv");
    }

    @Override
    public String systemGetEnv(final String key) {
        log.error("systemGetEnv - unsupported {}", getSessionId());
        throw new UnsupportedOperationException("Inverted - systemGetEnv");
    }

    @Override
    public S3FileSystem getFileSystem(final URI uri) {
        log.error("getFileSystem - unsupported {}", getSessionId());
        throw new UnsupportedOperationException("Inverted - getFileSystem");
    }

    @Override
    public S3FileSystem createFileSystem(final URI uri, final Properties props) {
        log.error("createFileSystem - unsupported {}", getSessionId());
        throw new UnsupportedOperationException("Inverted - createFileSystem");
    }

    @Override
    public Properties loadAmazonProperties() {
        log.error("loadAmazonProperties - unsupported {}", getSessionId());
        throw new UnsupportedOperationException("Inverted - loadAmazonProperties");
    }

    @Override
    public void close(final S3FileSystem fileSystem) {
        log.info("close {}", getSessionId());
    }

    @Override
    public boolean isOpen(final S3FileSystem s3FileSystem) {
        log.error("isOpen - unsupported {}", getSessionId());
        throw new UnsupportedOperationException("Inverted - isOpen");
    }

    @Override
    public Cache getCache() {
        log.error("getCache - unsupported {}", getSessionId());
        throw new UnsupportedOperationException("Inverted - getCache");
    }

    @Override
    public void setCache(final Cache cache) {
        log.error("setCache - unsupported {}", getSessionId());
        throw new UnsupportedOperationException("Inverted - setCache");
    }

    private String getSessionId() {
        return String.format("[%s@%s]", provider.getSession()
                                                .getUsername(), provider.getSession()
                                                                        .getIoSession()
                                                                        .getRemoteAddress());
    }
}
