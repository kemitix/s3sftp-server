package com.hubio.s3sftp.server;

import com.upplication.s3fs.S3FileStore;
import com.upplication.s3fs.S3Path;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kemitix.mon.maybe.Maybe;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("hideutilityclassconstructor")
final class S3PathUtil {

    /**
     * Extracts the directory path from an S3Path.
     *
     * @param s3Path the S3Path
     * @return the directory path within the S3 Bucket
     */
    public static String dirPath(final S3Path s3Path) {
        return fileStoreName(s3Path)
                + S3Path.PATH_SEPARATOR + s3Path.getKey();
    }

    private static String fileStoreName(final S3Path s3Path) {
        return Maybe.maybe(s3Path.getFileStore())
                .map(S3FileStore::name)
                .map(name -> S3Path.PATH_SEPARATOR + name)
                .orElse("");
    }

}
