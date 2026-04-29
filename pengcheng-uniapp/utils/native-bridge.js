/**
 * native-bridge.js
 *
 * V4.0 闭环⑤ D5 - 原生能力桥接层
 *
 * 统一封装 uni-app 与 5+ App 原生能力（极光推送、定位、相机、扫码），
 * 暴露 Promise 风格 API，避免 pages/ 层直接调用 plus.* 导致小程序端编译错误。
 *
 * 设计要点：
 *   - 平台保护：所有 plus.* 仅在 #ifdef APP-PLUS 范围内使用，对小程序自动降级
 *   - 单例：push 事件回调只注册一次
 *   - 失败 reject 含 code+msg，调用方可分类处理
 *
 * 用法（vue 组件）：
 *   import nativeBridge from '@/utils/native-bridge.js'
 *   await nativeBridge.push.init({ debug: true })
 *   const loc = await nativeBridge.location.getOnce({ enableHighAccuracy: true })
 */

const isApp = typeof plus !== 'undefined'
const isWeixinMp = typeof wx !== 'undefined' && typeof wx.login === 'function' && !isApp

// -----------------------------------------------------------------------------
// 推送：极光首选；所有方法 Promise 化；事件用 emitter 暴露
// -----------------------------------------------------------------------------
const pushListeners = {
  click: [],
  receive: [],
  registered: []
}
let pushInited = false

const push = {
  /** 初始化推送（极光） */
  init(options = {}) {
    return new Promise((resolve) => {
      if (!isApp) {
        // 小程序 / H5 上无原生推送，自动降级（订阅消息由后端通道决策处理）
        resolve({ skipped: true, reason: 'not-app' })
        return
      }
      if (pushInited) {
        resolve({ skipped: true, reason: 'already-inited' })
        return
      }
      try {
        // 极光 SDK 注入后挂在 plus.push 之外的命名空间，但 uni-app 推送 API
        // 已经把 jpush / umeng / getui 三家统一封装到 uni.getPushClientId
        uni.getPushClientId({
          success: (res) => {
            pushInited = true
            pushListeners.registered.forEach((fn) => fn(res))
            resolve({ inited: true, clientId: res && res.cid })
          },
          fail: (err) => resolve({ inited: false, error: err })
        })
        uni.onPushMessage((msg) => {
          if (msg.type === 'click') {
            pushListeners.click.forEach((fn) => fn(msg))
          } else {
            pushListeners.receive.forEach((fn) => fn(msg))
          }
        })
      } catch (ex) {
        resolve({ inited: false, error: { code: 'INIT_EX', msg: String(ex) } })
      }
    })
  },

  /** 获取 clientId（推送注册 ID） */
  getClientId() {
    return new Promise((resolve, reject) => {
      if (!isApp) {
        resolve('')
        return
      }
      uni.getPushClientId({
        success: (res) => resolve(res.cid),
        fail: (err) => reject({ code: 'GET_CID_FAIL', msg: err && err.errMsg, raw: err })
      })
    })
  },

  on(event, fn) {
    if (pushListeners[event]) {
      pushListeners[event].push(fn)
    }
  },

  off(event, fn) {
    const arr = pushListeners[event]
    if (!arr) return
    const idx = arr.indexOf(fn)
    if (idx >= 0) arr.splice(idx, 1)
  },

  /** 重置（仅测试） */
  _reset() {
    pushInited = false
    Object.keys(pushListeners).forEach((k) => (pushListeners[k] = []))
  }
}

// -----------------------------------------------------------------------------
// 定位
// -----------------------------------------------------------------------------
const location = {
  /**
   * 获取一次定位
   * @param {{enableHighAccuracy?: boolean, type?: 'wgs84'|'gcj02', timeout?: number}} options
   */
  getOnce(options = {}) {
    return new Promise((resolve, reject) => {
      uni.getLocation({
        type: options.type || 'gcj02',
        isHighAccuracy: !!options.enableHighAccuracy,
        highAccuracyExpireTime: options.timeout || 4000,
        success: (res) => resolve(res),
        fail: (err) =>
          reject({ code: 'LOCATION_FAIL', msg: err && err.errMsg, raw: err })
      })
    })
  },

  /** 持续监听（仅 APP） */
  watch(onChange) {
    if (!isApp) {
      return () => {}
    }
    uni.onLocationChange(onChange)
    uni.startLocationUpdateBackground({
      success: () => {}
    })
    return () => {
      uni.stopLocationUpdate()
      uni.offLocationChange(onChange)
    }
  }
}

// -----------------------------------------------------------------------------
// 相机
// -----------------------------------------------------------------------------
const camera = {
  /**
   * 拍照 / 从相册选择
   * @param {{count?: number, sourceType?: ('album'|'camera')[], sizeType?: ('original'|'compressed')[] }} options
   */
  pickImage(options = {}) {
    return new Promise((resolve, reject) => {
      uni.chooseImage({
        count: options.count || 1,
        sourceType: options.sourceType || ['album', 'camera'],
        sizeType: options.sizeType || ['compressed'],
        success: (res) => resolve(res.tempFilePaths || []),
        fail: (err) => reject({ code: 'PICK_IMAGE_FAIL', msg: err && err.errMsg, raw: err })
      })
    })
  },

  /** 直接调用相机拍照 */
  takePhoto() {
    return this.pickImage({ count: 1, sourceType: ['camera'] }).then((arr) => arr[0] || '')
  }
}

// -----------------------------------------------------------------------------
// 扫一扫
// -----------------------------------------------------------------------------
const scan = {
  /**
   * 扫码
   * @param {{scanType?: ('barCode'|'qrCode')[], onlyFromCamera?: boolean}} options
   */
  scan(options = {}) {
    return new Promise((resolve, reject) => {
      uni.scanCode({
        scanType: options.scanType || ['qrCode', 'barCode'],
        onlyFromCamera: options.onlyFromCamera !== false,
        success: (res) => resolve(res),
        fail: (err) => reject({ code: 'SCAN_FAIL', msg: err && err.errMsg, raw: err })
      })
    })
  }
}

// -----------------------------------------------------------------------------
// 平台标识 + OCR 上传辅助（OCR 走后端，前端只负责拍照 + 上传）
// -----------------------------------------------------------------------------
function getPlatform() {
  if (isApp) return 'app'
  if (isWeixinMp) return 'mp-weixin'
  return 'h5'
}

const nativeBridge = {
  push,
  location,
  camera,
  scan,
  getPlatform,
  isApp: () => isApp
}

export default nativeBridge
export { push, location, camera, scan, getPlatform }
