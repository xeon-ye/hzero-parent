package org.hzero.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import org.hzero.autoconfigure.report.EnableHZeroReport;

/**
 *
 * HZERO 报表平台
 *
 * @author xianzhi.chen@hand-china.com 2018年11月27日下午1:36:40
 */
@EnableHZeroReport
@EnableDiscoveryClient
@SpringBootApplication
public class ReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportApplication.class, args);
    }
}
