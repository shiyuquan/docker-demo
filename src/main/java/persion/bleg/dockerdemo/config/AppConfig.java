package persion.bleg.dockerdemo.config;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.io.Charsets.UTF_8;

/**
 * @author shiyuquan
 * @since 2020/9/23 3:46 下午
 */
@Configuration
public class AppConfig {
    /**
     * 功能描述：注册restTemplate服务
     *
     * @param
     * @author wangcanfeng
     * @time 2019/3/6 20:26
     * @since v1.0
     **/

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(getFactory());
        //这个地方需要配置消息转换器，不然收到消息后转换会出现异常
        restTemplate.setMessageConverters(getConverts());
        return restTemplate;
    }


    /**
     * 功能描述： 初始化请求工厂
     *
     * @param
     * @author wangcanfeng
     * @time 2019/3/6 20:27
     * @since v1.0
     **/
    private HttpComponentsClientHttpRequestFactory getFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        // 连接超时时间
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return factory;
    }

    /**
     * 功能描述：  设置数据转换器，我再这里只设置了String转换器
     *
     * @param
     * @author wangcanfeng
     * @time 2019/3/6 20:32
     * @since v1.0
     **/
    private List<HttpMessageConverter<?>> getConverts() {
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setSerializerFeatures(SerializerFeature.PrettyFormat);
        //处理中文乱码
        List<MediaType> fastMediaTypes = new ArrayList<>();
        fastMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
        fastMediaTypes.add(MediaType.TEXT_HTML);
        fastConverter.setSupportedMediaTypes(fastMediaTypes);
        //3、在convert中添加配置信息.
        fastConverter.setFastJsonConfig(fastJsonConfig);

        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter();
        stringConverter.setDefaultCharset(UTF_8);
        stringConverter.setSupportedMediaTypes(fastMediaTypes);
        messageConverters.add(fastConverter);
        messageConverters.add(stringConverter);
        return messageConverters;
    }

    /**
     * 忽略ssl的restemplat 配置
     * 请求https 使用
     */
    @Bean("sslRestemplate")
    public RestTemplate sslRestTemplate() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                                                               .loadTrustMaterial(null, acceptingTrustStrategy)
                                                               .build();

        //暂时关闭hostnameVerify
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(
                sslContext,
                new String[] { "TLSv1" },
                null,
                NoopHostnameVerifier.INSTANCE);

        CloseableHttpClient httpClient = HttpClients.custom()
                                                    .setSSLSocketFactory(csf)
                                                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                                    .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();

        requestFactory.setHttpClient(httpClient);
        return new RestTemplate(requestFactory);
    }

}
