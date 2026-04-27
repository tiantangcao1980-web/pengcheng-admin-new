/**
 * 房产模块 API
 *
 * TODO(K2): K2 端点上线后替换各函数内的 mock 数据为真实 request 调用。
 *   - listProjects  -> GET /admin/realty/projects
 *   - getProject    -> GET /admin/realty/projects/:id
 *   - listHouseTypes-> GET /admin/realty/house-types?projectId=:id
 *   - getUnitMatrix -> GET /admin/realty/units/matrix?projectId=:id
 *   - getUnit       -> GET /admin/realty/units/:id
 */

import request from './request'

// ─────────────────────────────────────────────
// Mock 数据（K2 上线前使用）
// ─────────────────────────────────────────────

const MOCK_PROJECTS = [
  {
    id: 1, projectName: '万科翡翠山', developerName: '万科地产',
    address: '北京市朝阳区望京路88号', district: '望京',
    status: 1, projectType: 1,
    coverImage: 'https://picsum.photos/seed/proj1/400/300',
    minPrice: 65000, maxPrice: 85000,
    availableCount: 36, soldCount: 124,
    agencyStartDate: '2024-01-01', agencyEndDate: '2025-12-31',
    contactPerson: '李经理', contactPhone: '13800138001',
    description: '望京核心地段，TOD综合体，交通便利。'
  },
  {
    id: 2, projectName: '融创壹号院', developerName: '融创中国',
    address: '北京市海淀区中关村南路12号', district: '中关村',
    status: 1, projectType: 1,
    coverImage: 'https://picsum.photos/seed/proj2/400/300',
    minPrice: 78000, maxPrice: 110000,
    availableCount: 18, soldCount: 82,
    agencyStartDate: '2024-03-01', agencyEndDate: '2025-08-31',
    contactPerson: '王经理', contactPhone: '13800138002',
    description: '中关村科技园旁，高端住宅社区。'
  },
  {
    id: 3, projectName: '绿地中央广场', developerName: '绿地集团',
    address: '北京市丰台区丽泽商务区', district: '丽泽',
    status: 1, projectType: 3,
    coverImage: 'https://picsum.photos/seed/proj3/400/300',
    minPrice: 42000, maxPrice: 58000,
    availableCount: 55, soldCount: 205,
    agencyStartDate: '2023-06-01', agencyEndDate: '2025-06-30',
    contactPerson: '张经理', contactPhone: '13800138003',
    description: '丽泽金融商务区，商办综合体项目。'
  }
]

const MOCK_HOUSE_TYPES = {
  1: [
    {
      id: 101, projectId: 1, code: 'A1', name: '两居室A',
      bedrooms: 2, livingRooms: 1, bathrooms: 1,
      area: 89.5, insideArea: 76.2, orientation: '南北通透',
      layoutImage: 'https://picsum.photos/seed/ht101/300/300',
      basePrice: 68000, description: '南北通透，采光极佳', enabled: 1
    },
    {
      id: 102, projectId: 1, code: 'B2', name: '三居室B',
      bedrooms: 3, livingRooms: 2, bathrooms: 2,
      area: 128.0, insideArea: 108.5, orientation: '正南朝向',
      layoutImage: 'https://picsum.photos/seed/ht102/300/300',
      basePrice: 72000, description: '超大客厅，三代同堂首选', enabled: 1
    },
    {
      id: 103, projectId: 1, code: 'C1', name: '四居室旗舰',
      bedrooms: 4, livingRooms: 2, bathrooms: 3,
      area: 178.0, insideArea: 150.3, orientation: '东南朝向',
      layoutImage: 'https://picsum.photos/seed/ht103/300/300',
      basePrice: 80000, description: '顶层复式，无遮挡景观', enabled: 1
    }
  ],
  2: [
    {
      id: 201, projectId: 2, code: 'A', name: '精装两居',
      bedrooms: 2, livingRooms: 1, bathrooms: 1,
      area: 95.0, insideArea: 80.0, orientation: '南向',
      layoutImage: 'https://picsum.photos/seed/ht201/300/300',
      basePrice: 85000, description: '精装交付', enabled: 1
    }
  ]
}

const MOCK_UNITS = {
  1: [
    {
      building: '1号楼',
      floors: [
        {
          floor: 18,
          units: [
            { id: 1001, projectId: 1, houseTypeId: 101, building: '1号楼', floor: 18, unitNo: '01', fullNo: '1-18-01', area: 89.5, listPrice: 6080000, status: 'AVAILABLE' },
            { id: 1002, projectId: 1, houseTypeId: 102, building: '1号楼', floor: 18, unitNo: '02', fullNo: '1-18-02', area: 128.0, listPrice: 9216000, status: 'RESERVED' },
            { id: 1003, projectId: 1, houseTypeId: 101, building: '1号楼', floor: 18, unitNo: '03', fullNo: '1-18-03', area: 89.5, listPrice: 6080000, status: 'SOLD' }
          ]
        },
        {
          floor: 17,
          units: [
            { id: 1004, projectId: 1, houseTypeId: 101, building: '1号楼', floor: 17, unitNo: '01', fullNo: '1-17-01', area: 89.5, listPrice: 5960000, status: 'AVAILABLE' },
            { id: 1005, projectId: 1, houseTypeId: 102, building: '1号楼', floor: 17, unitNo: '02', fullNo: '1-17-02', area: 128.0, listPrice: 9011200, status: 'SIGNED' },
            { id: 1006, projectId: 1, houseTypeId: 101, building: '1号楼', floor: 17, unitNo: '03', fullNo: '1-17-03', area: 89.5, listPrice: 5960000, status: 'AVAILABLE' }
          ]
        },
        {
          floor: 16,
          units: [
            { id: 1007, projectId: 1, houseTypeId: 103, building: '1号楼', floor: 16, unitNo: '01', fullNo: '1-16-01', area: 178.0, listPrice: 14240000, status: 'SUBSCRIBED' },
            { id: 1008, projectId: 1, houseTypeId: 101, building: '1号楼', floor: 16, unitNo: '02', fullNo: '1-16-02', area: 89.5, listPrice: 5840000, status: 'SOLD' },
            { id: 1009, projectId: 1, houseTypeId: 102, building: '1号楼', floor: 16, unitNo: '03', fullNo: '1-16-03', area: 128.0, listPrice: 8806400, status: 'AVAILABLE' }
          ]
        }
      ]
    },
    {
      building: '2号楼',
      floors: [
        {
          floor: 18,
          units: [
            { id: 2001, projectId: 1, houseTypeId: 102, building: '2号楼', floor: 18, unitNo: '01', fullNo: '2-18-01', area: 128.0, listPrice: 9216000, status: 'AVAILABLE' },
            { id: 2002, projectId: 1, houseTypeId: 101, building: '2号楼', floor: 18, unitNo: '02', fullNo: '2-18-02', area: 89.5, listPrice: 6080000, status: 'SOLD' }
          ]
        },
        {
          floor: 17,
          units: [
            { id: 2003, projectId: 1, houseTypeId: 102, building: '2号楼', floor: 17, unitNo: '01', fullNo: '2-17-01', area: 128.0, listPrice: 9011200, status: 'RESERVED' },
            { id: 2004, projectId: 1, houseTypeId: 101, building: '2号楼', floor: 17, unitNo: '02', fullNo: '2-17-02', area: 89.5, listPrice: 5960000, status: 'AVAILABLE' }
          ]
        }
      ]
    }
  ]
}

const MOCK_UNIT_DETAIL = {
  1001: { id: 1001, projectId: 1, houseTypeId: 101, building: '1号楼', floor: 18, unitNo: '01', fullNo: '1-18-01', area: 89.5, listPrice: 6080000, status: 'AVAILABLE', remark: '', createTime: '2024-01-15T10:00:00' },
  1002: { id: 1002, projectId: 1, houseTypeId: 102, building: '1号楼', floor: 18, unitNo: '02', fullNo: '1-18-02', area: 128.0, listPrice: 9216000, status: 'RESERVED', remark: '客户预留中', createTime: '2024-01-15T10:00:00' }
}

// ─────────────────────────────────────────────
// API 函数
// ─────────────────────────────────────────────

/**
 * 楼盘列表（分页 + 搜索 + 筛选）
 * TODO(K2): 替换为 request.get('/admin/realty/projects', params)
 *
 * @param {Object} params
 * @param {number} params.page
 * @param {number} [params.pageSize=10]
 * @param {string} [params.keyword]
 * @param {string} [params.region]
 * @param {number} [params.priceMin]
 * @param {number} [params.priceMax]
 */
export function listProjects(params = {}) {
  // TODO(K2): return request.get('/admin/realty/projects', { params })
  return new Promise((resolve) => {
    const { keyword, region, priceMin, priceMax } = params
    let list = [...MOCK_PROJECTS]
    if (keyword) {
      list = list.filter(p => p.projectName.includes(keyword) || p.address.includes(keyword))
    }
    if (region) {
      list = list.filter(p => p.district === region)
    }
    if (priceMin != null) {
      list = list.filter(p => p.minPrice >= priceMin)
    }
    if (priceMax != null) {
      list = list.filter(p => p.maxPrice <= priceMax)
    }
    setTimeout(() => {
      resolve({ code: 200, data: { list, total: list.length, page: params.page || 1 } })
    }, 300)
  })
}

/**
 * 楼盘详情
 * TODO(K2): 替换为 request.get(`/admin/realty/projects/${id}`)
 *
 * @param {number} id
 */
export function getProject(id) {
  // TODO(K2): return request.get(`/admin/realty/projects/${id}`)
  return new Promise((resolve, reject) => {
    const project = MOCK_PROJECTS.find(p => p.id === Number(id))
    setTimeout(() => {
      if (project) {
        resolve({ code: 200, data: project })
      } else {
        reject(new Error('楼盘不存在'))
      }
    }, 200)
  })
}

/**
 * 户型列表
 * TODO(K2): 替换为 request.get('/admin/realty/house-types', { params: { projectId } })
 *
 * @param {number} projectId
 */
export function listHouseTypes(projectId) {
  // TODO(K2): return request.get('/admin/realty/house-types', { params: { projectId } })
  return new Promise((resolve) => {
    const list = MOCK_HOUSE_TYPES[projectId] || []
    setTimeout(() => {
      resolve({ code: 200, data: list })
    }, 200)
  })
}

/**
 * 房源状态矩阵（楼栋 × 楼层 × 房间）
 * TODO(K2): 替换为 request.get('/admin/realty/units/matrix', { params: { projectId } })
 *
 * @param {number} projectId
 */
export function getUnitMatrix(projectId) {
  // TODO(K2): return request.get('/admin/realty/units/matrix', { params: { projectId } })
  return new Promise((resolve) => {
    const matrix = MOCK_UNITS[projectId] || []
    setTimeout(() => {
      resolve({ code: 200, data: matrix })
    }, 300)
  })
}

/**
 * 房源详情
 * TODO(K2): 替换为 request.get(`/admin/realty/units/${id}`)
 *
 * @param {number} id
 */
export function getUnit(id) {
  // TODO(K2): return request.get(`/admin/realty/units/${id}`)
  return new Promise((resolve, reject) => {
    const unit = MOCK_UNIT_DETAIL[Number(id)]
    setTimeout(() => {
      if (unit) {
        resolve({ code: 200, data: unit })
      } else {
        // 从 matrix mock 里找
        let found = null
        for (const buildings of Object.values(MOCK_UNITS)) {
          for (const building of buildings) {
            for (const floorRow of building.floors) {
              const u = floorRow.units.find(u => u.id === Number(id))
              if (u) { found = u; break }
            }
            if (found) break
          }
          if (found) break
        }
        if (found) {
          resolve({ code: 200, data: found })
        } else {
          reject(new Error('房源不存在'))
        }
      }
    }, 200)
  })
}
