package com.hnu.legal_cases.controller;

import com.hnu.legal_cases.dto.payment.CreateOrderReqVO;
import com.hnu.legal_cases.dto.payment.CreateOrderResultVO;
import com.hnu.legal_cases.dto.payment.MockConfirmReqVO;
import com.hnu.legal_cases.dto.payment.PaymentPackageVO;
import com.hnu.legal_cases.exception.ServiceException;
import com.hnu.legal_cases.service.PaymentService;
import com.hnu.legal_cases.util.JSONReturnBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping(value = "/packages", produces = "application/json")
    public JSONReturnBean<List<PaymentPackageVO>> packages(@RequestParam(defaultValue = "zh") String language) {
        try {
            return JSONReturnBean.success(paymentService.listPackages(language));
        } catch (Throwable e) {
            log.error("packages", e);
            return JSONReturnBean.failed("加载套餐失败");
        }
    }

    @PostMapping(value = "/order", consumes = "application/json", produces = "application/json")
    public JSONReturnBean<CreateOrderResultVO> createOrder(@RequestBody CreateOrderReqVO req) {
        try {
            if (req == null || req.getUserId() == null || req.getPackageId() == null) {
                return JSONReturnBean.failed("参数不完整");
            }
            return JSONReturnBean.success(paymentService.createOrder(req.getUserId(), req.getPackageId()));
        } catch (ServiceException e) {
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("createOrder", e);
            return JSONReturnBean.failed("创建订单失败");
        }
    }

    @PostMapping(value = "/mock/confirm", consumes = "application/json", produces = "application/json")
    public JSONReturnBean<String> mockConfirm(@RequestBody MockConfirmReqVO req) {
        try {
            if (req == null || req.getUserId() == null || req.getOrderNo() == null) {
                return JSONReturnBean.failed("参数不完整");
            }
            paymentService.mockConfirm(req.getUserId(), req.getOrderNo());
            return JSONReturnBean.success("ok");
        } catch (ServiceException e) {
            return JSONReturnBean.failed(e.getMessage());
        } catch (Throwable e) {
            log.error("mockConfirm", e);
            return JSONReturnBean.failed("确认失败");
        }
    }

    @GetMapping(value = "/channels", produces = "application/json")
    public JSONReturnBean<Map<String, Object>> channels() {
        return JSONReturnBean.success(paymentService.channelInfo());
    }
}
