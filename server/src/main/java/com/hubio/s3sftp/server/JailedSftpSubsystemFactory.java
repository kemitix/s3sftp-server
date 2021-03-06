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

package com.hubio.s3sftp.server;

import com.hubio.s3sftp.server.filesystem.UserFileSystemResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.subsystem.sftp.SftpErrorStatusDataHandler;
import org.apache.sshd.server.subsystem.sftp.SftpFileSystemAccessor;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;

/**
 * Factory for {@link JailedSftpSubsystem}, where the user is jailed within a subdirectory specified by a
 * {@link SessionHome}.
 *
 * @author Paul Campbell (paul.campbell@hubio.com)
 */
@Slf4j
@RequiredArgsConstructor
class JailedSftpSubsystemFactory extends SftpSubsystemFactory {

    private final SessionBucket sessionBucket;
    private final SessionHome sessionHome;
    private final SessionJail sessionJail;
    private final UserFileSystemResolver fileSystemProvider;
    private final SftpFileSystemAccessor accessor;
    private final SftpErrorStatusDataHandler errorStatusDataHandler;

    @Override
    public Command create() {
        log.trace("create()");
        val subsystem =
                new JailedSftpSubsystem(getExecutorService(), getUnsupportedAttributePolicy(),
                        sessionBucket, sessionHome, sessionJail, fileSystemProvider, accessor, errorStatusDataHandler);
        getRegisteredListeners().forEach(subsystem::addSftpEventListener);
        log.trace(" <= {}", subsystem);
        return subsystem;
    }
}
