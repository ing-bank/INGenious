/**
 * INGenious Modern Report - Library Loader
 * Handles loading of external libraries from CDN or local fallbacks
 * Version: 1.0.0
 */

(function() {
  'use strict';

  // CDN URLs for libraries
  const LIBRARIES = {
    alpineJs: {
      cdn: 'https://cdn.jsdelivr.net/npm/alpinejs@3.x.x/dist/cdn.min.js',
      integrity: '',
      crossorigin: 'anonymous',
      defer: true
    },
    chartJs: {
      cdn: 'https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js',
      integrity: '',
      crossorigin: 'anonymous'
    },
    tabulator: {
      css: 'https://cdn.jsdelivr.net/npm/tabulator-tables@6.2.1/dist/css/tabulator_semanticui.min.css',
      js: 'https://cdn.jsdelivr.net/npm/tabulator-tables@6.2.1/dist/js/tabulator.min.js',
      integrity: '',
      crossorigin: 'anonymous'
    }
  };

  /**
   * Load a script from URL
   */
  function loadScript(url, options = {}) {
    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = url;
      script.async = true;
      
      if (options.defer) script.defer = true;
      if (options.integrity) script.integrity = options.integrity;
      if (options.crossorigin) script.crossOrigin = options.crossorigin;
      
      script.onload = resolve;
      script.onerror = reject;
      
      document.head.appendChild(script);
    });
  }

  /**
   * Load a stylesheet from URL
   */
  function loadStylesheet(url, options = {}) {
    return new Promise((resolve, reject) => {
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = url;
      
      if (options.integrity) link.integrity = options.integrity;
      if (options.crossorigin) link.crossOrigin = options.crossorigin;
      
      link.onload = resolve;
      link.onerror = reject;
      
      document.head.appendChild(link);
    });
  }

  /**
   * Check if library is already loaded
   */
  function isLoaded(name) {
    switch (name) {
      case 'alpineJs':
        return typeof Alpine !== 'undefined';
      case 'chartJs':
        return typeof Chart !== 'undefined';
      case 'tabulator':
        return typeof Tabulator !== 'undefined';
      default:
        return false;
    }
  }

  /**
   * Load all required libraries
   */
  async function loadAllLibraries(options = {}) {
    const {
      useAlpine = true,
      useCharts = true,
      useTables = true
    } = options;

    const promises = [];

    // Load Alpine.js (required)
    if (useAlpine && !isLoaded('alpineJs')) {
      promises.push(loadScript(LIBRARIES.alpineJs.cdn, LIBRARIES.alpineJs));
    }

    // Load Chart.js (optional)
    if (useCharts && !isLoaded('chartJs')) {
      promises.push(loadScript(LIBRARIES.chartJs.cdn, LIBRARIES.chartJs));
    }

    // Load Tabulator (optional)
    if (useTables && !isLoaded('tabulator')) {
      promises.push(loadStylesheet(LIBRARIES.tabulator.css, LIBRARIES.tabulator));
      promises.push(loadScript(LIBRARIES.tabulator.js, LIBRARIES.tabulator));
    }

    try {
      await Promise.all(promises);
      console.log('[INGenious Report] Libraries loaded successfully');
      return true;
    } catch (error) {
      console.error('[INGenious Report] Failed to load some libraries:', error);
      return false;
    }
  }

  // Expose loader to global scope
  window.ReportLibraries = {
    load: loadAllLibraries,
    loadScript,
    loadStylesheet,
    isLoaded,
    LIBRARIES
  };
})();
