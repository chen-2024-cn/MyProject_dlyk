package com.cyk.service;

import com.cyk.model.TClue;
import com.cyk.model.TClueRemark;
import com.cyk.model.TDicValue;
import com.cyk.query.ClueQuery;
import com.cyk.query.ClueRemarkQuery;
import com.github.pagehelper.PageInfo;

import java.io.InputStream;
import java.util.List;


public interface ClueService {
    PageInfo<TClue> getClueByPage(Integer current);

    void importExcel(InputStream inputStream, String token);

    boolean checkPhone(String phone);

    int saveClue(ClueQuery clueQuery);

    TClue getClueById(Integer id);

    int updateClue(ClueQuery clueQuery);

    PageInfo<TClueRemark> getClueRemarkByPage(Integer current, Integer clueId);

    int addClueRemark(ClueRemarkQuery clueRemarkQuery);

    int updateClueRemark(ClueRemarkQuery clueRemarkQuery);

    int deleteClueRemark(Integer id);

    int deleteClue(Integer id);

    int deleteClueBatch(List<Integer> idList);
}
