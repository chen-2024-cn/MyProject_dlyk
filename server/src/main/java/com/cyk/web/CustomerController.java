package com.cyk.web;

import com.alibaba.excel.EasyExcel;
import com.cyk.constants.Constants;
import com.cyk.model.TCustomer;
import com.cyk.query.CustomerQuery;
import com.cyk.result.CustomerExcel;
import com.cyk.result.R;
import com.cyk.service.CustomerService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
public class CustomerController {

    @Resource
    private CustomerService customerService;

    @PostMapping(value = "/api/clue/customer")
    public R convertCustomer( CustomerQuery customerQuery, @RequestHeader(value = "Authorization") String token) {
        customerQuery.setToken(token);
        Boolean convert = customerService.convertCustomer(customerQuery);
        return convert ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('customer:view')")
    @GetMapping(value = "/api/customer/{id}")
    public R customerDetail(@PathVariable Integer id) {
        TCustomer customer = customerService.getCustomerById(id);
        return R.OK(customer);
    }

    @PreAuthorize("hasAuthority('customer:delete')")
    @DeleteMapping(value = "/api/customer/{id}")
    public R deleteCustomer(@PathVariable Integer id) {
        Boolean deleted = customerService.deleteCustomer(id);
        return deleted ? R.OK() : R.FAIL();
    }

    @PreAuthorize("hasAuthority('customer:list')")
    @GetMapping(value = "/api/customers")
    public R cluePage(@RequestParam(value = "current", required = false) Integer current) {
        if (current == null) {
            current = 1;
        }

        PageInfo<TCustomer> pageInfo = customerService.getCustomerByPage(current);
        return R.OK(pageInfo);
    }

    /**
     * 导出Excel
     *
     * @param response
     * @throws IOException
     */
    @PreAuthorize("hasAuthority('customer:export')")
    @GetMapping(value = "/api/exportExcel")
    public void exportExcel(HttpServletResponse response, @RequestParam(value = "ids", required = false) String ids) throws IOException {

        //要想让浏览器弹出下载框，你后端要设置一下响应头信息
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(Constants.EXCEL_FILE_NAME+System.currentTimeMillis(), StandardCharsets.UTF_8) + ".xlsx");

        //2、后端查询数据库的数据，把数据写入Excel，然后把Excel以IO流的方式输出到前端浏览器（我们来实现）

        List<String> idList = StringUtils.hasText(ids) ? Arrays.asList(ids.split(",")) : new ArrayList<>();
        List<CustomerExcel> dataList = customerService.getCustomerByExcel(idList);

        EasyExcel.write(response.getOutputStream(), CustomerExcel.class)
                .sheet()
                .doWrite(dataList);
    }
}
