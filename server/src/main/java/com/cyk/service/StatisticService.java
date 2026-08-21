package com.cyk.service;

import com.cyk.result.NameValue;
import com.cyk.result.SummaryData;
import com.cyk.result.TrendData;

import java.util.List;

public interface StatisticService {

    SummaryData loadSummaryData();

    List<NameValue> loadSaleFunnelData();

    List<NameValue> loadSourcePieData();

    TrendData loadTrendData();
}
