package acs.domain;

/**
 * UserType 表示员工/用户的类型分类。
 */
public enum UserType {
    EMPLOYEE,      // 正式员工
    CONTRACTOR,    // 承包商
    VISITOR,       // 访客
    ADMIN,         // 管理员
    GUEST          // 临时访客
}