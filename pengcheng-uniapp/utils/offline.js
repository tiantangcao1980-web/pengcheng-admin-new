/**
 * offline.js
 *
 * V4.0 闭环⑤ D5 - 三端统一离线缓存方案
 *
 * 功能：
 *   1) 白名单：仅指定的业务键允许离线（默认：customers / notifications / todos / daily-report-draft）
 *   2) TTL：每个键独立 TTL，过期自动失效
 *   3) 冲突合并：list-by-id / object / replace / 自定义 reducer
 *   4) Adapter 抽象：默认 uni.setStorageSync，可注入 Map adapter 做 Node 单测
 *
 * 用法（uniapp 端）：
 *   import offline from '@/utils/offline.js'
 *   offline.set('customers', list)
 *   const cached = offline.get('customers')
 *   offline.merge('customers', incrementalList)
 *
 * 用法（Node 单测）：
 *   const offline = require('./utils/offline.js')
 *
 * 模块格式：CommonJS。uniapp / vue-cli 链路下，babel 可正常支持
 * `import offline from '@/utils/offline.js'`，因为 default 已绑定。
 */

'use strict'

var DEFAULT_WHITELIST = {
  customers: { ttlMs: 30 * 60 * 1000, strategy: 'list-by-id' },
  notifications: { ttlMs: 10 * 60 * 1000, strategy: 'list-by-id' },
  todos: { ttlMs: 60 * 60 * 1000, strategy: 'list-by-id' },
  'daily-report-draft': { ttlMs: 24 * 60 * 60 * 1000, strategy: 'object' }
}

var STORAGE_PREFIX = 'pc_offline_'

function defaultAdapter() {
  if (typeof uni !== 'undefined' && uni && typeof uni.setStorageSync === 'function') {
    return {
      getItem: function (k) {
        try { return uni.getStorageSync(k) || null } catch (_) { return null }
      },
      setItem: function (k, v) { uni.setStorageSync(k, v) },
      removeItem: function (k) { uni.removeStorageSync(k) }
    }
  }
  var map = new Map()
  return {
    getItem: function (k) { return map.has(k) ? map.get(k) : null },
    setItem: function (k, v) { map.set(k, v) },
    removeItem: function (k) { map.delete(k) }
  }
}

var adapter = defaultAdapter()
var whitelist = Object.assign({}, DEFAULT_WHITELIST)
var nowFn = function () { return Date.now() }

function fullKey(key) { return STORAGE_PREFIX + key }

function readEntry(key) {
  var raw = adapter.getItem(fullKey(key))
  if (raw == null) return null
  try {
    return typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch (_) {
    return null
  }
}

function writeEntry(key, entry) {
  adapter.setItem(fullKey(key), JSON.stringify(entry))
}

function isAllowed(key) {
  return Object.prototype.hasOwnProperty.call(whitelist, key)
}

function isExpired(entry) {
  if (!entry || typeof entry.expireAt !== 'number') return true
  return entry.expireAt < nowFn()
}

function set(key, value, options) {
  options = options || {}
  if (!isAllowed(key)) {
    return { ok: false, reason: 'not-in-whitelist' }
  }
  var cfg = whitelist[key]
  var ttl = options.ttlMs || cfg.ttlMs
  var now = nowFn()
  var entry = {
    value: value,
    storedAt: now,
    expireAt: now + ttl,
    strategy: cfg.strategy
  }
  writeEntry(key, entry)
  return { ok: true }
}

function get(key) {
  if (!isAllowed(key)) return null
  var entry = readEntry(key)
  if (!entry || isExpired(entry)) {
    return null
  }
  return entry.value
}

function remove(key) {
  adapter.removeItem(fullKey(key))
}

function mergeListById(prev, next, idKey) {
  var map = new Map()
  for (var i = 0; i < prev.length; i++) {
    var p = prev[i]
    if (p && p[idKey] != null) map.set(p[idKey], p)
  }
  var ordered = []
  for (var j = 0; j < next.length; j++) {
    var n = next[j]
    if (n && n[idKey] != null) {
      map.set(n[idKey], n)
      ordered.push(n[idKey])
    }
  }
  var seen = new Set(ordered)
  var result = []
  for (var k = 0; k < ordered.length; k++) {
    result.push(map.get(ordered[k]))
  }
  for (var m = 0; m < prev.length; m++) {
    var pp = prev[m]
    if (pp && pp[idKey] != null && !seen.has(pp[idKey])) {
      result.push(map.get(pp[idKey]))
    }
  }
  return result
}

function merge(key, incoming, options) {
  options = options || {}
  if (!isAllowed(key)) return { ok: false, reason: 'not-in-whitelist' }
  var cfg = whitelist[key]
  var strategy = options.strategy || cfg.strategy
  var existing = get(key)
  var merged

  if (strategy === 'list-by-id') {
    merged = mergeListById(existing || [], incoming || [], options.idKey || 'id')
  } else if (strategy === 'object') {
    merged = Object.assign({}, existing || {}, incoming || {})
  } else if (strategy === 'replace') {
    merged = incoming
  } else if (typeof options.reducer === 'function') {
    merged = options.reducer(existing, incoming)
  } else {
    merged = incoming
  }
  set(key, merged, options)
  return { ok: true, value: merged }
}

function clearAll() {
  Object.keys(whitelist).forEach(function (k) { remove(k) })
}

function configure(opts) {
  opts = opts || {}
  if (opts.whitelist) {
    whitelist = Object.assign({}, DEFAULT_WHITELIST, opts.whitelist)
  }
  if (opts.adapter) {
    adapter = opts.adapter
  }
  if (typeof opts.now === 'function') {
    nowFn = opts.now
  }
}

function _reset() {
  whitelist = Object.assign({}, DEFAULT_WHITELIST)
  adapter = defaultAdapter()
  nowFn = function () { return Date.now() }
}

var offline = {
  set: set,
  get: get,
  remove: remove,
  merge: merge,
  clearAll: clearAll,
  configure: configure,
  _reset: _reset,
  _internal: {
    mergeListById: mergeListById,
    fullKey: fullKey,
    isExpired: isExpired
  }
}

// CommonJS exports（在 vue-cli/babel 下 default import 会自动绑定到此对象）
module.exports = offline
module.exports.default = offline
module.exports.set = set
module.exports.get = get
module.exports.remove = remove
module.exports.merge = merge
module.exports.clearAll = clearAll
module.exports.configure = configure
module.exports.mergeListById = mergeListById
