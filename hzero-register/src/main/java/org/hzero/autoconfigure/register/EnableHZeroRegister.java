package org.hzero.autoconfigure.register;
import java.lang.annotation.*;

import org.springframework.context.annotation.Import;
/**
 * Created by wushuai on 2021/5/21
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RegisterAutoConfiguration.class)
public @interface EnableHZeroRegister {
}
