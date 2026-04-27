package com.pengcheng.system.meeting.room.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pengcheng.system.meeting.room.entity.MeetingRoom;
import com.pengcheng.system.meeting.room.mapper.MeetingRoomMapper;
import com.pengcheng.system.meeting.room.service.MeetingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会议室服务实现（Phase 4 J5）
 */
@Service
@RequiredArgsConstructor
public class MeetingRoomServiceImpl implements MeetingRoomService {

    private final MeetingRoomMapper meetingRoomMapper;

    @Override
    public MeetingRoom create(MeetingRoom room) {
        meetingRoomMapper.insert(room);
        return room;
    }

    @Override
    public MeetingRoom update(MeetingRoom room) {
        meetingRoomMapper.updateById(room);
        return room;
    }

    @Override
    public void delete(Long id) {
        meetingRoomMapper.deleteById(id);
    }

    @Override
    public MeetingRoom getById(Long id) {
        return meetingRoomMapper.selectById(id);
    }

    @Override
    public List<MeetingRoom> listEnabled() {
        return meetingRoomMapper.selectList(
                new LambdaQueryWrapper<MeetingRoom>()
                        .eq(MeetingRoom::getEnabled, 1)
                        .orderByAsc(MeetingRoom::getName)
        );
    }

    @Override
    public List<MeetingRoom> listAll() {
        return meetingRoomMapper.selectList(
                new LambdaQueryWrapper<MeetingRoom>()
                        .orderByAsc(MeetingRoom::getName)
        );
    }
}
