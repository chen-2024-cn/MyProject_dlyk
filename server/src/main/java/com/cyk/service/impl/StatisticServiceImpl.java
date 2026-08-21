package com.cyk.service.impl;

import com.cyk.manager.StatisticManager;
import com.cyk.result.NameValue;
import com.cyk.result.SummaryData;
import com.cyk.result.TrendData;
import com.cyk.service.StatisticService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatisticServiceImpl implements StatisticService {

    @Resource
    private StatisticManager statisticManager;

    @Override
    public SummaryData loadSummaryData() {
        return statisticManager.loadSummaryData();
    }

    @Override
    public List<NameValue> loadSaleFunnelData() {
        return statisticManager.loadSaleFunnelData();
    }

    @Override
    public List<NameValue> loadSourcePieData() {
        return statisticManager.loadSourcePieData();
    }

    @Override
    public TrendData loadTrendData() {
        return statisticManager.loadTrendData();
    }
}
