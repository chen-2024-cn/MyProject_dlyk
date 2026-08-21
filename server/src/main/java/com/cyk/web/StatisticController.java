package com.cyk.web;

import com.cyk.result.NameValue;
import com.cyk.result.R;
import com.cyk.result.SummaryData;
import com.cyk.result.TrendData;
import com.cyk.service.StatisticService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class StatisticController {

    @Resource
    private StatisticService statisticService;

    @GetMapping(value = "/api/summary/data")
    public R summaryData() {
        SummaryData summaryData = statisticService.loadSummaryData();
        return R.OK(summaryData);
    }

    @GetMapping(value = "/api/saleFunnel/data")
    public R saleFunnelData() {
        /**
         * [
         *    { value: 20, name: '成交' },
         *    { value: 60, name: '交易' },
         *    { value: 80, name: '客户' },
         *    { value: 100, name: '线索' }
         * ]
         *
         */
        List<NameValue> nameValueList = statisticService.loadSaleFunnelData();
        return R.OK(nameValueList);
    }

    @GetMapping(value = "/api/sourcePie/data")
    public R sourcePieData() {
        /**
         *   [
         *       { value: 1048, name: 'Search Engine' },
         *       { value: 735, name: 'Direct' },
         *       { value: 580, name: 'Email' },
         *       { value: 484, name: 'Union Ads' },
         *       { value: 300, name: 'Video Ads' }
         *   ]
         *
         */
        List<NameValue> nameValueList = statisticService.loadSourcePieData();
        return R.OK(nameValueList);
    }

    @GetMapping(value = "/api/trend/data")
    public R trendData() {
        TrendData trendData = statisticService.loadTrendData();
        return R.OK(trendData);
    }
}
