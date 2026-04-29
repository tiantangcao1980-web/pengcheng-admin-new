#!/usr/bin/env node
/**
 * test-offline.js - utils/offline.js 的纯 Node 单测（无 vitest 依赖）
 *
 * 运行：
 *   node pengcheng-uniapp/scripts/test-offline.js
 *
 * 退出码：0 全部通过；1 有失败。
 *
 * 覆盖：
 *   - 白名单（不在白名单的键 set/get 失败）
 *   - TTL 过期返回 null
 *   - list-by-id 合并：增删覆盖、保持 next 顺序优先、prev 独有的兜底
 *   - object 合并：浅 merge
 *   - replace 策略
 *   - 自定义 reducer
 *   - clearAll
 *   - configure 注入 adapter / now
 */
'use strict'

const path = require('path')
const offline = require(path.resolve(__dirname, '../utils/offline.js'))

let passed = 0
let failed = 0
const failures = []

function assert(label, cond, detail) {
  if (cond) {
    passed++
    console.log('  PASS  -', label)
  } else {
    failed++
    failures.push(label + (detail ? ' :: ' + detail : ''))
    console.error('  FAIL  -', label, detail || '')
  }
}

function deepEqual(a, b) {
  return JSON.stringify(a) === JSON.stringify(b)
}

function makeMapAdapter() {
  const map = new Map()
  return {
    map,
    getItem: (k) => (map.has(k) ? map.get(k) : null),
    setItem: (k, v) => map.set(k, v),
    removeItem: (k) => map.delete(k)
  }
}

function suite(title, fn) {
  console.log('\n[suite] ' + title)
  fn()
}

// -----------------------------------------------------------------------------
suite('whitelist', () => {
  offline._reset()
  offline.configure({ adapter: makeMapAdapter() })

  const r = offline.set('not-in-whitelist', { foo: 1 })
  assert('non-whitelist set returns ok=false', r.ok === false && r.reason === 'not-in-whitelist')
  assert('non-whitelist get returns null', offline.get('not-in-whitelist') === null)
})

// -----------------------------------------------------------------------------
suite('TTL', () => {
  offline._reset()
  offline.configure({ adapter: makeMapAdapter(), now: () => 1_000_000 })

  offline.set('customers', [{ id: 1 }])
  assert('value is fresh', deepEqual(offline.get('customers'), [{ id: 1 }]))

  // 把 now 推进 60 分钟（默认 customers TTL 30 分钟）
  offline.configure({ now: () => 1_000_000 + 60 * 60 * 1000 + 1 })
  assert('value expired returns null', offline.get('customers') === null)
})

suite('TTL override via options', () => {
  offline._reset()
  offline.configure({ adapter: makeMapAdapter(), now: () => 100 })
  offline.set('customers', [{ id: 1 }], { ttlMs: 50 })
  offline.configure({ now: () => 200 })
  assert('custom ttlMs respected (expired)', offline.get('customers') === null)
})

// -----------------------------------------------------------------------------
suite('list-by-id merge', () => {
  offline._reset()
  offline.configure({ adapter: makeMapAdapter() })

  offline.set('customers', [{ id: 1, name: 'a' }, { id: 2, name: 'b' }])
  offline.merge('customers', [{ id: 2, name: 'B' }, { id: 3, name: 'c' }])
  const after = offline.get('customers')
  // next 顺序在前 (2,3)，prev 独有 (1) 兜底
  assert('merged length is 3', after.length === 3)
  assert('id=2 is overridden', after.find((x) => x.id === 2).name === 'B')
  assert('id=1 retained as fallback', !!after.find((x) => x.id === 1))
  assert('id=3 added', !!after.find((x) => x.id === 3))
  assert('order: next first', after[0].id === 2 && after[1].id === 3)
})

suite('list-by-id merge with empty existing', () => {
  offline._reset()
  offline.configure({ adapter: makeMapAdapter() })
  const r = offline.merge('customers', [{ id: 1 }])
  assert('empty existing merge', deepEqual(r.value, [{ id: 1 }]))
})

suite('list-by-id merge with custom idKey', () => {
  offline._reset()
  offline.configure({ adapter: makeMapAdapter() })
  offline.set('customers', [{ pk: 'a', v: 1 }])
  offline.merge('customers', [{ pk: 'a', v: 2 }, { pk: 'b', v: 3 }], { idKey: 'pk' })
  const after = offline.get('customers')
  assert('custom idKey works', after.find((x) => x.pk === 'a').v === 2 && after.length === 2)
})

// -----------------------------------------------------------------------------
suite('object merge', () => {
  offline._reset()
  offline.configure({ adapter: makeMapAdapter() })
  offline.set('daily-report-draft', { title: 't1', body: 'old' })
  offline.merge('daily-report-draft', { body: 'new', extra: 'x' })
  const after = offline.get('daily-report-draft')
  assert('object shallow merge', after.title === 't1' && after.body === 'new' && after.extra === 'x')
})

// -----------------------------------------------------------------------------
suite('replace strategy', () => {
  offline._reset()
  offline.configure({ adapter: makeMapAdapter() })
  offline.set('customers', [{ id: 1 }])
  offline.merge('customers', [{ id: 99 }], { strategy: 'replace' })
  assert('replace overrides', deepEqual(offline.get('customers'), [{ id: 99 }]))
})

// -----------------------------------------------------------------------------
suite('custom reducer', () => {
  offline._reset()
  offline.configure({ adapter: makeMapAdapter() })
  offline.set('customers', [{ id: 1 }, { id: 2 }])
  offline.merge('customers', [{ id: 2 }, { id: 3 }], {
    strategy: 'custom',
    reducer: (prev, next) => (prev || []).concat(next || [])
  })
  const after = offline.get('customers')
  assert('reducer concat', after.length === 4)
})

// -----------------------------------------------------------------------------
suite('clearAll & remove', () => {
  offline._reset()
  offline.configure({ adapter: makeMapAdapter() })
  offline.set('customers', [{ id: 1 }])
  offline.set('todos', [{ id: 1 }])
  offline.remove('customers')
  assert('remove cleared customers', offline.get('customers') === null)
  assert('todos retained', offline.get('todos') !== null)

  offline.clearAll()
  assert('clearAll cleared todos', offline.get('todos') === null)
})

// -----------------------------------------------------------------------------
suite('whitelist override via configure', () => {
  offline._reset()
  offline.configure({
    adapter: makeMapAdapter(),
    whitelist: { custom: { ttlMs: 1000, strategy: 'object' } }
  })
  // 默认白名单仍生效
  assert('default whitelist kept', offline.set('customers', []).ok === true)
  assert('custom whitelist added', offline.set('custom', { a: 1 }).ok === true)
})

// -----------------------------------------------------------------------------
suite('mergeListById direct', () => {
  const r = offline.mergeListById(
    [{ id: 1, name: 'a' }, { id: 2, name: 'b' }],
    [{ id: 2, name: 'B' }, { id: 3, name: 'c' }],
    'id'
  )
  assert('direct mergeListById', r.length === 3 && r[0].id === 2 && r[1].id === 3 && r[2].id === 1)
})

// -----------------------------------------------------------------------------
console.log('\n=========================================')
console.log(`SUMMARY: ${passed} passed, ${failed} failed`)
if (failed > 0) {
  console.log('FAILURES:')
  failures.forEach((f) => console.log('  -', f))
  process.exit(1)
} else {
  process.exit(0)
}
