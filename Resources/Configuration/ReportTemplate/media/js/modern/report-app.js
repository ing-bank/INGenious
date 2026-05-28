/**
 * INGenious Modern Report - Main Application
 * Alpine.js-based reactive report application
 * Version: 1.0.0
 * 
 * This file provides the core functionality for the modern HTML reports,
 * including data management, filtering, theming, and UI interactions.
 */

'use strict';

// ============================================
// ALPINE.JS CDN CONFIGURATION
// ============================================

// Alpine.js is loaded via CDN in the HTML templates
// This file provides the Alpine components and utilities

// ============================================
// CONSTANTS & CONFIGURATION
// ============================================

const REPORT_CONFIG = {
  version: '1.0.0',
  themes: ['light', 'dark'],
  accents: ['indigo', 'sky', 'orange', 'fuchsia'],
  defaultTheme: 'light',
  defaultAccent: 'indigo',
  storageKeys: {
    theme: 'ingenious-report-theme',
    accent: 'ingenious-report-accent',
    tableSettings: 'ingenious-report-table-settings',
    viewPreferences: 'ingenious-report-view-prefs'
  },
  statuses: {
    passed: ['passed', 'pass', 'done', 'success'],
    failed: ['failed', 'fail', 'error'],
    warning: ['warning', 'skipped', 'incomplete', 'norun']
  },
  chartColors: {
    passed: 'rgb(16, 185, 129)',
    failed: 'rgb(239, 68, 68)',
    warning: 'rgb(245, 158, 11)',
    info: 'rgb(59, 130, 246)',
    neutral: 'rgb(156, 163, 175)'
  },
  dateFormats: {
    full: { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' },
    short: { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' },
    date: { year: 'numeric', month: 'short', day: 'numeric' },
    time: { hour: '2-digit', minute: '2-digit', second: '2-digit' }
  }
};

// ============================================
// UTILITY FUNCTIONS
// ============================================

const Utils = {
  /**
   * Format duration from milliseconds to human-readable string
   */
  formatDuration(ms) {
    if (ms == null || isNaN(ms)) return '-';
    
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    
    if (hours > 0) {
      return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
    } else if (minutes > 0) {
      return `${minutes}m ${seconds % 60}s`;
    } else if (seconds > 0) {
      return `${seconds}.${Math.floor((ms % 1000) / 100)}s`;
    } else {
      return `${ms}ms`;
    }
  },

  /**
   * Format date to localized string
   */
  formatDate(dateString, format = 'full') {
    if (!dateString) return '-';
    
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) return dateString;
      
      return date.toLocaleString(undefined, REPORT_CONFIG.dateFormats[format]);
    } catch (e) {
      return dateString;
    }
  },

  /**
   * Calculate percentage with optional decimal places
   */
  percentage(value, total, decimals = 1) {
    if (total === 0) return 0;
    return Number(((value / total) * 100).toFixed(decimals));
  },

  /**
   * Get status category from status string
   */
  getStatusCategory(status) {
    const lower = (status || '').toLowerCase().trim();
    
    if (REPORT_CONFIG.statuses.passed.includes(lower)) return 'passed';
    if (REPORT_CONFIG.statuses.failed.includes(lower)) return 'failed';
    if (REPORT_CONFIG.statuses.warning.includes(lower)) return 'warning';
    
    return 'neutral';
  },

  /**
   * Get CSS class for status badge
   */
  getStatusBadgeClass(status) {
    const category = this.getStatusCategory(status);
    return `badge--${category}`;
  },

  /**
   * Debounce function
   */
  debounce(fn, delay = 300) {
    let timeoutId;
    return function (...args) {
      clearTimeout(timeoutId);
      timeoutId = setTimeout(() => fn.apply(this, args), delay);
    };
  },

  /**
   * Deep clone object
   */
  clone(obj) {
    return JSON.parse(JSON.stringify(obj));
  },

  /**
   * Local storage helpers
   */
  storage: {
    get(key, defaultValue = null) {
      try {
        const item = localStorage.getItem(key);
        return item ? JSON.parse(item) : defaultValue;
      } catch {
        return defaultValue;
      }
    },
    
    set(key, value) {
      try {
        localStorage.setItem(key, JSON.stringify(value));
        return true;
      } catch {
        return false;
      }
    },
    
    remove(key) {
      try {
        localStorage.removeItem(key);
        return true;
      } catch {
        return false;
      }
    }
  },

  /**
   * Generate unique ID
   */
  uniqueId(prefix = 'id') {
    return `${prefix}_${Date.now().toString(36)}_${Math.random().toString(36).substr(2, 9)}`;
  },

  /**
   * Sort array of objects by key
   */
  sortBy(arr, key, direction = 'asc') {
    return [...arr].sort((a, b) => {
      let valA = a[key];
      let valB = b[key];
      
      // Handle null/undefined
      if (valA == null) return direction === 'asc' ? 1 : -1;
      if (valB == null) return direction === 'asc' ? -1 : 1;
      
      // String comparison
      if (typeof valA === 'string') {
        valA = valA.toLowerCase();
        valB = valB.toLowerCase();
      }
      
      if (valA < valB) return direction === 'asc' ? -1 : 1;
      if (valA > valB) return direction === 'asc' ? 1 : -1;
      return 0;
    });
  },

  /**
   * Filter array by search query
   */
  searchFilter(arr, query, keys) {
    if (!query || !query.trim()) return arr;
    
    const lowerQuery = query.toLowerCase().trim();
    
    return arr.filter(item => {
      return keys.some(key => {
        const value = item[key];
        if (value == null) return false;
        return String(value).toLowerCase().includes(lowerQuery);
      });
    });
  },

  /**
   * Group array by key
   */
  groupBy(arr, key) {
    return arr.reduce((groups, item) => {
      const value = item[key] || 'Other';
      (groups[value] = groups[value] || []).push(item);
      return groups;
    }, {});
  },

  /**
   * Escape HTML for safe insertion
   */
  escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }
};

// ============================================
// THEME MANAGER
// ============================================

const ThemeManager = {
  currentTheme: REPORT_CONFIG.defaultTheme,
  currentAccent: REPORT_CONFIG.defaultAccent,

  init() {
    // Load saved preferences
    this.currentTheme = Utils.storage.get(REPORT_CONFIG.storageKeys.theme, REPORT_CONFIG.defaultTheme);
    this.currentAccent = Utils.storage.get(REPORT_CONFIG.storageKeys.accent, REPORT_CONFIG.defaultAccent);
    
    // Check for system preference
    if (this.currentTheme === 'system') {
      this.currentTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }
    
    // Apply theme
    this.apply();
    
    // Listen for system preference changes
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (Utils.storage.get(REPORT_CONFIG.storageKeys.theme) === 'system') {
        this.setTheme(e.matches ? 'dark' : 'light', false);
      }
    });
  },

  setTheme(theme, save = true) {
    this.currentTheme = theme;
    if (save) {
      Utils.storage.set(REPORT_CONFIG.storageKeys.theme, theme);
    }
    this.apply();
  },

  setAccent(accent, save = true) {
    this.currentAccent = accent;
    if (save) {
      Utils.storage.set(REPORT_CONFIG.storageKeys.accent, accent);
    }
    this.apply();
  },

  toggleTheme() {
    const newTheme = this.currentTheme === 'dark' ? 'light' : 'dark';
    this.setTheme(newTheme);
    return newTheme;
  },

  apply() {
    const root = document.documentElement;
    
    // Remove existing theme classes
    root.classList.remove('light', 'dark');
    root.removeAttribute('data-accent');
    
    // Apply new theme
    root.classList.add(this.currentTheme);
    root.setAttribute('data-theme', this.currentTheme);
    root.setAttribute('data-accent', this.currentAccent);
    
    // Update meta theme-color for mobile browsers
    const metaThemeColor = document.querySelector('meta[name="theme-color"]');
    if (metaThemeColor) {
      metaThemeColor.setAttribute('content', this.currentTheme === 'dark' ? '#0f172a' : '#ffffff');
    }
  },

  getTheme() {
    return this.currentTheme;
  },

  getAccent() {
    return this.currentAccent;
  }
};

// ============================================
// CHART HELPERS (Chart.js utilities)
// ============================================

const ChartHelpers = {
  /**
   * Default chart options
   */
  defaultOptions: {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          usePointStyle: true,
          padding: 16,
          font: {
            family: 'Inter, system-ui, sans-serif',
            size: 12
          }
        }
      },
      tooltip: {
        backgroundColor: 'rgba(17, 24, 39, 0.95)',
        titleFont: {
          family: 'Inter, system-ui, sans-serif',
          size: 13,
          weight: '600'
        },
        bodyFont: {
          family: 'Inter, system-ui, sans-serif',
          size: 12
        },
        padding: 12,
        cornerRadius: 8,
        displayColors: true,
        boxPadding: 4
      }
    }
  },

  /**
   * Create a doughnut chart for pass/fail distribution
   */
  createPassFailChart(canvasId, data) {
    const canvas = document.getElementById(canvasId);
    if (!canvas || typeof Chart === 'undefined') return null;

    return new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: ['Passed', 'Failed', 'Skipped'],
        datasets: [{
          data: [data.passed || 0, data.failed || 0, data.skipped || 0],
          backgroundColor: [
            REPORT_CONFIG.chartColors.passed,
            REPORT_CONFIG.chartColors.failed,
            REPORT_CONFIG.chartColors.warning
          ],
          borderWidth: 0,
          hoverOffset: 4
        }]
      },
      options: {
        ...this.defaultOptions,
        cutout: '70%',
        plugins: {
          ...this.defaultOptions.plugins,
          legend: {
            display: false
          }
        }
      }
    });
  },

  /**
   * Create a bar chart for test execution times
   */
  createExecutionTimesChart(canvasId, data) {
    const canvas = document.getElementById(canvasId);
    if (!canvas || typeof Chart === 'undefined') return null;

    return new Chart(canvas, {
      type: 'bar',
      data: {
        labels: data.labels,
        datasets: [{
          label: 'Execution Time (s)',
          data: data.values.map(v => v / 1000), // Convert to seconds
          backgroundColor: REPORT_CONFIG.chartColors.info,
          borderRadius: 4,
          barThickness: 'flex',
          maxBarThickness: 40
        }]
      },
      options: {
        ...this.defaultOptions,
        indexAxis: 'y',
        plugins: {
          ...this.defaultOptions.plugins,
          legend: {
            display: false
          }
        },
        scales: {
          x: {
            beginAtZero: true,
            grid: {
              display: true,
              color: 'rgba(0, 0, 0, 0.05)'
            },
            ticks: {
              font: {
                family: 'Inter, system-ui, sans-serif',
                size: 11
              }
            }
          },
          y: {
            grid: {
              display: false
            },
            ticks: {
              font: {
                family: 'Inter, system-ui, sans-serif',
                size: 11
              }
            }
          }
        }
      }
    });
  },

  /**
   * Create a line chart for trend data
   */
  createTrendChart(canvasId, data) {
    const canvas = document.getElementById(canvasId);
    if (!canvas || typeof Chart === 'undefined') return null;

    return new Chart(canvas, {
      type: 'line',
      data: {
        labels: data.labels,
        datasets: [{
          label: 'Pass Rate %',
          data: data.passRates,
          borderColor: REPORT_CONFIG.chartColors.passed,
          backgroundColor: `${REPORT_CONFIG.chartColors.passed}33`,
          fill: true,
          tension: 0.4,
          pointRadius: 4,
          pointHoverRadius: 6
        }]
      },
      options: {
        ...this.defaultOptions,
        scales: {
          x: {
            grid: {
              display: false
            },
            ticks: {
              font: {
                family: 'Inter, system-ui, sans-serif',
                size: 11
              }
            }
          },
          y: {
            beginAtZero: true,
            max: 100,
            grid: {
              color: 'rgba(0, 0, 0, 0.05)'
            },
            ticks: {
              callback: (value) => `${value}%`,
              font: {
                family: 'Inter, system-ui, sans-serif',
                size: 11
              }
            }
          }
        }
      }
    });
  },

  /**
   * Update chart colors based on theme
   */
  updateChartTheme(chart, isDark) {
    if (!chart) return;
    
    const textColor = isDark ? '#e2e8f0' : '#374151';
    const gridColor = isDark ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';
    
    if (chart.options.scales) {
      Object.values(chart.options.scales).forEach(scale => {
        if (scale.ticks) {
          scale.ticks.color = textColor;
        }
        if (scale.grid) {
          scale.grid.color = gridColor;
        }
      });
    }
    
    if (chart.options.plugins?.legend?.labels) {
      chart.options.plugins.legend.labels.color = textColor;
    }
    
    chart.update('none');
  }
};

// ============================================
// TABLE MANAGER (Tabulator integration)
// ============================================

const TableManager = {
  instances: new Map(),

  /**
   * Default table configuration
   */
  defaultConfig: {
    layout: 'fitDataStretch',
    responsiveLayout: 'collapse',
    pagination: true,
    paginationSize: 25,
    paginationSizeSelector: [10, 25, 50, 100],
    movableColumns: true,
    resizableColumnFit: true,
    placeholder: 'No data available',
    headerSortClickElement: 'icon',
    cssClass: 'modern-table'
  },

  /**
   * Create a new Tabulator table
   */
  create(elementId, columns, data, customConfig = {}) {
    const element = document.getElementById(elementId);
    if (!element || typeof Tabulator === 'undefined') return null;

    const config = {
      ...this.defaultConfig,
      ...customConfig,
      data: data,
      columns: columns
    };

    const table = new Tabulator(`#${elementId}`, config);
    this.instances.set(elementId, table);
    
    return table;
  },

  /**
   * Get existing table instance
   */
  get(elementId) {
    return this.instances.get(elementId);
  },

  /**
   * Update table data
   */
  updateData(elementId, data) {
    const table = this.get(elementId);
    if (table) {
      table.replaceData(data);
    }
  },

  /**
   * Apply filter to table
   */
  applyFilter(elementId, field, value) {
    const table = this.get(elementId);
    if (table) {
      if (!value) {
        table.clearFilter();
      } else {
        table.setFilter(field, 'like', value);
      }
    }
  },

  /**
   * Common column formatters
   */
  formatters: {
    status(cell) {
      const value = cell.getValue();
      const category = Utils.getStatusCategory(value);
      return `<span class="badge badge--${category}">${Utils.escapeHtml(value)}</span>`;
    },
    
    duration(cell) {
      return Utils.formatDuration(cell.getValue());
    },
    
    date(cell) {
      return Utils.formatDate(cell.getValue(), 'short');
    },
    
    link(cell, formatterParams) {
      const value = cell.getValue();
      const url = formatterParams.urlField 
        ? cell.getRow().getData()[formatterParams.urlField]
        : value;
      return `<a href="${Utils.escapeHtml(url)}" class="text-blue-600 hover:text-blue-800 hover:underline">${Utils.escapeHtml(value)}</a>`;
    },
    
    progress(cell) {
      const value = cell.getValue();
      const color = value >= 80 ? 'success' : value >= 50 ? 'warning' : 'fail';
      return `
        <div class="flex items-center gap-2">
          <div class="progress-bar flex-1">
            <div class="progress-bar__fill progress-bar__fill--${color}" style="width: ${value}%"></div>
          </div>
          <span class="text-xs font-medium">${value}%</span>
        </div>
      `;
    }
  },

  /**
   * Destroy table instance
   */
  destroy(elementId) {
    const table = this.get(elementId);
    if (table) {
      table.destroy();
      this.instances.delete(elementId);
    }
  }
};

// ============================================
// SCREENSHOT VIEWER
// ============================================

const ScreenshotViewer = {
  lightbox: null,
  currentIndex: 0,
  screenshots: [],

  init() {
    // Create lightbox element if doesn't exist
    if (!document.getElementById('screenshot-lightbox')) {
      const lightbox = document.createElement('div');
      lightbox.id = 'screenshot-lightbox';
      lightbox.className = 'lightbox';
      lightbox.innerHTML = `
        <button class="lightbox__close" aria-label="Close">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>
        <button class="lightbox__nav lightbox__nav--prev" aria-label="Previous">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
          </svg>
        </button>
        <img class="lightbox__image" src="" alt="">
        <button class="lightbox__nav lightbox__nav--next" aria-label="Next">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
          </svg>
        </button>
      `;
      document.body.appendChild(lightbox);
      
      // Event listeners
      lightbox.querySelector('.lightbox__close').addEventListener('click', () => this.close());
      lightbox.querySelector('.lightbox__nav--prev').addEventListener('click', () => this.prev());
      lightbox.querySelector('.lightbox__nav--next').addEventListener('click', () => this.next());
      lightbox.addEventListener('click', (e) => {
        if (e.target === lightbox) this.close();
      });
      
      // Keyboard navigation
      document.addEventListener('keydown', (e) => {
        if (!this.lightbox?.classList.contains('lightbox--open')) return;
        
        switch (e.key) {
          case 'Escape':
            this.close();
            break;
          case 'ArrowLeft':
            this.prev();
            break;
          case 'ArrowRight':
            this.next();
            break;
        }
      });
      
      this.lightbox = lightbox;
    }
  },

  open(screenshots, index = 0) {
    this.screenshots = screenshots;
    this.currentIndex = index;
    this.updateImage();
    this.lightbox.classList.add('lightbox--open');
    document.body.style.overflow = 'hidden';
  },

  close() {
    this.lightbox.classList.remove('lightbox--open');
    document.body.style.overflow = '';
  },

  prev() {
    this.currentIndex = (this.currentIndex - 1 + this.screenshots.length) % this.screenshots.length;
    this.updateImage();
  },

  next() {
    this.currentIndex = (this.currentIndex + 1) % this.screenshots.length;
    this.updateImage();
  },

  updateImage() {
    const img = this.lightbox.querySelector('.lightbox__image');
    const screenshot = this.screenshots[this.currentIndex];
    img.src = screenshot.src || screenshot;
    img.alt = screenshot.title || `Screenshot ${this.currentIndex + 1}`;
    
    // Update nav button visibility
    const prevBtn = this.lightbox.querySelector('.lightbox__nav--prev');
    const nextBtn = this.lightbox.querySelector('.lightbox__nav--next');
    prevBtn.style.display = this.screenshots.length > 1 ? '' : 'none';
    nextBtn.style.display = this.screenshots.length > 1 ? '' : 'none';
  }
};

// ============================================
// ALPINE.JS COMPONENTS
// ============================================

document.addEventListener('alpine:init', () => {
  // Theme store
  Alpine.store('theme', {
    current: ThemeManager.currentTheme,
    accent: ThemeManager.currentAccent,
    
    toggle() {
      this.current = ThemeManager.toggleTheme();
    },
    
    setTheme(theme) {
      this.current = theme;
      ThemeManager.setTheme(theme);
    },
    
    setAccent(accent) {
      this.accent = accent;
      ThemeManager.setAccent(accent);
    },
    
    isDark() {
      return this.current === 'dark';
    }
  });

  // Report summary component
  Alpine.data('reportSummary', (initialData = {}) => ({
    data: initialData,
    loading: true,
    error: null,
    
    init() {
      this.loadData();
    },
    
    async loadData() {
      try {
        // Data can be passed directly or loaded from a script tag
        if (typeof window.REPORT_DATA !== 'undefined') {
          this.data = window.REPORT_DATA;
        }
        this.loading = false;
      } catch (e) {
        this.error = 'Failed to load report data';
        this.loading = false;
      }
    },
    
    get totalTests() {
      return this.data.total || 0;
    },
    
    get passedTests() {
      return this.data.passed || 0;
    },
    
    get failedTests() {
      return this.data.failed || 0;
    },
    
    get skippedTests() {
      return this.data.skipped || 0;
    },
    
    get passRate() {
      return Utils.percentage(this.passedTests, this.totalTests);
    },
    
    get duration() {
      return Utils.formatDuration(this.data.duration);
    }
  }));

  // Test list component
  Alpine.data('testList', (tests = []) => ({
    allTests: tests,
    filteredTests: tests,
    searchQuery: '',
    statusFilter: 'all',
    sortField: 'name',
    sortDirection: 'asc',
    
    init() {
      this.applyFilters();
    },
    
    get statusCounts() {
      return {
        all: this.allTests.length,
        passed: this.allTests.filter(t => Utils.getStatusCategory(t.status) === 'passed').length,
        failed: this.allTests.filter(t => Utils.getStatusCategory(t.status) === 'failed').length,
        skipped: this.allTests.filter(t => Utils.getStatusCategory(t.status) === 'warning').length
      };
    },
    
    setStatusFilter(status) {
      this.statusFilter = status;
      this.applyFilters();
    },
    
    setSearchQuery(query) {
      this.searchQuery = query;
      this.applyFilters();
    },
    
    sortBy(field) {
      if (this.sortField === field) {
        this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
      } else {
        this.sortField = field;
        this.sortDirection = 'asc';
      }
      this.applyFilters();
    },
    
    applyFilters() {
      let result = [...this.allTests];
      
      // Status filter
      if (this.statusFilter !== 'all') {
        result = result.filter(test => {
          const category = Utils.getStatusCategory(test.status);
          return category === this.statusFilter;
        });
      }
      
      // Search filter
      if (this.searchQuery) {
        result = Utils.searchFilter(result, this.searchQuery, ['name', 'scenario', 'description']);
      }
      
      // Sort
      result = Utils.sortBy(result, this.sortField, this.sortDirection);
      
      this.filteredTests = result;
    }
  }));

  // Step viewer component for test case details
  Alpine.data('stepViewer', (steps = []) => ({
    steps: steps,
    expandedSteps: new Set(),
    showOnlyFailed: false,
    
    get visibleSteps() {
      if (this.showOnlyFailed) {
        return this.steps.filter(s => Utils.getStatusCategory(s.status) === 'failed');
      }
      return this.steps;
    },
    
    toggleStep(index) {
      if (this.expandedSteps.has(index)) {
        this.expandedSteps.delete(index);
      } else {
        this.expandedSteps.add(index);
      }
      // Trigger reactivity
      this.expandedSteps = new Set(this.expandedSteps);
    },
    
    isExpanded(index) {
      return this.expandedSteps.has(index);
    },
    
    expandAll() {
      this.expandedSteps = new Set(this.visibleSteps.map((_, i) => i));
    },
    
    collapseAll() {
      this.expandedSteps = new Set();
    },
    
    getStepIcon(status) {
      const category = Utils.getStatusCategory(status);
      const icons = {
        passed: '<svg class="w-5 h-5 text-emerald-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"></path></svg>',
        failed: '<svg class="w-5 h-5 text-red-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"></path></svg>',
        warning: '<svg class="w-5 h-5 text-amber-500" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"></path></svg>',
        neutral: '<svg class="w-5 h-5 text-gray-400" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd"></path></svg>'
      };
      return icons[category] || icons.neutral;
    }
  }));

  // Toast notification component
  Alpine.data('toastContainer', () => ({
    toasts: [],
    
    add(message, type = 'info', duration = 5000) {
      const id = Utils.uniqueId('toast');
      this.toasts.push({ id, message, type });
      
      if (duration > 0) {
        setTimeout(() => this.remove(id), duration);
      }
      
      return id;
    },
    
    remove(id) {
      this.toasts = this.toasts.filter(t => t.id !== id);
    },
    
    success(message, duration) {
      return this.add(message, 'success', duration);
    },
    
    error(message, duration) {
      return this.add(message, 'error', duration);
    },
    
    warning(message, duration) {
      return this.add(message, 'warning', duration);
    },
    
    info(message, duration) {
      return this.add(message, 'info', duration);
    }
  }));
});

// ============================================
// INITIALIZATION
// ============================================

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
  // Initialize theme
  ThemeManager.init();
  
  // Initialize screenshot viewer
  ScreenshotViewer.init();
  
  // Add smooth scrolling
  document.documentElement.style.scrollBehavior = 'smooth';
  
  // Print button functionality
  document.querySelectorAll('[data-print]').forEach(btn => {
    btn.addEventListener('click', () => {
      window.print();
    });
  });
  
  // Export functionality placeholder
  document.querySelectorAll('[data-export]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const format = e.currentTarget.dataset.export;
      console.log(`Export to ${format} requested`);
      // Export logic would be implemented here
    });
  });
});

// ============================================
// EXPORTS
// ============================================

// Expose to global scope for use in HTML
window.ReportApp = {
  Utils,
  ThemeManager,
  ChartHelpers,
  TableManager,
  ScreenshotViewer,
  REPORT_CONFIG
};
