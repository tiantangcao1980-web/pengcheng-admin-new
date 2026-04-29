/**
 * Front-end static messages — English (US).
 * UI-only keys; business messages are merged from server /api/i18n/en-US.json.
 */
export default {
  // Common actions
  common: {
    login: 'Login',
    logout: 'Logout',
    register: 'Register',
    save: 'Save',
    cancel: 'Cancel',
    confirm: 'Confirm',
    delete: 'Delete',
    edit: 'Edit',
    create: 'Create',
    search: 'Search',
    reset: 'Reset',
    submit: 'Submit',
    back: 'Back',
    close: 'Close',
    refresh: 'Refresh',
    export: 'Export',
    import: 'Import',
    download: 'Download',
    upload: 'Upload',
    preview: 'Preview',
    copy: 'Copy',
    detail: 'Detail',
    more: 'More',
    loading: 'Loading...',
    noData: 'No data',
  },

  // Feedback
  feedback: {
    success: 'Operation succeeded',
    failed: 'Operation failed',
    saveSuccess: 'Saved successfully',
    deleteSuccess: 'Deleted successfully',
    deleteConfirm: 'Are you sure you want to delete?',
    networkError: 'Network error, please try again later',
    unauthorized: 'Session expired, please login again',
    forbidden: 'Insufficient permissions',
  },

  // Navigation / menus
  menu: {
    dashboard: 'Dashboard',
    system: 'System',
    org: 'Organization',
    user: 'Users',
    role: 'Roles',
    menu: 'Menus',
    log: 'Logs',
    monitor: 'Monitor',
  },

  // Form validation
  validation: {
    required: 'This field is required',
    email: 'Please enter a valid email address',
    minLength: 'At least {min} characters required',
    maxLength: 'At most {max} characters allowed',
  },

  // Locale switcher
  locale: {
    zh: '简体中文',
    en: 'English',
  },
}
