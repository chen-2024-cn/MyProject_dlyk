package com.cyk.config.converter;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.metadata.GlobalConfiguration;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.metadata.property.ExcelContentProperty;
import com.cyk.ServerApplication;
import com.cyk.model.TDicValue;
import com.cyk.result.DicEnum;

import java.util.List;

/**
 * 称呼的转换器
 *
 * Excel中的 “先生”  ----> Java类中是 18
 * Excel中的 “女士”  ----> Java类中是 41
 */
public class AppellationConverter implements Converter<Integer> {

    /**
     * 把Excel中的数据转换为Java中的数据
     * 也就是Excel中的 “先生”  ----> Java类中是 18
     *
     * @param cellData
     * @param contentProperty
     * @param globalConfiguration
     * @return
     * @throws Exception
     */
    @Override
    public Integer convertToJavaData(ReadCellData<?> cellData, ExcelContentProperty contentProperty, GlobalConfiguration globalConfiguration) throws Exception {
        //cellData是Excel中读取到的数据，是“先生”、“女士”
        String cellAppellationName = cellData.getStringValue();

        List<TDicValue> tDicValueList = (List<TDicValue>)ServerApplication.cacheMap.get(DicEnum.APPELLATION.getCode());
        for (TDicValue tDicValue : tDicValueList) {
            Integer id  = tDicValue.getId();
            String name = tDicValue.getTypeValue();

            if (cellAppellationName.equals(name)) {
                return id;
            }
        }
        return -1;
    }
}
