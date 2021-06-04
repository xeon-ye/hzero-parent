package org.hzero.autoconfigure.register;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by wushuai on 2021/5/21
 */
@ComponentScan(value = {
        "org.hzero.register.api"
})
@Configuration
public class RegisterAutoConfiguration {
}
