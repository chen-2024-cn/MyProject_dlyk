package com.cyk.model;

import java.io.Serializable;
import lombok.Data;

/**
 * 角色权限关系表
 * t_role_permission
 */
@Data
public class TRolePermission implements Serializable {
    private Integer id;

    private Integer roleId;

    private Integer permissionId;

    private static final long serialVersionUID = 1L;
}