package com.pengcheng.system.meeting.room.service;

import com.pengcheng.system.meeting.room.entity.MeetingRoom;

import java.util.List;

/**
 * 会议室服务接口（Phase 4 J5）
 */
public interface MeetingRoomService {

    /** 新增会议室 */
    MeetingRoom create(MeetingRoom room);

    /** 修改会议室 */
    MeetingRoom update(MeetingRoom room);

    /** 删除会议室 */
    void delete(Long id);

    /** 根据 id 获取详情 */
    MeetingRoom getById(Long id);

    /** 查询所有启用的会议室 */
    List<MeetingRoom> listEnabled();

    /** 查询所有会议室（含停用） */
    List<MeetingRoom> listAll();
}
