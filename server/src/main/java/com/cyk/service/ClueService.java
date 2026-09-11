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

    /**
     * 线索分页查询。
     *
     * @param current 页码（从 1 开始，null 时由 Controller 兜底为 1）
     * @param query   筛选条件（可为 null，表示不过滤）——姓名/手机/负责人/来源/意向状态/是否已转化等
     */
    PageInfo<TClue> getClueByPage(Integer current, ClueQuery query);

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
