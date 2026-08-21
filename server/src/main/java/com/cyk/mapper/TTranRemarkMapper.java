package com.cyk.mapper;

import com.cyk.model.TTranRemark;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface TTranRemarkMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TTranRemark record);

    int insertSelective(TTranRemark record);

    TTranRemark selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TTranRemark record);

    int updateByPrimaryKey(TTranRemark record);

    List<TTranRemark> selectTranRemarkByPage(@Param("tranId") Integer tranId);

    int deleteByTranId(Integer tranId);
}