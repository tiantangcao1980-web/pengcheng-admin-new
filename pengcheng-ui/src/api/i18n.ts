import { request } from '@/utils/request'

/**
 * 获取服务端指定 locale 的全量词条。
 * 端点：GET /api/i18n/{locale}.json → { [key: string]: string }
 */
export function getLocaleMessages(locale: string): Promise<Record<string, string>> {
  return request<Record<string, string>>({
    url: `/i18n/${locale}.json`,
    method: 'get',
    _silent: true,
  } as any)
}

/**
 * 更新当前登录用户的语言偏好（持久化到服务端）。
 * 端点：PUT /app/me/locale
 */
export function setMyLocale(locale: string): Promise<void> {
  return request<void>({
    baseURL: '/',
    url: '/app/me/locale',
    method: 'put',
    data: { locale },
    _silent: true,
  } as any)
}
