package com.pengcheng.hr.attendance.dto;

import java.time.LocalDateTime;

/**
 * 打卡入参（含 GPS 与照片）。
 * 兼容旧字段 {@code location}，新字段为 longitude / latitude / photoUrl。
 */
public class ClockInDTO {
    private Long userId;
    private LocalDateTime clockTime;
    private String location;
    private Double longitude;
    private Double latitude;
    private String photoUrl;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDateTime getClockTime() { return clockTime; }
    public void setClockTime(LocalDateTime clockTime) { this.clockTime = clockTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long userId;
        private LocalDateTime clockTime;
        private String location;
        private Double longitude;
        private Double latitude;
        private String photoUrl;
        public Builder userId(Long v) { this.userId = v; return this; }
        public Builder clockTime(LocalDateTime v) { this.clockTime = v; return this; }
        public Builder location(String v) { this.location = v; return this; }
        public Builder longitude(Double v) { this.longitude = v; return this; }
        public Builder latitude(Double v) { this.latitude = v; return this; }
        public Builder photoUrl(String v) { this.photoUrl = v; return this; }
        public ClockInDTO build() {
            ClockInDTO r = new ClockInDTO();
            r.setUserId(userId);
            r.setClockTime(clockTime);
            r.setLocation(location);
            r.setLongitude(longitude);
            r.setLatitude(latitude);
            r.setPhotoUrl(photoUrl);
            return r;
        }
    }
}
