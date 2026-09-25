/**
 * Network & Chrome Activity Monitor - Authorized Extension
 * 
 * STRICT PRIVACY PRINCIPLES:
 * - Only supported search-engine result URLs are inspected.
 * - Extracts ONLY the search parameter (e.g. ?q=...).
 * - DOES NOT intercept passwords, form fields, cookies, keystrokes, or HTTPS page contents.
 * - Dispatches strictly: { device_id, browser: "Chrome", search_engine, search_query, timestamp }.
 */

const DEFAULT_SERVER_URL = "http://localhost:8000";
let lastLoggedQuery = "";
let lastLoggedTime = 0;

// Search engine patterns mapping to search query parameter
const SEARCH_ENGINES = [
  { host: "google.com", param: "q", name: "Google", path: "/search" },
  { host: "bing.com", param: "q", name: "Bing", path: "/search" },
  { host: "duckduckgo.com", param: "q", name: "DuckDuckGo", path: "/" },
  { host: "search.yahoo.com", param: "p", name: "Yahoo", path: "/search" },
  { host: "ecosia.org", param: "q", name: "Ecosia", path: "/search" }
];

function extractSearchInfo(urlStr) {
  try {
    const url = new URL(urlStr);
    for (const engine of SEARCH_ENGINES) {
      if (url.hostname.includes(engine.host)) {
        if (engine.path === "/" || url.pathname.startsWith(engine.path)) {
          const query = url.searchParams.get(engine.param);
          if (query && query.trim().length > 0) {
            return {
              search_engine: engine.name,
              search_query: query.trim()
            };
          }
        }
      }
    }
  } catch (e) {
    // Ignore invalid URLs
  }
  return null;
}

chrome.tabs.onUpdated.addListener(async (tabId, changeInfo, tab) => {
  // Only process complete page navigations with URL
  if (changeInfo.status === "complete" && tab.url) {
    const searchData = extractSearchInfo(tab.url);
    if (!searchData) return;

    // Check storage for paired device and consent
    const config = await chrome.storage.local.get([
      "deviceId",
      "monitoringConsent",
      "serverUrl"
    ]);

    if (!config.deviceId || !config.monitoringConsent) {
      console.log("[ActivityMonitor] Extension not paired or consent not granted.");
      return;
    }

    // Debounce exact duplicates within 5 seconds
    const now = Date.now();
    const queryKey = `${searchData.search_engine}:${searchData.search_query}`;
    if (queryKey === lastLoggedQuery && (now - lastLoggedTime) < 5000) {
      return;
    }

    lastLoggedQuery = queryKey;
    lastLoggedTime = now;

    const payload = {
      device_id: config.deviceId,
      browser: "Chrome",
      search_engine: searchData.search_engine,
      search_query: searchData.search_query,
      timestamp: new Date().toISOString()
    };

    const serverUrl = config.serverUrl || DEFAULT_SERVER_URL;

    try {
      const response = await fetch(`${serverUrl}/chrome-searches`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      if (response.ok) {
        console.log("[ActivityMonitor] Authorized search sent:", searchData.search_query);
      } else {
        console.warn("[ActivityMonitor] Server returned status:", response.status);
      }
    } catch (err) {
      console.error("[ActivityMonitor] Failed to transmit search activity:", err);
    }
  }
});
