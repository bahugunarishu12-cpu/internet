document.addEventListener("DOMContentLoaded", async () => {
  const statusDot = document.getElementById("statusDot");
  const pairingBadge = document.getElementById("pairingBadge");
  const pairedSection = document.getElementById("pairedSection");
  const setupSection = document.getElementById("setupSection");
  const displayDeviceId = document.getElementById("displayDeviceId");
  const displayDeviceName = document.getElementById("displayDeviceName");
  const displayServer = document.getElementById("displayServer");
  const consentToggle = document.getElementById("consentToggle");
  const unpairBtn = document.getElementById("unpairBtn");

  const serverUrlInput = document.getElementById("serverUrlInput");
  const pairingCodeInput = document.getElementById("pairingCodeInput");
  const initialConsentCheckbox = document.getElementById("initialConsentCheckbox");
  const pairBtn = document.getElementById("pairBtn");
  const statusMessage = document.getElementById("statusMessage");

  async function loadState() {
    const config = await chrome.storage.local.get([
      "deviceId",
      "deviceName",
      "monitoringConsent",
      "serverUrl"
    ]);

    if (config.deviceId) {
      pairedSection.classList.remove("hidden");
      setupSection.classList.add("hidden");
      displayDeviceId.textContent = config.deviceId;
      displayDeviceName.textContent = config.deviceName || config.deviceId;
      displayServer.textContent = config.serverUrl || "http://localhost:8000";
      consentToggle.checked = !!config.monitoringConsent;

      if (config.monitoringConsent) {
        statusDot.className = "status-indicator online";
        pairingBadge.textContent = "Monitoring Active";
        pairingBadge.className = "badge active";
      } else {
        statusDot.className = "status-indicator paused";
        pairingBadge.textContent = "Monitoring Paused";
        pairingBadge.className = "badge paused";
      }
    } else {
      pairedSection.classList.add("hidden");
      setupSection.classList.remove("hidden");
      statusDot.className = "status-indicator";
      pairingBadge.textContent = "Unpaired";
      pairingBadge.className = "badge";
    }
  }

  function checkFormValidity() {
    const code = pairingCodeInput.value.trim();
    const consent = initialConsentCheckbox.checked;
    pairBtn.disabled = !(code.length >= 6 && consent);
  }

  pairingCodeInput.addEventListener("input", checkFormValidity);
  initialConsentCheckbox.addEventListener("change", checkFormValidity);

  pairBtn.addEventListener("click", async () => {
    const serverUrl = serverUrlInput.value.trim().replace(/\/$/, "");
    const code = pairingCodeInput.value.trim().toUpperCase();

    statusMessage.textContent = "Verifying pairing code...";
    statusMessage.className = "status-msg info";

    try {
      const resp = await fetch(`${serverUrl}/pairing-codes/verify`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ code })
      });

      const data = await resp.json();
      if (!resp.ok) {
        throw new Error(data.detail || "Pairing failed");
      }

      await chrome.storage.local.set({
        deviceId: data.device_id,
        deviceName: data.device_name || data.device_id,
        serverUrl: serverUrl,
        monitoringConsent: true
      });

      statusMessage.textContent = "Paired successfully!";
      statusMessage.className = "status-msg success";
      setTimeout(loadState, 800);
    } catch (err) {
      statusMessage.textContent = err.message || "Failed to connect to server";
      statusMessage.className = "status-msg error";
    }
  });

  consentToggle.addEventListener("change", async () => {
    await chrome.storage.local.set({ monitoringConsent: consentToggle.checked });
    loadState();
  });

  unpairBtn.addEventListener("click", async () => {
    if (confirm("Are you sure you want to unpair this browser from the device?")) {
      await chrome.storage.local.clear();
      loadState();
    }
  });

  await loadState();
});
