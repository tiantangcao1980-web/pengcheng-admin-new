package com.pengcheng.crm.tag.controller;

import com.pengcheng.common.result.Result;
import com.pengcheng.crm.tag.entity.CustomerTag;
import com.pengcheng.crm.tag.service.CustomerTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crm/customer-tags")
public class CustomerTagController {

    @Autowired
    private CustomerTagService service;

    @PostMapping
    public Result<CustomerTag> create(@RequestBody CustomerTag tag) {
        return Result.ok(service.createTag(tag));
    }

    @GetMapping
    public Result<List<CustomerTag>> list() {
        return Result.ok(service.listTags());
    }

    @PutMapping("/customer/{customerId}")
    public Result<Void> bind(@PathVariable Long customerId, @RequestBody List<Long> tagIds) {
        service.setCustomerTags(customerId, tagIds);
        return Result.ok();
    }

    @GetMapping("/customer/{customerId}")
    public Result<List<Long>> getByCustomer(@PathVariable Long customerId) {
        return Result.ok(service.listCustomerTagIds(customerId));
    }
}
