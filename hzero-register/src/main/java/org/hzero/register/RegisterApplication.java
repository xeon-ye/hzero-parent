package org.hzero.register;

import org.hzero.autoconfigure.register.EnableHZeroRegister;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * HZERO 注册中心
 * Created by wushuai on 2021/5/21
 */
@EnableHZeroRegister
@EnableEurekaServer
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class RegisterApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegisterApplication.class, args);
    }
}
