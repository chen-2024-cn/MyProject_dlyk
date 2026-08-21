package com.cyk.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TrendData {

    private List<String> monthList;

    private List<Integer> clueNumList;

    private List<Integer> customerNumList;

    private List<BigDecimal> tranAmountList;
}
