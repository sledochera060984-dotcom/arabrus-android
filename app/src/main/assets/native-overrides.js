(function(){
  if (window.__arabrusNativeFlags) return;
  window.__arabrusNativeFlags = true;
  function applyFlags(){
    try {
      if (localStorage.getItem('arabrus_logged_in') !== '1') return;
      var now = Date.now();
      var future = 4102444800000;
      localStorage.setItem('arabrus_access_open', '1');
      localStorage.setItem('arabrus_native_local_sync', '1');
      localStorage.setItem('arabrus_trial_start', String(now));
      localStorage.setItem('arabrus_trial_until', String(future));
      localStorage.setItem('trialStart', String(now));
      localStorage.setItem('trialUntil', String(future));
      ['hasAccess','canUseFeature','isTrialActive','isAccessAllowed','checkAccess','requireAccess'].forEach(function(n){
        try { window[n] = function(){ return true; }; } catch(e) {}
      });
      ['isTrialExpired','isExpired','needsSubscription'].forEach(function(n){
        try { window[n] = function(){ return false; }; } catch(e) {}
      });
    } catch(e) {}
  }
  window.AndroidNativeBridge = window.AndroidNativeBridge || {};
  window.AndroidNativeBridge.openLocalAccess = applyFlags;
  window.AndroidNativeBridge.fixSyncUi = applyFlags;
  applyFlags();
  setInterval(applyFlags, 500);
})();
