package de.fimatas.home.controller.service;

import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.sql.DataSource;

import org.apache.commons.net.util.SSLContextUtils;
import org.apache.commons.net.util.TrustManagerUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.zaxxer.hikari.HikariDataSource;

import de.fimatas.home.library.util.HomeAppConstants;

@Configuration
@EnableRetry
public class SpringConfiguration implements WebMvcConfigurer {

    @Autowired
    private Environment env; // NOSONAR

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Bean
    public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
        return new ByteArrayHttpMessageConverter();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(10)).setReadTimeout(Duration.ofSeconds(10)).build();
    }

    @Bean(name = "restTemplateCCU")
    public RestTemplate restTemplateCCU() throws IOException, GeneralSecurityException {

        try (PEMParser pemParser = new PEMParser(new FileReader(env.getProperty("homematic.sslcert")))) {

            pemParser.readObject();
            PemObject pemObject = pemParser.readPemObject();

            X509CertificateHolder holder = new X509CertificateHolder(pemObject.getContent());
            X509Certificate bc = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", bc);

            TrustManager trustManager = TrustManagerUtils.getDefaultTrustManager(keyStore);
            SSLContext sslContext = SSLContextUtils.createSSLContext("TLS", null, trustManager);

            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

            RequestConfig config = RequestConfig.custom().setConnectTimeout(5 * 1000).setConnectionRequestTimeout(10 * 1000)
                .setSocketTimeout(10 * 1000).build();

            CloseableHttpClient httpClient =
                HttpClients.custom().setDefaultRequestConfig(config).setSSLSocketFactory(socketFactory).build();
            HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
            return new RestTemplate(httpComponentsClientHttpRequestFactory);
        }
    }

    @Bean(name = "restTemplateBinaryResponse")
    public RestTemplate restTemplateBinaryResponse(RestTemplateBuilder restTemplateBuilder,
            List<HttpMessageConverter<?>> messageConverters) {
        return restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(5)).setReadTimeout(Duration.ofSeconds(20))
            .additionalMessageConverters(messageConverters).build();
    }

    @Bean(name = "restTemplateLowTimeout")
    public RestTemplate restTemplateLowTimeout(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.setConnectTimeout(Duration.ofMillis(500)).setReadTimeout(Duration.ofMillis(500)).build();
    }

    @Bean(name = "restTemplateLongPolling")
    public RestTemplate restTemplateLongPolling(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(HomeAppConstants.CONTROLLER_CLIENT_LONGPOLLING_REQUEST_TIMEOUT_SECONDS * 2L))
            .build();
    }

    @Bean(name = "restTemplateHeatpumpDriver")
    public RestTemplate restTemplateHeatpumpDriver(RestTemplateBuilder restTemplateBuilder){
        return restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(10)).setReadTimeout(Duration.ofSeconds(80)).build();
    }

    @Bean
    @Primary
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        return dataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler(){
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
        threadPoolTaskScheduler.setPoolSize(10);
        return threadPoolTaskScheduler;
    }
}
