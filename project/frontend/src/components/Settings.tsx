import React, { useState } from "react";
import { ShieldCheck, Lock, Database, Globe, Key, Check } from "lucide-react";

export const Settings: React.FC = () => {
  const [copied, setCopied] = useState(false);

  const manifestV3Snippet = `{
  "manifest_version": 3,
  "name": "Network & Chrome Activity Monitor",
  "permissions": ["storage", "tabs"],
  "host_permissions": [
    "*://*.google.com/search*",
    "*://*.bing.com/search*",
    "*://duckduckgo.com/*"
  ]
}`;

  const copyManifest = () => {
    navigator.clipboard.writeText(manifestV3Snippet);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h2 className="text-2xl font-bold text-white">System Settings & Privacy Compliance</h2>
        <p className="text-sm text-slate-400">Security configurations, consent guidelines, and administrative controls</p>
      </div>

      {/* Privacy Guarantee Card */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-emerald-500/10 text-emerald-400 rounded-lg">
            <ShieldCheck size={22} />
          </div>
          <div>
            <h3 className="font-semibold text-white text-base">Explicit Consent & Privacy Principles</h3>
            <p className="text-xs text-slate-400">Guaranteed system architecture rules enforced by design</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2 text-xs">
          <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800">
            <div className="font-semibold text-slate-200 mb-1">Zero HTTPS Interception</div>
            <p className="text-slate-400 leading-relaxed">
              No TLS inspection, MITM proxying, or payload decryption. Network logging records high-level host domain connections only.
            </p>
          </div>
          <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800">
            <div className="font-semibold text-slate-200 mb-1">No Sensitive Credentials or Cookies</div>
            <p className="text-slate-400 leading-relaxed">
              Passwords, session tokens, private messages, email contents, and form inputs are never collected or transmitted.
            </p>
          </div>
          <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800">
            <div className="font-semibold text-slate-200 mb-1">Pairing by Authorization Code</div>
            <p className="text-slate-400 leading-relaxed">
              Extensions must be explicitly paired using single-use administrator codes (PAIR-XXXXX) rather than intrusive device fingerprinting.
            </p>
          </div>
          <div className="p-3 bg-slate-950/60 rounded-lg border border-slate-800">
            <div className="font-semibold text-slate-200 mb-1">Incognito Mode Notice</div>
            <p className="text-slate-400 leading-relaxed">
              Extensions cannot inspect Incognito mode unless the user explicitly enables &quot;Allow in Incognito&quot; inside Chrome extension settings.
            </p>
          </div>
        </div>
      </div>

      {/* Backend & Database Config */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-indigo-500/10 text-indigo-400 rounded-lg">
            <Database size={22} />
          </div>
          <div>
            <h3 className="font-semibold text-white text-base">Backend & MongoDB Endpoints</h3>
            <p className="text-xs text-slate-400">Connection details for REST APIs and Database</p>
          </div>
        </div>

        <div className="space-y-3 text-xs">
          <div className="flex justify-between items-center py-2 border-b border-slate-800">
            <span className="text-slate-400">FastAPI Server URL</span>
            <span className="font-mono text-indigo-300">http://localhost:8000</span>
          </div>
          <div className="flex justify-between items-center py-2 border-b border-slate-800">
            <span className="text-slate-400">MongoDB Database</span>
            <span className="font-mono text-indigo-300">network_monitor_db</span>
          </div>
          <div className="flex justify-between items-center py-2 border-b border-slate-800">
            <span className="text-slate-400">JWT Token Expiry</span>
            <span className="font-mono text-indigo-300">24 Hours (1440 mins)</span>
          </div>
          <div className="flex justify-between items-center py-2">
            <span className="text-slate-400">Active Admin User</span>
            <span className="font-mono text-emerald-400">admin (JWT Authenticated)</span>
          </div>
        </div>
      </div>

      {/* Extension Manifest Preview */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-amber-500/10 text-amber-400 rounded-lg">
              <Key size={22} />
            </div>
            <div>
              <h3 className="font-semibold text-white text-base">Chrome Extension Manifest V3 Policy</h3>
              <p className="text-xs text-slate-400">Minimal permissions for search engine query capturing</p>
            </div>
          </div>
          <button
            onClick={copyManifest}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs rounded-lg transition"
          >
            {copied ? <Check size={14} className="text-emerald-400" /> : <ShieldCheck size={14} />}
            <span>{copied ? "Copied" : "Copy Manifest"}</span>
          </button>
        </div>

        <pre className="p-4 bg-slate-950 border border-slate-800 rounded-lg text-xs font-mono text-slate-300 overflow-x-auto">
          {manifestV3Snippet}
        </pre>
      </div>
    </div>
  );
};
