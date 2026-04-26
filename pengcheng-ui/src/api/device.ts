import { request } from '@/utils/request'

export interface UserLoginDevice {
  id: number
  userId: number
  tokenValue: string
  clientType: string
  deviceId?: string
  deviceName?: string
  os?: string
  browser?: string
  ip?: string
  location?: string
  loginTime: string
  lastActive: string
  status: number
}

export const deviceApi = {
  /** 我的设备列表 */
  myDevices(): Promise<UserLoginDevice[]> {
    return request({
      url: '/auth/device',
      method: 'get'
    })
  },

  /** 踢下线一台设备 */
  kickout(id: number): Promise<void> {
    return request({
      url: `/auth/device/${id}/kickout`,
      method: 'post'
    })
  }
}
