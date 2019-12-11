package homecontroller.service;

import java.time.Duration;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.zaxxer.hikari.HikariDataSource;

import homecontroller.util.HomeAppConstants;

@Configuration
public class SpringConfiguration implements WebMvcConfigurer {

	@Autowired
	private Environment env;

	@Bean
	public ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
		return new ByteArrayHttpMessageConverter();
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(10))
				.setReadTimeout(Duration.ofSeconds(10)).build();
	}

	@Bean(name = "restTemplateBinaryResponse")
	public RestTemplate restTemplateBinaryResponse(RestTemplateBuilder restTemplateBuilder,
			List<HttpMessageConverter<?>> messageConverters) {
		return restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(5))
				.setReadTimeout(Duration.ofSeconds(20)).additionalMessageConverters(messageConverters)
				.build();
	}

	@Bean(name = "restTemplateLowTimeout")
	public RestTemplate restTemplateLowTimeout(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.setConnectTimeout(Duration.ofMillis(500))
				.setReadTimeout(Duration.ofMillis(500)).build();
	}

	@Bean(name = "restTemplateLongPolling")
	public RestTemplate restTemplateLongPolling(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(10)).setReadTimeout(Duration
				.ofSeconds(HomeAppConstants.CONTROLLER_CLIENT_LONGPOLLING_REQUEST_TIMEOUT_SECONDS * 2L))
				.build();
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
	public JdbcTemplate jdbcTemplateHistory(DataSource ds) {
		return new JdbcTemplate(ds);
	}

	@Bean(name = "datasourceMigration")
	public DataSource datasourceMigration() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(env.getProperty("datamigration.driverClassName"));
		dataSource.setUrl(env.getProperty("datamigration.url"));
		dataSource.setUsername(env.getProperty("datamigration.username"));
		dataSource.setPassword(env.getProperty("datamigration.password"));
		// System.out.println("MIGRATION: " + dataSource.getUrl() +
		// dataSource.getUsername() + "/" + dataSource.getPassword());
		return dataSource;
	}

	@Bean(name = "jdbcTemplateMigrationDB")
	public JdbcTemplate jdbcTemplateMigration(@Qualifier("datasourceMigration") DataSource ds) {
		return new JdbcTemplate(ds);
	}

}
