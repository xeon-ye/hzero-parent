package org.hzero.sso.core.domain.repository;

import java.util.List;

import org.hzero.sso.core.domain.entity.Domain;

/**
 * 域名配置
 *
 * @author minghui.qiu@hand-china.com 2019-06-27 20:50:16
 */
public interface DomainRepository {

    /**
     * 查询所有域名信息
     *
     * @return 返回所有域名，没有则返回空集合，不会为 null
     */
    List<Domain> selectAllDomain();

}
