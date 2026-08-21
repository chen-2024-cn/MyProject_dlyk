package com.cyk.web;

import com.cyk.ServerApplication;
import com.cyk.model.TActivity;
import com.cyk.model.TDicValue;
import com.cyk.model.TProduct;
import com.cyk.result.DicEnum;
import com.cyk.result.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DicController {

    @GetMapping("/api/dicvalue/{typeCode}")
    public R dicData(@PathVariable("typeCode") String typeCode){
        if (typeCode.equals(DicEnum.ACTIVITY)) {
            List<TActivity> list = (List<TActivity>) ServerApplication.cacheMap.get(typeCode);
            return R.OK(list);
        } else if (typeCode.equals(DicEnum.PRODUCT)) {
            List<TProduct> list = (List<TProduct>) ServerApplication.cacheMap.get(typeCode);
            return R.OK(list);
        } else {
            List<TDicValue> list = (List<TDicValue>) ServerApplication.cacheMap.get(typeCode);
            return R.OK(list);
        }
    }
}
