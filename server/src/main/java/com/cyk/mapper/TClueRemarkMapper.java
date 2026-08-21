package com.cyk.mapper;

import com.cyk.model.TClueRemark;
import jakarta.websocket.server.PathParam;

import java.util.List;

public interface TClueRemarkMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TClueRemark record);

    int insertSelective(TClueRemark record);

    TClueRemark selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TClueRemark record);

    int updateByPrimaryKey(TClueRemark record);

    List<TClueRemark> selectClueRemarkByPage(@PathParam("current")Integer current, @PathParam("clueId") Integer clueId);
}