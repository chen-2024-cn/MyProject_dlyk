package com.cyk.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * 字典类型表
 * t_dic_type
 */
@Data
public class TDicType implements Serializable {
    /**
     * 主键，自动增长，字典类型ID
     */
    private Integer id;

    /**
     * 字典类型代码
     */
    private String typeCode;

    /**
     * 字典类型名称
     */
    private String typeName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 一对多关联
     */
    private List<TDicValue> dicValueList;

    /**
     * 字典值数量（目录展示用，非表字段）。
     * <p>由 {@code selectAllTypes} 的相关子查询填充；其余查询不映射该列时为 null，
     * 不影响既有逻辑。字典类型目录页据此显示每类的值数徽章并标记「空类型」。</p>
     */
    private Integer valueCount;

    private static final long serialVersionUID = 1L;
}