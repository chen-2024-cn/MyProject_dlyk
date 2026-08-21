package com.cyk.manager;

import com.cyk.mapper.TActivityMapper;
import com.cyk.mapper.TClueMapper;
import com.cyk.mapper.TCustomerMapper;
import com.cyk.mapper.TTranMapper;
import com.cyk.result.NameValue;
import com.cyk.result.SummaryData;
import com.cyk.result.TrendData;
import com.cyk.result.TrendPoint;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class StatisticManager {

    @Resource
    private TActivityMapper tActivityMapper;

    @Resource
    private TClueMapper tClueMapper;

    @Resource
    private TCustomerMapper tCustomerMapper;

    @Resource
    private TTranMapper tTranMapper;

    public SummaryData loadSummaryData() {
        //有效的市场活动总数
        Integer effectiveActivityCount = tActivityMapper.selecOngoingActivity().size(); //偷懒了一下

        //总的市场活动数
        Integer totalActivityCount = tActivityMapper.selectByCount();

        //线索总数
        Integer totalClueCount = tClueMapper.selectClueByCount();

        //客户总数
        Integer totalCustomerCount = tCustomerMapper.selectByCount();

        //成功的交易额
        BigDecimal successTranAmount = tTranMapper.selectBySuccessTranAmount();

        //总的交易额（包含成功和不成功的）
        BigDecimal totalTranAmount = tTranMapper.selectByTotalTranAmount();

        return SummaryData.builder()
                .effectiveActivityCount(effectiveActivityCount)
                .totalActivityCount(totalActivityCount)
                .totalClueCount(totalClueCount)
                .totalCustomerCount(totalCustomerCount)
                .successTranAmount(successTranAmount)
                .totalTranAmount(totalTranAmount)
                .build();
    }

    public List<NameValue> loadSaleFunnelData() {
        List<NameValue> resultList = new ArrayList<>();

        /**
         * [
         *    { value: 20, name: '成交' },
         *    { value: 60, name: '交易' },
         *    { value: 80, name: '客户' },
         *    { value: 100, name: '线索' }
         * ]
         *
         */
        int clueCount = tClueMapper.selectClueByCount();
        int customerCount = tCustomerMapper.selectByCount();
        int tranCount = tTranMapper.selectByTotalTranCount();
        int tranSuccessCount = tTranMapper.selectBySuccessTranCount();

        NameValue clue = NameValue.builder().name("线索").value(clueCount).build();
        resultList.add(clue);

        NameValue customer = NameValue.builder().name("客户").value(customerCount).build();
        resultList.add(customer);

        NameValue tran = NameValue.builder().name("交易").value(tranCount).build();
        resultList.add(tran);

        NameValue tranSuccess = NameValue.builder().name("成交").value(tranSuccessCount).build();
        resultList.add(tranSuccess);

        return resultList;
    }

    public List<NameValue> loadSourcePieData() {
        return tClueMapper.selectBySource();
    }

    public TrendData loadTrendData() {
        List<NameValue> clueMonthly = tClueMapper.selectClueByMonth();
        List<NameValue> customerMonthly = tCustomerMapper.selectCustomerByMonth();
        List<TrendPoint> tranMonthly = tTranMapper.selectTranAmountByMonth();

        // 收集所有月份（去重并排序）
        Set<String> monthSet = new LinkedHashSet<>();
        clueMonthly.forEach(m -> monthSet.add(m.getName()));
        customerMonthly.forEach(m -> monthSet.add(m.getName()));
        tranMonthly.forEach(m -> monthSet.add(m.getName()));

        List<String> monthList = new ArrayList<>(monthSet);

        // 将每月数据转为 Map<月份, 数值>
        var clueMap = clueMonthly.stream()
                .collect(Collectors.toMap(NameValue::getName, NameValue::getValue));
        var customerMap = customerMonthly.stream()
                .collect(Collectors.toMap(NameValue::getName, NameValue::getValue));
        var tranMap = tranMonthly.stream()
                .collect(Collectors.toMap(TrendPoint::getName, TrendPoint::getValue));

        List<Integer> clueNumList = monthList.stream().map(m -> clueMap.getOrDefault(m, 0)).collect(Collectors.toList());
        List<Integer> customerNumList = monthList.stream().map(m -> customerMap.getOrDefault(m, 0)).collect(Collectors.toList());
        List<BigDecimal> tranAmountList = monthList.stream()
                .map(m -> tranMap.getOrDefault(m, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP))
                .collect(Collectors.toList());

        return TrendData.builder()
                .monthList(monthList)
                .clueNumList(clueNumList)
                .customerNumList(customerNumList)
                .tranAmountList(tranAmountList)
                .build();
    }
}
