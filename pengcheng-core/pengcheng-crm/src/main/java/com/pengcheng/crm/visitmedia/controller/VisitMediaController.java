package com.pengcheng.crm.visitmedia.controller;

import com.pengcheng.common.result.Result;
import com.pengcheng.crm.visitmedia.dto.VisitMediaUpdateDTO;
import com.pengcheng.crm.visitmedia.entity.CustomerVisitMedia;
import com.pengcheng.crm.visitmedia.service.VisitMediaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crm/visit-media")
public class VisitMediaController {

    @Autowired
    private VisitMediaService service;

    @PutMapping
    public Result<Void> update(@RequestBody @Valid VisitMediaUpdateDTO dto) {
        service.updateMedia(dto);
        return Result.ok();
    }

    @GetMapping("/{visitId}")
    public Result<CustomerVisitMedia> get(@PathVariable Long visitId) {
        return Result.ok(service.getMedia(visitId));
    }
}
