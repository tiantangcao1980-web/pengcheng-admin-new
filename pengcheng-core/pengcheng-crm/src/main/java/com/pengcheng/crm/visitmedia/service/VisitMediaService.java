package com.pengcheng.crm.visitmedia.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pengcheng.common.exception.BizErrorCode;
import com.pengcheng.common.exception.BusinessException;
import com.pengcheng.crm.visitmedia.dto.VisitMediaUpdateDTO;
import com.pengcheng.crm.visitmedia.entity.CustomerVisitMedia;
import com.pengcheng.crm.visitmedia.mapper.CustomerVisitMediaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 多媒体跟进 Service：扩展 customer_visit 的多媒体字段。
 * <p>不动 realty 的 Customer/CustomerVisit Service —— 只往扩展列写值。
 */
@Service
public class VisitMediaService {

    private static final List<String> ALLOWED = Arrays.asList("text", "image", "audio", "video", "mixed");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private CustomerVisitMediaMapper mediaMapper;

    public void updateMedia(VisitMediaUpdateDTO dto) {
        if (dto == null || dto.getVisitId() == null) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "visitId 必填");
        }
        if (dto.getMediaType() != null && !ALLOWED.contains(dto.getMediaType())) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "mediaType 非法");
        }
        if (dto.getVoiceDuration() != null && dto.getVoiceDuration() < 0) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "voiceDuration 不能为负");
        }

        CustomerVisitMedia row = mediaMapper.selectById(dto.getVisitId());
        if (row == null) {
            throw new BusinessException(BizErrorCode.BAD_REQUEST, "跟进记录不存在");
        }
        if (dto.getMediaType() != null) row.setMediaType(dto.getMediaType());
        if (dto.getMediaUrls() != null) {
            try {
                row.setMediaUrls(MAPPER.writeValueAsString(dto.getMediaUrls()));
            } catch (Exception e) {
                throw new BusinessException(BizErrorCode.BAD_REQUEST, "mediaUrls 序列化失败");
            }
        }
        if (dto.getVoiceDuration() != null) row.setVoiceDuration(dto.getVoiceDuration());
        if (dto.getRemark() != null) row.setRemark(dto.getRemark());
        row.setUpdateTime(LocalDateTime.now());
        mediaMapper.updateById(row);
    }

    public CustomerVisitMedia getMedia(Long visitId) {
        return mediaMapper.selectById(visitId);
    }
}
