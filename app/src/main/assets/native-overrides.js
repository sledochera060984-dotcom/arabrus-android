(function(){
  if (window.__arabrusNativeFlags) return;
  window.__arabrusNativeFlags = true;

  function isSignedIn(){
    try {
      return localStorage.getItem('arabrus_logged_in') === '1' ||
        !!localStorage.getItem('arabrus_user') ||
        !!localStorage.getItem('user');
    } catch(e) {
      return false;
    }
  }

  function ensureStyle(){
    if (document.getElementById('arabrus-native-style')) return;
    var style = document.createElement('style');
    style.id = 'arabrus-native-style';
    style.textContent = '.lock-card{display:none!important}.locked{opacity:1!important;pointer-events:auto!important}.btn.locked{opacity:1!important}';
    document.head.appendChild(style);
  }

  function applyFlags(){
    try {
      if (!isSignedIn()) return;
      var now = Date.now();
      var future = 4102444800000;
      localStorage.setItem('arabrus_logged_in', '1');
      localStorage.setItem('arabrus_access_open', '1');
      localStorage.setItem('arabrus_native_local_sync', '1');
      localStorage.setItem('arabrus_trial_start', String(now));
      localStorage.setItem('arabrus_trial_until', String(future));
      localStorage.setItem('trialStart', String(now));
      localStorage.setItem('trialUntil', String(future));
      localStorage.setItem('accessUntil', String(future));
      ensureStyle();

      ['hasAccess','canUseFeature','isTrialActive','isAccessAllowed','checkAccess','requireAccess','canOpenFavorites','canOpenNotes','canUseCards'].forEach(function(n){
        try { window[n] = function(){ return true; }; } catch(e) {}
      });
      ['isTrialExpired','isExpired','needsSubscription'].forEach(function(n){
        try { window[n] = function(){ return false; }; } catch(e) {}
      });

      document.querySelectorAll('.lock-card').forEach(function(el){ el.style.display = 'none'; });
      document.querySelectorAll('.locked').forEach(function(el){
        el.classList.remove('locked');
        el.removeAttribute('disabled');
        el.style.pointerEvents = '';
        el.style.opacity = '';
      });
      document.querySelectorAll('.tab,.chip,.btn,button').forEach(function(el){
        if (el.textContent) el.textContent = el.textContent.replace(/🔒/g, '').trim();
      });

      var sync = document.getElementById('syncIndicator');
      if (sync) {
        sync.className = 'status ok';
        sync.textContent = '✅ Сохранено на устройстве';
      }
      var trial = document.getElementById('trialIndicator');
      if (trial) {
        trial.className = 'status ok';
        trial.textContent = '✅ Доступ открыт';
      }
    } catch(e) {}
  }

  window.AndroidNativeBridge = window.AndroidNativeBridge || {};
  window.AndroidNativeBridge.openLocalAccess = applyFlags;
  window.AndroidNativeBridge.fixSyncUi = applyFlags;

  applyFlags();
  document.addEventListener('DOMContentLoaded', applyFlags);
  document.addEventListener('click', function(){
    setTimeout(applyFlags, 50);
    setTimeout(applyFlags, 300);
  }, true);
  new MutationObserver(applyFlags).observe(document.documentElement, {childList:true, subtree:true});
  setInterval(applyFlags, 500);
})();
