/**
 * Sample Trace Data for Testing the Trace Viewer
 * This file demonstrates the expected data format for the trace viewer component
 */

const SAMPLE_TRACE_DATA = {
  actionCount: 15,
  screenshotCount: 8,
  duration: "45.32s",
  actions: [
    {
      method: "navigateTo",
      startTime: 1000,
      endTime: 1250,
      description: "Navigate to login page"
    },
    {
      method: "fill",
      startTime: 1250,
      endTime: 1500,
      description: "Enter username"
    },
    {
      method: "fill",
      startTime: 1500,
      endTime: 1650,
      description: "Enter password"
    },
    {
      method: "click",
      startTime: 1650,
      endTime: 1900,
      description: "Click login button"
    },
    {
      method: "wait",
      startTime: 1900,
      endTime: 3200,
      description: "Wait for page load"
    },
    {
      method: "verifyElement",
      startTime: 3200,
      endTime: 3350,
      description: "Verify dashboard loaded"
    },
    {
      method: "click",
      startTime: 3350,
      endTime: 3500,
      description: "Click on menu"
    },
    {
      method: "click",
      startTime: 3500,
      endTime: 3650,
      description: "Select reports option"
    },
    {
      method: "wait",
      startTime: 3650,
      endTime: 5000,
      description: "Wait for reports to load"
    },
    {
      method: "verifyText",
      startTime: 5000,
      endTime: 5150,
      description: "Verify report title"
    },
    {
      method: "click",
      startTime: 5150,
      endTime: 5300,
      description: "Download report"
    },
    {
      method: "wait",
      startTime: 5300,
      endTime: 8000,
      description: "Wait for download"
    },
    {
      method: "verifyFile",
      startTime: 8000,
      endTime: 8200,
      description: "Verify download successful"
    },
    {
      method: "click",
      startTime: 8200,
      endTime: 8350,
      description: "Logout"
    },
    {
      method: "verifyElement",
      startTime: 8350,
      endTime: 8500,
      description: "Verify logout successful"
    }
  ],
  screenshots: [
    {
      timestamp: "2026-02-20T10:30:15.123Z",
      name: "login_page_initial"
    },
    {
      timestamp: "2026-02-20T10:30:31.500Z",
      name: "login_credentials_filled"
    },
    {
      timestamp: "2026-02-20T10:30:45.750Z",
      name: "dashboard_loaded"
    },
    {
      timestamp: "2026-02-20T10:31:05.200Z",
      name: "menu_opened"
    },
    {
      timestamp: "2026-02-20T10:31:22.600Z",
      name: "reports_selected"
    },
    {
      timestamp: "2026-02-20T10:31:45.300Z",
      name: "reports_list_displayed"
    },
    {
      timestamp: "2026-02-20T10:32:15.800Z",
      name: "report_downloaded"
    },
    {
      timestamp: "2026-02-20T10:32:48.320Z",
      name: "logout_complete"
    }
  ]
};

// Export for use in tests
if (typeof module !== 'undefined' && module.exports) {
  module.exports = SAMPLE_TRACE_DATA;
}
