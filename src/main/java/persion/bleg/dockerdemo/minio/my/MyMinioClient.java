package persion.bleg.dockerdemo.minio.my;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.http.Scheme;
import io.minio.messages.*;
import io.minio.org.apache.commons.validator.routines.InetAddressValidator;
import okhttp3.*;
import org.springframework.util.StringUtils;
import persion.bleg.dockerdemo.base.BlegException;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author shiyuquan
 * @since 2020/4/22 9:51 上午
 */
public class MyMinioClient {
    private static final byte[] EMPTY_BODY = new byte[] {};
    private static final Logger LOGGER = Logger.getLogger(MyMinioClient.class.getName());
    // default network I/O timeout is 15 minutes
    private static final long DEFAULT_CONNECTION_TIMEOUT = 15 * 60;
    // maximum allowed bucket policy size is 12KiB
    private static final int MAX_BUCKET_POLICY_SIZE = 12 * 1024;
    // default expiration for a presigned URL is 7 days in seconds
    private static final int DEFAULT_EXPIRY_TIME = 7 * 24 * 3600;
    private static final String DEFAULT_USER_AGENT =
            "MinIO ("
                    + System.getProperty("os.arch")
                    + "; "
                    + System.getProperty("os.arch")
                    + ") minio-java/"
                    + MyMinioProperties.INSTANCE.getVersion();
    private static final String NULL_STRING = "(null)";
    private static final String S3_AMAZONAWS_COM = "s3.amazonaws.com";
    private static final String END_HTTP = "----------END-HTTP----------";
    private static final String US_EAST_1 = "us-east-1";
    private static final String UPLOAD_ID = "uploadId";

    private static final Set<String> amzHeaders = new HashSet<>();

    static {
        amzHeaders.add("server-side-encryption");
        amzHeaders.add("server-side-encryption-aws-kms-key-id");
        amzHeaders.add("server-side-encryption-context");
        amzHeaders.add("server-side-encryption-customer-algorithm");
        amzHeaders.add("server-side-encryption-customer-key");
        amzHeaders.add("server-side-encryption-customer-key-md5");
        amzHeaders.add("website-redirect-location");
        amzHeaders.add("storage-class");
    }

    private static final Set<String> standardHeaders = new HashSet<>();

    static {
        standardHeaders.add("content-type");
        standardHeaders.add("cache-control");
        standardHeaders.add("content-encoding");
        standardHeaders.add("content-disposition");
        standardHeaders.add("content-language");
        standardHeaders.add("expires");
        standardHeaders.add("range");
    }

    private PrintWriter traceStream;

    // the current client instance's base URL.
    private HttpUrl baseUrl;
    // access key to sign all requests with
    private String accessKey;
    // Secret key to sign all requests with
    private String secretKey;
    // Region to sign all requests with
    private String region;

    private String userAgent = DEFAULT_USER_AGENT;

    private OkHttpClient httpClient;

    /**
     * Creates MinIO client object with given endpoint using anonymous access.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code MyMinioClient minioClient = new MyMinioClient("https://play.min.io");}</pre>
     *
     * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
     *     <pre>           Examples:
     *             * https://s3.amazonaws.com
     *             * https://s3.amazonaws.com/
     *             * https://play.min.io
     *             * http://play.min.io:9010/
     *             * localhost
     *             * localhost.localdomain
     *             * play.min.io
     *             * 127.0.0.1
     *             * 192.168.1.60
     *             * ::1</pre>
     *
     * @see #MyMinioClient(URL url)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, String region)
     * @see #MyMinioClient(URL url, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
     *     secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure, OkHttpClient httpClient)
     */
    public MyMinioClient(String endpoint) throws InvalidEndpointException, InvalidPortException {
        this(endpoint, 0, null, null);
    }

    /**
     * Creates MinIO client object with given URL object using anonymous access.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code MyMinioClient minioClient = new MyMinioClient(new URL("https://play.min.io"));}</pre>
     *
     * @param url Endpoint as {@link URL} object.
     * @see #MyMinioClient(String endpoint)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, String region)
     * @see #MyMinioClient(URL url, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
     *     secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure, OkHttpClient httpClient)
     */
    public MyMinioClient(URL url) throws InvalidEndpointException, InvalidPortException {
        this(url.toString(), 0, null, null);
    }

    /**
     * Creates MinIO client object with given HttpUrl object using anonymous access.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code MyMinioClient minioClient = new MyMinioClient(new HttpUrl.parse("https://play.min.io"));}
     * </pre>
     *
     * @param url Endpoint as {@link HttpUrl} object.
     * @see #MyMinioClient(String endpoint)
     * @see #MyMinioClient(URL url)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, String region)
     * @see #MyMinioClient(URL url, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
     *     secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure, OkHttpClient httpClient)
     */
    public MyMinioClient(HttpUrl url) throws InvalidEndpointException, InvalidPortException {
        this(url.toString(), 0, null, null);
    }

    /**
     * Creates MinIO client object with given endpoint, access key and secret key.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code MyMinioClient minioClient = new MyMinioClient("https://play.min.io",
     *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");}</pre>
     *
     * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
     *     <pre>           Examples:
     *             * https://s3.amazonaws.com
     *             * https://s3.amazonaws.com/
     *             * https://play.min.io
     *             * http://play.min.io:9010/
     *             * localhost
     *             * localhost.localdomain
     *             * play.min.io
     *             * 127.0.0.1
     *             * 192.168.1.60
     *             * ::1</pre>
     *
     * @param accessKey Access key (aka user ID) of your account in S3 service.
     * @param secretKey Secret Key (aka password) of your account in S3 service.
     * @see #MyMinioClient(String endpoint)
     * @see #MyMinioClient(URL url)
     * @see #MyMinioClient(URL url, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, String region)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
     *     secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure, OkHttpClient httpClient)
     */
    public MyMinioClient(String endpoint, String accessKey, String secretKey)
            throws InvalidEndpointException, InvalidPortException {
        this(endpoint, 0, accessKey, secretKey);
    }

    /**
     * Creates MinIO client object with given endpoint, access key, secret key and region name.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code MyMinioClient minioClient = new MyMinioClient("https://play.min.io",
     *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", "us-west-1");}</pre>
     *
     * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
     *     <pre>           Examples:
     *             * https://s3.amazonaws.com
     *             * https://s3.amazonaws.com/
     *             * https://play.min.io
     *             * http://play.min.io:9010/
     *             * localhost
     *             * localhost.localdomain
     *             * play.min.io
     *             * 127.0.0.1
     *             * 192.168.1.60
     *             * ::1</pre>
     *
     * @param accessKey Access key (aka user ID) of your account in S3 service.
     * @param secretKey Secret Key (aka password) of your account in S3 service.
     * @param region Region name of buckets in S3 service.
     * @see #MyMinioClient(String endpoint)
     * @see #MyMinioClient(URL url)
     * @see #MyMinioClient(URL url, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, String region)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
     *     secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure, OkHttpClient httpClient)
     */
    public MyMinioClient(String endpoint, String accessKey, String secretKey, String region)
            throws InvalidEndpointException, InvalidPortException {
        this(
                endpoint,
                0,
                accessKey,
                secretKey,
                region,
                !(endpoint != null && endpoint.startsWith("http://")));
    }

    /**
     * Creates MinIO client object with given URL object, access key and secret key.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code MyMinioClient minioClient = new MyMinioClient(new URL("https://play.min.io"),
     *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");}</pre>
     *
     * @param url Endpoint as {@link URL} object.
     * @param accessKey Access key (aka user ID) of your account in S3 service.
     * @param secretKey Secret Key (aka password) of your account in S3 service.
     * @see #MyMinioClient(String endpoint)
     * @see #MyMinioClient(URL url)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, String region)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
     *     secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure, OkHttpClient httpClient)
     */
    public MyMinioClient(URL url, String accessKey, String secretKey)
            throws InvalidEndpointException, InvalidPortException {
        this(url.toString(), 0, accessKey, secretKey);
    }

    /**
     * Creates MinIO client object with given URL object, access key and secret key.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code MyMinioClient minioClient = new MyMinioClient(HttpUrl.parse("https://play.min.io"),
     *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");}</pre>
     *
     * @param url Endpoint as {@link HttpUrl} object.
     * @param accessKey Access key (aka user ID) of your account in S3 service.
     * @param secretKey Secret Key (aka password) of your account in S3 service.
     * @see #MyMinioClient(String endpoint)
     * @see #MyMinioClient(URL url)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, String region)
     * @see #MyMinioClient(URL url, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
     *     secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure, OkHttpClient httpClient)
     */
    public MyMinioClient(HttpUrl url, String accessKey, String secretKey)
            throws InvalidEndpointException, InvalidPortException {
        this(url.toString(), 0, accessKey, secretKey);
    }

    /**
     * Creates MinIO client object with given endpoint, port, access key and secret key.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code MyMinioClient minioClient = new MyMinioClient("play.min.io", 9000,
     *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG");}</pre>
     *
     * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
     *     <pre>           Examples:
     *             * https://s3.amazonaws.com
     *             * https://s3.amazonaws.com/
     *             * https://play.min.io
     *             * http://play.min.io:9010/
     *             * localhost
     *             * localhost.localdomain
     *             * play.min.io
     *             * 127.0.0.1
     *             * 192.168.1.60
     *             * ::1</pre>
     *
     * @param port TCP/IP port number between 1 and 65535. Unused if endpoint is an URL.
     * @param accessKey Access key (aka user ID) of your account in S3 service.
     * @param secretKey Secret Key (aka password) of your account in S3 service.
     * @see #MyMinioClient(String endpoint)
     * @see #MyMinioClient(URL url)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, String region)
     * @see #MyMinioClient(URL url, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
     *     secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure, OkHttpClient httpClient)
     */
    public MyMinioClient(String endpoint, int port, String accessKey, String secretKey)
            throws InvalidEndpointException, InvalidPortException {
        this(
                endpoint,
                port,
                accessKey,
                secretKey,
                !(endpoint != null && endpoint.startsWith("http://")));
    }

    /**
     * Creates MinIO client object with given endpoint, access key and secret key using secure (TLS)
     * connection.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code MyMinioClient minioClient = new MyMinioClient("play.min.io",
     *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true);}</pre>
     *
     * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
     *     <pre>           Examples:
     *             * https://s3.amazonaws.com
     *             * https://s3.amazonaws.com/
     *             * https://play.min.io
     *             * http://play.min.io:9010/
     *             * localhost
     *             * localhost.localdomain
     *             * play.min.io
     *             * 127.0.0.1
     *             * 192.168.1.60
     *             * ::1</pre>
     *
     * @param accessKey Access key (aka user ID) of your account in S3 service.
     * @param secretKey Secret Key (aka password) of your account in S3 service.
     * @param secure Flag to indicate to use secure (TLS) connection to S3 service or not.
     * @see #MyMinioClient(String endpoint)
     * @see #MyMinioClient(URL url)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, String region)
     * @see #MyMinioClient(URL url, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, boolean
     *     secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure, OkHttpClient httpClient)
     */
    public MyMinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
            throws InvalidEndpointException, InvalidPortException {
        this(endpoint, 0, accessKey, secretKey, secure);
    }

    /**
     * Creates MinIO client object using given endpoint, port, access key, secret key and secure (TLS)
     * connection.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code MyMinioClient minioClient = new MyMinioClient("play.min.io", 9000,
     *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true);}</pre>
     *
     * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
     *     <pre>           Examples:
     *             * https://s3.amazonaws.com
     *             * https://s3.amazonaws.com/
     *             * https://play.min.io
     *             * http://play.min.io:9010/
     *             * localhost
     *             * localhost.localdomain
     *             * play.min.io
     *             * 127.0.0.1
     *             * 192.168.1.60
     *             * ::1</pre>
     *
     * @param port TCP/IP port number between 1 and 65535. Unused if endpoint is an URL.
     * @param accessKey Access key (aka user ID) of your account in S3 service.
     * @param secretKey Secret Key (aka password) of your account in S3 service.
     * @param secure Flag to indicate to use secure (TLS) connection to S3 service or not.
     * @see #MyMinioClient(String endpoint)
     * @see #MyMinioClient(URL url)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, String region)
     * @see #MyMinioClient(URL url, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure, OkHttpClient httpClient)
     */
    public MyMinioClient(String endpoint, int port, String accessKey, String secretKey, boolean secure)
            throws InvalidEndpointException, InvalidPortException {
        this(endpoint, port, accessKey, secretKey, null, secure);
    }

    /**
     * Creates MinIO client object using given endpoint, port, access key, secret key, region and
     * secure (TLS) connection.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code MyMinioClient minioClient = new MyMinioClient("play.min.io", 9000,
     *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true);}</pre>
     *
     * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
     *     <pre>           Examples:
     *             * https://s3.amazonaws.com
     *             * https://s3.amazonaws.com/
     *             * https://play.min.io
     *             * http://play.min.io:9010/
     *             * localhost
     *             * localhost.localdomain
     *             * play.min.io
     *             * 127.0.0.1
     *             * 192.168.1.60
     *             * ::1</pre>
     *
     * @param port TCP/IP port number between 1 and 65535. Unused if endpoint is an URL.
     * @param accessKey Access key (aka user ID) of your account in S3 service.
     * @param secretKey Secret Key (aka password) of your account in S3 service.
     * @param region Region name of buckets in S3 service.
     * @param secure Flag to indicate to use secure (TLS) connection to S3 service or not.
     * @see #MyMinioClient(String endpoint)
     * @see #MyMinioClient(URL url)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, String region)
     * @see #MyMinioClient(URL url, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure, OkHttpClient httpClient)
     */
    public MyMinioClient(
            String endpoint, int port, String accessKey, String secretKey, String region, boolean secure)
            throws InvalidEndpointException, InvalidPortException {
        this(endpoint, port, accessKey, secretKey, region, secure, null);
    }

    /**
     * Creates MinIO client object using given endpoint, port, access key, secret key, region and
     * secure (TLS) connection.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code MyMinioClient minioClient = new MyMinioClient("play.min.io", 9000,
     *     "Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG", true,
     *     customHttpClient);}</pre>
     *
     * @param endpoint Endpoint is an URL, domain name, IPv4 or IPv6 address of S3 service.
     *     <pre>           Examples:
     *             * https://s3.amazonaws.com
     *             * https://s3.amazonaws.com/
     *             * https://play.min.io
     *             * http://play.min.io:9010/
     *             * localhost
     *             * localhost.localdomain
     *             * play.min.io
     *             * 127.0.0.1
     *             * 192.168.1.60
     *             * ::1</pre>
     *
     * @param port TCP/IP port number between 1 and 65535. Unused if endpoint is an URL.
     * @param accessKey Access key (aka user ID) of your account in S3 service.
     * @param secretKey Secret Key (aka password) of your account in S3 service.
     * @param region Region name of buckets in S3 service.
     * @param secure Flag to indicate to use secure (TLS) connection to S3 service or not.
     * @param httpClient Customized HTTP client object.
     * @see #MyMinioClient(String endpoint)
     * @see #MyMinioClient(URL url)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, String region)
     * @see #MyMinioClient(URL url, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey)
     * @see #MyMinioClient(String endpoint, String accessKey, String secretKey, boolean secure)
     * @see #MyMinioClient(String endpoint, int port, String accessKey, String secretKey, String region,
     *     boolean secure)
     */
    public MyMinioClient(
            String endpoint,
            int port,
            String accessKey,
            String secretKey,
            String region,
            boolean secure,
            OkHttpClient httpClient)
            throws InvalidEndpointException, InvalidPortException {
        if (endpoint == null) {
            throw new InvalidEndpointException(NULL_STRING, "null endpoint");
        }

        if (port < 0 || port > 65535) {
            throw new InvalidPortException(port, "port must be in range of 1 to 65535");
        }

        if (httpClient != null) {
            this.httpClient = httpClient;
        } else {
            List<Protocol> protocol = new LinkedList<>();
            protocol.add(Protocol.HTTP_1_1);
            this.httpClient = new OkHttpClient();
            this.httpClient =
                    this.httpClient
                            .newBuilder()
                            .connectTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                            .writeTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                            .readTimeout(DEFAULT_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                            .protocols(protocol)
                            .build();
        }

        HttpUrl url = HttpUrl.parse(endpoint);
        if (url != null) {
            if (!"/".equals(url.encodedPath())) {
                throw new InvalidEndpointException(endpoint, "no path allowed in endpoint");
            }

            HttpUrl.Builder urlBuilder = url.newBuilder();
            Scheme scheme = Scheme.HTTP;
            if (secure) {
                scheme = Scheme.HTTPS;
            }

            urlBuilder.scheme(scheme.toString());

            if (port > 0) {
                urlBuilder.port(port);
            }

            this.baseUrl = urlBuilder.build();
            this.accessKey = accessKey;
            this.secretKey = secretKey;
            this.region = region;

            return;
        }

        // endpoint may be a valid hostname, IPv4 or IPv6 address
        if (!this.isValidEndpoint(endpoint)) {
            throw new InvalidEndpointException(endpoint, "invalid host");
        }

        Scheme scheme = Scheme.HTTP;
        if (secure) {
            scheme = Scheme.HTTPS;
        }

        if (port == 0) {
            this.baseUrl = new HttpUrl.Builder().scheme(scheme.toString()).host(endpoint).build();
        } else {
            this.baseUrl =
                    new HttpUrl.Builder().scheme(scheme.toString()).host(endpoint).port(port).build();
        }
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
    }

    /** Returns true if given endpoint is valid else false. */
    public boolean isValidEndpoint(String endpoint) {
        if (InetAddressValidator.getInstance().isValid(endpoint)) {
            return true;
        }

        // endpoint may be a hostname
        // refer https://en.wikipedia.org/wiki/Hostname#Restrictions_on_valid_host_names
        // why checks are done like below
        if (endpoint.length() < 1 || endpoint.length() > 253) {
            return false;
        }

        for (String label : endpoint.split("\\.")) {
            if (label.length() < 1 || label.length() > 63) {
                return false;
            }

            if (!(label.matches("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?$"))) {
                return false;
            }
        }

        return true;
    }

    /** Validates if given bucket name is DNS compatible. */
    public void checkBucketName(String name) throws InvalidBucketNameException {
        if (name == null) {
            throw new InvalidBucketNameException(NULL_STRING, "null bucket name");
        }

        // Bucket names cannot be no less than 3 and no more than 63 characters long.
        if (name.length() < 3 || name.length() > 63) {
            String msg = "bucket name must be at least 3 and no more than 63 characters long";
            throw new InvalidBucketNameException(name, msg);
        }
        // Successive periods in bucket names are not allowed.
        if (name.contains("..")) {
            String msg =
                    "bucket name cannot contain successive periods. For more information refer "
                            + "http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html";
            throw new InvalidBucketNameException(name, msg);
        }
        // Bucket names should be dns compatible.
        if (!name.matches("^[a-z0-9][a-z0-9\\.\\-]+[a-z0-9]$")) {
            String msg =
                    "bucket name does not follow Amazon S3 standards. For more information refer "
                            + "http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html";
            throw new InvalidBucketNameException(name, msg);
        }
    }

    public void checkObjectName(String objectName) throws IllegalArgumentException {
        if ((objectName == null) || (objectName.isEmpty())) {
            throw new IllegalArgumentException("object name cannot be empty");
        }
    }

    public void checkReadRequestSse(ServerSideEncryption sse) throws IllegalArgumentException {
        if (sse == null) {
            return;
        }

        if (sse.type() != ServerSideEncryption.Type.SSE_C) {
            throw new IllegalArgumentException("only SSE_C is supported for all read requests.");
        }

        if (sse.type().requiresTls() && !this.baseUrl.isHttps()) {
            throw new IllegalArgumentException(
                    sse.type().name() + "operations must be performed over a secure connection.");
        }
    }

    public void checkWriteRequestSse(ServerSideEncryption sse) throws IllegalArgumentException {
        if (sse == null) {
            return;
        }

        if (sse.type().requiresTls() && !this.baseUrl.isHttps()) {
            throw new IllegalArgumentException(
                    sse.type().name() + " operations must be performed over a secure connection.");
        }
    }

    public Map<String, String> normalizeHeaders(Map<String, String> headerMap) {
        Map<String, String> normHeaderMap = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : headerMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String keyLowerCased = key.toLowerCase(Locale.US);
            if (amzHeaders.contains(keyLowerCased)) {
                key = "x-amz-" + key;
            } else if (!standardHeaders.contains(keyLowerCased) && !keyLowerCased.startsWith("x-amz-")) {
                key = "x-amz-meta-" + key;
            }
            normHeaderMap.put(key, value);
        }
        return normHeaderMap;
    }

    public HttpUrl buildUrl(
            Method method,
            String bucketName,
            String objectName,
            String region,
            Multimap<String, String> queryParamMap)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException {
        if (bucketName == null && objectName != null) {
            throw new InvalidBucketNameException(
                    NULL_STRING, "null bucket name for object '" + objectName + "'");
        }

        HttpUrl.Builder urlBuilder = this.baseUrl.newBuilder();
        if (bucketName != null) {
            checkBucketName(bucketName);

            String host = this.baseUrl.host();
            if (host.equals(S3_AMAZONAWS_COM)) {
                // special case: handle s3.amazonaws.com separately
                if (region != null) {
                    host = MyAwsS3Endpoints.INSTANCE.endpoint(region);
                }

                boolean usePathStyle = false;
                if (method == Method.PUT && objectName == null && queryParamMap == null) {
                    // use path style for make bucket to workaround "AuthorizationHeaderMalformed" error from
                    // s3.amazonaws.com
                    usePathStyle = true;
                } else if (queryParamMap != null && queryParamMap.containsKey("location")) {
                    // use path style for location query
                    usePathStyle = true;
                } else if (bucketName.contains(".") && this.baseUrl.isHttps()) {
                    // use path style where '.' in bucketName causes SSL certificate validation error
                    usePathStyle = true;
                }

                if (usePathStyle) {
                    urlBuilder.host(host);
                    urlBuilder.addEncodedPathSegment(MyS3Escaper.encode(bucketName));
                } else {
                    urlBuilder.host(bucketName + "." + host);
                }
            } else {
                urlBuilder.addEncodedPathSegment(MyS3Escaper.encode(bucketName));
            }
        }

        if (objectName != null) {
            // Limitation: OkHttp does not allow to add '.' and '..' as path segment.
            for (String token : objectName.split("/")) {
                if (token.equals(".") || token.equals("..")) {
                    throw new IllegalArgumentException(
                            "object name with '.' or '..' path segment is not supported");
                }
            }

            urlBuilder.addEncodedPathSegments(MyS3Escaper.encodePath(objectName));
        }

        if (queryParamMap != null) {
            for (Map.Entry<String, String> entry : queryParamMap.entries()) {
                urlBuilder.addEncodedQueryParameter(
                        MyS3Escaper.encode(entry.getKey()), MyS3Escaper.encode(entry.getValue()));
            }
        }

        return urlBuilder.build();
    }

    public String getHostHeader(HttpUrl url) {
        // ignore port when port and service matches i.e HTTP -> 80, HTTPS -> 443
        if ((url.scheme().equals("http") && url.port() == 80)
                || (url.scheme().equals("https") && url.port() == 443)) {
            return url.host();
        }

        return url.host() + ":" + url.port();
    }

    public Request createRequest(
            HttpUrl url, Method method, Multimap<String, String> headerMap, Object body, int length)
            throws NoSuchAlgorithmException, IllegalArgumentException, IOException,
            InsufficientDataException, InternalException {
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);

        String contentType = null;
        String contentEncoding = null;
        if (headerMap != null) {
            contentEncoding =
                    headerMap.get("Content-Encoding").stream()
                            .distinct()
                            .filter(encoding -> !encoding.isEmpty())
                            .collect(Collectors.joining(","));
            for (Map.Entry<String, String> entry : headerMap.entries()) {
                if (entry.getKey().equals("Content-Type")) {
                    contentType = entry.getValue();
                }

                if (!entry.getKey().equals("Content-Encoding")) {
                    requestBuilder.header(entry.getKey(), entry.getValue());
                }
            }
        }

        if (contentEncoding != null) {
            requestBuilder.header("Content-Encoding", contentEncoding);
        }

        requestBuilder.header("Host", getHostHeader(url));
        // Disable default gzip compression by okhttp library.
        requestBuilder.header("Accept-Encoding", "identity");
        requestBuilder.header("User-Agent", this.userAgent);

        String sha256Hash = null;
        String md5Hash = null;
        if (this.accessKey != null && this.secretKey != null) {
            if (url.isHttps()) {
                // Fix issue #415: No need to compute sha256 if endpoint scheme is HTTPS.
                sha256Hash = "UNSIGNED-PAYLOAD";
                if (body != null) {
                    md5Hash = MyDigest.md5Hash(body, length);
                }
            } else {
                Object data = body;
                int len = length;
                if (data == null) {
                    data = new byte[0];
                    len = 0;
                }

                String[] hashes = MyDigest.sha256Md5Hashes(data, len);
                sha256Hash = hashes[0];
                md5Hash = hashes[1];
            }
        } else {
            // Fix issue #567: Compute MD5 hash only for anonymous access.
            if (body != null) {
                md5Hash = MyDigest.md5Hash(body, length);
            }
        }

        if (md5Hash != null) {
            requestBuilder.header("Content-MD5", md5Hash);
        }

        if (sha256Hash != null) {
            requestBuilder.header("x-amz-content-sha256", sha256Hash);
        }

        ZonedDateTime date = ZonedDateTime.now();
        requestBuilder.header("x-amz-date", date.format(Time.AMZ_DATE_FORMAT));

        RequestBody requestBody = null;
        if (body != null) {
            if (body instanceof RandomAccessFile) {
                requestBody = new MyHttpRequestBody((RandomAccessFile) body, length, contentType);
            } else if (body instanceof BufferedInputStream) {
                requestBody = new MyHttpRequestBody((BufferedInputStream) body, length, contentType);
            } else {
                requestBody = new MyHttpRequestBody((byte[]) body, length, contentType);
            }
        }

        requestBuilder.method(method.toString(), requestBody);
        return requestBuilder.build();
    }

    public Response execute(
            Method method,
            String bucketName,
            String objectName,
            String region,
            Multimap<String, String> headerMap,
            Multimap<String, String> queryParamMap,
            Object body,
            int length)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        boolean traceRequestBody = false;
        if (body != null
                && !(body instanceof InputStream
                || body instanceof RandomAccessFile
                || body instanceof byte[])) {
            byte[] bytes;
            if (body instanceof CharSequence) {
                bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            } else {
                bytes = Xml.marshal(body).getBytes(StandardCharsets.UTF_8);
            }

            body = bytes;
            length = bytes.length;
            traceRequestBody = true;
        }

        if (body == null && (method == Method.PUT || method == Method.POST)) {
            body = EMPTY_BODY;
        }

        HttpUrl url = buildUrl(method, bucketName, objectName, region, queryParamMap);
        Request request = createRequest(url, method, headerMap, body, length);

        if (this.accessKey != null && this.secretKey != null) {
            request = MySigner.signV4(request, region, accessKey, secretKey);
        }

        if (this.traceStream != null) {
            this.traceStream.println("---------START-HTTP---------");
            String encodedPath = request.url().encodedPath();
            String encodedQuery = request.url().encodedQuery();
            if (encodedQuery != null) {
                encodedPath += "?" + encodedQuery;
            }
            this.traceStream.println(request.method() + " " + encodedPath + " HTTP/1.1");
            String headers =
                    request
                            .headers()
                            .toString()
                            .replaceAll("Signature=([0-9a-f]+)", "Signature=*REDACTED*")
                            .replaceAll("Credential=([^/]+)", "Credential=*REDACTED*");
            this.traceStream.println(headers);
            if (traceRequestBody) {
                this.traceStream.println(new String((byte[]) body, StandardCharsets.UTF_8));
            }
        }

        Response response = this.httpClient.newCall(request).execute();
        if (this.traceStream != null) {
            this.traceStream.println(
                    response.protocol().toString().toUpperCase(Locale.US) + " " + response.code());
            this.traceStream.println(response.headers());
        }

        if (response.isSuccessful()) {
            if (this.traceStream != null) {
                this.traceStream.println(END_HTTP);
            }
            // response.headers().toMultimap();
            return response;
        }

        String errorXml = null;
        try (ResponseBody responseBody = response.body()) {
            errorXml = new String(responseBody.bytes(), StandardCharsets.UTF_8);
        }

        if (this.traceStream != null && !("".equals(errorXml) && method.equals(Method.HEAD))) {
            this.traceStream.println(errorXml);
        }

        // Error in case of Non-XML response from server for non-HEAD requests.
        String contentType = response.headers().get("content-type");
        if (!method.equals(Method.HEAD)
                && (contentType == null
                || !Arrays.asList(contentType.split(";")).contains("application/xml"))) {
            if (this.traceStream != null) {
                this.traceStream.println(END_HTTP);
            }
            throw new InvalidResponseException();
        }

        ErrorResponse errorResponse = null;
        if (!"".equals(errorXml)) {
            errorResponse = Xml.unmarshal(ErrorResponse.class, errorXml);
        } else if (!method.equals(Method.HEAD)) {
            if (this.traceStream != null) {
                this.traceStream.println(END_HTTP);
            }
            throw new InvalidResponseException();
        }

        if (this.traceStream != null) {
            this.traceStream.println(END_HTTP);
        }

        if (errorResponse == null) {
            ErrorCode ec;
            switch (response.code()) {
                case 307:
                    ec = ErrorCode.REDIRECT;
                    break;
                case 400:
                    // HEAD bucket with wrong region gives 400 without body.
                    if (method.equals(Method.HEAD)
                            && bucketName != null
                            && objectName == null
                            && MyBucketRegionCache.INSTANCE.exists(bucketName)) {
                        ec = ErrorCode.RETRY_HEAD_BUCKET;
                    } else {
                        ec = ErrorCode.INVALID_URI;
                    }
                    break;
                case 404:
                    if (objectName != null) {
                        ec = ErrorCode.NO_SUCH_KEY;
                    } else if (bucketName != null) {
                        ec = ErrorCode.NO_SUCH_BUCKET;
                    } else {
                        ec = ErrorCode.RESOURCE_NOT_FOUND;
                    }
                    break;
                case 501:
                case 405:
                    ec = ErrorCode.METHOD_NOT_ALLOWED;
                    break;
                case 409:
                    if (bucketName != null) {
                        ec = ErrorCode.NO_SUCH_BUCKET;
                    } else {
                        ec = ErrorCode.RESOURCE_CONFLICT;
                    }
                    break;
                case 403:
                    ec = ErrorCode.ACCESS_DENIED;
                    break;
                default:
                    throw new InternalException(
                            "unhandled HTTP code "
                                    + response.code()
                                    + ".  Please report this issue at "
                                    + "https://github.com/minio/minio-java/issues");
            }

            errorResponse =
                    new ErrorResponse(
                            ec,
                            bucketName,
                            objectName,
                            request.url().encodedPath(),
                            response.header("x-amz-request-id"),
                            response.header("x-amz-id-2"));
        }

        // invalidate region cache if needed
        if (errorResponse.errorCode() == ErrorCode.NO_SUCH_BUCKET
                || errorResponse.errorCode() == ErrorCode.RETRY_HEAD_BUCKET) {
            MyBucketRegionCache.INSTANCE.remove(bucketName);
            // TODO: handle for other cases as well
        }

        throw new ErrorResponseException(errorResponse, response);
    }

    public Response execute(
            Method method,
            String bucketName,
            String objectName,
            String region,
            Map<String, String> headerMap,
            Map<String, String> queryParamMap,
            Object body,
            int length)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Multimap<String, String> headerMultiMap = null;
        if (headerMap != null) {
            headerMultiMap = Multimaps.forMap(normalizeHeaders(headerMap));
        }

        Multimap<String, String> queryParamMultiMap = null;
        if (queryParamMap != null) {
            queryParamMultiMap = Multimaps.forMap(queryParamMap);
        }

        return execute(
                method, bucketName, objectName, region, headerMultiMap, queryParamMultiMap, body, length);
    }

    /** Updates Region cache for given bucket. */
    public void updateRegionCache(String bucketName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        if (bucketName != null
                && this.accessKey != null
                && this.secretKey != null
                && !MyBucketRegionCache.INSTANCE.exists(bucketName)) {
            Map<String, String> queryParamMap = new HashMap<>();
            queryParamMap.put("location", null);

            Response response =
                    execute(Method.GET, bucketName, null, US_EAST_1, null, queryParamMap, null, 0);

            String region;
            try (ResponseBody body = response.body()) {
                LocationConstraint lc = Xml.unmarshal(LocationConstraint.class, body.charStream());
                if (lc.location() == null || lc.location().equals("")) {
                    region = US_EAST_1; // default region
                } else if (lc.location().equals("EU")) {
                    region = "eu-west-1"; // eu-west-1 can be sometimes 'EU'.
                } else {
                    region = lc.location();
                }
            }

            // Add the new location.
            MyBucketRegionCache.INSTANCE.set(bucketName, region);
        }
    }

    /** Returns region of given bucket either from region cache or set in constructor. */
    public String getRegion(String bucketName)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IllegalArgumentException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        String region;
        if (this.region == null || "".equals(this.region)) {
            updateRegionCache(bucketName);
            region = MyBucketRegionCache.INSTANCE.region(bucketName);
        } else {
            region = this.region;
        }
        return region;
    }

    public Response executeGet(
            String bucketName,
            String objectName,
            Map<String, String> headerMap,
            Map<String, String> queryParamMap)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        return execute(
                Method.GET,
                bucketName,
                objectName,
                getRegion(bucketName),
                headerMap,
                queryParamMap,
                null,
                0);
    }

    public Response executeGet(
            String bucketName, String objectName, Multimap<String, String> queryParamMap)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        return execute(
                Method.GET, bucketName, objectName, getRegion(bucketName), null, queryParamMap, null, 0);
    }

    public Response executeHead(
            String bucketName,
            String objectName,
            Map<String, String> headerMap,
            Map<String, String> queryParamMap)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Response response =
                execute(
                        Method.HEAD,
                        bucketName,
                        objectName,
                        getRegion(bucketName),
                        headerMap,
                        queryParamMap,
                        null,
                        0);
        response.body().close();
        return response;
    }

    public Response executeHead(String bucketName, String objectName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        try {
            return executeHead(bucketName, objectName, null, null);
        } catch (ErrorResponseException e) {
            if (e.errorResponse().errorCode() != ErrorCode.RETRY_HEAD_BUCKET) {
                throw e;
            }
        }

        // Retry once for RETRY_HEAD_BUCKET error.
        return executeHead(bucketName, objectName, null, null);
    }

    public Response executeHead(String bucketName, String objectName, Map<String, String> headerMap)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        return executeHead(bucketName, objectName, headerMap, null);
    }

    public Response executeDelete(
            String bucketName, String objectName, Map<String, String> queryParamMap)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Response response =
                execute(
                        Method.DELETE,
                        bucketName,
                        objectName,
                        getRegion(bucketName),
                        null,
                        queryParamMap,
                        null,
                        0);
        response.body().close();
        return response;
    }

    public Response executePost(
            String bucketName,
            String objectName,
            Map<String, String> headerMap,
            Map<String, String> queryParamMap,
            Object data)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        return execute(
                Method.POST,
                bucketName,
                objectName,
                getRegion(bucketName),
                headerMap,
                queryParamMap,
                data,
                0);
    }

    public Response executePut(
            String bucketName,
            String objectName,
            String region,
            Map<String, String> headerMap,
            Map<String, String> queryParamMap,
            Object data,
            int length)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        return execute(
                Method.PUT, bucketName, objectName, region, headerMap, queryParamMap, data, length);
    }

    public Response executePut(
            String bucketName,
            String objectName,
            Map<String, String> headerMap,
            Map<String, String> queryParamMap,
            Object data,
            int length)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        return executePut(
                bucketName, objectName, getRegion(bucketName), headerMap, queryParamMap, data, length);
    }

    /**
     * Returns meta data information of given object in given bucket.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code ObjectStat objectStat = minioClient.statObject("my-bucketname", "my-objectname");}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @return Populated object meta data.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     * @see ObjectStat
     */
    public ObjectStat statObject(String bucketName, String objectName)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, XmlParserException, ErrorResponseException,
            InternalException, InvalidResponseException, IllegalArgumentException {
        return statObject(bucketName, objectName, null);
    }

    /**
     * Returns meta data information of given object in given bucket.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code ObjectStat objectStat = minioClient.statObject("my-bucketname", "my-objectname", sse);}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @param sse SSE-C type of server-side encryption.
     * @return Populated object meta data.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     * @see ObjectStat
     */
    public ObjectStat statObject(String bucketName, String objectName, ServerSideEncryption sse)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, XmlParserException, ErrorResponseException,
            InternalException, IllegalArgumentException, InvalidResponseException {
        checkReadRequestSse(sse);
        checkBucketName(bucketName);
        checkObjectName(objectName);

        Map<String, String> headers = null;
        if (sse != null) {
            headers = sse.headers();
        }

        Response response = executeHead(bucketName, objectName, headers);
        try {
            return new ObjectStat(bucketName, objectName, response.headers());
        } finally {
            response.close();
        }
    }

    /**
     * Gets object's URL in given bucket. The URL is ONLY useful to retrieve the object's data if the
     * object has public read permissions.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code String url = minioClient.getObjectUrl("my-bucketname", "my-objectname");}</pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @return string contains URL to download the object.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public String getObjectUrl(String bucketName, String objectName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        checkObjectName(objectName);
        HttpUrl url = buildUrl(Method.GET, bucketName, objectName, getRegion(bucketName), null);
        return url.toString();
    }

    /**
     * Gets entire object's data as {@link InputStream} in given bucket. The InputStream must be
     * closed after use else the connection will remain open.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code InputStream stream = minioClient.getObject("my-bucketname", "my-objectname");}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @return {@link InputStream} containing the object data.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public InputStream getObject(String bucketName, String objectName)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, XmlParserException, ErrorResponseException,
            InternalException, IllegalArgumentException, InvalidResponseException {
        return getObject(bucketName, objectName, null, null, null);
    }

    /**
     * Gets entire object's data as {@link InputStream} in given bucket. The InputStream must be
     * closed after use else the connection will remain open.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code InputStream stream = minioClient.getObject("my-bucketname", "my-objectname", sse);}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @param sse Encryption metadata only required for SSE-C.
     * @return {@link InputStream} containing the object data.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public InputStream getObject(String bucketName, String objectName, ServerSideEncryption sse)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, XmlParserException, ErrorResponseException,
            InternalException, IllegalArgumentException, InvalidResponseException {
        return getObject(bucketName, objectName, null, null, sse);
    }

    /**
     * Gets object's data starting from given offset as {@link InputStream} in the given bucket. The
     * InputStream must be closed after use else the connection will remain open.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code InputStream stream = minioClient.getObject("my-bucketname", "my-objectname", 1024L);}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @param offset Offset to read at.
     * @return {@link InputStream} containing the object's data.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public InputStream getObject(String bucketName, String objectName, long offset)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, XmlParserException, ErrorResponseException,
            InternalException, IllegalArgumentException, InvalidResponseException {
        return getObject(bucketName, objectName, offset, null, null);
    }

    /**
     * Gets object's data of given offset and length as {@link InputStream} in the given bucket. The
     * InputStream must be closed after use else the connection will remain open.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code InputStream stream = minioClient.getObject("my-bucketname", "my-objectname", 1024L, 4096L);}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @param offset Offset to read at.
     * @param length Length to read.
     * @return {@link InputStream} containing the object's data.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public InputStream getObject(String bucketName, String objectName, long offset, Long length)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, XmlParserException, ErrorResponseException,
            InternalException, IllegalArgumentException, InvalidResponseException {
        return getObject(bucketName, objectName, offset, length, null);
    }

    /**
     * Gets object's data of given offset and length as {@link InputStream} in the given bucket. The
     * InputStream must be closed after use else the connection will remain open.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code InputStream stream = minioClient.getObject("my-bucketname", "my-objectname", 1024L, 4096L, sse);}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @param offset Offset to read at.
     * @param length Length to read.
     * @param sse Server side encryption.
     * @return {@link InputStream} containing the object's data.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public InputStream getObject(
            String bucketName, String objectName, Long offset, Long length, ServerSideEncryption sse)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, XmlParserException, ErrorResponseException,
            InternalException, IllegalArgumentException, InvalidResponseException {
        if ((bucketName == null) || (bucketName.isEmpty())) {
            throw new IllegalArgumentException("bucket name cannot be empty");
        }

        checkObjectName(objectName);

        if (offset != null && offset < 0) {
            throw new IllegalArgumentException("offset should be zero or greater");
        }

        if (length != null && length <= 0) {
            throw new IllegalArgumentException("length should be greater than zero");
        }

        checkReadRequestSse(sse);

        if (length != null && offset == null) {
            offset = 0L;
        }

        Map<String, String> headerMap = null;
        if (offset != null || length != null || sse != null) {
            headerMap = new HashMap<>();
        }

        if (length != null) {
            headerMap.put("Range", "bytes=" + offset + "-" + (offset + length - 1));
        } else if (offset != null) {
            headerMap.put("Range", "bytes=" + offset + "-");
        }

        if (sse != null) {
            headerMap.putAll(sse.headers());
        }

        Response response = executeGet(bucketName, objectName, headerMap, null);
        return response.body().byteStream();
    }

    /**
     * Gets object's data in the given bucket and stores it to given file name.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code minioClient.getObject("my-bucketname", "my-objectname", "photo.jpg");}</pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @param fileName file name.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void getObject(String bucketName, String objectName, String fileName)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, XmlParserException, ErrorResponseException,
            InternalException, IllegalArgumentException, InvalidResponseException {
        getObject(bucketName, objectName, null, fileName);
    }

    /**
     * Gets encrypted object's data in the given bucket and stores it to given file name.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code minioClient.getObject("my-bucketname", "my-objectname", sse, "photo.jpg");}</pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @param sse encryption metadata.
     * @param fileName file name to download into.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void getObject(
            String bucketName, String objectName, ServerSideEncryption sse, String fileName)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, XmlParserException, ErrorResponseException,
            InternalException, IllegalArgumentException, InvalidResponseException {
        checkReadRequestSse(sse);

        Path filePath = Paths.get(fileName);
        boolean fileExists = Files.exists(filePath);

        if (fileExists && !Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException(fileName + ": not a regular file");
        }

        ObjectStat objectStat = statObject(bucketName, objectName, sse);
        long length = objectStat.length();
        String etag = objectStat.etag();

        String tempFileName = fileName + "." + etag + ".part.minio";
        Path tempFilePath = Paths.get(tempFileName);
        boolean tempFileExists = Files.exists(tempFilePath);

        if (tempFileExists && !Files.isRegularFile(tempFilePath)) {
            throw new IOException(tempFileName + ": not a regular file");
        }

        long tempFileSize = 0;
        if (tempFileExists) {
            tempFileSize = Files.size(tempFilePath);
            if (tempFileSize > length) {
                Files.delete(tempFilePath);
                tempFileExists = false;
                tempFileSize = 0;
            }
        }

        if (fileExists) {
            long fileSize = Files.size(filePath);
            if (fileSize == length) {
                // already downloaded. nothing to do
                return;
            } else if (fileSize > length) {
                throw new IllegalArgumentException(
                        "Source object, '"
                                + objectName
                                + "', size:"
                                + length
                                + " is smaller than the destination file, '"
                                + fileName
                                + "', size:"
                                + fileSize);
            } else if (!tempFileExists) {
                // before resuming the download, copy filename to tempfilename
                Files.copy(filePath, tempFilePath);
                tempFileSize = fileSize;
                tempFileExists = true;
            }
        }

        InputStream is = null;
        OutputStream os = null;
        try {
            is = getObject(bucketName, objectName, tempFileSize, null, sse);
            os =
                    Files.newOutputStream(tempFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            long bytesWritten = ByteStreams.copy(is, os);
            is.close();
            os.close();

            if (bytesWritten != length - tempFileSize) {
                throw new IOException(
                        tempFileName
                                + ": unexpected data written.  expected = "
                                + (length - tempFileSize)
                                + ", written = "
                                + bytesWritten);
            }
            Files.move(tempFilePath, filePath, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }

    /**
     * Copy a source object into a new object with the provided name in the provided bucket.
     * optionally can take a key value CopyConditions and server side encryption as well for
     * conditionally attempting copyObject.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code minioClient.copyObject("my-bucketname", "my-objectname", headers, sse, "my-srcbucketname","my-srcobjname", srcSse, copyConditions);}
     * </pre>
     *
     * @param bucketName Destination bucket name.
     * @param objectName Destination object name.
     * @param headerMap Destination object custom metadata.
     * @param sse Server side encryption of destination object.
     * @param srcBucketName Source bucket name.
     * @param srcObjectName Source object name.
     * @param srcSse Server side encryption of source object.
     * @param copyConditions CopyConditions object with collection of supported CopyObject conditions.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void copyObject(
            String bucketName,
            String objectName,
            Map<String, String> headerMap,
            ServerSideEncryption sse,
            String srcBucketName,
            String srcObjectName,
            ServerSideEncryption srcSse,
            CopyConditions copyConditions)
            throws InvalidKeyException, InvalidBucketNameException, NoSuchAlgorithmException,
            InsufficientDataException, ErrorResponseException, InternalException, IOException,
            XmlParserException, IllegalArgumentException, InvalidResponseException {
        if ((bucketName == null) || (bucketName.isEmpty())) {
            throw new IllegalArgumentException("bucket name cannot be empty");
        }

        checkObjectName(objectName);

        checkWriteRequestSse(sse);

        if ((srcBucketName == null) || (srcBucketName.isEmpty())) {
            throw new IllegalArgumentException("Source bucket name cannot be empty");
        }

        // Source object name is optional, if empty default to object name.
        if (srcObjectName == null) {
            srcObjectName = objectName;
        }

        checkReadRequestSse(srcSse);

        if (headerMap == null) {
            headerMap = new HashMap<>();
        }

        headerMap.put("x-amz-copy-source", MyS3Escaper.encodePath(srcBucketName + "/" + srcObjectName));

        if (sse != null) {
            headerMap.putAll(sse.headers());
        }

        if (srcSse != null) {
            headerMap.putAll(srcSse.copySourceHeaders());
        }

        if (copyConditions != null) {
            headerMap.putAll(copyConditions.getConditions());
        }

        Response response = executePut(bucketName, objectName, headerMap, null, "", 0);

        try (ResponseBody body = response.body()) {
            // For now ignore the copyObjectResult, just read and parse it.
            Xml.unmarshal(CopyObjectResult.class, body.charStream());
        }
    }

    /**
     * Create an object by concatenating a list of source objects using server-side copying.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code minioClient.composeObject("my-bucketname", "my-objectname", composeSources, userMetaData, sse);}
     * </pre>
     *
     * @param bucketName Destination Bucket to be created upon compose.
     * @param objectName Destination Object to be created upon compose.
     * @param sources List of Source Objects used to compose Object.
     * @param headerMap User Meta data.
     * @param sse Server Side Encryption.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void composeObject(
            String bucketName,
            String objectName,
            List<ComposeSource> sources,
            Map<String, String> headerMap,
            ServerSideEncryption sse)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, XmlParserException, ErrorResponseException,
            InternalException, IllegalArgumentException, InvalidResponseException {
        if ((bucketName == null) || (bucketName.isEmpty())) {
            throw new IllegalArgumentException("bucket name cannot be empty");
        }

        checkObjectName(objectName);

        if (sources.isEmpty()) {
            throw new IllegalArgumentException("compose sources cannot be empty");
        }

        checkWriteRequestSse(sse);

        long objectSize = 0;
        int partsCount = 0;
        for (int i = 0; i < sources.size(); i++) {
            ComposeSource src = sources.get(i);

            checkReadRequestSse(src.sse());

            ObjectStat stat = statObject(src.bucketName(), src.objectName(), src.sse());
            src.buildHeaders(stat.length(), stat.etag());

            if (i != 0 && src.headers().containsKey("x-amz-meta-x-amz-key")) {
                throw new IllegalArgumentException(
                        "Client side encryption is not supported for more than one source");
            }

            long size = stat.length();
            if (src.length() != null) {
                size = src.length();
            } else if (src.offset() != null) {
                size -= src.offset();
            }

            if (size < PutObjectOptions.MIN_MULTIPART_SIZE
                    && sources.size() != 1
                    && i != (sources.size() - 1)) {
                throw new IllegalArgumentException(
                        "source "
                                + src.bucketName()
                                + "/"
                                + src.objectName()
                                + ": size "
                                + size
                                + " must be greater than "
                                + PutObjectOptions.MIN_MULTIPART_SIZE);
            }

            objectSize += size;
            if (objectSize > PutObjectOptions.MAX_OBJECT_SIZE) {
                throw new IllegalArgumentException(
                        "Destination object size must be less than " + PutObjectOptions.MAX_OBJECT_SIZE);
            }

            if (size > PutObjectOptions.MAX_PART_SIZE) {
                long count = size / PutObjectOptions.MAX_PART_SIZE;
                long lastPartSize = size - (count * PutObjectOptions.MAX_PART_SIZE);
                if (lastPartSize > 0) {
                    count++;
                } else {
                    lastPartSize = PutObjectOptions.MAX_PART_SIZE;
                }

                if (lastPartSize < PutObjectOptions.MIN_MULTIPART_SIZE
                        && sources.size() != 1
                        && i != (sources.size() - 1)) {
                    throw new IllegalArgumentException(
                            "source "
                                    + src.bucketName()
                                    + "/"
                                    + src.objectName()
                                    + ": "
                                    + "for multipart split upload of "
                                    + size
                                    + ", last part size is less than "
                                    + PutObjectOptions.MIN_MULTIPART_SIZE);
                }

                partsCount += (int) count;
            } else {
                partsCount++;
            }

            if (partsCount > PutObjectOptions.MAX_MULTIPART_COUNT) {
                throw new IllegalArgumentException(
                        "Compose sources create more than allowed multipart count "
                                + PutObjectOptions.MAX_MULTIPART_COUNT);
            }
        }

        if (partsCount == 1) {
            ComposeSource src = sources.get(0);
            if (headerMap == null) {
                headerMap = new HashMap<>();
            }
            if ((src.offset() != null) && (src.length() == null)) {
                headerMap.put("x-amz-copy-source-range", "bytes=" + src.offset() + "-");
            }

            if ((src.offset() != null) && (src.length() != null)) {
                headerMap.put(
                        "x-amz-copy-source-range",
                        "bytes=" + src.offset() + "-" + (src.offset() + src.length() - 1));
            }
            copyObject(
                    bucketName,
                    objectName,
                    headerMap,
                    sse,
                    src.bucketName(),
                    src.objectName(),
                    src.sse(),
                    src.copyConditions());
            return;
        }

        Map<String, String> sseHeaders = null;
        if (sse != null) {
            sseHeaders = sse.headers();
            if (headerMap == null) {
                headerMap = new HashMap<>();
            }
            headerMap.putAll(sseHeaders);
        }

        String uploadId = initMultipartUpload(bucketName, objectName, headerMap);

        int partNumber = 0;
        Part[] totalParts = new Part[partsCount];
        try {
            for (int i = 0; i < sources.size(); i++) {
                ComposeSource src = sources.get(i);

                long size = src.objectSize();
                if (src.length() != null) {
                    size = src.length();
                } else if (src.offset() != null) {
                    size -= src.offset();
                }
                long offset = 0;
                if (src.offset() != null) {
                    offset = src.offset();
                }

                if (size <= PutObjectOptions.MAX_PART_SIZE) {
                    partNumber++;
                    Map<String, String> headers = new HashMap<>();
                    if (src.headers() != null) {
                        headers.putAll(src.headers());
                    }
                    if (src.length() != null) {
                        headers.put(
                                "x-amz-copy-source-range", "bytes=" + offset + "-" + (offset + src.length() - 1));
                    } else if (src.offset() != null) {
                        headers.put("x-amz-copy-source-range", "bytes=" + offset + "-" + (offset + size - 1));
                    }
                    if (sseHeaders != null) {
                        headers.putAll(sseHeaders);
                    }
                    String eTag = uploadPartCopy(bucketName, objectName, uploadId, partNumber, headers);

                    totalParts[partNumber - 1] = new Part(partNumber, eTag);
                    continue;
                }

                while (size > 0) {
                    partNumber++;

                    long startBytes = offset;
                    long endBytes = startBytes + PutObjectOptions.MAX_PART_SIZE;
                    if (size < PutObjectOptions.MAX_PART_SIZE) {
                        endBytes = startBytes + size;
                    }

                    Map<String, String> headers = src.headers();
                    headers.put("x-amz-copy-source-range", "bytes=" + startBytes + "-" + endBytes);
                    if (sseHeaders != null) {
                        headers.putAll(sseHeaders);
                    }
                    String eTag = uploadPartCopy(bucketName, objectName, uploadId, partNumber, headers);

                    totalParts[partNumber - 1] = new Part(partNumber, eTag);

                    offset = startBytes;
                    size -= (endBytes - startBytes);
                }
            }

            completeMultipart(bucketName, objectName, uploadId, totalParts);
        } catch (RuntimeException e) {
            abortMultipartUpload(bucketName, objectName, uploadId);
            throw e;
        } catch (Exception e) {
            abortMultipartUpload(bucketName, objectName, uploadId);
            throw e;
        }
    }

    /**
     * Do UploadPartCopy as per
     * https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPartCopy.html
     */
    public String uploadPartCopy(
            String bucketName,
            String objectName,
            String uploadId,
            int partNumber,
            Map<String, String> headerMap)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("partNumber", Integer.toString(partNumber));
        queryParamMap.put("uploadId", uploadId);
        Response response = executePut(bucketName, objectName, headerMap, queryParamMap, "", 0);
        try (ResponseBody body = response.body()) {
            CopyPartResult result = Xml.unmarshal(CopyPartResult.class, body.charStream());
            return result.etag();
        }
    }

    /**
     * Returns a presigned URL string with given HTTP method, expiry time and custom request params
     * for a specific object in the bucket.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code String url = minioClient.getPresignedObjectUrl(Method.DELETE, "my-bucketname", "my-objectname", 60 * 60 * 24, reqParams);}
     * </pre>
     *
     * @param method HTTP {@link Method}.
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @param expires Expiration time in seconds of presigned URL.
     * @param reqParams Override values for set of response headers. Currently supported request
     *     parameters are [response-expires, response-content-type, response-cache-control,
     *     response-content-disposition]
     * @return string contains URL to download the object.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidExpiresRangeException upon input expires is out of range
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public String getPresignedObjectUrl(
            Method method,
            String bucketName,
            String objectName,
            Integer expires,
            Map<String, String> reqParams)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidExpiresRangeException,
            InvalidResponseException {
        // Validate input.
        if (expires < 1 || expires > DEFAULT_EXPIRY_TIME) {
            throw new InvalidExpiresRangeException(
                    expires, "expires must be in range of 1 to " + DEFAULT_EXPIRY_TIME);
        }

        byte[] body = null;
        if (method == Method.PUT || method == Method.POST) {
            body = new byte[0];
        }

        Multimap<String, String> queryParamMap = null;
        if (reqParams != null) {
            queryParamMap = HashMultimap.create();
            for (Map.Entry<String, String> m : reqParams.entrySet()) {
                queryParamMap.put(m.getKey(), m.getValue());
            }
        }

        String region = getRegion(bucketName);
        HttpUrl url = buildUrl(method, bucketName, objectName, region, queryParamMap);
        Request request = createRequest(url, method, null, body, 0);
        url = MySigner.presignV4(request, region, accessKey, secretKey, expires);
        return url.toString();
    }

    /**
     * Returns an presigned URL to download the object in the bucket with given expiry time with
     * custom request params.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code String url = minioClient.presignedGetObject("my-bucketname", "my-objectname", 60 * 60 * 24, reqParams);}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @param expires Expiration time in seconds of presigned URL.
     * @param reqParams Override values for set of response headers. Currently supported request
     *     parameters are [response-expires, response-content-type, response-cache-control,
     *     response-content-disposition]
     * @return string contains URL to download the object.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidExpiresRangeException upon input expires is out of range
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public String presignedGetObject(
            String bucketName, String objectName, Integer expires, Map<String, String> reqParams)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidExpiresRangeException,
            InvalidResponseException {
        return getPresignedObjectUrl(Method.GET, bucketName, objectName, expires, reqParams);
    }

    /**
     * Returns an presigned URL to download the object in the bucket with given expiry time.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code String url = minioClient.presignedGetObject("my-bucketname", "my-objectname", 60 * 60 * 24);}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @param expires Expiration time in seconds of presigned URL.
     * @return string contains URL to download the object.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidExpiresRangeException upon input expires is out of range
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public String presignedGetObject(String bucketName, String objectName, Integer expires)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidExpiresRangeException,
            InvalidResponseException {
        return presignedGetObject(bucketName, objectName, expires, null);
    }

    /**
     * Returns an presigned URL to download the object in the bucket with default expiry time. Default
     * expiry time is 7 days in seconds.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code String url = minioClient.presignedGetObject("my-bucketname", "my-objectname");}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @return string contains URL to download the object
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidExpiresRangeException upon input expires is out of range
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public String presignedGetObject(String bucketName, String objectName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidExpiresRangeException,
            InvalidResponseException {
        return presignedGetObject(bucketName, objectName, DEFAULT_EXPIRY_TIME, null);
    }

    /**
     * Returns a presigned URL to upload an object in the bucket with given expiry time.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code String url = minioClient.presignedPutObject("my-bucketname", "my-objectname", 60 * 60 * 24);}
     * </pre>
     *
     * @param bucketName Bucket name
     * @param objectName Object name in the bucket
     * @param expires Expiration time in seconds to presigned URL.
     * @return string contains URL to upload the object.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidExpiresRangeException upon input expires is out of range
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public String presignedPutObject(String bucketName, String objectName, Integer expires)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidExpiresRangeException,
            InvalidResponseException {
        return getPresignedObjectUrl(Method.PUT, bucketName, objectName, expires, null);
    }

    /**
     * Returns a presigned URL to upload an object in the bucket with default expiry time. Default
     * expiry time is 7 days in seconds.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code String url = minioClient.presignedPutObject("my-bucketname", "my-objectname");}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @return string contains URL to upload the object.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidExpiresRangeException upon input expires is out of range
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public String presignedPutObject(String bucketName, String objectName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidExpiresRangeException,
            InvalidResponseException {
        return presignedPutObject(bucketName, objectName, DEFAULT_EXPIRY_TIME);
    }

    /**
     * Returns string map for given {@link PostPolicy} to upload object with various post policy
     * conditions.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code PostPolicy policy = new PostPolicy("my-bucketname", "my-objectname", ZonedDateTime.now().plusDays(7));
     *
     * // 'my-objectname' should be 'image/png' content type
     * policy.setContentType("image/png");
     *
     * // set success action status to 201 to receive XML document
     * policy.setSuccessActionStatus(201);
     *
     * Map<String,String> formData = minioClient.presignedPostPolicy(policy);
     *
     * // Print curl command to be executed by anonymous user to upload /tmp/userpic.png.
     * System.out.print("curl -X POST ");
     * for (Map.Entry<String,String> entry : formData.entrySet()) {
     *   System.out.print(" -F " + entry.getKey() + "=" + entry.getValue());
     * }
     * System.out.println(" -F file=@/tmp/userpic.png https://play.min.io/my-bucketname");}</pre>
     *
     * @param policy Post policy of an object.
     * @return Map of strings to construct form-data.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     * @see PostPolicy
     */
    public Map<String, String> presignedPostPolicy(PostPolicy policy)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, XmlParserException, ErrorResponseException,
            InternalException, IllegalArgumentException, InvalidResponseException {
        return policy.formData(this.accessKey, this.secretKey, getRegion(policy.bucketName()));
    }

    /**
     * Removes an object from a bucket.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code minioClient.removeObject("my-bucketname", "my-objectname");}</pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void removeObject(String bucketName, String objectName)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, XmlParserException, ErrorResponseException,
            InternalException, IllegalArgumentException, InvalidResponseException {
        if ((bucketName == null) || (bucketName.isEmpty())) {
            throw new IllegalArgumentException("bucket name cannot be empty");
        }

        checkObjectName(objectName);

        executeDelete(bucketName, objectName, null);
    }

    /**
     * Do DeleteObjects as per https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObjects.html
     */
    public List<DeleteError> removeObject(String bucketName, List<DeleteObject> objectList)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, XmlParserException, ErrorResponseException,
            InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("delete", "");

        DeleteRequest request = new DeleteRequest(objectList, true);
        Response response = executePost(bucketName, null, null, queryParamMap, request);

        String bodyContent = "";
        try (ResponseBody body = response.body()) {
            bodyContent = new String(body.bytes(), StandardCharsets.UTF_8);
        }

        List<DeleteError> errorList = null;

        try {
            if (Xml.validate(DeleteError.class, bodyContent)) {
                DeleteError error = Xml.unmarshal(DeleteError.class, bodyContent);
                errorList = new LinkedList<DeleteError>();
                errorList.add(error);
                return errorList;
            }
        } catch (XmlParserException e) {
            // As it is not <Error> message, parse it as <DeleteResult> message.
            // Ignore this exception
        }

        DeleteResult result = Xml.unmarshal(DeleteResult.class, bodyContent);
        return result.errorList();
    }

    /**
     * Removes multiple objects from a bucket. As objects removal are lazily executed, its required to
     * iterate the returned Iterable.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code List<String> myObjectNames = new LinkedList<String>();
     * objectNames.add("my-objectname1");
     * objectNames.add("my-objectname2");
     * objectNames.add("my-objectname3");
     * Iterable<Result<DeleteError>> results = minioClient.removeObjects("my-bucketname", myObjectNames);
     * for (Result<DeleteError> result : results) {
     *   DeleteError error = errorResult.get();
     *   System.out.println("Error in deleting object " + error.objectName() + "; " + error.message());
     * }}</pre>
     *
     * @param bucketName Bucket name.
     * @param objectNames List of Object names in the bucket.
     * @return (lazy) Iterable of the Result DeleteErrors.
     */
    public Iterable<Result<DeleteError>> removeObjects(
            final String bucketName, final Iterable<String> objectNames) {
        return new Iterable<Result<DeleteError>>() {
            @Override
            public Iterator<Result<DeleteError>> iterator() {
                return new Iterator<Result<DeleteError>>() {
                    private Result<DeleteError> error;
                    private Iterator<DeleteError> errorIterator;
                    private boolean completed = false;
                    private Iterator<String> objectNameIter = objectNames.iterator();

                    private synchronized void populate() {
                        List<DeleteError> errorList = null;
                        try {
                            List<DeleteObject> objectList = new LinkedList<DeleteObject>();
                            int i = 0;
                            while (objectNameIter.hasNext() && i < 1000) {
                                objectList.add(new DeleteObject(objectNameIter.next()));
                                i++;
                            }

                            if (i > 0) {
                                errorList = removeObject(bucketName, objectList);
                            }
                        } catch (InvalidBucketNameException
                                | NoSuchAlgorithmException
                                | InsufficientDataException
                                | IOException
                                | InvalidKeyException
                                | XmlParserException
                                | ErrorResponseException
                                | InternalException
                                | InvalidResponseException
                                | IllegalArgumentException e) {
                            this.error = new Result<>(e);
                        } finally {
                            if (errorList != null) {
                                this.errorIterator = errorList.iterator();
                            } else {
                                this.errorIterator = new LinkedList<DeleteError>().iterator();
                            }
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        if (this.completed) {
                            return false;
                        }

                        if (this.error == null && this.errorIterator == null) {
                            populate();
                        }

                        if (this.error == null && this.errorIterator != null && !this.errorIterator.hasNext()) {
                            populate();
                        }

                        if (this.error != null) {
                            return true;
                        }

                        if (this.errorIterator.hasNext()) {
                            return true;
                        }

                        this.completed = true;
                        return false;
                    }

                    @Override
                    public Result<DeleteError> next() {
                        if (this.completed) {
                            throw new NoSuchElementException();
                        }

                        if (this.error == null && this.errorIterator == null) {
                            populate();
                        }

                        if (this.error == null && this.errorIterator != null && !this.errorIterator.hasNext()) {
                            populate();
                        }

                        if (this.error != null) {
                            this.completed = true;
                            return this.error;
                        }

                        if (this.errorIterator.hasNext()) {
                            return new Result<>(this.errorIterator.next());
                        }

                        this.completed = true;
                        throw new NoSuchElementException();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * Lists object information in given bucket.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code Iterable<Result<Item>> results = minioClient.listObjects("my-bucketname");
     * for (Result<Item> result : results) {
     *   Item item = result.get();
     *   System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
     * }}</pre>
     *
     * @param bucketName Bucket name.
     * @return an iterator of Result Items.
     * @throws XmlParserException upon parsing response xml
     */
    public Iterable<Result<Item>> listObjects(final String bucketName) throws XmlParserException {
        return listObjects(bucketName, null);
    }

    /**
     * Lists object information in given bucket and prefix.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code Iterable<Result<Item>> results = minioClient.listObjects("my-bucketname", "my-obj");
     * for (Result<Item> result : results) {
     *   Item item = result.get();
     *   System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
     * }}</pre>
     *
     * @param bucketName Bucket name.
     * @param prefix Prefix string. List objects whose name starts with `prefix`.
     * @return an iterator of Result Items.
     * @throws XmlParserException upon parsing response xml
     */
    public Iterable<Result<Item>> listObjects(final String bucketName, final String prefix)
            throws XmlParserException {
        // list all objects recursively
        return listObjects(bucketName, prefix, true);
    }

    /**
     * Lists object information as {@code Iterable<Result><Item>} in given bucket, prefix and
     * recursive flag.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code Iterable<Result<Item>> results = minioClient.listObjects("my-bucketname", "my-obj", true);
     * for (Result<Item> result : results) {
     *   Item item = result.get();
     *   System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
     * }}</pre>
     *
     * @param bucketName Bucket name.
     * @param prefix Prefix string. List objects whose name starts with `prefix`.
     * @param recursive when false, emulates a directory structure where each listing returned is
     *     either a full object or part of the object's key up to the first '/'. All objects wit the
     *     same prefix up to the first '/' will be merged into one entry.
     * @return an iterator of Result Items.
     * @see #listObjects(String bucketName)
     * @see #listObjects(String bucketName, String prefix)
     * @see #listObjects(String bucketName, String prefix, boolean recursive, boolean useVersion1)
     */
    public Iterable<Result<Item>> listObjects(
            final String bucketName, final String prefix, final boolean recursive) {
        return listObjects(bucketName, prefix, recursive, false);
    }

    /**
     * Lists object information as {@code Iterable<Result><Item>} in given bucket, prefix, recursive
     * flag and S3 API version to use.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code Iterable<Result<Item>> results = minioClient.listObjects("my-bucketname", "my-obj", true, true);
     * for (Result<Item> result : results) {
     *   Item item = result.get();
     *   System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
     * }}</pre>
     *
     * @param bucketName Bucket name.
     * @param prefix Prefix string. List objects whose name starts with `prefix`.
     * @param recursive when false, emulates a directory structure where each listing returned is
     *     either a full object or part of the object's key up to the first '/'. All objects wit the
     *     same prefix up to the first '/' will be merged into one entry.
     * @param useVersion1 If set, Amazon AWS S3 List Object V1 is used, else List Object V2 is used as
     *     default.
     * @return an iterator of Result Items.
     * @see #listObjects(String bucketName)
     * @see #listObjects(String bucketName, String prefix)
     * @see #listObjects(String bucketName, String prefix, boolean recursive)
     */
    public Iterable<Result<Item>> listObjects(
            final String bucketName,
            final String prefix,
            final boolean recursive,
            final boolean useVersion1) {
        return listObjects(bucketName, prefix, recursive, false, false);
    }

    /**
     * Lists object information as {@code Iterable<Result><Item>} in given bucket, prefix, recursive
     * flag, user metadata flag and S3 API version to use.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code Iterable<Result<Item>> results = minioClient.listObjects("my-bucketname", "my-obj", true, true, false);
     * for (Result<Item> result : results) {
     *   Item item = result.get();
     *   System.out.println(item.lastModified() + ", " + item.size() + ", " + item.objectName());
     * }}</pre>
     *
     * @param bucketName Bucket name.
     * @param prefix Prefix string. List objects whose name starts with `prefix`.
     * @param recursive when false, emulates a directory structure where each listing returned is
     *     either a full object or part of the object's key up to the first '/'. All objects wit the
     *     same prefix up to the first '/' will be merged into one entry.
     * @param includeUserMetadata include user metadata of each object. This is MinIO specific
     *     extension to ListObjectsV2.
     * @param useVersion1 If set, Amazon AWS S3 List Object V1 is used, else List Object V2 is used as
     *     default.
     * @return an iterator of Result Items.
     * @see #listObjects(String bucketName)
     * @see #listObjects(String bucketName, String prefix)
     * @see #listObjects(String bucketName, String prefix, boolean recursive)
     */
    public Iterable<Result<Item>> listObjects(
            final String bucketName,
            final String prefix,
            final boolean recursive,
            final boolean includeUserMetadata,
            final boolean useVersion1) {
        if (useVersion1) {
            if (includeUserMetadata) {
                throw new IllegalArgumentException(
                        "include user metadata flag is not supported in version 1");
            }

            return listObjectsV1(bucketName, prefix, recursive);
        }

        return listObjectsV2(bucketName, prefix, recursive, includeUserMetadata);
    }

    public Iterable<Result<Item>> listObjectsV2(
            final String bucketName,
            final String prefix,
            final boolean recursive,
            final boolean includeUserMetadata) {
        return new Iterable<Result<Item>>() {
            @Override
            public Iterator<Result<Item>> iterator() {
                return new Iterator<Result<Item>>() {
                    private ListBucketResult listBucketResult;
                    private Result<Item> error;
                    private Iterator<Item> itemIterator;
                    private Iterator<Prefix> prefixIterator;
                    private boolean completed = false;

                    private synchronized void populate() {
                        String delimiter = "/";
                        if (recursive) {
                            delimiter = null;
                        }

                        String continuationToken = null;
                        if (this.listBucketResult != null) {
                            continuationToken = listBucketResult.nextContinuationToken();
                        }

                        this.listBucketResult = null;
                        this.itemIterator = null;
                        this.prefixIterator = null;

                        try {
                            this.listBucketResult =
                                    listObjectsV2(
                                            bucketName, continuationToken, prefix, delimiter, includeUserMetadata);
                        } catch (InvalidBucketNameException
                                | NoSuchAlgorithmException
                                | InsufficientDataException
                                | IOException
                                | InvalidKeyException
                                | XmlParserException
                                | ErrorResponseException
                                | InternalException
                                | InvalidResponseException
                                | IllegalArgumentException e) {
                            this.error = new Result<>(e);
                        } finally {
                            if (this.listBucketResult != null) {
                                this.itemIterator = this.listBucketResult.contents().iterator();
                                this.prefixIterator = this.listBucketResult.commonPrefixes().iterator();
                            } else {
                                this.itemIterator = new LinkedList<Item>().iterator();
                                this.prefixIterator = new LinkedList<Prefix>().iterator();
                            }
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        if (this.completed) {
                            return false;
                        }

                        if (this.error == null && this.itemIterator == null && this.prefixIterator == null) {
                            populate();
                        }

                        if (this.error == null
                                && !this.itemIterator.hasNext()
                                && !this.prefixIterator.hasNext()
                                && this.listBucketResult.isTruncated()) {
                            populate();
                        }

                        if (this.error != null) {
                            return true;
                        }

                        if (this.itemIterator.hasNext()) {
                            return true;
                        }

                        if (this.prefixIterator.hasNext()) {
                            return true;
                        }

                        this.completed = true;
                        return false;
                    }

                    @Override
                    public Result<Item> next() {
                        if (this.completed) {
                            throw new NoSuchElementException();
                        }

                        if (this.error == null && this.itemIterator == null && this.prefixIterator == null) {
                            populate();
                        }

                        if (this.error == null
                                && !this.itemIterator.hasNext()
                                && !this.prefixIterator.hasNext()
                                && this.listBucketResult.isTruncated()) {
                            populate();
                        }

                        if (this.error != null) {
                            this.completed = true;
                            return this.error;
                        }

                        if (this.itemIterator.hasNext()) {
                            Item item = this.itemIterator.next();
                            return new Result<>(item);
                        }

                        if (this.prefixIterator.hasNext()) {
                            return new Result<>(this.prefixIterator.next().toItem());
                        }

                        this.completed = true;
                        throw new NoSuchElementException();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * Do ListObjectsV2 as per https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjectsV2.html
     */
    public ListBucketResult listObjectsV2(
            String bucketName,
            String continuationToken,
            String prefix,
            String delimiter,
            boolean includeUserMetadata)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("list-type", "2");

        if (continuationToken != null) {
            queryParamMap.put("continuation-token", continuationToken);
        }

        if (prefix != null) {
            queryParamMap.put("prefix", prefix);
        } else {
            queryParamMap.put("prefix", "");
        }

        if (delimiter != null) {
            queryParamMap.put("delimiter", delimiter);
        } else {
            queryParamMap.put("delimiter", "");
        }

        if (includeUserMetadata) {
            queryParamMap.put("metadata", "true");
        }

        Response response = executeGet(bucketName, null, null, queryParamMap);

        try (ResponseBody body = response.body()) {
            return Xml.unmarshal(ListBucketResult.class, body.charStream());
        }
    }

    public Iterable<Result<Item>> listObjectsV1(
            final String bucketName, final String prefix, final boolean recursive) {
        return new Iterable<Result<Item>>() {
            @Override
            public Iterator<Result<Item>> iterator() {
                return new Iterator<Result<Item>>() {
                    private String lastObjectName;
                    private ListBucketResultV1 listBucketResult;
                    private Result<Item> error;
                    private Iterator<Item> itemIterator;
                    private Iterator<Prefix> prefixIterator;
                    private boolean completed = false;

                    private synchronized void populate() {
                        String delimiter = "/";
                        if (recursive) {
                            delimiter = null;
                        }

                        String marker = null;
                        if (this.listBucketResult != null) {
                            if (delimiter != null) {
                                marker = listBucketResult.nextMarker();
                            } else {
                                marker = this.lastObjectName;
                            }
                        }

                        this.listBucketResult = null;
                        this.itemIterator = null;
                        this.prefixIterator = null;

                        try {
                            this.listBucketResult = listObjectsV1(bucketName, marker, prefix, delimiter);
                        } catch (InvalidBucketNameException
                                | NoSuchAlgorithmException
                                | InsufficientDataException
                                | IOException
                                | InvalidKeyException
                                | XmlParserException
                                | ErrorResponseException
                                | InternalException
                                | InvalidResponseException e) {
                            this.error = new Result<>(e);
                        } finally {
                            if (this.listBucketResult != null) {
                                this.itemIterator = this.listBucketResult.contents().iterator();
                                this.prefixIterator = this.listBucketResult.commonPrefixes().iterator();
                            } else {
                                this.itemIterator = new LinkedList<Item>().iterator();
                                this.prefixIterator = new LinkedList<Prefix>().iterator();
                            }
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        if (this.completed) {
                            return false;
                        }

                        if (this.error == null && this.itemIterator == null && this.prefixIterator == null) {
                            populate();
                        }

                        if (this.error == null
                                && !this.itemIterator.hasNext()
                                && !this.prefixIterator.hasNext()
                                && this.listBucketResult.isTruncated()) {
                            populate();
                        }

                        if (this.error != null) {
                            return true;
                        }

                        if (this.itemIterator.hasNext()) {
                            return true;
                        }

                        if (this.prefixIterator.hasNext()) {
                            return true;
                        }

                        this.completed = true;
                        return false;
                    }

                    @Override
                    public Result<Item> next() {
                        if (this.completed) {
                            throw new NoSuchElementException();
                        }

                        if (this.error == null && this.itemIterator == null && this.prefixIterator == null) {
                            populate();
                        }

                        if (this.error == null
                                && !this.itemIterator.hasNext()
                                && !this.prefixIterator.hasNext()
                                && this.listBucketResult.isTruncated()) {
                            populate();
                        }

                        if (this.error != null) {
                            this.completed = true;
                            return this.error;
                        }

                        if (this.itemIterator.hasNext()) {
                            Item item = this.itemIterator.next();
                            this.lastObjectName = item.objectName();
                            return new Result<>(item);
                        }

                        if (this.prefixIterator.hasNext()) {
                            return new Result<>(this.prefixIterator.next().toItem());
                        }

                        this.completed = true;
                        throw new NoSuchElementException();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /** Do ListObjects as per https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html */
    public ListBucketResultV1 listObjectsV1(
            String bucketName, String marker, String prefix, String delimiter)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();

        if (marker != null) {
            queryParamMap.put("marker", marker);
        }

        if (prefix != null) {
            queryParamMap.put("prefix", prefix);
        } else {
            queryParamMap.put("prefix", "");
        }

        if (delimiter != null) {
            queryParamMap.put("delimiter", delimiter);
        } else {
            queryParamMap.put("delimiter", "");
        }

        Response response = executeGet(bucketName, null, null, queryParamMap);

        try (ResponseBody body = response.body()) {
            return Xml.unmarshal(ListBucketResultV1.class, body.charStream());
        }
    }

    /**
     * Returns all bucket information owned by the current user.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code List<Bucket> bucketList = minioClient.listBuckets();
     * for (Bucket bucket : bucketList) {
     *   System.out.println(bucket.creationDate() + ", " + bucket.name());
     * }}</pre>
     *
     * @return List of bucket type.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public List<Bucket> listBuckets()
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Response response = executeGet(null, null, (Multimap<String, String>) null);
        try (ResponseBody body = response.body()) {
            ListAllMyBucketsResult result =
                    Xml.unmarshal(ListAllMyBucketsResult.class, body.charStream());
            return result.buckets();
        }
    }

    /**
     * Checks if given bucket exist and is having read access.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code boolean found = minioClient.bucketExists("my-bucketname");
     * if (found) {
     *   System.out.println("my-bucketname exists");
     * } else {
     *   System.out.println("my-bucketname does not exist");
     * }}</pre>
     *
     * @param bucketName Bucket name.
     * @return True if the bucket exists and the user has at least read access.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public boolean bucketExists(String bucketName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        try {
            executeHead(bucketName, null);
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().errorCode() != ErrorCode.NO_SUCH_BUCKET) {
                throw e;
            }
        }

        return false;
    }

    /**
     * Creates a bucket with default region.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code boolean found = minioClient.makeBucket("my-bucketname");}</pre>
     *
     * @param bucketName Bucket name.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws RegionConflictException upon passed region conflicts with the one previously specified
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution.
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InsufficientDataException upon getting EOFException while reading given
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void makeBucket(String bucketName)
            throws InvalidBucketNameException, IllegalArgumentException, RegionConflictException,
            NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
        this.makeBucket(bucketName, null, false);
    }

    /**
     * Creates a bucket with given region.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code boolean found = minioClient.makeBucket("my-bucketname", "eu-west-1");}</pre>
     *
     * @param bucketName Bucket name.
     * @param region region in which the bucket will be created.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws RegionConflictException upon passed region conflicts with the one previously specified.
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InsufficientDataException upon getting EOFException while reading given
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void makeBucket(String bucketName, String region)
            throws InvalidBucketNameException, IllegalArgumentException, RegionConflictException,
            NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
        this.makeBucket(bucketName, region, false);
    }

    /**
     * Creates a bucket with given region and object lock option.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code boolean found = minioClient.makeBucket("my-bucketname", "eu-west-2", true);}</pre>
     *
     * @param bucketName Bucket name.
     * @param region region in which the bucket will be created.
     * @param objectLock enable object lock support.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws RegionConflictException upon passed region conflicts with the one previously specified.
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InsufficientDataException upon getting EOFException while reading given
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void makeBucket(String bucketName, String region, boolean objectLock)
            throws InvalidBucketNameException, IllegalArgumentException, RegionConflictException,
            NoSuchAlgorithmException, InsufficientDataException, IOException, InvalidKeyException,
            XmlParserException, ErrorResponseException, InternalException, InvalidResponseException {
        // If region param is not provided, set it with the one provided by constructor
        if (region == null) {
            region = this.region;
        }

        // If constructor already sets a region, check if it is equal to region param if provided
        if (this.region != null && !this.region.equals(region)) {
            throw new RegionConflictException(
                    "passed region conflicts with the one previously specified");
        }

        if (region == null) {
            region = US_EAST_1;
        }

        CreateBucketConfiguration config = null;
        if (!region.equals(US_EAST_1)) {
            config = new CreateBucketConfiguration(region);
        }

        Map<String, String> headerMap = null;
        if (objectLock) {
            headerMap = new HashMap<>();
            headerMap.put("x-amz-bucket-object-lock-enabled", "true");
        }

        Response response = executePut(bucketName, null, region, headerMap, null, config, 0);
        response.body().close();
    }

    /**
     * Enable object versioning in given bucket.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code minioClient.enableVersioning("my-bucketname");}</pre>
     *
     * @param bucketName Bucket name.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InsufficientDataException upon getting EOFException while reading given
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void enableVersioning(String bucketName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("versioning", "");
        String config =
                "<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
                        + "<Status>Enabled</Status></VersioningConfiguration>";
        Response response = executePut(bucketName, null, null, queryParamMap, config, 0);
        response.body().close();
    }

    /**
     * Disable object versioning in given bucket.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code minioClient.disableVersioning("my-bucketname");}</pre>
     *
     * @param bucketName Bucket name.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InsufficientDataException upon getting EOFException while reading given
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void disableVersioning(String bucketName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("versioning", "");
        String config =
                "<VersioningConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
                        + "<Status>Suspended</Status></VersioningConfiguration>";
        Response response = executePut(bucketName, null, null, queryParamMap, config, 0);
        response.body().close();
    }

    /**
     * Sets default object retention in given bucket.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code minioClient.setDefaultRetention("my-bucketname", config);}</pre>
     *
     * @param bucketName Bucket name.
     * @param config Object lock configuration.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void setDefaultRetention(String bucketName, ObjectLockConfiguration config)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("object-lock", "");

        Response response = executePut(bucketName, null, null, queryParamMap, config, 0);
        response.body().close();
    }

    /**
     * Gets default object retention in given bucket.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code ObjectLockConfiguration config = minioClient.getDefaultRetention("my-bucketname");}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public ObjectLockConfiguration getDefaultRetention(String bucketName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("object-lock", "");

        Response response = executeGet(bucketName, null, null, queryParamMap);

        try (ResponseBody body = response.body()) {
            return Xml.unmarshal(ObjectLockConfiguration.class, body.charStream());
        }
    }

    /**
     * Applies object retention lock onto an object.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code minioClient.setObjectRetention("my-bucketname", "my-object", null, config, true);}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name.
     * @param versionId Object versio id.
     * @param config Object lock configuration.
     * @param bypassGovernanceRetention By pass governance retention.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     */
    public void setObjectRetention(
            String bucketName,
            String objectName,
            String versionId,
            Retention config,
            boolean bypassGovernanceRetention)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException,
            IllegalArgumentException {

        if (config == null) {
            throw new IllegalArgumentException("null value is not allowed in config.");
        }

        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("retention", "");

        if (versionId == null) {
            queryParamMap.put("versionId", "");
        } else {
            queryParamMap.put("versionId", versionId);
        }

        Map<String, String> headerMap = new HashMap<>();
        if (bypassGovernanceRetention) {
            headerMap.put("x-amz-bypass-governance-retention", "True");
        }

        Response response = executePut(bucketName, objectName, headerMap, queryParamMap, config, 0);
        response.body().close();
    }

    /**
     * Fetches object retention lock of an object.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code Retention retention = s3Client.getObjectRetention("my-bucketname", "my-objectname", null);
     * System.out.println("mode: " + retention.mode() + "until: " + retention.retainUntilDate());}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name.
     * @param versionId Version Id.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public Retention getObjectRetention(String bucketName, String objectName, String versionId)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {

        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("retention", "");

        if (versionId == null) {
            queryParamMap.put("versionId", "");
        } else {
            queryParamMap.put("versionId", versionId);
        }

        Response response = executeGet(bucketName, objectName, null, queryParamMap);
        try (ResponseBody body = response.body()) {
            return Xml.unmarshal(Retention.class, body.charStream());
        }
    }

    /**
     * Enables object legal hold on an object.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code minioClient.enableObjectLegalHold("my-bucketname", "my-object", null);}</pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name.
     * @param versionId Object version id.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void enableObjectLegalHold(String bucketName, String objectName, String versionId)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {

        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("legal-hold", "");

        if (versionId == null) {
            queryParamMap.put("versionId", "");
        } else {
            queryParamMap.put("versionId", versionId);
        }

        LegalHold legalHold = new LegalHold(true);

        Response response = executePut(bucketName, objectName, null, queryParamMap, legalHold, 0);
        response.body().close();
    }

    /**
     * Disable object legal hold on an object.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code minioClient.disableObjectLegalHold("my-bucketname", "my-object", null);}</pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name.
     * @param versionId Object version id.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void disableObjectLegalHold(String bucketName, String objectName, String versionId)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {

        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("legal-hold", "");

        if (versionId == null) {
            queryParamMap.put("versionId", "");
        } else {
            queryParamMap.put("versionId", versionId);
        }

        LegalHold legalHold = new LegalHold(false);

        Response response = executePut(bucketName, objectName, null, queryParamMap, legalHold, 0);
        response.body().close();
    }

    /**
     * Returns true if the object legal hold is enabled.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code boolean status = s3Client.isObjectLegalHoldEnabled("my-bucketname", "my-objectname", null);
     * if (status) {
     *   System.out.println("Legal hold is on");
     * } else {
     *   System.out.println("Legal hold is off");
     * }}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name.
     * @param versionId Object version id.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public boolean isObjectLegalHoldEnabled(String bucketName, String objectName, String versionId)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {

        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("legal-hold", "");

        if (versionId == null) {
            queryParamMap.put("versionId", "");
        } else {
            queryParamMap.put("versionId", versionId);
        }
        Response response = executeGet(bucketName, objectName, null, queryParamMap);

        try (ResponseBody body = response.body()) {
            LegalHold result = Xml.unmarshal(LegalHold.class, body.charStream());
            return result.status();
        }
    }

    /**
     * Removes an empty bucket.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code minioClient.removeBucket("my-bucketname");}</pre>
     *
     * @param bucketName Bucket name.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InsufficientDataException upon getting EOFException while reading given
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void removeBucket(String bucketName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        executeDelete(bucketName, null, null);
    }

    /**
     * Do PutObject/UploadPart as per
     * https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html
     * https://docs.aws.amazon.com/AmazonS3/latest/API/API_UploadPart.html
     */
    public String putObject(
            String bucketName,
            String objectName,
            Object data,
            int length,
            Map<String, String> headerMap,
            String uploadId,
            int partNumber)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = null;
        if (partNumber > 0 && uploadId != null && !"".equals(uploadId)) {
            queryParamMap = new HashMap<>();
            queryParamMap.put("partNumber", Integer.toString(partNumber));
            queryParamMap.put(UPLOAD_ID, uploadId);
        }

        Response response = executePut(bucketName, objectName, headerMap, queryParamMap, data, length);
        response.close();
        return response.header("ETag").replaceAll("\"", "");
    }

    public void putObject(
            String bucketName, String objectName, PutObjectOptions options, Object data)
            throws InvalidBucketNameException, NoSuchAlgorithmException, IOException, InvalidKeyException,
            XmlParserException, ErrorResponseException, InternalException, IllegalArgumentException,
            InsufficientDataException, InvalidResponseException {
        Map<String, String> headerMap = new HashMap<>();

        if (options.headers() != null) {
            headerMap.putAll(options.headers());
        }

        if (options.sse() != null) {
            checkWriteRequestSse(options.sse());
            headerMap.putAll(options.sse().headers());
        }

        headerMap.put("Content-Type", options.contentType());

        // initiate new multipart upload.
        String uploadId = initMultipartUpload(bucketName, objectName, headerMap);

        long uploadedSize = 0L;
        int partCount = options.partCount();
        Part[] totalParts = new Part[PutObjectOptions.MAX_MULTIPART_COUNT];

        try {
            for (int partNumber = 1; partNumber <= partCount || partCount < 0; partNumber++) {
                long availableSize = options.partSize();
                if (partCount > 0) {
                    if (partNumber == partCount) {
                        availableSize = options.objectSize() - uploadedSize;
                    }
                } else {
                    availableSize = getAvailableSize(data, options.partSize() + 1);

                    // If availableSize is less or equal to options.partSize(), then we have reached last
                    // part.
                    if (availableSize <= options.partSize()) {
                        partCount = partNumber;
                    } else {
                        availableSize = options.partSize();
                    }
                }

                Map<String, String> ssecHeaders = null;
                // set encryption headers in the case of SSE-C.
                if (options.sse() != null && options.sse().type() == ServerSideEncryption.Type.SSE_C) {
                    ssecHeaders = options.sse().headers();
                }

                String etag =
                        putObject(
                                bucketName,
                                objectName,
                                data,
                                (int) availableSize,
                                ssecHeaders,
                                uploadId,
                                partNumber);
                totalParts[partNumber - 1] = new Part(partNumber, etag);
                uploadedSize += availableSize;
            }

            completeMultipart(bucketName, objectName, uploadId, totalParts);
        } catch (RuntimeException e) {
            abortMultipartUpload(bucketName, objectName, uploadId);
            throw e;
        } catch (Exception e) {
            abortMultipartUpload(bucketName, objectName, uploadId);
            throw e;
        }
    }

    /**
     * Uploads data from given file as object to given bucket using given PutObjectOptions. If any
     * error occurs, partial uploads are aborted.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code PutObjectOptions options = new PutObjectOptions(7003256, -1);
     * minioClient.putObject("my-bucketname", "my-objectname", "trip.mp4", options);}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name to create in the bucket.
     * @param filename Name of file to upload.
     * @param options Options to be used during object upload.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void putObject(
            String bucketName, String objectName, String filename, PutObjectOptions options)
            throws InvalidBucketNameException, NoSuchAlgorithmException, IOException, InvalidKeyException,
            XmlParserException, ErrorResponseException, InternalException, IllegalArgumentException,
            InsufficientDataException, InvalidResponseException {
        checkBucketName(bucketName);
        checkObjectName(objectName);

        if (filename == null || "".equals(filename)) {
            throw new IllegalArgumentException("empty filename is not allowed");
        }

        Path filePath = Paths.get(filename);
        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException(filename + " not a regular file");
        }

        long fileSize = Files.size(filePath);
        if (options == null) {
            options = new PutObjectOptions(fileSize, -1);
        } else if (options.objectSize() != fileSize) {
            throw new IllegalArgumentException(
                    "file size "
                            + fileSize
                            + " and object size in options "
                            + options.objectSize()
                            + " do not match");
        }

        if (options.contentType().equals("application/octet-stream")) {
            String contentType = Files.probeContentType(filePath);
            if (contentType != null && !contentType.equals("")) {
                options.setContentType(contentType);
            }
        }

        try (RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "r")) {
            putObject(bucketName, objectName, options, file);
        }
    }

    /**
     * Uploads data from given stream as object to given bucket using given PutObjectOptions. If any
     * error occurs, partial uploads are aborted.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code PutObjectOptions options = new PutObjectOptions(7003256, -1);
     * minioClient.putObject("my-bucketname", "my-objectname", inputStream, options);}
     * </pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name to create in the bucket.
     * @param stream Stream to upload.
     * @param options Options to be used during object upload.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void putObject(
            String bucketName, String objectName, InputStream stream, PutObjectOptions options)
            throws InvalidBucketNameException, NoSuchAlgorithmException, IOException, InvalidKeyException,
            XmlParserException, ErrorResponseException, InternalException, IllegalArgumentException,
            InsufficientDataException, InvalidResponseException {
        checkBucketName(bucketName);
        checkObjectName(objectName);

        if (stream == null) {
            throw new IllegalArgumentException("InputStream must be provided");
        }

        if (options == null) {
            throw new IllegalArgumentException("PutObjectOptions must be provided");
        }

        if (!(stream instanceof BufferedInputStream)) {
            stream = new BufferedInputStream(stream);
        }

        putObject(bucketName, objectName, options, stream);
    }

    /**
     * Get JSON string of bucket policy of the given bucket.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code String config = minioClient.getBucketPolicy("my-bucketname");
     * System.out.println("Bucket policy: " + config);}</pre>
     *
     * @param bucketName the name of the bucket for which policies are to be listed.
     * @return bucket policy JSON string.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws BucketPolicyTooLargeException upon bucket policy too large in size
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public String getBucketPolicy(String bucketName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, BucketPolicyTooLargeException,
            InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("policy", "");

        Response response = null;
        byte[] buf = new byte[MAX_BUCKET_POLICY_SIZE];
        int bytesRead = 0;

        try {
            response = executeGet(bucketName, null, null, queryParamMap);
            bytesRead = response.body().byteStream().read(buf, 0, MAX_BUCKET_POLICY_SIZE);
            if (bytesRead < 0) {
                throw new IOException("unexpected EOF when reading bucket policy");
            }

            // Read one byte extra to ensure only MAX_BUCKET_POLICY_SIZE data is sent by the server.
            if (bytesRead == MAX_BUCKET_POLICY_SIZE) {
                int byteRead = 0;
                while (byteRead == 0) {
                    byteRead = response.body().byteStream().read();
                    if (byteRead < 0) {
                        // reached EOF which is fine.
                        break;
                    }

                    if (byteRead > 0) {
                        throw new BucketPolicyTooLargeException(bucketName);
                    }
                }
            }
        } catch (ErrorResponseException e) {
            if (e.errorResponse().errorCode() != ErrorCode.NO_SUCH_BUCKET_POLICY) {
                throw e;
            }
        } finally {
            if (response != null) {
                response.body().close();
            }
        }

        return new String(buf, 0, bytesRead, StandardCharsets.UTF_8);
    }

    /**
     * Set JSON string of policy on given bucket.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code // Assume policyJson contains below JSON string;
     * // {
     * //     "Statement": [
     * //         {
     * //             "Action": [
     * //                 "s3:GetBucketLocation",
     * //                 "s3:ListBucket"
     * //             ],
     * //             "Effect": "Allow",
     * //             "Principal": "*",
     * //             "Resource": "arn:aws:s3:::my-bucketname"
     * //         },
     * //         {
     * //             "Action": "s3:GetObject",
     * //             "Effect": "Allow",
     * //             "Principal": "*",
     * //             "Resource": "arn:aws:s3:::my-bucketname/myobject*"
     * //         }
     * //     ],
     * //     "Version": "2012-10-17"
     * // }
     * //
     * minioClient.setBucketPolicy("my-bucketname", policyJson);}</pre>
     *
     * @param bucketName Bucket name.
     * @param policy Bucket policy JSON string.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void setBucketPolicy(String bucketName, String policy)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json");

        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("policy", "");

        Response response = executePut(bucketName, null, headerMap, queryParamMap, policy, 0);
        response.body().close();
    }

    /**
     * Set XML string of LifeCycle on a given bucket. Delete the lifecycle of bucket in case a null is
     * passed as lifeCycle.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code // Lets consider variable 'lifeCycleXml' contains below XML String;
     * // <LifecycleConfiguration>
     * //   <Rule>
     * //     <ID>expire-bucket</ID>
     * //     <Prefix></Prefix>
     * //     <Status>Enabled</Status>
     * //     <Expiration>
     * //       <Days>365</Days>
     * //     </Expiration>
     * //   </Rule>
     * // </LifecycleConfiguration>
     * //
     * minioClient.setBucketLifecycle("my-bucketname", lifeCycleXml);}</pre>
     *
     * @param bucketName Bucket name.
     * @param lifeCycle Bucket policy XML string.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws IllegalArgumentException upon invalid value is passed to a method.
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void setBucketLifeCycle(String bucketName, String lifeCycle)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, XmlParserException, ErrorResponseException,
            InternalException, IllegalArgumentException, InvalidResponseException {
        if ((lifeCycle == null) || "".equals(lifeCycle)) {
            throw new IllegalArgumentException("life cycle cannot be empty");
        }
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("lifecycle", "");
        Response response = executePut(bucketName, null, null, queryParamMap, lifeCycle, 0);
        response.body().close();
    }

    /**
     * Delete the LifeCycle of bucket.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code deleteBucketLifeCycle("my-bucketname");}</pre>
     *
     * @param bucketName Bucket name.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void deleteBucketLifeCycle(String bucketName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("lifecycle", "");
        Response response = executeDelete(bucketName, "", queryParamMap);
        response.body().close();
    }

    /**
     * Get bucket life cycle configuration.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code String lifecycle = minioClient.getBucketLifecycle("my-bucketname");
     * System.out.println("Life cycle settings: " + lifecycle);}</pre>
     *
     * @param bucketName Bucket name.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public String getBucketLifeCycle(String bucketName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("lifecycle", "");
        String bodyContent = "";
        Response response = null;
        try {
            response = executeGet(bucketName, null, null, queryParamMap);
            bodyContent = new String(response.body().bytes(), StandardCharsets.UTF_8);
        } catch (ErrorResponseException e) {
            if (e.errorResponse().errorCode() != ErrorCode.NO_SUCH_LIFECYCLE_CONFIGURATION) {
                throw e;
            }
        } finally {
            if (response != null) {
                response.body().close();
            }
        }

        return bodyContent;
    }

    /**
     * Get bucket notification configuration
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code NotificationConfiguration config = minioClient.getBucketNotification("my-bucketname");
     * System.out.println(config);}</pre>
     *
     * @param bucketName Bucket name.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public NotificationConfiguration getBucketNotification(String bucketName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("notification", "");

        Response response = executeGet(bucketName, null, null, queryParamMap);
        try (ResponseBody body = response.body()) {
            return Xml.unmarshal(NotificationConfiguration.class, body.charStream());
        }
    }

    /**
     * Set bucket notification configuration
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code List<EventType> eventList = new LinkedList<>();
     * eventList.add(EventType.OBJECT_CREATED_PUT);
     * eventList.add(EventType.OBJECT_CREATED_COPY);
     *
     * QueueConfiguration queueConfiguration = new QueueConfiguration();
     * queueConfiguration.setQueue("arn:minio:sqs::1:webhook");
     * queueConfiguration.setEvents(eventList);
     * queueConfiguration.setPrefixRule("images");
     * queueConfiguration.setSuffixRule("pg");
     *
     * List<QueueConfiguration> queueConfigurationList = new LinkedList<>();
     * queueConfigurationList.add(queueConfiguration);
     *
     * NotificationConfiguration config = new NotificationConfiguration();
     * config.setQueueConfigurationList(queueConfigurationList);
     *
     * minioClient.setBucketNotification("my-bucketname", config);}</pre>
     *
     * @param bucketName Bucket name.
     * @param notificationConfiguration Notification configuration to be set.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void setBucketNotification(
            String bucketName, NotificationConfiguration notificationConfiguration)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("notification", "");
        Response response =
                executePut(bucketName, null, null, queryParamMap, notificationConfiguration, 0);
        response.body().close();
    }

    /**
     * Remove all bucket notification.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code minioClient.removeAllBucketNotification("my-bucketname");}</pre>
     *
     * @param bucketName Bucket name.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void removeAllBucketNotification(String bucketName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        NotificationConfiguration notificationConfiguration = new NotificationConfiguration();
        setBucketNotification(bucketName, notificationConfiguration);
    }

    /**
     * Lists incomplete uploads of objects in given bucket.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code Iterable<Result<Upload>> results = minioClient.listIncompleteUploads("my-bucketname");
     * for (Result<Upload> result : results) {
     *   Upload upload = result.get();
     *   System.out.println(upload.uploadId() + ", " + upload.objectName());
     * }}</pre>
     *
     * @param bucketName Bucket name.
     * @return an iterator of Upload.
     * @see #listIncompleteUploads(String, String, boolean)
     */
    public Iterable<Result<Upload>> listIncompleteUploads(String bucketName)
            throws XmlParserException {
        return listIncompleteUploads(bucketName, null, true, true);
    }

    /**
     * Lists incomplete uploads of objects in given bucket and prefix.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code Iterable<Result<Upload>> results = minioClient.listIncompleteUploads("my-bucketname", "my-obj");
     * for (Result<Upload> result : results) {
     *   Upload upload = result.get();
     *   System.out.println(upload.uploadId() + ", " + upload.objectName());
     * }}</pre>
     *
     * @param bucketName Bucket name.
     * @param prefix filters the list of uploads to include only those that start with prefix.
     * @return an iterator of Upload.
     * @throws XmlParserException upon parsing response xml
     * @see #listIncompleteUploads(String, String, boolean)
     */
    public Iterable<Result<Upload>> listIncompleteUploads(String bucketName, String prefix)
            throws XmlParserException {
        return listIncompleteUploads(bucketName, prefix, true, true);
    }

    /**
     * Lists incomplete uploads of objects in given bucket, prefix and recursive flag.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code Iterable<Result<Upload>> results = minioClient.listIncompleteUploads("my-bucketname", "my-obj", true);
     * for (Result<Upload> result : results) {
     *   Upload upload = result.get();
     *   System.out.println(upload.uploadId() + ", " + upload.objectName());
     * }}</pre>
     *
     * @param bucketName Bucket name.
     * @param prefix Prefix string. List objects whose name starts with `prefix`.
     * @param recursive when false, emulates a directory structure where each listing returned is
     *     either a full object or part of the object's key up to the first '/'. All uploads with the
     *     same prefix up to the first '/' will be merged into one entry.
     * @return an iterator of Upload.
     * @see #listIncompleteUploads(String bucketName)
     * @see #listIncompleteUploads(String bucketName, String prefix)
     */
    public Iterable<Result<Upload>> listIncompleteUploads(
            String bucketName, String prefix, boolean recursive) {
        return listIncompleteUploads(bucketName, prefix, recursive, true);
    }

    /**
     * Returns Iterable<Result<Upload>> of given bucket name, prefix and recursive flag. All parts
     * size are aggregated when aggregatePartSize is true.
     */
    public Iterable<Result<Upload>> listIncompleteUploads(
            final String bucketName,
            final String prefix,
            final boolean recursive,
            final boolean aggregatePartSize) {
        return new Iterable<Result<Upload>>() {
            @Override
            public Iterator<Result<Upload>> iterator() {
                return new Iterator<Result<Upload>>() {
                    private String nextKeyMarker;
                    private String nextUploadIdMarker;
                    private ListMultipartUploadsResult listMultipartUploadsResult;
                    private Result<Upload> error;
                    private Iterator<Upload> uploadIterator;
                    private boolean completed = false;

                    private synchronized void populate() {
                        String delimiter = "/";
                        if (recursive) {
                            delimiter = null;
                        }

                        this.listMultipartUploadsResult = null;
                        this.uploadIterator = null;

                        try {
                            this.listMultipartUploadsResult =
                                    listIncompleteUploads(
                                            bucketName, nextKeyMarker, nextUploadIdMarker, prefix, delimiter, 1000);
                        } catch (InvalidBucketNameException
                                | NoSuchAlgorithmException
                                | InsufficientDataException
                                | IOException
                                | InvalidKeyException
                                | XmlParserException
                                | ErrorResponseException
                                | InternalException
                                | InvalidResponseException e) {
                            this.error = new Result<>(e);
                        } finally {
                            if (this.listMultipartUploadsResult != null) {
                                this.uploadIterator = this.listMultipartUploadsResult.uploads().iterator();
                            } else {
                                this.uploadIterator = new LinkedList<Upload>().iterator();
                            }
                        }
                    }

                    private synchronized long getAggregatedPartSize(String objectName, String uploadId)
                            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
                            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
                            ErrorResponseException, InternalException {
                        long aggregatedPartSize = 0;

                        for (Result<Part> result : listObjectParts(bucketName, objectName, uploadId)) {
                            aggregatedPartSize += result.get().partSize();
                        }

                        return aggregatedPartSize;
                    }

                    @Override
                    public boolean hasNext() {
                        if (this.completed) {
                            return false;
                        }

                        if (this.error == null && this.uploadIterator == null) {
                            populate();
                        }

                        if (this.error == null
                                && !this.uploadIterator.hasNext()
                                && this.listMultipartUploadsResult.isTruncated()) {
                            this.nextKeyMarker = this.listMultipartUploadsResult.nextKeyMarker();
                            this.nextUploadIdMarker = this.listMultipartUploadsResult.nextUploadIdMarker();
                            populate();
                        }

                        if (this.error != null) {
                            return true;
                        }

                        if (this.uploadIterator.hasNext()) {
                            return true;
                        }

                        this.completed = true;
                        return false;
                    }

                    @Override
                    public Result<Upload> next() {
                        if (this.completed) {
                            throw new NoSuchElementException();
                        }

                        if (this.error == null && this.uploadIterator == null) {
                            populate();
                        }

                        if (this.error == null
                                && !this.uploadIterator.hasNext()
                                && this.listMultipartUploadsResult.isTruncated()) {
                            this.nextKeyMarker = this.listMultipartUploadsResult.nextKeyMarker();
                            this.nextUploadIdMarker = this.listMultipartUploadsResult.nextUploadIdMarker();
                            populate();
                        }

                        if (this.error != null) {
                            this.completed = true;
                            return this.error;
                        }

                        if (this.uploadIterator.hasNext()) {
                            Upload upload = this.uploadIterator.next();

                            if (aggregatePartSize) {
                                long aggregatedPartSize;

                                try {
                                    aggregatedPartSize =
                                            getAggregatedPartSize(upload.objectName(), upload.uploadId());
                                } catch (InvalidBucketNameException
                                        | NoSuchAlgorithmException
                                        | InsufficientDataException
                                        | IOException
                                        | InvalidKeyException
                                        | XmlParserException
                                        | ErrorResponseException
                                        | InternalException
                                        | IllegalArgumentException e) {
                                    // special case: ignore the error as we can't propagate the exception in next()
                                    aggregatedPartSize = -1;
                                }

                                upload.setAggregatedPartSize(aggregatedPartSize);
                            }

                            return new Result<>(upload);
                        }

                        this.completed = true;
                        throw new NoSuchElementException();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * Do ListMultipartUploads as per
     * https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListMultipartUploads.html
     */
    public ListMultipartUploadsResult listIncompleteUploads(
            String bucketName,
            String keyMarker,
            String uploadIdMarker,
            String prefix,
            String delimiter,
            int maxUploads)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        if (maxUploads < 0 || maxUploads > 1000) {
            maxUploads = 1000;
        }

        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("uploads", "");
        queryParamMap.put("max-uploads", Integer.toString(maxUploads));

        if (prefix != null) {
            queryParamMap.put("prefix", prefix);
        } else {
            queryParamMap.put("prefix", "");
        }

        if (delimiter != null) {
            queryParamMap.put("delimiter", delimiter);
        } else {
            queryParamMap.put("delimiter", "");
        }

        if (keyMarker != null) {
            queryParamMap.put("key-marker", keyMarker);
        }

        if (uploadIdMarker != null) {
            queryParamMap.put("upload-id-marker", uploadIdMarker);
        }

        Response response = executeGet(bucketName, null, null, queryParamMap);

        try (ResponseBody body = response.body()) {
            return Xml.unmarshal(ListMultipartUploadsResult.class, body.charStream());
        }
    }

    /**
     * Do CreateMultipartUpload as per
     * https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateMultipartUpload.html
     */
    public String initMultipartUpload(
            String bucketName, String objectName, Map<String, String> headerMap)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        // set content type if not set already
        if ((headerMap != null) && (headerMap.get("Content-Type") == null)) {
            headerMap.put("Content-Type", "application/octet-stream");
        }

        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("uploads", "");

        Response response = executePost(bucketName, objectName, headerMap, queryParamMap, "");

        try (ResponseBody body = response.body()) {
            InitiateMultipartUploadResult result =
                    Xml.unmarshal(InitiateMultipartUploadResult.class, body.charStream());
            return result.uploadId();
        }
    }

    /**
     * Do CompleteMultipartUpload as per
     * https://docs.aws.amazon.com/AmazonS3/latest/API/API_CompleteMultipartUpload.html
     */
    public void completeMultipart(
            String bucketName, String objectName, String uploadId, Part[] parts)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put(UPLOAD_ID, uploadId);
        CompleteMultipartUpload completeManifest = new CompleteMultipartUpload(parts);
        Response response = executePost(bucketName, objectName, null, queryParamMap, completeManifest);
        String bodyContent = "";
        try (ResponseBody body = response.body()) {
            bodyContent = new String(body.bytes(), StandardCharsets.UTF_8);
            bodyContent = bodyContent.trim();
        }

        // Handle if body contains error.
        if (!bodyContent.isEmpty()) {
            try {
                if (Xml.validate(ErrorResponse.class, bodyContent)) {
                    ErrorResponse errorResponse = Xml.unmarshal(ErrorResponse.class, bodyContent);
                    throw new ErrorResponseException(errorResponse, response);
                }
            } catch (XmlParserException e) {
                // As it is not <ResponseError> message, ignore this exception
            }
        }
    }

    /**
     * Executes List object parts of multipart upload for given bucket name, object name and upload ID
     * and returns Iterable<Result<Part>>.
     */
    public Iterable<Result<Part>> listObjectParts(
            final String bucketName, final String objectName, final String uploadId) {
        return new Iterable<Result<Part>>() {
            @Override
            public Iterator<Result<Part>> iterator() {
                return new Iterator<Result<Part>>() {
                    private int nextPartNumberMarker;
                    private ListPartsResult listPartsResult;
                    private Result<Part> error;
                    private Iterator<Part> partIterator;
                    private boolean completed = false;

                    private synchronized void populate() {
                        this.listPartsResult = null;
                        this.partIterator = null;

                        try {
                            this.listPartsResult =
                                    listObjectParts(bucketName, objectName, uploadId, nextPartNumberMarker);
                        } catch (InvalidBucketNameException
                                | NoSuchAlgorithmException
                                | InsufficientDataException
                                | IOException
                                | InvalidKeyException
                                | XmlParserException
                                | ErrorResponseException
                                | InternalException
                                | InvalidResponseException
                                | IllegalArgumentException e) {
                            this.error = new Result<>(e);
                        } finally {
                            if (this.listPartsResult != null) {
                                this.partIterator = this.listPartsResult.partList().iterator();
                            } else {
                                this.partIterator = new LinkedList<Part>().iterator();
                            }
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        if (this.completed) {
                            return false;
                        }

                        if (this.error == null && this.partIterator == null) {
                            populate();
                        }

                        if (this.error == null
                                && !this.partIterator.hasNext()
                                && this.listPartsResult.isTruncated()) {
                            this.nextPartNumberMarker = this.listPartsResult.nextPartNumberMarker();
                            populate();
                        }

                        if (this.error != null) {
                            return true;
                        }

                        if (this.partIterator.hasNext()) {
                            return true;
                        }

                        this.completed = true;
                        return false;
                    }

                    @Override
                    public Result<Part> next() {
                        if (this.completed) {
                            throw new NoSuchElementException();
                        }

                        if (this.error == null && this.partIterator == null) {
                            populate();
                        }

                        if (this.error == null
                                && !this.partIterator.hasNext()
                                && this.listPartsResult.isTruncated()) {
                            this.nextPartNumberMarker = this.listPartsResult.nextPartNumberMarker();
                            populate();
                        }

                        if (this.error != null) {
                            this.completed = true;
                            return this.error;
                        }

                        if (this.partIterator.hasNext()) {
                            return new Result<>(this.partIterator.next());
                        }

                        this.completed = true;
                        throw new NoSuchElementException();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /** Do ListParts as per https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListParts.html */
    public ListPartsResult listObjectParts(
            String bucketName, String objectName, String uploadId, int partNumberMarker)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put(UPLOAD_ID, uploadId);
        if (partNumberMarker > 0) {
            queryParamMap.put("part-number-marker", Integer.toString(partNumberMarker));
        }

        Response response = executeGet(bucketName, objectName, null, queryParamMap);

        try (ResponseBody body = response.body()) {
            return Xml.unmarshal(ListPartsResult.class, body.charStream());
        }
    }

    /**
     * Do AbortMultipartUpload as per
     * https://docs.aws.amazon.com/AmazonS3/latest/API/API_AbortMultipartUpload.html
     */
    public void abortMultipartUpload(String bucketName, String objectName, String uploadId)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put(UPLOAD_ID, uploadId);
        executeDelete(bucketName, objectName, queryParamMap);
    }

    /**
     * Removes incomplete multipart upload of given object.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code minioClient.removeIncompleteUpload("my-bucketname", "my-objectname");}</pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public void removeIncompleteUpload(String bucketName, String objectName)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        for (Result<Upload> r : listIncompleteUploads(bucketName, objectName, true, false)) {
            Upload upload = r.get();
            if (objectName.equals(upload.objectName())) {
                abortMultipartUpload(bucketName, objectName, upload.uploadId());
                return;
            }
        }
    }

    /**
     * Listen to bucket notifications. As bucket notification are lazily executed, its required to
     * iterate. The returned closeable iterator must be used with try with resource; else the stream
     * will not be closed.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code String[] events = {"s3:ObjectCreated:*", "s3:ObjectAccessed:*"};
     * try (CloseableIterator<Result<NotificationInfo>> ci =
     *     minioClient.listenBucketNotification("bcketName", "", "", events)) {
     *   while (ci.hasNext()) {
     *     NotificationRecords records = ci.next().get();
     *     for (Event event : records.events()) {
     *       System.out.println("Event " + event.eventType() + " occurred at " + event.eventTime()
     *           + " for " + event.bucketName() + "/" + event.objectName());
     *     }
     *   }
     * }}</pre>
     *
     * @param bucketName Bucket name.
     * @param prefix Prefix of concerned objects events.
     * @param suffix Suffix of concerned objects events.
     * @param events List of events to watch.
     * @return (lazy) CloseableIterator of event records.
     */
    public CloseableIterator<Result<NotificationRecords>> listenBucketNotification(
            String bucketName, String prefix, String suffix, String[] events)
            throws IOException, InvalidKeyException, NoSuchAlgorithmException, InsufficientDataException,
            InvalidResponseException, InternalException, InvalidBucketNameException,
            XmlParserException, ErrorResponseException {
        Multimap<String, String> queryParamMap = HashMultimap.create();
        queryParamMap.put("prefix", prefix);
        queryParamMap.put("suffix", suffix);
        for (String event : events) {
            queryParamMap.put("events", event);
        }

        Response response = executeGet(bucketName, "", queryParamMap);

        MyMinioClient.NotificationResultRecords result = new MyMinioClient.NotificationResultRecords(response);
        return result.closeableIterator();
    }

    /**
     * Select object content using SQL expression.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code String sqlExpression = "select * from S3Object";
     * InputSerialization is = new InputSerialization(null, false, null, null, FileHeaderInfo.USE, null, null, null);
     * OutputSerialization os = new OutputSerialization(null, null, null, QuoteFields.ASNEEDED, null);
     * SelectResponseStream stream = minioClient.selectObjectContent("my-bucketname", "my-objectName", sqlExpression,
     *     is, os, true, null, null, null);
     *
     * byte[] buf = new byte[512];
     * int bytesRead = stream.read(buf, 0, buf.length);
     * System.out.println(new String(buf, 0, bytesRead, StandardCharsets.UTF_8));
     *
     * Stats stats = stream.stats();
     * System.out.println("bytes scanned: " + stats.bytesScanned());
     * System.out.println("bytes processed: " + stats.bytesProcessed());
     * System.out.println("bytes returned: " + stats.bytesReturned());
     *
     * stream.close();}</pre>
     *
     * @param bucketName Bucket name.
     * @param objectName Object name.
     * @param sqlExpression SQL expression.
     * @param is Input serialization.
     * @param os Output serialization.
     * @param requestProgress Request progress in response.
     * @param scanStartRange scan start range.
     * @param scanEndRange scan end range.
     * @param sse Server side encryption.
     * @throws InvalidBucketNameException upon invalid bucket name is given
     * @throws IllegalArgumentException upon empty object name is given
     * @throws NoSuchAlgorithmException upon requested algorithm was not found during signature
     *     calculation
     * @throws InsufficientDataException upon getting EOFException while reading given InputStream
     *     even before reading given length
     * @throws IOException upon connection error
     * @throws InvalidKeyException upon an invalid access key or secret key
     * @throws XmlParserException upon parsing response xml
     * @throws ErrorResponseException upon unsuccessful execution
     * @throws InternalException upon internal library error
     * @throws InvalidResponseException upon a non-xml response from server
     */
    public SelectResponseStream selectObjectContent(
            String bucketName,
            String objectName,
            String sqlExpression,
            InputSerialization is,
            OutputSerialization os,
            boolean requestProgress,
            Long scanStartRange,
            Long scanEndRange,
            ServerSideEncryption sse)
            throws InvalidBucketNameException, IllegalArgumentException, NoSuchAlgorithmException,
            InsufficientDataException, IOException, InvalidKeyException, XmlParserException,
            ErrorResponseException, InternalException, InvalidResponseException {
        if ((bucketName == null) || (bucketName.isEmpty())) {
            throw new IllegalArgumentException("bucket name cannot be empty");
        }
        checkObjectName(objectName);
        checkReadRequestSse(sse);

        Map<String, String> headerMap = null;
        if (sse != null) {
            headerMap = sse.headers();
        }

        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("select", "");
        queryParamMap.put("select-type", "2");

        SelectObjectContentRequest request =
                new SelectObjectContentRequest(
                        sqlExpression, requestProgress, is, os, scanStartRange, scanEndRange);
        Response response = executePost(bucketName, objectName, headerMap, queryParamMap, request);
        return new SelectResponseStream(response.body().byteStream());
    }

    public long getAvailableSize(Object data, long expectedReadSize)
            throws IOException, InternalException {
        if (!(data instanceof BufferedInputStream)) {
            throw new InternalException(
                    "data must be BufferedInputStream. This should not happen.  "
                            + "Please report to https://github.com/minio/minio-java/issues/");
        }

        BufferedInputStream stream = (BufferedInputStream) data;
        stream.mark((int) expectedReadSize);

        byte[] buf = new byte[16384]; // 16KiB buffer for optimization
        long totalBytesRead = 0;
        while (totalBytesRead < expectedReadSize) {
            long bytesToRead = expectedReadSize - totalBytesRead;
            if (bytesToRead > buf.length) {
                bytesToRead = buf.length;
            }

            int bytesRead = stream.read(buf, 0, (int) bytesToRead);
            if (bytesRead < 0) {
                break; // reached EOF
            }

            totalBytesRead += bytesRead;
        }

        stream.reset();
        return totalBytesRead;
    }

    /**
     * Sets HTTP connect, write and read timeouts. A value of 0 means no timeout, otherwise values
     * must be between 1 and Integer.MAX_VALUE when converted to milliseconds.
     *
     * <p><b>Example:</b>
     *
     * <pre>
     * {@code minioClient.setTimeout(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(30));}
     * </pre>
     *
     * @param connectTimeout HTTP connect timeout in milliseconds.
     * @param writeTimeout HTTP write timeout in milliseconds.
     * @param readTimeout HTTP read timeout in milliseconds.
     */
    public void setTimeout(long connectTimeout, long writeTimeout, long readTimeout) {
        this.httpClient =
                this.httpClient
                        .newBuilder()
                        .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                        .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                        .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                        .build();
    }

    /**
     * Ignores check on server certificate for HTTPS connection.
     *
     * <p><b>Example:</b>
     *
     * <pre>{@code minioClient.ignoreCertCheck();}</pre>
     */
    @SuppressFBWarnings(value = "SIC", justification = "Should not be used in production anyways.")
    public void ignoreCertCheck() throws NoSuchAlgorithmException, KeyManagementException {
        final TrustManager[] trustAllCerts =
                new TrustManager[] {
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType)
                                    throws CertificateException {}

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType)
                                    throws CertificateException {}

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[] {};
                            }
                        }
                };

        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        this.httpClient =
                this.httpClient
                        .newBuilder()
                        .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                        .hostnameVerifier(
                                new HostnameVerifier() {
                                    @Override
                                    public boolean verify(String hostname, SSLSession session) {
                                        return true;
                                    }
                                })
                        .build();
    }

    /**
     * Sets application's name/version to user agent. For more information about user agent refer <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">#rfc2616</a>.
     *
     * @param name Your application name.
     * @param version Your application version.
     */
    @SuppressWarnings("unused")
    public void setAppInfo(String name, String version) {
        if (name == null || version == null) {
            // nothing to do
            return;
        }

        this.userAgent = DEFAULT_USER_AGENT + " " + name.trim() + "/" + version.trim();
    }

    /**
     * Enables HTTP call tracing and written to traceStream.
     *
     * @param traceStream {@link OutputStream} for writing HTTP call tracing.
     * @see #traceOff
     */
    public void traceOn(OutputStream traceStream) {
        if (traceStream == null) {
            throw new NullPointerException();
        } else {
            this.traceStream =
                    new PrintWriter(new OutputStreamWriter(traceStream, StandardCharsets.UTF_8), true);
        }
    }

    /**
     * Disables HTTP call tracing previously enabled.
     *
     * @see #traceOn
     * @throws IOException upon connection error
     */
    public void traceOff() throws IOException {
        this.traceStream = null;
    }

    public static class NotificationResultRecords {
        Response response = null;
        Scanner scanner = null;
        ObjectMapper mapper = null;

        public NotificationResultRecords(Response response) {
            this.response = response;
            this.scanner = new Scanner(response.body().charStream()).useDelimiter("\n");
            this.mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        }

        /** returns closeable iterator of result of notification records. */
        public CloseableIterator<Result<NotificationRecords>> closeableIterator() {
            return new CloseableIterator<Result<NotificationRecords>>() {
                String recordsString = null;
                NotificationRecords records = null;
                boolean isClosed = false;

                @Override
                public void close() throws IOException {
                    if (!isClosed) {
                        try {
                            response.body().close();
                            scanner.close();
                        } finally {
                            isClosed = true;
                        }
                    }
                }

                public boolean populate() {
                    if (isClosed) {
                        return false;
                    }

                    if (recordsString != null) {
                        return true;
                    }

                    while (scanner.hasNext()) {
                        recordsString = scanner.next().trim();
                        if (!recordsString.equals("")) {
                            break;
                        }
                    }

                    if (recordsString == null || recordsString.equals("")) {
                        try {
                            close();
                        } catch (IOException e) {
                            isClosed = true;
                        }
                        return false;
                    }
                    return true;
                }

                @Override
                public boolean hasNext() {
                    return populate();
                }

                @Override
                public Result<NotificationRecords> next() {
                    if (isClosed) {
                        throw new NoSuchElementException();
                    }
                    if ((recordsString == null || recordsString.equals("")) && !populate()) {
                        throw new NoSuchElementException();
                    }

                    try {
                        records = mapper.readValue(recordsString, NotificationRecords.class);
                        return new Result<>(records);
                    } catch (JsonParseException e) {
                        return new Result<>(e);
                    } catch (JsonMappingException e) {
                        return new Result<>(e);
                    } catch (IOException e) {
                        return new Result<>(e);
                    } finally {
                        recordsString = null;
                        records = null;
                    }
                }
            };
        }
    }

    // add method by shiyuquan

    /**
     * Returns latest upload ID of incomplete multipart upload of given bucket name and object name.
     */
    public String getLatestIncompleteUploadId(String bucketName, String objectName)
            throws InvalidBucketNameException, NoSuchAlgorithmException, InsufficientDataException,
            IOException, InvalidKeyException, ErrorResponseException, InternalException,
            XmlParserException {
        Upload latestUpload = null;
        for (Result<Upload> result : listIncompleteUploads(bucketName, objectName, true, false)) {
            Upload upload = result.get();
            if (upload.objectName().equals(objectName)
                    && (latestUpload == null || latestUpload.initiated().compareTo(upload.initiated()) < 0)) {
                latestUpload = upload;
            }
        }

        if (latestUpload != null) {
            return latestUpload.uploadId();
        } else {
            return null;
        }
    }



    /**
     * 获取uploadId
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @return uploadId
     */
    public String getUploadId(String bucketName, String objectName) {
        String uploadId = "";
        try {

            Map<String, String> headerMap = new HashMap<>();

            headerMap.put("Content-Type", "application/octet-stream");

            // initiate new multipart upload.
            uploadId = initMultipartUpload(bucketName, objectName, headerMap);

            // uploadId = getLatestIncompleteUploadId( bucketName, objectName );
            if(StringUtils.isEmpty(uploadId)) {
                uploadId = initMultipartUpload( bucketName, objectName, new HashMap<String, String>() );
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return uploadId;
    }

    /**
     * 校验文件是否存在
     * @param bucketName 桶名称
     * @param objectName 文件名称
     * @return boolean
     */
    public boolean objectExists(String bucketName, String objectName) {
        try {
            statObject(bucketName, objectName);
        } catch (ErrorResponseException e) {
            if (e.errorResponse().errorCode() == ErrorCode.NO_SUCH_BUCKET
                    || e.errorResponse().errorCode() == ErrorCode.NO_SUCH_KEY
                    || e.errorResponse().errorCode() == ErrorCode.RESOURCE_NOT_FOUND) {
                return false;
            }
        } catch (Exception e) {
            throw new BlegException(500, "minio校验文件存在失败", e);
        }
        return true ;
    }

    /**
     * 校验分片是否存在
     * @param bucketName Bucket name.
     * @param objectName Object name in the bucket.
     * @param uploadId
     * @param partNumber
     * @param partSize
     * @return
     */
    public boolean objectPartExists(String bucketName, String objectName, String uploadId, int partNumber, int partSize) {
        boolean isExist = false;
        try {
            Iterator<Result<Part>> existingParts = listObjectParts(bucketName, objectName, uploadId).iterator();
            while ( existingParts.hasNext() ) {
                Part part = existingParts.next().get();
                if( part.partNumber() == partNumber && part.partSize() == partSize) {
                    isExist = true;
                    break;
                }
            }
        } catch (Exception e) {
            throw new BlegException(500, "校验分片是否存在失败", e);
        }
        return isExist;
    }
}
