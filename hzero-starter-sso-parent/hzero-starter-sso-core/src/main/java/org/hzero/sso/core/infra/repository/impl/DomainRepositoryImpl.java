package org.hzero.sso.core.infra.repository.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.hzero.common.HZeroService;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.redis.RedisHelper;
import org.hzero.core.redis.safe.SafeRedisHelper;
import org.hzero.sso.core.constant.SsoConstant;
import org.hzero.sso.core.domain.entity.Domain;
import org.hzero.sso.core.domain.repository.DomainRepository;

/**
 * 门户分配 资源库实现
 *
 * @author minghui.qiu@hand-china.com 2019-06-27 20:50:16
 */
public class DomainRepositoryImpl implements DomainRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainRepositoryImpl.class);

    @Autowired
    private RedisHelper redisHelper;

    private ObjectMapper mapper = BaseConstants.MAPPER;

    @Override
    public List<Domain> selectAllDomain() {
        List<String> strs = SafeRedisHelper.execute(HZeroService.Iam.REDIS_DB, () -> redisHelper.hshVals(SsoConstant.HIAM_DOMAIN));

        return Optional.ofNullable(strs).orElse(Collections.emptyList()).stream().map(str -> {
            try {
                return mapper.readValue(str, Domain.class);
            } catch (IOException e) {
                LOGGER.warn("deserialize error. ex={}", e.getMessage());
                return new Domain();
            }
        }).collect(Collectors.toList());
    }
}
