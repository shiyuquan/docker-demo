package persion.bleg.dockerdemo.minio.my;

import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

/**
 * @author shiyuquan
 * @since 2020/4/22 10:14 上午
 */
public class MyS3Escaper {
    private static final Escaper ESCAPER = UrlEscapers.urlPathSegmentEscaper();

    /** Returns S3 encoded string. */
    public static String encode(String str) {
        if (str == null) {
            return "";
        }

        return ESCAPER
                .escape(str)
                .replaceAll("\\!", "%21")
                .replaceAll("\\$", "%24")
                .replaceAll("\\&", "%26")
                .replaceAll("\\'", "%27")
                .replaceAll("\\(", "%28")
                .replaceAll("\\)", "%29")
                .replaceAll("\\*", "%2A")
                .replaceAll("\\+", "%2B")
                .replaceAll("\\,", "%2C")
                .replaceAll("\\/", "%2F")
                .replaceAll("\\:", "%3A")
                .replaceAll("\\;", "%3B")
                .replaceAll("\\=", "%3D")
                .replaceAll("\\@", "%40")
                .replaceAll("\\[", "%5B")
                .replaceAll("\\]", "%5D");
    }

    /** Returns S3 encoded string of given path where multiple '/' are trimmed. */
    public static String encodePath(String path) {
        StringBuffer encodedPathBuf = new StringBuffer();
        for (String pathSegment : path.split("/")) {
            if (!pathSegment.isEmpty()) {
                if (encodedPathBuf.length() > 0) {
                    encodedPathBuf.append("/");
                }
                encodedPathBuf.append(MyS3Escaper.encode(pathSegment));
            }
        }

        if (path.startsWith("/")) {
            encodedPathBuf.insert(0, "/");
        }
        if (path.endsWith("/")) {
            encodedPathBuf.append("/");
        }

        return encodedPathBuf.toString();
    }
}
