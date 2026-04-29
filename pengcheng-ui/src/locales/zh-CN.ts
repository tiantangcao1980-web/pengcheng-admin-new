/**
 * 前端静态词条 — 简体中文。
 * 仅包含纯前端 UI 关键词，业务词条由服务端 /api/i18n/zh-CN.json 合并。
 */
export default {
  // 通用操作
  common: {
    login: '登录',
    logout: '退出登录',
    register: '注册',
    save: '保存',
    cancel: '取消',
    confirm: '确认',
    delete: '删除',
    edit: '编辑',
    create: '新建',
    search: '搜索',
    reset: '重置',
    submit: '提交',
    back: '返回',
    close: '关闭',
    refresh: '刷新',
    export: '导出',
    import: '导入',
    download: '下载',
    upload: '上传',
    preview: '预览',
    copy: '复制',
    detail: '详情',
    more: '更多',
    loading: '加载中...',
    noData: '暂无数据',
  },

  // 反馈
  feedback: {
    success: '操作成功',
    failed: '操作失败',
    saveSuccess: '保存成功',
    deleteSuccess: '删除成功',
    deleteConfirm: '确定要删除吗？',
    networkError: '网络错误，请稍后重试',
    unauthorized: '登录已过期，请重新登录',
    forbidden: '权限不足',
  },

  // 导航/菜单
  menu: {
    dashboard: '首页',
    system: '系统管理',
    org: '组织架构',
    user: '用户管理',
    role: '角色管理',
    menu: '菜单管理',
    log: '日志管理',
    monitor: '系统监控',
  },

  // 表单校验
  validation: {
    required: '此项为必填项',
    email: '请输入有效的邮箱地址',
    minLength: '最少输入 {min} 个字符',
    maxLength: '最多输入 {max} 个字符',
  },

  // 语言切换
  locale: {
    zh: '简体中文',
    en: 'English',
  },
}
