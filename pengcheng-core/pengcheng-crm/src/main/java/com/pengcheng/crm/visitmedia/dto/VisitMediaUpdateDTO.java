package com.pengcheng.crm.visitmedia.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class VisitMediaUpdateDTO {

    @NotNull
    private Long visitId;

    /** text/image/audio/video/mixed */
    private String mediaType;

    /** MinIO 直传后回写的对象 key 列表 */
    private List<String> mediaUrls;

    /** 语音时长（秒） */
    private Integer voiceDuration;

    private String remark;
}
