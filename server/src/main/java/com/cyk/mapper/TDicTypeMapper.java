package com.cyk.mapper;

import com.cyk.model.TDicType;
import com.cyk.query.DicTypeQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TDicTypeMapper {
    int deleteByPrimaryKey(Integer id);

    /**
     * 按 typeCode 查重（type_code 库层唯一索引）。
     * <p>excludeId 不为空时排除自身，供编辑场景使用——改名但不改 code 时不应误报重复。</p>
     * 在 Service 层前置校验，把「唯一冲突」从数据库异常提前拦截为可读的业务提示。
     */
    int countByTypeCode(@Param("typeCode") String typeCode, @Param("excludeId") Integer excludeId);

    int insert(TDicType record);

    int insertSelective(TDicType record);

    TDicType selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TDicType record);

    int updateByPrimaryKey(TDicType record);

    List<TDicType> selectByAll();

    /**
     * 查询全部字典类型（不分页，带每类字典值数量）。
     * <p>供「字典类型目录」总览与「字典数据」页的类型选择器共用：类型总量有限（十余个），
     * 分页会割裂目录浏览体验，故一次性返回全量。</p>
     */
    List<TDicType> selectAllTypes();

    List<TDicType> selectDicTypeByPage(DicTypeQuery query);
}