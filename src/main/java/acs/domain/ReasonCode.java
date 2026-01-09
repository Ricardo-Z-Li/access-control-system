package acs.domain;

/**
 * ReasonCode 用于描述访问被允许或拒绝的“具体原因”
 *
 *！！！非常重要：
 * - 所有模块必须使用这些固定值
 * - 不允许随意新增字符串
 */
public enum ReasonCode {

    /** 初始化非空类型 */
    PENDING,

    /** 访问成功 */
    ALLOW,

    /** 未找到对应的 Badge */
    BADGE_NOT_FOUND,

    /** Badge 存在但不可用（禁用 / 挂失） */
    BADGE_INACTIVE,

    /** Badge 存在，但找不到对应员工 */
    EMPLOYEE_NOT_FOUND,

    /** 访问的资源不存在 */
    RESOURCE_NOT_FOUND,

    /** 资源被锁定 */
    RESOURCE_LOCKED,

    /** 资源当前被占用 */
    RESOURCE_OCCUPIED,

    /** 员工所属组没有访问权限 */
    NO_PERMISSION,

    /** 请求本身非法（参数缺失等） */
    INVALID_REQUEST,

    /** 系统内部错误 */
    SYSTEM_ERROR
}
