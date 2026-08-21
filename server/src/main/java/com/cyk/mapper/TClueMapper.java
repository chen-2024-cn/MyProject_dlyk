package com.cyk.mapper;

import com.cyk.model.TActivity;
import com.cyk.model.TClue;
import com.cyk.result.NameValue;

import java.util.List;

public interface TClueMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(TClue record);

    int insertSelective(TClue record);

    TClue selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(TClue record);

    int updateByPrimaryKey(TClue record);

    List<TClue> selectClueByPage(Integer current);

    void saveClue(List<TClue> cachedDataList);

    boolean existsUserById(Integer ownerId);

    int selectByPhone(String phone);

    TClue selectById(Integer id);

    int selectClueByCount();

    List<NameValue> selectBySource();

    List<NameValue> selectClueByMonth();
}