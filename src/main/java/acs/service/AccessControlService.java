package acs.service;

import acs.domain.AccessRequest;
import acs.domain.AccessResult;

/**
 * AccessControlService 是整个系统的“唯一访问控制入口”
 *
 * UI、Badge Reader 等外部模块：
 * - 只能通过这个接口请求访问
 * - 不允许直接操作 Repository 或 Domain 对象
 */
public interface AccessControlService {

    /**
     * 处理一次访问请求，并返回访问结果
     *
     * 实现类必须保证：
     * 1. 不抛出异常给 UI
     * 2. 所有异常情况转化为 DENY + ReasonCode
     * 3. 每一次调用都会产生一条访问日志
     *
     * @param request 访问请求
     * @return 访问结果
     */
    AccessResult processAccess(AccessRequest request);
}
