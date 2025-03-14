package com.bank.bootcoinservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication(exclude = {RedisReactiveAutoConfiguration.class})
@EnableEurekaClient
public class BootcoinServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BootcoinServiceApplication.class, args);
	}

}
