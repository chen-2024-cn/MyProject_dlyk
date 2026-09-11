package com.cyk.service;

import com.cyk.model.TTran;
import com.cyk.model.TTranHistory;
import com.cyk.model.TTranRemark;
import com.cyk.query.TranQuery;
import com.cyk.query.TranRemarkQuery;
import com.github.pagehelper.PageInfo;

import java.util.List;
import java.util.Map;

public interface TranService {

    /**
     * 交易分页查询。
     *
     * @param current 页码（从 1 开始）
     * @param query   筛选条件（可为 null）——客户/阶段/流水号/金额阈值
     */
    PageInfo<TTran> getTranByPage(Integer current, TranQuery query);

    int saveTran(TranQuery tranQuery);

    TTran getTranById(Integer id);

    int updateTran(TranQuery tranQuery);

    int deleteTran(Integer id);

    int deleteTranBatch(List<Integer> ids);

    void changeStage(Integer tranId, Integer stage, java.math.BigDecimal money, java.util.Date expectedDate, String token);

    List<TTranHistory> getHistoryByTranId(Integer tranId);

    PageInfo<TTran> getTransByCustomerId(Integer current, Integer customerId);

    PageInfo<TTranRemark> getRemarkByPage(Integer current, Integer tranId);

    int addRemark(TranRemarkQuery remarkQuery);

    int updateRemark(TranRemarkQuery remarkQuery);

    int deleteRemark(Integer id);

    List<Map<String, Object>> getCustomerOptions();
}
